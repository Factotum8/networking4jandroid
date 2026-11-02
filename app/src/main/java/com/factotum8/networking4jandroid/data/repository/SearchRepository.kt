package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import javax.inject.Inject
import kotlin.math.max

data class DuplicateCandidate(val contactA: ContactEntity, val contactB: ContactEntity, val score: Double, val matchedOn: List<String>)

private const val NAME_MATCH_FLOOR = 80.0

/**
 * Port of `SearchHandler` (`search` + `find_duplicates`).
 *
 * The source's name-similarity score is `rapidfuzz.fuzz.token_sort_ratio`.
 * No equivalent ships for Kotlin without a new dependency, so [nameRatio]
 * approximates it with a normalized Levenshtein ratio over
 * token-sorted names — not byte-identical to rapidfuzz, but the same
 * 0-100 "how similar are these names" signal.
 */
class SearchRepository @Inject constructor(
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun search(query: String, limit: Int = 20): List<ContactEntity> = contactRepository.search(query, limit)

    suspend fun findDuplicates(threshold: Double? = null): List<DuplicateCandidate> {
        val effectiveThreshold = threshold ?: settingsRepository.get().dedupScoreThreshold
        val contacts = contactRepository.listOnce(archived = false)
        val candidates = mutableListOf<DuplicateCandidate>()
        for (i in contacts.indices) {
            for (j in i + 1 until contacts.size) {
                pairCandidate(contacts[i], contacts[j])?.let { candidates += it }
            }
        }
        return candidates.filter { it.score >= effectiveThreshold }.sortedByDescending { it.score }
    }

    private fun pairCandidate(a: ContactEntity, b: ContactEntity): DuplicateCandidate? {
        val nameScore = nameRatio(a.name, b.name)
        val matchedOn = mutableListOf<String>()
        if (nameScore >= NAME_MATCH_FLOOR) matchedOn += "name"
        if (a.phone != null && b.phone != null && a.phone == b.phone) matchedOn += "phone"
        if (a.email != null && b.email != null && a.email.equals(b.email, ignoreCase = true)) matchedOn += "email"
        if (matchedOn.isEmpty()) return null
        val score = if ("phone" in matchedOn || "email" in matchedOn) 100.0 else nameScore
        return DuplicateCandidate(a, b, score, matchedOn)
    }
}

/** Token-sort ratio approximation: sort words, then a normalized Levenshtein ratio, 0-100 scale. */
private fun nameRatio(a: String, b: String): Double {
    val sortedA = a.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.sorted().joinToString(" ")
    val sortedB = b.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.sorted().joinToString(" ")
    if (sortedA.isEmpty() && sortedB.isEmpty()) return 100.0
    val distance = levenshtein(sortedA, sortedB)
    val maxLen = max(sortedA.length, sortedB.length).coerceAtLeast(1)
    return (1.0 - distance.toDouble() / maxLen) * 100.0
}

private fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1]
            } else {
                1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
    }
    return dp[a.length][b.length]
}
