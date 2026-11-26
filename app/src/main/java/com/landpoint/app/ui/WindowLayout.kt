package com.landpoint.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * What to do with the width the window happens to have.
 *
 * The app was drawn for a phone held upright, where there is one column of room
 * and the three destinations belong along the bottom edge. Turn that phone on its
 * side, or open the app on a tablet, and the shape of the problem inverts: width
 * to spare, height suddenly scarce, and a bottom bar spending some of the little
 * height left on something that would sit happily in the margin.
 *
 * Two thresholds, both Material's, both measured on the window rather than the
 * screen so a freeform or split-screen window is judged on the room it actually
 * got:
 *
 *  - from 600dp ("medium") the destinations move to a rail at the side;
 *  - from 840dp ("expanded") the land list keeps the detail beside it instead of
 *    handing the whole screen over to it.
 *
 * The fold decision at the bottom works the same way, on the fold's own reading
 * of the window ([FoldState]) instead of its width.
 *
 * Kept as plain arithmetic on a number, apart from the reading of that number, so
 * the decisions can be checked in a test rather than only on a device.
 */
object WindowLayout {

    /** Material's medium window: a tablet upright, most phones on their side. */
    const val RAIL_FROM_DP = 600

    /** Material's expanded window: room for two panes without cramping either. */
    const val TWO_PANE_FROM_DP = 840

    /** Destinations at the side rather than along the bottom. */
    fun railInsteadOfBottomBar(widthDp: Int): Boolean = widthDp >= RAIL_FROM_DP

    /**
     * The chosen record shown beside the list rather than over it.
     *
     * Deliberately stricter than [railInsteadOfBottomBar]: a 600dp window split
     * two ways leaves a record in about 360dp, which is a phone's worth of width
     * for a screen carrying a map, an area readout and a list of corners. The rail
     * is worth having long before a second pane is.
     */
    fun listBesideDetail(widthDp: Int): Boolean = widthDp >= TWO_PANE_FROM_DP

    /**
     * The bottom edge a bottom-anchored overlay should sit at, once the fold is
     * respected.
     *
     * A device stood half-opened on its hinge — the tabletop posture — draws its
     * window across two planes with the hinge between them: everything from the
     * hinge's top edge down is either the hinge band itself or the half lying
     * face-down on the table. An overlay anchored to the window's bottom edge
     * starts below that line by construction, so when a separating hinge runs
     * across the window, that edge belongs at the hinge's top — just above the
     * fold, in the half that faces the user.
     *
     * A hinge running the other way (down the window, dividing left from right)
     * takes nothing away vertically and moves nothing. Neither does a hinge
     * reported but not separating — a device opened flat is one plane, a tablet —
     * and neither does no hinge at all, which is every phone and every emulator.
     * The overlay's edge comes back unchanged in all of those, and the layout is
     * the one the app has always drawn.
     *
     * @param toolbarBottomY the overlay's natural bottom edge, in window pixels
     *   with y measured down from the window's top — the space the fold's bounds
     *   arrive in.
     * @param fold the window's fold reading; [FoldState.FLAT] where there is none.
     * @return [toolbarBottomY] unchanged unless a separating hinge runs across
     *   the window above that edge; then the hinge's top edge, so the overlay
     *   ends up exactly just above the hinge.
     */
    fun toolbarAboveHinge(toolbarBottomY: Int, fold: FoldState): Int {
        if (!fold.isSeparating || !fold.hingeRunsHorizontally) return toolbarBottomY
        // A hinge cannot sit at or above the top of the window it folds, and a
        // band cannot run bottom-up. A reading that says otherwise is garbage
        // from somewhere in the OEM stack, and the safe response to garbage is
        // to leave the layout alone.
        if (fold.foldTopY <= 0 || fold.foldTopY > fold.foldBottomY) return toolbarBottomY
        // Already clear of the hinge — including resting exactly on its top
        // edge, which is where it would have been moved to anyway.
        if (toolbarBottomY <= fold.foldTopY) return toolbarBottomY
        return fold.foldTopY
    }
}

/**
 * The width of the window, in dp — not of the screen.
 *
 * Reads it from the configuration rather than from a size class, because the size
 * classes live in a library this app does not otherwise need, and one integer is
 * all the two decisions above take.
 */
@Composable
@ReadOnlyComposable
fun windowWidthDp(): Int = LocalConfiguration.current.screenWidthDp
