package com.factotum8.networking4jandroid.ui.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import com.factotum8.networking4jandroid.data.repository.DuplicateCandidate
import com.factotum8.networking4jandroid.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicatesUiState(
    val thresholdText: String = "",
    val candidates: List<DuplicateCandidate> = emptyList(),
    val loading: Boolean = true,
)

/**
 * Port of `app/ui/pages/duplicates.py`: detection-only (no merge action,
 * per the source's stage-3 scope decision) — a threshold input and a quick
 * "archive one side of the pair" shortcut per candidate.
 */
@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val contactRepository: ContactRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onThresholdChange(text: String) = _uiState.update { it.copy(thresholdText = text) }

    /** Blank threshold falls back to `settings.dedupScoreThreshold`, matching [SearchRepository.findDuplicates]'s own default. */
    fun refresh() {
        viewModelScope.launch {
            val threshold = _uiState.value.thresholdText.trim().toDoubleOrNull()
            val candidates = searchRepository.findDuplicates(threshold)
            _uiState.update { it.copy(candidates = candidates, loading = false) }
        }
    }

    fun archive(contactId: String) {
        viewModelScope.launch {
            contactRepository.archive(contactId)
            refresh()
        }
    }
}
