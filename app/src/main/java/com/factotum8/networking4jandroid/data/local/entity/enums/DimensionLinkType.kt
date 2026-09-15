package com.factotum8.networking4jandroid.data.local.entity.enums

/**
 * The property-less `(:Contact)-[REL]->(:DimensionNode)` relationship
 * types. Not persisted as a column — the type is implied by which
 * Contact-dimension cross-ref table a row lives in — kept here only as the
 * code-level mapping from that table to the source graph's edge label.
 *
 * Ported from `reference/models/enums.py::DimensionLinkType`.
 */
enum class DimensionLinkType(val value: String) {
    WORKS_AT("WORKS_AT"),
    ATTENDED("ATTENDED"),
    MEMBER_OF("MEMBER_OF"),
    INVOLVED_IN("INVOLVED_IN"),
    INTERESTED_IN("INTERESTED_IN"),
    TAGGED("TAGGED"),
}
