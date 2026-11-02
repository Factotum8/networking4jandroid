package com.factotum8.networking4jandroid.ui.duplicates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factotum8.networking4jandroid.data.repository.DuplicateCandidate
import com.factotum8.networking4jandroid.ui.common.DetailScaffold
import com.factotum8.networking4jandroid.ui.common.MutedText
import com.factotum8.networking4jandroid.ui.common.SectionCard
import kotlin.math.roundToInt

/**
 * Port of `app/ui/pages/duplicates.py`: threshold input, candidate list,
 * per-side "archive" shortcut — detection only, no merge action (matches
 * the source's stage-3 scope decision). Backed by [DuplicatesViewModel].
 */
@Composable
fun DuplicatesScreen(onBack: () -> Unit, onOpenContact: (String) -> Unit, viewModel: DuplicatesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    DetailScaffold(title = "Дубликаты", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.thresholdText,
                        onValueChange = viewModel::onThresholdChange,
                        label = { Text("Порог схожести (0-100)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = viewModel::refresh) { Text("Обновить") }
                }
            }
            if (uiState.candidates.isEmpty()) {
                item { MutedText("Дубликаты не найдены") }
            } else {
                items(uiState.candidates.size) { i ->
                    CandidateCard(candidate = uiState.candidates[i], onOpenContact = onOpenContact, onArchive = viewModel::archive)
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(candidate: DuplicateCandidate, onOpenContact: (String) -> Unit, onArchive: (String) -> Unit) {
    val a = candidate.contactA
    val b = candidate.contactB
    SectionCard(title = "${a.name} ↔ ${b.name}") {
        MutedText("Совпадение: ${candidate.score.roundToInt()} (${candidate.matchedOn.joinToString(", ")})")
        listOf(a, b).forEach { contact ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenContact(contact.id) },
                )
                TextButton(onClick = { onArchive(contact.id) }) { Text("Архивировать «${contact.name}»") }
            }
        }
    }
}
