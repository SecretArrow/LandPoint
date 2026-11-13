package com.landpoint.app.ui.edit

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.ui.graphics.vector.ImageVector
import com.landpoint.app.R

/**
 * The four ways of working on a boundary's corners in the corner picker.
 *
 * The picker used to switch between these implicitly — a button per source for
 * adding, a long-press to drag, a tap-on-edge to insert — so a user could not
 * tell which gesture the screen was set up to receive until they had already
 * tried it. An explicit mode makes that visible: the segmented control in
 * [CornerModeToolbar] names what the next touch is going to do, and a one-line
 * hint says how.
 *
 * The enum is presentation only — it carries an icon and two string resource
 * ids (label and hint). Wiring each mode to actually change the picker's
 * behaviour is a follow-up; this type exists so the toolbar can be drawn and
 * the active mode held in state before any of that wiring lands.
 */
enum class CornerPickerMode(
    val icon: ImageVector,
    @StringRes val labelRes: Int,
    @StringRes val hintRes: Int
) {
    /**
     * Place a new corner. The map crosshair, the "Place a mark here" button and
     * the four alternative sources (GPS, coordinates, bearing, neighbour) all
     * belong to this mode.
     */
    ADD(
        icon = Icons.Outlined.Add,
        labelRes = R.string.corner_mode_add,
        hintRes = R.string.corner_mode_hint_add
    ),

    /**
     * Reposition a corner that is already part of the boundary by dragging its
     * handle on the map.
     */
    MOVE(
        icon = Icons.Outlined.OpenWith,
        labelRes = R.string.corner_mode_move,
        hintRes = R.string.corner_mode_hint_move
    ),

    /**
     * Add a corner between two existing ones by tapping a midpoint on the edge
     * that should gain a vertex — fixing an outline that closes but misses a
     * peg without reordering the whole ring.
     */
    INSERT(
        icon = Icons.Outlined.AddCircleOutline,
        labelRes = R.string.corner_mode_insert,
        hintRes = R.string.corner_mode_hint_insert
    ),

    /**
     * Remove a corner by tapping it. Selection and the existing delete button
     * stay available; this mode makes the tap itself the gesture.
     */
    DELETE(
        icon = Icons.Outlined.Delete,
        labelRes = R.string.corner_mode_delete,
        hintRes = R.string.corner_mode_hint_delete
    )
}
