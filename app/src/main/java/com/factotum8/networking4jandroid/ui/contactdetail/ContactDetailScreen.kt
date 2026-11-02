package com.factotum8.networking4jandroid.ui.contactdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import com.factotum8.networking4jandroid.data.repository.DimensionLink
import com.factotum8.networking4jandroid.ui.Routes
import com.factotum8.networking4jandroid.ui.common.ACTION_TYPE_LABELS
import com.factotum8.networking4jandroid.ui.common.AssignActionDialog
import com.factotum8.networking4jandroid.ui.common.CIRCLE_LABELS
import com.factotum8.networking4jandroid.ui.common.CONTACT_TYPE_LABELS
import com.factotum8.networking4jandroid.ui.common.DIMENSION_TYPE_LABELS
import com.factotum8.networking4jandroid.ui.common.DetailScaffold
import com.factotum8.networking4jandroid.ui.common.FilterDropdown
import com.factotum8.networking4jandroid.ui.common.INTERACTION_TYPE_LABELS
import com.factotum8.networking4jandroid.ui.common.MutedText
import com.factotum8.networking4jandroid.ui.common.SectionCard
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Port of `app/ui/pages/contact_detail.py`: the main field form (shared
 * between "new" and "edit") plus the six relationship sections. Backed by
 * [ContactDetailViewModel]; [contactId] is [Routes.NEW_CONTACT_ID] in
 * create mode.
 */
@Composable
fun ContactDetailScreen(contactId: String, onBack: () -> Unit, viewModel: ContactDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(contactId) { viewModel.load(contactId) }
    LaunchedEffect(uiState.navigateBack) { if (uiState.navigateBack) onBack() }

    val title = if (uiState.isNew) "Новый контакт" else uiState.loadedContact?.name ?: "Контакт"

    DetailScaffold(title = title, onBack = onBack) { padding ->
        when {
            uiState.loading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.notFound -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Контакт не найден", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onBack) { Text("К списку контактов") }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                item { MainFormSection(uiState = uiState, viewModel = viewModel, onBack = onBack) }
                if (!uiState.isNew) {
                    item { DimensionLinksSection(links = uiState.dimensionLinks, viewModel = viewModel) }
                    item { KnowsSection(uiState = uiState, viewModel = viewModel) }
                    item { RelativesSection(relatives = uiState.relatives, viewModel = viewModel) }
                    item { InteractionsSection(interactions = uiState.interactions, viewModel = viewModel) }
                    item { ActionsSection(actions = uiState.actions, viewModel = viewModel) }
                }
            }
        }
    }

    if (uiState.deleteConfirmOpen) {
        DeleteConfirmDialog(
            name = uiState.loadedContact?.name.orEmpty(),
            onDismiss = viewModel::cancelDelete,
            onConfirm = viewModel::confirmDelete,
        )
    }
    if (uiState.assignActionDialogOpen) {
        AssignActionDialog(
            contactName = uiState.loadedContact?.name.orEmpty(),
            error = uiState.assignActionError,
            onDismiss = viewModel::dismissAssignDialog,
            onConfirm = viewModel::assignAction,
        )
    }
}

private val RATING_OPTIONS: List<Int?> = listOf(null) + (1..10).toList()
private fun ratingLabel(v: Int?) = v?.toString() ?: "—"

