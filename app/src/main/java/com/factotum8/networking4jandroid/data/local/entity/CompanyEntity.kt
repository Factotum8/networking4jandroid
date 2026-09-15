package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The `(:Company)` node, reached via `(:Contact)-[:WORKS_AT]->`.
 *
 * Ported from `reference/models/dimensions.py::Company`.
 */
@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: String,
    val name: String,
)
