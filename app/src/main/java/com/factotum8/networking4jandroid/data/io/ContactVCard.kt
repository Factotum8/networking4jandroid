package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import java.time.LocalDate

/**
 * Pure vCard <-> Contact conversion — DB-agnostic, mirrors
 * `app/services/contact_io.py`'s vCard half (no `vobject`: a small
 * hand-rolled reader/writer covering exactly the fields the source uses).
 *
 * Standard fields map onto Contact directly (FN->name, TEL->phone,
 * EMAIL->email, TITLE->position, NOTE->notes, BDAY->birthday,
 * ORG->companies, CATEGORIES->tags). `X-CIRCLE`/`X-CONTACT-TYPE`/
 * `X-DANGEROUS`/`X-INTERESTING`/`X-DIFFICULT` are non-standard properties,
 * populated only by our own [contactsToVCard] — never expected on an
 * externally-produced vCard. Embedded PHOTOs are skipped — `Contact.photo`
 * is a path/URL string, no file-storage backend.
 */
private const val X_CIRCLE = "X-CIRCLE"
private const val X_CONTACT_TYPE = "X-CONTACT-TYPE"
private const val X_DANGEROUS = "X-DANGEROUS"
private const val X_INTERESTING = "X-INTERESTING"
private const val X_DIFFICULT = "X-DIFFICULT"

fun parseVCard(text: String): Pair<List<ContactImportRow>, List<ImportRowError>> {
    val rows = mutableListOf<ContactImportRow>()
    val errors = mutableListOf<ImportRowError>()
    val cards = splitCards(text)

    cards.forEachIndexed { index, card ->
        val position = index + 1
        val properties = unfoldLines(card).mapNotNull(::parseProperty)
        try {
            val byName = properties.groupBy { it.name }
            val name = byName["FN"]?.firstOrNull()?.value?.takeIf { it.isNotBlank() }
                ?: throw ImportFieldException("FN (name) is required")
            val contact = ContactEntity(
                id = "",
                name = name,
                phone = byName["TEL"]?.firstOrNull()?.value,
                email = byName["EMAIL"]?.firstOrNull()?.value,
                position = byName["TITLE"]?.firstOrNull()?.value,
                notes = byName["NOTE"]?.firstOrNull()?.value,
                birthday = byName["BDAY"]?.firstOrNull()?.value?.let(::parseVCardDate),
                circle = byName[X_CIRCLE]?.firstOrNull()?.value?.let { v -> Circle.entries.firstOrNull { it.value == v } },
                contactType = byName[X_CONTACT_TYPE]?.firstOrNull()?.value?.let { v -> ContactType.entries.firstOrNull { it.value == v } },
                dangerous = byName[X_DANGEROUS]?.firstOrNull()?.value?.toIntOrNull(),
                interesting = byName[X_INTERESTING]?.firstOrNull()?.value?.toIntOrNull(),
                difficult = byName[X_DIFFICULT]?.firstOrNull()?.value?.toIntOrNull(),
            )
            val companies = byName["ORG"]?.firstOrNull()?.value?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            val tags = byName["CATEGORIES"]?.firstOrNull()?.value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            rows += ContactImportRow(contact = contact, companies = companies, tags = tags)
        } catch (exc: ImportFieldException) {
            errors += ImportRowError(position, exc.message ?: "invalid vCard")
        }
    }
    return rows to errors
}

fun contactsToVCard(contacts: List<ContactEntity>): String {
    val builder = StringBuilder()
    contacts.forEach { contact ->
        builder.append("BEGIN:VCARD\r\n").append("VERSION:3.0\r\n")
        builder.append("FN:").append(vcardEscape(contact.name)).append("\r\n")
        builder.append("N:").append(vcardEscape(contact.name)).append("\r\n")
        contact.phone?.let { builder.append("TEL:").append(vcardEscape(it)).append("\r\n") }
        contact.email?.let { builder.append("EMAIL:").append(vcardEscape(it)).append("\r\n") }
        contact.position?.let { builder.append("TITLE:").append(vcardEscape(it)).append("\r\n") }
        contact.notes?.let { builder.append("NOTE:").append(vcardEscape(it)).append("\r\n") }
        contact.birthday?.let { builder.append("BDAY:").append(it.toString()).append("\r\n") }
        contact.circle?.let { builder.append("$X_CIRCLE:").append(it.value).append("\r\n") }
        contact.contactType?.let { builder.append("$X_CONTACT_TYPE:").append(it.value).append("\r\n") }
        contact.dangerous?.let { builder.append("$X_DANGEROUS:").append(it).append("\r\n") }
        contact.interesting?.let { builder.append("$X_INTERESTING:").append(it).append("\r\n") }
        contact.difficult?.let { builder.append("$X_DIFFICULT:").append(it).append("\r\n") }
        builder.append("END:VCARD\r\n")
    }
    return builder.toString()
}

private data class VCardProperty(val name: String, val value: String)

private fun splitCards(text: String): List<String> {
    val cards = mutableListOf<String>()
    var start = -1
    val lines = text.lines()
    lines.forEachIndexed { i, line ->
        val trimmed = line.trim().uppercase()
        if (trimmed == "BEGIN:VCARD") start = i
        if (trimmed == "END:VCARD" && start >= 0) {
            cards += lines.subList(start, i + 1).joinToString("\n")
            start = -1
        }
    }
    return cards
}

/** Unfolds RFC2426-continuation lines (a line starting with a space/tab continues the previous one). */
private fun unfoldLines(card: String): List<String> {
    val raw = card.lines()
    val unfolded = mutableListOf<String>()
    raw.forEach { line ->
        if ((line.startsWith(" ") || line.startsWith("\t")) && unfolded.isNotEmpty()) {
            unfolded[unfolded.size - 1] = unfolded.last() + line.drop(1)
        } else {
            unfolded += line
        }
    }
    return unfolded
}

private fun parseProperty(line: String): VCardProperty? {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return null
    val upper = trimmed.uppercase()
    if (upper == "BEGIN:VCARD" || upper == "END:VCARD" || upper.startsWith("VERSION:")) return null
    val colonIndex = trimmed.indexOf(':')
    if (colonIndex < 0) return null
    val nameAndParams = trimmed.substring(0, colonIndex)
    val name = nameAndParams.substringBefore(";").uppercase()
    val value = vcardUnescape(trimmed.substring(colonIndex + 1))
    return VCardProperty(name, value)
}

private fun parseVCardDate(value: String): LocalDate? {
    val digitsOnly = value.take(10)
    return try {
        LocalDate.parse(digitsOnly)
    } catch (exc: Exception) {
        try {
            LocalDate.parse(value.take(8), java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
        } catch (exc2: Exception) {
            null
        }
    }
}

private fun vcardUnescape(value: String): String =
    value.replace("\\n", "\n").replace("\\N", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

private fun vcardEscape(value: String): String =
    value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
