package com.factotum8.networking4jandroid.ui

/** Route strings for [NetworkingApp]'s `NavHost` — kept separate from the graph wiring itself. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val CONTACTS = "contacts"
    const val GRAPH = "graph"
    const val MORE = "more"
    const val DUPLICATES = "duplicates"
    const val SETTINGS = "settings"
    const val IMPORT_EXPORT = "import_export"

    const val CONTACT_ID_ARG = "contactId"
    const val CONTACT_DETAIL = "contact/{$CONTACT_ID_ARG}"

    /** Passed as [CONTACT_ID_ARG] to open the contact-detail screen in "create" mode. */
    const val NEW_CONTACT_ID = "new"

    fun contactDetail(contactId: String): String = "contact/$contactId"
}
