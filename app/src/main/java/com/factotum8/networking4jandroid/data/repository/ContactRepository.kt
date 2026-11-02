package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.ContactDao
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Port of `app.handlers.contact_handler.ContactHandler` +
 * `app.repositories.contact_repository.ContactRepository`.
 *
 * `dangerous`/`interesting`/`difficult`'s 1-10 bound is enforced by the
 * caller (the contact-detail form, mirroring the source: the source has no
 * DB-level or handler-level check either, only Pydantic's `Field(ge=1,
 * le=10)` at model-construction time).
 */
class ContactRepository @Inject constructor(
    private val contactDao: ContactDao,
) {
    fun list(
        archived: Boolean = false,
        circle: Circle? = null,
        contactType: ContactType? = null,
    ): Flow<List<ContactEntity>> = contactDao.getFiltered(archived, circle, contactType)

    suspend fun listOnce(
        archived: Boolean = false,
        circle: Circle? = null,
        contactType: ContactType? = null,
    ): List<ContactEntity> = contactDao.getFilteredOnce(archived, circle, contactType)

    suspend fun getById(id: String): ContactEntity? = contactDao.getById(id)

    suspend fun search(query: String, limit: Int = 20): List<ContactEntity> =
        if (query.isBlank()) emptyList() else contactDao.search(query.trim(), limit)

    suspend fun create(draft: ContactEntity): ContactEntity {
        val now = Instant.now()
        val contact = draft.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
        contactDao.insert(contact)
        return contact
    }

    suspend fun update(contact: ContactEntity) {
        contactDao.update(contact.copy(updatedAt = Instant.now()))
    }

    suspend fun archive(id: String) {
        val contact = contactDao.getById(id) ?: return
        contactDao.update(contact.copy(archived = true, archivedAt = Instant.now(), updatedAt = Instant.now()))
    }

    suspend fun unarchive(id: String) {
        val contact = contactDao.getById(id) ?: return
        contactDao.update(contact.copy(archived = false, archivedAt = null, updatedAt = Instant.now()))
    }

    /**
     * Cascades to `Relative`/`Knows`/dimension cross-refs/`Interaction`/
     * `Action` rows via `ON DELETE CASCADE` on `contactId` — matching the
     * source's `DETACH DELETE`. Never touches the shared dimension nodes
     * themselves (`Company`/`Tag`/etc.), only this contact's join rows.
     */
    suspend fun delete(id: String) {
        val contact = contactDao.getById(id) ?: return
        contactDao.delete(contact)
    }
}
