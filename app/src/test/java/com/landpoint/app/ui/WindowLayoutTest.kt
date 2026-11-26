package com.landpoint.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The width decisions and the fold decision, on the windows real devices hand
 * the app.
 *
 * Worth a test because none of them is visible in a unit test otherwise — a
 * layout is checked by looking at it, and there is no tablet or foldable here to
 * look at. What can be pinned down is the arithmetic: that a phone held upright
 * keeps the layout it was drawn for, that the same phone on its side gets both
 * width changes, that the thresholds are the exact dp they claim to be rather
 * than one out, and that a half-opened hinge lifts the bottom of the window
 * exactly to the hinge's top edge and not a pixel further.
 */
class WindowLayoutTest {

    /** dp widths, taken from the window sizes these devices actually report. */
    private val phoneUpright = 411
    private val phoneOnItsSide = 891
    private val smallPhoneOnItsSide = 731
    private val tabletUpright = 800
    private val tabletOnItsSide = 1280
    private val splitScreenSliver = 320

    /**
     * Pixel geometry of a device stood half-opened on its hinge: a 2200px-tall
     * window, the hinge band across it at 1380–1420, and the bottom chrome
     * resting at the window's bottom edge less its 6dp of air.
     */
    private val hingeTop = 1380
    private val hingeBottom = 1420
    private val bottomChromeEdge = 2194
    private val tabletopFold = FoldState(
        isSeparating = true,
        hingeRunsHorizontally = true,
        foldTopY = hingeTop,
        foldBottomY = hingeBottom
    )

    @Test
    fun `a phone held upright keeps the bottom bar and one pane`() {
        assertFalse(WindowLayout.railInsteadOfBottomBar(phoneUpright))
        assertFalse(WindowLayout.listBesideDetail(phoneUpright))
    }

    @Test
    fun `a phone on its side moves the destinations aside and shows both panes`() {
        assertTrue(WindowLayout.railInsteadOfBottomBar(phoneOnItsSide))
        assertTrue(WindowLayout.listBesideDetail(phoneOnItsSide))
    }

    @Test
    fun `a small phone on its side gets the rail but not a second pane`() {
        // 731dp split two ways is a 290dp record: a rail is worth having long
        // before a second pane is, which is the whole reason for two thresholds.
        assertTrue(WindowLayout.railInsteadOfBottomBar(smallPhoneOnItsSide))
        assertFalse(WindowLayout.listBesideDetail(smallPhoneOnItsSide))
    }

    @Test
    fun `a tablet upright gets the rail and keeps one pane`() {
        assertTrue(WindowLayout.railInsteadOfBottomBar(tabletUpright))
        assertFalse(WindowLayout.listBesideDetail(tabletUpright))
    }

    @Test
    fun `a tablet on its side gets both`() {
        assertTrue(WindowLayout.railInsteadOfBottomBar(tabletOnItsSide))
        assertTrue(WindowLayout.listBesideDetail(tabletOnItsSide))
    }

    @Test
    fun `a window squeezed into a corner of a split screen is treated as a phone`() {
        // The decisions read the window, not the screen, so a tablet running the
        // app in a third of its width must still get the phone layout.
        assertFalse(WindowLayout.railInsteadOfBottomBar(splitScreenSliver))
        assertFalse(WindowLayout.listBesideDetail(splitScreenSliver))
    }

    @Test
    fun `both thresholds include the width they name and exclude the one below`() {
        assertFalse(WindowLayout.railInsteadOfBottomBar(WindowLayout.RAIL_FROM_DP - 1))
        assertTrue(WindowLayout.railInsteadOfBottomBar(WindowLayout.RAIL_FROM_DP))
        assertFalse(WindowLayout.listBesideDetail(WindowLayout.TWO_PANE_FROM_DP - 1))
        assertTrue(WindowLayout.listBesideDetail(WindowLayout.TWO_PANE_FROM_DP))
    }

    @Test
    fun `a second pane never appears without the rail`() {
        // A window wide enough for two panes but still carrying a bottom bar would
        // be the worst of both: the shorter half of the screen spent on the bar,
        // and the record squeezed for it.
        assertTrue(WindowLayout.TWO_PANE_FROM_DP >= WindowLayout.RAIL_FROM_DP)
        for (width in 0..1600 step 7) {
            if (WindowLayout.listBesideDetail(width)) {
                assertTrue(
                    "two panes at ${width}dp but no rail",
                    WindowLayout.railInsteadOfBottomBar(width)
                )
            }
        }
    }

