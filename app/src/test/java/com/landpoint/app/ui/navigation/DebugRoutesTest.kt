package com.landpoint.app.ui.navigation

import com.landpoint.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gallery route is the one destination a release APK must never carry:
 * an empty route list means no navigation destination is registered and no
 * Settings row can navigate to it (docs/IMPLEMENTATION_PLAN.md, Phase 2 —
 * "debug builds only").
 */
class DebugRoutesTest {

    @Test
    fun `debug build registers exactly the gallery route`() {
        assertEquals(listOf(Routes.GALLERY), DebugRoutes.routes(debugBuild = true))
    }

    @Test
    fun `release build registers no debug routes`() {
        assertTrue(DebugRoutes.routes(debugBuild = false).isEmpty())
    }

    @Test
    fun `active routes follow the build flag of the running variant`() {
        assertEquals(DebugRoutes.routes(BuildConfig.DEBUG), DebugRoutes.active)
    }

    @Test
    fun `gallery route is the documented path`() {
        assertEquals("component_gallery", Routes.GALLERY)
    }
}
