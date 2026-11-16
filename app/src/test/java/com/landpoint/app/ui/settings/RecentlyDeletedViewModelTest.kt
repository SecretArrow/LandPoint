package com.landpoint.app.ui.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.landpoint.app.data.LandRepository
import com.landpoint.app.data.PhotoStore
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.db.LandDatabase
import com.landpoint.app.data.export.LandTestFactory
import com.landpoint.app.data.model.toEntity
import com.landpoint.app.util.AppStrings
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The bin is where a delete ends up, so these tests pin the difference between
 * its two exits: restore, which changes nothing but the one column, and purge,
 * which takes the row and its photo rows for good. The order of the list is
 * pinned too, because "the most recent mistake first" is how the screen reads.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecentlyDeletedViewModelTest {

    private lateinit var context: Context
    private lateinit var db: LandDatabase
    private lateinit var repository: LandRepository
    private lateinit var viewModel: RecentlyDeletedViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LandDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LandRepository(db.landDao(), PhotoStore(context))
        viewModel = RecentlyDeletedViewModel(
            repository = repository,
            strings = AppStrings(context),
            settings = SettingsRepository(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    /** `uiState` only produces while something collects it. */
    private fun TestScope.collectState() {
        backgroundScope.launch { viewModel.uiState.collect { } }
    }

    /**
     * Room's query executor emits from real background threads, so the state
     * this screen shows arrives when it arrives. Every wait here is a real one
     * with a deadline, for the same reason [com.landpoint.app.ui.lands.LandListViewModelTest]
     * waits this way: a virtual-time advance would return before the database
     * had said anything at all and make the assertion below vacuous.
     */
    private suspend fun awaitState(reason: String, predicate: (RecentlyDeletedUiState) -> Boolean): RecentlyDeletedUiState =
        withContext(Dispatchers.Default) {
            try {
                withTimeout(10_000) { viewModel.uiState.first(predicate) }
            } catch (e: TimeoutCancellationException) {
                throw AssertionError("timed out waiting for $reason; last state was ${describe()}")
            }
        }

    private fun describe(): String = viewModel.uiState.value.let {
        "bin=${it.bin.map { land -> land.name }} busy=${it.isBusy} message=${it.message}"
    }

    private suspend fun seed(vararg names: String): List<String> = names.map { name ->
        val land = LandTestFactory.land(name = name)
        repository.save(land.toEntity())
        land.id
    }

    /**
     * `runTest`'s clock is virtual, so the age is written into the column
     * directly — the same trick the repository's own clock-injection tests use.
     */
    private suspend fun seedExpired(name: String, daysAgo: Long): String {
        val land = LandTestFactory.land(name = name)
        repository.save(land.toEntity())
        val deletedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo)
        db.landDao().softDeleteById(land.id, deletedAt)
        return land.id
    }

    @Test
    fun `the bin lists the newest deletion first`() = runTest {
        val ids = seed("Sawah utara", "Kebun kopi")
        collectState()
        awaitState("both lands live") { it.bin.isEmpty() && !it.isLoading }

        repository.delete(ids[0])
        // A pause so the second delete carries a strictly later timestamp than
        // the first — two deletes inside one millisecond would order by luck.
        withContext(Dispatchers.Default) { Thread.sleep(10) }
        repository.delete(ids[1])

        val state = awaitState("both lands binned") { it.bin.size == 2 }
        assertEquals(listOf("Kebun kopi", "Sawah utara"), state.bin.map { it.name })
        assertTrue("the bin must say when each parcel was deleted", state.bin.all { it.deletedAt != null })
    }

    @Test
    fun `restoring a parcel puts it back among the live ones`() = runTest {
        val ids = seed("Sawah utara", "Kebun kopi")
        collectState()
        awaitState("both lands live") { it.bin.isEmpty() && !it.isLoading }

        repository.delete(ids[0])
        awaitState("one land binned") { it.bin.size == 1 }

        viewModel.restore(ids[0])
        awaitState("the restore to finish") { it.bin.isEmpty() && !it.isBusy && it.message != null }

        // Order follows created_at, which the factory gives both lands equally,
        // so the restore is asserted on membership rather than the list's tie-break.
        val live = repository.observeLands().first()
        assertEquals(2, live.size)
        assertEquals(setOf("Sawah utara", "Kebun kopi"), live.map { it.name }.toSet())
    }

    @Test
    fun `purging a parcel removes it for good`() = runTest {
        val ids = seed("Sawah utara", "Kebun kopi")
        collectState()
        awaitState("both lands live") { it.bin.isEmpty() && !it.isLoading }

        repository.delete(ids[0])
        awaitState("one land binned") { it.bin.size == 1 }

        viewModel.purge(ids[0])
        awaitState("the purge to finish") { it.bin.isEmpty() && !it.isBusy && it.message != null }

        // Nowhere left to look: not live, not binned. The row is genuinely gone.
        assertEquals(1, repository.getAllLands().size)
        assertNull(db.landDao().getDeletedById(ids[0]))
    }

    @Test
    fun `emptying the bin purges exactly what was on screen`() = runTest {
        val ids = seed("Sawah utara", "Kebun kopi", "Kebun lada")
        collectState()
        awaitState("all three lands live") { it.bin.isEmpty() && !it.isLoading }

        repository.delete(ids[0])
        repository.delete(ids[1])
        awaitState("two lands binned") { it.bin.size == 2 }

        viewModel.emptyBin()
        awaitState("the empty to finish") { it.bin.isEmpty() && !it.isBusy && it.message != null }

        // The third parcel was never binned, so emptying the bin leaves it be.
        assertEquals(1, repository.getAllLands().size)
        assertNull(db.landDao().getDeletedById(ids[0]))
        assertNull(db.landDao().getDeletedById(ids[1]))
    }

    @Test
    fun `entering the bin sweeps the expired parcels away`() = runTest {
        val expiredId = seedExpired("Gudang lama", daysAgo = 31)
        val (freshId) = seed("Sawah utara")

        // A fresh ViewModel is the screen being entered: its init sweep is the
        // one-off purgeExpired the screen asks for, so the expired row never
        // shows and the fresh one — which is live, not binned — survives it.
        viewModel = RecentlyDeletedViewModel(
            repository = repository,
            strings = AppStrings(context),
            settings = SettingsRepository(context)
        )
        collectState()

        // The bin's first settled emission still holds the expired row unless
        // the sweep has already beaten the query to it; either way, an empty bin
        // with loading done can only mean the sweep finished its work.
        awaitState("the bin to settle empty after the sweep") {
            !it.isLoading && it.bin.isEmpty()
        }
        assertNull("the expired parcel must be gone for good", db.landDao().getDeletedById(expiredId))
        assertNull("the fresh parcel was never binned, so the sweep must not touch it", db.landDao().getDeletedById(freshId))
        assertEquals(listOf("Sawah utara"), repository.getAllLands().map { it.name })
    }
}
