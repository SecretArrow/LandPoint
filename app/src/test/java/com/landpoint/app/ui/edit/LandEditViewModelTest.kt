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
}

/** A recorder that never walks: the commit tests never touch the walk path. */
private class NotWalking : WalkRecorder {
    override val state = MutableStateFlow(WalkState())
    override fun start(): Boolean = false
    override fun stop(): List<GeoPoint> = emptyList()
}
