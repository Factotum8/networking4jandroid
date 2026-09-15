package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * The `(:Relative)` node, reached via `(:Contact)-[:HAS_RELATIVE]->`.
 *
 * A separate node linked to Contact — not properties on the contact
 * itself — so relatives' birthdays can be aggregated across contacts too.
 *
 * Ported from `reference/models/relative.py::Relative`.
 */
@Entity(
    tableName = "relatives",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contactId")],
)
data class RelativeEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val name: String,

    // Not enumerated in the spec (e.g. "mother", "spouse", "child") — kept
    // as free text rather than inventing a fixed set of relation kinds.
    val relationType: String? = null,

    val birthday: LocalDate? = null,
)
