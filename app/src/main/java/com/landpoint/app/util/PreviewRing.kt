package com.landpoint.app.util

/**
 * The dashed preview line drawn on top of the committed outline while a corner
 * is being placed.
 *
 * One working of the "Tambah sudut" button adds one corner — but the user is
 * shown what that corner *would* close before they confirm it. This object
 * works out the polyline to pass to the map's preview layer from the draft
 * corners that already stand and the mark that hangs:
 *
 * - with one committed corner and a mark, the line is the new side from that
 *   corner to the mark.
 * - with two or more committed corners and a mark, the line keeps going from
 *   the last committed corner to the mark, and then closes back to the first
 *   committed corner — so the user sees the whole ring the next commit would
 *   leave behind, not just its newest edge.
 *
 * Returns an empty list when there is nothing to preview: no mark hangs, or no
 * committed corner stands for the mark to reach from. The map draws nothing in
 * that case, which is the right answer — a mark on its own is already shown as
 * the pending dot.
 */
object PreviewRing {

    /**
     * @param committed the corners already in the draft, in ring order.
     * @param mark the point the user is positioning, or null when none hangs.
     */
    fun segments(committed: List<GeoPoint>, mark: GeoPoint?): List<GeoPoint> {
        if (mark == null || committed.isEmpty()) return emptyList()
        // The newest side: from the last committed corner to the mark. Always
        // drawn, whatever the count, because every new corner adds exactly one
        // side to the corner before it.
        val ring = ArrayList<GeoPoint>(committed.size + 2)
        ring.add(committed.last())
        ring.add(mark)
        // The closing side: from the mark back to the first corner. Once two
        // corners stand, the mark and those two are three points — enough to be
        // a shape — so the closing edge turns the two open sides into the ring
        // the next commit would leave behind. Before that, a single committed
        // corner has nothing to close back to: the mark is a spoke off it,
        // not a loop, and closing would draw a degenerate there-and-back.
        if (committed.size >= 2) {
            ring.add(committed.first())
        }
        return ring
    }
}
