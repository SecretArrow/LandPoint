package com.landpoint.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.landpoint.app.R
import com.landpoint.app.data.LandRepository
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.Land
import com.landpoint.app.ui.container
import com.landpoint.app.util.AppStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The bin screen's state: what is in the bin right now, whether work is running,
 * and the one message the screen is allowed to be showing.
 */
data class RecentlyDeletedUiState(
    /** Binned parcels, the most recently deleted first — the query's own order. */
    val bin: List<Land> = emptyList(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val message: String? = null,
    val areaUnit: SettingsRepository.AreaUnit = SettingsRepository.AreaUnit.SQM
) {
    /** True once the bin is known to be empty, so the empty state can reassure. */
    val isEmpty: Boolean get() = !isLoading && bin.isEmpty()
}

/**
 * Drives the recently-deleted bin: it lists what is recoverable, puts a parcel
 * back among the live ones, and finishes what a delete started for good.
 *
 * The screen also asks for a one-off sweep of everything past its thirty-day
 * window on entry, so what the user sees is up to date even if the daily worker
 * has not had its turn yet.
 */
class RecentlyDeletedViewModel(private val repository: LandRepository, private val strings: AppStrings, settings: SettingsRepository) :
    ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecentlyDeletedUiState> = combine(
        repository.recentlyDeleted(),
        settings.areaUnit,
        combine(busy, message) { isBusy, msg -> isBusy to msg }
    ) { bin, areaUnit, task ->
        val (isBusy, msg) = task
        RecentlyDeletedUiState(
            bin = bin,
            isLoading = false,
            isBusy = isBusy,
            message = msg,
            areaUnit = areaUnit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecentlyDeletedUiState()
    )

    init {
        // Fire-and-forget, so the user never waits on a sweep to see the bin:
        // anything past its retention window vanishes from the list the moment
        // the query re-emits, which is the point rather than a race to guard.
        viewModelScope.launch {
            runCatching { repository.purgeExpired() }
        }
    }

    /** Puts a binned parcel back among the live ones. */
    fun restore(id: String) {
        viewModelScope.launch {
            busy.value = true
            try {
                repository.restore(id)
                message.value = strings.get(R.string.msg_restored)
            } finally {
                busy.value = false
            }
        }
    }

    /**
     * Erases one binned parcel for good — row, photo rows and photo files.
     *
     * The screen confirms before calling this; the ViewModel itself does not
     * keep a "pending confirmation", because a confirmation is presentation
     * state and dies with the screen.
     */
    fun purge(id: String) {
        viewModelScope.launch {
            busy.value = true
            try {
                repository.purge(id)
                message.value = strings.get(R.string.msg_purged_forever_one)
            } finally {
                busy.value = false
            }
        }
    }

    /**
     * Erases every parcel currently in the bin.
     *
     * The ids are read once from the state: parcels binned by another screen
     * *after* the user tapped "Empty bin" did not fit the confirmation they
     * gave, so only what was on screen when they confirmed goes.
     */
    fun emptyBin() {
        val ids = uiState.value.bin.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            busy.value = true
            try {
                ids.forEach { repository.purge(it) }
                message.value = strings.plural(R.plurals.msg_purged_forever, ids.size)
            } finally {
                busy.value = false
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = container()
                RecentlyDeletedViewModel(c.repository, c.strings, c.settings)
            }
        }
    }
}
