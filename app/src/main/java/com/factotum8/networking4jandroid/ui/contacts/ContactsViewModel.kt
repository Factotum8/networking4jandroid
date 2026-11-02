package com.factotum8.networking4jandroid.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ContactsFilters(
    val circle: Circle? = null,
    val contactType: ContactType? = null,
    val showArchived: Boolean = false,
)

data class ContactsUiState(
    val filters: ContactsFilters = ContactsFilters(),
    val contacts: List<ContactEntity> = emptyList(),
)

/**
 * Port of `app/ui/pages/contacts.py`'s filters (circle/type AND-combine,
 * archived a strict boolean toggle). Unlike the source's manual
 * `refresh()` after every filter change, this drives the list off
 * [ContactRepository.list]'s `Flow` directly, so it also picks up
 * inserts/edits/archives made from the (future) contact-detail screen
 * without an explicit refresh call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel @Inject constructor(
    contactRepository: ContactRepository,
) : ViewModel() {
    private val filters = MutableStateFlow(ContactsFilters())

    val uiState: StateFlow<ContactsUiState> = filters
        .flatMapLatest { current ->
            contactRepository.list(archived = current.showArchived, circle = current.circle, contactType = current.contactType)
                .map { contacts -> ContactsUiState(filters = current, contacts = contacts) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsUiState())

    fun onCircleFilterChange(circle: Circle?) {
        filters.update { it.copy(circle = circle) }
    }

    fun onContactTypeFilterChange(contactType: ContactType?) {
        filters.update { it.copy(contactType = contactType) }
    }

    fun onShowArchivedChange(showArchived: Boolean) {
        filters.update { it.copy(showArchived = showArchived) }
    }
}
