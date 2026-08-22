package dev.bugiel.kiroku.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.HabitRepository
import dev.bugiel.kiroku.domain.model.HabitWithStatus
import dev.bugiel.kiroku.domain.time.DateClock
import dev.bugiel.kiroku.domain.time.HabitHistoryDates
import dev.bugiel.kiroku.domain.time.TodayProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitHistoryState(
    val epochDay: Long,
    val habits: List<HabitWithStatus>,
)

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

    private val mutableHistoryEpochDay = MutableStateFlow<Long?>(null)
    val historyState: StateFlow<HabitHistoryState?> = mutableHistoryEpochDay
        .flatMapLatest { epochDay ->
            if (epochDay == null) {
                flowOf(null)
            } else {
                repository.observeOverview(epochDay)
                    .map<List<HabitWithStatus>, HabitHistoryState?> { HabitHistoryState(epochDay, it) }
                    .onStart { emit(null) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun selectHistoryDate(epochDay: Long?) {
        mutableHistoryEpochDay.value = HabitHistoryDates.pastDateOrNull(epochDay, todayEpochDay.value)
    }

    fun toggleHistory(item: HabitWithStatus) {
        val epochDay = mutableHistoryEpochDay.value ?: return
        if (epochDay >= todayEpochDay.value || (!item.isScheduledToday && !item.isCompletedToday)) return
        viewModelScope.launch {
            repository.setCompletion(
                habitId = item.habit.id,
                epochDay = epochDay,
                completed = !item.isCompletedToday,
                timestamp = dateClock.nowMillis(),
            )
        }
    }
}
