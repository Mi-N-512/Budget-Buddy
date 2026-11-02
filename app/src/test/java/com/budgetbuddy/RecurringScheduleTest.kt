package com.budgetbuddy

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): RecurringScheduleTest
 * Parent class: Any; child classes: none; related classes: RecurringSchedule, RepeatFrequency, and JUnit.
 * What the class does: Verifies which repeat dates are due and that none are missed, doubled or invented.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class RecurringScheduleTest {
    private fun d(value: String) = LocalDate.parse(value)

    @Test
    fun monthlyFromThePastFillsEveryMissedMonthUpToToday() {
        val due = RecurringSchedule.dueDates(d("2026-08-01"), RepeatFrequency.MONTHLY, null, d("2026-11-02"))
        assertEquals(listOf(d("2026-09-01"), d("2026-10-01"), d("2026-11-01")), due)
    }

    @Test
    fun theOriginalDateIsNeverRepeated() {
        assertTrue(RecurringSchedule.dueDates(d("2026-11-02"), RepeatFrequency.WEEKLY, null, d("2026-11-02")).isEmpty())
    }

    @Test
    fun alreadyAddedDatesAreSkipped() {
        val due = RecurringSchedule.dueDates(d("2026-10-01"), RepeatFrequency.WEEKLY, d("2026-10-15"), d("2026-10-29"))
        assertEquals(listOf(d("2026-10-22"), d("2026-10-29")), due)
    }

    @Test
    fun endOfMonthDoesNotDriftAfterAShortMonth() {
        val due = RecurringSchedule.dueDates(d("2026-01-31"), RepeatFrequency.MONTHLY, null, d("2026-04-30"))
        assertEquals(listOf(d("2026-02-28"), d("2026-03-31"), d("2026-04-30")), due)
    }

    @Test
    fun futureStartsAndNoneNeverProduceDates() {
        assertTrue(RecurringSchedule.dueDates(d("2027-01-01"), RepeatFrequency.MONTHLY, null, d("2026-11-02")).isEmpty())
        assertTrue(RecurringSchedule.dueDates(d("2020-01-01"), RepeatFrequency.NONE, null, d("2026-11-02")).isEmpty())
    }

    @Test
    fun yearlyAndCatchUpCapAreRespected() {
        assertEquals(
            listOf(d("2025-03-10"), d("2026-03-10")),
            RecurringSchedule.dueDates(d("2024-03-10"), RepeatFrequency.YEARLY, null, d("2026-11-02"))
        )
        val huge = RecurringSchedule.dueDates(d("1990-01-01"), RepeatFrequency.WEEKLY, null, d("2026-11-02"))
        assertEquals(RecurringSchedule.MAX_OCCURRENCES_PER_CATCH_UP, huge.size)
    }

    @Test
    fun copyIdsAreStablePerDateAndUnknownStoredValuesMeanNone() {
        assertEquals("abc@2026-11-01", RecurringSchedule.copyId("abc", d("2026-11-01")))
        assertEquals(RepeatFrequency.NONE, RepeatFrequency.fromStored("FORTNIGHTLY"))
        assertEquals(RepeatFrequency.MONTHLY, RepeatFrequency.fromStored("MONTHLY"))
    }
}
// End of class: RecurringScheduleTest
