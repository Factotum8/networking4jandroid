package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.ContactCommunityCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactCompanyCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactEventCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactInterestCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactProjectCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactTagCrossRefDao
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import javax.inject.Inject

/** View-model shapes for the graph screen — mirrors `reference/models/graph.py`. */
data class GraphVisNode(val id: String, val label: String, val name: String, val circle: Circle? = null, val contactType: ContactType? = null)
data class GraphVisEdge(val id: String, val source: String, val target: String, val relType: String, val edgeLabel: String? = null)
data class GraphSnapshot(val nodes: List<GraphVisNode>, val edges: List<GraphVisEdge>, val totalContacts: Int) {
    val truncated: Boolean get() = nodes.count { it.label == "Contact" } < totalContacts
}

/**
 * Port of `GraphRepository`/`GraphHandler` (`snapshot`/`neighbors`/
 * `shortest_path`). `shortest_path` has no relational equivalent to Neo4j's
 * `shortestPath()` — implemented here as an in-memory BFS over all KNOWS
 * rows treated as undirected edges, fine at personal-CRM scale.
 */
class GraphRepository @Inject constructor(
    private val contactRepository: ContactRepository,
    private val knowsRepository: KnowsRepository,
    private val dimensionLinkRepository: DimensionLinkRepository,
    private val contactCompanyCrossRefDao: ContactCompanyCrossRefDao,
    private val contactEventCrossRefDao: ContactEventCrossRefDao,
    private val contactCommunityCrossRefDao: ContactCommunityCrossRefDao,
    private val contactProjectCrossRefDao: ContactProjectCrossRefDao,
    private val contactInterestCrossRefDao: ContactInterestCrossRefDao,
    private val contactTagCrossRefDao: ContactTagCrossRefDao,
) {
    suspend fun snapshot(
        limit: Int = 150,
        circles: List<Circle>? = null,
        contactTypes: List<ContactType>? = null,
        dimensionFilter: Pair<DimensionLinkType, String>? = null,
    ): GraphSnapshot {
        var contacts = contactRepository.listOnce(archived = false)
        if (circles != null) contacts = contacts.filter { it.circle in circles }
        if (contactTypes != null) contacts = contacts.filter { it.contactType in contactTypes }
        if (dimensionFilter != null) {
            val (relType, targetId) = dimensionFilter
            contacts = contacts.filter { c -> hasDimensionLink(c.id, relType, targetId) }
        }
        val totalContacts = contacts.size
        val capped = contacts.take(limit)
        val cappedIds = capped.map { it.id }.toSet()

        val contactNodes = capped.map { GraphVisNode(it.id, "Contact", it.name, it.circle, it.contactType) }

        val knowsEdges = cappedIds.flatMap { contactId -> knowsRepository.listForContactOnce(contactId) }
            .distinctBy { setOf(it.contactId, it.relatedContactId) }
            .filter { it.contactId in cappedIds && it.relatedContactId in cappedIds }
            .map { GraphVisEdge("${it.contactId}-${it.relatedContactId}", it.contactId, it.relatedContactId, "KNOWS", it.closeness?.toString()) }

        val dimensionNodes = mutableMapOf<String, GraphVisNode>()
        val dimensionEdges = mutableListOf<GraphVisEdge>()
        capped.forEach { contact ->
            dimensionLinkRepository.listForContact(contact.id).forEach { link ->
                dimensionNodes[link.targetId] = GraphVisNode(link.targetId, dimensionLabel(link.relType), link.targetName)
                dimensionEdges += GraphVisEdge("${contact.id}-${link.targetId}", contact.id, link.targetId, link.relType.name)
            }
        }

        return GraphSnapshot(contactNodes + dimensionNodes.values, knowsEdges + dimensionEdges, totalContacts)
    }

    /** One contact + all KNOWS neighbors (either direction, 1 hop, no filters) + that contact's own dimension links. */
    suspend fun neighbors(contactId: String): GraphSnapshot {
        val contact = contactRepository.getById(contactId) ?: return GraphSnapshot(emptyList(), emptyList(), 0)
        val knows = knowsRepository.listForContactOnce(contactId)
        val neighborIds = knows.map { if (it.contactId == contactId) it.relatedContactId else it.contactId }.distinct()
        val neighborContacts = neighborIds.mapNotNull { contactRepository.getById(it) }

        val nodes = mutableListOf(GraphVisNode(contact.id, "Contact", contact.name, contact.circle, contact.contactType))
        nodes += neighborContacts.map { GraphVisNode(it.id, "Contact", it.name, it.circle, it.contactType) }
        val edges = knows.distinctBy { setOf(it.contactId, it.relatedContactId) }
            .map { GraphVisEdge("${it.contactId}-${it.relatedContactId}", it.contactId, it.relatedContactId, "KNOWS", it.closeness?.toString()) }.toMutableList()

        dimensionLinkRepository.listForContact(contactId).forEach { link ->
            nodes += GraphVisNode(link.targetId, dimensionLabel(link.relType), link.targetName)
            edges += GraphVisEdge("$contactId-${link.targetId}", contactId, link.targetId, link.relType.name)
        }

        return GraphSnapshot(nodes, edges, nodes.count { it.label == "Contact" })
    }

    /** Undirected BFS over every KNOWS row. Returns the contact-id chain, or null if no path exists. */
    suspend fun shortestPath(fromId: String, toId: String): List<String>? {
        if (fromId == toId) return listOf(fromId)
        val adjacency = mutableMapOf<String, MutableList<String>>()
        knowsRepository.getAll().forEach { edge ->
            adjacency.getOrPut(edge.contactId) { mutableListOf() } += edge.relatedContactId
            adjacency.getOrPut(edge.relatedContactId) { mutableListOf() } += edge.contactId
        }
        val visited = mutableSetOf(fromId)
        val queue = ArrayDeque<List<String>>().apply { add(listOf(fromId)) }
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val last = path.last()
            for (next in adjacency[last].orEmpty()) {
                if (next == toId) return path + next
                if (visited.add(next)) queue.add(path + next)
            }
        }
        return null
    }

    private suspend fun hasDimensionLink(contactId: String, relType: DimensionLinkType, targetId: String): Boolean = when (relType) {
        DimensionLinkType.WORKS_AT -> contactCompanyCrossRefDao.getForContactOnce(contactId).any { it.companyId == targetId }
        DimensionLinkType.ATTENDED -> contactEventCrossRefDao.getForContactOnce(contactId).any { it.eventId == targetId }
        DimensionLinkType.MEMBER_OF -> contactCommunityCrossRefDao.getForContactOnce(contactId).any { it.communityId == targetId }
        DimensionLinkType.INVOLVED_IN -> contactProjectCrossRefDao.getForContactOnce(contactId).any { it.projectId == targetId }
        DimensionLinkType.INTERESTED_IN -> contactInterestCrossRefDao.getForContactOnce(contactId).any { it.interestId == targetId }
        DimensionLinkType.TAGGED -> contactTagCrossRefDao.getForContactOnce(contactId).any { it.tagId == targetId }
    }

    private fun dimensionLabel(type: DimensionLinkType): String = when (type) {
        DimensionLinkType.WORKS_AT -> "Company"
        DimensionLinkType.ATTENDED -> "Event"
        DimensionLinkType.MEMBER_OF -> "Community"
        DimensionLinkType.INVOLVED_IN -> "Project"
        DimensionLinkType.INTERESTED_IN -> "Interest"
        DimensionLinkType.TAGGED -> "Tag"
    }
}
