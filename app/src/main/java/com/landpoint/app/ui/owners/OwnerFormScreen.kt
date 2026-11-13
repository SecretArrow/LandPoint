package com.landpoint.app.ui.owners

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.landpoint.app.R
import java.io.File

/**
 * Add or edit an owner. Validation is inline and phrased for people: a field
 * says what is wrong with its content, not that "an error occurred".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerFormScreen(ownerId: String?, onDone: () -> Unit, onCancel: () -> Unit) {
    key(ownerId) {
        val viewModel: OwnerFormViewModel = viewModel(factory = OwnerFormViewModel.FACTORY)
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        var unsavedChanges by remember { mutableStateOf(false) }
        var confirmExit by remember { mutableStateOf(false) }

        // Camera capture goes through the system camera app, same as the land
        // editor: no CAMERA permission of our own to ask for, and the user
        // keeps whatever camera app they prefer.
        var cameraTarget by remember { mutableStateOf<Pair<File, Uri>?>(null) }
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val target = cameraTarget
            if (ok && target != null) {
                unsavedChanges = true
                viewModel.requestPersistCapture(target.first)
            } else {
                target?.first?.delete()
            }
        }
        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                unsavedChanges = true
                viewModel.requestImportFromUri(uri)
            }
        }

        val tryExit: () -> Unit = {
            if (unsavedChanges) confirmExit = true else onCancel()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                if (state.isEdit) R.string.owner_edit_title else R.string.owner_new_title
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = tryExit) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_cancel)
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = { viewModel.save(onDone) },
                            enabled = !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.action_save))
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.photoPath != null) {
                        AsyncImage(
                            model = java.io.File(state.photoPath!!),
                            contentDescription = stringResource(R.string.owner_photo_description),
                            modifier = Modifier.size(72.dp)
                        )
                    } else {
                        OwnerInitials(state.fullName.ifBlank { "?" }, modifier = Modifier.size(72.dp))
                    }
                    Column {
                        OutlinedButton(onClick = {
                            val target = viewModel.newCameraTarget()
                            cameraTarget = target
                            cameraLauncher.launch(target.second)
                        }) {
                            Text(stringResource(R.string.corner_photo_take))
                        }
                        TextButton(onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Text(stringResource(R.string.corner_photo_pick))
                        }
                    }
                }

                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = {
                        unsavedChanges = true
                        viewModel.setFullName(it)
                    },
                    label = { Text(stringResource(R.string.owner_field_name)) },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { res -> { Text(stringResource(res)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.nik,
                    onValueChange = {
                        unsavedChanges = true
                        viewModel.setNik(it)
                    },
                    label = { Text(stringResource(R.string.owner_field_nik)) },
                    supportingText = state.nikError?.let { res -> { Text(stringResource(res)) } }
                        ?: state.duplicateNikWarning?.let { res -> { Text(stringResource(res)) } }
                        ?: {
                            Text(
                                stringResource(R.string.owner_field_nik_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                    isError = state.nikError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.address,
                    onValueChange = {
                        unsavedChanges = true
                        viewModel.setAddress(it)
                    },
                    label = { Text(stringResource(R.string.owner_field_address)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {
                        unsavedChanges = true
                        viewModel.setPhone(it)
                    },
                    label = { Text(stringResource(R.string.owner_field_phone)) },
                    isError = state.phoneError != null,
                    supportingText = state.phoneError?.let { res -> { Text(stringResource(res)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = {
                        unsavedChanges = true
                        viewModel.setEmail(it)
                    },
                    label = { Text(stringResource(R.string.owner_field_email)) },
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { res -> { Text(stringResource(res)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (confirmExit) {
            AlertDialog(
                onDismissRequest = { confirmExit = false },
                title = { Text(stringResource(R.string.owner_unsaved_title)) },
                text = { Text(stringResource(R.string.owner_unsaved_body)) },
                confirmButton = {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_close)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmExit = false }) {
                        Text(stringResource(R.string.owner_keep_editing))
                    }
                }
            )
        }
    }
}
