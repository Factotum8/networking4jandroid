package com.factotum8.networking4jandroid.ui.importexport

import com.factotum8.networking4jandroid.data.io.BackupFormatException
import com.factotum8.networking4jandroid.data.io.BackupRestoreData
import com.factotum8.networking4jandroid.data.io.ImportRowError
import com.factotum8.networking4jandroid.data.io.ImportSummary
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
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

    private fun restoreData(skipped: Int = 0) = BackupRestoreData(
        contacts = listOf(ContactEntity("a", "A"), ContactEntity("b", "B")),
        interactions = emptyList(), actions = emptyList(), relatives = emptyList(), knows = emptyList(),
        companies = emptyList(), events = emptyList(), communities = emptyList(), projects = emptyList(),
        interests = emptyList(), tags = emptyList(), companyLinks = emptyList(), eventLinks = emptyList(),
        communityLinks = emptyList(), projectLinks = emptyList(), interestLinks = emptyList(), tagLinks = emptyList(),
        settings = SettingsEntity(), skipped = skipped,
    )

    @Test
    fun `importDesktopBackup publishes a summary with counts`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { repository.importDesktopBackup("json") } returns restoreData(skipped = 3)
        val viewModel = createViewModel()

        viewModel.importDesktopBackup("json")
        assertTrue(viewModel.uiState.value.importing)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.importing)
        assertNull(state.errorMessage)
        assertTrue(state.desktopImportMessage!!.contains("контактов 2"))
        assertTrue(state.desktopImportMessage!!.contains("Пропущено записей: 3"))
    }

    @Test
    fun `importDesktopBackup shows a format error to the user`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { repository.importDesktopBackup(any()) } throws BackupFormatException("Файл не является корректным JSON")
        val viewModel = createViewModel()

        viewModel.importDesktopBackup("x")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.importing)
        assertNull(state.desktopImportMessage)
        assertEquals("Импорт бэкапа не выполнен: Файл не является корректным JSON", state.errorMessage)
    }

    @Test
    fun `importDesktopBackup does not leak raw database error text`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { repository.importDesktopBackup(any()) } throws IllegalStateException("SQLITE_CONSTRAINT detail")
        val viewModel = createViewModel()

        viewModel.importDesktopBackup("x")
        advanceUntilIdle()

        val error = viewModel.uiState.value.errorMessage!!
        assertFalse(error.contains("SQLITE"))
        assertTrue(error.contains("не изменены"))
    }

    @Test
    fun `desktop import summary omits the skipped part when nothing was skipped`() {
        assertFalse(desktopImportSummary(restoreData()).contains("Пропущено"))
    }
}
