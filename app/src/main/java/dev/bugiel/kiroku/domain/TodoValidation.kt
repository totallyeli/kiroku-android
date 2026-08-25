package dev.bugiel.kiroku.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TodoValidation {
    fun deadlineMillis(dueEpochDay: Long, dueTimeMinutes: Int, zoneId: ZoneId): Long {
        require(dueTimeMinutes in 0 until MINUTES_PER_DAY)
        return LocalDate.ofEpochDay(dueEpochDay)
            .atTime(dueTimeMinutes / 60, dueTimeMinutes % 60)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun isNewDeadlineValid(
        todoId: Long,
        dueEpochDay: Long?,
        dueTimeMinutes: Int?,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (dueEpochDay == null || dueTimeMinutes == null) return false
        if (todoId != 0L) return true
        return deadlineMillis(dueEpochDay, dueTimeMinutes, zoneId) > Instant.ofEpochMilli(nowMillis).toEpochMilli()
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
