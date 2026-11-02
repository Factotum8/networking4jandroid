package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rows are built from column->value maps (via [row]) rather than raw comma-joined
 * strings, so a test can never silently misalign with [fullHeader]'s column count.
 */
class ContactCsvTest {

    private val fullHeader =
        "name,photo,phone,email,position,city,birthday,notes,met_place,met_date,contact_type,circle,dangerous,interesting,difficult,company,tags,interests"
    private val headerColumns = fullHeader.split(",")

    private fun row(values: Map<String, String>): String =
        headerColumns.joinToString(",") { col -> csvField(values[col] ?: "") }

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun csv(vararg rows: Map<String, String>): String =
        (listOf(fullHeader) + rows.map { row(it) }).joinToString("\r\n") + "\r\n"

    @Test
    fun `parseCsv returns empty result for empty text`() {
        val (rows, errors) = parseCsv("")
        assertTrue(rows.isEmpty())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `parseCsv returns empty result for header-only text`() {
        val (rows, errors) = parseCsv(fullHeader)
        assertTrue(rows.isEmpty())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `parseCsv parses a fully populated row including dimension columns`() {
        val text = csv(
            mapOf(
                "name" to "Ann",
                "phone" to "+1 555",
                "email" to "ann@example.com",
                "position" to "Engineer",
                "city" to "Berlin",
                "birthday" to "1990-05-04",
                "notes" to "Some notes",
                "met_place" to "Conf",
                "met_date" to "2020-01-02",
                "contact_type" to "connector",
                "circle" to "support_circle",
                "dangerous" to "7",
                "interesting" to "8",
                "difficult" to "3",
                "company" to "Acme",
                "tags" to "vip",
                "interests" to "climbing,chess",
            ),
        )

        val (rows, errors) = parseCsv(text)

        assertTrue(errors.isEmpty())
        assertEquals(1, rows.size)
        val parsed = rows.single()
        assertEquals("Ann", parsed.contact.name)
        assertEquals("+1 555", parsed.contact.phone)
        assertEquals("ann@example.com", parsed.contact.email)
        assertEquals("Engineer", parsed.contact.position)
        assertEquals("Berlin", parsed.contact.city)
        assertEquals(LocalDate.of(1990, 5, 4), parsed.contact.birthday)
        assertEquals("Some notes", parsed.contact.notes)
        assertEquals("Conf", parsed.contact.metPlace)
        assertEquals(LocalDate.of(2020, 1, 2), parsed.contact.metDate)
        assertEquals(ContactType.CONNECTOR, parsed.contact.contactType)
        assertEquals(Circle.SUPPORT_CIRCLE, parsed.contact.circle)
        assertEquals(7, parsed.contact.dangerous)
        assertEquals(8, parsed.contact.interesting)
        assertEquals(3, parsed.contact.difficult)
        assertEquals("", parsed.contact.id)
        assertEquals(listOf("Acme"), parsed.companies)
        assertEquals(listOf("vip"), parsed.tags)
        assertEquals(listOf("climbing", "chess"), parsed.interests)
    }

    @Test
    fun `parseCsv leaves unpopulated optional fields null`() {
        val text = csv(mapOf("name" to "Bob"))

        val (rows, errors) = parseCsv(text)

        assertTrue(errors.isEmpty())
        val contact = rows.single().contact
        assertEquals("Bob", contact.name)
        assertNull(contact.phone)
        assertNull(contact.birthday)
        assertNull(contact.contactType)
        assertNull(contact.circle)
        assertNull(contact.dangerous)
        assertTrue(rows.single().companies.isEmpty())
    }

    @Test
    fun `parseCsv reports a missing name as a row error without aborting parsing`() {
        val text = csv(mapOf("phone" to "+1 555"))

        val (rows, errors) = parseCsv(text)

        assertTrue(rows.isEmpty())
        assertEquals(1, errors.size)
        assertEquals(2, errors.single().row)
        assertTrue(errors.single().message.contains("name is required"))
    }

    @Test
    fun `parseCsv reports invalid date without aborting the batch`() {
        val text = csv(
            mapOf("name" to "Ann", "birthday" to "not-a-date"),
            mapOf("name" to "Bob"),
        )

        val (rows, errors) = parseCsv(text)

        assertEquals(1, rows.size)
        assertEquals("Bob", rows.single().contact.name)
        assertEquals(1, errors.size)
        assertEquals(2, errors.single().row)
        assertTrue(errors.single().message.contains("birthday"))
    }

    @Test
    fun `parseCsv reports a second bad row at position 3`() {
        val text = csv(
            mapOf("name" to "Ann"),
            mapOf("name" to "Bob", "dangerous" to "abc"),
        )

        val (rows, errors) = parseCsv(text)

        assertEquals(1, rows.size)
        assertEquals(1, errors.size)
        assertEquals(3, errors.single().row)
    }

    @Test
    fun `parseCsv reports non-numeric criteria field`() {
        val text = csv(mapOf("name" to "Ann", "dangerous" to "abc"))

        val (rows, errors) = parseCsv(text)

        assertTrue(rows.isEmpty())
        assertEquals(1, errors.size)
        assertTrue(errors.single().message.contains("dangerous"))
        assertTrue(errors.single().message.contains("not a number"))
    }

    @Test
    fun `parseCsv accepts criteria boundary values 1 and 10`() {
        val text = csv(mapOf("name" to "Ann", "dangerous" to "1", "interesting" to "10", "difficult" to "1"))

        val (rows, errors) = parseCsv(text)

        assertTrue(errors.isEmpty())
        assertEquals(1, rows.single().contact.dangerous)
        assertEquals(10, rows.single().contact.interesting)
        assertEquals(1, rows.single().contact.difficult)
    }

    @Test
    fun `parseCsv rejects criteria values outside 1 to 10`() {
        val tooLow = parseCsv(csv(mapOf("name" to "Ann", "dangerous" to "0")))
        val tooHigh = parseCsv(csv(mapOf("name" to "Ann", "dangerous" to "11")))

        assertEquals(1, tooLow.second.size)
        assertTrue(tooLow.second.single().message.contains("must be 1-10"))
        assertEquals(1, tooHigh.second.size)
        assertTrue(tooHigh.second.single().message.contains("must be 1-10"))
    }

    @Test
    fun `parseCsv rejects unknown contact_type and circle values`() {
        val badType = parseCsv(csv(mapOf("name" to "Ann", "contact_type" to "unknown_type")))
        val badCircle = parseCsv(csv(mapOf("name" to "Ann", "circle" to "unknown_circle")))

        assertEquals(1, badType.second.size)
        assertTrue(badType.second.single().message.contains("contact_type"))
        assertEquals(1, badCircle.second.size)
        assertTrue(badCircle.second.single().message.contains("circle"))
    }

    @Test
    fun `parseCsv assigns 1-indexed human-facing row numbers, header is row 1`() {
        val text = csv(
            mapOf("name" to "Ann"),   // row 2, valid
            mapOf("phone" to "x"),    // row 3, missing name
            mapOf("name" to "Bob"),   // row 4, valid
            mapOf("phone" to "y"),    // row 5, missing name
        )

        val (rows, errors) = parseCsv(text)

        assertEquals(2, rows.size)
        assertEquals(listOf(3, 5), errors.map { it.row })
    }

    @Test
    fun `parseCsv handles quoted fields containing commas, quotes and newlines`() {
        val text = csv(mapOf("name" to "Ann", "notes" to "line one, with a comma\nand a \"quoted\" word"))

        val (rows, errors) = parseCsv(text)

        assertTrue(errors.isEmpty())
        assertEquals("line one, with a comma\nand a \"quoted\" word", rows.single().contact.notes)
    }

    @Test
    fun `contactsToCsv writes only the core columns as a CRLF-terminated header`() {
        val output = contactsToCsv(emptyList())

        assertEquals(
            "name,photo,phone,email,position,city,birthday,notes,met_place,met_date,contact_type,circle,dangerous,interesting,difficult\r\n",
            output,
        )
    }

    @Test
    fun `contactsToCsv excludes id and round-trips through parseCsv`() {
        val contact = ContactEntity(
            id = "some-id-should-not-appear",
            name = "Ann",
            phone = "+1 555",
            email = "ann@example.com",
            birthday = LocalDate.of(1990, 5, 4),
            contactType = ContactType.BRIDGE,
            circle = Circle.SUCCESS_CIRCLE,
            dangerous = 4,
        )

        val csvText = contactsToCsv(listOf(contact))
        assertTrue(!csvText.contains("some-id-should-not-appear"))

        val (rows, errors) = parseCsv(csvText)
        assertTrue(errors.isEmpty())
        val parsed = rows.single().contact
        assertEquals("", parsed.id)
        assertEquals(contact.name, parsed.name)
        assertEquals(contact.phone, parsed.phone)
        assertEquals(contact.email, parsed.email)
        assertEquals(contact.birthday, parsed.birthday)
        assertEquals(contact.contactType, parsed.contactType)
        assertEquals(contact.circle, parsed.circle)
        assertEquals(contact.dangerous, parsed.dangerous)
    }

    @Test
    fun `contactsToCsv quotes values containing commas, quotes or newlines and round-trips them`() {
        val contact = ContactEntity(id = "id", name = "Ann", notes = "hello, \"world\"\nnext line")

        val csvText = contactsToCsv(listOf(contact))
        val (rows, errors) = parseCsv(csvText)

        assertTrue(errors.isEmpty())
        assertEquals("hello, \"world\"\nnext line", rows.single().contact.notes)
    }

    @Test
    fun `contactsToCsv writes one CRLF-terminated line per contact`() {
        val contacts = listOf(ContactEntity(id = "1", name = "Ann"), ContactEntity(id = "2", name = "Bob"))

        val output = contactsToCsv(contacts)

        assertEquals(3, output.split("\r\n").filter { it.isNotEmpty() }.size)
    }
}
