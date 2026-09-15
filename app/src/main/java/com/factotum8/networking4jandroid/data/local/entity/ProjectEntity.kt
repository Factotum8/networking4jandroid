package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The `(:Project)` node, reached via `(:Contact)-[:INVOLVED_IN]->`.
 *
 * Ported from `reference/models/dimensions.py::Project`.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
)
