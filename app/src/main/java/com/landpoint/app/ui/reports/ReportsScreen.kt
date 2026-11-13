package com.landpoint.app.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.landpoint.app.R
import com.landpoint.app.data.model.Land
import com.landpoint.app.util.AreaFormat

/**
 * The export desk: every way this app's records can leave the phone, on one
 * screen, each through the system file picker so the destination is always the
 * user's own choice. Nothing here uploads anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onOpenSettings: () -> Unit) {
    val viewModel: ReportsViewModel = viewModel(factory = ReportsViewModel.FACTORY)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pickingLand by remember { mutableStateOf(false) }
    var pendingLand by remember { mutableStateOf<Land?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val land = pendingLand
        if (uri != null && land != null) viewModel.exportSinglePdf(uri, land)
        pendingLand = null
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportCsv) }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportJson) }
    val gpxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri -> uri?.let(viewModel::exportGpx) }
    val kmlLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.google-earth.kml+xml")
    ) { uri -> uri?.let(viewModel::exportKml) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_reports)) }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.reports_summary_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        SummaryLine(
                            stringResource(R.string.home_stat_parcels),
                            state.parcelCount.toString()
                        )
                        SummaryLine(
                            stringResource(R.string.home_stat_owners),
                            state.ownerCount.toString()
                        )
                        SummaryLine(
                            stringResource(R.string.home_stat_total_area),
                            stringResource(
                                state.areaUnit.valueRes,
                                AreaFormat.value(state.totalAreaSqm, state.areaUnit)
                            )
                        )
                        if (state.disputedCount > 0) {
                            SummaryLine(
                                stringResource(R.string.home_stat_disputed),
                                state.disputedCount.toString()
                            )
                        }
                        if (state.soldCount > 0) {
                            SummaryLine(
                                stringResource(R.string.land_status_sold),
                                state.soldCount.toString()
                            )
                        }
                        if (state.transferredCount > 0) {
                            SummaryLine(
                                stringResource(R.string.land_status_transferred),
                                state.transferredCount.toString()
                            )
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.reports_section_export)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reports_export_pdf_one)) },
                    supportingContent = { Text(stringResource(R.string.reports_export_pdf_one_hint)) },
                    trailingContent = {
                        if (state.isBusy) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(onClick = { pickingLand = true }, enabled = state.parcelCount > 0) {
                                Text(stringResource(R.string.action_more))
                            }
                        }
                    }
                )
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { csvLauncher.launch("landpoint-locations.csv") },
                            enabled = !state.isBusy && state.parcelCount > 0
                        ) { Text(stringResource(R.string.settings_export_csv)) }
                        OutlinedButton(
                            onClick = { jsonLauncher.launch("landpoint-backup.json") },
                            enabled = !state.isBusy && state.parcelCount > 0
                        ) { Text(stringResource(R.string.settings_backup_json)) }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { gpxLauncher.launch("landpoint-locations.gpx") },
                            enabled = !state.isBusy && state.parcelCount > 0
                        ) { Text(stringResource(R.string.settings_export_gpx)) }
                        OutlinedButton(
                            onClick = { kmlLauncher.launch("landpoint-locations.kml") },
                            enabled = !state.isBusy && state.parcelCount > 0
                        ) { Text(stringResource(R.string.settings_export_kml)) }
                    }
                }
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SectionTitle(stringResource(R.string.reports_section_backup))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_section_backup)) },
                    supportingContent = { Text(stringResource(R.string.settings_backup_archive_hint)) },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                TextButton(onClick = onOpenSettings, modifier = Modifier.padding(start = 16.dp)) {
                    Text(stringResource(R.string.reports_open_backup))
                }
            }
        }
    }

    if (pickingLand) {
        AlertDialog(
            onDismissRequest = { pickingLand = false },
            title = { Text(stringResource(R.string.reports_pick_land)) },
            text = {
                var lands by remember { mutableStateOf<List<Land>>(emptyList()) }
                LaunchedEffect(Unit) { lands = viewModel.allLands() }
                Column {
                    if (lands.isEmpty()) {
                        Text(stringResource(R.string.reports_no_lands))
                    }
                    lands.take(12).forEach { land ->
                        TextButton(
                            onClick = {
                                pickingLand = false
                                pendingLand = land
                                pdfLauncher.launch(viewModel.suggestedName(land))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(land.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingLand = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}
