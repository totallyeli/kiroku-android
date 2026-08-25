package dev.bugiel.kiroku.domain

import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class TodoValidationTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = ZonedDateTime.of(2026, 8, 26, 12, 0, 0, 0, zone)

    @Test
    fun `new todo requires a future deadline`() {
        val day = now.toLocalDate().toEpochDay()

        assertThat(TodoValidation.isNewDeadlineValid(0, day, 12 * 60 - 1, now.toInstant().toEpochMilli(), zone)).isFalse()
        assertThat(TodoValidation.isNewDeadlineValid(0, day, 12 * 60, now.toInstant().toEpochMilli(), zone)).isFalse()
        assertThat(TodoValidation.isNewDeadlineValid(0, day, 12 * 60 + 1, now.toInstant().toEpochMilli(), zone)).isTrue()
    }

    @Test
    fun `existing overdue todo remains valid for editing`() {
        assertThat(
            TodoValidation.isNewDeadlineValid(
                todoId = 7,
                dueEpochDay = now.toLocalDate().minusDays(3).toEpochDay(),
                dueTimeMinutes = 9 * 60,
                nowMillis = now.toInstant().toEpochMilli(),
                zoneId = zone,
            ),
        ).isTrue()
    }

    @Test
    fun `missing date or time is invalid`() {
        assertThat(TodoValidation.isNewDeadlineValid(0, null, 600, now.toInstant().toEpochMilli(), zone)).isFalse()
        assertThat(TodoValidation.isNewDeadlineValid(0, now.toLocalDate().toEpochDay(), null, now.toInstant().toEpochMilli(), zone)).isFalse()
    }
}
