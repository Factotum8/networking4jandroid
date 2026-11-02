package com.factotum8.networking4jandroid.data.repository

import com.factotum8.networking4jandroid.data.local.dao.RelativeDao
import com.factotum8.networking4jandroid.data.local.entity.ActionEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.SettingsEntity
import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ReminderRepositoryTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var relativeDao: RelativeDao
    private lateinit var interactionRepository: InteractionRepository
    private lateinit var actionRepository: ActionRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: ReminderRepository

    private val today = LocalDate.of(2026, 6, 15)

    @Before
    fun setUp() {
        contactRepository = mockk()
        relativeDao = mockk()
        interactionRepository = mockk()
        actionRepository = mockk()
        settingsRepository = mockk()
        repository = ReminderRepository(contactRepository, relativeDao, interactionRepository, actionRepository, settingsRepository)

        coEvery { relativeDao.getAllOnce() } returns emptyList()
        coEvery { interactionRepository.lastInteractionDatePerContact() } returns emptyMap()
        coEvery { settingsRepository.get() } returns SettingsEntity(staleContactDays = 60, dedupScoreThreshold = 85.0)
        coEvery { actionRepository.listDue(before = today) } returns emptyList()
    }

    private fun contact(id: String, name: String, birthday: LocalDate? = null) =
        ContactEntity(id = id, name = name, birthday = birthday, archived = false)

    // --- dueActions ---

    @Test
    fun `dueActions only include actions whose contact is still active`() = runTest {
        val activeContact = contact("c1", "Active")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(activeContact)
        val actionForActive = ActionEntity(id = "a1", contactId = "c1", type = ActionType.CALL, dueDate = today)
        val actionForMissingContact = ActionEntity(id = "a2", contactId = "ghost", type = ActionType.WRITE, dueDate = today)
        coEvery { actionRepository.listDue(before = today) } returns listOf(actionForActive, actionForMissingContact)

        val digest = repository.buildDigest(today)

        assertEquals(1, digest.dueActions.size)
        assertEquals("a1", digest.dueActions[0].action.id)
        assertEquals("Active", digest.dueActions[0].contactName)
    }

    // --- upcomingBirthdays ---

    @Test
    fun `birthday today has daysAway zero and is included`() = runTest {
        val c = contact("c1", "Bday Today", birthday = LocalDate.of(1990, 6, 15))
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)

        val digest = repository.buildDigest(today)

        assertEquals(1, digest.upcomingBirthdays.size)
        assertEquals(0L, digest.upcomingBirthdays[0].daysAway)
        assertEquals("contact", digest.upcomingBirthdays[0].kind)
    }

    @Test
    fun `birthday more than 30 days away is excluded`() = runTest {
        val c = contact("c1", "Far Bday", birthday = LocalDate.of(1990, 7, 20)) // 35 days after today
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)

        val digest = repository.buildDigest(today)

        assertEquals(emptyList<Any>(), digest.upcomingBirthdays)
    }

    @Test
    fun `birthday exactly 30 days away is included (boundary)`() = runTest {
        val c = contact("c1", "Boundary Bday", birthday = LocalDate.of(1990, 7, 15)) // exactly 30 days after today
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)

        val digest = repository.buildDigest(today)

        assertEquals(1, digest.upcomingBirthdays.size)
        assertEquals(30L, digest.upcomingBirthdays[0].daysAway)
    }

    @Test
    fun `birthday that already passed this year rolls over to next year`() = runTest {
        // today is June 15; a June 1 birthday already passed this year, so the
        // next occurrence is ~351 days away (June 1 next year) -> excluded.
        val c = contact("c1", "Passed Bday", birthday = LocalDate.of(1990, 6, 1))
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)

        val digest = repository.buildDigest(today)

        assertEquals(emptyList<Any>(), digest.upcomingBirthdays)
    }

    @Test
    fun `birthdays union contacts and relatives and sort ascending by daysAway`() = runTest {
        val c = contact("c1", "Contact Bday", birthday = LocalDate.of(1990, 6, 20)) // 5 days away
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)
        val relative = RelativeEntity(id = "r1", contactId = "c1", name = "Relative Bday", birthday = LocalDate.of(1980, 6, 16)) // 1 day away
        coEvery { relativeDao.getAllOnce() } returns listOf(relative)

        val digest = repository.buildDigest(today)

        assertEquals(2, digest.upcomingBirthdays.size)
        assertEquals("relative", digest.upcomingBirthdays[0].kind)
        assertEquals(1L, digest.upcomingBirthdays[0].daysAway)
        assertEquals("contact", digest.upcomingBirthdays[1].kind)
        assertEquals(5L, digest.upcomingBirthdays[1].daysAway)
    }

    @Test
    fun `Feb 29 birthday does not crash in a non-leap year and clamps to Feb 28`() = runTest {
        // Regression test: birthdayEntry() used to call LocalDate.withYear() directly,
        // which throws DateTimeException for Feb 29 in a non-leap target year.
        val nonLeapToday = LocalDate.of(2025, 2, 20) // 2025 is not a leap year
        val c = contact("c1", "Leap Bday", birthday = LocalDate.of(1992, 2, 29))
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)
        coEvery { actionRepository.listDue(before = nonLeapToday) } returns emptyList()

        val digest = repository.buildDigest(nonLeapToday)

        assertEquals(1, digest.upcomingBirthdays.size)
        assertEquals(8L, digest.upcomingBirthdays[0].daysAway) // Feb 20 -> Feb 28 (clamped)
    }

    // --- staleContacts ---

    @Test
    fun `contact never interacted with is stale regardless of threshold`() = runTest {
        val c = contact("c1", "Never Contacted")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)
        coEvery { interactionRepository.lastInteractionDatePerContact() } returns emptyMap()

        val digest = repository.buildDigest(today)

        assertEquals(1, digest.staleContacts.size)
        assertNull(digest.staleContacts[0].lastInteractionDate)
    }

    @Test
    fun `contact just under the stale threshold is not stale`() = runTest {
        val c = contact("c1", "Recent")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)
        // staleContactDays = 60; 59 days ago must NOT be stale.
        coEvery { interactionRepository.lastInteractionDatePerContact() } returns mapOf("c1" to today.minusDays(59))

        val digest = repository.buildDigest(today)

        assertEquals(emptyList<Any>(), digest.staleContacts)
    }

    @Test
    fun `contact exactly at the stale threshold is stale (boundary, inclusive)`() = runTest {
        val c = contact("c1", "Boundary")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)
        coEvery { interactionRepository.lastInteractionDatePerContact() } returns mapOf("c1" to today.minusDays(60))

        val digest = repository.buildDigest(today)

        assertEquals(1, digest.staleContacts.size)
        assertEquals(today.minusDays(60), digest.staleContacts[0].lastInteractionDate)
    }

    @Test
    fun `stale contacts sort with never-contacted first then ascending by last date`() = runTest {
        val neverContacted = contact("c1", "Never")
        val veryStale = contact("c2", "VeryStale")
        val justStale = contact("c3", "JustStale")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(justStale, veryStale, neverContacted)
        coEvery { interactionRepository.lastInteractionDatePerContact() } returns mapOf(
            "c2" to today.minusDays(200),
            "c3" to today.minusDays(60),
        )

        val digest = repository.buildDigest(today)

        assertEquals(3, digest.staleContacts.size)
        assertEquals("c1", digest.staleContacts[0].contact.id) // null (never contacted) first
        assertEquals("c2", digest.staleContacts[1].contact.id) // oldest date next
        assertEquals("c3", digest.staleContacts[2].contact.id)
    }

    @Test
    fun `custom staleContactDays from settings is honored`() = runTest {
        val c = contact("c1", "ShortThreshold")
        coEvery { contactRepository.listOnce(archived = false) } returns listOf(c)
        coEvery { settingsRepository.get() } returns SettingsEntity(staleContactDays = 10, dedupScoreThreshold = 85.0)
        coEvery { interactionRepository.lastInteractionDatePerContact() } returns mapOf("c1" to today.minusDays(10))

        val digest = repository.buildDigest(today)

        assertEquals(1, digest.staleContacts.size)
    }
}
