package com.factotum8.networking4jandroid.data.repository

import app.cash.turbine.test
import com.factotum8.networking4jandroid.data.local.dao.ActionDao
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ActionRepositoryTest {

    private lateinit var dao: ActionDao
    private lateinit var repository: ActionRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = ActionRepository(dao)
    }

    @Test
    fun `listForContact delegates to dao`() = runTest {
        val actions = listOf(
            ActionEntity(id = "a1", contactId = "c1", type = ActionType.CALL, dueDate = LocalDate.of(2026, 1, 1)),
        )
        every { dao.getForContact("c1") } returns flowOf(actions)

        repository.listForContact("c1").test {
            assertEquals(actions, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `add inserts a new action with random id for the given contact`() = runTest {
        val slot = slot<ActionEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit

        repository.add("c1", ActionType.MEET, LocalDate.of(2026, 3, 1))

        coVerify(exactly = 1) { dao.insert(any()) }
        val inserted = slot.captured
        assertEquals("c1", inserted.contactId)
        assertEquals(ActionType.MEET, inserted.type)
        assertEquals(LocalDate.of(2026, 3, 1), inserted.dueDate)
        assertFalse(inserted.completed)
        assertTrue(inserted.id.isNotBlank())
    }

    @Test
    fun `complete marks the action completed and stamps completedAt when it exists`() = runTest {
        val existing = ActionEntity(id = "a1", contactId = "c1", type = ActionType.WRITE, dueDate = LocalDate.now())
        coEvery { dao.getById("a1") } returns existing
        val slot = slot<ActionEntity>()
        coEvery { dao.update(capture(slot)) } returns Unit

        repository.complete("a1")

        coVerify(exactly = 1) { dao.update(any()) }
        assertTrue(slot.captured.completed)
        assertEquals(existing.id, slot.captured.id)
        assertTrue(slot.captured.completedAt != null)
    }

    @Test
    fun `complete is a no-op when the action does not exist`() = runTest {
        coEvery { dao.getById("missing") } returns null

        repository.complete("missing")

        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `listDue delegates to dao with defaults`() = runTest {
        val due = listOf(ActionEntity(id = "a1", contactId = "c1", type = ActionType.CALL, dueDate = LocalDate.now()))
        coEvery { dao.getDue(any(), any()) } returns due

        val result = repository.listDue()

        assertEquals(due, result)
        coVerify(exactly = 1) { dao.getDue(any(), false) }
    }

    @Test
    fun `listDue passes through explicit before date and includeCompleted`() = runTest {
        val before = LocalDate.of(2026, 5, 1)
        coEvery { dao.getDue(before, true) } returns emptyList()

        repository.listDue(before = before, includeCompleted = true)

        coVerify(exactly = 1) { dao.getDue(before, true) }
    }
}
