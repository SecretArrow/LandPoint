package com.landpoint.app.ui.owners

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.landpoint.app.AppContainer
import com.landpoint.app.data.model.Land
import com.landpoint.app.data.model.Owner
import com.landpoint.app.ui.container
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OwnerDetailUiState(val owner: Owner? = null, val lands: List<Land> = emptyList())

class OwnerDetailViewModel(private val container: AppContainer, savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repository = container.repository

    private val ownerId: String = checkNotNull(savedStateHandle["ownerId"])

    val uiState: StateFlow<OwnerDetailUiState> = combine(
        repository.observeOwner(ownerId),
        repository.observeLandsOfOwner(ownerId)
    ) { owner, lands ->
        OwnerDetailUiState(owner = owner, lands = lands)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnerDetailUiState())

    /** Deletes the owner; their parcels stay and become unassigned. */
    fun deleteOwner(onDone: () -> Unit) {
        val owner = uiState.value.owner ?: return
        viewModelScope.launch {
            repository.deleteOwner(owner)
            onDone()
        }
    }

    companion object {
        val FACTORY = viewModelFactory {
            initializer { OwnerDetailViewModel(container(), createSavedStateHandle()) }
        }
    }
}
