package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.io.DimensionLinkRecord
import com.factotum8.networking4jandroid.data.local.dao.CommunityDao
import com.factotum8.networking4jandroid.data.local.dao.CompanyDao
import com.factotum8.networking4jandroid.data.local.dao.ContactCommunityCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactCompanyCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactEventCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactInterestCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactProjectCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactTagCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.EventDao
import com.factotum8.networking4jandroid.data.local.dao.InterestDao
import com.factotum8.networking4jandroid.data.local.dao.ProjectDao
import com.factotum8.networking4jandroid.data.local.dao.TagDao
import com.factotum8.networking4jandroid.data.local.entity.CommunityEntity
import com.factotum8.networking4jandroid.data.local.entity.CompanyEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactCommunityCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactCompanyCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactEventCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactInterestCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactProjectCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactTagCrossRef
import com.factotum8.networking4jandroid.data.local.entity.EventEntity
import com.factotum8.networking4jandroid.data.local.entity.InterestEntity
import com.factotum8.networking4jandroid.data.local.entity.ProjectEntity
import com.factotum8.networking4jandroid.data.local.entity.TagEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import java.util.UUID
import javax.inject.Inject

/** One attached dimension node, as the contact-detail screen's "Связи" section needs it. */
data class DimensionLink(val relType: DimensionLinkType, val targetId: String, val targetName: String)

/**
 * Port of the dimension-node half of `LinkRepository`/`LinkHandler` +
 * `NamedNodeRepository` (the "attach existing / create new" pickers for
 * Company/Event/Community/Project/Interest/Tag).
 *
 * `getOrCreate` is exact, case-sensitive match scoped per dimension type —
 * a Company "Acme" and a Tag "Acme" are different rows, matching the
 * source's per-label uniqueness. `attach` is idempotent (the cross-ref
 * DAOs already insert with `OnConflictStrategy.IGNORE`); `detach` is a
 * no-op, not an error, if the row isn't there.
 */
