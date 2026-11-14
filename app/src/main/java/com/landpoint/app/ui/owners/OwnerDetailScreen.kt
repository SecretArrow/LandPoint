package com.landpoint.app.ui.owners

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.landpoint.app.R
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.util.AreaFormat
import java.io.File
import java.text.DateFormat

/**
 * One person and everything they hold. The parcel list is the point of the
 * screen: it answers "which of my records belong to this one?" without a trip
 * through the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDetailScreen(onBack: () -> Unit, onEdit: (String) -> Unit, onOpenLand: (String) -> Unit) {
    val viewModel: OwnerDetailViewModel = viewModel(factory = OwnerDetailViewModel.FACTORY)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.owner?.fullName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    state.owner?.let { owner ->
                        IconButton(onClick = { onEdit(owner.id) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.action_edit)
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val owner = state.owner
        if (owner == null) {
            Text(
                stringResource(R.string.owner_missing),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (owner.photoPath != null) {
                        AsyncImage(
                            model = File(owner.photoPath!!),
                            contentDescription = stringResource(R.string.owner_photo_description),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        OwnerInitials(owner.fullName, modifier = Modifier.size(72.dp))
                    }
                    Column {
                        Text(owner.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        owner.nik?.let {
                            Text(
                                stringResource(R.string.owner_nik_shown, it),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (owner.address.isNotBlank()) {
                        DetailLine(stringResource(R.string.owner_field_address), owner.address)
                    }
                    owner.phone?.let { DetailLine(stringResource(R.string.owner_field_phone), it) }
                    owner.email?.let { DetailLine(stringResource(R.string.owner_field_email), it) }
                    DetailLine(
                        stringResource(R.string.owner_member_since),
                        DateFormat.getDateInstance(DateFormat.MEDIUM)
                            .format(java.util.Date(owner.createdAt))
                    )
                }
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    pluralStringResource(
                        R.plurals.owners_parcel_count,
                        state.lands.size,
                        state.lands.size
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.lands.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.owner_no_lands),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.lands.size) { index ->
                    val land = state.lands[index]
                    ListItem(
                        modifier = Modifier.clickable { onOpenLand(land.id) },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Landscape,
                                contentDescription = null,
                                tint = if (land.landStatus == LandStatus.DISPUTED) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        },
                        headlineContent = { Text(land.name) },
                        supportingContent = {
                            land.areaSqm?.let {
                                Text(
                                    stringResource(
                                        R.string.value_area_sqm,
                                        AreaFormat.value(it, SettingsRepository.AreaUnit.SQM)
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (confirmDelete && state.owner != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.owner_delete_title, state.owner!!.fullName)) },
            text = { Text(stringResource(R.string.owner_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteOwner(onBack)
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
