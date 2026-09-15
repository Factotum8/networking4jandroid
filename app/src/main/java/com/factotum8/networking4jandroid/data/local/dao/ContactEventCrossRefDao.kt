package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.factotum8.networking4jandroid.data.local.entity.ContactEventCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactEventCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: ContactEventCrossRef)

    @Delete
    suspend fun delete(crossRef: ContactEventCrossRef)

    @Query("SELECT * FROM contact_event_cross_refs WHERE contactId = :contactId")
    fun getForContact(contactId: String): Flow<List<ContactEventCrossRef>>
}
