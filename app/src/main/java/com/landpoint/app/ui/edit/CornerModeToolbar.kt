package com.landpoint.app.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The floating bottom toolbar that names what the next touch on the map is
 * going to do.
 *
 * A Material 3 [SingleChoiceSegmentedButtonRow] switches between the four
 * [CornerPickerMode]s — add, move, insert, delete — and a single line of hint
 * text below it says how the active mode is used. The hint sits in a polite
 * live region so a screen reader reads it the moment the mode changes, the way
 * a sighted user reads it without taking their eyes off the map.
 *
 * The toolbar is additive: the existing add-source buttons (GPS, coordinates,
 * bearing, neighbour) remain available within ADD mode, and wiring MOVE,
 * INSERT and DELETE to actually change the picker's behaviour is a follow-up.
 * This composable only owns the mode's presentation — the state itself lives
 * in [CornerPickerScreen].
 *
 * Each segmented button is held to a 48 dp minimum height so a fingertip can
 * switch modes at the bottom of the glass where the thumb already is, rather
 * than at the 40 dp Material 3 default which is a lottery on a moving bus.
 */
@Composable
fun CornerModeToolbar(currentMode: CornerPickerMode, onModeChange: (CornerPickerMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = CornerPickerMode.entries
    val modeLabel = stringResource(currentMode.labelRes)
    val hintText = stringResource(currentMode.hintRes)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == currentMode,
                        onClick = { onModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                        icon = { Icon(mode.icon, contentDescription = null) },
                        selectedIcon = { Icon(mode.icon, contentDescription = null) },
                        label = {
                            Text(
                                stringResource(mode.labelRes),
                                maxLines = 1
                            )
                        }
                    )
                }
            }
            Text(
                text = hintText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "$modeLabel: $hintText"
                }
            )
        }
    }
}
