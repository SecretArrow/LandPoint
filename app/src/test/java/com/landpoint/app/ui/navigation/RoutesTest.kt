package com.landpoint.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The route builders are the one place a destination string is assembled, and
 * the Navigation matcher matches them against the pattern constants. A builder
 * that drifts from its pattern produces a destination nothing can open, so
 * each one is checked against the constant it must satisfy.
 */
class RoutesTest {

    @Test
    fun `land destinations build from the id`() {
        assertEquals("land/abc123", Routes.detail("abc123"))
        assertEquals("land/abc123/edit", Routes.edit("abc123"))
        assertEquals("land/abc123/compass", Routes.compass("abc123"))
        assertEquals("land/abc123/shape", Routes.shape("abc123"))
    }

    @Test
    fun `owner destinations build from the id`() {
        assertEquals("owner/o42", Routes.ownerDetail("o42"))
        assertEquals("owner/o42/edit", Routes.ownerEdit("o42"))
    }

    @Test
    fun `every builder fills the placeholder of its pattern constant`() {
        assertEquals(Routes.DETAIL.replace("{landId}", "x"), Routes.detail("x"))
        assertEquals(Routes.EDIT.replace("{landId}", "x"), Routes.edit("x"))
        assertEquals(Routes.COMPASS.replace("{landId}", "x"), Routes.compass("x"))
        assertEquals(Routes.SHAPE.replace("{landId}", "x"), Routes.shape("x"))
        assertEquals(Routes.OWNER_DETAIL.replace("{ownerId}", "x"), Routes.ownerDetail("x"))
        assertEquals(Routes.OWNER_EDIT.replace("{ownerId}", "x"), Routes.ownerEdit("x"))
    }
}
