package com.landpoint.app.data.model

import com.landpoint.app.util.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Corner identity — the part that lets a photo stay bound to its corner across
 * a reorder — and the typed status behind the stored free-text key. Generated
 * ids are checked for shape rather than value; everything else is exact.
 */
class CornerIdentityTest {

    // ---- GeoPoint.toCorner --------------------------------------------------

    @Test
    fun `a corner keeps the position and accuracy of its point`() {
        val corner = GeoPoint(latitude = -6.9, longitude = 107.6, accuracyM = 1.5).toCorner("fixed-id")

        assertEquals("fixed-id", corner.id)
        assertEquals(-6.9, corner.latitude, 0.0)
        assertEquals(107.6, corner.longitude, 0.0)
        assertEquals(1.5, corner.accuracyM!!, 0.0)
    }

    @Test
    fun `a corner without a given id gets a fresh one`() {
        val first = GeoPoint(latitude = -6.9, longitude = 107.6).toCorner()
        val second = GeoPoint(latitude = -6.9, longitude = 107.6).toCorner()

        // UUID-shaped and distinct: two corners typed at the same peg are still
        // two corners, each its own identity.
        assertTrue(first.id.isNotBlank())
        assertTrue(first.id.contains('-'))
        assertNotEquals(first.id, second.id)
    }

    // ---- List<CornerPoint>.toGeoPoints --------------------------------------

    @Test
    fun `corners read back as plain points`() {
        val corners = listOf(
            CornerPoint(id = "a", latitude = -6.9, longitude = 107.6, accuracyM = 2.0),
            CornerPoint(id = "b", latitude = -6.8, longitude = 107.7)
        )
        val points = corners.toGeoPoints()

        assertEquals(2, points.size)
        assertEquals(GeoPoint(latitude = -6.9, longitude = 107.6, accuracyM = 2.0), points[0])
        assertEquals(GeoPoint(latitude = -6.8, longitude = 107.7, accuracyM = null), points[1])
    }

    // ---- GeometryCodec.decodeCorners, malformed positions -------------------

    @Test
    fun `a malformed position is dropped instead of shifting the corners`() {
        // The first position holds one number, not a pair; the two good ones
        // must come back with their own coordinates, not slid onto it.
        val corners = GeometryCodec.decodeCorners(
            """{"type":"Polygon","coordinates":[[107.6],[-107.0,-6.9],[-106.9,-7.0]]}"""
        )

        assertEquals(2, corners.size)
        assertEquals(-6.9, corners[0].latitude, 0.0)
        assertEquals(-107.0, corners[0].longitude, 0.0)
        assertEquals(-7.0, corners[1].latitude, 0.0)
        assertEquals(-106.9, corners[1].longitude, 0.0)
    }

    // ---- CornerPoint as a value ---------------------------------------------

    @Test
    fun `a corner can be read apart and put back together`() {
        val corner = CornerPoint(id = "a", latitude = 1.0, longitude = 2.0, accuracyM = null)

        // Destructuring keeps every field where a caller would expect it.
        val (id, latitude, longitude, accuracyM) = corner
        assertEquals("a", id)
        assertEquals(1.0, latitude, 0.0)
        assertEquals(2.0, longitude, 0.0)
        assertNull(accuracyM)

        assertEquals(corner, corner.copy())
        assertEquals(5.0, corner.copy(longitude = 5.0).longitude, 0.0)
    }

    // ---- Land.landStatus ----------------------------------------------------

    private fun land(status: String) = Land(
        id = "l1",
        name = "Sawah",
        description = "",
        notes = "",
        latitude = -6.9,
        longitude = 107.6,
        altitude = null,
        accuracy = null,
        address = null,
        parcelNumber = null,
        areaSqm = null,
        status = status,
        createdAt = 0L,
        updatedAt = 0L,
        photos = emptyList()
    )

    @Test
    fun `the stored status key maps onto the typed status`() {
        assertEquals(LandStatus.ACTIVE, land("aktif").landStatus)
        assertEquals(LandStatus.DISPUTED, land("sengketa").landStatus)
        assertEquals(LandStatus.SOLD, land("terjual").landStatus)
        assertEquals(LandStatus.TRANSFERRED, land("dialihkan").landStatus)
    }

    @Test
    fun `a status no preset covers reads as active, the default for old rows`() {
        assertEquals(LandStatus.ACTIVE, land("lama").landStatus)
        assertEquals(LandStatus.ACTIVE, land("").landStatus)
    }
}
