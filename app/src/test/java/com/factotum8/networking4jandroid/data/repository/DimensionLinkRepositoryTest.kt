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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DimensionLinkRepositoryTest {

    private val companyDao: CompanyDao = mockk()
    private val eventDao: EventDao = mockk()
    private val communityDao: CommunityDao = mockk()
    private val projectDao: ProjectDao = mockk()
    private val interestDao: InterestDao = mockk()
    private val tagDao: TagDao = mockk()
    private val contactCompanyCrossRefDao: ContactCompanyCrossRefDao = mockk()
    private val contactEventCrossRefDao: ContactEventCrossRefDao = mockk()
    private val contactCommunityCrossRefDao: ContactCommunityCrossRefDao = mockk()
    private val contactProjectCrossRefDao: ContactProjectCrossRefDao = mockk()
    private val contactInterestCrossRefDao: ContactInterestCrossRefDao = mockk()
    private val contactTagCrossRefDao: ContactTagCrossRefDao = mockk()

    private lateinit var repository: DimensionLinkRepository

    @Before
    fun setUp() {
        repository = DimensionLinkRepository(
            companyDao = companyDao,
            eventDao = eventDao,
            communityDao = communityDao,
            projectDao = projectDao,
            interestDao = interestDao,
            tagDao = tagDao,
            contactCompanyCrossRefDao = contactCompanyCrossRefDao,
            contactEventCrossRefDao = contactEventCrossRefDao,
            contactCommunityCrossRefDao = contactCommunityCrossRefDao,
            contactProjectCrossRefDao = contactProjectCrossRefDao,
            contactInterestCrossRefDao = contactInterestCrossRefDao,
            contactTagCrossRefDao = contactTagCrossRefDao,
        )
    }

    // --- getOrCreate: exact case-sensitive match scoped per dimension type ---

    @Test
    fun `getOrCreate WORKS_AT returns existing company id without inserting`() = runTest {
        val existing = CompanyEntity(id = "co1", name = "Acme")
        coEvery { companyDao.getByName("Acme") } returns existing

        val id = repository.getOrCreate(DimensionLinkType.WORKS_AT, "Acme")

        assertEquals("co1", id)
        coVerify(exactly = 0) { companyDao.insert(any()) }
    }

    @Test
    fun `getOrCreate WORKS_AT creates a new company when none matches by exact name`() = runTest {
        coEvery { companyDao.getByName("Acme") } returns null
        coEvery { companyDao.insert(any()) } returns Unit

        val id = repository.getOrCreate(DimensionLinkType.WORKS_AT, "Acme")

        assertTrue(id.isNotBlank())
        coVerify(exactly = 1) { companyDao.insert(CompanyEntity(id = id, name = "Acme")) }
    }

    @Test
    fun `getOrCreate ATTENDED delegates to event dao`() = runTest {
        val existing = EventEntity(id = "e1", name = "Conf")
        coEvery { eventDao.getByName("Conf") } returns existing

        assertEquals("e1", repository.getOrCreate(DimensionLinkType.ATTENDED, "Conf"))
        coVerify(exactly = 0) { eventDao.insert(any()) }
    }

    @Test
    fun `getOrCreate MEMBER_OF delegates to community dao`() = runTest {
        val existing = CommunityEntity(id = "co1", name = "Rotary")
        coEvery { communityDao.getByName("Rotary") } returns existing

        assertEquals("co1", repository.getOrCreate(DimensionLinkType.MEMBER_OF, "Rotary"))
        coVerify(exactly = 0) { communityDao.insert(any()) }
    }

    @Test
    fun `getOrCreate INVOLVED_IN delegates to project dao`() = runTest {
        val existing = ProjectEntity(id = "p1", name = "Launch")
        coEvery { projectDao.getByName("Launch") } returns existing

        assertEquals("p1", repository.getOrCreate(DimensionLinkType.INVOLVED_IN, "Launch"))
        coVerify(exactly = 0) { projectDao.insert(any()) }
    }

    @Test
    fun `getOrCreate INTERESTED_IN delegates to interest dao`() = runTest {
        val existing = InterestEntity(id = "i1", name = "Chess")
        coEvery { interestDao.getByName("Chess") } returns existing

        assertEquals("i1", repository.getOrCreate(DimensionLinkType.INTERESTED_IN, "Chess"))
        coVerify(exactly = 0) { interestDao.insert(any()) }
    }

    @Test
    fun `getOrCreate TAGGED delegates to tag dao and creates when absent`() = runTest {
        coEvery { tagDao.getByName("VIP") } returns null
        coEvery { tagDao.insert(any()) } returns Unit

        val id = repository.getOrCreate(DimensionLinkType.TAGGED, "VIP")

        coVerify(exactly = 1) { tagDao.insert(TagEntity(id = id, name = "VIP")) }
    }

    @Test
    fun `getOrCreate name matching is case-sensitive per the dao query contract`() = runTest {
        // Repository doesn't itself lowercase/normalize - it trusts the dao's exact-match query.
        // A different-case name is treated as a miss, so a new row is created rather than reused.
        coEvery { companyDao.getByName("acme") } returns null
        coEvery { companyDao.getByName("Acme") } returns CompanyEntity(id = "co1", name = "Acme")
        coEvery { companyDao.insert(any()) } returns Unit

        val lowerCaseId = repository.getOrCreate(DimensionLinkType.WORKS_AT, "acme")
        val exactId = repository.getOrCreate(DimensionLinkType.WORKS_AT, "Acme")

        assertTrue(lowerCaseId != exactId)
        assertEquals("co1", exactId)
    }

    // --- attach: delegates to the matching cross-ref dao's insert (IGNORE conflict is a dao-level concern) ---

    @Test
    fun `attach WORKS_AT inserts the company cross-ref`() = runTest {
        coEvery { contactCompanyCrossRefDao.insert(any()) } returns Unit

        repository.attach("c1", DimensionLinkType.WORKS_AT, "co1")

        coVerify(exactly = 1) { contactCompanyCrossRefDao.insert(ContactCompanyCrossRef("c1", "co1")) }
    }

    @Test
    fun `attach ATTENDED inserts the event cross-ref`() = runTest {
        coEvery { contactEventCrossRefDao.insert(any()) } returns Unit

        repository.attach("c1", DimensionLinkType.ATTENDED, "e1")

        coVerify(exactly = 1) { contactEventCrossRefDao.insert(ContactEventCrossRef("c1", "e1")) }
    }

    @Test
    fun `attach MEMBER_OF inserts the community cross-ref`() = runTest {
        coEvery { contactCommunityCrossRefDao.insert(any()) } returns Unit

        repository.attach("c1", DimensionLinkType.MEMBER_OF, "co1")

        coVerify(exactly = 1) { contactCommunityCrossRefDao.insert(ContactCommunityCrossRef("c1", "co1")) }
    }

    @Test
    fun `attach INVOLVED_IN inserts the project cross-ref`() = runTest {
        coEvery { contactProjectCrossRefDao.insert(any()) } returns Unit

        repository.attach("c1", DimensionLinkType.INVOLVED_IN, "p1")

        coVerify(exactly = 1) { contactProjectCrossRefDao.insert(ContactProjectCrossRef("c1", "p1")) }
    }

    @Test
    fun `attach INTERESTED_IN inserts the interest cross-ref`() = runTest {
        coEvery { contactInterestCrossRefDao.insert(any()) } returns Unit

        repository.attach("c1", DimensionLinkType.INTERESTED_IN, "i1")

        coVerify(exactly = 1) { contactInterestCrossRefDao.insert(ContactInterestCrossRef("c1", "i1")) }
    }

    @Test
    fun `attach TAGGED inserts the tag cross-ref`() = runTest {
        coEvery { contactTagCrossRefDao.insert(any()) } returns Unit

        repository.attach("c1", DimensionLinkType.TAGGED, "t1")

        coVerify(exactly = 1) { contactTagCrossRefDao.insert(ContactTagCrossRef("c1", "t1")) }
    }

    @Test
    fun `attach called twice for the same pair issues two insert calls, idempotency lives in the dao's conflict strategy`() = runTest {
        coEvery { contactCompanyCrossRefDao.insert(any()) } returns Unit

        repository.attach("c1", DimensionLinkType.WORKS_AT, "co1")
        repository.attach("c1", DimensionLinkType.WORKS_AT, "co1")

        // The repository itself has no de-dup logic; real idempotency depends on
        // ContactCompanyCrossRefDao.insert's @Insert(onConflict = IGNORE) annotation,
        // which a mocked dao does not exercise. This test documents that boundary.
        coVerify(exactly = 2) { contactCompanyCrossRefDao.insert(ContactCompanyCrossRef("c1", "co1")) }
    }

    // --- detach: delegates to the matching cross-ref dao's delete ---

    @Test
    fun `detach WORKS_AT deletes the company cross-ref`() = runTest {
        coEvery { contactCompanyCrossRefDao.delete(any()) } returns Unit

        repository.detach("c1", DimensionLinkType.WORKS_AT, "co1")

        coVerify(exactly = 1) { contactCompanyCrossRefDao.delete(ContactCompanyCrossRef("c1", "co1")) }
    }

    @Test
    fun `detach TAGGED deletes the tag cross-ref`() = runTest {
        coEvery { contactTagCrossRefDao.delete(any()) } returns Unit

        repository.detach("c1", DimensionLinkType.TAGGED, "t1")

        coVerify(exactly = 1) { contactTagCrossRefDao.delete(ContactTagCrossRef("c1", "t1")) }
    }

    @Test
    fun `detach does not throw when the row is not present`() = runTest {
        // @Delete on a Room dao is a no-op (no exception) when zero rows match; the mock mirrors that.
        coEvery { contactCompanyCrossRefDao.delete(any()) } returns Unit

        repository.detach("c1", DimensionLinkType.WORKS_AT, "never-attached")

        coVerify(exactly = 1) { contactCompanyCrossRefDao.delete(ContactCompanyCrossRef("c1", "never-attached")) }
    }

    // --- listForContact: combines all six dimension types for one contact ---

    @Test
    fun `listForContact combines all six dimension types and resolves target names`() = runTest {
        coEvery { contactCompanyCrossRefDao.getForContactOnce("c1") } returns listOf(ContactCompanyCrossRef("c1", "co1"))
        coEvery { companyDao.getById("co1") } returns CompanyEntity("co1", "Acme")

        coEvery { contactEventCrossRefDao.getForContactOnce("c1") } returns listOf(ContactEventCrossRef("c1", "e1"))
        coEvery { eventDao.getById("e1") } returns EventEntity("e1", "Conf")

        coEvery { contactCommunityCrossRefDao.getForContactOnce("c1") } returns emptyList()
        coEvery { contactProjectCrossRefDao.getForContactOnce("c1") } returns emptyList()
        coEvery { contactInterestCrossRefDao.getForContactOnce("c1") } returns emptyList()
        coEvery { contactTagCrossRefDao.getForContactOnce("c1") } returns emptyList()

        val links = repository.listForContact("c1")

        assertEquals(
            listOf(
                DimensionLink(DimensionLinkType.WORKS_AT, "co1", "Acme"),
                DimensionLink(DimensionLinkType.ATTENDED, "e1", "Conf"),
            ),
            links,
        )
    }

    @Test
    fun `listForContact skips a cross-ref whose target node was deleted`() = runTest {
        coEvery { contactCompanyCrossRefDao.getForContactOnce("c1") } returns listOf(ContactCompanyCrossRef("c1", "co1"))
        coEvery { companyDao.getById("co1") } returns null // dangling ref, target deleted

        coEvery { contactEventCrossRefDao.getForContactOnce("c1") } returns emptyList()
        coEvery { contactCommunityCrossRefDao.getForContactOnce("c1") } returns emptyList()
        coEvery { contactProjectCrossRefDao.getForContactOnce("c1") } returns emptyList()
        coEvery { contactInterestCrossRefDao.getForContactOnce("c1") } returns emptyList()
        coEvery { contactTagCrossRefDao.getForContactOnce("c1") } returns emptyList()

        assertEquals(emptyList<DimensionLink>(), repository.listForContact("c1"))
    }

    // --- listAll: flattened edge list across every contact, for backups ---

    @Test
    fun `listAll flattens cross-refs across all contacts and dimension types`() = runTest {
        coEvery { contactCompanyCrossRefDao.getAllOnce() } returns listOf(ContactCompanyCrossRef("c1", "co1"))
        coEvery { companyDao.getById("co1") } returns CompanyEntity("co1", "Acme")

        coEvery { contactEventCrossRefDao.getAllOnce() } returns emptyList()
        coEvery { contactCommunityCrossRefDao.getAllOnce() } returns emptyList()
        coEvery { contactProjectCrossRefDao.getAllOnce() } returns emptyList()
        coEvery { contactInterestCrossRefDao.getAllOnce() } returns emptyList()
        coEvery { contactTagCrossRefDao.getAllOnce() } returns emptyList()

        val records = repository.listAll()

        assertEquals(
            listOf(DimensionLinkRecord("c1", DimensionLinkType.WORKS_AT, "co1", "Acme")),
            records,
        )
    }
}
