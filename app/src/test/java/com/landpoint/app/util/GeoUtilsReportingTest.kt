package com.landpoint.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The reporting and machine-format half of [GeoUtils]: accuracy figures, the
 * cardinal index behind translated abbreviations, the geo: URI handed to
 * navigation apps, and the degree-carry corner of DMS formatting.
 *
 * The labelled `geo:` overload is not exercised here — it percent-encodes
 * through `android.net.Uri`, which only exists on a device or Robolectric.
 */
class GeoUtilsReportingTest {

    // ---- formatAccuracy -----------------------------------------------------

    @Test
    fun `an accuracy below a metre is printed as less than one`() {
        assertEquals("<1", GeoUtils.formatAccuracy(0.0))
        assertEquals("<1", GeoUtils.formatAccuracy(0.4))
        assertEquals("<1", GeoUtils.formatAccuracy(0.99))
    }

    @Test
    fun `an accuracy of a metre or more is printed as whole metres`() {
        assertEquals("1", GeoUtils.formatAccuracy(1.0))
        assertEquals("12", GeoUtils.formatAccuracy(12.4))
        assertEquals("13", GeoUtils.formatAccuracy(12.5))
    }

    // ---- cardinalIndex ------------------------------------------------------

    @Test
    fun `cardinal indices run north to northwest`() {
        assertEquals(0, GeoUtils.cardinalIndex(0.0))
        assertEquals(1, GeoUtils.cardinalIndex(45.0))
        assertEquals(2, GeoUtils.cardinalIndex(90.0))
        assertEquals(3, GeoUtils.cardinalIndex(135.0))
        assertEquals(4, GeoUtils.cardinalIndex(180.0))
        assertEquals(5, GeoUtils.cardinalIndex(225.0))
        assertEquals(6, GeoUtils.cardinalIndex(270.0))
        assertEquals(7, GeoUtils.cardinalIndex(315.0))
    }

    @Test
    fun `the index buckets sit between the cardinal centres`() {
        // A bearing is indexed by which 45-degree wedge it falls in, not by how
        // close it is to a cardinal: 22.4 is still north, 22.6 already northeast.
        assertEquals(0, GeoUtils.cardinalIndex(22.4))
        assertEquals(1, GeoUtils.cardinalIndex(22.6))
        assertEquals(0, GeoUtils.cardinalIndex(337.5))
        // Just under it is still the northwest wedge.
        assertEquals(7, GeoUtils.cardinalIndex(337.4))
        assertEquals(0, GeoUtils.cardinalIndex(359.9))
    }

    @Test
    fun `negative and over-full bearings are normalised before indexing`() {
        assertEquals(6, GeoUtils.cardinalIndex(-90.0))
        assertEquals(2, GeoUtils.cardinalIndex(810.0))
    }

    @Test
    fun `the index stays in step with the abbreviation it translates`() {
        // R.array.cardinal_directions orders its entries N, NE ... NW. Whatever
        // the index returns, the abbreviation picked with it must be the one
        // bearingToCardinal would have said, or a translated label can disagree
        // with the untranslated one on the same screen.
        val cardinals = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        for (bearing in 0..359) {
            assertEquals(
                "bearing $bearing",
                GeoUtils.bearingToCardinal(bearing.toDouble()),
                cardinals[GeoUtils.cardinalIndex(bearing.toDouble())]
            )
        }
    }

    // ---- geoUri -------------------------------------------------------------

    @Test
    fun `a geo uri without a label is the bare coordinate`() {
        assertEquals("geo:-6.9,107.6", GeoUtils.geoUri(-6.9, 107.6))
        assertEquals("geo:0.0,0.0", GeoUtils.geoUri(0.0, 0.0))
    }

    // ---- formatDMS, degree carry --------------------------------------------

    @Test
    fun `a seconds carry that fills the minute carries into the next degree`() {
        // 1°59'59.9999999994" — rounding the seconds to 60.0 flows uphill twice:
        // into the minutes, and then into the degrees.
        val dms = GeoUtils.formatDMS(1.9999999999, -1.9999999999)
        assertEquals("2°0'0.0\" N, 2°0'0.0\" W", dms)
    }
}
