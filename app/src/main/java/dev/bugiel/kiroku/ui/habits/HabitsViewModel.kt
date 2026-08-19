package dev.bugiel.kiroku.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.HabitRepository
import dev.bugiel.kiroku.domain.model.HabitWithStatus
import dev.bugiel.kiroku.domain.time.DateClock
import dev.bugiel.kiroku.domain.time.TodayProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(
    private val repository: HabitRepository,
    private val todayProvider: TodayProvider,
    private val dateClock: DateClock,
) : ViewModel() {
    val todayEpochDay = todayProvider.todayEpochDay

    val habits: StateFlow<List<HabitWithStatus>> = todayEpochDay
        .flatMapLatest(repository::observeOverview)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleToday(item: HabitWithStatus) {
        viewModelScope.launch {
            repository.setCompletion(
                habitId = item.habit.id,
                epochDay = todayEpochDay.value,
                completed = !item.isCompletedToday,
                timestamp = dateClock.nowMillis(),
            )
        }
    }
}

