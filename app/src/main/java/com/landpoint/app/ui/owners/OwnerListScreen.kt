package com.landpoint.app.ui.owners

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.landpoint.app.R

/**
 * The owner book: searchable, sortable, one row per person with their parcel
 * count. Everything here is local — there is no directory to sync with and
 * none is pretended at.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerListScreen(onOpenOwner: (String) -> Unit, onNewOwner: () -> Unit) {
    val viewModel: OwnerListViewModel = viewModel(factory = OwnerListViewModel.FACTORY)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_owners)) },
                actions = {
                    if (searchOpen) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::setQuery,
                            placeholder = { Text(stringResource(R.string.owners_search_hint)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (state.query.isEmpty()) {
                                        searchOpen = false
                                    } else {
                                        viewModel.setQuery("")
                                    }
                                }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.list_search_clear)
                                    )
                                }
                            }
                        )
                    } else {
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search)
                            )
                        }
                        Box {
                            TextButton(onClick = { sortMenuOpen = true }) {
                                Text(stringResource(R.string.action_sort))
                            }
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                OwnerSort.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    when (sort) {
                                                        OwnerSort.NAME -> R.string.sort_name_asc
                                                        OwnerSort.PARCELS -> R.string.owners_sort_parcels
                                                        OwnerSort.NEWEST -> R.string.sort_date_desc
                                                    }
                                                )
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSort(sort)
                                            sortMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val fabLabel = stringResource(R.string.owners_fab_add)
            ExtendedFloatingActionButton(
                onClick = onNewOwner,
                // Like the list FAB: state the label explicitly so the
                // accessibility tree carries it.
                modifier = Modifier.semantics { contentDescription = fabLabel },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(fabLabel) }
            )
        }
    ) { padding ->
        if (state.owners.isEmpty()) {
            EmptyOwners(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val rows = state.filtered
                if (rows.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.owners_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                items(rows.size) { index ->
                    val owner = rows[index]
                    ListItem(
                        modifier = Modifier.clickable { onOpenOwner(owner.id) },
                        leadingContent = { OwnerInitials(owner.fullName) },
                        headlineContent = { Text(owner.fullName, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            val summary = owner.nik?.let { "NIK $it" }
                                ?: owner.phone
                                ?: owner.address.ifBlank { null }
                            summary?.let { Text(it, maxLines = 1) }
                        },
                        trailingContent = {
                            Text(
                                pluralStringResource(R.plurals.owners_parcel_count, owner.parcelCount, owner.parcelCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    }
}

/** The avatar before there is a photo: initials in a coloured circle. */
@Composable
fun OwnerInitials(fullName: String, modifier: Modifier = Modifier) {
    val initials = fullName.split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
    Box(
        modifier = modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun EmptyOwners(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.People,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.owners_empty_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            stringResource(R.string.owners_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
