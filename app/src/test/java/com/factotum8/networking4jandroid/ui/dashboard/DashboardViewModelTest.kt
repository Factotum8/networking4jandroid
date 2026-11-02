package com.factotum8.networking4jandroid.ui.dashboard

import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.repository.ActionRepository
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import com.factotum8.networking4jandroid.data.repository.DueActionEntry
import com.factotum8.networking4jandroid.data.repository.ReminderDigest
import com.factotum8.networking4jandroid.data.repository.ReminderRepository
import com.factotum8.networking4jandroid.data.repository.SearchRepository
import com.factotum8.networking4jandroid.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchRepository = mockk<SearchRepository>()
    private val contactRepository = mockk<ContactRepository>()
    private val reminderRepository = mockk<ReminderRepository>()
    private val actionRepository = mockk<ActionRepository>()

    private val emptyDigest = ReminderDigest(emptyList(), emptyList(), emptyList())

    private fun contact(id: String, name: String = "Contact $id", createdAt: Instant? = Instant.EPOCH) =
        ContactEntity(id = id, name = name, createdAt = createdAt)

    private fun createViewModel() = DashboardViewModel(searchRepository, contactRepository, reminderRepository, actionRepository)

    /**
     * Baseline stub for the init{}-triggered calls (refreshOverview/refreshRecent both resolve
     * to `listOnce(archived = false, circle = null, contactType = null)` when filters are
     * unset, and refreshDigest calls `buildDigest()`). Individual tests override with `coEvery`
     * again where they need a specific return value — MockK's most recent stub wins.
     */
    @Before
    fun setUp() {
        coEvery { contactRepository.listOnce(archived = false, circle = null, contactType = null) } returns emptyList()
        coEvery { reminderRepository.buildDigest() } returns emptyDigest
    }

    @Test
    fun `init populates overview, recent and digest from repositories`() = runTest(mainDispatcherRule.dispatcher) {
        val contacts = listOf(contact("1"), contact("2"))
        val digest = ReminderDigest(
            dueActions = listOf(DueActionEntry("1", "Contact 1", ActionEntity("a1", "1", ActionType.CALL, LocalDate.now()))),
            upcomingBirthdays = emptyList(),
            staleContacts = emptyList(),
        )
        coEvery { contactRepository.listOnce(archived = false, circle = null, contactType = null) } returns contacts
        coEvery { reminderRepository.buildDigest() } returns digest

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(contacts, state.overviewContacts)
        assertEquals(contacts, state.recentContacts)
        assertEquals(digest, state.digest)
    }

    @Test
    fun `onSearchQueryChange updates query without triggering a search`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("john")
        advanceUntilIdle()

        assertEquals("john", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
        assertFalse(viewModel.uiState.value.hasSearched)
        coVerify(exactly = 0) { searchRepository.search(any(), any()) }
    }

    @Test
    fun `runSearch with blank query clears results and does not call the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("   ")
        viewModel.runSearch()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
        assertFalse(viewModel.uiState.value.hasSearched)
        coVerify(exactly = 0) { searchRepository.search(any(), any()) }
    }

    @Test
    fun `runSearch with a query calls the repository and populates results`() = runTest(mainDispatcherRule.dispatcher) {
        val results = listOf(contact("5", "Ivan"))
        coEvery { searchRepository.search("ivan") } returns results

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("ivan")
        viewModel.runSearch()
        advanceUntilIdle()

        assertEquals(results, viewModel.uiState.value.searchResults)
        assertTrue(viewModel.uiState.value.hasSearched)
        coVerify(exactly = 1) { searchRepository.search("ivan") }
    }

    @Test
    fun `onCircleFilterChange updates filter and refreshes overview with the new circle`() = runTest(mainDispatcherRule.dispatcher) {
        val filtered = listOf(contact("9"))
        coEvery { contactRepository.listOnce(archived = false, circle = Circle.SUPPORT_CIRCLE, contactType = null) } returns filtered

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCircleFilterChange(Circle.SUPPORT_CIRCLE)
        advanceUntilIdle()

        assertEquals(Circle.SUPPORT_CIRCLE, viewModel.uiState.value.circleFilter)
        assertEquals(filtered, viewModel.uiState.value.overviewContacts)
        coVerify(exactly = 1) { contactRepository.listOnce(archived = false, circle = Circle.SUPPORT_CIRCLE, contactType = null) }
    }

    @Test
    fun `onContactTypeFilterChange updates filter and refreshes overview with the new type`() = runTest(mainDispatcherRule.dispatcher) {
        val filtered = listOf(contact("9"))
        coEvery { contactRepository.listOnce(archived = false, circle = null, contactType = ContactType.CONNECTOR) } returns filtered

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onContactTypeFilterChange(ContactType.CONNECTOR)
        advanceUntilIdle()

        assertEquals(ContactType.CONNECTOR, viewModel.uiState.value.contactTypeFilter)
        assertEquals(filtered, viewModel.uiState.value.overviewContacts)
        coVerify(exactly = 1) { contactRepository.listOnce(archived = false, circle = null, contactType = ContactType.CONNECTOR) }
    }

    @Test
    fun `completeAction marks the action complete then refreshes the digest`() = runTest(mainDispatcherRule.dispatcher) {
        val action = ActionEntity("a1", "1", ActionType.CALL, LocalDate.now())
        coEvery { actionRepository.complete("a1") } just Runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.completeAction(action)
        advanceUntilIdle()

        coVerify(exactly = 1) { actionRepository.complete("a1") }
        coVerify(exactly = 2) { reminderRepository.buildDigest() } // once from init, once from the post-complete refresh
    }

    @Test
    fun `openAssignDialog sets the target and clears any previous error`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openAssignDialog("1", "Ivan")

        val target = viewModel.uiState.value.assignActionTarget
        assertEquals("1", target?.contactId)
        assertEquals("Ivan", target?.contactName)
        assertNull(viewModel.uiState.value.assignActionError)
    }

    @Test
    fun `dismissAssignDialog clears target and error`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.openAssignDialog("1", "Ivan")

        viewModel.dismissAssignDialog()

        assertNull(viewModel.uiState.value.assignActionTarget)
        assertNull(viewModel.uiState.value.assignActionError)
    }

    @Test
    fun `assignAction with an unparseable date sets an error and does not call the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.openAssignDialog("1", "Ivan")

        viewModel.assignAction(ActionType.CALL, "not-a-date")
        advanceUntilIdle()

        assertEquals("Некорректная дата, ожидается формат ГГГГ-ММ-ДД", viewModel.uiState.value.assignActionError)
        assertEquals("1", viewModel.uiState.value.assignActionTarget?.contactId) // dialog stays open on error
        coVerify(exactly = 0) { actionRepository.add(any(), any(), any()) }
    }

    @Test
    fun `assignAction with a valid date adds the action, dismisses the dialog and refreshes the digest`() =
        runTest(mainDispatcherRule.dispatcher) {
            coEvery { actionRepository.add("1", ActionType.MEET, LocalDate.parse("2026-01-15")) } just Runs

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.openAssignDialog("1", "Ivan")

            viewModel.assignAction(ActionType.MEET, "2026-01-15")
            advanceUntilIdle()

            coVerify(exactly = 1) { actionRepository.add("1", ActionType.MEET, LocalDate.parse("2026-01-15")) }
            assertNull(viewModel.uiState.value.assignActionTarget)
            assertNull(viewModel.uiState.value.assignActionError)
            coVerify(exactly = 2) { reminderRepository.buildDigest() } // init + post-assign refresh
        }

    @Test
    fun `assignAction with no open target is a no-op`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.assignAction(ActionType.CALL, "2026-01-15")
        advanceUntilIdle()

        coVerify(exactly = 0) { actionRepository.add(any(), any(), any()) }
        assertNull(viewModel.uiState.value.assignActionTarget)
        assertNull(viewModel.uiState.value.assignActionError)
    }

    @Test
    fun `recentContacts is sorted by createdAt descending and capped at 5`() = runTest(mainDispatcherRule.dispatcher) {
        val contacts = (1..6).map { i -> contact(id = "$i", createdAt = Instant.EPOCH.plusSeconds(i.toLong())) }
        // Repository returns them in an arbitrary order; the ViewModel must sort+cap itself.
        coEvery { contactRepository.listOnce(archived = false, circle = null, contactType = null) } returns contacts.shuffled(java.util.Random(42))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val expectedIds = listOf("6", "5", "4", "3", "2") // 5 most recent, descending
        assertEquals(expectedIds, viewModel.uiState.value.recentContacts.map { it.id })
    }
}
