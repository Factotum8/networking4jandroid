package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factotum8.networking4jandroid.data.local.entity.CompanyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(company: CompanyEntity)

    @Update
    suspend fun update(company: CompanyEntity)

    @Delete
    suspend fun delete(company: CompanyEntity)

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getById(id: String): CompanyEntity?

    @Query("SELECT * FROM companies")
    fun getAll(): Flow<List<CompanyEntity>>
}
