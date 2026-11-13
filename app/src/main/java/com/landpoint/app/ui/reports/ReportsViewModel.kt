package com.landpoint.app.ui.reports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.landpoint.app.AppContainer
import com.landpoint.app.R
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.Land
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReportsUiState(
    val parcelCount: Int = 0,
    val ownerCount: Int = 0,
    val totalAreaSqm: Double = 0.0,
    val areaUnit: SettingsRepository.AreaUnit = SettingsRepository.AreaUnit.SQM,
    val usesDms: Boolean = false,
    val disputedCount: Int = 0,
    val soldCount: Int = 0,
    val transferredCount: Int = 0,
    val isBusy: Boolean = false
)

class ReportsViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository
    private val importExport = container.importExport
    private val pdfExporter = container.pdfExporter
    private val settings = container.settings
    private val strings = container.strings

    private val busy = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<ReportsUiState> = combine(
        repository.observeLands(),
        repository.observeOwners(),
        settings.areaUnit,
        settings.coordFormat,
        busy
    ) { lands, owners, unit, format, isBusy ->
        ReportsUiState(
            parcelCount = lands.size,
            ownerCount = owners.size,
            totalAreaSqm = lands.sumOf { it.areaSqm ?: 0.0 },
            areaUnit = unit,
            usesDms = format == SettingsRepository.CoordFormat.DMS,
            disputedCount = lands.count { it.landStatus == LandStatus.DISPUTED },
            soldCount = lands.count { it.landStatus == LandStatus.SOLD },
            transferredCount = lands.count { it.landStatus == LandStatus.TRANSFERRED },
            isBusy = isBusy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    /** Every land, newest first, for the per-parcel PDF picker. */
    suspend fun allLands(): List<Land> = repository.getAllLands()

    fun suggestedName(single: Land?): String = importExport.suggestFileName(
        com.landpoint.app.data.export.ExportFormat.PDF,
        single
    )

    private fun runTask(block: suspend () -> String?) {
        viewModelScope.launch {
            busy.value = true
            _message.value = runCatching { block() }.getOrElse {
                strings.get(R.string.msg_generic_error, it.message ?: it::class.simpleName.orEmpty())
            }
            busy.value = false
        }
    }

    fun exportSinglePdf(uri: Uri, land: Land) = runTask {
        val dms = uiState.value.usesDms
        val result = pdfExporter.exportReport(uri, listOf(land), dms, uiState.value.areaUnit)
        if (result.isFailure) {
            strings.get(R.string.msg_pdf_failed, result.error ?: "")
        } else {
            strings.get(R.string.msg_pdf_done, land.name)
        }
    }

    fun exportCsv(uri: Uri) = runTask {
        val result = importExport.exportCsv(uri, repository.getAllLands())
        if (result.isFailure) {
            strings.get(R.string.msg_csv_failed, result.error ?: "")
        } else {
            strings.plural(R.plurals.msg_exported, result.count)
        }
    }

    fun exportJson(uri: Uri) = runTask {
        val result = importExport.exportJson(uri, repository.getAllLands())
        if (result.isFailure) {
            strings.get(R.string.msg_generic_error, result.error ?: "")
        } else {
            strings.plural(R.plurals.msg_exported, result.count)
        }
    }

    fun exportGpx(uri: Uri) = runTask {
        val result = importExport.exportGpx(uri, repository.getAllLands())
        if (result.isFailure) {
            strings.get(R.string.msg_geo_export_failed, result.error ?: "")
        } else {
            strings.plural(R.plurals.msg_exported, result.count)
        }
    }

    fun exportKml(uri: Uri) = runTask {
        val result = importExport.exportKml(uri, repository.getAllLands())
        if (result.isFailure) {
            strings.get(R.string.msg_geo_export_failed, result.error ?: "")
        } else {
            strings.plural(R.plurals.msg_exported, result.count)
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    companion object {
        val FACTORY = viewModelFactory {
            initializer { ReportsViewModel(container()) }
        }
    }
}
