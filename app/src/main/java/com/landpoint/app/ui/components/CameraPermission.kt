package com.landpoint.app.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * True when the user has granted the CAMERA permission.
 *
 * The camera intent itself does not strictly require CAMERA on stock Android —
 * `ACTION_IMAGE_CAPTURE` is open to any caller — but a number of manufacturer
 * camera apps (Samsung, Xiaomi, OPPO among others) refuse to start for a caller
 * that does not hold it, throwing `SecurityException` or
 * `ActivityNotFoundException` from `launch`. That throw is what surfaced as the
 * "no camera app is available" message, so the permission has to be asked for
 * at runtime the same way location is.
 */
fun Context.hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * Whether the CAMERA permission is granted, whether the system will still ask,
 * and a trigger for the request. Mirrors [rememberLocationPermission] so the
 * two permissions behave the same way from a screen's point of view.
 *
 * Once the user has refused twice (or once with "don't allow" on Android 11+),
 * launching the request does nothing visible, so the caller must be told to
 * point at the system settings page instead — exactly as with location.
 */
@Composable
fun rememberCameraPermission(onGranted: () -> Unit = {}): CameraPermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val granted0 = context.hasCameraPermission()
    var granted by remember(granted0) { mutableStateOf(granted0) }
    // Survives rotation: it is what separates "never asked" from "asked and
    // refused for good", and the platform cannot tell us apart.
    var asked by rememberSaveable { mutableStateOf(false) }
    val latestOnGranted by rememberUpdatedState(onGranted)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission granted in the system settings is granted in another app, and
    // we are told nothing about it. Coming back to the front is the only signal.
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                !granted &&
                context.hasCameraPermission()
            ) {
                granted = true
                latestOnGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        asked = true
        granted = isGranted
        if (isGranted) latestOnGranted()
    }

    // False both before the first ask and after a permanent refusal, which is why
    // it is only trusted once we know an ask has happened.
    val blocked = !granted &&
        asked &&
        activity != null &&
        !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

    return remember(granted, blocked) {
        CameraPermissionState(
            isGranted = granted,
            isBlocked = blocked,
            request = { launcher.launch(Manifest.permission.CAMERA) }
        )
    }
}

data class CameraPermissionState(
    val isGranted: Boolean,
    /** The system will not ask again; only its settings page can grant it now. */
    val isBlocked: Boolean = false,
    val request: () -> Unit
)

// The Activity is not LocalContext here — the theme wraps it to switch language
// — so unwrap until it turns up. Null where there is none, as in a preview.
// Duplicated from LocationPermission.kt rather than shared because the two files
// are deliberately independent: either can be read in isolation.
private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
