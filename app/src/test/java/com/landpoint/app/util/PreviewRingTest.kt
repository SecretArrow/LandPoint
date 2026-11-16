package com.landpoint.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The preview line is what makes a corner being placed read as "this is the
 * shape I am about to make". These tests pin the geometry that produces:
 * nothing before a mark hangs, a single spoke off one corner, a two-edge open
 * ring, and the closing edge that turns an open ring into a loop once a second
 * corner stands.
 */
class PreviewRingTest {

    private val a = GeoPoint(-6.2, 106.8)
    private val b = GeoPoint(-6.21, 106.81)
    private val c = GeoPoint(-6.22, 106.80)
    private val mark = GeoPoint(-6.205, 106.805)

    @Test
    fun `nothing is drawn when no mark hangs`() {
        assertTrue(PreviewRing.segments(listOf(a), null).isEmpty())
        assertTrue(PreviewRing.segments(listOf(a, b), null).isEmpty())
    }

    @Test
    fun `nothing is drawn when no corner stands yet`() {
        assertTrue(PreviewRing.segments(emptyList(), mark).isEmpty())
    }

    @Test
    fun `one corner and a mark draws the single spoke to the mark`() {
        // One committed corner has nothing to close back to: the mark is a
        // spoke off it, not a loop.
        val ring = PreviewRing.segments(listOf(a), mark)

        assertEquals(listOf(a, mark), ring)
    }

    @Test
    fun `two corners and a mark closes the ring back to the first`() {
        // Two committed corners plus the mark is three points — a shape. The
        // closing edge from the mark back to the first corner turns the two
        // open sides into the triangle the next commit would leave behind.
        val ring = PreviewRing.segments(listOf(a, b), mark)

        assertEquals(listOf(b, mark, a), ring)
    }

    @Test
    fun `three or more corners close the ring back to the first`() {
        val ring = PreviewRing.segments(listOf(a, b, c), mark)

        assertEquals(listOf(c, mark, a), ring)
    }

    @Test
    fun `the mark is never the first point of the preview`() {
        // The preview leads off the last committed corner, not the mark — the
        // mark is where the user is heading, not where they came from. A preview
        // that opened on the mark would read as drawing away from the ring
        // rather than towards it.
        val oneCorner = PreviewRing.segments(listOf(a), mark)
        val twoCorners = PreviewRing.segments(listOf(a, b), mark)

        assertTrue(oneCorner.isNotEmpty())
        assertTrue(twoCorners.isNotEmpty())
        assertEquals(a, oneCorner.first())
        assertEquals(b, twoCorners.first())
    }
}
