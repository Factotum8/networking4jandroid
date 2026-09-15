package com.factotum8.networking4jandroid.data.local.entity.enums

/**
 * The 3 concentric "FC SC SC" zones. Full names only — never abbreviated,
 * since Support Circle and Success Circle both shorten to "SC".
 *
 * Ported from `reference/models/enums.py::Circle`.
 */
enum class Circle(val value: String) {
    SUPPORT_CIRCLE("support_circle"),
    FUNCTIONAL_CIRCLES("functional_circles"),
    SUCCESS_CIRCLE("success_circle"),
}
