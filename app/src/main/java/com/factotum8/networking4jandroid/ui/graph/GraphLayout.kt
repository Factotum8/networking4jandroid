package com.factotum8.networking4jandroid.ui.graph

import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.repository.GraphVisNode
import kotlin.math.cos
import kotlin.math.sin

/**
 * Port of `reference/ui/graph_layout.py`'s position math — deliberately
 * framework-agnostic (no Compose/Android types) so it's plain-JUnit
 * testable. [GraphScreen] converts these data-space coordinates to screen
 * pixels using the current zoom/pan.
 */
data class NodePosition(val x: Float, val y: Float)

/** Contacts with no [Circle] set get a small dedicated cluster at the very center rather than folding into a real ring. */
const val UNASSIGNED_RADIUS = 30f

val RING_RADII: Map<Circle, Float> = mapOf(
    Circle.SUPPORT_CIRCLE to 110f,
    Circle.FUNCTIONAL_CIRCLES to 230f,
    Circle.SUCCESS_CIRCLE to 350f,
)

const val DIMENSION_RADIUS = 470f
private const val MAX_RADIUS = DIMENSION_RADIUS

/**
 * Half-width of the shared view range at a given [zoom] level — higher zoom
 * means a smaller bound, i.e. nodes spread further apart on screen. Used to
 * convert between data-space and screen-pixel coordinates for both drawing
 * and drag-to-pan/pinch-to-zoom gesture handling, so panning/zooming feels
 * consistent with what's actually drawn.
 */
fun boundForZoom(zoom: Float): Float = (MAX_RADIUS * 1.15f) / zoom

private fun placeRing(nodes: List<GraphVisNode>, radius: Float, positions: MutableMap<String, NodePosition>) {
    val count = nodes.size
    nodes.forEachIndexed { i, node ->
        val angle = if (count > 0) (2 * Math.PI * i / count) else 0.0
        positions[node.id] = NodePosition((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
    }
}

/** One (x, y) per node: contacts arranged into their circle's ring (or the central unassigned cluster), dimension nodes on one outer ring. */
fun computePositions(nodes: List<GraphVisNode>): Map<String, NodePosition> {
    val groups = LinkedHashMap<Circle?, MutableList<GraphVisNode>>()
    val dimensionNodes = mutableListOf<GraphVisNode>()
    nodes.forEach { node ->
        if (node.label == "Contact") {
            groups.getOrPut(node.circle) { mutableListOf() } += node
        } else {
            dimensionNodes += node
        }
    }
    val positions = mutableMapOf<String, NodePosition>()
    groups.forEach { (circle, group) ->
        val radius = circle?.let(RING_RADII::getValue) ?: UNASSIGNED_RADIUS
        placeRing(group, radius, positions)
    }
    placeRing(dimensionNodes, DIMENSION_RADIUS, positions)
    return positions
}
