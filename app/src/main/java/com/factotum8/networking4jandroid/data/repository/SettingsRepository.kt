package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.SettingsDao
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import javax.inject.Inject

/**
 * Port of `SettingsRepository`/`SettingsHandler`. Single global row, no
 * per-user scoping — returns [SettingsEntity]'s defaults if no row exists
 * yet, matching the source's "fresh default instance" fallback.
 */
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao,
) {
    suspend fun get(): SettingsEntity = settingsDao.get() ?: SettingsEntity()

    suspend fun update(staleContactDays: Int, dedupScoreThreshold: Double) {
        settingsDao.upsert(SettingsEntity(staleContactDays = staleContactDays, dedupScoreThreshold = dedupScoreThreshold))
    }
}
