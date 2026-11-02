package com.factotum8.networking4jandroid.data.repository

import app.cash.turbine.test
import com.factotum8.networking4jandroid.data.local.dao.RelativeDao
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class RelativeRepositoryTest {

    private lateinit var dao: RelativeDao
    private lateinit var repository: RelativeRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = RelativeRepository(dao)
    }

    @Test
    fun `listForContact delegates to dao`() = runTest {
        val relatives = listOf(RelativeEntity(id = "r1", contactId = "c1", name = "Mom"))
        every { dao.getForContact("c1") } returns flowOf(relatives)

        repository.listForContact("c1").test {
            assertEquals(relatives, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `add inserts a relative with a random id and the given fields`() = runTest {
        val slot = slot<RelativeEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit

        repository.add("c1", "Mom", "mother", LocalDate.of(1970, 1, 1))

        coVerify(exactly = 1) { dao.insert(any()) }
        val inserted = slot.captured
        assertTrue(inserted.id.isNotBlank())
        assertEquals("c1", inserted.contactId)
        assertEquals("Mom", inserted.name)
        assertEquals("mother", inserted.relationType)
        assertEquals(LocalDate.of(1970, 1, 1), inserted.birthday)
    }

    @Test
    fun `add allows null relationType and birthday`() = runTest {
        val slot = slot<RelativeEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit

        repository.add("c1", "Someone", null, null)

        assertNull(slot.captured.relationType)
        assertNull(slot.captured.birthday)
    }

    @Test
    fun `delete forwards the entity to dao`() = runTest {
        val relative = RelativeEntity(id = "r1", contactId = "c1", name = "Mom")
        coEvery { dao.delete(relative) } returns Unit

        repository.delete(relative)

        coVerify(exactly = 1) { dao.delete(relative) }
    }
}
