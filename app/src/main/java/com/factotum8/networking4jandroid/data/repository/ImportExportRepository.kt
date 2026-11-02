package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.io.ContactImportRow
import com.factotum8.networking4jandroid.data.io.GraphBackupData
import com.factotum8.networking4jandroid.data.io.ImportRowError
import com.factotum8.networking4jandroid.data.io.ImportSummary
import com.factotum8.networking4jandroid.data.io.buildBackupJson
import com.factotum8.networking4jandroid.data.io.contactsToCsv
import com.factotum8.networking4jandroid.data.io.contactsToVCard
import com.factotum8.networking4jandroid.data.io.parseCsv
import com.factotum8.networking4jandroid.data.io.parseVCard
import com.factotum8.networking4jandroid.data.local.dao.CommunityDao
import com.factotum8.networking4jandroid.data.local.dao.CompanyDao
import com.factotum8.networking4jandroid.data.local.dao.EventDao
import com.factotum8.networking4jandroid.data.local.dao.InterestDao
import com.factotum8.networking4jandroid.data.local.dao.ProjectDao
import com.factotum8.networking4jandroid.data.local.dao.RelativeDao
import com.factotum8.networking4jandroid.data.local.dao.TagDao
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** The two files a "backup now" produces, ready to be handed to SAF `CreateDocument`. */
data class GraphBackup(val json: String, val contactsCsv: String)

/**
 * Port of `app/services/contact_io.py` (CSV/vCard import/export) +
 * `app/services/backup_export.py` (full-graph JSON + contacts-CSV
 * backup). `NetworkingGoal`/`MonthlyGoal` are out of scope for this port
 * (not in the source's own nav — see the plan), so [buildBackup] omits
 * them too, unlike the source's `GraphBackup`.
 */
class ImportExportRepository @Inject constructor(
    private val contactRepository: ContactRepository,
    private val dimensionLinkRepository: DimensionLinkRepository,
    private val relativeDao: RelativeDao,
    private val knowsRepository: KnowsRepository,
    private val interactionRepository: InteractionRepository,
    private val actionRepository: ActionRepository,
    private val settingsRepository: SettingsRepository,
    private val companyDao: CompanyDao,
    private val eventDao: EventDao,
    private val communityDao: CommunityDao,
    private val projectDao: ProjectDao,
    private val interestDao: InterestDao,
    private val tagDao: TagDao,
) {
    suspend fun importCsv(text: String): ImportSummary = importRows(parseCsv(text))

    suspend fun importVCard(text: String): ImportSummary = importRows(parseVCard(text))

    private suspend fun importRows(parsed: Pair<List<ContactImportRow>, List<ImportRowError>>): ImportSummary {
        val (rows, errors) = parsed
        rows.forEach { row -> persistImportRow(row) }
        return ImportSummary(created = rows.size, errors = errors)
    }

    private suspend fun persistImportRow(row: ContactImportRow) {
        val contact = contactRepository.create(row.contact)
        attachDimension(contact.id, DimensionLinkType.WORKS_AT, row.companies)
        attachDimension(contact.id, DimensionLinkType.TAGGED, row.tags)
        attachDimension(contact.id, DimensionLinkType.INTERESTED_IN, row.interests)
    }

    private suspend fun attachDimension(contactId: String, type: DimensionLinkType, names: List<String>) {
        names.forEach { name ->
            val targetId = dimensionLinkRepository.getOrCreate(type, name)
            dimensionLinkRepository.attach(contactId, type, targetId)
        }
    }

    /** "Include archived" appends a second archived-only fetch, matching the source (not an OR filter). */
    suspend fun exportContactsCsv(includeArchived: Boolean = false): String = contactsToCsv(exportableContacts(includeArchived))

    suspend fun exportContactsVCard(includeArchived: Boolean = false): String = contactsToVCard(exportableContacts(includeArchived))

    private suspend fun exportableContacts(includeArchived: Boolean): List<ContactEntity> {
        val active = contactRepository.listOnce(archived = false)
        return if (includeArchived) active + contactRepository.listOnce(archived = true) else active
    }

    /**
     * Full-graph JSON + contacts-CSV backup, matching
     * `backup_export.py::build_backup` + `write_backup_files` minus goals
     * (out of scope for this port). The caller (import/export screen) is
     * responsible for writing both strings out via SAF `CreateDocument` —
     * there's no fixed server-side `backups/` folder on Android.
     */
    suspend fun buildBackup(): GraphBackup {
        val contacts = contactRepository.listOnce(archived = false) + contactRepository.listOnce(archived = true)
        val data = GraphBackupData(
            exportedAt = Instant.now(),
            contacts = contacts,
            interactions = interactionRepository.listAllOnce(),
            actions = actionRepository.listDue(before = LocalDate.MAX, includeCompleted = true),
            relatives = relativeDao.getAllOnce(),
            knows = knowsRepository.getAll(),
            dimensionLinks = dimensionLinkRepository.listAll(),
            companies = companyDao.getAllOnce(),
            events = eventDao.getAllOnce(),
            communities = communityDao.getAllOnce(),
            projects = projectDao.getAllOnce(),
            interests = interestDao.getAllOnce(),
            tags = tagDao.getAllOnce(),
            settings = settingsRepository.get(),
        )
        return GraphBackup(json = buildBackupJson(data), contactsCsv = contactsToCsv(contacts))
    }
}
