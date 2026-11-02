package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.RelativeDao
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/** Port of the `(:Contact)-[:HAS_RELATIVE]->(:Relative)` side of `LinkRepository`/`LinkHandler`. */
class RelativeRepository @Inject constructor(
    private val relativeDao: RelativeDao,
) {
    fun listForContact(contactId: String): Flow<List<RelativeEntity>> = relativeDao.getForContact(contactId)

    suspend fun add(contactId: String, name: String, relationType: String?, birthday: java.time.LocalDate?) {
        relativeDao.insert(
            RelativeEntity(
                id = UUID.randomUUID().toString(),
                contactId = contactId,
                name = name,
                relationType = relationType,
                birthday = birthday,
            ),
        )
    }

    suspend fun delete(relative: RelativeEntity) = relativeDao.delete(relative)
}
