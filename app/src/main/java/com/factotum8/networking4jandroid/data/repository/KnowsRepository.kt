package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.KnowsDao
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Port of the KNOWS-edge half of `LinkRepository`/`LinkHandler`. */
class KnowsRepository @Inject constructor(
    private val knowsDao: KnowsDao,
) {
    fun listForContact(contactId: String): Flow<List<KnowsEntity>> = knowsDao.getForContact(contactId)

    suspend fun listForContactOnce(contactId: String): List<KnowsEntity> = knowsDao.getForContactOnce(contactId)

    suspend fun getAll(): List<KnowsEntity> = knowsDao.getAll()

    /** Upsert — one row per directed pair, matching the source's `MERGE`. */
    suspend fun set(contactId: String, relatedContactId: String, type: String?, date: java.time.LocalDate?, comment: String?, closeness: Int?) {
        knowsDao.set(KnowsEntity(contactId, relatedContactId, type, date, comment, closeness))
    }

    suspend fun remove(contactId: String, relatedContactId: String): Boolean =
        knowsDao.delete(contactId, relatedContactId) > 0
}
