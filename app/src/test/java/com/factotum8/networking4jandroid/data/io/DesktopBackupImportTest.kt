package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ContactCompanyCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactTagCrossRef
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Fixtures follow desktop networking4j's `GraphBackup.model_dump_json()` shape (snake_case, nested records). */
class DesktopBackupImportTest {

    private fun contact(id: String, name: String, extra: String = "") =
        """{"id":"$id","name":"$name"${if (extra.isEmpty()) "" else ",$extra"}}"""

    private fun backup(
        contacts: String = "[]",
        interactions: String = "[]",
        actions: String = "[]",
        relatives: String = "[]",
        knows: String = "[]",
        dimensionLinks: String = "[]",
        companies: String = "[]",
        tags: String = "[]",
        settings: String = """{"id":"s1","stale_contact_days":30,"dedup_score_threshold":90.0}""",
    ) = """
        {
          "exported_at": "2026-11-20T10:00:00Z",
          "contacts": $contacts,
          "interactions": $interactions,
          "actions": $actions,
          "relatives": $relatives,
          "knows": $knows,
          "dimension_links": $dimensionLinks,
          "companies": $companies,
          "events": [],
          "communities": [],
          "projects": [],
          "interests": [],
          "tags": $tags,
          "networking_goal": {"id":"g1","target":5},
          "monthly_goals": [],
          "settings": $settings
        }
    """.trimIndent()

    private fun assertFormatError(text: String, messagePart: String) {
        try {
            parseDesktopBackup(text)
            fail("expected BackupFormatException")
        } catch (e: BackupFormatException) {
            assertTrue("message was: ${e.message}", e.message!!.contains(messagePart))
        }
    }

    @Test
    fun `parses a full desktop backup`() {
        val text = backup(
            contacts = """[
                ${contact("4:a:1", "Анна", """"phone":"+100","birthday":"1990-05-01","met_place":"Conf","met_date":"2024-03-02","contact_type":"connector","circle":"support_circle","dangerous":2,"interesting":9,"difficult":null,"archived":false,"archived_at":null,"created_at":"2024-01-01T08:00:00.123456Z","updated_at":"2024-02-01T08:00:00+03:00"""")},
                ${contact("4:a:2", "Boris", """"archived":true,"archived_at":"2024-06-01T00:00:00"""")}
            ]""",
            interactions = """[{"contact_id":"4:a:1","interaction":{"id":"i1","type":"meeting","date":"2024-04-01","comment":"coffee"}}]""",
            actions = """[{"contact_id":"4:a:1","contact_name":"Анна","action":{"id":"x1","type":"follow_up_later","due_date":"2024-05-01","completed":true,"completed_at":"2024-05-02T09:00:00Z"}}]""",
            relatives = """[{"contact_id":"4:a:2","relative":{"id":"r1","name":"Kid","relation_type":"child","birthday":"2015-01-01"}}]""",
            knows = """[{"from_id":"4:a:1","to_id":"4:a:2","knows":{"type":"colleague","date":"2023-01-01","comment":null,"closeness":7}}]""",
            dimensionLinks = """[
                {"contact_id":"4:a:1","rel_type":"WORKS_AT","target_label":"Company","target_id":"c1"},
                {"contact_id":"4:a:2","rel_type":"TAGGED","target_label":"Tag","target_id":"t1"}
            ]""",
            companies = """[{"id":"c1","name":"Acme"}]""",
            tags = """[{"id":"t1","name":"friends"}]""",
        )

        val data = parseDesktopBackup(text)

        assertEquals(2, data.contacts.size)
        val anna = data.contacts.first { it.id == "4:a:1" }
        assertEquals("Анна", anna.name)
        assertEquals("+100", anna.phone)
        assertEquals(LocalDate.of(1990, 5, 1), anna.birthday)
        assertEquals("Conf", anna.metPlace)
        assertEquals(LocalDate.of(2024, 3, 2), anna.metDate)
        assertEquals(ContactType.CONNECTOR, anna.contactType)
        assertEquals(Circle.SUPPORT_CIRCLE, anna.circle)
        assertEquals(2, anna.dangerous)
        assertEquals(9, anna.interesting)
        assertNull(anna.difficult)
        assertEquals(Instant.parse("2024-01-01T08:00:00.123456Z"), anna.createdAt)
        assertEquals(Instant.parse("2024-02-01T05:00:00Z"), anna.updatedAt)
        val boris = data.contacts.first { it.id == "4:a:2" }
        assertTrue(boris.archived)
        assertEquals(Instant.parse("2024-06-01T00:00:00Z"), boris.archivedAt) // naive datetime taken as UTC

        assertEquals(InteractionType.MEETING, data.interactions.single().type)
        assertEquals("coffee", data.interactions.single().comment)
        val action = data.actions.single()
        assertEquals(ActionType.FOLLOW_UP_LATER, action.type)
        assertTrue(action.completed)
        assertEquals(Instant.parse("2024-05-02T09:00:00Z"), action.completedAt)
        assertEquals("child", data.relatives.single().relationType)
        assertEquals(KnowsEntity("4:a:1", "4:a:2", "colleague", LocalDate.of(2023, 1, 1), null, 7), data.knows.single())
        assertEquals(listOf(ContactCompanyCrossRef("4:a:1", "c1")), data.companyLinks)
        assertEquals(listOf(ContactTagCrossRef("4:a:2", "t1")), data.tagLinks)
        assertEquals(2, data.dimensionLinkCount)
        assertEquals("Acme", data.companies.single().name)
        assertEquals(30, data.settings.staleContactDays)
        assertEquals(90.0, data.settings.dedupScoreThreshold, 0.0)
        assertEquals(0, data.skipped)
    }

