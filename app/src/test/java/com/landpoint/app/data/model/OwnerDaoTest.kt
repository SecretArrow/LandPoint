package com.landpoint.app.data.model

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.landpoint.app.data.LandRepository
import com.landpoint.app.data.PhotoStore
import com.landpoint.app.data.db.LandDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The rules the owners feature depends on, against a real (in-memory)
 * database: saving, the audit trail, and the one cascade that matters —
 * deleting an owner un-assigns their parcels instead of deleting them.
 */
@RunWith(RobolectricTestRunner::class)
class OwnerDaoTest {

    private lateinit var context: Context
    private lateinit var db: LandDatabase
    private lateinit var repository: LandRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LandDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LandRepository(db.landDao(), PhotoStore(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun owner(name: String, nik: String? = null) = Owner(
        id = name,
        fullName = name,
        nik = nik,
        address = "",
        phone = null,
        email = null,
        photoPath = null,
        createdAt = 1_700_000_000_000,
        updatedAt = 1_700_000_000_000
    )

    private fun land(id: String, ownerId: String?) = LandEntity(
        id = id,
        name = id,
        latitude = -6.9,
        longitude = 107.6,
        ownerId = ownerId
    )

    @Test
    fun `an owner can be saved and found again`() = runTest {
        repository.saveOwner(owner("Budi Santoso", "3201234567890001"))

        val stored = repository.getOwner("Budi Santoso")
        assertNotNull(stored)
        assertEquals("Budi Santoso", stored!!.fullName)
        assertEquals("3201234567890001", stored.nik)

        assertEquals("Budi Santoso", repository.findOwnerByNik("3201234567890001")?.fullName)
        assertNull(repository.findOwnerByNik("9999999999999999"))
    }

    @Test
    fun `the owner list counts parcels`() = runTest {
        repository.saveOwner(owner("Budi"))
        repository.save(land("kebun", ownerId = "Budi"))
        repository.save(land("sawah", ownerId = "Budi"))
        repository.save(land("tanah kosong", ownerId = null))

        val owners = repository.observeOwners().first()
        assertEquals(1, owners.size)
        assertEquals(2, owners.first().parcelCount)
    }

    @Test
    fun `lands carry their owner's name`() = runTest {
        repository.saveOwner(owner("Budi"))
        repository.save(land("kebun", ownerId = "Budi"))
        repository.save(land("sawah", ownerId = null))

        val lands = repository.observeLands().first().associateBy { it.id }
        assertEquals("Budi", lands.getValue("kebun").ownerName)
        assertNull(lands.getValue("sawah").ownerName)
    }

    @Test
    fun `deleting an owner un-assigns their lands but keeps them`() = runTest {
        val budi = owner("Budi")
        repository.saveOwner(budi)
        repository.save(land("kebun", ownerId = "Budi"))
        repository.save(land("sawah", ownerId = "Budi"))

        repository.deleteOwner(budi)

        assertNull(repository.getOwner("Budi"))
        val lands = repository.observeLands().first()
        assertEquals(2, lands.size)
        assertTrue(lands.all { it.ownerId == null })
    }

    @Test
    fun `saving a land writes an audit entry`() = runTest {
        repository.save(land("kebun", ownerId = null))

        val entries = repository.observeAuditFor(AuditEntities.LAND, "kebun").first()
        assertEquals(1, entries.size)
        assertEquals(AuditActions.CREATE, entries.first().action)
        assertEquals("kebun", entries.first().detail)
    }

    @Test
    fun `saving an existing land records an update, not a second creation`() = runTest {
        repository.save(land("kebun", ownerId = null))
        repository.save(land("kebun", ownerId = null).copy(name = "kebun baru"))

        val entries = repository.observeAuditFor(AuditEntities.LAND, "kebun").first()
        assertEquals(2, entries.size)
        assertEquals(AuditActions.CREATE, entries.last().action)
        assertEquals(AuditActions.UPDATE, entries.first().action)
        assertEquals("kebun baru", entries.first().detail)
    }

    @Test
    fun `deleting a land records the deletion`() = runTest {
        repository.save(land("kebun", ownerId = null))
        repository.delete("kebun")

        val entries = repository.observeAuditFor(AuditEntities.LAND, "kebun").first()
        assertEquals(AuditActions.DELETE, entries.first().action)
    }

    @Test
    fun `owner changes are audited too`() = runTest {
        val budi = owner("Budi")
        repository.saveOwner(budi)
        repository.updateOwner(budi.copy(address = "Jl. Merdeka 1"))
        repository.deleteOwner(budi.copy(address = "Jl. Merdeka 1"))

        val entries = repository.observeAuditFor(AuditEntities.OWNER, "Budi").first()
        assertEquals(listOf(AuditActions.DELETE, AuditActions.UPDATE, AuditActions.CREATE), entries.map { it.action })
    }
}
