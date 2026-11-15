package com.landpoint.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.landpoint.app.R
import com.landpoint.app.data.model.LandStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The §4 mapping is a contract the whole status system rests on: chip fill,
 * icon shape and label must stay in step, and the container must always be
 * the status color at [STATUS_CONTAINER_ALPHA] — never the full saturation.
 */
class ParcelStatusVisualTest {

    private val extended = lightLandExtendedColors()
    private val scheme = lightColorScheme(
        secondary = Color(0xFF112233),
        outline = Color(0xFF445566)
    )

    private fun visual(status: LandStatus?) = parcelStatusVisual(status, extended, scheme)

    @Test
    fun `active maps to success with a check icon`() {
        val v = visual(LandStatus.ACTIVE)

        assertEquals(extended.success, v.color)
        assertSame(Icons.Outlined.CheckCircle, v.icon)
        assertEquals(R.string.land_status_active, v.labelRes)
    }

    @Test
    fun `disputed maps to warning with a triangle icon`() {
        val v = visual(LandStatus.DISPUTED)

        assertEquals(extended.warning, v.color)
        assertSame(Icons.Outlined.Warning, v.icon)
        assertEquals(R.string.land_status_disputed, v.labelRes)
    }

    @Test
    fun `sold maps to info with a sell icon`() {
        val v = visual(LandStatus.SOLD)

        assertEquals(extended.info, v.color)
        assertSame(Icons.Outlined.Sell, v.icon)
        assertEquals(R.string.land_status_sold, v.labelRes)
    }

    @Test
    fun `transferred maps to the scheme secondary with a swap icon`() {
        val v = visual(LandStatus.TRANSFERRED)

        assertEquals(Color(0xFF112233), v.color)
        assertSame(Icons.Outlined.SwapHoriz, v.icon)
        assertEquals(R.string.land_status_transferred, v.labelRes)
    }

    @Test
    fun `unknown status maps to the scheme outline with a help icon and no label`() {
        val v = visual(null)

        assertEquals(Color(0xFF445566), v.color)
        assertSame(Icons.Outlined.HelpOutline, v.icon)
        assertNull(v.labelRes)
    }

    @Test
    fun `container is always the status color at the chip fill alpha`() {
        LandStatus.entries.forEach { status ->
            val v = visual(status)
            assertEquals(v.color.copy(alpha = STATUS_CONTAINER_ALPHA), v.containerColor)
        }
    }

    @Test
    fun `chip fill alpha is the documented twelve percent`() {
        assertEquals(0.12f, STATUS_CONTAINER_ALPHA)
    }

    @Test
    fun `dark theme swaps every semantic color for its dark variant`() {
        val dark = darkLandExtendedColors()

        assertEquals(SuccessDark, dark.success)
        assertEquals(WarningDark, dark.warning)
        assertEquals(InfoDark, dark.info)
        assertEquals(MapOverlayLineDark, dark.mapOverlayLine)
    }

    @Test
    fun `accent aliases the theme primary in both modes`() {
        assertEquals(Green40, lightLandExtendedColors().accent)
        assertEquals(Green80, darkLandExtendedColors().accent)
    }
}
