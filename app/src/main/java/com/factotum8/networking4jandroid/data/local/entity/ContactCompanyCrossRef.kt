package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Property-less `(:Contact)-[:WORKS_AT]->(:Company)` edge.
 *
 * Ported from `reference/models/enums.py::DimensionLinkType.WORKS_AT`.
 */
@Entity(
    tableName = "contact_company_cross_refs",
    primaryKeys = ["contactId", "companyId"],
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CompanyEntity::class, parentColumns = ["id"], childColumns = ["companyId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("contactId"), Index("companyId")],
)
data class ContactCompanyCrossRef(
    val contactId: String,
    val companyId: String,
)
