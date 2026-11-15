package com.landpoint.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The elevation scale (docs/DESIGN_SYSTEM.md §2.5).
 *
 * Material 3 tonal elevation carries almost everything: cards in a list sit at
 * [E0] and are separated by [Spacing] rather than shadow. A real shadow is
 * reserved for the places tonal elevation cannot reach — map markers and
 * corner handles floating over satellite imagery, where a shadow is the only
 * thing that lifts a marker off a photograph.
 *
 * [ShadowSpec] mirrors the shadow column of §2.5 (`x y blur black/alpha`).
 * Compose's `Modifier.shadow(elevation, shape)` picks its own offset and blur
 * from the elevation, so the spec is consumed by the hand-drawn map-overlay
 * canvases; the tonal level is consumed by `Modifier.shadow` and containers.
 */
data class ShadowSpec(val offsetX: Dp, val offsetY: Dp, val blurRadius: Dp, val color: Color)

/** One step of the scale: the tonal elevation plus the shadow that goes with it. */
data class ElevationLevel(val tonal: Dp, val shadow: ShadowSpec?)

object Elevation {

    /** Cards in a list (separated by spacing, not shadow). */
    val E0 = ElevationLevel(tonal = 0.dp, shadow = null)

    /** Cards on a card. */
    val E1 = ElevationLevel(tonal = 1.dp, shadow = null)

    /** Bottom sheet (resting). */
    val E2 = ElevationLevel(tonal = 3.dp, shadow = null)

    /** Floating map toolbar. */
    val E3 = ElevationLevel(
        tonal = 6.dp,
        shadow = ShadowSpec(
            offsetX = 0.dp,
            offsetY = 1.dp,
            blurRadius = 3.dp,
            color = Color.Black.copy(alpha = 0.20f)
        )
    )

    /** FAB, central Add speed-dial. */
    val E4 = ElevationLevel(
        tonal = 8.dp,
        shadow = ShadowSpec(
            offsetX = 0.dp,
            offsetY = 4.dp,
            blurRadius = 8.dp,
            color = Color.Black.copy(alpha = 0.24f)
        )
    )

    /**
     * Dragged corner handle, modal bottom sheet (active). Map markers and
     * corner handles use E5 shadow + a white ring ([LocalLandColors] map
     * overlay tokens) so they remain visible against satellite imagery.
     */
    val E5 = ElevationLevel(
        tonal = 12.dp,
        shadow = ShadowSpec(
            offsetX = 0.dp,
            offsetY = 8.dp,
            blurRadius = 12.dp,
            color = Color.Black.copy(alpha = 0.26f)
        )
    )
}
