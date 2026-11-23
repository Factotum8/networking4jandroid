package com.factotum8.networking4jandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.factotum8.networking4jandroid.data.io.BackupRestoreData
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.CommunityEntity
import com.factotum8.networking4jandroid.data.local.entity.CompanyEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactCommunityCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactCompanyCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEventCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactInterestCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactProjectCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactTagCrossRef
import com.factotum8.networking4jandroid.data.local.entity.EventEntity
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.InterestEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.ProjectEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import com.factotum8.networking4jandroid.data.local.entity.TagEntity

/**
 * Whole-database replace for "import desktop backup". [replaceAll] runs in
 * one Room transaction: if any insert fails, the delete is rolled back too
 * and the user's existing data stays untouched. Only fixed, parameterless
 * statements and Room-generated inserts — no query is built from file content.
 */
@Dao
abstract class BackupRestoreDao {
    @Query("DELETE FROM contact_company_cross_refs") protected abstract suspend fun clearCompanyLinks()
    @Query("DELETE FROM contact_event_cross_refs") protected abstract suspend fun clearEventLinks()
    @Query("DELETE FROM contact_community_cross_refs") protected abstract suspend fun clearCommunityLinks()
    @Query("DELETE FROM contact_project_cross_refs") protected abstract suspend fun clearProjectLinks()
    @Query("DELETE FROM contact_interest_cross_refs") protected abstract suspend fun clearInterestLinks()
    @Query("DELETE FROM contact_tag_cross_refs") protected abstract suspend fun clearTagLinks()
    @Query("DELETE FROM knows") protected abstract suspend fun clearKnows()
    @Query("DELETE FROM interactions") protected abstract suspend fun clearInteractions()
    @Query("DELETE FROM actions") protected abstract suspend fun clearActions()
    @Query("DELETE FROM relatives") protected abstract suspend fun clearRelatives()
    @Query("DELETE FROM companies") protected abstract suspend fun clearCompanies()
    @Query("DELETE FROM events") protected abstract suspend fun clearEvents()
    @Query("DELETE FROM communities") protected abstract suspend fun clearCommunities()
    @Query("DELETE FROM projects") protected abstract suspend fun clearProjects()
    @Query("DELETE FROM interests") protected abstract suspend fun clearInterests()
    @Query("DELETE FROM tags") protected abstract suspend fun clearTags()
    @Query("DELETE FROM contacts") protected abstract suspend fun clearContacts()

    @Insert protected abstract suspend fun insertContacts(rows: List<ContactEntity>)
    @Insert protected abstract suspend fun insertInteractions(rows: List<InteractionEntity>)
    @Insert protected abstract suspend fun insertActions(rows: List<ActionEntity>)
    @Insert protected abstract suspend fun insertRelatives(rows: List<RelativeEntity>)
    @Insert protected abstract suspend fun insertKnows(rows: List<KnowsEntity>)
    @Insert protected abstract suspend fun insertCompanies(rows: List<CompanyEntity>)
    @Insert protected abstract suspend fun insertEvents(rows: List<EventEntity>)
    @Insert protected abstract suspend fun insertCommunities(rows: List<CommunityEntity>)
    @Insert protected abstract suspend fun insertProjects(rows: List<ProjectEntity>)
    @Insert protected abstract suspend fun insertInterests(rows: List<InterestEntity>)
    @Insert protected abstract suspend fun insertTags(rows: List<TagEntity>)
    @Insert protected abstract suspend fun insertCompanyLinks(rows: List<ContactCompanyCrossRef>)
    @Insert protected abstract suspend fun insertEventLinks(rows: List<ContactEventCrossRef>)
    @Insert protected abstract suspend fun insertCommunityLinks(rows: List<ContactCommunityCrossRef>)
    @Insert protected abstract suspend fun insertProjectLinks(rows: List<ContactProjectCrossRef>)
    @Insert protected abstract suspend fun insertInterestLinks(rows: List<ContactInterestCrossRef>)
    @Insert protected abstract suspend fun insertTagLinks(rows: List<ContactTagCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertSettings(settings: SettingsEntity)

    /** Children are cleared before parents and parents inserted before children, so it works with or without FK enforcement. */
    @Transaction
    open suspend fun replaceAll(data: BackupRestoreData) {
        clearCompanyLinks()
        clearEventLinks()
        clearCommunityLinks()
        clearProjectLinks()
        clearInterestLinks()
        clearTagLinks()
        clearKnows()
        clearInteractions()
        clearActions()
        clearRelatives()
        clearCompanies()
        clearEvents()
        clearCommunities()
        clearProjects()
        clearInterests()
        clearTags()
        clearContacts()

        insertContacts(data.contacts)
        insertCompanies(data.companies)
        insertEvents(data.events)
        insertCommunities(data.communities)
        insertProjects(data.projects)
        insertInterests(data.interests)
        insertTags(data.tags)
        insertInteractions(data.interactions)
        insertActions(data.actions)
        insertRelatives(data.relatives)
        insertKnows(data.knows)
        insertCompanyLinks(data.companyLinks)
        insertEventLinks(data.eventLinks)
        insertCommunityLinks(data.communityLinks)
        insertProjectLinks(data.projectLinks)
        insertInterestLinks(data.interestLinks)
        insertTagLinks(data.tagLinks)
        upsertSettings(data.settings)
    }
}
