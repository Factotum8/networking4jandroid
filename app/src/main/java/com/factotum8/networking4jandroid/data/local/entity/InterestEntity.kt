package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The `(:Interest)` node, reached via `(:Contact)-[:INTERESTED_IN]->`.
 *
 * Ported from `reference/models/dimensions.py::Interest`.
 */
@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey val id: String,
    val name: String,
)
