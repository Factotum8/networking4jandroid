package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Property-less `(:Contact)-[:INTERESTED_IN]->(:Interest)` edge.
 *
 * Ported from `reference/models/enums.py::DimensionLinkType.INTERESTED_IN`.
 */
@Entity(
    tableName = "contact_interest_cross_refs",
    primaryKeys = ["contactId", "interestId"],
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = InterestEntity::class, parentColumns = ["id"], childColumns = ["interestId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId"), Index("interestId")],
)
data class ContactInterestCrossRef(
    val contactId: String,
    val interestId: String,
)
