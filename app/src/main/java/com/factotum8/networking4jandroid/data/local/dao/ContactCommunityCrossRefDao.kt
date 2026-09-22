package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.factotum8.networking4jandroid.data.local.entity.ContactCommunityCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactCommunityCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: ContactCommunityCrossRef)

    @Delete
    suspend fun delete(crossRef: ContactCommunityCrossRef)

    @Query("SELECT * FROM contact_community_cross_refs WHERE contactId = :contactId")
    fun getForContact(contactId: String): Flow<List<ContactCommunityCrossRef>>

    @Query("SELECT * FROM contact_community_cross_refs WHERE contactId = :contactId")
    suspend fun getForContactOnce(contactId: String): List<ContactCommunityCrossRef>

    @Query("SELECT * FROM contact_community_cross_refs")
    suspend fun getAllOnce(): List<ContactCommunityCrossRef>
}
