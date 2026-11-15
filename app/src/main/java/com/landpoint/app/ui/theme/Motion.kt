package com.landpoint.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import kotlin.math.roundToInt

/**
 * Duration and easing tokens (docs/DESIGN_SYSTEM.md §2.6).
 *
 * Today's animations are Compose defaults and a couple of ad-hoc
 * `animate*AsState` calls; these tokens give every new animation the same
 * clock. Durations are milliseconds; easings are
 * [androidx.compose.animation.core.Easing] values.
 */
// The durations are named after docs/DESIGN_SYSTEM.md §2.6 — `Motion.slow` is
// the token the docs spell — so they stay camelCase vals rather than the
// SCREAMING_SNAKE consts ktlint's property-naming would demand. detekt's
// MayBeConst is suppressed for the same reason. Spacing.kt keeps the same
// token style; its Dp values simply cannot be consts either.
@Suppress("MayBeConst")
object Motion {

    // ---- Durations (ms) ----

    /** State swaps with no animation. */
    val instant = 0

    /** Ripple, chip selection, press feedback. */
    val fast = 100

    /** Sheet slide, FAB show/hide. */
    val standard = 200

    /**
     * Polygon draw-in on the parcel preview hero. The one duration a user
     * watches for a full beat, so it is a third of a second and no more.
     */
    val slow = 350

    /** Hero header collapse, page transition. */
    val emphasized = 250

    /** Scrim fade behind dialogs. */
    val scrim = 150

    // ---- Easings ----

    /** The Material standard curve; backs fast, standard and slow. */
    val standardEasing: Easing = FastOutSlowInEasing

    /** The emphasized curve; decelerates hard into the resting position. */
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Straight-line fade; backs the scrim. */
    val linearEasing: Easing = LinearEasing

    /**
     * Generic tween token: [Motion.tweenSpec] with a duration and an easing,
     * for the odd animation that does not sit on the named rows above.
     */
    fun <T> tweenSpec(durationMillis: Int, easing: Easing = standardEasing): TweenSpec<T> =
        TweenSpec(durationMillis = durationMillis, easing = easing)

    /** [Motion.fast] tween — ripple, chip selection, press feedback. */
    fun <T> tweenFast(): TweenSpec<T> = tweenSpec(fast)

    /** [Motion.standard] tween — sheet slide, FAB show/hide. */
    fun <T> tweenStandard(): TweenSpec<T> = tweenSpec(standard)

    /** [Motion.slow] tween — the polygon draw-in. */
    fun <T> tweenSlow(): TweenSpec<T> = tweenSpec(slow)

    /** [Motion.emphasized] tween with its own curve — hero collapse, pages. */
    fun <T> tweenEmphasized(): TweenSpec<T> = tweenSpec(emphasized, emphasizedEasing)

    /** [Motion.scrim] tween with a linear curve — scrim fades never ease. */
    fun <T> tweenScrim(): TweenSpec<T> = tweenSpec(scrim, linearEasing)

    /**
     * Applies the system animator scale (docs/DESIGN_SYSTEM.md §2.6,
     * reduced motion). A scale of 0 means the user has disabled animations —
     * any duration collapses to [instant]; otherwise the duration stretches
     * or shrinks proportionally, exactly as the platform would.
     */
    fun scaledDuration(durationMillis: Int, animatorScale: Float): Int {
        if (animatorScale <= 0f) return instant
        return (durationMillis * animatorScale).roundToInt().coerceAtLeast(0)
    }
}
