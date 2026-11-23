package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.CommunityEntity
import com.factotum8.networking4jandroid.data.local.entity.CompanyEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactCommunityCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactCompanyCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEventCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactInterestCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactProjectCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactTagCrossRef
import com.factotum8.networking4jandroid.data.local.entity.EventEntity
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.InterestEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.ProjectEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import com.factotum8.networking4jandroid.data.local.entity.TagEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/** Upper bound on a backup file read from the user's storage — guards against OOM on a huge or wrong file. */
const val MAX_DESKTOP_BACKUP_BYTES: Long = 20L * 1024 * 1024

/** The desktop backup is unreadable as a whole; nothing must be written to the database. Message is user-facing (Russian). */
class BackupFormatException(message: String) : Exception(message)

/**
 * Everything restored from a desktop backup, already validated and
 * de-duplicated: every reference points at a row that is present here,
 * so it can be inserted as-is into an emptied database.
 */
data class BackupRestoreData(
    val contacts: List<ContactEntity>,
    val interactions: List<InteractionEntity>,
    val actions: List<ActionEntity>,
    val relatives: List<RelativeEntity>,
    val knows: List<KnowsEntity>,
    val companies: List<CompanyEntity>,
    val events: List<EventEntity>,
    val communities: List<CommunityEntity>,
    val projects: List<ProjectEntity>,
    val interests: List<InterestEntity>,
    val tags: List<TagEntity>,
    val companyLinks: List<ContactCompanyCrossRef>,
    val eventLinks: List<ContactEventCrossRef>,
    val communityLinks: List<ContactCommunityCrossRef>,
    val projectLinks: List<ContactProjectCrossRef>,
    val interestLinks: List<ContactInterestCrossRef>,
    val tagLinks: List<ContactTagCrossRef>,
    val settings: SettingsEntity,
    /** Records dropped because they lack an id or point at a contact/node absent from the file. */
    val skipped: Int,
) {
    val dimensionLinkCount: Int
        get() = companyLinks.size + eventLinks.size + communityLinks.size + projectLinks.size + interestLinks.size + tagLinks.size
}

/**
 * Parses the full-graph JSON written by the desktop networking4j app
 * (`app/services/backup_export.py::write_backup_files`, i.e.
 * `GraphBackup.model_dump_json()` — snake_case keys, nested
 * `{contact_id, interaction: {...}}`-style records). This is a different
 * shape from this app's own export ([buildBackupJson]).
 *
 * Policy: a structurally broken file or an unknown enum value aborts the
 * whole import ([BackupFormatException]); individual records with a missing
 * id or a dangling reference are skipped and counted in
 * [BackupRestoreData.skipped]. `networking_goal`/`monthly_goals` are
 * ignored — this app has no such feature.
 */
