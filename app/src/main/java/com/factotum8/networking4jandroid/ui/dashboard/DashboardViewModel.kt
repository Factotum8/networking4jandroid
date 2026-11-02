package com.factotum8.networking4jandroid.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.repository.ActionRepository
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import com.factotum8.networking4jandroid.data.repository.ReminderDigest
import com.factotum8.networking4jandroid.data.repository.ReminderRepository
import com.factotum8.networking4jandroid.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

private const val RECENT_COUNT = 5

/** Which stale contact the "Назначить действие" dialog is currently open for. */
data class AssignActionTarget(val contactId: String, val contactName: String)

data class DashboardUiState(
    val searchQuery: String = "",
    val searchResults: List<ContactEntity> = emptyList(),
    val hasSearched: Boolean = false,
    val circleFilter: Circle? = null,
    val contactTypeFilter: ContactType? = null,
    val overviewContacts: List<ContactEntity> = emptyList(),
    val recentContacts: List<ContactEntity> = emptyList(),
    val digest: ReminderDigest = ReminderDigest(emptyList(), emptyList(), emptyList()),
    val assignActionTarget: AssignActionTarget? = null,
    val assignActionError: String? = null,
)

/**
 * Port of `app/ui/pages/dashboard.py`'s state/behaviour: search-on-submit
 * (not live-as-you-type, matching the source's `run_search` triggered by
 * Enter/button), circle/type-filtered overview, 5 most-recently-created
 * contacts, and the reminders digest (due actions + birthdays + stale
 * contacts) with its two inline actions (complete a due action, assign a
 * new one to a stale contact).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val contactRepository: ContactRepository,
    private val reminderRepository: ReminderRepository,
    private val actionRepository: ActionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshOverview()
        refreshRecent()
        refreshDigest()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun runSearch() {
        val query = _uiState.value.searchQuery.trim()
        viewModelScope.launch {
            val results = if (query.isEmpty()) emptyList() else searchRepository.search(query)
            _uiState.update { it.copy(searchResults = results, hasSearched = query.isNotEmpty()) }
        }
    }

    fun onCircleFilterChange(circle: Circle?) {
        _uiState.update { it.copy(circleFilter = circle) }
        refreshOverview()
    }

    fun onContactTypeFilterChange(contactType: ContactType?) {
        _uiState.update { it.copy(contactTypeFilter = contactType) }
        refreshOverview()
    }

    private fun refreshOverview() {
        viewModelScope.launch {
            val state = _uiState.value
            val contacts = contactRepository.listOnce(circle = state.circleFilter, contactType = state.contactTypeFilter)
            _uiState.update { it.copy(overviewContacts = contacts) }
        }
    }

    private fun refreshRecent() {
        viewModelScope.launch {
            val recent = contactRepository.listOnce()
                .sortedByDescending { it.createdAt }
                .take(RECENT_COUNT)
            _uiState.update { it.copy(recentContacts = recent) }
        }
    }

    private fun refreshDigest() {
        viewModelScope.launch {
            val digest = reminderRepository.buildDigest()
            _uiState.update { it.copy(digest = digest) }
        }
    }

    fun completeAction(action: ActionEntity) {
        viewModelScope.launch {
            actionRepository.complete(action.id)
            refreshDigest()
        }
    }

    fun openAssignDialog(contactId: String, contactName: String) {
        _uiState.update { it.copy(assignActionTarget = AssignActionTarget(contactId, contactName), assignActionError = null) }
    }

    fun dismissAssignDialog() {
        _uiState.update { it.copy(assignActionTarget = null, assignActionError = null) }
    }

    /** [dueDateIso] is validated here (not the dialog) so the error message can live in state, like the rest of the screen. */
    fun assignAction(type: ActionType, dueDateIso: String) {
        val target = _uiState.value.assignActionTarget ?: return
        val dueDate = try {
            LocalDate.parse(dueDateIso)
        } catch (exc: DateTimeParseException) {
            _uiState.update { it.copy(assignActionError = "Некорректная дата, ожидается формат ГГГГ-ММ-ДД") }
            return
        }
        viewModelScope.launch {
            actionRepository.add(target.contactId, type, dueDate)
            dismissAssignDialog()
            refreshDigest()
        }
    }
}
