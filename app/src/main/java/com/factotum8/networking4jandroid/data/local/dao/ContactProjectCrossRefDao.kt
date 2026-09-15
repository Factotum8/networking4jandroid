package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.factotum8.networking4jandroid.data.local.entity.ContactProjectCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactProjectCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: ContactProjectCrossRef)

    @Delete
    suspend fun delete(crossRef: ContactProjectCrossRef)

    @Query("SELECT * FROM contact_project_cross_refs WHERE contactId = :contactId")
    fun getForContact(contactId: String): Flow<List<ContactProjectCrossRef>>
}
