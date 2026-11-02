package com.factotum8.networking4jandroid.data.repository

import app.cash.turbine.test
import com.factotum8.networking4jandroid.data.local.dao.ContactDao
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactRepositoryTest {

    private lateinit var dao: ContactDao
    private lateinit var repository: ContactRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = ContactRepository(dao)
    }

    @Test
    fun `list delegates filters to dao`() = runTest {
        val contacts = listOf(ContactEntity(id = "c1", name = "Alice"))
        every { dao.getFiltered(true, Circle.SUPPORT_CIRCLE, ContactType.CONNECTOR) } returns flowOf(contacts)

        repository.list(archived = true, circle = Circle.SUPPORT_CIRCLE, contactType = ContactType.CONNECTOR).test {
            assertEquals(contacts, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `listOnce defaults to non-archived with no filters`() = runTest {
        coEvery { dao.getFilteredOnce(false, null, null) } returns emptyList()

        repository.listOnce()

        coVerify(exactly = 1) { dao.getFilteredOnce(false, null, null) }
    }

    @Test
    fun `getById delegates to dao`() = runTest {
        val contact = ContactEntity(id = "c1", name = "Alice")
        coEvery { dao.getById("c1") } returns contact

        assertEquals(contact, repository.getById("c1"))
    }

    @Test
    fun `search returns empty list without querying dao when query is blank`() = runTest {
        val result = repository.search("   ")

        assertEquals(emptyList<ContactEntity>(), result)
        coVerify(exactly = 0) { dao.search(any(), any()) }
    }

    @Test
    fun `search trims the query before delegating to dao`() = runTest {
        val matches = listOf(ContactEntity(id = "c1", name = "Bob"))
        coEvery { dao.search("bob", 20) } returns matches

        val result = repository.search("  bob  ")

        assertEquals(matches, result)
        coVerify(exactly = 1) { dao.search("bob", 20) }
    }

    @Test
    fun `create assigns a fresh id and timestamps then inserts`() = runTest {
        val draft = ContactEntity(id = "ignored", name = "New Contact")
        val slot = slot<ContactEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit

        val created = repository.create(draft)

        coVerify(exactly = 1) { dao.insert(any()) }
        assertNotEquals("ignored", created.id)
        assertTrue(created.id.isNotBlank())
        assertNotNull(created.createdAt)
        assertNotNull(created.updatedAt)
        assertEquals(created, slot.captured)
        assertEquals("New Contact", created.name)
    }

    @Test
    fun `update stamps updatedAt and forwards to dao`() = runTest {
        val contact = ContactEntity(id = "c1", name = "Alice", updatedAt = null)
        val slot = slot<ContactEntity>()
        coEvery { dao.update(capture(slot)) } returns Unit

        repository.update(contact)

        assertNotNull(slot.captured.updatedAt)
        assertEquals("c1", slot.captured.id)
    }

    @Test
    fun `archive sets archived flag and archivedAt when contact exists`() = runTest {
        val contact = ContactEntity(id = "c1", name = "Alice")
        coEvery { dao.getById("c1") } returns contact
        val slot = slot<ContactEntity>()
        coEvery { dao.update(capture(slot)) } returns Unit

        repository.archive("c1")

        assertTrue(slot.captured.archived)
        assertNotNull(slot.captured.archivedAt)
    }

    @Test
    fun `archive is a no-op when contact does not exist`() = runTest {
        coEvery { dao.getById("missing") } returns null

        repository.archive("missing")

        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `unarchive clears archived flag and archivedAt when contact exists`() = runTest {
        val contact = ContactEntity(id = "c1", name = "Alice", archived = true, archivedAt = java.time.Instant.now())
        coEvery { dao.getById("c1") } returns contact
        val slot = slot<ContactEntity>()
        coEvery { dao.update(capture(slot)) } returns Unit

        repository.unarchive("c1")

        assertEquals(false, slot.captured.archived)
        assertNull(slot.captured.archivedAt)
    }

    @Test
    fun `unarchive is a no-op when contact does not exist`() = runTest {
        coEvery { dao.getById("missing") } returns null

        repository.unarchive("missing")

        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `delete removes the contact when it exists`() = runTest {
        val contact = ContactEntity(id = "c1", name = "Alice")
        coEvery { dao.getById("c1") } returns contact
        coEvery { dao.delete(contact) } returns Unit

        repository.delete("c1")

        coVerify(exactly = 1) { dao.delete(contact) }
    }

    @Test
    fun `delete is a no-op when contact does not exist`() = runTest {
        coEvery { dao.getById("missing") } returns null

        repository.delete("missing")

        coVerify(exactly = 0) { dao.delete(any()) }
    }
}
