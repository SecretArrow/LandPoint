package com.landpoint.app.ui.components

import com.landpoint.app.util.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the side-length label the map draws at each edge's midpoint
 * ([LandMapOverlay.edgeLabel]).
 *
 * A boundary is a measurement before it is a shape, and the question the
 * labels exist to answer — "is this side the 18 m the deed says it is?" — is
 * the one a survey pans the map to check. The label is `formatDistance` over
 * `distance`, so the cases that matter are the same ones `GeoUtilsTest` pins
 * for the two pieces; what this test adds is the contract the overlay relies
 * on: every label carries a unit, short sides read in metres, long ones in
 * kilometres, and a degenerate side is "0 m" rather than crashing.
 */
class EdgeLabelTest {

    private val origin = GeoPoint(latitude = -6.914744, longitude = 107.609810)

    /** A point [northM] metres north and [eastM] metres east of the origin. */
    private fun at(northM: Double, eastM: Double): GeoPoint {
        val lat = origin.latitude
        return GeoPoint(
            latitude = lat + northM / 111_320.0,
            longitude = origin.longitude + eastM / (111_320.0 * Math.cos(Math.toRadians(lat)))
        )
    }

    @Test
    fun `a short side reads in whole metres`() {
        // ~18 m north — the kind of figure a deed states for one side of a plot.
        val label = LandMapOverlay.edgeLabel(origin, at(northM = 18.0, eastM = 0.0))
        assertTrue("expected '18 m', got '$label'", label.endsWith(" m"))
        assertTrue("expected a whole-metre figure, got '$label'", label.startsWith("18"))
    }

    @Test
    fun `a long side switches to kilometres`() {
        val label = LandMapOverlay.edgeLabel(origin, at(northM = 2_500.0, eastM = 0.0))
        assertEquals("2.5 km", label)
    }

    @Test
    fun `a degenerate side is zero metres`() {
        val label = LandMapOverlay.edgeLabel(origin, origin)
        assertEquals("0 m", label)
    }

    @Test
    fun `the label is symmetric`() {
        // A side has no direction; the label read corner-to-corner must match
        // the one read corner-from-corner, or the map would show two lengths
        // for the one side.
        val a = at(northM = 0.0, eastM = 0.0)
        val b = at(northM = 50.0, eastM = 0.0)
        assertEquals(LandMapOverlay.edgeLabel(a, b), LandMapOverlay.edgeLabel(b, a))
    }

    @Test
    fun `every label carries a unit suffix`() {
        // Whatever the distance, the map must never show a bare number — a side
        // length without a unit is a figure the user cannot act on.
        val sides = listOf(
            at(northM = 1.0, eastM = 0.0),
            at(northM = 100.0, eastM = 0.0),
            at(northM = 1_500.0, eastM = 0.0)
        )
        for (point in sides) {
            val label = LandMapOverlay.edgeLabel(origin, point)
            assertTrue("label '$label' has no unit", label.endsWith(" m") || label.endsWith(" km"))
        }
    }
}
