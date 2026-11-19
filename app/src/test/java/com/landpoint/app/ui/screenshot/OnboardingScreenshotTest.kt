package com.landpoint.app.ui.screenshot

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.landpoint.app.ui.onboarding.OnboardingScreen
import com.landpoint.app.ui.theme.LandPointTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The introduction's first page — the welcome step with the stepper, the Skip
 * door and the Next button. The following steps carry permission-state text
 * that depends on the Robolectric grant model rather than on a real dialog,
 * so only the step that looks the same on every device is gated here; the
 * permission flow itself is exercised by the Maestro e2e suite.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenshotTest {

    @Test
    @Config(sdk = [34], qualifiers = SCREENSHOT_QUALIFIERS)
    fun onboardingWelcomeLightEnglish() {
        capture("onboarding_light_en.png") { OnboardingScreen(onFinish = {}) }
    }

    @Test
    @Config(sdk = [34], qualifiers = "in-$SCREENSHOT_QUALIFIERS")
    fun onboardingWelcomeLightIndonesian() {
        capture("onboarding_light_in.png") { OnboardingScreen(onFinish = {}) }
    }

    @Test
    @Config(sdk = [34], qualifiers = "$SCREENSHOT_QUALIFIERS-night")
    fun onboardingWelcomeDarkEnglish() {
        capture("onboarding_dark_en.png") { OnboardingScreen(onFinish = {}) }
    }

    @Test
    @Config(sdk = [34], qualifiers = "in-$SCREENSHOT_QUALIFIERS-night")
    fun onboardingWelcomeDarkIndonesian() {
        capture("onboarding_dark_in.png") { OnboardingScreen(onFinish = {}) }
    }

    private fun capture(fileName: String, content: @Composable () -> Unit) {
        settleAnimations()
        captureRoboImage("$SCREENSHOT_DIR$fileName") {
            LandPointTheme { content() }
        }
    }
}
