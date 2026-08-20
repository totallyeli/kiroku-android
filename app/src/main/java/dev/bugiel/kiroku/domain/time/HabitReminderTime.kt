package dev.bugiel.kiroku.domain.time

import java.time.ZonedDateTime

object HabitReminderTime {
    fun nextOccurrence(
        dueTimeMinutes: Int,
        now: ZonedDateTime,
        isScheduledOn: (Long) -> Boolean = { true },
    ): ZonedDateTime {
        require(dueTimeMinutes in 0 until MINUTES_PER_DAY)

        val hour = dueTimeMinutes / 60
        val minute = dueTimeMinutes % 60
        repeat(MAX_SEARCH_DAYS) { offset ->
            val date = now.toLocalDate().plusDays(offset.toLong())
            val candidate = date.atTime(hour, minute).atZone(now.zone)
            if (candidate.isAfter(now) && isScheduledOn(date.toEpochDay())) return candidate
        }
        error("No scheduled reminder occurrence was found.")
    }

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MAX_SEARCH_DAYS = 3_650
}
