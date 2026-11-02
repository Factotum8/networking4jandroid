package com.factotum8.networking4jandroid.ui.graph

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.repository.GraphSnapshot
import com.factotum8.networking4jandroid.data.repository.GraphVisNode
import com.factotum8.networking4jandroid.ui.common.CIRCLE_LABELS
import com.factotum8.networking4jandroid.ui.common.CONTACT_TYPE_LABELS
import com.factotum8.networking4jandroid.ui.common.FilterDropdown
import com.factotum8.networking4jandroid.ui.common.MutedText
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val TAP_HIT_RADIUS_PX = 48f
private const val CONTACT_NODE_RADIUS_PX = 14f
private const val DIMENSION_NODE_RADIUS_PX = 9f

/**
 * Port of `app/ui/pages/graph.py`: a filtered graph snapshot drawn as
 * concentric rings (via [computePositions]) on a Compose [Canvas], with
 * pinch-zoom/drag-pan gestures (the source only had mouse-drag pan and
 * zoom buttons — pinch is a phone-appropriate addition per the plan),
 * tap-to-select bottom sheet, a collapsible filter section and a
 * collapsible shortest-path finder. Backed by [GraphViewModel].
 */
@Composable
fun GraphScreen(onOpenContact: (String) -> Unit, viewModel: GraphViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.statusText.isNotEmpty()) {
                Text(uiState.statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ExpandableSection(title = "Фильтры графа") {
                FiltersSection(uiState = uiState, viewModel = viewModel)
            }
            ExpandableSection(title = "Цепочка знакомств") {
                PathFinderSection(uiState = uiState, viewModel = viewModel)
            }
            ZoomControlsRow(
                zoom = uiState.zoom,
                onZoomIn = viewModel::zoomIn,
                onZoomOut = viewModel::zoomOut,
                onReset = viewModel::resetView,
            )
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                GraphCanvas(uiState = uiState, viewModel = viewModel, modifier = Modifier.fillMaxSize())
                if (uiState.loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    uiState.selectedNode?.let { node ->
        NodeInfoSheet(
            node = node,
            onDismiss = viewModel::dismissNodeSheet,
            onOpenContact = onOpenContact,
            onExpand = viewModel::expandNeighborhood,
            onFilterByNode = viewModel::filterByDimensionNode,
        )
    }
}

@Composable
private fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (expanded) "▾" else "▸", style = MaterialTheme.typography.titleMedium)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun FiltersSection(uiState: GraphUiState, viewModel: GraphViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterDropdown(
                modifier = Modifier.weight(1f),
                label = "Круг",
                options = listOf<Circle?>(null) + Circle.entries,
                optionLabel = { it?.let(CIRCLE_LABELS::getValue) ?: "Все" },
                selected = uiState.circleFilter,
                onSelected = viewModel::onCircleFilterChange,
            )
            FilterDropdown(
                modifier = Modifier.weight(1f),
                label = "Тип",
                options = listOf<ContactType?>(null) + ContactType.entries,
                optionLabel = { it?.let(CONTACT_TYPE_LABELS::getValue) ?: "Все" },
                selected = uiState.contactTypeFilter,
                onSelected = viewModel::onContactTypeFilterChange,
            )
        }
        OutlinedTextField(
            value = uiState.limitText,
            onValueChange = viewModel::onLimitChange,
            label = { Text("Лимит контактов") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = viewModel::refreshSnapshot) { Text("Обновить") }
    }
}

@Composable
private fun PathFinderSection(uiState: GraphUiState, viewModel: GraphViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterDropdown(
            label = "От кого",
            options = listOf<ContactEntity?>(null) + uiState.allContacts,
            optionLabel = { it?.name ?: "Выберите контакт" },
            selected = uiState.allContacts.find { it.id == uiState.fromContactId },
            onSelected = { viewModel.onFromContactChange(it?.id) },
        )
        FilterDropdown(
            label = "До кого",
            options = listOf<ContactEntity?>(null) + uiState.allContacts,
            optionLabel = { it?.name ?: "Выберите контакт" },
            selected = uiState.allContacts.find { it.id == uiState.toContactId },
            onSelected = { viewModel.onToContactChange(it?.id) },
        )
        uiState.pathError?.let { pathError ->
            Text(pathError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::findPath) { Text("Найти путь") }
            OutlinedButton(onClick = viewModel::refreshSnapshot) { Text("Показать весь граф") }
        }
    }
}

