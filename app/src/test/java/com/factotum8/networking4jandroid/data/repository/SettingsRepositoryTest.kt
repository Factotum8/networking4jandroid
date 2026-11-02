package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.SettingsDao
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var dao: SettingsDao
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = SettingsRepository(dao)
    }

    @Test
    fun `get returns the dao row when one exists`() = runTest {
        val settings = SettingsEntity(staleContactDays = 30, dedupScoreThreshold = 90.0)
        coEvery { dao.get() } returns settings

        assertEquals(settings, repository.get())
    }

    @Test
    fun `get falls back to defaults when no row exists yet`() = runTest {
        coEvery { dao.get() } returns null

        assertEquals(SettingsEntity(), repository.get())
    }

    @Test
    fun `update upserts a singleton row with the given values`() = runTest {
        coEvery { dao.upsert(any()) } returns Unit

        repository.update(staleContactDays = 45, dedupScoreThreshold = 80.0)

        coVerify(exactly = 1) {
            dao.upsert(SettingsEntity(staleContactDays = 45, dedupScoreThreshold = 80.0))
        }
    }
}
