package com.factotum8.networking4jandroid.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.repository.DueActionEntry
import com.factotum8.networking4jandroid.data.repository.StaleContact
import com.factotum8.networking4jandroid.data.repository.UpcomingBirthday
import com.factotum8.networking4jandroid.ui.common.ACTION_TYPE_LABELS
import com.factotum8.networking4jandroid.ui.common.AssignActionDialog
import com.factotum8.networking4jandroid.ui.common.CIRCLE_LABELS
import com.factotum8.networking4jandroid.ui.common.CONTACT_TYPE_LABELS
import com.factotum8.networking4jandroid.ui.common.FilterDropdown
import com.factotum8.networking4jandroid.ui.common.MutedText
import com.factotum8.networking4jandroid.ui.common.SectionCard

private const val OVERVIEW_DISPLAY_LIMIT = 20

/**
 * Port of `app/ui/pages/dashboard.py`: search, circle/type-filtered
 * overview, recently-added, reminders digest, stale-contacts list with a
 * quick "assign action" dialog. Backed by [DashboardViewModel].
 */
@Composable
fun DashboardScreen(onOpenContact: (String) -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                SearchCard(
                    query = uiState.searchQuery,
                    results = uiState.searchResults,
                    hasSearched = uiState.hasSearched,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = viewModel::runSearch,
                    onContactClick = onOpenContact,
                )
            }
            item {
                FiltersCard(
                    circleFilter = uiState.circleFilter,
                    contactTypeFilter = uiState.contactTypeFilter,
                    contacts = uiState.overviewContacts,
                    onCircleChange = viewModel::onCircleFilterChange,
                    onContactTypeChange = viewModel::onContactTypeFilterChange,
                    onContactClick = onOpenContact,
                )
            }
            item {
                RecentCard(contacts = uiState.recentContacts, onContactClick = onOpenContact)
            }
            item {
                RemindersCard(
                    dueActions = uiState.digest.dueActions,
                    upcomingBirthdays = uiState.digest.upcomingBirthdays,
                    onComplete = viewModel::completeAction,
                    onContactClick = onOpenContact,
                )
            }
            item {
                StaleContactsCard(
                    staleContacts = uiState.digest.staleContacts,
                    onAssign = viewModel::openAssignDialog,
                    onContactClick = onOpenContact,
                )
            }
        }
    }

    uiState.assignActionTarget?.let { target ->
        AssignActionDialog(
            contactName = target.contactName,
            error = uiState.assignActionError,
            onDismiss = viewModel::dismissAssignDialog,
            onConfirm = viewModel::assignAction,
        )
    }
}

@Composable
private fun SearchCard(
    query: String,
    results: List<ContactEntity>,
    hasSearched: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onContactClick: (String) -> Unit,
) {
    SectionCard(title = "Поиск") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Имя или заметка") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSearch) { Text("Искать") }
        }
        if (hasSearched) {
            if (results.isEmpty()) {
                MutedText("Ничего не найдено")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    results.forEach { contact -> ContactRow(contact = contact, onClick = { onContactClick(contact.id) }) }
                }
            }
        }
    }
}

@Composable
private fun FiltersCard(
    circleFilter: Circle?,
    contactTypeFilter: ContactType?,
    contacts: List<ContactEntity>,
    onCircleChange: (Circle?) -> Unit,
    onContactTypeChange: (ContactType?) -> Unit,
    onContactClick: (String) -> Unit,
) {
    SectionCard(title = "Контакты") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterDropdown(
                modifier = Modifier.weight(1f),
                label = "Круг",
                options = listOf<Circle?>(null) + Circle.entries,
                optionLabel = { it?.let(CIRCLE_LABELS::getValue) ?: "Все" },
                selected = circleFilter,
                onSelected = onCircleChange,
            )
            FilterDropdown(
                modifier = Modifier.weight(1f),
                label = "Тип",
                options = listOf<ContactType?>(null) + ContactType.entries,
                optionLabel = { it?.let(CONTACT_TYPE_LABELS::getValue) ?: "Все" },
                selected = contactTypeFilter,
                onSelected = onContactTypeChange,
            )
        }
        if (contacts.isEmpty()) {
            MutedText("Нет контактов по заданным фильтрам")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                contacts.take(OVERVIEW_DISPLAY_LIMIT).forEach { contact ->
                    ContactRow(contact = contact, onClick = { onContactClick(contact.id) })
                }
            }
            if (contacts.size > OVERVIEW_DISPLAY_LIMIT) {
                MutedText("...и ещё ${contacts.size - OVERVIEW_DISPLAY_LIMIT}")
            }
        }
    }
}

@Composable
private fun RecentCard(contacts: List<ContactEntity>, onContactClick: (String) -> Unit) {
    SectionCard(title = "Недавно добавленные") {
        if (contacts.isEmpty()) {
            MutedText("Пока нет контактов")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                contacts.forEach { contact -> ContactRow(contact = contact, onClick = { onContactClick(contact.id) }) }
            }
        }
    }
}

@Composable
private fun RemindersCard(
    dueActions: List<DueActionEntry>,
    upcomingBirthdays: List<UpcomingBirthday>,
    onComplete: (ActionEntity) -> Unit,
    onContactClick: (String) -> Unit,
) {
    SectionCard(title = "Напоминания") {
        if (dueActions.isEmpty() && upcomingBirthdays.isEmpty()) {
            MutedText("Нечего напомнить")
        }
        dueActions.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContactRow(
                    modifier = Modifier.weight(1f),
                    name = entry.contactName,
                    circle = null,
                    contactType = null,
                    subtitle = "${ACTION_TYPE_LABELS.getValue(entry.action.type)}, срок ${entry.action.dueDate}",
                    onClick = { onContactClick(entry.contactId) },
                )
                TextButton(onClick = { onComplete(entry.action) }) { Text("Готово") }
            }
        }
        upcomingBirthdays.forEach { bday ->
            Text("🎂 ${bday.name} — через ${bday.daysAway} дн. (${bday.birthday})", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StaleContactsCard(
    staleContacts: List<StaleContact>,
    onAssign: (String, String) -> Unit,
    onContactClick: (String) -> Unit,
) {
    SectionCard(title = "Давно не общались") {
        if (staleContacts.isEmpty()) {
            MutedText("Все контакты в тонусе")
        }
        staleContacts.forEach { stale ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContactRow(
                    modifier = Modifier.weight(1f),
                    contact = stale.contact,
                    subtitle = stale.lastInteractionDate?.let { "последний контакт: $it" } ?: "ещё не общались",
                    onClick = { onContactClick(stale.contact.id) },
                )
                TextButton(onClick = { onAssign(stale.contact.id, stale.contact.name) }) { Text("Назначить действие") }
            }
        }
    }
}

@Composable
private fun ContactRow(modifier: Modifier = Modifier, contact: ContactEntity, subtitle: String = "", onClick: () -> Unit) {
    ContactRow(
        modifier = modifier,
        name = contact.name,
        circle = contact.circle,
        contactType = contact.contactType,
        subtitle = subtitle,
        onClick = onClick,
    )
}

@Composable
private fun ContactRow(
    modifier: Modifier = Modifier,
    name: String,
    circle: Circle?,
    contactType: ContactType?,
    subtitle: String = "",
    onClick: () -> Unit,
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        val meta = listOfNotNull(
            circle?.let(CIRCLE_LABELS::getValue),
            contactType?.let(CONTACT_TYPE_LABELS::getValue),
            subtitle.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

