package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * `(:Contact)-[:KNOWS {type, date, comment, closeness}]->(:Contact)`.
 *
 * `type` and `closeness` aren't given a fixed value set/scale anywhere in
 * the source spec (unlike the Contact criteria, which are explicitly
 * 1-10) — kept as free-form rather than inventing bounds.
 *
 * Ported from `reference/models/relationships.py::Knows`.
 */
@Entity(
    tableName = "knows",
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["relatedContactId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId"), Index("relatedContactId")],
)
data class KnowsEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val relatedContactId: String,
    val type: String? = null,
    val date: LocalDate? = null,
    val comment: String? = null,
    val closeness: Int? = null,
)
