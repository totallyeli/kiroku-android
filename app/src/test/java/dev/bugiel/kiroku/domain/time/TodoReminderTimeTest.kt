package dev.bugiel.kiroku.domain.time

import com.google.common.truth.Truth.assertThat
import dev.bugiel.kiroku.domain.model.Todo
import dev.bugiel.kiroku.domain.model.TodoReminderOffset
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class TodoReminderTimeTest {
    private val zone = ZoneId.of("Europe/Berlin")

    @Test
    fun `reminder offset can cross into previous date`() {
        val due = ZonedDateTime.of(2026, 8, 27, 0, 5, 0, 0, zone)
        val todo = todo(due, TodoReminderOffset.FIFTEEN_MINUTES)

        val trigger = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(requireNotNull(TodoReminderTime.triggerAtMillis(todo, zone))),
            zone,
        )

        assertThat(trigger).isEqualTo(ZonedDateTime.of(2026, 8, 26, 23, 50, 0, 0, zone))
    }

    @Test
    fun `reminder calculation handles daylight saving transition`() {
        val due = ZonedDateTime.of(2026, 3, 29, 3, 30, 0, 0, zone)
        val todo = todo(due, TodoReminderOffset.ONE_HOUR)

        val trigger = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(requireNotNull(TodoReminderTime.triggerAtMillis(todo, zone))),
            zone,
        )

        assertThat(trigger).isEqualTo(ZonedDateTime.of(2026, 3, 29, 1, 30, 0, 0, zone))
    }

    @Test
    fun `scheduling rejects completed disabled and past reminders`() {
        val now = ZonedDateTime.of(2026, 8, 26, 12, 0, 0, 0, zone)
        val future = todo(now.plusHours(2), TodoReminderOffset.AT_DUE_TIME)
        val past = todo(now.minusMinutes(1), TodoReminderOffset.AT_DUE_TIME)

        assertThat(TodoReminderTime.shouldSchedule(future, now.toInstant().toEpochMilli(), zone)).isTrue()
        assertThat(TodoReminderTime.shouldSchedule(past, now.toInstant().toEpochMilli(), zone)).isFalse()
        assertThat(TodoReminderTime.shouldSchedule(future.copy(completedAt = 1), now.toInstant().toEpochMilli(), zone)).isFalse()
        assertThat(TodoReminderTime.shouldSchedule(future.copy(reminderOffsetMinutes = null), now.toInstant().toEpochMilli(), zone)).isFalse()
    }

    @Test
    fun `snooze schedules exactly ten minutes later`() {
        val now = ZonedDateTime.of(2026, 8, 26, 23, 58, 0, 0, zone).toInstant().toEpochMilli()

        assertThat(TodoReminderTime.snoozeUntilMillis(now)).isEqualTo(now + 10 * 60_000L)
    }

    private fun todo(due: ZonedDateTime, offset: Int) = Todo(
        id = 1,
        title = "Deadline",
        dueEpochDay = due.toLocalDate().toEpochDay(),
        dueTimeMinutes = due.hour * 60 + due.minute,
        reminderOffsetMinutes = offset,
        createdAt = 1,
        updatedAt = 1,
    )
}
