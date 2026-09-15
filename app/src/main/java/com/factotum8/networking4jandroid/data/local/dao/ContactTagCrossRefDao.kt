package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.factotum8.networking4jandroid.data.local.entity.ContactTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactTagCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: ContactTagCrossRef)

    @Delete
    suspend fun delete(crossRef: ContactTagCrossRef)

    @Query("SELECT * FROM contact_tag_cross_refs WHERE contactId = :contactId")
    fun getForContact(contactId: String): Flow<List<ContactTagCrossRef>>
}