@Composable
private fun ZoomControlsRow(zoom: Float, onZoomIn: () -> Unit, onZoomOut: () -> Unit, onReset: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onZoomOut) { Text("−", style = MaterialTheme.typography.headlineSmall) }
        Text("${(zoom * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = onZoomIn) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        TextButton(onClick = onReset) { Text("Сбросить вид") }
        Text("Перетаскивайте/щипок — пан и зум", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GraphCanvas(uiState: GraphUiState, viewModel: GraphViewModel, modifier: Modifier = Modifier) {
    var canvasPx by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    viewModel.onGesture(pan.x, pan.y, zoom, canvasPx)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val current = viewModel.uiState.value
                    hitTestNode(offset, current, canvasPx)?.let { viewModel.selectNode(it.id) }
                }
            },
    ) {
        val snapshot = uiState.snapshot ?: return@Canvas
        drawGraph(snapshot, uiState.zoom, uiState.panX, uiState.panY)
    }
}

private fun dataToScreen(pos: NodePosition, canvasPx: Float, zoom: Float, panX: Float, panY: Float): Offset {
    val bound = boundForZoom(zoom)
    val scale = canvasPx / (2f * bound)
    val x = canvasPx / 2f + (pos.x - panX) * scale
    val y = canvasPx / 2f - (pos.y - panY) * scale
    return Offset(x, y)
}

private fun hitTestNode(tap: Offset, state: GraphUiState, canvasPx: Float): GraphVisNode? {
    val snapshot = state.snapshot ?: return null
    if (canvasPx <= 0f) return null
    val positions = computePositions(snapshot.nodes)
    var closest: GraphVisNode? = null
    var closestDist = Float.MAX_VALUE
    snapshot.nodes.forEach { node ->
        val pos = positions[node.id] ?: return@forEach
        val screen = dataToScreen(pos, canvasPx, state.zoom, state.panX, state.panY)
        val dist = hypot((screen.x - tap.x).toDouble(), (screen.y - tap.y).toDouble()).toFloat()
        if (dist < TAP_HIT_RADIUS_PX && dist < closestDist) {
            closest = node
            closestDist = dist
        }
    }
    return closest
}

private fun DrawScope.drawGraph(snapshot: GraphSnapshot, zoom: Float, panX: Float, panY: Float) {
    val canvasPx = size.width
    val positions = computePositions(snapshot.nodes)
    val origin = dataToScreen(NodePosition(0f, 0f), canvasPx, zoom, panX, panY)

    RING_RADII.values.forEach { r ->
        val edgePoint = dataToScreen(NodePosition(r, 0f), canvasPx, zoom, panX, panY)
        val screenRadius = abs(edgePoint.x - origin.x)
        drawCircle(color = RING_COLOR, radius = screenRadius, center = origin, style = Stroke(width = 2f))
    }

    snapshot.edges.forEach { edge ->
        val s = positions[edge.source] ?: return@forEach
        val t = positions[edge.target] ?: return@forEach
        drawLine(
            color = if (edge.relType == "KNOWS") KNOWS_EDGE_COLOR else DIMENSION_EDGE_COLOR,
            start = dataToScreen(s, canvasPx, zoom, panX, panY),
            end = dataToScreen(t, canvasPx, zoom, panX, panY),
            strokeWidth = if (edge.relType == "KNOWS") 3f else 1.5f,
        )
    }

    val textPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 28f
        isAntiAlias = true
    }
    snapshot.nodes.forEach { node ->
        val pos = positions[node.id] ?: return@forEach
        val screen = dataToScreen(pos, canvasPx, zoom, panX, panY)
        val isContact = node.label == "Contact"
        val radius = if (isContact) CONTACT_NODE_RADIUS_PX else DIMENSION_NODE_RADIUS_PX
        drawCircle(color = nodeColor(node), radius = radius, center = screen)
        drawContext.canvas.nativeCanvas.drawText(node.name, screen.x + radius + 4f, screen.y + 4f, textPaint)
    }
}

private val RING_COLOR = Color(0x59648CDC)
private val KNOWS_EDGE_COLOR = Color(0xFF8A8A8A)
private val DIMENSION_EDGE_COLOR = Color(0xFFBDBDBD)

private fun nodeColor(node: GraphVisNode): Color = when {
    node.label != "Contact" -> Color(0xFF9C27B0)
    node.circle == Circle.SUPPORT_CIRCLE -> Color(0xFF2196F3)
    node.circle == Circle.FUNCTIONAL_CIRCLES -> Color(0xFF4CAF50)
    node.circle == Circle.SUCCESS_CIRCLE -> Color(0xFFFF9800)
    else -> Color(0xFF9E9E9E)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeInfoSheet(
    node: GraphVisNode,
    onDismiss: () -> Unit,
    onOpenContact: (String) -> Unit,
    onExpand: (String) -> Unit,
    onFilterByNode: (GraphVisNode) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(node.name, style = MaterialTheme.typography.titleLarge)
            if (node.label == "Contact") {
                TextButton(onClick = { onOpenContact(node.id) }) { Text("Открыть карточку") }
                TextButton(onClick = { onExpand(node.id) }) { Text("Развернуть окружение") }
            } else {
                MutedText("Тип: ${node.label}")
                TextButton(onClick = { onFilterByNode(node) }) { Text("Показать связанные контакты") }
            }
        }
    }
}
