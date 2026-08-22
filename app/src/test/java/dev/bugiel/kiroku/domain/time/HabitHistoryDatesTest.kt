package dev.bugiel.kiroku.domain.time

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class HabitHistoryDatesTest {
    private val today = LocalDate.of(2026, 8, 23).toEpochDay()

    @Test
    fun `past date is accepted`() {
        assertThat(HabitHistoryDates.pastDateOrNull(today - 1, today)).isEqualTo(today - 1)
    }

    @Test
    fun `today and future dates are rejected`() {
        assertThat(HabitHistoryDates.pastDateOrNull(today, today)).isNull()
        assertThat(HabitHistoryDates.pastDateOrNull(today + 1, today)).isNull()
    }

    @Test
    fun `date picker UTC milliseconds preserve the epoch day`() {
        val epochDay = LocalDate.of(2024, 2, 29).toEpochDay()
        val utcMillis = HabitHistoryDates.utcMillisFromEpochDay(epochDay)

        assertThat(HabitHistoryDates.epochDayFromUtcMillis(utcMillis)).isEqualTo(epochDay)
    }
}
