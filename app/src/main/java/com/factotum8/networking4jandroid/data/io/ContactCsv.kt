package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Pure CSV <-> Contact conversion — DB-agnostic, mirrors
 * `app/services/contact_io.py`'s CSV half (no pandas: a hand-rolled
 * RFC4180-ish reader/writer, since pulling in a CSV library for this one
 * narrow format isn't worth a new dependency).
 */
private val CONTACT_COLUMNS = listOf(
    "name", "photo", "phone", "email", "position", "city", "birthday", "notes",
    "met_place", "met_date", "contact_type", "circle", "dangerous", "interesting", "difficult",
)
private val NUMERIC_FIELDS = setOf("dangerous", "interesting", "difficult")
private val DATE_FIELDS = setOf("birthday", "met_date")

fun parseCsv(text: String): Pair<List<ContactImportRow>, List<ImportRowError>> {
    val allRows = parseCsvRows(text)
    if (allRows.isEmpty()) return emptyList<ContactImportRow>() to emptyList()
    val header = allRows.first().map { it.trim() }
    val rows = mutableListOf<ContactImportRow>()
    val errors = mutableListOf<ImportRowError>()

    allRows.drop(1).forEachIndexed { index, rawRow ->
        val position = index + 2 // row 1 is the header
        val byColumn = header.zip(rawRow).toMap()
        try {
            val fields = rowToContactFields(byColumn)
            val contact = ContactEntity(
                id = "",
                name = fields["name"] as String,
                photo = fields["photo"] as String?,
                phone = fields["phone"] as String?,
                email = fields["email"] as String?,
                position = fields["position"] as String?,
                city = fields["city"] as String?,
                birthday = fields["birthday"] as LocalDate?,
                notes = fields["notes"] as String?,
                metPlace = fields["met_place"] as String?,
                metDate = fields["met_date"] as LocalDate?,
                contactType = fields["contact_type"] as ContactType?,
                circle = fields["circle"] as Circle?,
                dangerous = fields["dangerous"] as Int?,
                interesting = fields["interesting"] as Int?,
                difficult = fields["difficult"] as Int?,
            )
            rows += ContactImportRow(
                contact = contact,
                companies = splitList(byColumn["company"]),
                tags = splitList(byColumn["tags"]),
                interests = splitList(byColumn["interests"]),
            )
        } catch (exc: ImportFieldException) {
            errors += ImportRowError(position, exc.message ?: "invalid row")
        }
    }
    return rows to errors
}

private fun rowToContactFields(byColumn: Map<String, String>): Map<String, Any?> {
    val fields = mutableMapOf<String, Any?>()
    for (column in CONTACT_COLUMNS) {
        val raw = byColumn[column]?.trim()?.takeIf { it.isNotEmpty() } ?: continue
        fields[column] = when {
            column in NUMERIC_FIELDS -> {
                val value = raw.toIntOrNull() ?: throw ImportFieldException("$column: not a number ($raw)")
                if (value < 1 || value > 10) throw ImportFieldException("$column: must be 1-10 ($raw)")
                value
            }
            column in DATE_FIELDS -> try {
                LocalDate.parse(raw)
            } catch (exc: DateTimeParseException) {
                throw ImportFieldException("$column: invalid date ($raw)")
            }
            column == "contact_type" -> ContactType.entries.firstOrNull { it.value == raw }
                ?: throw ImportFieldException("contact_type: unknown value ($raw)")
            column == "circle" -> Circle.entries.firstOrNull { it.value == raw }
                ?: throw ImportFieldException("circle: unknown value ($raw)")
            else -> raw
        }
    }
    if (fields["name"] !is String || (fields["name"] as String).isBlank()) {
        throw ImportFieldException("name is required")
    }
    return fields
}

private fun splitList(value: String?): List<String> =
    value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

/** Excludes `id`, matching the source (only Contact's own fields round-trip export -> import). */
fun contactsToCsv(contacts: List<ContactEntity>): String {
    val builder = StringBuilder()
    builder.append(CONTACT_COLUMNS.joinToString(",") { csvEscape(it) }).append("\r\n")
    contacts.forEach { contact ->
        val values = listOf(
            contact.name, contact.photo, contact.phone, contact.email, contact.position, contact.city,
            contact.birthday?.toString(), contact.notes, contact.metPlace, contact.metDate?.toString(),
            contact.contactType?.value, contact.circle?.value, contact.dangerous?.toString(),
            contact.interesting?.toString(), contact.difficult?.toString(),
        )
        builder.append(values.joinToString(",") { csvEscape(it ?: "") }).append("\r\n")
    }
    return builder.toString()
}

private fun csvEscape(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

/** RFC4180-ish reader: handles quoted fields with embedded commas/newlines and doubled-quote escaping. */
private fun parseCsvRows(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    var field = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            when {
                c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = false
                else -> field.append(c)
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(field.toString()); field = StringBuilder() }
                '\r' -> {}
                '\n' -> { row.add(field.toString()); field = StringBuilder(); rows.add(row); row = mutableListOf() }
                else -> field.append(c)
            }
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
    return rows.filter { it.isNotEmpty() && !(it.size == 1 && it[0].isEmpty()) }
}
