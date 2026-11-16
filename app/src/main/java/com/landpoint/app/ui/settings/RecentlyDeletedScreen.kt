package com.landpoint.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.landpoint.app.R
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.Land
import com.landpoint.app.ui.theme.Spacing
import com.landpoint.app.util.AreaFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The bin: parcels the user deleted but that can still come back.
 *
 * Everything here reads straight off the repository's bin flow, so a restore or
 * a purge updates the list reactively — there is no separate "refresh" step to
 * forget. Deleting forever and emptying the bin both ask first, because unlike
 * the list screen's delete there is no undo behind this one: the photo files go
 * with the row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(onBack: () -> Unit, viewModel: RecentlyDeletedViewModel = viewModel(factory = RecentlyDeletedViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    var purgeTarget by remember { mutableStateOf<Land?>(null) }
    var showEmptyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    purgeTarget?.let { land ->
        PurgeForeverDialog(
            onDismiss = { purgeTarget = null },
            onConfirm = {
                purgeTarget = null
                viewModel.purge(land.id)
            }
        )
    }

    if (showEmptyDialog) {
        EmptyBinDialog(
            onDismiss = { showEmptyDialog = false },
            onConfirm = {
                showEmptyDialog = false
                viewModel.emptyBin()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recently_deleted_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    // An empty bin has nothing to empty; hiding the action keeps
                    // the offer honest rather than confirming a no-op.
                    if (!state.isEmpty) {
                        IconButton(onClick = { showEmptyDialog = true }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.action_empty_bin)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                state.isEmpty -> BinEmptyState(modifier = Modifier.fillMaxSize())

                else -> BinList(
                    state = state,
                    onRestore = viewModel::restore,
                    onAskPurge = { purgeTarget = it }
                )
            }
        }
    }
}

/** Every binned parcel, the most recently deleted first. */
@Composable
private fun BinList(state: RecentlyDeletedUiState, onRestore: (String) -> Unit, onAskPurge: (Land) -> Unit) {
    // From the configuration rather than the default locale, so the date a
    // parcel was deleted follows the language the app is showing.
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = Spacing.lg,
            top = Spacing.md,
            end = Spacing.lg,
            bottom = Spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(state.bin, key = { it.id }) { land ->
            BinRow(
                land = land,
                deletedLabel = stringResource(R.string.bin_deleted_on, dateFormat.format(Date(land.deletedAt ?: 0L))),
                areaUnit = state.areaUnit,
                onRestore = { onRestore(land.id) },
                onAskPurge = { onAskPurge(land) }
            )
        }
    }
}

/**
 * One binned parcel: what it was called, when it was deleted, and the two ways
 * out. Restore is the prominent action — it is the reason the bin exists;
 * deleting forever sits beside it as plain text, because it is the one choice
 * in this screen that cannot be taken back.
 */
@Composable
private fun BinRow(land: Land, deletedLabel: String, areaUnit: SettingsRepository.AreaUnit, onRestore: () -> Unit, onAskPurge: () -> Unit) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                land.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val area = land.areaSqm
            if (land.isPolygon && area != null && area > 0.0) {
                Text(
                    stringResource(areaUnit.valueRes, AreaFormat.value(area, areaUnit)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                deletedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(stringResource(R.string.action_restore))
                }
                TextButton(onClick = onAskPurge) {
                    Text(
                        stringResource(R.string.action_delete_forever),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** The bin is empty, and that is good news rather than a loss to explain. */
@Composable
private fun BinEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(horizontal = Spacing.xxl, vertical = 40.dp)
        ) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                stringResource(R.string.recently_deleted_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.recently_deleted_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** The one dialog on this screen whose confirm button does something final. */
@Composable
private fun PurgeForeverDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bin_purge_dialog_title)) },
        text = { Text(stringResource(R.string.bin_purge_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.action_delete_forever),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Confirms emptying the bin, since what it deletes does not come back. */
@Composable
private fun EmptyBinDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bin_empty_dialog_title)) },
        text = { Text(stringResource(R.string.bin_empty_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.action_delete_forever),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
