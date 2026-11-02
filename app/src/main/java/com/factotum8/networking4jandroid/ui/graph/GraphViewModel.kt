package com.factotum8.networking4jandroid.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import com.factotum8.networking4jandroid.data.repository.GraphRepository
import com.factotum8.networking4jandroid.data.repository.GraphSnapshot
import com.factotum8.networking4jandroid.data.repository.GraphVisEdge
import com.factotum8.networking4jandroid.data.repository.GraphVisNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val DEFAULT_LIMIT = 150
const val ZOOM_STEP = 1.25f
const val ZOOM_MIN = 0.4f
const val ZOOM_MAX = 4f

/** Mirrors `GraphRepository`'s private `dimensionLabel()` — the graph screen only has each node's already-rendered [GraphVisNode.label] to work back from. */
private val LABEL_TO_REL_TYPE: Map<String, DimensionLinkType> = mapOf(
    "Company" to DimensionLinkType.WORKS_AT,
    "Event" to DimensionLinkType.ATTENDED,
    "Community" to DimensionLinkType.MEMBER_OF,
    "Project" to DimensionLinkType.INVOLVED_IN,
    "Interest" to DimensionLinkType.INTERESTED_IN,
    "Tag" to DimensionLinkType.TAGGED,
)

data class GraphUiState(
    val snapshot: GraphSnapshot? = null,
    val loading: Boolean = true,
    val statusText: String = "",
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val circleFilter: Circle? = null,
    val contactTypeFilter: ContactType? = null,
    val limitText: String = DEFAULT_LIMIT.toString(),
    val allContacts: List<ContactEntity> = emptyList(),
    val fromContactId: String? = null,
    val toContactId: String? = null,
    val pathError: String? = null,
    val selectedNode: GraphVisNode? = null,
)

/**
 * Port of `app/ui/pages/graph.py`: a filtered graph snapshot rendered as
 * concentric rings via [computePositions], zoom/pan state, the shortest-path
 * finder, and tap-to-select node info (open card / expand neighborhood /
 * filter by dimension node) — [GraphScreen] owns the actual Canvas drawing
 * and gesture handling.
 *
 * The source's circle/type filters are multi-select; this port uses a
 * single nullable selection per filter (like every other screen's
 * [com.factotum8.networking4jandroid.ui.common.FilterDropdown]-based
 * filter), wrapped in a one-element list for [GraphRepository.snapshot].
 */
