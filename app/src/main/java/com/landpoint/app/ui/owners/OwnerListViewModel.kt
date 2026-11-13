package com.landpoint.app.ui.owners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.landpoint.app.AppContainer
import com.landpoint.app.data.model.Owner
import com.landpoint.app.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class OwnerSort(val key: String) {
    NAME,
    PARCELS,
    NEWEST;

    companion object {
        fun fromKey(key: String?): OwnerSort = entries.firstOrNull { it.key == key } ?: NAME
    }
}

data class OwnerListUiState(val query: String = "", val sort: OwnerSort = OwnerSort.NAME, val owners: List<Owner> = emptyList()) {
    val filtered: List<Owner>
        get() {
            val q = query.trim().lowercase()
            val matched = if (q.isEmpty()) {
                owners
            } else {
                owners.filter { owner ->
                    owner.fullName.lowercase().contains(q) ||
                        owner.nik?.contains(q) == true ||
                        owner.address.lowercase().contains(q) ||
                        owner.phone?.contains(q) == true
                }
            }
            return when (sort) {
                OwnerSort.NAME -> matched.sortedBy { it.fullName.lowercase() }
                OwnerSort.PARCELS -> matched.sortedByDescending { it.parcelCount }
                OwnerSort.NEWEST -> matched.sortedByDescending { it.createdAt }
            }
        }
}

class OwnerListViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(OwnerSort.NAME)
    val sort: StateFlow<OwnerSort> = _sort.asStateFlow()

    val uiState: StateFlow<OwnerListUiState> = combine(
        repository.observeOwners(),
        _query,
        _sort
    ) { owners, query, sort ->
        OwnerListUiState(query = query, sort = sort, owners = owners)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnerListUiState())

    fun setQuery(value: String) {
        _query.update { value }
    }

    fun setSort(sort: OwnerSort) {
        _sort.update { sort }
    }

    companion object {
        val FACTORY = viewModelFactory {
            initializer { OwnerListViewModel(container()) }
        }
    }
}
