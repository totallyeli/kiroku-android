package dev.bugiel.kiroku.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.HabitRepository
import dev.bugiel.kiroku.domain.StreakCalculator
import dev.bugiel.kiroku.domain.model.Habit
import dev.bugiel.kiroku.domain.model.HabitStats
import dev.bugiel.kiroku.domain.model.isScheduledOn
import dev.bugiel.kiroku.domain.time.DateClock
import dev.bugiel.kiroku.domain.time.TodayProvider
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitDetailState(
    val habit: Habit? = null,
    val completedDays: Set<Long> = emptySet(),
    val stats: HabitStats = HabitStats(),
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
    val visibleMonth: YearMonth = YearMonth.now(),
)

class HabitDetailViewModel(
    private val habitId: Long,
    private val repository: HabitRepository,
    private val todayProvider: TodayProvider,
    private val dateClock: DateClock,
) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.from(dateClock.today()))

    val state: StateFlow<HabitDetailState> = combine(
        repository.observeHabit(habitId),
        repository.observeCompletionDays(habitId),
        todayProvider.todayEpochDay,
        visibleMonth,
    ) { habit, days, today, month ->
        HabitDetailState(
            habit = habit,
            completedDays = days,
            stats = habit?.let {
                StreakCalculator.calculateScheduled(
                    completedEpochDays = days,
                    todayEpochDay = today,
                    createdEpochDay = it.createdEpochDay,
                    isScheduledOn = it::isScheduledOn,
                )
            } ?: HabitStats(),
            todayEpochDay = today,
            visibleMonth = month,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailState())

    fun previousMonth() {
        visibleMonth.value = visibleMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        val current = YearMonth.from(LocalDate.ofEpochDay(todayProvider.todayEpochDay.value))
        if (visibleMonth.value < current) visibleMonth.value = visibleMonth.value.plusMonths(1)
    }

    fun toggleDate(epochDay: Long) {
        val snapshot = state.value
        val habit = snapshot.habit ?: return
        if (
            epochDay > snapshot.todayEpochDay ||
            epochDay < habit.createdEpochDay ||
            (!habit.isScheduledOn(epochDay) && epochDay !in snapshot.completedDays)
        ) return
        viewModelScope.launch {
            repository.setCompletion(
                habitId = habitId,
                epochDay = epochDay,
                completed = epochDay !in snapshot.completedDays,
                timestamp = dateClock.nowMillis(),
            )
        }
    }
}
