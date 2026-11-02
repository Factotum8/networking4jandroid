package com.factotum8.networking4jandroid.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import java.time.LocalDate

/**
 * Port of `app/ui/components/assign_action_dialog.py` — the "Назначить
 * действие" dialog shared by the dashboard's stale-contacts list and the
 * contact-detail screen's actions section.
 */
@Composable
fun AssignActionDialog(contactName: String, error: String?, onDismiss: () -> Unit, onConfirm: (ActionType, String) -> Unit) {
    var type by remember { mutableStateOf(ActionType.CALL) }
    var dueDateText by remember { mutableStateOf(LocalDate.now().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Назначить действие: $contactName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown(
                    label = "Тип действия",
                    options = ActionType.entries,
                    optionLabel = ACTION_TYPE_LABELS::getValue,
                    selected = type,
                    onSelected = { type = it },
                )
                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = { dueDateText = it },
                    label = { Text("Срок (ГГГГ-ММ-ДД)") },
                    singleLine = true,
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(type, dueDateText) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
