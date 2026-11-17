package com.landpoint.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the stepper's state mapping (docs/DESIGN_SYSTEM.md §6.4): passed steps
 * read as done, the current one as active, and everything ahead as upcoming —
 * including the "all done" reading onboarding's permission page relies on,
 * where the flow's last stop is not itself a numbered step.
 */
class StepperPositionTest {

    @Test
    fun `the first step is active on entry`() {
        assertEquals(StepPosition.ACTIVE, stepPosition(0, currentStep = 0, stepCount = 3))
        assertEquals(StepPosition.UPCOMING, stepPosition(1, currentStep = 0, stepCount = 3))
        assertEquals(StepPosition.UPCOMING, stepPosition(2, currentStep = 0, stepCount = 3))
    }

    @Test
    fun `passed steps are done, the current one active`() {
        assertEquals(StepPosition.DONE, stepPosition(0, currentStep = 1, stepCount = 3))
        assertEquals(StepPosition.ACTIVE, stepPosition(1, currentStep = 1, stepCount = 3))
        assertEquals(StepPosition.UPCOMING, stepPosition(2, currentStep = 1, stepCount = 3))
    }

    @Test
    fun `every step is done on the last numbered step's follower`() {
        assertEquals(StepPosition.DONE, stepPosition(0, currentStep = 3, stepCount = 3))
        assertEquals(StepPosition.DONE, stepPosition(1, currentStep = 3, stepCount = 3))
        assertEquals(StepPosition.DONE, stepPosition(2, currentStep = 3, stepCount = 3))
    }

    @Test
    fun `a position beyond the end is still all done`() {
        assertEquals(StepPosition.DONE, stepPosition(0, currentStep = 9, stepCount = 3))
        assertEquals(StepPosition.DONE, stepPosition(2, currentStep = 9, stepCount = 3))
    }
}
