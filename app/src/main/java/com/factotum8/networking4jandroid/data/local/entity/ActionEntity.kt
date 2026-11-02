package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import java.time.Instant
import java.time.LocalDate

/**
 * `(:Contact)-[:NEXT_ACTION]->(:Action)` — a scheduled next step for a
 * contact (write/call/meet/come back later).
 *
 * `completedAt` isn't in the original spec's property list — added purely
 * so "due" vs "done" reminders can be queried without a separate flag-only
 * timestamp lookup.
 *
 * Ported from `reference/models/enums.py::ActionType` usage +
 * networking4j's `app/models/action.py::Action`.
 */
@Entity(
    tableName = "actions",
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId")],
)
data class ActionEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val type: ActionType,
    val dueDate: LocalDate,
    val completed: Boolean = false,
    val completedAt: Instant? = null,
)
