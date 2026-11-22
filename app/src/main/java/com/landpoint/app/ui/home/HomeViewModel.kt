package com.landpoint.app.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.landpoint.app.AppContainer
import com.landpoint.app.R
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.AuditActions
import com.landpoint.app.data.model.AuditEntry
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.ui.container
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One row of the recent-activity list, already shortened for the header. */
data class HomeAuditRow(val id: Long, @StringRes val actionRes: Int, val detail: String, val timestamp: Long)

data class HomeUiState(
    val parcelCount: Int = 0,
    val ownerCount: Int = 0,
    val disputedCount: Int = 0,
    val totalAreaSqm: Double = 0.0,
    val areaUnit: SettingsRepository.AreaUnit = SettingsRepository.AreaUnit.SQM,
    val recentActivity: List<HomeAuditRow> = emptyList()
)

/**
 * The Parcels tab header's state — the old Home tab's dashboard, repurposed
 * per the Phase 5 file table (docs/IMPLEMENTATION_PLAN.md) after ADR-002
 * dropped the Home tab. The class keeps its name and package so the
 * repurpose is one reviewable diff rather than a rename across three layers.
 *
 * What it stopped doing: "recent parcels" is gone, because the Parcels tab is
 * the full parcel list — repeating five of its rows in the header above them
 * would show the same information twice on one screen. What it still serves:
 * the four counts the old Home showed and the audit trail, which has no other
 * surface in the app.
 */
class HomeViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeLands(),
        repository.observeOwners(),
        repository.observeAudit(limit = 5),
        container.settings.areaUnit
    ) { lands, owners, audit, unit ->
        HomeUiState(
            parcelCount = lands.size,
            ownerCount = owners.size,
            disputedCount = lands.count { it.landStatus == LandStatus.DISPUTED },
            totalAreaSqm = lands.sumOf { it.areaSqm ?: 0.0 },
            areaUnit = unit,
            recentActivity = audit.map { it.toRow() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun AuditEntry.toRow() = HomeAuditRow(
        id = id,
        actionRes = when (action) {
            AuditActions.CREATE -> R.string.audit_action_created
            AuditActions.UPDATE -> R.string.audit_action_updated
            else -> R.string.audit_action_deleted
        },
        detail = detail,
        timestamp = timestamp
    )

    companion object {
        val FACTORY = viewModelFactory {
            initializer { HomeViewModel(container()) }
        }
    }
}
