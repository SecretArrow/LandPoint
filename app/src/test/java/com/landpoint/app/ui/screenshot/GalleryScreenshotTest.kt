package com.landpoint.app.ui.screenshot

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.landpoint.app.ui.gallery.ComponentGalleryScreen
import com.landpoint.app.ui.theme.LandPointTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The gallery is the whole design system in one screen — color swatches,
 * spacing, elevation, motion, type scale, status chips — so its capture gates
 * every token at once (docs/DESIGN_SYSTEM.md §2). The screen re-themes itself
 * from DarkMode.SYSTEM, so the dark captures come straight from the `-night`
 * qualifier rather than from tapping the in-screen toggle.
 *
 * Baselines are committed under app/src/test/screenshots/; CI compares every
 * relevant PR against them (.github/workflows/screenshot.yml). Record a
 * refreshed baseline with `./gradlew recordRoborazziDebug` when the change is
 * intentional, and commit the new PNGs with the code.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GalleryScreenshotTest {

    @Test
    @Config(sdk = [34], qualifiers = SCREENSHOT_QUALIFIERS)
    fun galleryLightEnglish() {
        capture("gallery_light_en.png") { ComponentGalleryScreen(onBack = {}) }
    }

    @Test
    @Config(sdk = [34], qualifiers = "in-$SCREENSHOT_QUALIFIERS")
    fun galleryLightIndonesian() {
        capture("gallery_light_in.png") { ComponentGalleryScreen(onBack = {}) }
    }

    @Test
    @Config(sdk = [34], qualifiers = "$SCREENSHOT_QUALIFIERS-night")
    fun galleryDarkEnglish() {
        capture("gallery_dark_en.png") { ComponentGalleryScreen(onBack = {}) }
    }

    @Test
    @Config(sdk = [34], qualifiers = "in-$SCREENSHOT_QUALIFIERS-night")
    fun galleryDarkIndonesian() {
        capture("gallery_dark_in.png") { ComponentGalleryScreen(onBack = {}) }
    }

    private fun capture(fileName: String, content: @Composable () -> Unit) {
        settleAnimations()
        captureRoboImage("$SCREENSHOT_DIR$fileName") {
            LandPointTheme { content() }
        }
    }
}
