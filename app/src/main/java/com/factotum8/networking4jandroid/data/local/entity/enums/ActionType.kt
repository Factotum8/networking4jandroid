package com.factotum8.networking4jandroid.data.local.entity.enums

/**
 * `(:Contact)-[:NEXT_ACTION]->(:Action)` types (write/call/meet/come back
 * later).
 *
 * Ported from `reference/models/enums.py::ActionType` (present in enums.py
 * but skipped in the previous stage since the `Action` node itself hadn't
 * been snapshotted yet).
 */
enum class ActionType(val value: String) {
    WRITE("write"),
    CALL("call"),
    MEET("meet"),
    FOLLOW_UP_LATER("follow_up_later"),
}
