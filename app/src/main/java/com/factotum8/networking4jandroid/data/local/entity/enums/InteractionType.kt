package com.factotum8.networking4jandroid.data.local.entity.enums

/**
 * `(:Contact)-[:HAD_INTERACTION]->(:Interaction)` types
 * (meetings/calls/correspondence/referrals).
 *
 * Ported from `reference/models/enums.py::InteractionType` (present in
 * enums.py but skipped in the previous stage since the `Interaction` node
 * itself hadn't been snapshotted yet).
 */
enum class InteractionType(val value: String) {
    MEETING("meeting"),
    CALL("call"),
    CORRESPONDENCE("correspondence"),
    REFERRAL("referral"),
}
