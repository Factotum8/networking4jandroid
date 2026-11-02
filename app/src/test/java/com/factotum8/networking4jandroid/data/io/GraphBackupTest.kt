package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.CompanyEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [buildBackupJson] is a hand-rolled serializer with no matching parser (export-only, see its
 * kdoc), so these tests assert on the produced string directly rather than round-tripping.
 */
class GraphBackupTest {

    private fun emptyData(settings: SettingsEntity = SettingsEntity()) = GraphBackupData(
        exportedAt = Instant.parse("2024-01-01T00:00:00Z"),
        contacts = emptyList(),
        interactions = emptyList(),
        actions = emptyList(),
        relatives = emptyList(),
        knows = emptyList(),
        dimensionLinks = emptyList(),
        companies = emptyList(),
        events = emptyList(),
        communities = emptyList(),
        projects = emptyList(),
        interests = emptyList(),
        tags = emptyList(),
        settings = settings,
    )

    @Test
    fun `buildBackupJson produces the exact deterministic shape for empty collections`() {
        val json = buildBackupJson(emptyData())

        assertEquals(
            "{\"exportedAt\":\"2024-01-01T00:00:00Z\",\"contacts\":[],\"interactions\":[],\"actions\":[]," +
                "\"relatives\":[],\"knows\":[],\"dimensionLinks\":[],\"companies\":[],\"events\":[]," +
                "\"communities\":[],\"projects\":[],\"interests\":[],\"tags\":[]," +
                "\"settings\":{\"staleContactDays\":60,\"dedupScoreThreshold\":85.0}}",
            json,
        )
    }

    /** Mirrors jStr's escaping *spec* (not its private implementation, which tests can't call). */
    private fun jsonQuoted(raw: String): String = buildString {
        append('"')
        raw.forEach { c ->
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c.code < 0x20 -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
        append('"')
    }

    @Test
    fun `buildBackupJson includes the contact id, unlike the CSV export`() {
        val contact = ContactEntity(id = "contact-1", name = "Ann")
        val json = buildBackupJson(emptyData().copy(contacts = listOf(contact)))

        assertTrue(json.contains("\"id\":\"contact-1\""))
        assertTrue(json.contains("\"name\":\"Ann\""))
    }

    @Test
    fun `buildBackupJson writes null for absent optional contact fields`() {
        val contact = ContactEntity(id = "1", name = "Ann")
        val json = buildBackupJson(emptyData().copy(contacts = listOf(contact)))

        assertTrue(json.contains("\"phone\":null"))
        assertTrue(json.contains("\"birthday\":null"))
        assertTrue(json.contains("\"dangerous\":null"))
        assertTrue(json.contains("\"archived\":false"))
        assertTrue(json.contains("\"archivedAt\":null"))
    }

    @Test
    fun `buildBackupJson escapes quotes, backslashes, whitespace and control characters in strings`() {
        val raw = "Ann \"Q\" \\ line" + "\n" + "break" + "\t" + "tab" + Character.toString(1) + "end"
        val contact = ContactEntity(id = "1", name = raw)
        val json = buildBackupJson(emptyData().copy(contacts = listOf(contact)))

        assertTrue(json.contains("\"name\":${jsonQuoted(raw)}"))
    }

    @Test
    fun `buildBackupJson serializes numeric and boolean contact fields`() {
        val contact = ContactEntity(id = "1", name = "Ann", dangerous = 5, interesting = 10, difficult = 1, archived = true)
        val json = buildBackupJson(emptyData().copy(contacts = listOf(contact)))

        assertTrue(json.contains("\"dangerous\":5"))
        assertTrue(json.contains("\"interesting\":10"))
        assertTrue(json.contains("\"difficult\":1"))
        assertTrue(json.contains("\"archived\":true"))
    }

    @Test
    fun `buildBackupJson joins multiple contacts with a comma and no trailing comma`() {
        val contacts = listOf(ContactEntity(id = "1", name = "Ann"), ContactEntity(id = "2", name = "Bob"))
        val json = buildBackupJson(emptyData().copy(contacts = contacts))

        val contactsArray = json.substringAfter("\"contacts\":").substringBefore(",\"interactions\"")
        assertTrue(contactsArray.startsWith("[{\"id\":\"1\",\"name\":\"Ann\""))
        assertTrue(contactsArray.contains("},{\"id\":\"2\",\"name\":\"Bob\""))
        assertTrue(contactsArray.endsWith("}]"))
    }

    @Test
    fun `buildBackupJson serializes interactions, actions, relatives and knows`() {
        val data = emptyData().copy(
            interactions = listOf(
                InteractionEntity(id = "i1", contactId = "c1", type = InteractionType.CALL, date = LocalDate.of(2024, 2, 1), comment = "hi"),
            ),
            actions = listOf(
                ActionEntity(id = "a1", contactId = "c1", type = ActionType.MEET, dueDate = LocalDate.of(2024, 3, 1)),
            ),
            relatives = listOf(RelativeEntity(id = "r1", contactId = "c1", name = "Mom", relationType = "mother")),
            knows = listOf(KnowsEntity(contactId = "c1", relatedContactId = "c2", type = "friend", closeness = 3)),
        )

        val json = buildBackupJson(data)

        assertTrue(json.contains("\"type\":\"call\""))
        assertTrue(json.contains("\"comment\":\"hi\""))
        assertTrue(json.contains("\"type\":\"meet\""))
        assertTrue(json.contains("\"completed\":false"))
        assertTrue(json.contains("\"relationType\":\"mother\""))
        assertTrue(json.contains("\"relatedContactId\":\"c2\""))
        assertTrue(json.contains("\"type\":\"friend\""))
        assertTrue(json.contains("\"closeness\":3"))
    }

    @Test
    fun `buildBackupJson serializes dimension links and named nodes without a wrapping id-name type`() {
        val data = emptyData().copy(
            dimensionLinks = listOf(DimensionLinkRecord("c1", DimensionLinkType.WORKS_AT, "co1", "Acme")),
            companies = listOf(CompanyEntity(id = "co1", name = "Acme")),
        )

        val json = buildBackupJson(data)

        assertTrue(json.contains("\"relType\":\"WORKS_AT\""))
        assertTrue(json.contains("\"targetId\":\"co1\""))
        assertTrue(json.contains("\"targetName\":\"Acme\""))
        val companiesArray = json.substringAfter("\"companies\":").substringBefore(",\"events\"")
        assertEquals("[{\"id\":\"co1\",\"name\":\"Acme\"}]", companiesArray)
    }

    @Test
    fun `buildBackupJson serializes settings with the double formatted as a decimal`() {
        val json = buildBackupJson(emptyData(SettingsEntity(staleContactDays = 30, dedupScoreThreshold = 90.5)))

        assertTrue(json.contains("\"settings\":{\"staleContactDays\":30,\"dedupScoreThreshold\":90.5}"))
    }
}
