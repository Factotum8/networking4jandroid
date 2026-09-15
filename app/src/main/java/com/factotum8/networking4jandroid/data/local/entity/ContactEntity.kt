package com.factotum8.networking4jandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import java.time.Instant
import java.time.LocalDate

/**
 * The `(:Contact)` node — the central entity of the graph model.
 *
 * Ported from `reference/models/contact.py::Contact`. The 1-10 range on
 * [dangerous]/[interesting]/[difficult] is enforced by the Pydantic model,
 * not here — a future repository/ViewModel layer's job, not Room's.
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,

    // Single avatar photo (one photo per contact, not a gallery). Stored
    // as a path/URL string; the storage backend is a stage-2 detail.
    val photo: String? = null,

    val phone: String? = null,
    val email: String? = null,
    val position: String? = null,
    val city: String? = null,
    val birthday: LocalDate? = null,

    // Doubles as the "important facts about a contact" field — same thing
    // as notes, no separate structured entity.
    val notes: String? = null,

    val metPlace: String? = null,
    val metDate: LocalDate? = null,
    val contactType: ContactType? = null,
    val circle: Circle? = null,
    val dangerous: Int? = null,
    val interesting: Int? = null,
    val difficult: Int? = null,

    // Soft delete/archive: deletion only after confirmation; archiving is
    // a separate, reversible operation.
    val archived: Boolean = false,
    val archivedAt: Instant? = null,

    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
