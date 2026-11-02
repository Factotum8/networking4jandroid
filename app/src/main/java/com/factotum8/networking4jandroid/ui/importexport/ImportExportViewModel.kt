package com.factotum8.networking4jandroid.ui.importexport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factotum8.networking4jandroid.data.io.ImportSummary
import com.factotum8.networking4jandroid.data.repository.GraphBackup
import com.factotum8.networking4jandroid.data.repository.ImportExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportExportUiState(
    val includeArchived: Boolean = false,
    val importing: Boolean = false,
    val importSummary: ImportSummary? = null,
    val backupMessage: String? = null,
    val errorMessage: String? = null,
)

/**
 * Port of `app/ui/pages/import_export.py`: CSV/vCard import (one shared
 * result area for both, matching the source), CSV/vCard export, and an
 * on-demand full-graph backup. File I/O itself (SAF pickers, reading/writing
 * the picked `Uri`s) lives in [ImportExportScreen] — this ViewModel only
 * owns parse/persist/build calls into [ImportExportRepository], so it stays
 * unit-testable without an Android `Context`.
 */
@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val importExportRepository: ImportExportRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportExportUiState())
    val uiState: StateFlow<ImportExportUiState> = _uiState.asStateFlow()

    fun setIncludeArchived(value: Boolean) = _uiState.update { it.copy(includeArchived = value) }

    fun importCsv(text: String) = runImport { importExportRepository.importCsv(text) }

    fun importVCard(text: String) = runImport { importExportRepository.importVCard(text) }

    private fun runImport(block: suspend () -> ImportSummary) {
        _uiState.update { it.copy(importing = true, errorMessage = null) }
        viewModelScope.launch {
            val summary = block()
            _uiState.update { it.copy(importing = false, importSummary = summary) }
        }
    }

    suspend fun exportCsv(): String = importExportRepository.exportContactsCsv(_uiState.value.includeArchived)

    suspend fun exportVCard(): String = importExportRepository.exportContactsVCard(_uiState.value.includeArchived)

    suspend fun buildBackup(): GraphBackup = importExportRepository.buildBackup()

    /** Combines the source's `result_label.text` + `ui.notify("Бэкап создан", ...)` into one inline message, matching this app's inline-status convention (no toast/snackbar infra elsewhere). */
    fun onBackupSaved(jsonFileName: String, csvFileName: String) =
        _uiState.update { it.copy(backupMessage = "Бэкап создан. Сохранено: $jsonFileName, $csvFileName", errorMessage = null) }

    fun onFileError(message: String) = _uiState.update { it.copy(errorMessage = message, importing = false) }

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, backupMessage = null) }
}
