package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Property-less `(:Contact)-[:INVOLVED_IN]->(:Project)` edge.
 *
 * Ported from `reference/models/enums.py::DimensionLinkType.INVOLVED_IN`.
 */
@Entity(
    tableName = "contact_project_cross_refs",
    primaryKeys = ["contactId", "projectId"],
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId"), Index("projectId")],
)
data class ContactProjectCrossRef(
    val contactId: String,
    val projectId: String,
)