@Composable
private fun MainFormSection(uiState: ContactDetailUiState, viewModel: ContactDetailViewModel, onBack: () -> Unit) {
    val form = uiState.form
    SectionCard(title = "Основное") {
        if (!uiState.isNew && uiState.loadedContact?.archived == true) {
            Text("Архивирован", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = form.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Имя *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(form.phone, viewModel::onPhoneChange, label = { Text("Телефон") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(form.email, viewModel::onEmailChange, label = { Text("Email") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(form.position, viewModel::onPositionChange, label = { Text("Должность") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(form.city, viewModel::onCityChange, label = { Text("Город") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(
            value = form.photo,
            onValueChange = viewModel::onPhotoChange,
            label = { Text("Фото (URL)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterDropdown(
                modifier = Modifier.weight(1f),
                label = "Круг",
                options = listOf<Circle?>(null) + Circle.entries,
                optionLabel = { it?.let(CIRCLE_LABELS::getValue) ?: "Не указано" },
                selected = form.circle,
                onSelected = viewModel::onCircleChange,
            )
            FilterDropdown(
                modifier = Modifier.weight(1f),
                label = "Тип контакта",
                options = listOf<ContactType?>(null) + ContactType.entries,
                optionLabel = { it?.let(CONTACT_TYPE_LABELS::getValue) ?: "Не указано" },
                selected = form.contactType,
                onSelected = viewModel::onContactTypeChange,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterDropdown(
                modifier = Modifier.weight(1f), label = "Dangerous", options = RATING_OPTIONS,
                optionLabel = ::ratingLabel, selected = form.dangerous, onSelected = viewModel::onDangerousChange,
            )
            FilterDropdown(
                modifier = Modifier.weight(1f), label = "Interesting", options = RATING_OPTIONS,
                optionLabel = ::ratingLabel, selected = form.interesting, onSelected = viewModel::onInterestingChange,
            )
            FilterDropdown(
                modifier = Modifier.weight(1f), label = "Difficult", options = RATING_OPTIONS,
                optionLabel = ::ratingLabel, selected = form.difficult, onSelected = viewModel::onDifficultChange,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                form.birthday, viewModel::onBirthdayChange, label = { Text("День рождения (ГГГГ-ММ-ДД)") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(form.metPlace, viewModel::onMetPlaceChange, label = { Text("Место встречи") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(
                form.metDate, viewModel::onMetDateChange, label = { Text("Дата встречи (ГГГГ-ММ-ДД)") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = form.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Заметки") },
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.formError?.let { formError ->
            Text(formError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)) {
            TextButton(onClick = onBack) { Text("Назад") }
            if (!uiState.isNew) {
                val archived = uiState.loadedContact?.archived == true
                TextButton(onClick = if (archived) viewModel::unarchive else viewModel::archive) {
                    Text(if (archived) "Восстановить из архива" else "Архивировать")
                }
                TextButton(onClick = viewModel::requestDelete) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            }
            Button(onClick = viewModel::save) { Text(if (uiState.isNew) "Создать" else "Сохранить") }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить контакт?") },
        text = { Text("Удалить контакт «$name» безвозвратно?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

// --- Связи (dimension links) ---

@Composable
private fun DimensionLinksSection(links: List<DimensionLink>, viewModel: ContactDetailViewModel) {
    SectionCard(title = "Связи") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DimensionLinkType.entries.forEach { type ->
                DimensionTypeRow(
                    type = type,
                    attached = links.filter { it.relType == type },
                    onAttach = { name -> viewModel.attachDimension(type, name) },
                    onDetach = { targetId -> viewModel.detachDimension(type, targetId) },
                )
            }
        }
    }
}

@Composable
private fun DimensionTypeRow(type: DimensionLinkType, attached: List<DimensionLink>, onAttach: (String) -> Unit, onDetach: (String) -> Unit) {
    var newName by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(DIMENSION_TYPE_LABELS.getValue(type), style = MaterialTheme.typography.labelLarge)
        if (attached.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(attached.size) { i ->
                    val link = attached[i]
                    AssistChip(
                        onClick = { onDetach(link.targetId) },
                        label = { Text(link.targetName) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Открепить", modifier = Modifier.width(16.dp)) },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("новое или существующее") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onAttach(newName); newName = "" }, enabled = newName.isNotBlank()) { Text("Добавить") }
        }
    }
}

// --- Знакомства (knows) ---

@Composable
private fun KnowsSection(uiState: ContactDetailUiState, viewModel: ContactDetailViewModel) {
    SectionCard(title = "Знакомства") {
        if (uiState.knows.isEmpty()) {
            MutedText("Пока нет связей")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                uiState.knows.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.otherContactName)
                            val meta = listOfNotNull(entry.knows.type, entry.knows.date?.toString(), entry.knows.closeness?.toString())
                                .joinToString(", ")
                            if (meta.isNotEmpty()) MutedText(meta)
                        }
                        IconButton(onClick = { viewModel.removeKnows(entry.knows) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
        AddKnowsRow(otherContacts = uiState.otherContacts, onAdd = viewModel::addKnows)
    }
}

@Composable
private fun AddKnowsRow(otherContacts: List<ContactEntity>, onAdd: (String, String?, LocalDate?, Int?) -> Unit) {
    var selected by remember { mutableStateOf<ContactEntity?>(null) }
    var type by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var closenessText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterDropdown(
            label = "Контакт",
            options = listOf<ContactEntity?>(null) + otherContacts,
            optionLabel = { it?.name ?: "Выберите контакт" },
            selected = selected,
            onSelected = { selected = it },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(type, { type = it }, label = { Text("Тип") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(dateText, { dateText = it }, label = { Text("Дата (ГГГГ-ММ-ДД)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(closenessText, { closenessText = it }, label = { Text("Близость") }, singleLine = true, modifier = Modifier.width(100.dp))
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            val contact = selected
            if (contact == null) {
                error = "Выберите контакт"
                return@TextButton
            }
            val date = try {
                dateText.trim().ifEmpty { null }?.let(LocalDate::parse)
            } catch (exc: DateTimeParseException) {
                error = "Некорректная дата"
                return@TextButton
            }
            error = null
            onAdd(contact.id, type.trim().ifBlank { null }, date, closenessText.trim().toIntOrNull())
            selected = null
            type = ""
            dateText = ""
            closenessText = ""
        }) { Text("Добавить") }
    }
}

// --- Родственники (relatives) ---

@Composable
private fun RelativesSection(relatives: List<RelativeEntity>, viewModel: ContactDetailViewModel) {
    SectionCard(title = "Родственники") {
        if (relatives.isEmpty()) {
            MutedText("Пока нет данных")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                relatives.forEach { relative ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val meta = listOfNotNull(relative.relationType, relative.birthday?.toString()).joinToString(" · ")
                            Text(relative.name)
                            if (meta.isNotEmpty()) MutedText(meta)
                        }
                        IconButton(onClick = { viewModel.deleteRelative(relative) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
        AddRelativeRow(onAdd = viewModel::addRelative)
    }
}

@Composable
private fun AddRelativeRow(onAdd: (String, String?, LocalDate?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var relationType by remember { mutableStateOf("") }
    var birthdayText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(relationType, { relationType = it }, label = { Text("Кем приходится") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(birthdayText, { birthdayText = it }, label = { Text("День рождения (ГГГГ-ММ-ДД)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            if (name.isBlank()) {
                error = "Имя обязательно"
                return@TextButton
            }
            val birthday = try {
                birthdayText.trim().ifEmpty { null }?.let(LocalDate::parse)
            } catch (exc: DateTimeParseException) {
                error = "Некорректная дата"
                return@TextButton
            }
            error = null
            onAdd(name, relationType.trim().ifBlank { null }, birthday)
            name = ""
            relationType = ""
            birthdayText = ""
        }) { Text("Добавить") }
    }
}

// --- История взаимодействий (interactions) ---

@Composable
private fun InteractionsSection(interactions: List<InteractionEntity>, viewModel: ContactDetailViewModel) {
    SectionCard(title = "История взаимодействий") {
        if (interactions.isEmpty()) {
            MutedText("Пока нет записей")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                interactions.forEach { interaction ->
                    val label = buildString {
                        append(INTERACTION_TYPE_LABELS.getValue(interaction.type))
                        append(" — ")
                        append(interaction.date)
                        if (!interaction.comment.isNullOrBlank()) append(": ${interaction.comment}")
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        AddInteractionRow(onAdd = viewModel::addInteraction)
    }
}

@Composable
private fun AddInteractionRow(onAdd: (InteractionType, LocalDate, String?) -> Unit) {
    var type by remember { mutableStateOf(InteractionType.MEETING) }
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterDropdown(
                modifier = Modifier.weight(1f), label = "Тип", options = InteractionType.entries,
                optionLabel = INTERACTION_TYPE_LABELS::getValue, selected = type, onSelected = { type = it },
            )
            OutlinedTextField(dateText, { dateText = it }, label = { Text("Дата (ГГГГ-ММ-ДД)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            val date = try {
                LocalDate.parse(dateText.trim())
            } catch (exc: DateTimeParseException) {
                error = "Некорректная дата"
                return@TextButton
            }
            error = null
            onAdd(type, date, comment.trim().ifBlank { null })
            comment = ""
        }) { Text("Добавить") }
    }
}

// --- Действия (actions) ---

@Composable
private fun ActionsSection(actions: List<ActionEntity>, viewModel: ContactDetailViewModel) {
    SectionCard(title = "Действия") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = viewModel::openAssignDialog) { Text("Назначить действие") }
        }
        if (actions.isEmpty()) {
            MutedText("Пока нет действий")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                actions.forEach { action ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val status = if (action.completed) "выполнено" else "в работе"
                        Text(
                            "${ACTION_TYPE_LABELS.getValue(action.type)} — срок ${action.dueDate} ($status)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (!action.completed) {
                            TextButton(onClick = { viewModel.completeAction(action.id) }) { Text("Готово") }
                        }
                    }
                }
            }
        }
    }
}
