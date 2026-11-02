package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchRepositoryTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: SearchRepository

    @Before
    fun setUp() {
        contactRepository = mockk()
        settingsRepository = mockk()
        repository = SearchRepository(contactRepository, settingsRepository)
    }

    private fun contact(id: String, name: String, phone: String? = null, email: String? = null) =
        ContactEntity(id = id, name = name, phone = phone, email = email, archived = false)

    // --- search() ---

    @Test
    fun `search delegates to contactRepository search`() = runTest {
        val results = listOf(contact("c1", "Alice"))
        coEvery { contactRepository.search("ali", 20) } returns results

        val result = repository.search("ali")

        assertEquals(results, result)
    }

    // --- findDuplicates(): threshold resolution ---

    @Test
    fun `explicit threshold overrides settings and skips the settings lookup`() = runTest {
        coEvery { contactRepository.listOnce(archived = false) } returns emptyList()

        repository.findDuplicates(threshold = 50.0)

        coVerify(exactly = 0) { settingsRepository.get() }
    }

    @Test
    fun `null threshold falls back to settings dedupScoreThreshold`() = runTest {
        coEvery { contactRepository.listOnce(archived = false) } returns emptyList()
        coEvery { settingsRepository.get() } returns SettingsEntity(dedupScoreThreshold = 85.0)

        repository.findDuplicates(threshold = null)

        coVerify(exactly = 1) { settingsRepository.get() }
    }

    // --- findDuplicates(): matching rules ---

    @Test
    fun `identical names are a full duplicate at score 100`() = runTest {
        val a = contact("c1", "Alice Smith")
        val b = contact("c2", "Alice Smith")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(a, b)

        val result = repository.findDuplicates(threshold = 85.0)

        assertEquals(1, result.size)
        assertEquals(100.0, result[0].score, 0.0)
        assertEquals(listOf("name"), result[0].matchedOn)
    }

    @Test
    fun `name similarity exactly at the 80 floor is matched as name (boundary)`() = runTest {
        // "alice" vs "alicx": length 5, edit distance 1 -> ratio = (1 - 1/5) * 100 = 80.0 exactly.
        val a = contact("c1", "alice")
        val b = contact("c2", "alicx")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(a, b)

        val result = repository.findDuplicates(threshold = 80.0)

        assertEquals(1, result.size)
        assertEquals(listOf("name"), result[0].matchedOn)
        assertEquals(80.0, result[0].score, 0.0001)
    }

    @Test
    fun `name similarity just below the 80 floor is not a candidate at all`() = runTest {
        // "alice" vs "bobby": same length, all 5 chars differ -> ratio 0, no phone/email -> no candidate.
        val a = contact("c1", "alice")
        val b = contact("c2", "bobby")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(a, b)

        val result = repository.findDuplicates(threshold = 0.0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `matching phone number scores 100 even with dissimilar names`() = runTest {
        val a = contact("c1", "Alice", phone = "+1234567890")
        val b = contact("c2", "Zed", phone = "+1234567890")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(a, b)

        val result = repository.findDuplicates(threshold = 85.0)

        assertEquals(1, result.size)
        assertEquals(100.0, result[0].score, 0.0)
        assertTrue("phone" in result[0].matchedOn)
        assertTrue("name" !in result[0].matchedOn)
    }

    @Test
    fun `matching email is case-insensitive and scores 100`() = runTest {
        val a = contact("c1", "Alice", email = "Alice@Example.com")
        val b = contact("c2", "Zed", email = "alice@example.com")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(a, b)

        val result = repository.findDuplicates(threshold = 85.0)

        assertEquals(1, result.size)
        assertTrue("email" in result[0].matchedOn)
        assertEquals(100.0, result[0].score, 0.0)
    }

    @Test
    fun `null phones are never considered a match`() = runTest {
        val a = contact("c1", "Alice", phone = null)
        val b = contact("c2", "Bob", phone = null)
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(a, b)

        val result = repository.findDuplicates(threshold = 0.0)

        assertTrue(result.none { "phone" in it.matchedOn })
    }

    @Test
    fun `candidates below the effective threshold are filtered out`() = runTest {
        val a = contact("c1", "alice")
        val b = contact("c2", "alicx") // name score 80.0
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(a, b)

        val result = repository.findDuplicates(threshold = 90.0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `results are sorted descending by score`() = runTest {
        val a = contact("c1", "Alice Smith")
        val b = contact("c2", "Alice Smith") // exact match -> 100
        val c = contact("c3", "alicx") // vs a: 80.0 by name
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(a, b, c)

        val result = repository.findDuplicates(threshold = 0.0)

        assertTrue(result.zipWithNext().all { (x, y) -> x.score >= y.score })
        assertEquals(100.0, result.first().score, 0.0)
    }

    @Test
    fun `no candidates for a single contact`() = runTest {
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(contact("c1", "Solo"))

        val result = repository.findDuplicates(threshold = 0.0)

        assertTrue(result.isEmpty())
        assertNull(result.firstOrNull())
    }
}
