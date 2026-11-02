package com.factotum8.networking4jandroid.ui.importexport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factotum8.networking4jandroid.data.repository.GraphBackup
import com.factotum8.networking4jandroid.ui.common.DetailScaffold
import com.factotum8.networking4jandroid.ui.common.MutedText
import com.factotum8.networking4jandroid.ui.common.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val CSV_MIME_TYPES = arrayOf("text/csv", "text/comma-separated-values", "text/plain")
private val VCARD_MIME_TYPES = arrayOf("text/vcard", "text/x-vcard")
private val BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

/**
 * Port of `app/ui/pages/import_export.py`: CSV/vCard import, CSV/vCard
 * export, and an on-demand full-graph backup. The source's browser
 * upload/download becomes the Storage Access Framework here (decision #7 in
 * PLAN.md) — `OpenDocument` for import, `CreateDocument` for export/backup,
 * so no broad storage permission is needed. Backed by [ImportExportViewModel].
 */
@Composable
fun ImportExportScreen(onBack: () -> Unit, viewModel: ImportExportViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Carries the JSON half of a built backup across the two sequential
    // CreateDocument pickers (JSON, then CSV) since SAF only lets us save
    // one file per picker call.
    var pendingBackup by remember { mutableStateOf<GraphBackup?>(null) }
    var pendingBackupJsonName by remember { mutableStateOf<String?>(null) }
    var pendingBackupTimestamp by remember { mutableStateOf("") }

    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { readUriText(context, uri) } }
                .onSuccess { viewModel.importCsv(it) }
                .onFailure { viewModel.onFileError("Не удалось прочитать файл CSV") }
        }
    }
    val importVCardLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { readUriText(context, uri) } }
                .onSuccess { viewModel.importVCard(it) }
                .onFailure { viewModel.onFileError("Не удалось прочитать файл vCard") }
        }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val content = viewModel.exportCsv()
                withContext(Dispatchers.IO) { writeUriText(context, uri, content) }
            }.onFailure { viewModel.onFileError("Не удалось сохранить CSV") }
        }
    }
    val exportVCardLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/vcard")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val content = viewModel.exportVCard()
                withContext(Dispatchers.IO) { writeUriText(context, uri, content) }
            }.onFailure { viewModel.onFileError("Не удалось сохранить vCard") }
        }
    }
    val backupCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val backup = pendingBackup
        if (uri == null || backup == null) {
            viewModel.onFileError("Бэкап отменён: CSV не сохранён")
            pendingBackup = null
            pendingBackupJsonName = null
            pendingBackupTimestamp = ""
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { writeUriText(context, uri, backup.contactsCsv) }
                val csvName = queryDisplayName(context, uri) ?: "contacts.csv"
                viewModel.onBackupSaved(pendingBackupJsonName ?: "backup.json", csvName)
            }.onFailure { viewModel.onFileError("Не удалось сохранить CSV бэкапа") }
            pendingBackup = null
            pendingBackupJsonName = null
            pendingBackupTimestamp = ""
        }
    }
    val backupJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val backup = viewModel.buildBackup()
                withContext(Dispatchers.IO) { writeUriText(context, uri, backup.json) }
                pendingBackup = backup
                pendingBackupJsonName = queryDisplayName(context, uri) ?: "backup.json"
                backupCsvLauncher.launch("contacts_$pendingBackupTimestamp.csv")
            }.onFailure { viewModel.onFileError("Не удалось сохранить JSON бэкапа") }
        }
    }

    DetailScaffold(title = "Импорт/экспорт", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            uiState.errorMessage?.let { errorMessage ->
                item { Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            item {
                SectionCard(title = "Импорт") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { importCsvLauncher.launch(CSV_MIME_TYPES) },
                            enabled = !uiState.importing,
                        ) { Text("CSV / Excel") }
                        Button(
                            onClick = { importVCardLauncher.launch(VCARD_MIME_TYPES) },
                            enabled = !uiState.importing,
                        ) { Text("vCard") }
                    }
                    if (uiState.importing) {
                        MutedText("Импорт...")
                    }
                    uiState.importSummary?.let { summary ->
                        Text("Создано контактов: ${summary.created}", style = MaterialTheme.typography.bodySmall)
                        if (summary.errors.isNotEmpty()) {
                            Text(
                                "Ошибок: ${summary.errors.size}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            summary.errors.forEach { error ->
                                MutedText("Строка ${error.row}: ${error.message}")
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Экспорт") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Включать архивные")
                        Switch(checked = uiState.includeArchived, onCheckedChange = viewModel::setIncludeArchived)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportCsvLauncher.launch("contacts.csv") }) { Text("Экспорт CSV") }
                        Button(onClick = { exportVCardLauncher.launch("contacts.vcf") }) { Text("Экспорт vCard") }
                    }
                }
            }
            item {
                SectionCard(title = "Резервная копия") {
                    MutedText("Полный снимок данных (JSON) + CSV контактов.")
                    Button(onClick = {
                        pendingBackupTimestamp = BACKUP_TIMESTAMP.format(Instant.now())
                        backupJsonLauncher.launch("backup_$pendingBackupTimestamp.json")
                    }) {
                        Text("Сделать бэкап сейчас")
                    }
                    uiState.backupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

private fun readUriText(context: Context, uri: Uri): String {
    val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

private fun writeUriText(context: Context, uri: Uri, content: String) {
    val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("Cannot open $uri")
    stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
}

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
