package com.landpoint.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.landpoint.app.data.model.LandStatus

/**
 * Fraction of the full-saturation status color used as a status chip's
 * container fill (docs/DESIGN_SYSTEM.md §4): the color is the chip's fill at
 * this alpha, while icon and label text use the full-saturation color.
 */
const val STATUS_CONTAINER_ALPHA = 0.12f

/**
 * Visual treatment of a parcel status: color + icon + label.
 *
 * Color never carries the meaning alone (the colorblind-safety rule, §1 and
 * §4): the chip always pairs the tint with a distinct icon shape (circle,
 * triangle, currency tag, arrows) and a text label, so a user who sees none
 * of the colors can still read the status.
 */
data class ParcelStatusVisual(
    /** Full-saturation status color; icon and label draw with this. */
    val color: Color,
    /** Chip container fill: [color] at [STATUS_CONTAINER_ALPHA]. */
    val containerColor: Color,
    val icon: ImageVector,
    /** Label resource; null for the unknown/no-status row, which ships no label. */
    @StringRes val labelRes: Int?
)

/**
 * The §4 mapping, pure so it is unit-testable and usable outside composition.
 *
 * [colors] supplies the semantic tokens (success / warning / info);
 * [colorScheme] supplies the roles that alias the M3 scheme (secondary for
 * transferred, outline for unknown). Pass the scheme you are rendering with —
 * in composition that is `MaterialTheme.colorScheme`.
 */
fun parcelStatusVisual(status: LandStatus?, colors: LandExtendedColors, colorScheme: ColorScheme): ParcelStatusVisual = when (status) {
    LandStatus.ACTIVE -> statusVisual(colors.success, status, Icons.Outlined.CheckCircle)
    LandStatus.DISPUTED -> statusVisual(colors.warning, status, Icons.Outlined.Warning)
    LandStatus.SOLD -> statusVisual(colors.info, status, Icons.Outlined.Sell)
    LandStatus.TRANSFERRED -> statusVisual(colorScheme.secondary, status, Icons.Outlined.SwapHoriz)
    null -> ParcelStatusVisual(
        color = colorScheme.outline,
        containerColor = colorScheme.outline.copy(alpha = STATUS_CONTAINER_ALPHA),
        icon = Icons.Outlined.HelpOutline,
        labelRes = null
    )
}

private fun statusVisual(color: Color, status: LandStatus, icon: ImageVector) = ParcelStatusVisual(
    color = color,
    containerColor = color.copy(alpha = STATUS_CONTAINER_ALPHA),
    icon = icon,
    labelRes = status.labelRes
)

/**
 * Theme-resolved form of the mapping for composables.
 *
 * Every parcel carries a status ([LandStatus.fromKey] defaults to ACTIVE), so
 * a null here means unknown or legacy data — the §4 unknown row. Render it
 * with an explicit [label][StatusChip.label] so the chip still announces
 * something.
 */
@Composable
@ReadOnlyComposable
fun LandStatus?.parcelStatusVisual(): ParcelStatusVisual = parcelStatusVisual(this, LocalLandColors.current, MaterialTheme.colorScheme)
