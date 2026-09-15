package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The `(:Event)` node, reached via `(:Contact)-[:ATTENDED]->`.
 *
 * Ported from `reference/models/dimensions.py::Event`.
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
)
