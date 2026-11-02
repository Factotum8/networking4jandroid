package com.factotum8.networking4jandroid.ui.importexport

import com.factotum8.networking4jandroid.data.io.ImportRowError
import com.factotum8.networking4jandroid.data.io.ImportSummary
import com.factotum8.networking4jandroid.data.repository.GraphBackup
import com.factotum8.networking4jandroid.data.repository.ImportExportRepository
import com.factotum8.networking4jandroid.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
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

class ImportExportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ImportExportRepository>()

    private fun createViewModel() = ImportExportViewModel(repository)

    @Before
    fun setUp() {
        coEvery { repository.exportContactsCsv(any()) } returns "csv"
        coEvery { repository.exportContactsVCard(any()) } returns "vcard"
        coEvery { repository.buildBackup() } returns GraphBackup(json = "{}", contactsCsv = "csv")
    }

    @Test
    fun `importCsv delegates to the repository and publishes the summary`() = runTest(mainDispatcherRule.dispatcher) {
        val summary = ImportSummary(created = 3, errors = listOf(ImportRowError(2, "name is required")))
        coEvery { repository.importCsv("raw csv") } returns summary
        val viewModel = createViewModel()

        viewModel.importCsv("raw csv")
        assertTrue(viewModel.uiState.value.importing)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.importing)
        assertEquals(summary, state.importSummary)
    }

    @Test
    fun `importVCard delegates to the repository and publishes the summary`() = runTest(mainDispatcherRule.dispatcher) {
        val summary = ImportSummary(created = 1)
        coEvery { repository.importVCard("raw vcard") } returns summary
        val viewModel = createViewModel()

        viewModel.importVCard("raw vcard")
        advanceUntilIdle()

        assertEquals(summary, viewModel.uiState.value.importSummary)
    }

    @Test
    fun `exportCsv passes the current includeArchived flag`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.setIncludeArchived(true)

        viewModel.exportCsv()

        coVerify(exactly = 1) { repository.exportContactsCsv(true) }
    }

    @Test
    fun `exportVCard defaults includeArchived to false`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.exportVCard()

        coVerify(exactly = 1) { repository.exportContactsVCard(false) }
    }

    @Test
    fun `buildBackup delegates to the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        val backup = viewModel.buildBackup()

        assertEquals(GraphBackup(json = "{}", contactsCsv = "csv"), backup)
    }

    @Test
    fun `onBackupSaved formats a combined confirmation message`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.onBackupSaved("backup_20260921.json", "contacts_20260921.csv")

        val state = viewModel.uiState.value
        assertEquals("Бэкап создан. Сохранено: backup_20260921.json, contacts_20260921.csv", state.backupMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onFileError sets the error message and stops any in-progress import`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.onFileError("Не удалось прочитать файл CSV")

        val state = viewModel.uiState.value
        assertEquals("Не удалось прочитать файл CSV", state.errorMessage)
        assertFalse(state.importing)
    }

    @Test
    fun `clearMessages resets both error and backup messages`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onFileError("boom")
        viewModel.onBackupSaved("a.json", "b.csv")

        viewModel.clearMessages()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNull(state.backupMessage)
    }
}
