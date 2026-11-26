package com.landpoint.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.landpoint.app.R
import com.landpoint.app.ui.theme.Spacing
import com.landpoint.app.util.AreaFormat
import java.text.DateFormat
import java.util.Date

/**
 * The Parcels tab's summary header — the old Home dashboard, moved here by
 * ADR-002 when the tab set went to Map / Parcels / Tools / Settings.
 *
 * It answers "what does this phone hold, and what happened to it lately" above
 * the list itself: the four counts as stat cards, then the audit trail. Two
 * deliberate differences from its old Home form:
 *
 *  - "Recent parcels" is not repeated here, because the full parcel list starts
 *    right under this header — five of its rows would be on screen twice.
 *  - It only renders when the list is non-empty and no search filter is active;
 *    the list's own empty and no-matches states are the right words for those
 *    moments, and zeroed stats next to them would only be noise.
 *
 * The stat cards stay tappable the way they were on Home: owners opens the
 * owners list (a former tab), the area and disputed counts open the reports
 * screen where those numbers get their context.
 */
@Composable
fun ParcelsHeader(onOpenOwners: () -> Unit, onOpenReports: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.FACTORY)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        StatGrid(
            parcelCount = state.parcelCount,
            ownerCount = state.ownerCount,
            disputedCount = state.disputedCount,
            totalArea = stringResource(
                state.areaUnit.valueRes,
                AreaFormat.value(state.totalAreaSqm, state.areaUnit)
            ),
            onOpenOwners = onOpenOwners,
            onOpenReports = onOpenReports
        )
        if (state.recentActivity.isNotEmpty()) {
            Text(
                stringResource(R.string.home_recent_activity),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.sm)
            )
            state.recentActivity.forEach { entry ->
                ActivityRow(entry)
            }
        }
    }
}

@Composable
private fun StatGrid(
    parcelCount: Int,
    ownerCount: Int,
    disputedCount: Int,
    totalArea: String,
    onOpenOwners: () -> Unit,
    onOpenReports: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                icon = Icons.Outlined.Landscape,
                label = stringResource(R.string.home_stat_parcels),
                value = parcelCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Outlined.People,
                label = stringResource(R.string.home_stat_owners),
                value = ownerCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onOpenOwners
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                icon = Icons.Outlined.Map,
                label = stringResource(R.string.home_stat_total_area),
                value = totalArea,
                modifier = Modifier.weight(1f),
                onClick = onOpenReports
            )
            StatCard(
                icon = Icons.Outlined.Gavel,
                label = stringResource(R.string.home_stat_disputed),
                value = disputedCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onOpenReports
            )
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * One line of the audit trail. The timestamp is rendered through the locale's
 * own short date-time (not the monospace coordinate style) — it is a moment,
 * not a measurement.
 */
@Composable
private fun ActivityRow(entry: HomeAuditRow) {
    ListItem(
        leadingContent = {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(stringResource(entry.actionRes, entry.detail)) },
        supportingContent = {
            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timestamp)))
        }
    )
}
