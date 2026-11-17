package com.landpoint.app.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the introduction's walk: forward through the three read-through steps
 * into the permission ask, back out of it, and no wrapping at either end — the
 * screen's buttons decide when the introduction is over, never an overflow of
 * Next past the last stop.
 */
class OnboardingStepTest {

    @Test
    fun `forward walks the three steps into the permission ask`() {
        assertEquals(OnboardingStep.MEASURE, OnboardingStep.WELCOME.next())
        assertEquals(OnboardingStep.PRIVATE, OnboardingStep.MEASURE.next())
        assertEquals(OnboardingStep.LOCATION, OnboardingStep.PRIVATE.next())
    }

    @Test
    fun `next at the permission ask stays there`() {
        assertEquals(OnboardingStep.LOCATION, OnboardingStep.LOCATION.next())
    }

    @Test
    fun `back at the first stop stays there`() {
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.WELCOME.back())
    }

    @Test
    fun `back leaves the permission ask to the last step`() {
        assertEquals(OnboardingStep.PRIVATE, OnboardingStep.LOCATION.back())
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.MEASURE.back())
    }

    @Test
    fun `the permission ask is not a numbered step`() {
        // The stepper shows three steps; the ask is the stop after them.
        assertEquals(3, OnboardingStep.entries.size - 1)
        assertEquals(3, OnboardingStep.LOCATION.ordinal)
    }
}
