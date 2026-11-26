package com.landpoint.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * The fold on the window this composition is drawn into, or [FoldState.FLAT]
 * where there is none to mind about.
 *
 * Read from the Activity's window with [WindowInfoTracker], because the posture
 * is the hardware's arrangement, not the composition's: it does not change
 * because a screen did, and it outlives every screen. Collected with
 * [collectAsStateWithLifecycle], so the subscription runs only while the app is
 * on screen and is torn down with the composition — a posture stream followed in
 * the background would spend battery answering a screen that is not there.
 *
 * Everything that can fail is answered with flat rather than fatal. A device
 * without the OEM window extensions — most phones, and every emulator the e2e
 * flows run on — reports a window with no features, which is [FoldState.FLAT].
 * A tracker that cannot be built, or an OEM stub that throws mid-stream, is
 * caught and answered with the same reading: no fold-aware code path may crash
 * a device for reporting its hardware oddly.
 */
@Composable
fun rememberWindowInfo(): FoldState {
    val activity = LocalContext.current.findActivity() ?: return FoldState.FLAT

    val foldFlow = remember(activity) {
        // Building the stream can itself fail where the window library cannot
        // start (an OEM stub class missing under it); an exception mid-stream is
        // the other shape of the same problem. Both end at the flat reading.
        try {
            WindowInfoTracker.getOrCreate(activity)
                .windowLayoutInfo(activity)
                .map { it.toFoldState() }
                .catch { emit(FoldState.FLAT) }
        } catch (_: Throwable) {
            flowOf(FoldState.FLAT)
        }
    }
    return foldFlow.collectAsStateWithLifecycle(initialValue = FoldState.FLAT).value
}

/**
 * The platform's reading of the fold, in this app's terms.
 *
 * One hinge is all a real window reports; the other display features a device
 * may name (screen cutouts and the like) are not folds and are ignored. A hinge
 * separates the window only while the device is stood on it
 * ([FoldingFeature.State.HALF_OPENED]) or the two halves are physically apart
 * ([FoldingFeature.State.OCCLUDED]) — opened flat the device is one plane, and
 * one plane is no fold, which is also what a hinge reported as
 * [FoldingFeature.State.FLAT] is saying.
 */
private fun WindowLayoutInfo.toFoldState(): FoldState {
    val hinge = displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull() ?: return FoldState.FLAT
    return FoldState(
        isSeparating = hinge.state != FoldingFeature.State.FLAT,
        hingeRunsHorizontally = hinge.orientation == FoldingFeature.Orientation.HORIZONTAL,
        foldTopY = hinge.bounds.top,
        foldBottomY = hinge.bounds.bottom
    )
}

/**
 * The Activity whose window this composition is drawn into, or null where there
 * is none — a preview surface, a tile in the component gallery. A fold is a
 * property of a window, so a composition with no window behind it has no fold
 * to report.
 */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext?.findActivity()
    else -> null
}
