package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.CommunityEntity
import com.factotum8.networking4jandroid.data.local.entity.CompanyEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.EventEntity
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.InterestEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.ProjectEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import com.factotum8.networking4jandroid.data.local.entity.TagEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import java.time.Instant

/**
 * One flattened `(:Contact)-[REL]->(:DimensionNode)` edge — the Kotlin
 * analogue of `app.models.backup.DimensionLinkRecord`, since the six
 * cross-ref tables don't carry the target's name on their own.
 */
data class DimensionLinkRecord(
    val contactId: String,
    val relType: DimensionLinkType,
    val targetId: String,
    val targetName: String,
)

/**
 * Everything a full-graph backup needs, pre-fetched — the Kotlin analogue
 * of `app.models.backup.GraphBackup`, minus `networkingGoal`/`monthlyGoals`
 * (out of scope for this port, see the plan: not in the source's own nav).
 */
data class GraphBackupData(
    val exportedAt: Instant,
    val contacts: List<ContactEntity>,
    val interactions: List<InteractionEntity>,
    val actions: List<ActionEntity>,
    val relatives: List<RelativeEntity>,
    val knows: List<KnowsEntity>,
    val dimensionLinks: List<DimensionLinkRecord>,
    val companies: List<CompanyEntity>,
    val events: List<EventEntity>,
    val communities: List<CommunityEntity>,
    val projects: List<ProjectEntity>,
    val interests: List<InterestEntity>,
    val tags: List<TagEntity>,
    val settings: SettingsEntity,
)

/**
 * Serializes [data] to the full-graph JSON backup file, mirroring
 * `backup_export.py::build_backup`'s `model_dump_json`. Hand-rolled rather
 * than pulling in a JSON library, for exactly one fixed, known-in-advance
 * shape — the same reasoning as [contactsToCsv]/[contactsToVCard]. Export
 * only: this isn't meant to be read back into the app (see the source
 * module's docstring), so there's no matching parser.
 */
fun buildBackupJson(data: GraphBackupData): String = jObj(
    "exportedAt" to jStr(data.exportedAt.toString()),
    "contacts" to jArr(data.contacts.map(::contactJson)),
    "interactions" to jArr(data.interactions.map(::interactionJson)),
    "actions" to jArr(data.actions.map(::actionJson)),
    "relatives" to jArr(data.relatives.map(::relativeJson)),
    "knows" to jArr(data.knows.map(::knowsJson)),
    "dimensionLinks" to jArr(data.dimensionLinks.map(::dimensionLinkJson)),
    "companies" to jArr(data.companies.map { namedNodeJson(it.id, it.name) }),
    "events" to jArr(data.events.map { namedNodeJson(it.id, it.name) }),
    "communities" to jArr(data.communities.map { namedNodeJson(it.id, it.name) }),
    "projects" to jArr(data.projects.map { namedNodeJson(it.id, it.name) }),
    "interests" to jArr(data.interests.map { namedNodeJson(it.id, it.name) }),
    "tags" to jArr(data.tags.map { namedNodeJson(it.id, it.name) }),
    "settings" to settingsJson(data.settings),
)

private fun contactJson(contact: ContactEntity): String = jObj(
    "id" to jStr(contact.id),
    "name" to jStr(contact.name),
    "photo" to jStr(contact.photo),
    "phone" to jStr(contact.phone),
    "email" to jStr(contact.email),
    "position" to jStr(contact.position),
    "city" to jStr(contact.city),
    "birthday" to jStr(contact.birthday?.toString()),
    "notes" to jStr(contact.notes),
    "metPlace" to jStr(contact.metPlace),
    "metDate" to jStr(contact.metDate?.toString()),
    "contactType" to jStr(contact.contactType?.value),
    "circle" to jStr(contact.circle?.value),
    "dangerous" to jNum(contact.dangerous),
    "interesting" to jNum(contact.interesting),
    "difficult" to jNum(contact.difficult),
    "archived" to jBool(contact.archived),
    "archivedAt" to jStr(contact.archivedAt?.toString()),
    "createdAt" to jStr(contact.createdAt?.toString()),
    "updatedAt" to jStr(contact.updatedAt?.toString()),
)

private fun interactionJson(interaction: InteractionEntity): String = jObj(
    "id" to jStr(interaction.id),
    "contactId" to jStr(interaction.contactId),
    "type" to jStr(interaction.type.value),
    "date" to jStr(interaction.date.toString()),
    "comment" to jStr(interaction.comment),
)

private fun actionJson(action: ActionEntity): String = jObj(
    "id" to jStr(action.id),
    "contactId" to jStr(action.contactId),
    "type" to jStr(action.type.value),
    "dueDate" to jStr(action.dueDate.toString()),
    "completed" to jBool(action.completed),
    "completedAt" to jStr(action.completedAt?.toString()),
)

private fun relativeJson(relative: RelativeEntity): String = jObj(
    "id" to jStr(relative.id),
    "contactId" to jStr(relative.contactId),
    "name" to jStr(relative.name),
    "relationType" to jStr(relative.relationType),
    "birthday" to jStr(relative.birthday?.toString()),
)

private fun knowsJson(knows: KnowsEntity): String = jObj(
    "contactId" to jStr(knows.contactId),
    "relatedContactId" to jStr(knows.relatedContactId),
    "type" to jStr(knows.type),
    "date" to jStr(knows.date?.toString()),
    "comment" to jStr(knows.comment),
    "closeness" to jNum(knows.closeness),
)

private fun dimensionLinkJson(link: DimensionLinkRecord): String = jObj(
    "contactId" to jStr(link.contactId),
    "relType" to jStr(link.relType.value),
    "targetId" to jStr(link.targetId),
    "targetName" to jStr(link.targetName),
)

private fun namedNodeJson(id: String, name: String): String = jObj(
    "id" to jStr(id),
    "name" to jStr(name),
)

private fun settingsJson(settings: SettingsEntity): String = jObj(
    "staleContactDays" to jNum(settings.staleContactDays),
    "dedupScoreThreshold" to jNum(settings.dedupScoreThreshold),
)

private fun jObj(vararg fields: Pair<String, String>): String =
    fields.joinToString(",", "{", "}") { (key, value) -> "${jStr(key)}:$value" }

private fun jArr(items: List<String>): String = items.joinToString(",", "[", "]")

private fun jNum(value: Number?): String = value?.toString() ?: "null"

private fun jBool(value: Boolean): String = value.toString()

private fun jStr(value: String?): String {
    if (value == null) return "null"
    val builder = StringBuilder(value.length + 2).append('"')
    value.forEach { c ->
        when {
            c == '"' -> builder.append("\\\"")
            c == '\\' -> builder.append("\\\\")
            c == '\n' -> builder.append("\\n")
            c == '\r' -> builder.append("\\r")
            c == '\t' -> builder.append("\\t")
            c.code < 0x20 -> builder.append("\\u%04x".format(c.code))
            else -> builder.append(c)
        }
    }
    return builder.append('"').toString()
}
