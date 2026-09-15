package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.factotum8.networking4jandroid.data.local.entity.ContactCompanyCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactCompanyCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: ContactCompanyCrossRef)

    @Delete
    suspend fun delete(crossRef: ContactCompanyCrossRef)

    @Query("SELECT * FROM contact_company_cross_refs WHERE contactId = :contactId")
    fun getForContact(contactId: String): Flow<List<ContactCompanyCrossRef>>
}
