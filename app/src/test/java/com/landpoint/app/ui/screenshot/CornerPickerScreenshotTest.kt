package com.landpoint.app.ui.screenshot

import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.ui.edit.CornerPickerScreen
import com.landpoint.app.ui.edit.DraftCorner
import com.landpoint.app.ui.edit.LandEditUiState
import com.landpoint.app.ui.theme.LandPointTheme
import com.landpoint.app.util.GeoPoint
import com.landpoint.app.util.MarkSource
import com.landpoint.app.util.PendingMark
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The corner picker, captured in its most decision-heavy state: four corners
 * in the draft and a mark hanging on the map waiting to be confirmed, so the
 * capture gates the confirm bar, the mark's placement line and the map chrome
 * together.
 *
 * The map itself draws its JVM fallback (MapLibreInit.isAvailable is false in
 * tests, so LandMap renders the "no renderer" message instead of tiles):
 * tile rendering needs the emulator and stays with the Maestro e2e job.
 *
 * The PICKER_SOURCES coach mark reads its "seen" flag from DataStore
 * asynchronously — like the preview's coach mark, it must be marked seen
 * before the first frame or the capture could land on either side of the
 * async read (a verify flake that looks like a UI regression).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CornerPickerScreenshotTest {

    @Test
    @Config(sdk = [34], qualifiers = SCREENSHOT_QUALIFIERS)
    fun pickerWithPendingMarkLightEnglish() {
        capture("corner_picker_light_en.png")
    }

    @Test
    @Config(sdk = [34], qualifiers = "in-$SCREENSHOT_QUALIFIERS")
    fun pickerWithPendingMarkLightIndonesian() {
        capture("corner_picker_light_in.png")
    }

    @Test
    @Config(sdk = [34], qualifiers = "$SCREENSHOT_QUALIFIERS-night")
    fun pickerWithPendingMarkDarkEnglish() {
        capture("corner_picker_dark_en.png")
    }

    @Test
    @Config(sdk = [34], qualifiers = "in-$SCREENSHOT_QUALIFIERS-night")
    fun pickerWithPendingMarkDarkIndonesian() {
        capture("corner_picker_dark_in.png")
    }

    private fun capture(fileName: String) {
        settleAnimations()
        runBlocking {
            SettingsRepository(ApplicationProvider.getApplicationContext())
                .setCoachMarkSeen(SettingsRepository.CoachMark.PICKER_SOURCES)
        }
        captureRoboImage("$SCREENSHOT_DIR$fileName") {
            LandPointTheme {
                CornerPickerScreen(
                    state = pickerState(),
                    currentLocation = GeoPoint(-6.1751, 106.8271, 2.5),
                    onMessageShown = {},
                    onHintShown = {},
                    onPlaceMark = { _, _, _ -> },
                    onMoveMark = { _, _, _ -> },
                    onNudgeMark = {},
                    onMarkStep = {},
                    onMarkFromFix = {},
                    onMarkTyped = { _, _, _ -> null },
                    onMarkFromBearing = { _, _, _ -> null },
                    onUseMark = {},
                    onDiscardMark = {},
                    onLoadNeighbours = {},
                    onTakeFromNeighbour = { _, _ -> },
                    onMoveCorner = { _, _, _ -> },
                    onSelectCorner = {},
                    onDeleteSelected = {},
                    onDeleteCorner = {},
                    onUndoDelete = {},
                    onUndo = {},
                    onClear = {},
                    onDone = {},
                    onCancel = {}
                )
            }
        }
    }

    /**
     * Four drafted corners plus a pending mark measured out from corner 2 —
     * the state a user is in when they must judge "corner 5 or insert
     * between?" before committing.
     */
    private fun pickerState(): LandEditUiState {
        val corners = shapeCorners()
        return LandEditUiState(
            name = "Kebun Percobaan",
            isPickerOpen = true,
            boundary = corners,
            cornerIds = corners.mapIndexed { index, _ -> "c${index + 1}" },
            draftBoundary = corners.mapIndexed { index, point ->
                DraftCorner(id = "c${index + 1}", point = point)
            },
            pendingMark = PendingMark(
                point = GeoPoint(-6.1753, 106.8276),
                source = MarkSource.MAP,
                after = 1
            )
        )
    }

    private fun shapeCorners(): List<GeoPoint> = listOf(
        GeoPoint(-6.175, 106.827),
        GeoPoint(-6.1755, 106.8285),
        GeoPoint(-6.1762, 106.8278),
        GeoPoint(-6.1758, 106.8265)
    )
}
