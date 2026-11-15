package com.landpoint.app.ui.gallery

import androidx.compose.ui.graphics.Color
import com.landpoint.app.ui.theme.Elevation
import com.landpoint.app.ui.theme.Green40
import com.landpoint.app.ui.theme.Green80
import com.landpoint.app.ui.theme.MapOverlayLine
import com.landpoint.app.ui.theme.MapOverlayLineDark
import com.landpoint.app.ui.theme.Motion
import com.landpoint.app.ui.theme.Spacing
import com.landpoint.app.ui.theme.Success
import com.landpoint.app.ui.theme.SuccessDark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gallery's token tables must stay in step with the theme helpers they
 * mirror (docs/DESIGN_SYSTEM.md §2): if a token is added to Color.kt or the
 * scales change order, these tests fail until the gallery is updated too.
 */
class ComponentGalleryScreenTest {

    @Test
    fun `semantic swatches pair the light and dark token values`() {
        val swatches = galleryColorSwatches()

        val success = swatches.first { it.token == "success" }
        assertEquals(Success, success.light)
        assertEquals(SuccessDark, success.dark)
    }

    @Test
    fun `map overlay swatches pair the light and dark token values`() {
        val line = galleryColorSwatches().first { it.token == "map.overlay.line" }

        assertEquals(MapOverlayLine, line.light)
        assertEquals(MapOverlayLineDark, line.dark)
    }

    @Test
    fun `semantic group precedes the map overlay group`() {
        val tokens = galleryColorSwatches().map { it.token }

        assertEquals("success", tokens.first())
        assertTrue(tokens.indexOf("accent") < tokens.indexOf("map.overlay.fill"))
        assertEquals("map.overlay.crosshair", tokens.last())
    }

    @Test
    fun `accent swatch aliases the theme primary in both modes`() {
        val accent = galleryColorSwatches().first { it.token == "accent" }

        // The same contract ParcelStatusVisualTest asserts for the theme:
        // accent is an alias of primary (Green40 light / Green80 dark), kept
        // separate so a redesign can move it independently of the brand green.
        assertEquals(Green40, accent.light)
        assertEquals(Green80, accent.dark)
    }

    @Test
    fun `hex labels are eight digit argb`() {
        assertEquals("#FF112233", Color(0xFF112233).toHexLabel())
        assertEquals("#80112233", Color(0x80112233.toInt()).toHexLabel())
        assertEquals("#FF000000", Color.Black.toHexLabel())
    }

    @Test
    fun `spacing gallery lists every token in scale order`() {
        assertEquals(
            listOf("xs", "sm", "md", "lg", "xl", "xxl", "fab"),
            gallerySpacingTokens().map { it.name }
        )
        assertEquals(Spacing.xs, gallerySpacingTokens().first().size)
    }

    @Test
    fun `elevation gallery covers E0 through E5`() {
        assertEquals(
            listOf("E0", "E1", "E2", "E3", "E4", "E5"),
            galleryElevationTokens().map { it.name }
        )
        assertEquals(Elevation.E5, galleryElevationTokens().last().level)
    }

    @Test
    fun `only the top of the elevation scale carries a shadow`() {
        val withShadow = galleryElevationTokens().filter { it.level.shadow != null }

        assertEquals(listOf("E3", "E4", "E5"), withShadow.map { it.name })
        assertNull(galleryElevationTokens().first().level.shadow)
    }

    @Test
    fun `motion gallery uses the named tweens with their durations`() {
        assertEquals(
            listOf("fast", "standard", "slow"),
            galleryMotionTokens().map { it.name }
        )
        assertEquals(Motion.fast, galleryMotionTokens().first().durationMillis)
        assertEquals(Motion.slow, galleryMotionTokens().last().durationMillis)
    }

    @Test
    fun `typography gallery lists the whole scale plus the coordinate style`() {
        val names = galleryTextStyleTokens().map { it.name }

        assertEquals("displaySmall", names.first())
        assertEquals("CoordinateStyle", names.last())
        assertEquals(13, names.size)
    }
}
