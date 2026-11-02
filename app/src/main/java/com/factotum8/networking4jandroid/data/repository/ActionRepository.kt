package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.ActionDao
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Port of the `(:Contact)-[:NEXT_ACTION]->(:Action)` half of `LinkRepository`/`LinkHandler`. */
class ActionRepository @Inject constructor(
    private val actionDao: ActionDao,
) {
    fun listForContact(contactId: String): Flow<List<ActionEntity>> = actionDao.getForContact(contactId)

    suspend fun add(contactId: String, type: ActionType, dueDate: LocalDate) {
        actionDao.insert(ActionEntity(id = UUID.randomUUID().toString(), contactId = contactId, type = type, dueDate = dueDate))
    }

    suspend fun complete(actionId: String) {
        val action = actionDao.getById(actionId) ?: return
        actionDao.update(action.copy(completed = true, completedAt = Instant.now()))
    }

    /** Due-actions digest: `dueDate <= before AND NOT completed` (unless [includeCompleted]), ascending. */
    suspend fun listDue(before: LocalDate = LocalDate.now(), includeCompleted: Boolean = false): List<ActionEntity> =
        actionDao.getDue(before, includeCompleted)
}
