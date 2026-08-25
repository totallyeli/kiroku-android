package dev.bugiel.kiroku.domain.time

import dev.bugiel.kiroku.domain.TodoValidation
import dev.bugiel.kiroku.domain.model.Todo
import java.time.Instant
import java.time.ZoneId

object TodoReminderTime {
    fun triggerAtMillis(todo: Todo, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        val offsetMinutes = todo.reminderOffsetMinutes ?: return null
        return TodoValidation.deadlineMillis(todo.dueEpochDay, todo.dueTimeMinutes, zoneId) -
            offsetMinutes * MILLIS_PER_MINUTE
    }

    fun shouldSchedule(todo: Todo, nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean =
        !todo.isCompleted && triggerAtMillis(todo, zoneId)?.let { it > nowMillis } == true

    fun snoozeUntilMillis(nowMillis: Long): Long =
        Instant.ofEpochMilli(nowMillis).plusSeconds(SNOOZE_MINUTES * 60L).toEpochMilli()

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val SNOOZE_MINUTES = 10
}
