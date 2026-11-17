package com.landpoint.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The coach-mark contract (docs/IMPLEMENTATION_PLAN.md, Phase 4): every mark
 * starts unseen, dismissing one never touches the others, and the record is
 * whatever was last written.
 *
 * Each test gets its own DataStore file through the repository's injectable
 * store parameter, so nothing here shares state with the production singleton
 * or with another test's file.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryCoachMarkTest {

    private val testScope = CoroutineScope(Job())
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "coachmark-test-${System.nanoTime()}.preferences_pb")
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { file }
        )
        settings = SettingsRepository(context, dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `a mark starts unseen`() = runBlocking {
        SettingsRepository.CoachMark.entries.forEach { mark ->
            assertFalse(settings.coachMarkSeen(mark).first())
        }
    }

    @Test
    fun `dismissing one mark leaves the others unseen`() = runBlocking {
        settings.setCoachMarkSeen(SettingsRepository.CoachMark.PICKER_SOURCES)

        assertTrue(settings.coachMarkSeen(SettingsRepository.CoachMark.PICKER_SOURCES).first())
        assertFalse(settings.coachMarkSeen(SettingsRepository.CoachMark.MAP_TOOLBAR).first())
        assertFalse(settings.coachMarkSeen(SettingsRepository.CoachMark.PREVIEW_ACTIONS).first())
    }

    @Test
    fun `a dismissal survives a later read`() = runBlocking {
        settings.setCoachMarkSeen(SettingsRepository.CoachMark.MAP_TOOLBAR)

        assertEquals(true, settings.coachMarkSeen(SettingsRepository.CoachMark.MAP_TOOLBAR).first())
        assertEquals(true, settings.coachMarkSeen(SettingsRepository.CoachMark.MAP_TOOLBAR).first())
    }

    @Test
    fun `a mark can be marked unseen again`() = runBlocking {
        val mark = SettingsRepository.CoachMark.PREVIEW_ACTIONS
        settings.setCoachMarkSeen(mark)
        settings.setCoachMarkSeen(mark, seen = false)

        assertFalse(settings.coachMarkSeen(mark).first())
    }

    @Test
    fun `every mark persists under its own key`() {
        val keys = SettingsRepository.CoachMark.entries.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }
}
