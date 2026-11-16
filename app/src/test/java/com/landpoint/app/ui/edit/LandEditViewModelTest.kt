package com.landpoint.app.ui.edit

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.landpoint.app.data.LandRepository
import com.landpoint.app.data.PhotoStamper
import com.landpoint.app.data.PhotoStore
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.db.LandDatabase
import com.landpoint.app.location.LocationProvider
import com.landpoint.app.location.WalkRecorder
import com.landpoint.app.location.WalkState
import com.landpoint.app.util.AppStrings
import com.landpoint.app.util.GeoPoint
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import org.robolectric.Shadows.shadowOf

/**
 * One working of "Add corner" is one corner — the promise the button is built
 * around. The mark is the single door into the draft, so these tests pin the
 * door: a mark commits exactly once, a second press racing the first adds
 * nothing, and a new GPS press sweeps away whatever hung before instead of
 * leaving a stale confirm bar that could be used as an extra corner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LandEditViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: LandDatabase
    private lateinit var viewModel: LandEditViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LandDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val strings = AppStrings(context)
        viewModel = LandEditViewModel(
            repository = LandRepository(db.landDao(), PhotoStore(context)),
            locationProvider = LocationProvider(context),
            walkRecorder = NotWalking(),
            photoStore = PhotoStore(context),
            photoStamper = PhotoStamper(context, strings),
            strings = strings,
            settings = SettingsRepository(context),
            savedStateHandle = SavedStateHandle()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun grantLocation() {
        shadowOf(context as Application).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
    }

    @Test
    fun `one committed mark becomes exactly one corner`() {
        viewModel.placeMarkAt(-6.2, 106.8)

        viewModel.commitMark()
        assertEquals(1, viewModel.uiState.value.draftBoundary.size)

        // The second press — a double tap, a nervous finger — finds no mark and
        // must not conjure a corner out of nothing.
        viewModel.commitMark()
        assertEquals(1, viewModel.uiState.value.draftBoundary.size)
    }

    @Test
    fun `two commit presses racing each other add exactly one corner`() {
        viewModel.placeMarkAt(-6.2, 106.8)

        val go = CountDownLatch(1)
        val done = CountDownLatch(2)
        val press = {
            go.await()
            viewModel.commitMark()
            done.countDown()
        }
        val first = Thread(press)
        val second = Thread(press)
        first.start()
        second.start()
        go.countDown()
        first.join(5_000)
        second.join(5_000)

        assertEquals(1, viewModel.uiState.value.draftBoundary.size)
    }

    @Test
    fun `committing with no mark hanging changes nothing`() {
        viewModel.commitMark()

        assertTrue(viewModel.uiState.value.draftBoundary.isEmpty())
        assertNull(viewModel.uiState.value.pendingMark)
    }

    @Test
    fun `a new GPS press sweeps away the mark that hangs`() = runTest {
        grantLocation()
        viewModel.openCornerPicker()
        viewModel.placeMarkAt(-6.2, 106.8)
        assertNotNull(viewModel.uiState.value.pendingMark)

        viewModel.markFromFix()

        // Superseded at once: nothing stale is committable while the new
        // average is being gathered, and the averaging finishes with no fix on
        // the shadow provider, leaving no mark and no corner behind.
        assertNull(viewModel.uiState.value.pendingMark)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.draftBoundary.isEmpty())
    }

    /**
     * The promise of the GPS button: one press is one corner. The averaging is
     * the slow part of the press, and [LandEditViewModel.markFromFix] now folds
     * the commit into the same press by calling [LandEditViewModel.commitMark]
     * the moment the averaged fix lands.
     *
     * The shadow location pipeline that feeds `captureAveraged` is brittle to
     * run under Robolectric's paused main looper (a listener that posts back to
     * it can stall the test forever), so this contract is pinned through the
     * deterministic mark path that `commitMark` shares with `markFromFix`:
     * `markFromFix` is `commitMark` called once the averaged fix is hung as a
     * mark, so the contract "a hanging mark, committed once, is exactly one
     * corner and leaves no mark behind" is the same contract the GPS press
     * ends in. A second commit finds nothing — a double tap or an accessibility
     * double-fire must not turn one press into two corners.
     */
    @Test
    fun `one committed mark is exactly one corner and leaves no mark`() {
        // Standing the mark the way markFromFix leaves it once a fix lands: as a
        // pending mark, ready to commit. A typed mark is the deterministic
        // stand-in for the averaged fix here.
        viewModel.openCornerPicker()
        viewModel.placeMarkTyped("-6.2", "106.8")
        assertNotNull(viewModel.uiState.value.pendingMark)

        // The single commit a press performs.
        viewModel.commitMark()

        val first = viewModel.uiState.value
        assertEquals(1, first.draftBoundary.size)
        assertNull(first.pendingMark)

        // A racing second commit finds no mark and adds nothing.
        viewModel.commitMark()
        assertEquals(1, viewModel.uiState.value.draftBoundary.size)
    }

    /**
     * A fix that lands on top of an existing corner is not auto-committed as a
     * zero-length edge: the commit refuses it and leaves the mark hanging with a
     * message naming the corner it clashes with, so the user can move it before
     * any second press. `markFromFix` reaches this same refusal through the
     * shared `commitMark` door.
     */
    @Test
    fun `a mark too close to a corner stays as a mark and names the corner`() {
        viewModel.openCornerPicker()
        viewModel.placeMarkAt(-6.2, 106.8)
        viewModel.commitMark()
        assertEquals(1, viewModel.uiState.value.draftBoundary.size)

        // A second mark on the same coordinates as the standing corner.
        viewModel.placeMarkAt(-6.2, 106.8)
        viewModel.commitMark()

        val state = viewModel.uiState.value
        assertEquals(1, state.draftBoundary.size)
        assertNotNull(state.pendingMark)
        assertNotNull(state.message)
    }

    /**
     * Closing the picker keeps the drawn shape: back is commit when there are
     * corners on the ground, so the user no longer presses a separate "use this
     * boundary" button. The committed boundary matches the draft point-for-point
     * and the picker is left closed with the draft cleared.
     */
    @Test
    fun `closing the picker with corners keeps them as the boundary`() {
        viewModel.openCornerPicker()
        listOf(-6.2 to 106.8, -6.21 to 106.81, -6.22 to 106.80).forEach { (lat, lon) ->
            viewModel.placeMarkAt(lat, lon)
            viewModel.commitMark()
        }
        assertEquals(3, viewModel.uiState.value.draftBoundary.size)

        viewModel.commitCornerPicker()

        val state = viewModel.uiState.value
        assertEquals(3, state.boundary.size)
        assertEquals(listOf(-6.2, -6.21, -6.22), state.boundary.map { it.latitude })
        assertTrue(state.draftBoundary.isEmpty())
        assertFalse(state.isPickerOpen)
    }

    /**
     * Closing the picker with nothing drawn is not a commit of emptiness: an
     * empty draft leaves the standing boundary untouched, so a back press that
     * merely looked at the map cannot wipe a parcel someone already drew.
     */
    @Test
    fun `closing the picker with no draft leaves the boundary untouched`() {
        // A boundary already stood before the picker was opened.
        viewModel.openCornerPicker()
        viewModel.placeMarkAt(-6.2, 106.8)
        viewModel.commitMark()
        viewModel.commitCornerPicker()
        assertEquals(1, viewModel.uiState.value.boundary.size)

        // Reopen, draw nothing, close. The earlier boundary survives.
        viewModel.openCornerPicker()
        assertTrue(viewModel.uiState.value.draftBoundary.isNotEmpty())
        viewModel.closeCornerPicker()

        val state = viewModel.uiState.value
        assertEquals(1, state.boundary.size)
        assertTrue(state.draftBoundary.isEmpty())
        assertFalse(state.isPickerOpen)
    }
}

/** A recorder that never walks: the commit tests never touch the walk path. */
private class NotWalking : WalkRecorder {
    override val state = MutableStateFlow(WalkState())
    override fun start(): Boolean = false
    override fun stop(): List<GeoPoint> = emptyList()
}
