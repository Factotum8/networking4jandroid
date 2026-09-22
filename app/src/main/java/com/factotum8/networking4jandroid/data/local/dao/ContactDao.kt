package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts")
    fun getAll(): Flow<List<ContactEntity>>

    /**
     * `list_contacts` port: [circle]/[contactType] AND-combine when both
     * given (null = "don't filter on this field"); [archived] is a strict
     * boolean, never tri-state.
     */
    @Query(
        "SELECT * FROM contacts WHERE archived = :archived " +
            "AND (:circle IS NULL OR circle = :circle) " +
            "AND (:contactType IS NULL OR contactType = :contactType) " +
            "ORDER BY name ASC",
    )
    fun getFiltered(archived: Boolean, circle: Circle?, contactType: ContactType?): Flow<List<ContactEntity>>

    @Query(
        "SELECT * FROM contacts WHERE archived = :archived " +
            "AND (:circle IS NULL OR circle = :circle) " +
            "AND (:contactType IS NULL OR contactType = :contactType) " +
            "ORDER BY name ASC",
    )
    suspend fun getFilteredOnce(archived: Boolean, circle: Circle?, contactType: ContactType?): List<ContactEntity>

    /**
     * `search_fulltext` port: source used a Lucene fulltext index over
     * name+notes; a plain `LIKE` is sufficient at personal-CRM scale.
     * Active contacts only, matching the source (this backs the live
     * dashboard search box, not an archive-browsing tool).
     */
    @Query(
        "SELECT * FROM contacts WHERE archived = 0 AND " +
            "(LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(notes) LIKE '%' || LOWER(:query) || '%') " +
            "ORDER BY name ASC LIMIT :limit",
    )
    suspend fun search(query: String, limit: Int = 20): List<ContactEntity>
}
