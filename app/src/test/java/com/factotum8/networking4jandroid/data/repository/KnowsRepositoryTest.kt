package com.factotum8.networking4jandroid.data.repository

import app.cash.turbine.test
import com.factotum8.networking4jandroid.data.local.dao.KnowsDao
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class KnowsRepositoryTest {

    private lateinit var dao: KnowsDao
    private lateinit var repository: KnowsRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = KnowsRepository(dao)
    }

    @Test
    fun `listForContact delegates to dao`() = runTest {
        val rows = listOf(KnowsEntity("c1", "c2"))
        every { dao.getForContact("c1") } returns flowOf(rows)

        repository.listForContact("c1").test {
            assertEquals(rows, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `listForContactOnce delegates to dao`() = runTest {
        val rows = listOf(KnowsEntity("c1", "c2"))
        coEvery { dao.getForContactOnce("c1") } returns rows

        assertEquals(rows, repository.listForContactOnce("c1"))
    }

    @Test
    fun `getAll delegates to dao`() = runTest {
        val rows = listOf(KnowsEntity("c1", "c2"), KnowsEntity("c2", "c3"))
        coEvery { dao.getAll() } returns rows

        assertEquals(rows, repository.getAll())
    }

    @Test
    fun `set builds a KnowsEntity from the given fields and upserts it`() = runTest {
        coEvery { dao.set(any()) } returns Unit
        val date = LocalDate.of(2026, 4, 1)

        repository.set("c1", "c2", "friend", date, "met at gym", 7)

        coVerify(exactly = 1) {
            dao.set(KnowsEntity("c1", "c2", "friend", date, "met at gym", 7))
        }
    }

    @Test
    fun `remove returns true when a row was deleted`() = runTest {
        coEvery { dao.delete("c1", "c2") } returns 1

        assertTrue(repository.remove("c1", "c2"))
    }

    @Test
    fun `remove returns false when no row matched`() = runTest {
        coEvery { dao.delete("c1", "c2") } returns 0

        assertFalse(repository.remove("c1", "c2"))
    }
}
