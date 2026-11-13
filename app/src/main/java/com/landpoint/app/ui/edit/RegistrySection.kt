package com.landpoint.app.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.landpoint.app.R
import com.landpoint.app.data.model.LandRights
import com.landpoint.app.data.model.LandStatus
import java.text.DateFormat
import java.util.Date

/**
 * Registry bookkeeping for a parcel: who holds it, under what right, in what
 * state, and which papers say so. Every field is optional — a boundary drawn
 * in a field with no papers yet is as valid a record as a certified one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrySection(
    state: LandEditUiState,
    onOwnerPicked: (String?) -> Unit,
    onLandRightChange: (String) -> Unit,
    onStatusPicked: (LandStatus) -> Unit,
    onCertificateChange: (String) -> Unit,
    onSpptChange: (String) -> Unit,
    onAcquisitionDate: (Long?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ---- Owner ------------------------------------------------------
        OwnerPicker(
            owners = state.owners,
            selectedOwnerId = state.ownerId,
            onPicked = onOwnerPicked
        )

        // ---- Land right (free text with presets) ------------------------
        var rightMenuOpen by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = rightMenuOpen,
            onExpandedChange = { rightMenuOpen = it }
        ) {
            OutlinedTextField(
                value = state.landRight,
                onValueChange = onLandRightChange,
                label = { Text(stringResource(R.string.field_land_right)) },
                placeholder = { Text(stringResource(R.string.field_land_right_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
            )
            ExposedDropdownMenu(expanded = rightMenuOpen, onDismissRequest = { rightMenuOpen = false }) {
                LandRights.PRESETS.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset) },
                        onClick = {
                            onLandRightChange(preset)
                            rightMenuOpen = false
                        }
                    )
                }
            }
        }

        // ---- Status chips ------------------------------------------------
        Text(
            stringResource(R.string.field_status),
            style = MaterialTheme.typography.labelMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LandStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.status == status.key,
                    onClick = { onStatusPicked(status) },
                    label = { Text(stringResource(status.labelRes)) }
                )
            }
        }

        // ---- Certificate / SPPT -----------------------------------------
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.certificateNumber,
                onValueChange = onCertificateChange,
                label = { Text(stringResource(R.string.field_certificate_number)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.spptNumber,
                onValueChange = onSpptChange,
                label = { Text(stringResource(R.string.field_sppt_number)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---- Acquisition date --------------------------------------------
        var datePickerOpen by remember { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { datePickerOpen = true }) {
                Text(
                    state.acquisitionDate?.let {
                        stringResource(
                            R.string.field_acquisition_date_set,
                            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))
                        )
                    } ?: stringResource(R.string.field_acquisition_date)
                )
            }
            if (state.acquisitionDate != null) {
                TextButton(onClick = { onAcquisitionDate(null) }) {
                    Text(stringResource(R.string.field_acquisition_date_clear))
                }
            }
        }
        if (datePickerOpen) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = state.acquisitionDate
            )
            DatePickerDialog(
                onDismissRequest = { datePickerOpen = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onAcquisitionDate(pickerState.selectedDateMillis)
                            datePickerOpen = false
                        }
                    ) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { datePickerOpen = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerPicker(owners: List<com.landpoint.app.data.model.Owner>, selectedOwnerId: String?, onPicked: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = owners.firstOrNull { it.id == selectedOwnerId }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected?.fullName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_owner)) },
            placeholder = { Text(stringResource(R.string.field_owner_none)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.field_owner_none)) },
                onClick = {
                    onPicked(null)
                    open = false
                }
            )
            owners.forEach { owner ->
                DropdownMenuItem(
                    text = { Text(owner.fullName) },
                    onClick = {
                        onPicked(owner.id)
                        open = false
                    }
                )
            }
        }
    }
}
