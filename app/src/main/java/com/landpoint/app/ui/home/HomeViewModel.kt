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
import com.landpoint.app.data.model.Land
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.ui.container
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One row of the recent-parcels list on the home screen. */
data class HomeLandRow(val id: String, val name: String, val ownerName: String?, val areaSqm: Double?, val status: LandStatus)

/** One row of the recent-activity list, already shortened for the card. */
data class HomeAuditRow(val id: Long, @StringRes val actionRes: Int, val detail: String, val timestamp: Long)

data class HomeUiState(
    val parcelCount: Int = 0,
    val ownerCount: Int = 0,
    val disputedCount: Int = 0,
    val totalAreaSqm: Double = 0.0,
    val areaUnit: SettingsRepository.AreaUnit = SettingsRepository.AreaUnit.SQM,
    val recentLands: List<HomeLandRow> = emptyList(),
    val recentActivity: List<HomeAuditRow> = emptyList()
)

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
            recentLands = lands.take(5).map { it.toRow() },
            recentActivity = audit.map { it.toRow() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun Land.toRow() = HomeLandRow(
        id = id,
        name = name,
        ownerName = ownerName,
        areaSqm = areaSqm,
        status = landStatus
    )

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
