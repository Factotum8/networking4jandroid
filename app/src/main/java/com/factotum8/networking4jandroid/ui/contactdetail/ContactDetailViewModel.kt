package com.factotum8.networking4jandroid.ui.contactdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import com.factotum8.networking4jandroid.data.repository.ActionRepository
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import com.factotum8.networking4jandroid.data.repository.DimensionLink
import com.factotum8.networking4jandroid.data.repository.DimensionLinkRepository
import com.factotum8.networking4jandroid.data.repository.InteractionRepository
import com.factotum8.networking4jandroid.data.repository.KnowsRepository
import com.factotum8.networking4jandroid.data.repository.RelativeRepository
import com.factotum8.networking4jandroid.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class ContactFormState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val position: String = "",
    val city: String = "",
    val photo: String = "",
    val circle: Circle? = null,
    val contactType: ContactType? = null,
    val dangerous: Int? = null,
    val interesting: Int? = null,
    val difficult: Int? = null,
    val birthday: String = "",
    val metPlace: String = "",
    val metDate: String = "",
    val notes: String = "",
)

/** One KNOWS edge resolved to the *other* contact's id/name — [KnowsEntity] itself only carries both raw ids. */
data class KnowsDisplayEntry(val otherContactId: String, val otherContactName: String, val knows: KnowsEntity)

data class ContactDetailUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val loadedContact: ContactEntity? = null,
    val form: ContactFormState = ContactFormState(),
    val formError: String? = null,
    val navigateBack: Boolean = false,
    val dimensionLinks: List<DimensionLink> = emptyList(),
    val knows: List<KnowsDisplayEntry> = emptyList(),
    val otherContacts: List<ContactEntity> = emptyList(),
    val relatives: List<RelativeEntity> = emptyList(),
    val interactions: List<InteractionEntity> = emptyList(),
    val actions: List<ActionEntity> = emptyList(),
    val deleteConfirmOpen: Boolean = false,
    val assignActionDialogOpen: Boolean = false,
    val assignActionError: String? = null,
)

/**
 * Port of `app/ui/pages/contact_detail.py`: the shared "new"/"edit" main
 * form plus the six relationship sections (dimension links, KNOWS,
 * relatives, interactions, actions). Each section is loaded once and
 * refreshed after its own mutations, mirroring the source's per-section
 * `refresh()` closures rather than one big reactive query — the six
 * underlying repositories don't share a single natural `Flow` to drive
 * a fully-reactive state off of.
 */
