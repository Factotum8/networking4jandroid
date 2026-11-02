package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.ContactCommunityCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactCompanyCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactEventCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactInterestCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactProjectCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactTagCrossRefDao
import com.factotum8.networking4jandroid.data.local.entity.ContactCompanyCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GraphRepositoryTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var knowsRepository: KnowsRepository
    private lateinit var dimensionLinkRepository: DimensionLinkRepository
    private lateinit var contactCompanyCrossRefDao: ContactCompanyCrossRefDao
    private lateinit var contactEventCrossRefDao: ContactEventCrossRefDao
    private lateinit var contactCommunityCrossRefDao: ContactCommunityCrossRefDao
    private lateinit var contactProjectCrossRefDao: ContactProjectCrossRefDao
    private lateinit var contactInterestCrossRefDao: ContactInterestCrossRefDao
    private lateinit var contactTagCrossRefDao: ContactTagCrossRefDao
    private lateinit var repository: GraphRepository

    @Before
    fun setUp() {
        contactRepository = mockk()
        knowsRepository = mockk()
        dimensionLinkRepository = mockk()
        contactCompanyCrossRefDao = mockk()
        contactEventCrossRefDao = mockk()
        contactCommunityCrossRefDao = mockk()
        contactProjectCrossRefDao = mockk()
        contactInterestCrossRefDao = mockk()
        contactTagCrossRefDao = mockk()
        repository = GraphRepository(
            contactRepository,
            knowsRepository,
            dimensionLinkRepository,
            contactCompanyCrossRefDao,
            contactEventCrossRefDao,
            contactCommunityCrossRefDao,
            contactProjectCrossRefDao,
            contactInterestCrossRefDao,
            contactTagCrossRefDao,
        )
        coEvery { dimensionLinkRepository.listForContact(any()) } returns emptyList()
    }

    private fun contact(id: String, name: String = id, circle: Circle? = null, contactType: ContactType? = null) =
        ContactEntity(id = id, name = name, circle = circle, contactType = contactType, archived = false)

    // --- snapshot() ---

    @Test
    fun `snapshot with no filters returns all active contacts as nodes with correct totalContacts`() = runTest {
        val contacts = listOf(contact("c1"), contact("c2"))
        coEvery { contactRepository.listOnce(archived = false) } returns contacts
        coEvery { knowsRepository.listForContactOnce(any()) } returns emptyList()

        val snapshot = repository.snapshot()

        assertEquals(2, snapshot.totalContacts)
        assertEquals(2, snapshot.nodes.size)
        assertTrue(snapshot.edges.isEmpty())
        assertEquals(false, snapshot.truncated)
    }

    @Test
    fun `snapshot AND-combines circle and contactType filters`() = runTest {
        val matches = contact("c1", circle = Circle.SUPPORT_CIRCLE, contactType = ContactType.CONNECTOR)
        val wrongType = contact("c2", circle = Circle.SUPPORT_CIRCLE, contactType = ContactType.BRIDGE)
        val wrongCircle = contact("c3", circle = Circle.SUCCESS_CIRCLE, contactType = ContactType.CONNECTOR)
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(matches, wrongType, wrongCircle)
        coEvery { knowsRepository.listForContactOnce(any()) } returns emptyList()

        val snapshot = repository.snapshot(circles = listOf(Circle.SUPPORT_CIRCLE), contactTypes = listOf(ContactType.CONNECTOR))

        assertEquals(1, snapshot.totalContacts)
        assertEquals(listOf("c1"), snapshot.nodes.map { it.id })
    }

    @Test
    fun `snapshot caps nodes at limit but reports the true totalContacts and marks truncated`() = runTest {
        val contacts = listOf(contact("c1"), contact("c2"), contact("c3"))
        coEvery { contactRepository.listOnce(archived = false) } returns contacts
        coEvery { knowsRepository.listForContactOnce(any()) } returns emptyList()

        val snapshot = repository.snapshot(limit = 2)

        assertEquals(3, snapshot.totalContacts)
        assertEquals(2, snapshot.nodes.size)
        assertTrue(snapshot.truncated)
    }

    @Test
    fun `snapshot drops KNOWS edges whose other endpoint was cut by the limit`() = runTest {
        val c1 = contact("c1")
        val c2 = contact("c2")
        val c3 = contact("c3") // will be cut by limit = 2
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c1, c2, c3)
        val edgeWithinCap = KnowsEntity(contactId = "c1", relatedContactId = "c2")
        val edgeOutsideCap = KnowsEntity(contactId = "c1", relatedContactId = "c3")
        coEvery { knowsRepository.listForContactOnce("c1") } returns listOf(edgeWithinCap, edgeOutsideCap)
        coEvery { knowsRepository.listForContactOnce("c2") } returns listOf(edgeWithinCap)

        val snapshot = repository.snapshot(limit = 2)

        assertEquals(1, snapshot.edges.size)
        assertEquals("c1", snapshot.edges[0].source)
        assertEquals("c2", snapshot.edges[0].target)
    }

    @Test
    fun `snapshot deduplicates a KNOWS edge seen from both endpoints`() = runTest {
        val c1 = contact("c1")
        val c2 = contact("c2")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c1, c2)
        val edge = KnowsEntity(contactId = "c1", relatedContactId = "c2")
        // The DAO query is `contactId = X OR relatedContactId = X`, so the same row
        // comes back once per capped contact whose id appears on either side.
        coEvery { knowsRepository.listForContactOnce("c1") } returns listOf(edge)
        coEvery { knowsRepository.listForContactOnce("c2") } returns listOf(edge)

        val snapshot = repository.snapshot()

        assertEquals(1, snapshot.edges.size)
    }

    @Test
    fun `snapshot applies the dimension filter via the matching cross-ref dao`() = runTest {
        val withCompany = contact("c1")
        val withoutCompany = contact("c2")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(withCompany, withoutCompany)
        coEvery { knowsRepository.listForContactOnce(any()) } returns emptyList()
        coEvery { contactCompanyCrossRefDao.getForContactOnce("c1") } returns listOf(ContactCompanyCrossRef("c1", "acme"))
        coEvery { contactCompanyCrossRefDao.getForContactOnce("c2") } returns emptyList()

        val snapshot = repository.snapshot(dimensionFilter = DimensionLinkType.WORKS_AT to "acme")

        assertEquals(1, snapshot.totalContacts)
        assertEquals(listOf("c1"), snapshot.nodes.map { it.id })
    }

    // --- neighbors() ---

    @Test
    fun `neighbors returns an empty snapshot when the contact does not exist`() = runTest {
        coEvery { contactRepository.getById("missing") } returns null

        val snapshot = repository.neighbors("missing")

        assertEquals(GraphSnapshot(emptyList(), emptyList(), 0), snapshot)
        coVerify(exactly = 0) { knowsRepository.listForContactOnce(any()) }
    }

    @Test
    fun `neighbors includes contacts on either side of a KNOWS edge and counts only Contact nodes`() = runTest {
        val center = contact("c1")
        val outgoingNeighbor = contact("c2")
        val incomingNeighbor = contact("c3")
        coEvery { contactRepository.getById("c1") } returns center
        coEvery { contactRepository.getById("c2") } returns outgoingNeighbor
        coEvery { contactRepository.getById("c3") } returns incomingNeighbor
        coEvery { knowsRepository.listForContactOnce("c1") } returns listOf(
            KnowsEntity(contactId = "c1", relatedContactId = "c2"),
            KnowsEntity(contactId = "c3", relatedContactId = "c1"),
        )

        val snapshot = repository.neighbors("c1")

        assertEquals(setOf("c1", "c2", "c3"), snapshot.nodes.map { it.id }.toSet())
        assertEquals(2, snapshot.edges.size)
        assertEquals(3, snapshot.totalContacts)
    }

    // --- shortestPath() ---

    @Test
    fun `shortestPath from a node to itself returns a single-element path without querying edges`() = runTest {
        val path = repository.shortestPath("c1", "c1")

        assertEquals(listOf("c1"), path)
        coVerify(exactly = 0) { knowsRepository.getAll() }
    }

    @Test
    fun `shortestPath finds a direct neighbor`() = runTest {
        coEvery { knowsRepository.getAll() } returns listOf(KnowsEntity(contactId = "c1", relatedContactId = "c2"))

        val path = repository.shortestPath("c1", "c2")

        assertEquals(listOf("c1", "c2"), path)
    }

    @Test
    fun `shortestPath is undirected regardless of which side is stored as source`() = runTest {
        coEvery { knowsRepository.getAll() } returns listOf(KnowsEntity(contactId = "c2", relatedContactId = "c1"))

        val path = repository.shortestPath("c1", "c2")

        assertEquals(listOf("c1", "c2"), path)
    }

    @Test
    fun `shortestPath finds the shortest multi-hop chain`() = runTest {
        coEvery { knowsRepository.getAll() } returns listOf(
            KnowsEntity(contactId = "c1", relatedContactId = "c2"),
            KnowsEntity(contactId = "c2", relatedContactId = "c3"),
            KnowsEntity(contactId = "c1", relatedContactId = "c4"),
            KnowsEntity(contactId = "c4", relatedContactId = "c3"),
        )

        val path = repository.shortestPath("c1", "c3")

        assertEquals(3, path?.size)
        assertEquals("c1", path?.first())
        assertEquals("c3", path?.last())
    }

    @Test
    fun `shortestPath returns null when no path exists`() = runTest {
        coEvery { knowsRepository.getAll() } returns listOf(KnowsEntity(contactId = "c1", relatedContactId = "c2"))

        val path = repository.shortestPath("c1", "unreachable")

        assertNull(path)
    }
}
