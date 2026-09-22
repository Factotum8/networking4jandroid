package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.factotum8.networking4jandroid.data.local.entity.ContactInterestCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactInterestCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: ContactInterestCrossRef)

    @Delete
    suspend fun delete(crossRef: ContactInterestCrossRef)

    @Query("SELECT * FROM contact_interest_cross_refs WHERE contactId = :contactId")
    fun getForContact(contactId: String): Flow<List<ContactInterestCrossRef>>

    @Query("SELECT * FROM contact_interest_cross_refs WHERE contactId = :contactId")
    suspend fun getForContactOnce(contactId: String): List<ContactInterestCrossRef>

    @Query("SELECT * FROM contact_interest_cross_refs")
    suspend fun getAllOnce(): List<ContactInterestCrossRef>
}
