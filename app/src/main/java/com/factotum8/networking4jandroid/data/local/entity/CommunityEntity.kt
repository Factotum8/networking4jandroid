package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The `(:Community)` node, reached via `(:Contact)-[:MEMBER_OF]->`.
 *
 * Ported from `reference/models/dimensions.py::Community`.
 */
@Entity(tableName = "communities")
data class CommunityEntity(
    @PrimaryKey val id: String,
    val name: String,
)
