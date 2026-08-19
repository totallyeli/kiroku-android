package dev.bugiel.kiroku.domain.time

import java.time.ZonedDateTime

object HabitReminderTime {
    fun nextOccurrence(dueTimeMinutes: Int, now: ZonedDateTime): ZonedDateTime {
        require(dueTimeMinutes in 0 until MINUTES_PER_DAY)

        val hour = dueTimeMinutes / 60
        val minute = dueTimeMinutes % 60
        val today = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
