package dev.bugiel.kiroku.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class HabitScheduleTest {
    private val monday = LocalDate.of(2026, 8, 17).toEpochDay()

    @Test
    fun `daily habits remain scheduled every day`() {
        val habit = habit(createdEpochDay = monday)

        assertThat(habit.isScheduledOn(monday)).isTrue()
        assertThat(habit.isScheduledOn(monday + 1)).isTrue()
        assertThat(habit.isScheduledOn(monday + 30)).isTrue()
    }

    @Test
    fun `weekly habits only appear on selected weekdays`() {
        val habit = habit(
            createdEpochDay = monday,
            repeatType = HabitRepeatType.WEEKLY,
            repeatWeekdaysMask = HabitWeekdays.MONDAY or HabitWeekdays.WEDNESDAY,
        )

        assertThat(habit.isScheduledOn(monday)).isTrue()
        assertThat(habit.isScheduledOn(monday + 1)).isFalse()
        assertThat(habit.isScheduledOn(monday + 2)).isTrue()
        assertThat(habit.isScheduledOn(monday + 7)).isTrue()
    }

    @Test
    fun `interval habits are anchored to their creation day`() {
        val habit = habit(
            createdEpochDay = monday,
            repeatType = HabitRepeatType.INTERVAL,
            repeatIntervalDays = 2,
        )

        assertThat(habit.isScheduledOn(monday)).isTrue()
        assertThat(habit.isScheduledOn(monday + 1)).isFalse()
        assertThat(habit.isScheduledOn(monday + 2)).isTrue()
        assertThat(habit.isScheduledOn(monday + 4)).isTrue()
    }

    @Test
    fun `habits are never scheduled before creation or while inactive`() {
        assertThat(habit(createdEpochDay = monday).isScheduledOn(monday - 1)).isFalse()
        assertThat(habit(createdEpochDay = monday, isActive = false).isScheduledOn(monday)).isFalse()
    }

    private fun habit(
        createdEpochDay: Long,
        repeatType: String = HabitRepeatType.DAILY,
        repeatIntervalDays: Int = 1,
        repeatWeekdaysMask: Int = HabitWeekdays.ALL,
        isActive: Boolean = true,
    ) = Habit(
        name = "Read",
        createdEpochDay = createdEpochDay,
        repeatType = repeatType,
        repeatIntervalDays = repeatIntervalDays,
        repeatWeekdaysMask = repeatWeekdaysMask,
        isActive = isActive,
    )
}
