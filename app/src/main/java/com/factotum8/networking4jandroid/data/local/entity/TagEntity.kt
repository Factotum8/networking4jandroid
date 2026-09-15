package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The `(:Tag)` node, reached via `(:Contact)-[:TAGGED]->`.
 *
 * Ported from `reference/models/dimensions.py::Tag`.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
)