@HiltViewModel
class GraphViewModel @Inject constructor(
    private val graphRepository: GraphRepository,
    private val contactRepository: ContactRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
        refreshSnapshot()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(allContacts = contactRepository.listOnce()) }
        }
    }

    fun onCircleFilterChange(circle: Circle?) = _uiState.update { it.copy(circleFilter = circle) }
    fun onContactTypeFilterChange(contactType: ContactType?) = _uiState.update { it.copy(contactTypeFilter = contactType) }
    fun onLimitChange(text: String) = _uiState.update { it.copy(limitText = text) }

    /** Bound to the "Обновить"/"Показать весь граф" buttons — filters only take effect on an explicit refresh, matching the source. */
    fun refreshSnapshot() {
        viewModelScope.launch {
            val state = _uiState.value
            val limit = state.limitText.trim().toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_LIMIT
            val snapshot = graphRepository.snapshot(
                limit = limit,
                circles = state.circleFilter?.let { listOf(it) },
                contactTypes = state.contactTypeFilter?.let { listOf(it) },
            )
            _uiState.update { it.copy(snapshot = snapshot, loading = false, selectedNode = null, statusText = statusFor(snapshot)) }
        }
    }

    /** Resets pan/zoom *and* drops out of an isolated expand/path/dimension-filter view back to the full filtered graph. */
    fun resetView() {
        _uiState.update { it.copy(zoom = 1f, panX = 0f, panY = 0f) }
        refreshSnapshot()
    }

    fun zoomIn() = _uiState.update { it.copy(zoom = (it.zoom * ZOOM_STEP).coerceAtMost(ZOOM_MAX)) }
    fun zoomOut() = _uiState.update { it.copy(zoom = (it.zoom / ZOOM_STEP).coerceAtLeast(ZOOM_MIN)) }

    /** [panDxPx]/[panDyPx] are raw on-screen pixel deltas for this gesture frame, [zoomFactor] is relative (1f = no change), [canvasPx] is the square canvas's current side length in the same pixel units. */
    fun onGesture(panDxPx: Float, panDyPx: Float, zoomFactor: Float, canvasPx: Float) {
        if (canvasPx <= 0f) return
        val state = _uiState.value
        val bound = boundForZoom(state.zoom)
        val dataPerPx = (2 * bound) / canvasPx
        val newZoom = (state.zoom * zoomFactor).coerceIn(ZOOM_MIN, ZOOM_MAX)
        _uiState.update {
            it.copy(
                zoom = newZoom,
                panX = it.panX - panDxPx * dataPerPx,
                panY = it.panY + panDyPx * dataPerPx,
            )
        }
    }

    // --- path finder ---

    fun onFromContactChange(contactId: String?) = _uiState.update { it.copy(fromContactId = contactId, pathError = null) }
    fun onToContactChange(contactId: String?) = _uiState.update { it.copy(toContactId = contactId, pathError = null) }

    fun findPath() {
        val state = _uiState.value
        val from = state.fromContactId
        val to = state.toContactId
        if (from == null || to == null) {
            _uiState.update { it.copy(pathError = "Выберите оба контакта") }
            return
        }
        viewModelScope.launch {
            val chain = graphRepository.shortestPath(from, to)
            if (chain == null) {
                _uiState.update { it.copy(pathError = "Путь не найден — нет цепочки знакомств между этими контактами") }
                return@launch
            }
            val contacts = chain.mapNotNull { contactRepository.getById(it) }
            val nodes = contacts.map { GraphVisNode(it.id, "Contact", it.name, it.circle, it.contactType) }
            val edges = chain.zipWithNext { a, b -> GraphVisEdge("$a-$b", a, b, "KNOWS") }
            val snapshot = GraphSnapshot(nodes, edges, nodes.size)
            _uiState.update { it.copy(snapshot = snapshot, pathError = null, selectedNode = null, statusText = statusFor(snapshot)) }
        }
    }

    // --- node selection ---

    fun selectNode(nodeId: String) {
        val node = _uiState.value.snapshot?.nodes?.find { it.id == nodeId } ?: return
        _uiState.update { it.copy(selectedNode = node) }
    }

    fun dismissNodeSheet() = _uiState.update { it.copy(selectedNode = null) }

    /** Isolates the view on this contact's immediate neighborhood — replaces the current snapshot, doesn't merge into it. */
    fun expandNeighborhood(contactId: String) {
        viewModelScope.launch {
            val neighborhood = graphRepository.neighbors(contactId)
            _uiState.update { it.copy(snapshot = neighborhood, selectedNode = null, statusText = statusFor(neighborhood)) }
        }
    }

    fun filterByDimensionNode(node: GraphVisNode) {
        val relType = LABEL_TO_REL_TYPE[node.label] ?: return
        viewModelScope.launch {
            val state = _uiState.value
            val limit = state.limitText.trim().toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_LIMIT
            val snapshot = graphRepository.snapshot(limit = limit, dimensionFilter = relType to node.id)
            _uiState.update { it.copy(snapshot = snapshot, selectedNode = null, statusText = statusFor(snapshot)) }
        }
    }

    private fun statusFor(snapshot: GraphSnapshot): String {
        val contactCount = snapshot.nodes.count { it.label == "Contact" }
        return if (snapshot.truncated) "Показано $contactCount из ${snapshot.totalContacts}" else "Контактов: ${snapshot.totalContacts}"
    }
}