class DimensionLinkRepository @Inject constructor(
    private val companyDao: CompanyDao,
    private val eventDao: EventDao,
    private val communityDao: CommunityDao,
    private val projectDao: ProjectDao,
    private val interestDao: InterestDao,
    private val tagDao: TagDao,
    private val contactCompanyCrossRefDao: ContactCompanyCrossRefDao,
    private val contactEventCrossRefDao: ContactEventCrossRefDao,
    private val contactCommunityCrossRefDao: ContactCommunityCrossRefDao,
    private val contactProjectCrossRefDao: ContactProjectCrossRefDao,
    private val contactInterestCrossRefDao: ContactInterestCrossRefDao,
    private val contactTagCrossRefDao: ContactTagCrossRefDao,
) {
    suspend fun getOrCreate(type: DimensionLinkType, name: String): String = when (type) {
        DimensionLinkType.WORKS_AT ->
            companyDao.getByName(name)?.id ?: UUID.randomUUID().toString().also {
                companyDao.insert(CompanyEntity(id = it, name = name))
            }

        DimensionLinkType.ATTENDED ->
            eventDao.getByName(name)?.id ?: UUID.randomUUID().toString().also {
                eventDao.insert(EventEntity(id = it, name = name))
            }

        DimensionLinkType.MEMBER_OF ->
            communityDao.getByName(name)?.id ?: UUID.randomUUID().toString().also {
                communityDao.insert(CommunityEntity(id = it, name = name))
            }

        DimensionLinkType.INVOLVED_IN ->
            projectDao.getByName(name)?.id ?: UUID.randomUUID().toString().also {
                projectDao.insert(ProjectEntity(id = it, name = name))
            }

        DimensionLinkType.INTERESTED_IN ->
            interestDao.getByName(name)?.id ?: UUID.randomUUID().toString().also {
                interestDao.insert(InterestEntity(id = it, name = name))
            }

        DimensionLinkType.TAGGED ->
            tagDao.getByName(name)?.id ?: UUID.randomUUID().toString().also {
                tagDao.insert(TagEntity(id = it, name = name))
            }
    }

    suspend fun attach(contactId: String, type: DimensionLinkType, targetId: String) {
        when (type) {
            DimensionLinkType.WORKS_AT -> contactCompanyCrossRefDao.insert(ContactCompanyCrossRef(contactId, targetId))
            DimensionLinkType.ATTENDED -> contactEventCrossRefDao.insert(ContactEventCrossRef(contactId, targetId))
            DimensionLinkType.MEMBER_OF -> contactCommunityCrossRefDao.insert(ContactCommunityCrossRef(contactId, targetId))
            DimensionLinkType.INVOLVED_IN -> contactProjectCrossRefDao.insert(ContactProjectCrossRef(contactId, targetId))
            DimensionLinkType.INTERESTED_IN -> contactInterestCrossRefDao.insert(ContactInterestCrossRef(contactId, targetId))
            DimensionLinkType.TAGGED -> contactTagCrossRefDao.insert(ContactTagCrossRef(contactId, targetId))
        }
    }

    suspend fun detach(contactId: String, type: DimensionLinkType, targetId: String) {
        when (type) {
            DimensionLinkType.WORKS_AT -> contactCompanyCrossRefDao.delete(ContactCompanyCrossRef(contactId, targetId))
            DimensionLinkType.ATTENDED -> contactEventCrossRefDao.delete(ContactEventCrossRef(contactId, targetId))
            DimensionLinkType.MEMBER_OF -> contactCommunityCrossRefDao.delete(ContactCommunityCrossRef(contactId, targetId))
            DimensionLinkType.INVOLVED_IN -> contactProjectCrossRefDao.delete(ContactProjectCrossRef(contactId, targetId))
            DimensionLinkType.INTERESTED_IN -> contactInterestCrossRefDao.delete(ContactInterestCrossRef(contactId, targetId))
            DimensionLinkType.TAGGED -> contactTagCrossRefDao.delete(ContactTagCrossRef(contactId, targetId))
        }
    }

    /** All six dimension types' attached nodes for one contact, combined for the "Связи" section. */
    suspend fun listForContact(contactId: String): List<DimensionLink> {
        val links = mutableListOf<DimensionLink>()
        contactCompanyCrossRefDao.getForContactOnce(contactId).forEach { ref ->
            companyDao.getById(ref.companyId)?.let { links += DimensionLink(DimensionLinkType.WORKS_AT, it.id, it.name) }
        }
        contactEventCrossRefDao.getForContactOnce(contactId).forEach { ref ->
            eventDao.getById(ref.eventId)?.let { links += DimensionLink(DimensionLinkType.ATTENDED, it.id, it.name) }
        }
        contactCommunityCrossRefDao.getForContactOnce(contactId).forEach { ref ->
            communityDao.getById(ref.communityId)?.let { links += DimensionLink(DimensionLinkType.MEMBER_OF, it.id, it.name) }
        }
        contactProjectCrossRefDao.getForContactOnce(contactId).forEach { ref ->
            projectDao.getById(ref.projectId)?.let { links += DimensionLink(DimensionLinkType.INVOLVED_IN, it.id, it.name) }
        }
        contactInterestCrossRefDao.getForContactOnce(contactId).forEach { ref ->
            interestDao.getById(ref.interestId)?.let { links += DimensionLink(DimensionLinkType.INTERESTED_IN, it.id, it.name) }
        }
        contactTagCrossRefDao.getForContactOnce(contactId).forEach { ref ->
            tagDao.getById(ref.tagId)?.let { links += DimensionLink(DimensionLinkType.TAGGED, it.id, it.name) }
        }
        return links
    }

    /** All six dimension types' edges across every contact — the backup's flattened edge list. */
    suspend fun listAll(): List<DimensionLinkRecord> {
        val links = mutableListOf<DimensionLinkRecord>()
        contactCompanyCrossRefDao.getAllOnce().forEach { ref ->
            companyDao.getById(ref.companyId)?.let { links += DimensionLinkRecord(ref.contactId, DimensionLinkType.WORKS_AT, it.id, it.name) }
        }
        contactEventCrossRefDao.getAllOnce().forEach { ref ->
            eventDao.getById(ref.eventId)?.let { links += DimensionLinkRecord(ref.contactId, DimensionLinkType.ATTENDED, it.id, it.name) }
        }
        contactCommunityCrossRefDao.getAllOnce().forEach { ref ->
            communityDao.getById(ref.communityId)?.let { links += DimensionLinkRecord(ref.contactId, DimensionLinkType.MEMBER_OF, it.id, it.name) }
        }
        contactProjectCrossRefDao.getAllOnce().forEach { ref ->
            projectDao.getById(ref.projectId)?.let { links += DimensionLinkRecord(ref.contactId, DimensionLinkType.INVOLVED_IN, it.id, it.name) }
        }
        contactInterestCrossRefDao.getAllOnce().forEach { ref ->
            interestDao.getById(ref.interestId)?.let { links += DimensionLinkRecord(ref.contactId, DimensionLinkType.INTERESTED_IN, it.id, it.name) }
        }
        contactTagCrossRefDao.getAllOnce().forEach { ref ->
            tagDao.getById(ref.tagId)?.let { links += DimensionLinkRecord(ref.contactId, DimensionLinkType.TAGGED, it.id, it.name) }
        }
        return links
    }
}
