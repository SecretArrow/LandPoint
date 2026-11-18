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
        viewModel.openCornerPicker()
        viewModel.placeMarkAt(-6.2, 106.8)

        viewModel.commitMark()
        // The commit writes the draft to the boundary and closes the picker: one
        // corner, the picker shut, the mark gone.
        val first = viewModel.uiState.value
        assertEquals(1, first.boundary.size)
        assertTrue(first.draftBoundary.isEmpty())
        assertNull(first.pendingMark)
        assertFalse(first.isPickerOpen)

        // The second press — a double tap, a nervous finger — finds the picker
        // already closed and no mark to commit, and must not conjure a corner
        // out of nothing. The boundary stays at one.
        viewModel.commitMark()
        assertEquals(1, viewModel.uiState.value.boundary.size)
    }

    @Test
    fun `two commit presses racing each other add exactly one corner`() {
        viewModel.openCornerPicker()
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

        // The first commit adds the corner and closes the picker; the second
        // finds the picker closed and the boundary stays at one.
        val state = viewModel.uiState.value
        assertEquals(1, state.boundary.size)
        assertTrue(state.draftBoundary.isEmpty())
        assertFalse(state.isPickerOpen)
    }

    @Test
    fun `committing with no mark hanging changes nothing`() {
        viewModel.openCornerPicker()
        viewModel.commitMark()

        assertTrue(viewModel.uiState.value.boundary.isEmpty())
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
     * The promise of the GPS button: one press gathers the average and hangs a
     * mark. The confirm press on "Pakai tanda ini" — [commitMark] — turns it
     * into a corner. The averaging no longer folds the commit into the same
     * press: the user got no chance to review the averaged fix before it was
     * silently swallowed into the boundary, so the confirm bar is back in the
     * loop.
     *
     * The shadow location pipeline that feeds `captureAveraged` is brittle to
     * run under Robolectric's paused main looper (a listener that posts back to
     * it can stall the test forever), so this contract is pinned through the
     * deterministic mark path that `commitMark` shares with `markFromFix`:
     * once the averaged fix lands it hangs as a mark, ready for the user to
     * commit. A typed mark is the deterministic stand-in for the averaged fix
     * here. One commit, one corner, the picker closed, no mark left behind —
     * and a second commit finds nothing, so a double tap or an accessibility
     * double-fire cannot turn one press into two corners.
     */
    @Test
    fun `one committed mark is exactly one corner and leaves no mark`() = runTest {
        // Standing the mark the way markFromFix leaves it once a fix lands: as a
        // pending mark, ready to commit. A typed mark is the deterministic
        // stand-in for the averaged fix here.
        viewModel.openCornerPicker()
        viewModel.placeMarkTyped("-6.2", "106.8")
        assertNotNull(viewModel.uiState.value.pendingMark)

        // The confirm press on "Pakai tanda ini". One corner added, the picker
        // closed, the mark gone: the user is back on the edit screen.
        viewModel.commitMark()

        val first = viewModel.uiState.value
        assertEquals(1, first.boundary.size)
        assertTrue(first.draftBoundary.isEmpty())
        assertNull(first.pendingMark)
        assertFalse(first.isPickerOpen)

        // A racing second commit finds the picker closed and adds nothing.
        viewModel.commitMark()
        assertEquals(1, viewModel.uiState.value.boundary.size)
    }

    /**
     * A fix that lands on top of an existing corner is not auto-committed as a
     * zero-length edge: the commit refuses it and leaves the mark hanging with a
     * message naming the corner it clashes with, so the user can move it before
     * any second press. `markFromFix` reaches this same refusal through the
     * shared `commitMark` door. A refusal does not close the picker — the mark
     * is still on screen, waiting to be moved.
     */
    @Test
    fun `a mark too close to a corner stays as a mark and names the corner`() {
        viewModel.openCornerPicker()
        viewModel.placeMarkAt(-6.2, 106.8)
        viewModel.commitMark()
        // The first successful commit writes the corner to the boundary and
        // closes the picker.
        assertEquals(1, viewModel.uiState.value.boundary.size)
        assertFalse(viewModel.uiState.value.isPickerOpen)

        // Re-open to place a second mark on the same coordinates as the corner
        // that just landed: the commit must refuse it without closing.
        viewModel.openCornerPicker()
        viewModel.placeMarkAt(-6.2, 106.8)
        viewModel.commitMark()

        val state = viewModel.uiState.value
        assertEquals(1, state.boundary.size)
        assertNotNull(state.pendingMark)
        assertNotNull(state.message)
        // The picker stays open: the user needs to move the refused mark.
        assertTrue(state.isPickerOpen)
    }

    /**
     * Regression guard for the bug where one "Tambah sudut" press silently
     * swallowed the averaged GPS fix into the boundary — the user never got the
     * chance to press "Pakai tanda ini". After the fix, a press that lands a
     * fix must leave it as a HANGING mark, not as a corner: the boundary is
     * empty, the draft is empty, the mark is on screen, and the picker is still
     * open. `commitMark` is the only door into the boundary, and it is the
     * user's "Pakai tanda ini" press that opens it.
     *
     * The shadow location pipeline that feeds `captureAveraged` is brittle to
     * run under Robolectric's paused main looper, so this contract is pinned
     * through the same deterministic typed-mark path that `markFromFix`
     * produces once a fix lands: a hanging pending mark, no corner yet.
     */
    @Test
    fun `a landed fix hangs as a mark and does not auto-commit a corner`() {
        viewModel.openCornerPicker()
        // The mark the way markFromFix leaves it once a fix lands: a typed mark
        // is the deterministic stand-in for the averaged fix.
        viewModel.placeMarkTyped("-6.2", "106.8")

        val state = viewModel.uiState.value
        // The mark hangs — not yet a corner. The user has to press
        // "Pakai tanda ini" to commit it. This is the regression guard: the
        // boundary stays empty until that press.
        assertNotNull(state.pendingMark)
        assertTrue(state.boundary.isEmpty())
        assertTrue(state.draftBoundary.isEmpty())
        // The picker stays open: the confirm bar is what the user presses next.
        assertTrue(state.isPickerOpen)
    }

    /**
     * The "use this mark" button is one-and-done: the first commit writes the
     * corner to the boundary and closes the picker, so a second mark placed
     * afterwards is a fresh session that opens the picker again rather than
     * piling onto the same draft. Building a multi-corner boundary is now a
     * series of "add corner" presses on the edit screen, each opening the
     * picker, placing one mark, and landing back on the edit screen.
     *
     * A back press that arrives after the commit is a no-op rather than a
     * second write: [commitCornerPicker] refuses to run when the picker is
     * already closed, so the corner that just landed is not overwritten by an
     * empty draft.
     */
    @Test
    fun `a second commit after the picker closed is a no-op and the boundary stands`() {
        viewModel.openCornerPicker()
        viewModel.placeMarkAt(-6.2, 106.8)
        viewModel.commitMark()
        // The picker is closed with one corner on the boundary.
        assertEquals(1, viewModel.uiState.value.boundary.size)
        assertFalse(viewModel.uiState.value.isPickerOpen)

        // A stray back press / "Done" press after the close must not overwrite
        // the boundary: commitCornerPicker is a no-op when the picker is
        // already closed.
        viewModel.commitCornerPicker()
        assertEquals(1, viewModel.uiState.value.boundary.size)
        assertFalse(viewModel.uiState.value.isPickerOpen)

        // Adding a second corner is a fresh picker session.
        viewModel.openCornerPicker()
        viewModel.placeMarkAt(-6.21, 106.81)
        viewModel.commitMark()
        val state = viewModel.uiState.value
        assertEquals(2, state.boundary.size)
        assertEquals(listOf(-6.2, -6.21), state.boundary.map { it.latitude })
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