    @Test
    fun `a window with no fold moves nothing`() {
        assertEquals(bottomChromeEdge, WindowLayout.toolbarAboveHinge(bottomChromeEdge, FoldState.FLAT))
    }

    @Test
    fun `a foldable opened flat is one plane and moves nothing`() {
        // Fully open, the device is a tablet: one plane, and the bottom chrome is
        // exactly where a tablet's would be. The hinge is still reported — it is
        // just not separating anything.
        val openFlat = FoldState(
            isSeparating = false,
            hingeRunsHorizontally = true,
            foldTopY = hingeTop,
            foldBottomY = hingeBottom
        )
        assertEquals(bottomChromeEdge, WindowLayout.toolbarAboveHinge(bottomChromeEdge, openFlat))
    }

    @Test
    fun `a hinge across the window lifts the chrome to its top edge`() {
        // The usual tabletop case: the band sits mid-window and the chrome is
        // anchored to the window's bottom edge, below it. "Just above the hinge"
        // is measured from the band's top edge, not its bottom — the whole band
        // is hinge.
        assertEquals(hingeTop, WindowLayout.toolbarAboveHinge(bottomChromeEdge, tabletopFold))
    }

    @Test
    fun `a hinge down the window from left to right moves nothing vertically`() {
        // A book-posture fold divides left from right; the bottom edge is still
        // the bottom edge of a plane the user is looking at.
        val bookPosture = FoldState(
            isSeparating = true,
            hingeRunsHorizontally = false,
            foldTopY = hingeTop,
            foldBottomY = hingeBottom
        )
        assertEquals(bottomChromeEdge, WindowLayout.toolbarAboveHinge(bottomChromeEdge, bookPosture))
    }

    @Test
    fun `a chrome already clear of the hinge stays put`() {
        // Its bottom edge above the hinge's top means it is already in the half
        // that faces the user; lifting it again would only open a gap.
        assertEquals(900, WindowLayout.toolbarAboveHinge(900, tabletopFold))
    }

    @Test
    fun `resting exactly on the hinge top is already the answer`() {
        assertEquals(hingeTop, WindowLayout.toolbarAboveHinge(hingeTop, tabletopFold))
    }

    @Test
    fun `the first pixel inside the fold is lifted to the hinge top`() {
        assertEquals(hingeTop, WindowLayout.toolbarAboveHinge(hingeTop + 1, tabletopFold))
    }

    @Test
    fun `a chrome touching the hinge's far edge is lifted by the same rule`() {
        // Bottom edge at the band's bottom still means the chrome reaches the
        // hinge; the rule reads the edge against the band's top because anything
        // at or below that is on the fold's side of the window.
        assertEquals(hingeTop, WindowLayout.toolbarAboveHinge(hingeBottom, tabletopFold))
    }

    @Test
    fun `a hinge reported as a single line still lifts the chrome to it`() {
        val lineHinge = FoldState(
            isSeparating = true,
            hingeRunsHorizontally = true,
            foldTopY = 1500,
            foldBottomY = 1500
        )
        assertEquals(1500, WindowLayout.toolbarAboveHinge(2200, lineHinge))
    }

    @Test
    fun `a nonsense reading of the hinge moves nothing`() {
        // A hinge at or above the top of the window it folds, or a band reported
        // bottom-up, is not a fold. Garbage in, layout untouched.
        val atTheVeryTop = FoldState(
            isSeparating = true,
            hingeRunsHorizontally = true,
            foldTopY = 0,
            foldBottomY = hingeBottom
        )
        val inverted = FoldState(
            isSeparating = true,
            hingeRunsHorizontally = true,
            foldTopY = 1500,
            foldBottomY = 1400
        )
        assertEquals(bottomChromeEdge, WindowLayout.toolbarAboveHinge(bottomChromeEdge, atTheVeryTop))
        assertEquals(bottomChromeEdge, WindowLayout.toolbarAboveHinge(bottomChromeEdge, inverted))
    }

    @Test
    fun `the fold never moves an overlay downward, for any reading`() {
        // A lift is a lift: whatever the hinge claims, the overlay's edge may only
        // come back where it was or higher up. Every hinge-top a device could
        // plausibly report, against every chrome edge in the same window.
        for (edge in 100..2400 step 173) {
            for (top in listOf(0, 1, 700, hingeTop, hingeBottom, 2200, 2600)) {
                assertTrue(
                    "hinge top $top lifted an edge at $edge downward",
                    WindowLayout.toolbarAboveHinge(edge, tabletopFold.copy(foldTopY = top)) <= edge
                )
            }
        }
    }
}
