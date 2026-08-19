package dev.bugiel.kiroku.domain.time

import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class HabitReminderTimeTest {
    private val zone = ZoneId.of("Europe/Berlin")

    @Test
    fun `future reminder is scheduled for today`() {
        val now = ZonedDateTime.of(2026, 8, 20, 8, 15, 0, 0, zone)

        val reminder = HabitReminderTime.nextOccurrence(9 * 60, now)

        assertThat(reminder).isEqualTo(ZonedDateTime.of(2026, 8, 20, 9, 0, 0, 0, zone))
    }

    @Test
    fun `passed reminder is scheduled for tomorrow`() {
        val now = ZonedDateTime.of(2026, 8, 20, 9, 1, 0, 0, zone)

        val reminder = HabitReminderTime.nextOccurrence(9 * 60, now)

        assertThat(reminder).isEqualTo(ZonedDateTime.of(2026, 8, 21, 9, 0, 0, 0, zone))
    }

    @Test
    fun `reminder at the current minute is not repeated immediately`() {
        val now = ZonedDateTime.of(2026, 8, 20, 9, 0, 0, 0, zone)

        val reminder = HabitReminderTime.nextOccurrence(9 * 60, now)

        assertThat(reminder).isEqualTo(ZonedDateTime.of(2026, 8, 21, 9, 0, 0, 0, zone))
    }

    @Test
    fun `next reminder follows a daylight saving change`() {
        val now = ZonedDateTime.of(2026, 3, 28, 23, 0, 0, 0, zone)

        val reminder = HabitReminderTime.nextOccurrence(9 * 60, now)

        assertThat(reminder).isEqualTo(ZonedDateTime.of(2026, 3, 29, 9, 0, 0, 0, zone))
        assertThat(reminder.offset).isNotEqualTo(now.offset)
    }
}
