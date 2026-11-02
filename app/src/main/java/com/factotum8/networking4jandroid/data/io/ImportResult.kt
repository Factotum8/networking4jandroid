package com.factotum8.networking4jandroid.data.io

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity

/**
 * Transient import-result shapes — plain DTOs, not persisted, matching
 * `reference`/networking4j's `app/models/import_result.py`. [contact]'s
 * `id`/`createdAt`/`updatedAt` are placeholders; `ContactRepository.create`
 * assigns the real ones.
 */
data class ContactImportRow(
    val contact: ContactEntity,
    val companies: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
)

/** [row] is 1-indexed and human-facing (spreadsheet row or nth vCard), not a 0-indexed list index. */
data class ImportRowError(val row: Int, val message: String)

data class ImportSummary(val created: Int, val errors: List<ImportRowError> = emptyList())

/** Shared by [parseCsv]/[parseVCard] — one bad row/card becomes an [ImportRowError], not a failed batch. */
internal class ImportFieldException(message: String) : Exception(message)
