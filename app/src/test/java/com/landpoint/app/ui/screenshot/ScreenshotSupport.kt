package com.landpoint.app.ui.screenshot

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider

/**
 * Every capture is a Pixel-class phone viewport (411×891 dp) so images from
 * different tests, machines and runs land on the same grid before they are
 * compared. The qualifiers string is written in Android's canonical order —
 * locale before width before height before night — because Robolectric parses
 * it as a resource-configuration name.
 */
const val SCREENSHOT_QUALIFIERS = "w411dp-h891dp"

/**
 * Where the baselines live: inside the source tree, next to the code they
 * gate, so a PR that changes what a screen looks like shows the pixel diff
 * as a normal file change. Paths passed to Roborazzi resolve against the
 * module directory, which is app/ here.
 */
const val SCREENSHOT_DIR = "src/test/screenshots/"

/**
 * Freezes every animation clock before content is composed.
 *
 * A screenshot must be the settled frame, never an arbitrary frame of a
 * draw-in: with the animator scale at 0 the parcel hero snaps straight to its
 * finished polygon and every other animation ends before the first capture.
 * This is the "reduced motion" path the app itself honours, read by
 * ParcelHero.readAnimatorScale.
 */
fun settleAnimations() {
    Settings.Global.putFloat(
        ApplicationProvider.getApplicationContext<Context>().contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        0f
    )
}
