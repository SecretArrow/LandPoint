package com.landpoint.app.map

import com.landpoint.app.util.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Bounds are plain arithmetic — that is why this type exists instead of
 * MapLibre's — so the cases that matter are the edges: fewer than two points,
 * a rectangle with no extent, and a constructor asked for south above north.
 */
class GeoBoundsTest {

    private val cluster = listOf(
        GeoPoint(latitude = -6.9, longitude = 107.5),
        GeoPoint(latitude = -7.1, longitude = 107.7),
        GeoPoint(latitude = -6.8, longitude = 107.4)
    )

    // ---- companion of() -----------------------------------------------------

    @Test
    fun `fewer than two points is not a rectangle`() {
        assertNull(GeoBounds.of(emptyList()))
        assertNull(GeoBounds.of(listOf(GeoPoint(latitude = -6.9, longitude = 107.6))))
    }

    @Test
    fun `the rectangle holds every point given`() {
        val bounds = GeoBounds.of(cluster)!!

        assertEquals(-7.1, bounds.south, 0.0)
        assertEquals(107.4, bounds.west, 0.0)
        assertEquals(-6.8, bounds.north, 0.0)
        assertEquals(107.7, bounds.east, 0.0)
    }

    @Test
    fun `points on top of each other enclose nothing`() {
        val same = GeoPoint(latitude = -6.9, longitude = 107.6)
        assertNull(GeoBounds.of(listOf(same, same, same)))
    }

    // ---- centre -------------------------------------------------------------

    @Test
    fun `the centre is the midpoint of each axis`() {
        val bounds = GeoBounds(south = -7.0, west = 107.0, north = -6.0, east = 108.0)

        assertEquals(-6.5, bounds.centreLatitude, 0.0)
        assertEquals(107.5, bounds.centreLongitude, 0.0)
    }

    // ---- degeneracy ---------------------------------------------------------

    @Test
    fun `a rectangle with no extent in either axis is degenerate`() {
        assertTrue(GeoBounds(0.0, 10.0, 0.0, 15.0).isDegenerate)
        assertTrue(GeoBounds(0.0, 10.0, 5.0, 10.0).isDegenerate)
        assertFalse(GeoBounds(0.0, 10.0, 5.0, 15.0).isDegenerate)
    }

    // ---- constructor guards -------------------------------------------------

    @Test
    fun `south above north is refused`() {
        try {
            GeoBounds(south = 1.0, west = 0.0, north = 0.0, east = 1.0)
            fail("expected south above north to be refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("south"))
        }
    }

    @Test
    fun `west east of east is refused`() {
        try {
            GeoBounds(south = 0.0, west = 2.0, north = 1.0, east = 1.0)
            fail("expected west east of east to be refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("west"))
        }
    }
}