@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val dimensionLinkRepository: DimensionLinkRepository,
    private val knowsRepository: KnowsRepository,
    private val relativeRepository: RelativeRepository,
    private val interactionRepository: InteractionRepository,
    private val actionRepository: ActionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    /** Loads (or resets to blank-create-mode) for [contactId]; call once when the screen is entered. */
    fun load(contactId: String) {
        if (contactId == Routes.NEW_CONTACT_ID) {
            _uiState.value = ContactDetailUiState(isNew = true, loading = false)
            return
        }
        _uiState.value = ContactDetailUiState(isNew = false, loading = true)
        viewModelScope.launch {
            val contact = contactRepository.getById(contactId)
            if (contact == null) {
                _uiState.update { it.copy(loading = false, notFound = true) }
                return@launch
            }
            _uiState.update { it.copy(loading = false, loadedContact = contact, form = contact.toFormState()) }
            refreshDimensionLinks()
            refreshKnows()
            refreshRelatives()
            refreshInteractions()
            refreshActions()
        }
    }

    // --- main form ---

    fun onNameChange(v: String) = updateForm { it.copy(name = v) }
    fun onPhoneChange(v: String) = updateForm { it.copy(phone = v) }
    fun onEmailChange(v: String) = updateForm { it.copy(email = v) }
    fun onPositionChange(v: String) = updateForm { it.copy(position = v) }
    fun onCityChange(v: String) = updateForm { it.copy(city = v) }
    fun onPhotoChange(v: String) = updateForm { it.copy(photo = v) }
    fun onCircleChange(v: Circle?) = updateForm { it.copy(circle = v) }
    fun onContactTypeChange(v: ContactType?) = updateForm { it.copy(contactType = v) }
    fun onDangerousChange(v: Int?) = updateForm { it.copy(dangerous = v) }
    fun onInterestingChange(v: Int?) = updateForm { it.copy(interesting = v) }
    fun onDifficultChange(v: Int?) = updateForm { it.copy(difficult = v) }
    fun onBirthdayChange(v: String) = updateForm { it.copy(birthday = v) }
    fun onMetPlaceChange(v: String) = updateForm { it.copy(metPlace = v) }
    fun onMetDateChange(v: String) = updateForm { it.copy(metDate = v) }
    fun onNotesChange(v: String) = updateForm { it.copy(notes = v) }

    private inline fun updateForm(transform: (ContactFormState) -> ContactFormState) {
        _uiState.update { it.copy(form = transform(it.form), formError = null) }
    }

    /** [name] required; [birthday]/[metDate] optional but must parse as ISO `yyyy-MM-dd` if non-blank. */
    fun save() {
        val state = _uiState.value
        val form = state.form
        val name = form.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(formError = "Имя обязательно") }
            return
        }
        val birthday: LocalDate?
        val metDate: LocalDate?
        try {
            birthday = parseOptionalDate(form.birthday)
            metDate = parseOptionalDate(form.metDate)
        } catch (exc: DateTimeParseException) {
            _uiState.update { it.copy(formError = "Некорректная дата, ожидается формат ГГГГ-ММ-ДД") }
            return
        }
        viewModelScope.launch {
            if (state.isNew) {
                contactRepository.create(
                    ContactEntity(
                        id = "",
                        name = name,
                        phone = form.phone.trim().ifBlank { null },
                        email = form.email.trim().ifBlank { null },
                        position = form.position.trim().ifBlank { null },
                        city = form.city.trim().ifBlank { null },
                        photo = form.photo.trim().ifBlank { null },
                        circle = form.circle,
                        contactType = form.contactType,
                        dangerous = form.dangerous,
                        interesting = form.interesting,
                        difficult = form.difficult,
                        birthday = birthday,
                        metPlace = form.metPlace.trim().ifBlank { null },
                        metDate = metDate,
                        notes = form.notes.trim().ifBlank { null },
                    ),
                )
            } else {
                val existing = state.loadedContact ?: return@launch
                contactRepository.update(
                    existing.copy(
                        name = name,
                        phone = form.phone.trim().ifBlank { null },
                        email = form.email.trim().ifBlank { null },
                        position = form.position.trim().ifBlank { null },
                        city = form.city.trim().ifBlank { null },
                        photo = form.photo.trim().ifBlank { null },
                        circle = form.circle,
                        contactType = form.contactType,
                        dangerous = form.dangerous,
                        interesting = form.interesting,
                        difficult = form.difficult,
                        birthday = birthday,
                        metPlace = form.metPlace.trim().ifBlank { null },
                        metDate = metDate,
                        notes = form.notes.trim().ifBlank { null },
                    ),
                )
            }
            _uiState.update { it.copy(navigateBack = true) }
        }
    }

    fun archive() {
        val id = _uiState.value.loadedContact?.id ?: return
        viewModelScope.launch {
            contactRepository.archive(id)
            reloadContactOnly(id)
        }
    }

    fun unarchive() {
        val id = _uiState.value.loadedContact?.id ?: return
        viewModelScope.launch {
            contactRepository.unarchive(id)
            reloadContactOnly(id)
        }
    }

    private suspend fun reloadContactOnly(id: String) {
        val contact = contactRepository.getById(id) ?: return
        _uiState.update { it.copy(loadedContact = contact, form = contact.toFormState()) }
    }

    fun requestDelete() = _uiState.update { it.copy(deleteConfirmOpen = true) }
    fun cancelDelete() = _uiState.update { it.copy(deleteConfirmOpen = false) }

    fun confirmDelete() {
        val id = _uiState.value.loadedContact?.id ?: return
        viewModelScope.launch {
            contactRepository.delete(id)
            _uiState.update { it.copy(deleteConfirmOpen = false, navigateBack = true) }
        }
    }

    // --- dimension links (Связи) ---

    fun attachDimension(type: DimensionLinkType, name: String) {
        val id = _uiState.value.loadedContact?.id ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val targetId = dimensionLinkRepository.getOrCreate(type, trimmed)
            dimensionLinkRepository.attach(id, type, targetId)
            refreshDimensionLinks()
        }
    }

    fun detachDimension(type: DimensionLinkType, targetId: String) {
        val id = _uiState.value.loadedContact?.id ?: return
        viewModelScope.launch {
            dimensionLinkRepository.detach(id, type, targetId)
            refreshDimensionLinks()
        }
    }

    private suspend fun refreshDimensionLinks() {
        val id = _uiState.value.loadedContact?.id ?: return
        _uiState.update { it.copy(dimensionLinks = dimensionLinkRepository.listForContact(id)) }
    }

    // --- knows (Знакомства) ---

    fun addKnows(otherContactId: String, type: String?, date: LocalDate?, closeness: Int?) {
        val id = _uiState.value.loadedContact?.id ?: return
        viewModelScope.launch {
            knowsRepository.set(id, otherContactId, type, date, null, closeness)
            refreshKnows()
        }
    }

    /** [knows] must be the exact stored row (its `contactId`/`relatedContactId` may have either contact on either side — KNOWS is directed storage, undirected display). */
    fun removeKnows(knows: KnowsEntity) {
        viewModelScope.launch {
            knowsRepository.remove(knows.contactId, knows.relatedContactId)
            refreshKnows()
        }
    }

    private suspend fun refreshKnows() {
        val id = _uiState.value.loadedContact?.id ?: return
        val rows = knowsRepository.listForContactOnce(id)
        val entries = rows.mapNotNull { row ->
            val otherId = if (row.contactId == id) row.relatedContactId else row.contactId
            contactRepository.getById(otherId)?.let { KnowsDisplayEntry(otherId, it.name, row) }
        }
        val others = contactRepository.listOnce().filter { it.id != id }
        _uiState.update { it.copy(knows = entries, otherContacts = others) }
    }

    // --- relatives (Родственники) ---

    fun addRelative(name: String, relationType: String?, birthday: LocalDate?) {
        val id = _uiState.value.loadedContact?.id ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            relativeRepository.add(id, trimmed, relationType?.trim()?.ifBlank { null }, birthday)
            refreshRelatives()
        }
    }

    fun deleteRelative(relative: RelativeEntity) {
        viewModelScope.launch {
            relativeRepository.delete(relative)
            refreshRelatives()
        }
    }

    private suspend fun refreshRelatives() {
        val id = _uiState.value.loadedContact?.id ?: return
        _uiState.update { it.copy(relatives = relativeRepository.listForContact(id).first()) }
    }

    // --- interactions (История взаимодействий) ---

    fun addInteraction(type: InteractionType, date: LocalDate, comment: String?) {
        val id = _uiState.value.loadedContact?.id ?: return
        viewModelScope.launch {
            interactionRepository.add(id, type, date, comment?.trim()?.ifBlank { null })
            refreshInteractions()
        }
    }

    private suspend fun refreshInteractions() {
        val id = _uiState.value.loadedContact?.id ?: return
        _uiState.update { it.copy(interactions = interactionRepository.listForContact(id).first()) }
    }

    // --- actions (Действия) ---

    fun openAssignDialog() = _uiState.update { it.copy(assignActionDialogOpen = true, assignActionError = null) }
    fun dismissAssignDialog() = _uiState.update { it.copy(assignActionDialogOpen = false, assignActionError = null) }

    fun assignAction(type: ActionType, dueDateIso: String) {
        val id = _uiState.value.loadedContact?.id ?: return
        val dueDate = try {
            LocalDate.parse(dueDateIso)
        } catch (exc: DateTimeParseException) {
            _uiState.update { it.copy(assignActionError = "Некорректная дата, ожидается формат ГГГГ-ММ-ДД") }
            return
        }
        viewModelScope.launch {
            actionRepository.add(id, type, dueDate)
            _uiState.update { it.copy(assignActionDialogOpen = false, assignActionError = null) }
            refreshActions()
        }
    }

    fun completeAction(actionId: String) {
        viewModelScope.launch {
            actionRepository.complete(actionId)
            refreshActions()
        }
    }

    private suspend fun refreshActions() {
        val id = _uiState.value.loadedContact?.id ?: return
        _uiState.update { it.copy(actions = actionRepository.listForContact(id).first()) }
    }
}

private fun ContactEntity.toFormState() = ContactFormState(
    name = name,
    phone = phone.orEmpty(),
    email = email.orEmpty(),
    position = position.orEmpty(),
    city = city.orEmpty(),
    photo = photo.orEmpty(),
    circle = circle,
    contactType = contactType,
    dangerous = dangerous,
    interesting = interesting,
    difficult = difficult,
    birthday = birthday?.toString().orEmpty(),
    metPlace = metPlace.orEmpty(),
    metDate = metDate?.toString().orEmpty(),
    notes = notes.orEmpty(),
)

private fun parseOptionalDate(text: String): LocalDate? {
    val trimmed = text.trim()
    return if (trimmed.isEmpty()) null else LocalDate.parse(trimmed)
}
