package com.budgetbuddy

import java.time.LocalDate

/*
 * Start of class
 * Name of class and related classes (parent/child classes): RepeatFrequency
 * Parent class: Enum; child classes: none; related classes: Transaction, RecurringSchedule, LocalDataStore, and TransactionActivity.
 * What the class does: Names how often a repeating transaction comes round again.
 * What's important to other classes, if applicable: The enum names are persisted, so existing names must never be renamed.
 * Code with comments begins below.
 */
enum class RepeatFrequency {
    NONE, WEEKLY, MONTHLY, YEARLY;

    /** The date of the nth occurrence after [start]; always counted from the original start so the 31st never drifts. */
    fun occurrence(start: LocalDate, n: Long): LocalDate = when (this) {
        NONE -> start
        WEEKLY -> start.plusWeeks(n)
        MONTHLY -> start.plusMonths(n)
        YEARLY -> start.plusYears(n)
    }

    companion object {
        fun fromStored(value: String?): RepeatFrequency =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}
// End of class: RepeatFrequency

/*
 * Start of class
 * Name of class and related classes (parent/child classes): RecurringSchedule
 * Parent class: Any; child classes: none; related classes: RepeatFrequency, LocalDataStore, and Transaction.
 * What the class does: Works out which repeat dates of a transaction are due and not yet recorded.
 * What's important to other classes, if applicable: It is pure and deterministic; LocalDataStore relies on it never returning a date twice or a future date.
 * Code with comments begins below.
 */
object RecurringSchedule {
    /** Upper bound per catch-up so a very old start date can never flood storage. */
    const val MAX_OCCURRENCES_PER_CATCH_UP = 400

    /**
     * Dates after [start] (and after [generatedThrough], when set) up to and including [today].
     * The original transaction itself is never included.
     */
    fun dueDates(
        start: LocalDate,
        frequency: RepeatFrequency,
        generatedThrough: LocalDate?,
        today: LocalDate
    ): List<LocalDate> {
        if (frequency == RepeatFrequency.NONE || start.isAfter(today)) return emptyList()
        val after = listOfNotNull(start, generatedThrough).max()
        val dates = mutableListOf<LocalDate>()
        var n = 1L
        while (dates.size < MAX_OCCURRENCES_PER_CATCH_UP) {
            val next = frequency.occurrence(start, n)
            if (next.isAfter(today)) break
            if (next.isAfter(after)) dates += next
            n++
        }
        return dates
    }

    /** Stable id for one generated copy, so the same date can never be added twice. */
    fun copyId(sourceId: String, date: LocalDate): String = "$sourceId@$date"
}
// End of class: RecurringSchedule
