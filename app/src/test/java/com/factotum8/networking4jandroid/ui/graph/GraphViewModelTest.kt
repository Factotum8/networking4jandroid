package com.factotum8.networking4jandroid.ui.graph

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import com.factotum8.networking4jandroid.data.repository.ContactRepository
import com.factotum8.networking4jandroid.data.repository.GraphRepository
import com.factotum8.networking4jandroid.data.repository.GraphSnapshot
import com.factotum8.networking4jandroid.data.repository.GraphVisNode
import com.factotum8.networking4jandroid.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GraphViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val graphRepository = mockk<GraphRepository>()
    private val contactRepository = mockk<ContactRepository>()

    private fun contact(id: String, name: String = "Contact $id") = ContactEntity(id = id, name = name)
    private fun node(id: String, label: String = "Contact") = GraphVisNode(id, label, "Name $id")
    private fun emptySnapshot(total: Int = 0) = GraphSnapshot(emptyList(), emptyList(), total)

    private fun createViewModel() = GraphViewModel(graphRepository, contactRepository)

    @Before
    fun setUp() {
        coEvery { contactRepository.listOnce() } returns emptyList()
        coEvery { graphRepository.snapshot(any(), any(), any(), any()) } returns emptySnapshot()
    }

    // --- init / refreshSnapshot ---

    @Test
    fun `init loads all contacts and the default snapshot`() = runTest(mainDispatcherRule.dispatcher) {
        val contacts = listOf(contact("c1"))
        coEvery { contactRepository.listOnce() } returns contacts
        // Not truncated: the returned Contact-node count matches totalContacts (nothing was capped by the limit).
        val fullSnapshot = GraphSnapshot(listOf(node("c1"), node("c2"), node("c3")), emptyList(), totalContacts = 3)
        coEvery { graphRepository.snapshot(DEFAULT_LIMIT, null, null, null) } returns fullSnapshot

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(contacts, state.allContacts)
        assertFalse(state.loading)
        assertEquals("Контактов: 3", state.statusText)
    }

    @Test
    fun `refreshSnapshot with a blank or invalid limit falls back to the default`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLimitChange("not-a-number")
        viewModel.refreshSnapshot()
        advanceUntilIdle()

        coVerify { graphRepository.snapshot(DEFAULT_LIMIT, null, null, null) }
    }

    @Test
    fun `refreshSnapshot wraps single-select filters into one-element lists`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCircleFilterChange(Circle.SUPPORT_CIRCLE)
        viewModel.onContactTypeFilterChange(ContactType.CONNECTOR)
        viewModel.onLimitChange("50")
        viewModel.refreshSnapshot()
        advanceUntilIdle()

        coVerify { graphRepository.snapshot(50, listOf(Circle.SUPPORT_CIRCLE), listOf(ContactType.CONNECTOR), null) }
    }

    @Test
    fun `truncated snapshot produces a shown-of-total status`() = runTest(mainDispatcherRule.dispatcher) {
        val snapshot = GraphSnapshot(listOf(node("c1")), emptyList(), totalContacts = 5)
        coEvery { graphRepository.snapshot(any(), any(), any(), any()) } returns snapshot

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Показано 1 из 5", viewModel.uiState.value.statusText)
    }

    // --- zoom / pan ---

    @Test
    fun `zoomIn and zoomOut clamp at the configured bounds`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        repeat(20) { viewModel.zoomIn() }
        assertEquals(ZOOM_MAX, viewModel.uiState.value.zoom, 0.001f)

        repeat(40) { viewModel.zoomOut() }
        assertEquals(ZOOM_MIN, viewModel.uiState.value.zoom, 0.001f)
    }

    @Test
    fun `resetView resets zoom and pan then re-fetches the snapshot`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.zoomIn()
        viewModel.onGesture(panDxPx = 100f, panDyPx = 100f, zoomFactor = 1f, canvasPx = 500f)

        viewModel.resetView()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1f, state.zoom, 0.001f)
        assertEquals(0f, state.panX, 0.001f)
        assertEquals(0f, state.panY, 0.001f)
        coVerify(atLeast = 2) { graphRepository.snapshot(any(), any(), any(), any()) } // once on init, once on reset
    }

    @Test
    fun `onGesture with a non-positive canvas size is a no-op`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val before = viewModel.uiState.value

        viewModel.onGesture(panDxPx = 50f, panDyPx = 50f, zoomFactor = 2f, canvasPx = 0f)

        assertEquals(before.zoom, viewModel.uiState.value.zoom, 0.001f)
        assertEquals(before.panX, viewModel.uiState.value.panX, 0.001f)
    }

    @Test
    fun `onGesture pans opposite to drag direction on x and with it on y, and applies zoom`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onGesture(panDxPx = 100f, panDyPx = 100f, zoomFactor = 1.5f, canvasPx = 500f)

        val state = viewModel.uiState.value
        assertEquals(1.5f, state.zoom, 0.001f)
        assertTrue("panX should decrease when dragging right (dx > 0)", state.panX < 0f)
        assertTrue("panY should increase when dragging down (dy > 0)", state.panY > 0f)
    }

    // --- path finder ---

    @Test
    fun `findPath with a missing endpoint sets pathError and does not call the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onFromContactChange("c1")
        // toContactId left unset

        viewModel.findPath()
        advanceUntilIdle()

        assertEquals("Выберите оба контакта", viewModel.uiState.value.pathError)
        coVerify(exactly = 0) { graphRepository.shortestPath(any(), any()) }
    }

    @Test
    fun `findPath with no path sets a not-found pathError`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { graphRepository.shortestPath("c1", "c2") } returns null
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onFromContactChange("c1")
        viewModel.onToContactChange("c2")

        viewModel.findPath()
        advanceUntilIdle()

        assertEquals("Путь не найден — нет цепочки знакомств между этими контактами", viewModel.uiState.value.pathError)
    }

    @Test
    fun `findPath with a chain builds a snapshot of that chain's contacts and consecutive KNOWS edges`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { graphRepository.shortestPath("c1", "c3") } returns listOf("c1", "c2", "c3")
        coEvery { contactRepository.getById("c1") } returns contact("c1")
        coEvery { contactRepository.getById("c2") } returns contact("c2")
        coEvery { contactRepository.getById("c3") } returns contact("c3")
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onFromContactChange("c1")
        viewModel.onToContactChange("c3")

        viewModel.findPath()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.pathError)
        val snapshot = state.snapshot!!
        assertEquals(3, snapshot.nodes.size)
        assertEquals(2, snapshot.edges.size)
        assertEquals("c1", snapshot.edges[0].source)
        assertEquals("c2", snapshot.edges[0].target)
        assertEquals("c2", snapshot.edges[1].source)
        assertEquals("c3", snapshot.edges[1].target)
        assertTrue(snapshot.edges.all { it.relType == "KNOWS" })
    }

    // --- node selection ---

    @Test
    fun `selectNode finds the node in the current snapshot`() = runTest(mainDispatcherRule.dispatcher) {
        val snapshot = GraphSnapshot(listOf(node("c1"), node("c2")), emptyList(), 2)
        coEvery { graphRepository.snapshot(any(), any(), any(), any()) } returns snapshot
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectNode("c2")

        assertEquals("c2", viewModel.uiState.value.selectedNode?.id)
    }

    @Test
    fun `selectNode with an unknown id is a no-op`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectNode("missing")

        assertNull(viewModel.uiState.value.selectedNode)
    }

    @Test
    fun `dismissNodeSheet clears the selection`() = runTest(mainDispatcherRule.dispatcher) {
        val snapshot = GraphSnapshot(listOf(node("c1")), emptyList(), 1)
        coEvery { graphRepository.snapshot(any(), any(), any(), any()) } returns snapshot
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.selectNode("c1")

        viewModel.dismissNodeSheet()

        assertNull(viewModel.uiState.value.selectedNode)
    }

    @Test
    fun `expandNeighborhood replaces the snapshot and closes the sheet`() = runTest(mainDispatcherRule.dispatcher) {
        val neighborhood = GraphSnapshot(listOf(node("c1"), node("c2")), emptyList(), 2)
        coEvery { graphRepository.neighbors("c1") } returns neighborhood
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.expandNeighborhood("c1")
        advanceUntilIdle()

        assertEquals(neighborhood, viewModel.uiState.value.snapshot)
        assertNull(viewModel.uiState.value.selectedNode)
    }

    @Test
    fun `filterByDimensionNode with an unrecognized label is a no-op`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.filterByDimensionNode(node("d1", label = "Unknown"))
        advanceUntilIdle()

        // exactly 1 == only the init-triggered refreshSnapshot() call; the no-op filter added none
        coVerify(exactly = 1) { graphRepository.snapshot(any(), any(), any(), any()) }
    }

    @Test
    fun `filterByDimensionNode maps the node label to its DimensionLinkType and filters by it`() = runTest(mainDispatcherRule.dispatcher) {
        val filtered = GraphSnapshot(listOf(node("c1")), emptyList(), 1)
        coEvery { graphRepository.snapshot(DEFAULT_LIMIT, null, null, DimensionLinkType.TAGGED to "d1") } returns filtered
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.filterByDimensionNode(node("d1", label = "Tag"))
        advanceUntilIdle()

        assertEquals(filtered, viewModel.uiState.value.snapshot)
        assertNull(viewModel.uiState.value.selectedNode)
    }
}
