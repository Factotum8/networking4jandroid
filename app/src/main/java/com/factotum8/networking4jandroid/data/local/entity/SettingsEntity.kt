package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * `(:AppSettings)` — a single global settings row, no per-user/per-contact
 * scoping (matches the source: `LIMIT 1` on a single node).
 *
 * [id] is always [SINGLETON_ID] — there is never more than one row.
 *
 * Ported from networking4j's `app/models/settings.py::UserSettings`.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val staleContactDays: Int = 60,
    val dedupScoreThreshold: Double = 85.0,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
