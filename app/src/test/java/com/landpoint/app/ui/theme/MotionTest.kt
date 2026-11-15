package com.landpoint.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the duration tokens and the reduced-motion helper. The polygon
 * draw-in and every later animation read these, so a drift here is a drift
 * in the whole app's clock.
 */
class MotionTest {

    @Test
    fun `durations match the design system table`() {
        assertEquals(0, Motion.instant)
        assertEquals(100, Motion.fast)
        assertEquals(200, Motion.standard)
        assertEquals(350, Motion.slow)
        assertEquals(250, Motion.emphasized)
        assertEquals(150, Motion.scrim)
    }

    @Test
    fun `tween helpers carry their named durations`() {
        assertEquals(Motion.fast, Motion.tweenFast<Float>().durationMillis)
        assertEquals(Motion.standard, Motion.tweenStandard<Float>().durationMillis)
        assertEquals(Motion.slow, Motion.tweenSlow<Float>().durationMillis)
        assertEquals(Motion.scrim, Motion.tweenScrim<Float>().durationMillis)
    }

    @Test
    fun `emphasized tween uses the emphasized curve`() {
        assertEquals(Motion.emphasized, Motion.tweenEmphasized<Float>().durationMillis)
        assertEquals(Motion.emphasizedEasing, Motion.tweenEmphasized<Float>().easing)
    }

    @Test
    fun `scrim tween never eases`() {
        assertEquals(Motion.linearEasing, Motion.tweenScrim<Float>().easing)
    }

    @Test
    fun `a full animator scale leaves durations alone`() {
        assertEquals(350, Motion.scaledDuration(350, 1f))
    }

    @Test
    fun `a zero animator scale is reduced motion and collapses to instant`() {
        assertEquals(Motion.instant, Motion.scaledDuration(350, 0f))
    }

    @Test
    fun `a fractional animator scale scales proportionally`() {
        assertEquals(175, Motion.scaledDuration(350, 0.5f))
        assertEquals(700, Motion.scaledDuration(350, 2f))
    }

    @Test
    fun `a negative scale is treated as disabled`() {
        assertEquals(Motion.instant, Motion.scaledDuration(350, -1f))
    }
}
