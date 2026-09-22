package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowsDao {
    /** Upsert — one row per (contactId, relatedContactId) pair, matching the source's `MERGE` semantics. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(knows: KnowsEntity)

    @Query("DELETE FROM knows WHERE contactId = :contactId AND relatedContactId = :relatedContactId")
    suspend fun delete(contactId: String, relatedContactId: String): Int

    @Query("SELECT * FROM knows WHERE contactId = :contactId AND relatedContactId = :relatedContactId")
    suspend fun getForPair(contactId: String, relatedContactId: String): KnowsEntity?

    @Query("SELECT * FROM knows WHERE contactId = :contactId OR relatedContactId = :contactId")
    fun getForContact(contactId: String): Flow<List<KnowsEntity>>

    @Query("SELECT * FROM knows WHERE contactId = :contactId OR relatedContactId = :contactId")
    suspend fun getForContactOnce(contactId: String): List<KnowsEntity>

    @Query("SELECT * FROM knows")
    suspend fun getAll(): List<KnowsEntity>
}