    @Test
    fun `dangling references and id-less records are skipped and counted`() {
        val text = backup(
            contacts = """[${contact("a", "A")}, {"id":null,"name":"NoId"}]""",
            interactions = """[{"contact_id":"ghost","interaction":{"id":"i1","type":"call","date":"2024-01-01"}}]""",
            actions = """[{"contact_id":"a","contact_name":"A","action":{"id":null,"type":"call","due_date":"2024-01-01"}}]""",
            knows = """[{"from_id":"a","to_id":"ghost","knows":{}}]""",
            dimensionLinks = """[{"contact_id":"a","rel_type":"WORKS_AT","target_label":"Company","target_id":"missing"}]""",
        )

        val data = parseDesktopBackup(text)

        assertEquals(listOf("a"), data.contacts.map { it.id })
        assertTrue(data.interactions.isEmpty())
        assertTrue(data.actions.isEmpty())
        assertTrue(data.knows.isEmpty())
        assertTrue(data.companyLinks.isEmpty())
        assertEquals(5, data.skipped)
    }

    @Test
    fun `duplicate knows pairs and links are collapsed but both directions are kept`() {
        val text = backup(
            contacts = """[${contact("a", "A")}, ${contact("b", "B")}]""",
            knows = """[
                {"from_id":"a","to_id":"b","knows":{"closeness":1}},
                {"from_id":"a","to_id":"b","knows":{"closeness":2}},
                {"from_id":"b","to_id":"a","knows":{}}
            ]""",
            dimensionLinks = """[
                {"contact_id":"a","rel_type":"TAGGED","target_label":"Tag","target_id":"t"},
                {"contact_id":"a","rel_type":"TAGGED","target_label":"Tag","target_id":"t"}
            ]""",
            tags = """[{"id":"t","name":"x"}, {"id":"t","name":"x"}]""",
        )

        val data = parseDesktopBackup(text)

        assertEquals(setOf("a" to "b", "b" to "a"), data.knows.map { it.contactId to it.relatedContactId }.toSet())
        assertEquals(1, data.tagLinks.size)
        assertEquals(1, data.tags.size)
    }

    @Test
    fun `missing or null settings fall back to defaults`() {
        val data = parseDesktopBackup(backup(settings = "null"))
        assertEquals(SettingsEntity(), data.settings)
    }

    @Test
    fun `unknown enum value aborts the import`() {
        assertFormatError(backup(contacts = """[${contact("a", "A", "\"circle\":\"inner_circle\"")}]"""), "inner_circle")
        assertFormatError(
            backup(
                contacts = """[${contact("a", "A")}]""",
                dimensionLinks = """[{"contact_id":"a","rel_type":"LIKES","target_id":"t"}]""",
            ),
            "LIKES",
        )
    }

    @Test
    fun `broken JSON is rejected`() {
        assertFormatError("{not json", "JSON")
        assertFormatError("", "JSON")
    }

    @Test
    fun `JSON that is not a desktop backup is rejected`() {
        assertFormatError("[]", "не бэкап")
        assertFormatError("""{"contacts":[]}""", "не бэкап")
        // This app's own export is camelCase (exportedAt) and must not be mistaken for a desktop backup.
        assertFormatError("""{"exportedAt":"2024-01-01T00:00:00Z","contacts":[]}""", "не бэкап")
    }

    @Test
    fun `wrongly typed fields are rejected`() {
        assertFormatError(backup(contacts = """{"id":"a"}"""), "contacts")
        assertFormatError(backup(contacts = """[${contact("a", "A", "\"dangerous\":\"high\"")}]"""), "dangerous")
        assertFormatError(backup(contacts = """[${contact("a", "A", "\"birthday\":\"01.02.1990\"")}]"""), "birthday")
        assertFormatError(backup(contacts = """[{"id":"a"}]"""), "имя")
    }

    @Test
    fun `required fields of nested records are enforced`() {
        assertFormatError(
            backup(
                contacts = """[${contact("a", "A")}]""",
                interactions = """[{"contact_id":"a","interaction":{"id":"i1","type":"call"}}]""",
            ),
            "дата",
        )
    }

    @Test
    fun `empty backup parses to empty data`() {
        val data = parseDesktopBackup(backup())
        assertTrue(data.contacts.isEmpty())
        assertFalse(data.skipped > 0)
    }
}
