package com.landpoint.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.landpoint.app.PendingDeletes
import com.landpoint.app.data.db.LandDao
import com.landpoint.app.data.db.LandDatabase
import com.landpoint.app.data.export.LandTestFactory
import com.landpoint.app.data.model.AuditActions
import com.landpoint.app.data.model.AuditEntities
import com.landpoint.app.data.model.toEntity
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The soft-delete layer, against a real (in-memory) database and real photo
 * files: deleting puts a row in the bin and touches nothing else, restoring is
 * a one-column update, purging is the only thing that erases, and the
 * retention sweep is deterministic enough to run twice in a row.
 *
 * The undo round-trips here pin [PendingDeletes]' contract after the change to
 * soft deletes: an undone delete leaves the row exactly as it was, and a
 * committed one leaves it IN the bin rather than nowhere.
 */
@RunWith(RobolectricTestRunner::class)
class SoftDeleteRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: LandDatabase
    private lateinit var repository: LandRepository
    private lateinit var dao: LandDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LandDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.landDao()
        repository = LandRepository(dao, PhotoStore(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seed(name: String): String {
        val land = LandTestFactory.land(name = name)
        repository.save(land.toEntity())
        return land.id
    }

    /** A photo file with content, attached to [landId]; returns the file. */
    private suspend fun seedPhoto(landId: String): File =
        File.createTempFile("photo", ".jpg", context.cacheDir).apply { writeBytes(byteArrayOf(1, 2, 3)) }
            .also { repository.addPhoto(landId, it.absolutePath) }

    @Test
    fun `soft-deleted parcels leave the live flows and join the bin newest first`() = runTest {
        val (oldest, newest, survivor) = listOf(seed("Sawah utara"), seed("Kebun kopi"), seed("Sawah selatan"))
        dao.softDeleteById(oldest, 1_000L)
        dao.softDeleteById(newest, 2_000L)

        assertEquals(listOf(newest, oldest), repository.recentlyDeleted().first().map { it.id })
        assertEquals(listOf("Sawah selatan"), repository.getAllLands().map { it.name })
        assertEquals(1, repository.observeCount().first())
        assertNull("the detail path must not serve a binned parcel", repository.getLand(oldest))

        repository.restore(oldest)
        assertEquals(listOf(newest), repository.recentlyDeleted().first().map { it.id })
        assertEquals(2, repository.getAllLands().size)
    }

    @Test
    fun `deleting through the repository keeps the row and its photo files`() = runTest {
        val id = seed("Sawah utara")
        val photo = seedPhoto(id)

        repository.delete(id)

        assertTrue("the row must survive in the bin", dao.getDeletedById(id) != null)
        assertTrue("the photo file must survive restore", photo.exists())
        assertEquals(0, repository.getAllLands().size)
        assertEquals(1, repository.recentlyDeleted().first().size)

        val entries = repository.observeAuditFor(AuditEntities.LAND, id).first()
        assertEquals(AuditActions.DELETE, entries.first().action)
    }

    @Test
    fun `an undone delete leaves the row exactly as it was`() = runTest {
        val id = seed("Sawah utara")
        val photo = seedPhoto(id)
        val pending = PendingDeletes(repository, windowMs = 60_000)
        try {
            pending.schedule(setOf(id))
            pending.undo()

            assertTrue(pending.ids.value.isEmpty())
            assertEquals(1, repository.getAllLands().size)
            assertTrue(repository.recentlyDeleted().first().isEmpty())
            assertTrue("undo must not touch the photo file", photo.exists())
        } finally {
            // Cancels the timer for good: one firing after tearDown would hit a
            // closed database.
            pending.undo()
        }
    }

    @Test
    fun `a committed delete puts the row in the bin, not the void`() = runTest {
        val id = seed("Sawah utara")
        val photo = seedPhoto(id)
        val pending = PendingDeletes(repository, windowMs = 60_000)
        try {
            pending.schedule(setOf(id))
            pending.flush()

            assertTrue(pending.ids.value.isEmpty())
            assertTrue("the row is binned, not erased", dao.getDeletedById(id) != null)
            assertEquals(0, repository.getAllLands().size)
            assertEquals(1, repository.recentlyDeleted().first().size)
            assertTrue("binning must not touch the photo file", photo.exists())
        } finally {
            pending.undo()
        }
    }

    @Test
    fun `purging a binned parcel erases the row, its photo rows and the files`() = runTest {
        val id = seed("Sawah utara")
        val photo = seedPhoto(id)
        repository.delete(id)

        repository.purge(id)

        assertNull(dao.getDeletedById(id))
        assertNull(dao.getById(id))
        assertTrue(dao.getPhotosFor(id).isEmpty())
        assertFalse("purge is what finally removes the photo file", photo.exists())

        val entries = repository.observeAuditFor(AuditEntities.LAND, id).first()
        assertEquals(AuditActions.PURGE, entries.first().action)
    }

    @Test
    fun `purgeExpired runs twice and the second pass has nothing left to do`() = runTest {
        val now = 1_700_000_000_000L
        val expired = seed("Tua")
        val boundary = seed("Tepi")
        val fresh = seed("Baru")
        dao.softDeleteById(expired, now - THIRTY_DAYS - 1_000L)
        dao.softDeleteById(boundary, now - THIRTY_DAYS)
        dao.softDeleteById(fresh, now - THIRTY_DAYS + 60_000L)

        assertEquals(1, repository.purgeExpired(now = now))
        assertEquals(0, repository.purgeExpired(now = now))

        assertEquals(listOf(fresh, boundary), repository.recentlyDeleted().first().map { it.id })
        assertNull(dao.getDeletedById(expired))
    }

    @Test
    fun `purgeExpired also removes the photo files of what it purges`() = runTest {
        val now = 1_700_000_000_000L
        val id = seed("Tua")
        val photo = seedPhoto(id)
        dao.softDeleteById(id, now - THIRTY_DAYS - 1_000L)

        assertEquals(1, repository.purgeExpired(now = now))

        assertFalse(photo.exists())
        assertTrue(dao.getAllPhotos().isEmpty())
    }

    @Test
    fun `clearing all bins everything and a restore brings it back`() = runTest {
        val (first, second) = listOf(seed("Sawah utara"), seed("Kebun kopi"))
        val photo = seedPhoto(first)

        repository.deleteAll()

        assertEquals(0, repository.getAllLands().size)
        assertEquals(2, repository.recentlyDeleted().first().size)
        assertTrue("a recoverable clear-all must keep the photos", photo.exists())

        repository.restore(first)
        repository.restore(second)
        assertEquals(2, repository.getAllLands().size)
        assertTrue(repository.recentlyDeleted().first().isEmpty())
    }

    @Test
    fun `import no longer sees a binned parcel as an existing duplicate`() = runTest {
        val id = seed("Sawah utara")
        dao.softDeleteById(id, 1_000L)

        assertTrue(repository.findExistingIds(listOf(id)).isEmpty())
        assertNull(repository.findDuplicateByCoordinates("Sawah utara", -6.9, 107.6))
        assertNotNull("the row is still there for the bin", dao.getDeletedById(id))
    }

    private companion object {
        const val THIRTY_DAYS = 30L * 24 * 60 * 60 * 1000
    }
}
