package com.factotum8.networking4jandroid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factotum8.networking4jandroid.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val loading: Boolean = true,
    val staleContactDaysText: String = "",
    val dedupScoreThresholdText: String = "",
    val errorMessage: String? = null,
    val savedMessage: String? = null,
)

/**
 * Port of `app/ui/pages/settings.py`: the two global settings fields
 * (`stale_contact_days`, `dedup_score_threshold`), with the same client-side
 * validation and Russian messages as the source's `save()` handler.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.get()
            _uiState.update {
                it.copy(
                    loading = false,
                    staleContactDaysText = settings.staleContactDays.toString(),
                    dedupScoreThresholdText = settings.dedupScoreThreshold.toString(),
                )
            }
        }
    }

    fun onStaleContactDaysChange(text: String) =
        _uiState.update { it.copy(staleContactDaysText = text, errorMessage = null, savedMessage = null) }

    fun onDedupScoreThresholdChange(text: String) =
        _uiState.update { it.copy(dedupScoreThresholdText = text, errorMessage = null, savedMessage = null) }

    fun save() {
        val state = _uiState.value
        val staleContactDays = state.staleContactDaysText.trim().toIntOrNull()
        if (staleContactDays == null || staleContactDays < 1) {
            _uiState.update { it.copy(errorMessage = "Порог должен быть не меньше 1 дня", savedMessage = null) }
            return
        }
        val dedupScoreThreshold = state.dedupScoreThresholdText.trim().toDoubleOrNull()
        if (dedupScoreThreshold == null || dedupScoreThreshold < 0 || dedupScoreThreshold > 100) {
            _uiState.update { it.copy(errorMessage = "Порог схожести должен быть от 0 до 100", savedMessage = null) }
            return
        }
        viewModelScope.launch {
            settingsRepository.update(staleContactDays, dedupScoreThreshold)
            _uiState.update { it.copy(errorMessage = null, savedMessage = "Настройки сохранены") }
        }
    }
}
