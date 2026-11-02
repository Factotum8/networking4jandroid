package com.factotum8.networking4jandroid.ui.contactdetail

import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
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
import com.factotum8.networking4jandroid.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ContactDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository = mockk<ContactRepository>()
    private val dimensionLinkRepository = mockk<DimensionLinkRepository>()
    private val knowsRepository = mockk<KnowsRepository>()
    private val relativeRepository = mockk<RelativeRepository>()
    private val interactionRepository = mockk<InteractionRepository>()
    private val actionRepository = mockk<ActionRepository>()

    private fun createViewModel() = ContactDetailViewModel(
        contactRepository, dimensionLinkRepository, knowsRepository, relativeRepository, interactionRepository, actionRepository,
    )

    private fun contact(id: String, name: String = "Contact $id", archived: Boolean = false) =
        ContactEntity(id = id, name = name, archived = archived)

    /** Stubs every section-refresh call `load()` fires for an existing contact, all empty by default. */
    private fun stubSectionsEmpty(id: String) {
        coEvery { dimensionLinkRepository.listForContact(id) } returns emptyList()
        coEvery { knowsRepository.listForContactOnce(id) } returns emptyList()
        coEvery { contactRepository.listOnce() } returns emptyList()
        every { relativeRepository.listForContact(id) } returns flowOf(emptyList())
        every { interactionRepository.listForContact(id) } returns flowOf(emptyList())
        every { actionRepository.listForContact(id) } returns flowOf(emptyList())
    }

    // --- load ---

    @Test
    fun `load with NEW_CONTACT_ID resets to blank create-mode state without touching repositories`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.load(Routes.NEW_CONTACT_ID)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isNew)
        assertFalse(state.loading)
        assertEquals(ContactFormState(), state.form)
        coVerify(exactly = 0) { contactRepository.getById(any()) }
    }

    @Test
    fun `load with unknown id sets notFound`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { contactRepository.getById("missing") } returns null
        val viewModel = createViewModel()

        viewModel.load("missing")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.notFound)
        assertFalse(state.loading)
    }

    @Test
    fun `load with existing id populates form and all sections`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1", name = "Alice").copy(phone = "123", birthday = LocalDate.of(1990, 5, 1))
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        coEvery { dimensionLinkRepository.listForContact("c1") } returns listOf(DimensionLink(DimensionLinkType.TAGGED, "t1", "VIP"))

        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isNew)
        assertFalse(state.loading)
        assertEquals(c, state.loadedContact)
        assertEquals("Alice", state.form.name)
        assertEquals("123", state.form.phone)
        assertEquals("1990-05-01", state.form.birthday)
        assertEquals(1, state.dimensionLinks.size)
    }

    // --- form field updates ---

    @Test
    fun `field setters update form and clear formError`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.load(Routes.NEW_CONTACT_ID)
        advanceUntilIdle()

        viewModel.save() // blank name -> sets formError
        assertEquals("Имя обязательно", viewModel.uiState.value.formError)

        viewModel.onNameChange("Bob")

        assertEquals("Bob", viewModel.uiState.value.form.name)
        assertNull(viewModel.uiState.value.formError)
    }

    // --- save (create) ---

    @Test
    fun `save with blank name sets formError and does not create`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.load(Routes.NEW_CONTACT_ID)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals("Имя обязательно", viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.navigateBack)
        coVerify(exactly = 0) { contactRepository.create(any()) }
    }

    @Test
    fun `save with invalid birthday sets formError and does not create`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.load(Routes.NEW_CONTACT_ID)
        advanceUntilIdle()

        viewModel.onNameChange("Bob")
        viewModel.onBirthdayChange("not-a-date")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Некорректная дата, ожидается формат ГГГГ-ММ-ДД", viewModel.uiState.value.formError)
        coVerify(exactly = 0) { contactRepository.create(any()) }
    }

    @Test
    fun `save on a new contact creates it and navigates back`() = runTest(mainDispatcherRule.dispatcher) {
        val slot = slot<ContactEntity>()
        coEvery { contactRepository.create(capture(slot)) } returns contact("new-id")
        val viewModel = createViewModel()
        viewModel.load(Routes.NEW_CONTACT_ID)
        advanceUntilIdle()

        viewModel.onNameChange("  Bob  ")
        viewModel.onPhoneChange("  555  ")
        viewModel.onBirthdayChange("1990-05-01")
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) { contactRepository.create(any()) }
        assertEquals("Bob", slot.captured.name)
        assertEquals("555", slot.captured.phone)
        assertEquals(LocalDate.of(1990, 5, 1), slot.captured.birthday)
        assertTrue(viewModel.uiState.value.navigateBack)
    }

    @Test
    fun `save on an existing contact updates it preserving id and archived state`() = runTest(mainDispatcherRule.dispatcher) {
        val existing = contact("c1", name = "Old").copy(archived = true)
        coEvery { contactRepository.getById("c1") } returns existing
        stubSectionsEmpty("c1")
        val slot = slot<ContactEntity>()
        coEvery { contactRepository.update(capture(slot)) } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.onNameChange("New")
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) { contactRepository.update(any()) }
        assertEquals("c1", slot.captured.id)
        assertEquals("New", slot.captured.name)
        assertTrue(slot.captured.archived)
        assertTrue(viewModel.uiState.value.navigateBack)
    }

    // --- archive / unarchive / delete ---

    @Test
    fun `archive archives then reloads the contact`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returnsMany listOf(c, c.copy(archived = true))
        stubSectionsEmpty("c1")
        coEvery { contactRepository.archive("c1") } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.archive()
        advanceUntilIdle()

        coVerify(exactly = 1) { contactRepository.archive("c1") }
        assertTrue(viewModel.uiState.value.loadedContact?.archived == true)
    }

    @Test
    fun `requestDelete opens the confirm dialog and cancelDelete closes it`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.load(Routes.NEW_CONTACT_ID)
        advanceUntilIdle()

        viewModel.requestDelete()
        assertTrue(viewModel.uiState.value.deleteConfirmOpen)

        viewModel.cancelDelete()
        assertFalse(viewModel.uiState.value.deleteConfirmOpen)
    }

    @Test
    fun `confirmDelete deletes the contact, closes the dialog and navigates back`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        coEvery { contactRepository.delete("c1") } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()
        viewModel.requestDelete()

        viewModel.confirmDelete()
        advanceUntilIdle()

        coVerify(exactly = 1) { contactRepository.delete("c1") }
        assertFalse(viewModel.uiState.value.deleteConfirmOpen)
        assertTrue(viewModel.uiState.value.navigateBack)
    }

    // --- dimension links ---

    @Test
    fun `attachDimension with blank name is a no-op`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.attachDimension(DimensionLinkType.TAGGED, "   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { dimensionLinkRepository.getOrCreate(any(), any()) }
    }

    @Test
    fun `attachDimension gets-or-creates the target then attaches and refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        coEvery { dimensionLinkRepository.getOrCreate(DimensionLinkType.TAGGED, "VIP") } returns "tag-1"
        coEvery { dimensionLinkRepository.attach("c1", DimensionLinkType.TAGGED, "tag-1") } returns Unit
        coEvery { dimensionLinkRepository.listForContact("c1") } returns listOf(DimensionLink(DimensionLinkType.TAGGED, "tag-1", "VIP"))
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.attachDimension(DimensionLinkType.TAGGED, "VIP")
        advanceUntilIdle()

        coVerify(exactly = 1) { dimensionLinkRepository.attach("c1", DimensionLinkType.TAGGED, "tag-1") }
        assertEquals(1, viewModel.uiState.value.dimensionLinks.size)
    }

    @Test
    fun `detachDimension detaches then refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        coEvery { dimensionLinkRepository.detach("c1", DimensionLinkType.TAGGED, "tag-1") } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.detachDimension(DimensionLinkType.TAGGED, "tag-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { dimensionLinkRepository.detach("c1", DimensionLinkType.TAGGED, "tag-1") }
        coVerify(atLeast = 2) { dimensionLinkRepository.listForContact("c1") } // once on load, once on refresh after detach
    }

    // --- knows ---

    @Test
    fun `refreshKnows resolves the other contact on either side of the directed row`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        val other = contact("c2", name = "Other")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        // stored in reverse direction: contactId = other, relatedContactId = this contact
        val row = KnowsEntity(contactId = "c2", relatedContactId = "c1", type = "friend", date = null, comment = null, closeness = 5)
        coEvery { knowsRepository.listForContactOnce("c1") } returns listOf(row)
        coEvery { contactRepository.getById("c2") } returns other
        coEvery { contactRepository.listOnce() } returns listOf(c, other)
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.knows.size)
        assertEquals("c2", state.knows[0].otherContactId)
        assertEquals("Other", state.knows[0].otherContactName)
        assertEquals(listOf(other), state.otherContacts) // self excluded
    }

    @Test
    fun `removeKnows deletes using the row's own stored direction, not an assumed one`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        coEvery { knowsRepository.remove("c2", "c1") } returns true
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        // row stored as (contactId=c2, relatedContactId=c1) — the reverse of the loaded contact's id
        val row = KnowsEntity(contactId = "c2", relatedContactId = "c1")
        viewModel.removeKnows(row)
        advanceUntilIdle()

        coVerify(exactly = 1) { knowsRepository.remove("c2", "c1") }
        coVerify(exactly = 0) { knowsRepository.remove("c1", "c2") }
    }

    @Test
    fun `addKnows sets the edge from this contact to the other and refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        val date = LocalDate.of(2026, 1, 1)
        coEvery { knowsRepository.set("c1", "c2", "friend", date, null, 5) } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.addKnows("c2", "friend", date, 5)
        advanceUntilIdle()

        coVerify(exactly = 1) { knowsRepository.set("c1", "c2", "friend", date, null, 5) }
    }

    // --- relatives ---

    @Test
    fun `addRelative with blank name is a no-op`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.addRelative("   ", null, null)
        advanceUntilIdle()

        coVerify(exactly = 0) { relativeRepository.add(any(), any(), any(), any()) }
    }

    @Test
    fun `addRelative adds then refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        coEvery { relativeRepository.add("c1", "Mom", "mother", null) } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.addRelative("Mom", "mother", null)
        advanceUntilIdle()

        coVerify(exactly = 1) { relativeRepository.add("c1", "Mom", "mother", null) }
    }

    @Test
    fun `deleteRelative deletes then refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        val relative = RelativeEntity(id = "r1", contactId = "c1", name = "Mom")
        coEvery { relativeRepository.delete(relative) } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.deleteRelative(relative)
        advanceUntilIdle()

        coVerify(exactly = 1) { relativeRepository.delete(relative) }
    }

    // --- interactions ---

    @Test
    fun `addInteraction adds then refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        val date = LocalDate.of(2026, 3, 1)
        coEvery { interactionRepository.add("c1", InteractionType.CALL, date, "hi") } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.addInteraction(InteractionType.CALL, date, "hi")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactionRepository.add("c1", InteractionType.CALL, date, "hi") }
    }

    // --- actions ---

    @Test
    fun `openAssignDialog and dismissAssignDialog toggle state`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.load(Routes.NEW_CONTACT_ID)
        advanceUntilIdle()

        viewModel.openAssignDialog()
        assertTrue(viewModel.uiState.value.assignActionDialogOpen)

        viewModel.dismissAssignDialog()
        assertFalse(viewModel.uiState.value.assignActionDialogOpen)
    }

    @Test
    fun `assignAction with invalid date sets assignActionError and does not add`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.assignAction(ActionType.CALL, "not-a-date")
        advanceUntilIdle()

        assertEquals("Некорректная дата, ожидается формат ГГГГ-ММ-ДД", viewModel.uiState.value.assignActionError)
        coVerify(exactly = 0) { actionRepository.add(any(), any(), any()) }
    }

    @Test
    fun `assignAction with a valid date adds the action, closes the dialog and refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        coEvery { actionRepository.add("c1", ActionType.CALL, LocalDate.of(2026, 6, 1)) } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()
        viewModel.openAssignDialog()

        viewModel.assignAction(ActionType.CALL, "2026-06-01")
        advanceUntilIdle()

        coVerify(exactly = 1) { actionRepository.add("c1", ActionType.CALL, LocalDate.of(2026, 6, 1)) }
        assertFalse(viewModel.uiState.value.assignActionDialogOpen)
    }

    @Test
    fun `completeAction completes then refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        val c = contact("c1")
        coEvery { contactRepository.getById("c1") } returns c
        stubSectionsEmpty("c1")
        coEvery { actionRepository.complete("a1") } returns Unit
        val viewModel = createViewModel()
        viewModel.load("c1")
        advanceUntilIdle()

        viewModel.completeAction("a1")
        advanceUntilIdle()

        coVerify(exactly = 1) { actionRepository.complete("a1") }
    }
}
