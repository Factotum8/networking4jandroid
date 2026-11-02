package com.factotum8.networking4jandroid.ui.contacts

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import com.factotum8.networking4jandroid.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContactsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository = mockk<ContactRepository>()

    private fun contact(id: String, name: String = "Contact $id") = ContactEntity(id = id, name = name)

    @Test
    fun `uiState default value before any subscriber is the empty state`() = runTest(mainDispatcherRule.dispatcher) {
        every { contactRepository.list(archived = false, circle = null, contactType = null) } returns flowOf(emptyList())

        val viewModel = ContactsViewModel(contactRepository)

        // stateIn's WhileSubscribed source only starts once collected; before that, .value is the seed default.
        assertEquals(ContactsFilters(), viewModel.uiState.value.filters)
        assertTrue(viewModel.uiState.value.contacts.isEmpty())
    }

    @Test
    fun `uiState reflects the repository's contacts for the default filters once subscribed`() = runTest(mainDispatcherRule.dispatcher) {
        val contacts = listOf(contact("1"), contact("2"))
        every { contactRepository.list(archived = false, circle = null, contactType = null) } returns flowOf(contacts)

        val viewModel = ContactsViewModel(contactRepository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(contacts, viewModel.uiState.value.contacts)
        assertEquals(ContactsFilters(), viewModel.uiState.value.filters)
        collector.cancel()
    }

    @Test
    fun `onCircleFilterChange switches to the repository flow for the new circle`() = runTest(mainDispatcherRule.dispatcher) {
        val defaultContacts = listOf(contact("1"))
        val supportCircleContacts = listOf(contact("2"))
        every { contactRepository.list(archived = false, circle = null, contactType = null) } returns flowOf(defaultContacts)
        every { contactRepository.list(archived = false, circle = Circle.SUPPORT_CIRCLE, contactType = null) } returns flowOf(supportCircleContacts)

        val viewModel = ContactsViewModel(contactRepository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onCircleFilterChange(Circle.SUPPORT_CIRCLE)
        advanceUntilIdle()

        assertEquals(Circle.SUPPORT_CIRCLE, viewModel.uiState.value.filters.circle)
        assertEquals(supportCircleContacts, viewModel.uiState.value.contacts)
        collector.cancel()
    }

    @Test
    fun `onContactTypeFilterChange switches to the repository flow for the new type`() = runTest(mainDispatcherRule.dispatcher) {
        val defaultContacts = listOf(contact("1"))
        val connectorContacts = listOf(contact("3"))
        every { contactRepository.list(archived = false, circle = null, contactType = null) } returns flowOf(defaultContacts)
        every { contactRepository.list(archived = false, circle = null, contactType = ContactType.CONNECTOR) } returns flowOf(connectorContacts)

        val viewModel = ContactsViewModel(contactRepository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onContactTypeFilterChange(ContactType.CONNECTOR)
        advanceUntilIdle()

        assertEquals(ContactType.CONNECTOR, viewModel.uiState.value.filters.contactType)
        assertEquals(connectorContacts, viewModel.uiState.value.contacts)
        collector.cancel()
    }

    @Test
    fun `onShowArchivedChange toggles the archived flag passed to the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val activeContacts = listOf(contact("1"))
        val archivedContacts = listOf(contact("4"))
        every { contactRepository.list(archived = false, circle = null, contactType = null) } returns flowOf(activeContacts)
        every { contactRepository.list(archived = true, circle = null, contactType = null) } returns flowOf(archivedContacts)

        val viewModel = ContactsViewModel(contactRepository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onShowArchivedChange(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.filters.showArchived)
        assertEquals(archivedContacts, viewModel.uiState.value.contacts)
        collector.cancel()
    }

    @Test
    fun `circle and contact type filters AND-combine in a single repository call`() = runTest(mainDispatcherRule.dispatcher) {
        val defaultContacts = listOf(contact("1"))
        val circleOnlyContacts = listOf(contact("2"))
        val combinedContacts = listOf(contact("5"))
        every { contactRepository.list(archived = false, circle = null, contactType = null) } returns flowOf(defaultContacts)
        // flatMapLatest re-subscribes after EACH filter change, so the intermediate
        // circle-only combination (before contactType is also set) needs its own stub.
        every { contactRepository.list(archived = false, circle = Circle.SUCCESS_CIRCLE, contactType = null) } returns flowOf(circleOnlyContacts)
        every {
            contactRepository.list(archived = false, circle = Circle.SUCCESS_CIRCLE, contactType = ContactType.BRIDGE)
        } returns flowOf(combinedContacts)

        val viewModel = ContactsViewModel(contactRepository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onCircleFilterChange(Circle.SUCCESS_CIRCLE)
        advanceUntilIdle()
        viewModel.onContactTypeFilterChange(ContactType.BRIDGE)
        advanceUntilIdle()

        assertEquals(combinedContacts, viewModel.uiState.value.contacts)
        verify(exactly = 1) { contactRepository.list(archived = false, circle = Circle.SUCCESS_CIRCLE, contactType = ContactType.BRIDGE) }
        collector.cancel()
    }
}
