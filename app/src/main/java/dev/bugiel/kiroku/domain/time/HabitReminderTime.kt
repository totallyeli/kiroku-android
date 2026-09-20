package dev.bugiel.kiroku.domain.time

import java.time.ZonedDateTime

object HabitReminderTime {
    /**
     * Returns null when no occurrence exists within the search window (e.g. an invalid
     * due time or a habit whose weekday mask selects no days), so callers can skip
     * scheduling instead of crashing on corrupted data restored from a backup.
     */
    fun nextOccurrence(
        dueTimeMinutes: Int,
        now: ZonedDateTime,
        isScheduledOn: (Long) -> Boolean = { true },
    ): ZonedDateTime? {
        if (dueTimeMinutes !in 0 until MINUTES_PER_DAY) return null

        val hour = dueTimeMinutes / 60
        val minute = dueTimeMinutes % 60
        repeat(MAX_SEARCH_DAYS) { offset ->
            val date = now.toLocalDate().plusDays(offset.toLong())
            val candidate = date.atTime(hour, minute).atZone(now.zone)
            if (candidate.isAfter(now) && isScheduledOn(date.toEpochDay())) return candidate
        }
        return null
    }

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MAX_SEARCH_DAYS = 3_650
}