fun parseDesktopBackup(text: String): BackupRestoreData {
    val root = try {
        Json.parseToJsonElement(text)
    } catch (e: SerializationException) {
        throw BackupFormatException("Файл не является корректным JSON")
    } catch (e: IllegalArgumentException) {
        throw BackupFormatException("Файл не является корректным JSON")
    }
    if (root !is JsonObject || "contacts" !in root || "exported_at" !in root) {
        throw BackupFormatException("Это не бэкап networking4j (нет полей contacts/exported_at)")
    }
    var skipped = 0

    val contacts = root.objects("contacts").mapNotNull { obj ->
        val id = obj.str("id")
        if (id.isNullOrBlank()) {
            skipped++
            null
        } else {
            parseContact(id, obj)
        }
    }.distinctBy { it.id }
    val contactIds = contacts.mapTo(HashSet()) { it.id }

    fun <T> nested(section: String, childKey: String, build: (contactId: String, child: JsonObject) -> T?): List<T> =
        root.objects(section).mapNotNull { record ->
            val contactId = record.str("contact_id")
            val child = record.obj(childKey)
            val result = if (contactId != null && contactId in contactIds) build(contactId, child) else null
            if (result == null) skipped++
            result
        }

    val interactions = nested("interactions", "interaction") { contactId, obj ->
        obj.str("id")?.takeIf { it.isNotBlank() }?.let { id ->
            InteractionEntity(
                id = id,
                contactId = contactId,
                type = obj.enumValue("type", InteractionType.entries, InteractionType::value)
                    ?: throw BackupFormatException("Взаимодействие $id: не указан тип"),
                date = obj.date("date") ?: throw BackupFormatException("Взаимодействие $id: не указана дата"),
                comment = obj.str("comment"),
            )
        }
    }.distinctBy { it.id }

    val actions = nested("actions", "action") { contactId, obj ->
        obj.str("id")?.takeIf { it.isNotBlank() }?.let { id ->
            ActionEntity(
                id = id,
                contactId = contactId,
                type = obj.enumValue("type", ActionType.entries, ActionType::value)
                    ?: throw BackupFormatException("Задача $id: не указан тип"),
                dueDate = obj.date("due_date") ?: throw BackupFormatException("Задача $id: не указан срок"),
                completed = obj.bool("completed") ?: false,
                completedAt = obj.instant("completed_at"),
            )
        }
    }.distinctBy { it.id }

    val relatives = nested("relatives", "relative") { contactId, obj ->
        obj.str("id")?.takeIf { it.isNotBlank() }?.let { id ->
            RelativeEntity(
                id = id,
                contactId = contactId,
                name = obj.str("name") ?: throw BackupFormatException("Родственник $id: не указано имя"),
                relationType = obj.str("relation_type"),
                birthday = obj.date("birthday"),
            )
        }
    }.distinctBy { it.id }

    val knows = root.objects("knows").mapNotNull { record ->
        val from = record.str("from_id")
        val to = record.str("to_id")
        if (from == null || to == null || from !in contactIds || to !in contactIds) {
            skipped++
            return@mapNotNull null
        }
        val props = record.obj("knows")
        KnowsEntity(
            contactId = from,
            relatedContactId = to,
            type = props.str("type"),
            date = props.date("date"),
            comment = props.str("comment"),
            closeness = props.int("closeness"),
        )
    }.distinctBy { it.contactId to it.relatedContactId }

    fun namedNodes(section: String): List<Pair<String, String>> = root.objects(section).mapNotNull { obj ->
        val id = obj.str("id")
        val name = obj.str("name")
        if (id.isNullOrBlank() || name == null) {
            skipped++
            null
        } else {
            id to name
        }
    }.distinctBy { it.first }

    val companies = namedNodes("companies").map { (id, name) -> CompanyEntity(id, name) }
    val events = namedNodes("events").map { (id, name) -> EventEntity(id, name) }
    val communities = namedNodes("communities").map { (id, name) -> CommunityEntity(id, name) }
    val projects = namedNodes("projects").map { (id, name) -> ProjectEntity(id, name) }
    val interests = namedNodes("interests").map { (id, name) -> InterestEntity(id, name) }
    val tags = namedNodes("tags").map { (id, name) -> TagEntity(id, name) }

    val targetIds: Map<DimensionLinkType, Set<String>> = mapOf(
        DimensionLinkType.WORKS_AT to companies.mapTo(HashSet()) { it.id },
        DimensionLinkType.ATTENDED to events.mapTo(HashSet()) { it.id },
        DimensionLinkType.MEMBER_OF to communities.mapTo(HashSet()) { it.id },
        DimensionLinkType.INVOLVED_IN to projects.mapTo(HashSet()) { it.id },
        DimensionLinkType.INTERESTED_IN to interests.mapTo(HashSet()) { it.id },
        DimensionLinkType.TAGGED to tags.mapTo(HashSet()) { it.id },
    )
    val links = mutableMapOf<DimensionLinkType, MutableSet<Pair<String, String>>>()
    root.objects("dimension_links").forEach { record ->
        val relType = record.enumValue("rel_type", DimensionLinkType.entries, DimensionLinkType::value)
        val contactId = record.str("contact_id")
        val targetId = record.str("target_id")
        if (relType == null || contactId == null || targetId == null ||
            contactId !in contactIds || targetId !in targetIds.getValue(relType)
        ) {
            skipped++
        } else {
            links.getOrPut(relType) { LinkedHashSet() } += contactId to targetId
        }
    }
    fun linksOf(type: DimensionLinkType) = links[type].orEmpty().toList()

    val settingsObj = root["settings"] as? JsonObject
    val defaults = SettingsEntity()
    val settings = SettingsEntity(
        staleContactDays = settingsObj?.int("stale_contact_days")?.takeIf { it >= 1 } ?: defaults.staleContactDays,
        dedupScoreThreshold = settingsObj?.double("dedup_score_threshold")?.takeIf { it in 0.0..100.0 }
            ?: defaults.dedupScoreThreshold,
    )

    return BackupRestoreData(
        contacts = contacts,
        interactions = interactions,
        actions = actions,
        relatives = relatives,
        knows = knows,
        companies = companies,
        events = events,
        communities = communities,
        projects = projects,
        interests = interests,
        tags = tags,
        companyLinks = linksOf(DimensionLinkType.WORKS_AT).map { ContactCompanyCrossRef(it.first, it.second) },
        eventLinks = linksOf(DimensionLinkType.ATTENDED).map { ContactEventCrossRef(it.first, it.second) },
        communityLinks = linksOf(DimensionLinkType.MEMBER_OF).map { ContactCommunityCrossRef(it.first, it.second) },
        projectLinks = linksOf(DimensionLinkType.INVOLVED_IN).map { ContactProjectCrossRef(it.first, it.second) },
        interestLinks = linksOf(DimensionLinkType.INTERESTED_IN).map { ContactInterestCrossRef(it.first, it.second) },
        tagLinks = linksOf(DimensionLinkType.TAGGED).map { ContactTagCrossRef(it.first, it.second) },
        settings = settings,
        skipped = skipped,
    )
}

