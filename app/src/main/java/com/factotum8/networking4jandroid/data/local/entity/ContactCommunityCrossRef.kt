package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Property-less `(:Contact)-[:MEMBER_OF]->(:Community)` edge.
 *
 * Ported from `reference/models/enums.py::DimensionLinkType.MEMBER_OF`.
 */
@Entity(
    tableName = "contact_community_cross_refs",
    primaryKeys = ["contactId", "communityId"],
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CommunityEntity::class, parentColumns = ["id"], childColumns = ["communityId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId"), Index("communityId")],
)
data class ContactCommunityCrossRef(
    val contactId: String,
    val communityId: String,
)
