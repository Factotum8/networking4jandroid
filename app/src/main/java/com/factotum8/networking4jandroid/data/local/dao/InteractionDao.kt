package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interaction: InteractionEntity)

    @Update
    suspend fun update(interaction: InteractionEntity)

    @Delete
    suspend fun delete(interaction: InteractionEntity)

    @Query("SELECT * FROM interactions WHERE contactId = :contactId ORDER BY date DESC")
    fun getForContact(contactId: String): Flow<List<InteractionEntity>>

    @Query("SELECT * FROM interactions")
    suspend fun getAllOnce(): List<InteractionEntity>

    /** Latest interaction date per contact — for the "stale contact" reminder rule. */
    @Query("SELECT contactId, MAX(date) as lastDate FROM interactions GROUP BY contactId")
    suspend fun getLastInteractionDatePerContact(): List<LastInteractionRow>
}

data class LastInteractionRow(val contactId: String, val lastDate: java.time.LocalDate)
