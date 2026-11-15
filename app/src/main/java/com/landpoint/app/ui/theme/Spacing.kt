package com.landpoint.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The named spacing scale (docs/DESIGN_SYSTEM.md §2.3).
 *
 * Material 3 has no spacing tokens, and without them every `padding(16.dp)`
 * is a magic number that drifts. Everything sits on a 4/8 grid, with one
 * deliberate exception: [md] is 12 because that is what separates a card's
 * title from its body and reads better than 8 or 16 at arm's length.
 *
 * Every `padding(...)` in new code references a token from here; raw dp
 * literals are a review flag.
 */
object Spacing {

    /** Icon-to-label gap inside a chip or button. */
    val xs: Dp = 4.dp

    /** Tight vertical rhythm inside a card. */
    val sm: Dp = 8.dp

    /** Card content padding, list-item leading; the deliberate 12 exception. */
    val md: Dp = 12.dp

    /** Card-to-card gap, screen edge gutter. */
    val lg: Dp = 16.dp

    /** Section separation. */
    val xl: Dp = 24.dp

    /** Empty-state illustration gap. */
    val xxl: Dp = 32.dp

    /** FAB inset from the screen edge. */
    val fab: Dp = 16.dp
}
