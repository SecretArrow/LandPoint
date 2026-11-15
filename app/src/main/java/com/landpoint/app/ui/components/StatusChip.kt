package com.landpoint.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.ui.theme.LandPointTheme
import com.landpoint.app.ui.theme.Spacing
import com.landpoint.app.ui.theme.parcelStatusVisual

/**
 * A parcel status chip (docs/DESIGN_SYSTEM.md §4 and §6.2).
 *
 * Always icon + label + color, in that order of redundancy: the color is the
 * chip's container fill at 12% alpha with the full-saturation color for the
 * icon and label, and the icons are distinct shapes so status still reads
 * without any color at all. Used by list rows, the detail hero and map popups.
 *
 * [status] is null only for unknown/legacy data — the §4 unknown row, drawn
 * with the outline color and a help icon and no label. Pass [label] in that
 * case so the chip still has something to announce.
 */
@Composable
fun StatusChip(status: LandStatus?, modifier: Modifier = Modifier, label: String? = null) {
    val visual = status.parcelStatusVisual()
    val resolvedLabel = label ?: status?.let { stringResource(it.labelRes) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = visual.containerColor,
        contentColor = visual.color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                imageVector = visual.icon,
                // The label carries the meaning; the icon is the redundant
                // colorblind-safe signal, not a second one to announce.
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            resolvedLabel?.let {
                Text(text = it, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StatusChipLightPreview() {
    LandPointTheme(darkMode = SettingsRepository.DarkMode.LIGHT) {
        PreviewColumn()
    }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF0F1511)
@Composable
private fun StatusChipDarkPreview() {
    LandPointTheme(darkMode = SettingsRepository.DarkMode.DARK) {
        PreviewColumn()
    }
}

@Composable
private fun PreviewColumn() {
    Column(
        modifier = Modifier.padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        StatusChip(LandStatus.ACTIVE)
        StatusChip(LandStatus.DISPUTED)
        StatusChip(LandStatus.SOLD)
        StatusChip(LandStatus.TRANSFERRED)
        StatusChip(status = null, label = "Unknown")
    }
}
