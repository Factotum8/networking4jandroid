package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import java.time.LocalDate

/**
 * `(:Contact)-[:HAD_INTERACTION]->(:Interaction)` — a logged
 * meeting/call/correspondence/referral with a contact.
 *
 * Modeled as its own node (not a Contact list field) so "last interaction
 * date" can be a simple aggregation query, matching the source model.
 *
 * Ported from `reference/models/enums.py::InteractionType` usage +
 * networking4j's `app/models/interaction.py::Interaction`.
 */
@Entity(
    tableName = "interactions",
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId")],
)
data class InteractionEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val type: InteractionType,
    val date: LocalDate,
    val comment: String? = null,
)
