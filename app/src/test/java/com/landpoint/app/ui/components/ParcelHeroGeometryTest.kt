package com.landpoint.app.ui.components

import com.landpoint.app.util.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the hero canvas' bounds normalisation (docs/DESIGN_SYSTEM.md §6.3).
 * The thumbnail in the hero is the shape the user compares against the ground
 * and against the map underneath, so the projection must keep aspect ratio,
 * keep north up, and keep metres of east–west the same size as metres of
 * north–south at any latitude.
 */
class ParcelHeroGeometryTest {

    private val lat = -6.914744
    private val lon = 107.609810

    /** A point [northM] metres north and [eastM] metres east of the origin at [originLat]. */
    private fun at(originLat: Double, northM: Double, eastM: Double) = GeoPoint(
        latitude = originLat + northM / 111_320.0,
        longitude = lon + eastM / (111_320.0 * Math.cos(Math.toRadians(originLat)))
    )

    /** A closed square of [sideM] metres, corners walked anticlockwise. */
    private fun square(sideM: Double, originLat: Double = lat) = listOf(
        at(originLat, 0.0, 0.0),
        at(originLat, sideM, 0.0),
        at(originLat, sideM, sideM),
        at(originLat, 0.0, sideM)
    )

    @Test
    fun `nothing in, nothing out`() {
        assertTrue(ParcelHeroGeometry.project(emptyList(), width = 100f, height = 100f, inset = 10f).isEmpty())
    }

    @Test
    fun `a square projects to a square that fills the box within its inset`() {
        // A 200 x 100 canvas with a 10 inset leaves an 180 x 80 box; the square
        // is height-limited, so both of its projected sides become 80.
        val projected = ParcelHeroGeometry.project(square(20.0), width = 200f, height = 100f, inset = 10f)
        assertEquals(4, projected.size)

        val xs = projected.map { it.first }
        val ys = projected.map { it.second }
        val w = xs.max() - xs.min()
        val h = ys.max() - ys.min()
        assertEquals("aspect ratio preserved", h, w, 0.5f)
        assertEquals("height-limited square fills the box height", 80f, h, 0.5f)

        assertTrue("left edge inside the inset", xs.min() >= 10f)
        assertTrue("right edge inside the inset", xs.max() <= 190f)
        assertTrue("top edge inside the inset", ys.min() >= 10f)
        assertTrue("bottom edge inside the inset", ys.max() <= 90f)
    }

    @Test
    fun `the northernmost corner draws at the top`() {
        // Screen y grows downwards and latitude grows north, so the projection
        // flips y; a shape that forgot the flip would draw every parcel upside
        // down.
        val projected = ParcelHeroGeometry.project(square(20.0), width = 100f, height = 100f, inset = 10f)
        val north = projected[1] // square()[1] is the corner 20 m north
        val south = projected[0]
        assertTrue("north must have the smaller y", north.second < south.second)
    }

    @Test
    fun `metres of east-west project the same as metres of north-south`() {
        // The cos(latitude) correction: without it, a square near the poles
        // would draw as a tall sliver because one degree of longitude shrinks.
        val bandung = ParcelHeroGeometry.project(square(20.0), width = 100f, height = 100f, inset = 10f)
        val wBandung = bandung.map { it.first }.max() - bandung.map { it.first }.min()
        val hBandung = bandung.map { it.second }.max() - bandung.map { it.second }.min()
        assertEquals("square stays square at 7°S", hBandung, wBandung, hBandung * 0.01f)

        val tromso = square(20.0, originLat = 69.65)
        val projected = ParcelHeroGeometry.project(tromso, width = 100f, height = 100f, inset = 10f)
        val w = projected.map { it.first }.max() - projected.map { it.first }.min()
        val h = projected.map { it.second }.max() - projected.map { it.second }.min()
        assertEquals("square stays square at 69°N", h, w, h * 0.01f)
    }

    @Test
    fun `a lone corner lands inside the box rather than at infinity`() {
        // Degenerate bounds coerce to a hair of span; the projection must come
        // back finite and on the canvas, not NaN from a division by zero.
        val projected = ParcelHeroGeometry.project(listOf(GeoPoint(lat, lon)), width = 100f, height = 100f, inset = 10f)
        assertEquals(1, projected.size)
        val (x, y) = projected[0]
        assertTrue("x finite and on canvas, got $x", x.isFinite() && x in 0f..100f)
        assertTrue("y finite and on canvas, got $y", y.isFinite() && y in 0f..100f)
    }

    @Test
    fun `corners stacked on one spot do not divide by zero`() {
        val stacked = listOf(GeoPoint(lat, lon), GeoPoint(lat, lon), GeoPoint(lat, lon))
        val projected = ParcelHeroGeometry.project(stacked, width = 100f, height = 100f, inset = 10f)
        assertEquals(3, projected.size)
        projected.forEach { (x, y) ->
            assertTrue("x finite, got $x", x.isFinite())
            assertTrue("y finite, got $y", y.isFinite())
        }
    }

    @Test
    fun `the projection is stable across calls`() {
        val corners = square(20.0)
        val first = ParcelHeroGeometry.project(corners, width = 320f, height = 200f, inset = 12f)
        val second = ParcelHeroGeometry.project(corners, width = 320f, height = 200f, inset = 12f)
        assertEquals(first, second)
    }

    @Test
    fun `a wider box centres the drawing rather than stretching it`() {
        val corners = square(20.0)
        val narrow = ParcelHeroGeometry.project(corners, width = 100f, height = 100f, inset = 10f)
        val wide = ParcelHeroGeometry.project(corners, width = 300f, height = 100f, inset = 10f)
        val wNarrow = narrow.map { it.first }.max() - narrow.map { it.first }.min()
        val wWide = wide.map { it.first }.max() - wide.map { it.first }.min()
        // Both are height-limited (the square fills its 80-high box), so extra
        // canvas width must centre the shape, not distort it — aspect ratio is
        // the whole contract.
        assertEquals(wNarrow, wWide, 0.5f)
    }
}
