package com.factotum8.networking4jandroid.data.local.entity.enums

/**
 * Exactly one value per contact.
 *
 * Ported from `reference/models/enums.py::ContactType`.
 */
enum class ContactType(val value: String) {
    CONNECTOR("connector"),
    CONDENSER("condenser"),
    BRIDGE("bridge"),
    INSIDER("insider"),
}
