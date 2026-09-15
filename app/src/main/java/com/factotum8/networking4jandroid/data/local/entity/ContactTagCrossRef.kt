package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Property-less `(:Contact)-[:TAGGED]->(:Tag)` edge.
 *
 * Ported from `reference/models/enums.py::DimensionLinkType.TAGGED`.
 */
@Entity(
    tableName = "contact_tag_cross_refs",
    primaryKeys = ["contactId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId"), Index("tagId")],
)
data class ContactTagCrossRef(
    val contactId: String,
    val tagId: String,
)
