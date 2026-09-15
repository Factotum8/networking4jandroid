package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelativeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relative: RelativeEntity)

    @Update
    suspend fun update(relative: RelativeEntity)

    @Delete
    suspend fun delete(relative: RelativeEntity)

    @Query("SELECT * FROM relatives WHERE id = :id")
    suspend fun getById(id: String): RelativeEntity?

    @Query("SELECT * FROM relatives WHERE contactId = :contactId")
    fun getForContact(contactId: String): Flow<List<RelativeEntity>>

    @Query("SELECT * FROM relatives")
    fun getAll(): Flow<List<RelativeEntity>>
}
