package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Property-less `(:Contact)-[:ATTENDED]->(:Event)` edge.
 *
 * Ported from `reference/models/enums.py::DimensionLinkType.ATTENDED`.
 */
@Entity(
    tableName = "contact_event_cross_refs",
    primaryKeys = ["contactId", "eventId"],
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId"), Index("eventId")],
)
data class ContactEventCrossRef(
    val contactId: String,
    val eventId: String,
)
