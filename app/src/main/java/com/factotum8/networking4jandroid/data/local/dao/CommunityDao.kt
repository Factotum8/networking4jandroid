package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factotum8.networking4jandroid.data.local.entity.CommunityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(community: CommunityEntity)

    @Update
    suspend fun update(community: CommunityEntity)

    @Delete
    suspend fun delete(community: CommunityEntity)

    @Query("SELECT * FROM communities WHERE id = :id")
    suspend fun getById(id: String): CommunityEntity?

    @Query("SELECT * FROM communities")
    fun getAll(): Flow<List<CommunityEntity>>
}
