package com.landpoint.app.map

import com.landpoint.app.data.BasemapMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The registry half of the map: what each provider tells the engine about its
 * tiles. The numbers below are the licence and coverage facts the download
 * budget and the UI act on — a wrong one either costs the user a download that
 * cannot exist or draws a blank map, so they are pinned here.
 */
class MapProviderTileZoomTest {

    // ---- tileMaxZoom per tile spec ------------------------------------------

    @Test
    fun `the street style publishes tiles to level fourteen`() {
        val street = MapProviders.of(BasemapMode.STREET, null)!!

        assertSame(MapProviders.streetStyle, street.tiles)
        assertEquals(14.0, street.tileMaxZoom, 0.0)
    }

    @Test
    fun `satellite costs tiles to its deeper detail tier`() {
        val satellite = MapProviders.of(BasemapMode.SATELLITE, null)!!

        // Esri's zoom-19 tier is asked for wherever it exists, and that is the
        // deepest tile a download would actually fetch.
        assertEquals(19.0, satellite.tileMaxZoom, 0.0)
    }

    @Test
    fun `terrain falls back to the deepest published tile`() {
        val terrain = MapProviders.of(BasemapMode.TERRAIN, null)!!

        // No detail tier: OpenTopoMap stops at 17 and the camera limit (19) is
        // not a tile limit.
        assertEquals(17.0, terrain.tileMaxZoom, 0.0)
    }

    @Test
    fun `an imported archive costs nothing deeper than its camera allows`() {
        val archive = ImportedArchiveSpec(
            file = File("/device/maps/banjarnegara.pmtiles"),
            vector = true,
            minZoom = 7.0
        )
        val imported = MapProviders.of(BasemapMode.IMPORTED, archive)!!

        assertEquals(7.0, imported.minZoom, 0.0)
        assertEquals(MapProviders.DEEPEST_ZOOM, imported.tileMaxZoom, 0.0)
    }

    // ---- network and licence pass-through -----------------------------------

    @Test
    fun `the licence and radio answers come from the mode`() {
        val street = MapProviders.of(BasemapMode.STREET, null)!!
        val satellite = MapProviders.of(BasemapMode.SATELLITE, null)!!

        // OpenFreeMap permits caching; Esri does not.
        assertTrue(street.allowsOfflineDownload)
        assertFalse(satellite.allowsOfflineDownload)

        assertTrue(street.needsNetwork)
        assertTrue(satellite.needsNetwork)
    }

    @Test
    fun `an imported map needs no radio and offers nothing to download`() {
        val archive = ImportedArchiveSpec(file = File("/device/maps/paras.pmtiles"), vector = true)
        val imported = MapProviders.of(BasemapMode.IMPORTED, archive)!!

        assertFalse(imported.needsNetwork)
        assertFalse(imported.allowsOfflineDownload)
    }

    // ---- of() and resolved() ------------------------------------------------

    @Test
    fun `an imported mode with no archive on the device draws nothing`() {
        assertNull(MapProviders.of(BasemapMode.IMPORTED, archive = null))
    }

    @Test
    fun `no choice and no archive resolves to the street map`() {
        val resolved = MapProviders.resolved(chosen = null, archive = null)

        assertEquals(BasemapMode.STREET, resolved.mode)
        assertSame(MapProviders.streetStyle, resolved.tiles)
    }

    @Test
    fun `a chosen imported mode without a vector map falls back to the street map`() {
        val resolved = MapProviders.resolved(chosen = BasemapMode.IMPORTED, archive = null)

        assertEquals(BasemapMode.STREET, resolved.mode)
    }
}
