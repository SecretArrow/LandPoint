package com.landpoint.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.landpoint.app.R
import com.landpoint.app.ui.theme.Elevation
import com.landpoint.app.ui.theme.Motion
import com.landpoint.app.ui.theme.Pill
import com.landpoint.app.ui.theme.Spacing

/**
 * One entry of the Add speed-dial. The label names the way a parcel starts,
 * which is the vocabulary the corner picker uses again once the editor is open
 * (ADR-002: "GPS point / Walk boundary / Type coordinates / Tap map").
 */
private data class AddAction(val icon: ImageVector, val labelRes: Int)

private val addActions = listOf(
    AddAction(Icons.Outlined.MyLocation, R.string.dial_gps_point),
    AddAction(Icons.Outlined.Explore, R.string.dial_walk_boundary),
    AddAction(Icons.Outlined.Straighten, R.string.dial_type_coordinates),
    AddAction(Icons.Outlined.TouchApp, R.string.dial_tap_map)
)

/**
 * The central Add speed-dial (docs/DECISIONS.md ADR-002, docs/DESIGN_SYSTEM.md
 * §6.1, §2.5 E4).
 *
 * It sits above the bottom bar, centred between the four tabs, and expands
 * into the four ways a parcel starts. Every entry opens the same editor the
 * old single FAB opened — the editor auto-captures a GPS fix on open and its
 * corner picker owns the source choice from there — so the dial is a menu of
 * *names* for one destination, not four different flows. Pre-selecting a
 * source through a nav argument would touch the editor and picker for no
 * behavioural gain yet, and is deliberately deferred.
 *
 * [iconOnly] is the rail form (ADR-002: "the Add speed-dial docks at the
 * bottom of the rail"). A rail is ~80dp wide; labelled pills would widen the
 * whole side column and squeeze the content beside it, so there the entries
 * shrink to icon minis and speak through their content descriptions instead.
 */
@Composable
fun AddSpeedDial(onAdd: () -> Unit, modifier: Modifier = Modifier, iconOnly: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }

    // The back gesture's first job while the dial is open is to close it,
    // rather than leave the screen behind a menu the finger never chose.
    BackHandler(enabled = expanded) { expanded = false }

    val addLabel = stringResource(R.string.dial_add)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // The entries stack upward from the FAB and are part of the same slot,
        // so a taller stack simply lifts off the bar instead of pushing the
        // bar or the screen around.
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(Motion.tweenFast()) + expandVertically(Motion.tweenStandard()),
            exit = fadeOut(Motion.tweenFast()) + shrinkVertically(Motion.tweenStandard())
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                addActions.forEach { action ->
                    val label = stringResource(action.labelRes)
                    if (iconOnly) {
                        SmallFloatingActionButton(
                            onClick = {
                                expanded = false
                                onAdd()
                            },
                            modifier = Modifier.semantics { contentDescription = label },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = Elevation.E1.tonal)
                        ) {
                            Icon(action.icon, contentDescription = null)
                        }
                    } else {
                        Surface(
                            // Clipped before the click: the pill's ripple would
                            // otherwise fill the rectangular node bounds and
                            // spill past the rounded ends.
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.Pill)
                                .clickable(
                                    onClick = {
                                        expanded = false
                                        onAdd()
                                    }
                                ),
                            shape = MaterialTheme.shapes.Pill,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = Elevation.E1.tonal
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = Spacing.lg,
                                    vertical = Spacing.md
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(action.icon, contentDescription = null)
                                Text(label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            // The merged FAB semantics can drop the text-less icon, which leaves
            // TalkBack an unlabelled button and the e2e flows nothing to tap.
            // State the label explicitly, as the old list FAB did.
            modifier = Modifier.semantics { contentDescription = addLabel },
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = Elevation.E4.tonal)
        ) {
            // One glyph that turns is cheaper to follow than a swap: the Add
            // cross rotating into an X reads as "open, then close" with no new
            // icon to find. Faster than a sheet, because it is a press state.
            val rotation by animateFloatAsState(
                targetValue = if (expanded) 45f else 0f,
                animationSpec = Motion.tweenFast(),
                label = "dialRotation"
            )
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
        }
    }
}
