package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.InteractionDao
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Port of the `(:Contact)-[:HAD_INTERACTION]->(:Interaction)` half of `LinkRepository`/`LinkHandler`. */
class InteractionRepository @Inject constructor(
    private val interactionDao: InteractionDao,
) {
    fun listForContact(contactId: String): Flow<List<InteractionEntity>> = interactionDao.getForContact(contactId)

    suspend fun listAllOnce(): List<InteractionEntity> = interactionDao.getAllOnce()

    suspend fun add(contactId: String, type: InteractionType, date: LocalDate, comment: String?) {
        interactionDao.insert(
            InteractionEntity(id = UUID.randomUUID().toString(), contactId = contactId, type = type, date = date, comment = comment),
        )
    }

    suspend fun lastInteractionDatePerContact(): Map<String, LocalDate> =
        interactionDao.getLastInteractionDatePerContact().associate { it.contactId to it.lastDate }
}
