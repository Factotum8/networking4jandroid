package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactVCardTest {

    @Test
    fun `parseVCard returns empty result for text with no cards`() {
        val (rows, errors) = parseVCard("just some text, no vcard markers")
        assertTrue(rows.isEmpty())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `parseVCard maps every standard field`() {
        val text = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Ann Example
            TEL:+1 555
            EMAIL:ann@example.com
            TITLE:Engineer
            NOTE:Some notes
            BDAY:1990-05-04
            ORG:Acme;Other
            CATEGORIES:vip,climber
            END:VCARD
        """.trimIndent()

        val (rows, errors) = parseVCard(text)

        assertTrue(errors.isEmpty())
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("Ann Example", row.contact.name)
        assertEquals("+1 555", row.contact.phone)
        assertEquals("ann@example.com", row.contact.email)
        assertEquals("Engineer", row.contact.position)
        assertEquals("Some notes", row.contact.notes)
        assertEquals(LocalDate.of(1990, 5, 4), row.contact.birthday)
        assertEquals(listOf("Acme", "Other"), row.companies)
        assertEquals(listOf("vip", "climber"), row.tags)
        assertEquals("", row.contact.id)
        assertNull(row.contact.photo)
    }

    @Test
    fun `parseVCard maps our own X- extension properties`() {
        val text = """
            BEGIN:VCARD
            FN:Ann
            X-CIRCLE:support_circle
            X-CONTACT-TYPE:bridge
            X-DANGEROUS:4
            X-INTERESTING:7
            X-DIFFICULT:2
            END:VCARD
        """.trimIndent()

        val (rows, errors) = parseVCard(text)

        assertTrue(errors.isEmpty())
        val contact = rows.single().contact
        assertEquals(Circle.SUPPORT_CIRCLE, contact.circle)
        assertEquals(ContactType.BRIDGE, contact.contactType)
        assertEquals(4, contact.dangerous)
        assertEquals(7, contact.interesting)
        assertEquals(2, contact.difficult)
    }

    @Test
    fun `parseVCard requires FN and reports the card position without aborting the batch`() {
        val text = """
            BEGIN:VCARD
            TEL:+1 555
            END:VCARD
            BEGIN:VCARD
            FN:Bob
            END:VCARD
        """.trimIndent()

        val (rows, errors) = parseVCard(text)

        assertEquals(1, rows.size)
        assertEquals("Bob", rows.single().contact.name)
        assertEquals(1, errors.size)
        assertEquals(1, errors.single().row)
        assertTrue(errors.single().message.contains("FN"))
    }

    @Test
    fun `parseVCard blank FN is treated as missing`() {
        val text = "BEGIN:VCARD\nFN:   \nEND:VCARD"

        val (rows, errors) = parseVCard(text)

        assertTrue(rows.isEmpty())
        assertEquals(1, errors.size)
    }

    @Test
    fun `parseVCard uses only the first occurrence of a repeated property`() {
        val text = """
            BEGIN:VCARD
            FN:Ann
            TEL:+1 111
            TEL:+1 222
            END:VCARD
        """.trimIndent()

        val (rows, _) = parseVCard(text)

        assertEquals("+1 111", rows.single().contact.phone)
    }

    @Test
    fun `parseVCard ignores an unrecognized property such as PHOTO`() {
        val text = """
            BEGIN:VCARD
            FN:Ann
            PHOTO;ENCODING=b:aGVsbG8=
            END:VCARD
        """.trimIndent()

        val (rows, errors) = parseVCard(text)

        assertTrue(errors.isEmpty())
        assertNull(rows.single().contact.photo)
    }

    @Test
    fun `parseVCard unfolds RFC2426 continuation lines`() {
        val text = "BEGIN:VCARD\nFN:Ann\nNOTE:line one\n continued\nEND:VCARD"

        val (rows, errors) = parseVCard(text)

        assertTrue(errors.isEmpty())
        assertEquals("line onecontinued", rows.single().contact.notes)
    }

    @Test
    fun `parseVCard falls back to basic ISO date for an 8-digit BDAY`() {
        val text = "BEGIN:VCARD\nFN:Ann\nBDAY:19900504\nEND:VCARD"

        val (rows, errors) = parseVCard(text)

        assertTrue(errors.isEmpty())
        assertEquals(LocalDate.of(1990, 5, 4), rows.single().contact.birthday)
    }

    @Test
    fun `parseVCard leaves birthday null for an unparseable BDAY instead of erroring the card`() {
        val text = "BEGIN:VCARD\nFN:Ann\nBDAY:not-a-date\nEND:VCARD"

        val (rows, errors) = parseVCard(text)

        assertTrue(errors.isEmpty())
        assertNull(rows.single().contact.birthday)
    }

    @Test
    fun `parseVCard recognizes BEGIN and END markers case-insensitively`() {
        val text = "begin:vcard\nFN:Ann\nend:vcard"

        val (rows, errors) = parseVCard(text)

        assertTrue(errors.isEmpty())
        assertEquals(1, rows.size)
    }

    @Test
    fun `contactsToVCard emits mandatory FN, N, BEGIN and END for every contact`() {
        val output = contactsToVCard(listOf(ContactEntity(id = "1", name = "Ann")))

        val lines = output.split("\r\n").filter { it.isNotEmpty() }
        assertEquals("BEGIN:VCARD", lines[0])
        assertEquals("VERSION:3.0", lines[1])
        assertEquals("FN:Ann", lines[2])
        assertEquals("N:Ann", lines[3])
        assertEquals("END:VCARD", lines.last())
    }

    @Test
    fun `contactsToVCard omits optional properties that are null`() {
        val output = contactsToVCard(listOf(ContactEntity(id = "1", name = "Ann")))

        assertTrue(!output.contains("TEL:"))
        assertTrue(!output.contains("EMAIL:"))
        assertTrue(!output.contains("BDAY:"))
        assertTrue(!output.contains("X-CIRCLE:"))
    }

    @Test
    fun `contactsToVCard and parseVCard round-trip the standard and X- fields`() {
        val contact = ContactEntity(
            id = "some-id-should-not-appear",
            name = "Ann Example",
            phone = "+1 555",
            email = "ann@example.com",
            position = "Engineer",
            notes = "Some notes",
            birthday = LocalDate.of(1990, 5, 4),
            circle = Circle.FUNCTIONAL_CIRCLES,
            contactType = ContactType.INSIDER,
            dangerous = 5,
            interesting = 6,
            difficult = 2,
        )

        val text = contactsToVCard(listOf(contact))
        assertTrue(!text.contains("some-id-should-not-appear"))

        val (rows, errors) = parseVCard(text)
        assertTrue(errors.isEmpty())
        val parsed = rows.single().contact
        assertEquals("", parsed.id)
        assertEquals(contact.name, parsed.name)
        assertEquals(contact.phone, parsed.phone)
        assertEquals(contact.email, parsed.email)
        assertEquals(contact.position, parsed.position)
        assertEquals(contact.notes, parsed.notes)
        assertEquals(contact.birthday, parsed.birthday)
        assertEquals(contact.circle, parsed.circle)
        assertEquals(contact.contactType, parsed.contactType)
        assertEquals(contact.dangerous, parsed.dangerous)
        assertEquals(contact.interesting, parsed.interesting)
        assertEquals(contact.difficult, parsed.difficult)
    }

    @Test
    fun `contactsToVCard escapes and parseVCard unescapes special characters in NOTE`() {
        val contact = ContactEntity(id = "1", name = "Ann", notes = "A; B, C\\D\nE")

        val text = contactsToVCard(listOf(contact))
        val (rows, errors) = parseVCard(text)

        assertTrue(errors.isEmpty())
        assertEquals("A; B, C\\D\nE", rows.single().contact.notes)
    }

    @Test
    fun `contactsToVCard writes one card per contact`() {
        val contacts = listOf(ContactEntity(id = "1", name = "Ann"), ContactEntity(id = "2", name = "Bob"))

        val text = contactsToVCard(contacts)

        assertEquals(2, "BEGIN:VCARD".toRegex().findAll(text).count())
        val (rows, errors) = parseVCard(text)
        assertTrue(errors.isEmpty())
        assertEquals(listOf("Ann", "Bob"), rows.map { it.contact.name })
    }
}
