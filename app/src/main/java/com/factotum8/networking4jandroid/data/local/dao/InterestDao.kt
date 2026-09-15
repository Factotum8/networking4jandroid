package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factotum8.networking4jandroid.data.local.entity.InterestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interest: InterestEntity)

    @Update
    suspend fun update(interest: InterestEntity)

    @Delete
    suspend fun delete(interest: InterestEntity)

    @Query("SELECT * FROM interests WHERE id = :id")
    suspend fun getById(id: String): InterestEntity?

    @Query("SELECT * FROM interests")
    fun getAll(): Flow<List<InterestEntity>>
}
