package com.landpoint.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.landpoint.app.R
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.util.AreaFormat

/**
 * The first screen: what this phone holds, and what happened to it lately.
 * Every number here is one tap from its list — the card is a door, not a
 * dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenLand: (String) -> Unit,
    onNewLand: () -> Unit,
    onOpenOwners: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.FACTORY)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.parcelCount > 0) {
                val fabLabel = stringResource(R.string.list_fab_save_land)
                ExtendedFloatingActionButton(
                    onClick = onNewLand,
                    // Same reasoning as the list FAB: the label must reach the
                    // accessibility tree.
                    modifier = Modifier.semantics { contentDescription = fabLabel },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(fabLabel) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.parcelCount == 0) {
                item {
                    EmptyHome(
                        onNewLand = onNewLand,
                        onOpenOwners = onOpenOwners
                    )
                }
            } else {
                item {
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
                }
                item {
                    SectionTitle(stringResource(R.string.home_recent_lands))
                }
                items(state.recentLands.size) { index ->
                    val row = state.recentLands[index]
                    RecentLandRow(row = row, areaUnit = state.areaUnit, onClick = { onOpenLand(row.id) })
                }
                if (state.recentActivity.isNotEmpty()) {
                    item {
                        SectionTitle(stringResource(R.string.home_recent_activity))
                    }
                    items(state.recentActivity.size) { index ->
                        val entry = state.recentActivity[index]
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
                                Text(
                                    java.text.DateFormat
                                        .getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
                                        .format(java.util.Date(entry.timestamp))
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(onNewLand: () -> Unit, onOpenOwners: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Map,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            val emptyCtaLabel = stringResource(R.string.list_fab_save_land)
            ExtendedFloatingActionButton(
                onClick = onNewLand,
                modifier = Modifier.semantics { contentDescription = emptyCtaLabel },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(emptyCtaLabel) }
            )
            androidx.compose.material3.TextButton(onClick = onOpenOwners) {
                Text(stringResource(R.string.home_empty_add_owner))
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun RecentLandRow(row: HomeLandRow, areaUnit: com.landpoint.app.data.SettingsRepository.AreaUnit, onClick: () -> Unit) {
    val area = row.areaSqm?.let {
        stringResource(areaUnit.valueRes, AreaFormat.value(it, areaUnit))
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                Icons.Outlined.Landscape,
                contentDescription = null,
                tint = if (row.status == LandStatus.DISPUTED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        },
        headlineContent = { Text(row.name) },
        supportingContent = {
            Text(
                listOfNotNull(row.ownerName, area).joinToString(" · ").ifBlank {
                    stringResource(R.string.home_unassigned)
                }
            )
        }
    )
}
