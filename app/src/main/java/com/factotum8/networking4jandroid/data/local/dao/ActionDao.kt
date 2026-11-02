package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: ActionEntity)

    @Update
    suspend fun update(action: ActionEntity)

    @Delete
    suspend fun delete(action: ActionEntity)

    @Query("SELECT * FROM actions WHERE id = :id")
    suspend fun getById(id: String): ActionEntity?

    @Query("SELECT * FROM actions WHERE contactId = :contactId ORDER BY dueDate ASC")
    fun getForContact(contactId: String): Flow<List<ActionEntity>>

    /** Due-actions digest: `dueDate <= before AND (includeCompleted OR NOT completed)`, ascending. */
    @Query(
        "SELECT * FROM actions WHERE dueDate <= :before AND (:includeCompleted OR completed = 0) " +
            "ORDER BY dueDate ASC",
    )
    suspend fun getDue(before: LocalDate, includeCompleted: Boolean = false): List<ActionEntity>
}
