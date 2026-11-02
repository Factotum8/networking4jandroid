package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.CommunityDao
import com.factotum8.networking4jandroid.data.local.dao.CompanyDao
import com.factotum8.networking4jandroid.data.local.dao.EventDao
import com.factotum8.networking4jandroid.data.local.dao.InterestDao
import com.factotum8.networking4jandroid.data.local.dao.ProjectDao
import com.factotum8.networking4jandroid.data.local.dao.RelativeDao
import com.factotum8.networking4jandroid.data.local.dao.TagDao
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.CommunityEntity
import com.factotum8.networking4jandroid.data.local.entity.CompanyEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.EventEntity
import com.factotum8.networking4jandroid.data.local.entity.InteractionEntity
import com.factotum8.networking4jandroid.data.local.entity.InterestEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.ProjectEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import com.factotum8.networking4jandroid.data.local.entity.TagEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportExportRepositoryTest {

    private val contactRepository = mockk<ContactRepository>()
    private val dimensionLinkRepository = mockk<DimensionLinkRepository>(relaxUnitFun = true)
    private val relativeDao = mockk<RelativeDao>()
    private val knowsRepository = mockk<KnowsRepository>()
    private val interactionRepository = mockk<InteractionRepository>()
    private val actionRepository = mockk<ActionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val companyDao = mockk<CompanyDao>()
    private val eventDao = mockk<EventDao>()
    private val communityDao = mockk<CommunityDao>()
    private val projectDao = mockk<ProjectDao>()
    private val interestDao = mockk<InterestDao>()
    private val tagDao = mockk<TagDao>()

    private lateinit var repository: ImportExportRepository

    @Before
    fun setUp() {
        repository = ImportExportRepository(
            contactRepository = contactRepository,
            dimensionLinkRepository = dimensionLinkRepository,
            relativeDao = relativeDao,
            knowsRepository = knowsRepository,
            interactionRepository = interactionRepository,
            actionRepository = actionRepository,
            settingsRepository = settingsRepository,
            companyDao = companyDao,
            eventDao = eventDao,
            communityDao = communityDao,
            projectDao = projectDao,
            interestDao = interestDao,
            tagDao = tagDao,
        )
    }

    @Test
    fun `importCsv persists each valid row and attaches its dimensions`() = runTest {
        val csv = "name,photo,phone,email,position,city,birthday,notes,met_place,met_date,contact_type,circle,dangerous,interesting,difficult,company,tags,interests\r\n" +
            "Ann,,,,,,,,,,,,,,,\"Acme,Beta\",,\r\n"

        val created = ContactEntity(id = "new-id", name = "Ann")
        coEvery { contactRepository.create(any()) } returns created
        coEvery { dimensionLinkRepository.getOrCreate(DimensionLinkType.WORKS_AT, "Acme") } returns "co-acme"
        coEvery { dimensionLinkRepository.getOrCreate(DimensionLinkType.WORKS_AT, "Beta") } returns "co-beta"

        val summary = repository.importCsv(csv)

        assertEquals(1, summary.created)
        assertTrue(summary.errors.isEmpty())
        coVerify(exactly = 1) { contactRepository.create(match { it.name == "Ann" }) }
        coVerify(exactly = 1) { dimensionLinkRepository.getOrCreate(DimensionLinkType.WORKS_AT, "Acme") }
        coVerify(exactly = 1) { dimensionLinkRepository.attach("new-id", DimensionLinkType.WORKS_AT, "co-acme") }
        coVerify(exactly = 1) { dimensionLinkRepository.getOrCreate(DimensionLinkType.WORKS_AT, "Beta") }
        coVerify(exactly = 1) { dimensionLinkRepository.attach("new-id", DimensionLinkType.WORKS_AT, "co-beta") }
        coVerify(exactly = 0) { dimensionLinkRepository.attach(any(), DimensionLinkType.TAGGED, any()) }
        coVerify(exactly = 0) { dimensionLinkRepository.attach(any(), DimensionLinkType.INTERESTED_IN, any()) }
    }

    @Test
    fun `importCsv skips persistence for a row with a real parse error but keeps other rows`() = runTest {
        val csv = "name,photo,phone,email,position,city,birthday,notes,met_place,met_date,contact_type,circle,dangerous,interesting,difficult\r\n" +
            ",,,,,,,,,,,,,,\r\n" + // missing name -> ImportRowError
            "Bob,,,,,,,,,,,,,,\r\n"

        val created = ContactEntity(id = "bob-id", name = "Bob")
        coEvery { contactRepository.create(any()) } returns created

        val summary = repository.importCsv(csv)

        assertEquals(1, summary.created)
        assertEquals(1, summary.errors.size)
        coVerify(exactly = 1) { contactRepository.create(any()) }
    }

    @Test
    fun `importVCard routes through the vCard parser`() = runTest {
        val vcard = "BEGIN:VCARD\nFN:Ann\nEND:VCARD"
        val created = ContactEntity(id = "new-id", name = "Ann")
        coEvery { contactRepository.create(any()) } returns created

        val summary = repository.importVCard(vcard)

        assertEquals(1, summary.created)
        assertTrue(summary.errors.isEmpty())
        coVerify(exactly = 1) { contactRepository.create(match { it.name == "Ann" }) }
    }

    @Test
    fun `exportContactsCsv without archived only fetches the active contacts`() = runTest {
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(ContactEntity(id = "1", name = "Ann"))

        val csv = repository.exportContactsCsv(includeArchived = false)

        assertTrue(csv.contains("Ann"))
        coVerify(exactly = 1) { contactRepository.listOnce(archived = false) }
        coVerify(exactly = 0) { contactRepository.listOnce(archived = true) }
    }

    @Test
    fun `exportContactsCsv with includeArchived appends a second archived-only fetch`() = runTest {
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(ContactEntity(id = "1", name = "Active"))
        coEvery { contactRepository.listOnce(archived = true) } returns listOf(ContactEntity(id = "2", name = "Archived"))

        val csv = repository.exportContactsCsv(includeArchived = true)

        assertTrue(csv.contains("Active"))
        assertTrue(csv.contains("Archived"))
        coVerify(exactly = 1) { contactRepository.listOnce(archived = false) }
        coVerify(exactly = 1) { contactRepository.listOnce(archived = true) }
    }

    @Test
    fun `exportContactsVCard without archived only fetches the active contacts`() = runTest {
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(ContactEntity(id = "1", name = "Ann"))

        val vcard = repository.exportContactsVCard(includeArchived = false)

        assertTrue(vcard.contains("FN:Ann"))
        coVerify(exactly = 0) { contactRepository.listOnce(archived = true) }
    }

    @Test
    fun `buildBackup fetches all active and archived contacts and every dimension collection`() = runTest {
        val active = ContactEntity(id = "1", name = "Active")
        val archived = ContactEntity(id = "2", name = "Archived", archived = true)
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(active)
        coEvery { contactRepository.listOnce(archived = true) } returns listOf(archived)
        coEvery { interactionRepository.listAllOnce() } returns listOf(
            InteractionEntity(id = "i1", contactId = "1", type = InteractionType.CALL, date = LocalDate.of(2024, 1, 1)),
        )
        // buildBackup fetches the *entire* action history (not just due ones) by calling
        // listDue with LocalDate.MAX and includeCompleted=true.
        coEvery { actionRepository.listDue(before = LocalDate.MAX, includeCompleted = true) } returns listOf(
            ActionEntity(id = "a1", contactId = "1", type = ActionType.CALL, dueDate = LocalDate.of(2024, 1, 1)),
        )
        coEvery { relativeDao.getAllOnce() } returns listOf(RelativeEntity(id = "r1", contactId = "1", name = "Mom"))
        coEvery { knowsRepository.getAll() } returns listOf(KnowsEntity(contactId = "1", relatedContactId = "2"))
        coEvery { dimensionLinkRepository.listAll() } returns emptyList()
        coEvery { companyDao.getAllOnce() } returns listOf(CompanyEntity(id = "co1", name = "Acme"))
        coEvery { eventDao.getAllOnce() } returns listOf(EventEntity(id = "e1", name = "Meetup"))
        coEvery { communityDao.getAllOnce() } returns listOf(CommunityEntity(id = "cm1", name = "Club"))
        coEvery { projectDao.getAllOnce() } returns listOf(ProjectEntity(id = "p1", name = "Project"))
        coEvery { interestDao.getAllOnce() } returns listOf(InterestEntity(id = "in1", name = "Chess"))
        coEvery { tagDao.getAllOnce() } returns listOf(TagEntity(id = "t1", name = "VIP"))
        coEvery { settingsRepository.get() } returns SettingsEntity()

        val backup = repository.buildBackup()

        assertTrue(backup.json.contains("\"name\":\"Active\""))
        assertTrue(backup.json.contains("\"name\":\"Archived\""))
        assertTrue(backup.json.contains("\"name\":\"Acme\""))
        assertTrue(backup.json.contains("\"name\":\"Meetup\""))
        assertTrue(backup.contactsCsv.contains("Active"))
        assertTrue(backup.contactsCsv.contains("Archived"))
        coVerify(exactly = 1) { actionRepository.listDue(before = LocalDate.MAX, includeCompleted = true) }
    }
}
