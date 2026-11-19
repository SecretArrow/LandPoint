package com.landpoint.app.ui.screenshot

import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.toCorner
import com.landpoint.app.ui.preview.LandPreviewScreen
import com.landpoint.app.ui.preview.PreviewDraftStore
import com.landpoint.app.ui.theme.LandPointTheme
import com.landpoint.app.util.GeoPoint
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The parcel preview — the redesign's flagship screen — with a real draft
 * seeded into PreviewDraftStore: hero card, corner cards and the quick
 * actions column over the map.
 *
 * The PREVIEW_ACTIONS coach mark reads its "seen" flag from DataStore
 * asynchronously. If the flag were left unset the coach could appear on the
 * frame one run captures and be gone the next — a verify flake that looks
 * like a UI regression — so the flag is written before the first frame.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreviewScreenshotTest {

    @Test
    @Config(sdk = [34], qualifiers = SCREENSHOT_QUALIFIERS)
    fun previewLightEnglish() {
        capture("preview_light_en.png")
    }

    @Test
    @Config(sdk = [34], qualifiers = "$SCREENSHOT_QUALIFIERS-night")
    fun previewDarkEnglish() {
        capture("preview_dark_en.png")
    }

    private fun capture(fileName: String) {
        settleAnimations()
        runBlocking {
            SettingsRepository(ApplicationProvider.getApplicationContext())
                .setCoachMarkSeen(SettingsRepository.CoachMark.PREVIEW_ACTIONS)
        }
        PreviewDraftStore.set(
            name = "Kebun Percobaan",
            corners = shapeCorners().map { it.toCorner() },
            photoCounts = emptyMap()
        )
        captureRoboImage("$SCREENSHOT_DIR$fileName") {
            LandPointTheme {
                LandPreviewScreen(
                    onBackToEdit = {},
                    onSave = {},
                    onDiscard = {},
                    areaUnit = SettingsRepository.AreaUnit.SQM
                )
            }
        }
        PreviewDraftStore.clear()
    }

    private fun shapeCorners(): List<GeoPoint> = listOf(
        GeoPoint(-6.175, 106.827),
        GeoPoint(-6.1755, 106.8285),
        GeoPoint(-6.1762, 106.8278),
        GeoPoint(-6.1758, 106.8265)
    )
}
