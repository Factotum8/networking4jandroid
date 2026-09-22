package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

/**
 * `(:Contact)-[:KNOWS {type, date, comment, closeness}]->(:Contact)`.
 *
 * `type` and `closeness` aren't given a fixed value set/scale anywhere in
 * the source spec (unlike the Contact criteria, which are explicitly
 * 1-10) — kept as free-form rather than inventing bounds.
 *
 * Composite `(contactId, relatedContactId)` primary key, not a separate
 * `id` — the source's KNOWS edge is Neo4j `MERGE`d, i.e. exactly one
 * relationship per directed pair, upserted in place rather than
 * accumulating duplicate rows for the same pair.
 *
 * Ported from `reference/models/relationships.py::Knows`.
 */
@Entity(
    tableName = "knows",
    primaryKeys = ["contactId", "relatedContactId"],
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["relatedContactId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId"), Index("relatedContactId")],
)
data class KnowsEntity(
    val contactId: String,
    val relatedContactId: String,
    val type: String? = null,
    val date: LocalDate? = null,
    val comment: String? = null,
    val closeness: Int? = null,
)
