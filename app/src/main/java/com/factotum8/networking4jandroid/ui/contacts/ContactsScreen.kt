package com.factotum8.networking4jandroid.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.ui.common.CIRCLE_LABELS
import com.factotum8.networking4jandroid.ui.common.CONTACT_TYPE_LABELS
import com.factotum8.networking4jandroid.ui.common.FilterDropdown

/**
 * Port of `app/ui/pages/contacts.py`: circle/type filters, an archived
 * toggle, and the filtered list — "+ Новый контакт" becomes a FAB
 * (phone convention) rather than the source's inline button.
 */
@Composable
fun ContactsScreen(onOpenContact: (String) -> Unit, onNewContact: () -> Unit, viewModel: ContactsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewContact) {
                Icon(Icons.Default.Add, contentDescription = "Новый контакт")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Круг",
                    options = listOf<Circle?>(null) + Circle.entries,
                    optionLabel = { it?.let(CIRCLE_LABELS::getValue) ?: "Все" },
                    selected = uiState.filters.circle,
                    onSelected = viewModel::onCircleFilterChange,
                )
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Тип",
                    options = listOf<ContactType?>(null) + ContactType.entries,
                    optionLabel = { it?.let(CONTACT_TYPE_LABELS::getValue) ?: "Все" },
                    selected = uiState.filters.contactType,
                    onSelected = viewModel::onContactTypeFilterChange,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = uiState.filters.showArchived, onCheckedChange = viewModel::onShowArchivedChange)
                Text("Показывать архивные")
            }

            if (uiState.contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Нет контактов по заданным фильтрам",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(uiState.contacts, key = { it.id }) { contact ->
                        ContactListRow(contact = contact, onClick = { onOpenContact(contact.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactListRow(contact: ContactEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(contact.name, style = MaterialTheme.typography.bodyLarge)
        val meta = listOfNotNull(
            contact.circle?.let(CIRCLE_LABELS::getValue),
            contact.contactType?.let(CONTACT_TYPE_LABELS::getValue),
            contact.city,
            contact.phone,
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
