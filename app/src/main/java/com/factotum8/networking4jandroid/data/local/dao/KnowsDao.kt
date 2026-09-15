package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(knows: KnowsEntity)

    @Update
    suspend fun update(knows: KnowsEntity)

    @Delete
    suspend fun delete(knows: KnowsEntity)

    @Query("SELECT * FROM knows WHERE id = :id")
    suspend fun getById(id: String): KnowsEntity?

    @Query("SELECT * FROM knows WHERE contactId = :contactId OR relatedContactId = :contactId")
    fun getForContact(contactId: String): Flow<List<KnowsEntity>>

    @Query("SELECT * FROM knows")
    fun getAll(): Flow<List<KnowsEntity>>
}
