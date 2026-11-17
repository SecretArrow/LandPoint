package com.landpoint.app.ui.preview

import com.landpoint.app.data.model.CornerPoint
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The draft hand-off between the editor and the preview destination. The store
 * is a process-wide singleton — the editor writes before navigating, preview
 * reads — so every test cleans up behind itself, and the empty default is what
 * a reader gets after a save wiped it.
 */
class PreviewDraftStoreTest {

    @After
    fun tearDown() {
        PreviewDraftStore.clear()
    }

    @Test
    fun `a cleared store holds the empty draft`() {
        PreviewDraftStore.clear()

        assertEquals("", PreviewDraftStore.draft.name)
        assertTrue(PreviewDraftStore.draft.corners.isEmpty())
        assertTrue(PreviewDraftStore.draft.photoCounts.isEmpty())
    }

    @Test
    fun `the draft set is the draft read`() {
        val corners = listOf(
            CornerPoint(id = "c1", latitude = -6.9, longitude = 107.6),
            CornerPoint(id = "c2", latitude = -6.8, longitude = 107.7)
        )

        PreviewDraftStore.set(
            name = "Sawah Pak Amat",
            corners = corners,
            photoCounts = mapOf("c1" to 2)
        )

        assertEquals("Sawah Pak Amat", PreviewDraftStore.draft.name)
        assertEquals(corners, PreviewDraftStore.draft.corners)
        assertEquals(mapOf("c1" to 2), PreviewDraftStore.draft.photoCounts)
    }

    @Test
    fun `clearing after a set returns to the empty draft`() {
        PreviewDraftStore.set(
            name = "unsaved",
            corners = listOf(CornerPoint(latitude = 0.0, longitude = 0.0)),
            photoCounts = emptyMap()
        )

        PreviewDraftStore.clear()

        assertTrue(PreviewDraftStore.draft.corners.isEmpty())
        assertEquals("", PreviewDraftStore.draft.name)
    }
}
