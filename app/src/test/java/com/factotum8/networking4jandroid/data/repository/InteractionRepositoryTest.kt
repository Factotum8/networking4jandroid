package com.factotum8.networking4jandroid.data.repository

import app.cash.turbine.test
import com.factotum8.networking4jandroid.data.local.dao.InteractionDao
import com.factotum8.networking4jandroid.data.local.dao.LastInteractionRow
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class InteractionRepositoryTest {

    private lateinit var dao: InteractionDao
    private lateinit var repository: InteractionRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = InteractionRepository(dao)
    }

    @Test
    fun `listForContact delegates to dao`() = runTest {
        val interactions = listOf(
            InteractionEntity(id = "i1", contactId = "c1", type = InteractionType.CALL, date = LocalDate.now()),
        )
        every { dao.getForContact("c1") } returns flowOf(interactions)

        repository.listForContact("c1").test {
            assertEquals(interactions, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `listAllOnce delegates to dao`() = runTest {
        val interactions = listOf(
            InteractionEntity(id = "i1", contactId = "c1", type = InteractionType.MEETING, date = LocalDate.now()),
        )
        coEvery { dao.getAllOnce() } returns interactions

        assertEquals(interactions, repository.listAllOnce())
    }

    @Test
    fun `add inserts an interaction with a random id and the given fields`() = runTest {
        val slot = slot<InteractionEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit
        val date = LocalDate.of(2026, 2, 2)

        repository.add("c1", InteractionType.REFERRAL, date, "met at conf")

        coVerify(exactly = 1) { dao.insert(any()) }
        val inserted = slot.captured
        assertTrue(inserted.id.isNotBlank())
        assertEquals("c1", inserted.contactId)
        assertEquals(InteractionType.REFERRAL, inserted.type)
        assertEquals(date, inserted.date)
        assertEquals("met at conf", inserted.comment)
    }

    @Test
    fun `lastInteractionDatePerContact maps rows to contactId to lastDate`() = runTest {
        val date1 = LocalDate.of(2026, 1, 1)
        val date2 = LocalDate.of(2026, 2, 2)
        coEvery { dao.getLastInteractionDatePerContact() } returns listOf(
            LastInteractionRow("c1", date1),
            LastInteractionRow("c2", date2),
        )

        val result = repository.lastInteractionDatePerContact()

        assertEquals(mapOf("c1" to date1, "c2" to date2), result)
    }

    @Test
    fun `lastInteractionDatePerContact returns empty map when dao returns no rows`() = runTest {
        coEvery { dao.getLastInteractionDatePerContact() } returns emptyList()

        assertEquals(emptyMap<String, LocalDate>(), repository.lastInteractionDatePerContact())
    }
}
