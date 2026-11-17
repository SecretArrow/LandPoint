package com.landpoint.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The id-list twin of the corner swap. Ids travel with their corner so photos
 * stay bound across a reorder, so [BoundaryEdits.swapStrings] must behave
 * exactly like [BoundaryEdits.swap] does on coordinates — including absorbing
 * an index a concurrent rewrite made stale.
 */
class BoundaryEditsSwapTest {

    private val ids = listOf("c1", "c2", "c3", "c4")

    @Test
    fun `swapping two ids reorders the list and leaves the rest alone`() {
        assertEquals(listOf("c1", "c4", "c3", "c2"), BoundaryEdits.swapStrings(ids, 1, 3))
        assertEquals(listOf("c2", "c1", "c3"), BoundaryEdits.swapStrings(listOf("c1", "c2", "c3"), 0, 1))
    }

    @Test
    fun `swapping an id with itself changes nothing`() {
        assertSame(ids, BoundaryEdits.swapStrings(ids, 2, 2))
    }

    @Test
    fun `an index the list does not have changes nothing`() {
        assertSame(ids, BoundaryEdits.swapStrings(ids, 0, ids.size))
        assertSame(ids, BoundaryEdits.swapStrings(ids, -1, 1))
    }

    @Test
    fun `the list handed in is never mutated`() {
        BoundaryEdits.swapStrings(ids, 0, 3)
        assertEquals(listOf("c1", "c2", "c3", "c4"), ids)
    }
}
