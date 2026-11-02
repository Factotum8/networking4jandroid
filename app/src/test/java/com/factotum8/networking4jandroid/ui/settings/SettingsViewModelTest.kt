package com.factotum8.networking4jandroid.ui.settings

import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import com.factotum8.networking4jandroid.data.repository.SettingsRepository
import com.factotum8.networking4jandroid.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = mockk<SettingsRepository>()

    private fun createViewModel() = SettingsViewModel(settingsRepository)

    @Before
    fun setUp() {
        coEvery { settingsRepository.get() } returns SettingsEntity(staleContactDays = 60, dedupScoreThreshold = 85.0)
        coEvery { settingsRepository.update(any(), any()) } returns Unit
    }

    @Test
    fun `init loads the current settings into text fields`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals("60", state.staleContactDaysText)
        assertEquals("85.0", state.dedupScoreThresholdText)
    }

    @Test
    fun `save rejects a stale-contact threshold below 1`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onStaleContactDaysChange("0")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Порог должен быть не меньше 1 дня", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { settingsRepository.update(any(), any()) }
    }

    @Test
    fun `save rejects a non-numeric stale-contact threshold`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onStaleContactDaysChange("abc")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Порог должен быть не меньше 1 дня", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { settingsRepository.update(any(), any()) }
    }

    @Test
    fun `save rejects a dedup threshold outside 0 to 100`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDedupScoreThresholdChange("150")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Порог схожести должен быть от 0 до 100", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { settingsRepository.update(any(), any()) }
    }

    @Test
    fun `save rejects a negative dedup threshold`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDedupScoreThresholdChange("-1")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Порог схожести должен быть от 0 до 100", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { settingsRepository.update(any(), any()) }
    }

    @Test
    fun `save persists valid values and shows a confirmation`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onStaleContactDaysChange("45")
        viewModel.onDedupScoreThresholdChange("80")
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.update(45, 80.0) }
        val state = viewModel.uiState.value
        assertEquals("Настройки сохранены", state.savedMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun `editing a field after an error clears the message`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onStaleContactDaysChange("0")
        viewModel.save()
        advanceUntilIdle()
        assertEquals("Порог должен быть не меньше 1 дня", viewModel.uiState.value.errorMessage)

        viewModel.onStaleContactDaysChange("30")

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