private fun parseContact(id: String, obj: JsonObject) = ContactEntity(
    id = id,
    name = obj.str("name") ?: throw BackupFormatException("Контакт $id: не указано имя"),
    photo = obj.str("photo"),
    phone = obj.str("phone"),
    email = obj.str("email"),
    position = obj.str("position"),
    city = obj.str("city"),
    birthday = obj.date("birthday"),
    notes = obj.str("notes"),
    metPlace = obj.str("met_place"),
    metDate = obj.date("met_date"),
    contactType = obj.enumValue("contact_type", ContactType.entries, ContactType::value),
    circle = obj.enumValue("circle", Circle.entries, Circle::value),
    dangerous = obj.int("dangerous"),
    interesting = obj.int("interesting"),
    difficult = obj.int("difficult"),
    archived = obj.bool("archived") ?: false,
    archivedAt = obj.instant("archived_at"),
    createdAt = obj.instant("created_at"),
    updatedAt = obj.instant("updated_at"),
)

// --- typed field access; a present-but-wrong-typed value is a format error, a missing/null one is null ---

private fun JsonObject.objects(key: String): List<JsonObject> = when (val value = this[key]) {
    null, JsonNull -> emptyList()
    is JsonArray -> value.map { it as? JsonObject ?: throw BackupFormatException("Раздел «$key»: ожидался список объектов") }
    else -> throw BackupFormatException("Раздел «$key»: ожидался список")
}

private fun JsonObject.obj(key: String): JsonObject = when (val value = this[key]) {
    null, JsonNull -> JsonObject(emptyMap())
    is JsonObject -> value
    else -> throw BackupFormatException("Поле «$key»: ожидался объект")
}

private fun JsonObject.primitive(key: String): JsonPrimitive? = when (val value: JsonElement? = this[key]) {
    null, JsonNull -> null
    is JsonPrimitive -> value
    else -> throw BackupFormatException("Поле «$key»: ожидалось простое значение")
}

private fun JsonObject.str(key: String): String? = primitive(key)?.let {
    if (it.isString) it.content else throw BackupFormatException("Поле «$key»: ожидалась строка")
}

private fun JsonObject.int(key: String): Int? = primitive(key)?.let {
    (if (it.isString) null else it.intOrNull) ?: throw BackupFormatException("Поле «$key»: ожидалось целое число")
}

private fun JsonObject.double(key: String): Double? = primitive(key)?.let {
    (if (it.isString) null else it.doubleOrNull) ?: throw BackupFormatException("Поле «$key»: ожидалось число")
}

private fun JsonObject.bool(key: String): Boolean? = primitive(key)?.let {
    (if (it.isString) null else it.booleanOrNull) ?: throw BackupFormatException("Поле «$key»: ожидалось true/false")
}

private fun JsonObject.date(key: String): LocalDate? = str(key)?.let {
    try {
        LocalDate.parse(it)
    } catch (e: DateTimeParseException) {
        throw BackupFormatException("Поле «$key»: некорректная дата")
    }
}

/** Pydantic writes timezone-aware datetimes with `Z`/`+hh:mm`; a naive one is taken as UTC. */
private fun JsonObject.instant(key: String): Instant? = str(key)?.let {
    try {
        OffsetDateTime.parse(it).toInstant()
    } catch (e: DateTimeParseException) {
        try {
            LocalDateTime.parse(it).toInstant(ZoneOffset.UTC)
        } catch (e2: DateTimeParseException) {
            throw BackupFormatException("Поле «$key»: некорректные дата и время")
        }
    }
}

private fun <E> JsonObject.enumValue(key: String, entries: List<E>, value: (E) -> String): E? = str(key)?.let { raw ->
    entries.firstOrNull { value(it) == raw }
        ?: throw BackupFormatException("Поле «$key»: неизвестное значение «${raw.take(40)}»")
}
