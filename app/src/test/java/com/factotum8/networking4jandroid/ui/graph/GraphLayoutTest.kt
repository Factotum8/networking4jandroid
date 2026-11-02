package com.factotum8.networking4jandroid.ui.graph

import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.repository.GraphVisNode
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

private const val EPS = 1e-3f

class GraphLayoutTest {

    private fun contactNode(id: String, circle: Circle? = null) = GraphVisNode(id, "Contact", "Name $id", circle)
    private fun dimensionNode(id: String, label: String = "Tag") = GraphVisNode(id, label, "Name $id")

    @Test
    fun `single contact in a circle lands exactly on that circle's ring radius`() {
        val positions = computePositions(listOf(contactNode("c1", Circle.SUPPORT_CIRCLE)))

        val p = positions.getValue("c1")
        val radius = sqrt(p.x * p.x + p.y * p.y)
        assertEquals(RING_RADII.getValue(Circle.SUPPORT_CIRCLE), radius, EPS)
    }

    @Test
    fun `contact with no circle lands on the unassigned radius`() {
        val positions = computePositions(listOf(contactNode("c1", circle = null)))

        val p = positions.getValue("c1")
        val radius = sqrt(p.x * p.x + p.y * p.y)
        assertEquals(UNASSIGNED_RADIUS, radius, EPS)
    }

    @Test
    fun `dimension node lands on the dimension radius regardless of type`() {
        val positions = computePositions(listOf(dimensionNode("d1", "Company")))

        val p = positions.getValue("d1")
        val radius = sqrt(p.x * p.x + p.y * p.y)
        assertEquals(DIMENSION_RADIUS, radius, EPS)
    }

    @Test
    fun `two contacts in the same circle are placed at opposite points on the ring`() {
        val positions = computePositions(listOf(contactNode("c1", Circle.FUNCTIONAL_CIRCLES), contactNode("c2", Circle.FUNCTIONAL_CIRCLES)))

        val p1 = positions.getValue("c1")
        val p2 = positions.getValue("c2")
        // angle 0 and angle pi -> diametrically opposite, so p1 == -p2
        assertEquals(-p1.x, p2.x, EPS)
        assertEquals(-p1.y, p2.y, EPS)
    }

    @Test
    fun `each circle group is placed independently starting at angle zero`() {
        // A lone node in each of two different circles both start at angle 0 -> both land on the positive x-axis of their own ring.
        val positions = computePositions(
            listOf(contactNode("c1", Circle.SUPPORT_CIRCLE), contactNode("c2", Circle.SUCCESS_CIRCLE)),
        )

        val p1 = positions.getValue("c1")
        val p2 = positions.getValue("c2")
        assertEquals(RING_RADII.getValue(Circle.SUPPORT_CIRCLE), p1.x, EPS)
        assertEquals(0f, p1.y, EPS)
        assertEquals(RING_RADII.getValue(Circle.SUCCESS_CIRCLE), p2.x, EPS)
        assertEquals(0f, p2.y, EPS)
    }

    @Test
    fun `empty node list returns empty positions`() {
        assertEquals(emptyMap<String, NodePosition>(), computePositions(emptyList()))
    }

    @Test
    fun `boundForZoom halves as zoom doubles`() {
        val b1 = boundForZoom(1f)
        val b2 = boundForZoom(2f)
        assertEquals(b1 / 2f, b2, EPS)
    }

    @Test
    fun `boundForZoom at the default zoom covers the outer dimension ring with margin`() {
        val bound = boundForZoom(1f)
        assertEquals(DIMENSION_RADIUS * 1.15f, bound, EPS)
    }
}
