package com.factotum8.networking4jandroid.ui.duplicates

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import com.factotum8.networking4jandroid.data.repository.DuplicateCandidate
import com.factotum8.networking4jandroid.data.repository.SearchRepository
import com.factotum8.networking4jandroid.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DuplicatesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchRepository = mockk<SearchRepository>()
    private val contactRepository = mockk<ContactRepository>()

    private fun contact(id: String, name: String = "Contact $id") = ContactEntity(id = id, name = name)
    private fun createViewModel() = DuplicatesViewModel(searchRepository, contactRepository)

    @Before
    fun setUp() {
        coEvery { searchRepository.findDuplicates(any()) } returns emptyList()
    }

    @Test
    fun `init loads duplicates with no explicit threshold`() = runTest(mainDispatcherRule.dispatcher) {
        val candidate = DuplicateCandidate(contact("c1"), contact("c2"), 92.0, listOf("name"))
        coEvery { searchRepository.findDuplicates(null) } returns listOf(candidate)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(listOf(candidate), state.candidates)
    }

    @Test
    fun `blank threshold text passes null so the repository falls back to settings`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onThresholdChange("   ")
        viewModel.refresh()
        advanceUntilIdle()

        coVerify { searchRepository.findDuplicates(null) }
    }

    @Test
    fun `non-blank threshold text is parsed and passed through`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onThresholdChange("75")
        viewModel.refresh()
        advanceUntilIdle()

        coVerify { searchRepository.findDuplicates(75.0) }
    }

    @Test
    fun `archive archives the contact then refreshes`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { contactRepository.archive("c1") } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.archive("c1")
        advanceUntilIdle()

        coVerify(exactly = 1) { contactRepository.archive("c1") }
        coVerify(exactly = 2) { searchRepository.findDuplicates(any()) } // once on init, once after archive
    }
}
