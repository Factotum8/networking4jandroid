package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.RelativeDao
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class StaleContact(val contact: ContactEntity, val lastInteractionDate: LocalDate?)

/** `kind` is "contact" or "relative" — which side's birthday this is. */
data class UpcomingBirthday(val name: String, val birthday: LocalDate, val daysAway: Long, val kind: String)

data class DueActionEntry(val contactId: String, val contactName: String, val action: ActionEntity)

data class ReminderDigest(
    val dueActions: List<DueActionEntry>,
    val upcomingBirthdays: List<UpcomingBirthday>,
    val staleContacts: List<StaleContact>,
)

private const val UPCOMING_BIRTHDAY_WINDOW_DAYS = 30L

/**
 * Port of `ReminderHandler.build_digest` + the stale/birthday queries from
 * `ContactRepository`/`LinkRepository`.
 */
class ReminderRepository @Inject constructor(
    private val contactRepository: ContactRepository,
    private val relativeDao: RelativeDao,
    private val interactionRepository: InteractionRepository,
    private val actionRepository: ActionRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun buildDigest(today: LocalDate = LocalDate.now()): ReminderDigest {
        val activeContacts = contactRepository.listOnce(archived = false)
        val contactsById = activeContacts.associateBy { it.id }

        val dueActions = actionRepository.listDue(before = today).mapNotNull { action ->
            contactsById[action.contactId]?.let { DueActionEntry(action.contactId, it.name, action) }
        }

        val upcomingBirthdays = buildUpcomingBirthdays(activeContacts, today)
        val staleContacts = buildStaleContacts(activeContacts, today)

        return ReminderDigest(dueActions, upcomingBirthdays, staleContacts)
    }

    private suspend fun buildUpcomingBirthdays(activeContacts: List<ContactEntity>, today: LocalDate): List<UpcomingBirthday> {
        val relatives = relativeDao.getAllOnce()
        val entries = mutableListOf<UpcomingBirthday>()
        activeContacts.forEach { contact ->
            contact.birthday?.let { entries += birthdayEntry(contact.name, it, today, "contact") }
        }
        relatives.forEach { relative ->
            relative.birthday?.let { entries += birthdayEntry(relative.name, it, today, "relative") }
        }
        return entries.filter { it.daysAway <= UPCOMING_BIRTHDAY_WINDOW_DAYS }.sortedBy { it.daysAway }
    }

    /** Next occurrence of [birthday]'s month/day, ignoring its year — this year if not yet past, else next year. */
    private fun birthdayEntry(name: String, birthday: LocalDate, today: LocalDate, kind: String): UpcomingBirthday {
        var next = occurrenceInYear(birthday, today.year)
        if (next.isBefore(today)) next = occurrenceInYear(birthday, today.year + 1)
        return UpcomingBirthday(name, birthday, ChronoUnit.DAYS.between(today, next), kind)
    }

    /** [birthday]'s month/day in [year], clamped to that month's last day — e.g. Feb 29 -> Feb 28 in a non-leap [year]. */
    private fun occurrenceInYear(birthday: LocalDate, year: Int): LocalDate {
        val day = minOf(birthday.dayOfMonth, birthday.month.length(java.time.Year.isLeap(year.toLong())))
        return LocalDate.of(year, birthday.month, day)
    }

    private suspend fun buildStaleContacts(activeContacts: List<ContactEntity>, today: LocalDate): List<StaleContact> {
        val settings = settingsRepository.get()
        val lastDates = interactionRepository.lastInteractionDatePerContact()
        return activeContacts
            .map { contact -> StaleContact(contact, lastDates[contact.id]) }
            .filter { stale ->
                val lastDate = stale.lastInteractionDate ?: return@filter true
                ChronoUnit.DAYS.between(lastDate, today) >= settings.staleContactDays
            }
            .sortedWith(compareBy(nullsFirst()) { it.lastInteractionDate })
    }
}
