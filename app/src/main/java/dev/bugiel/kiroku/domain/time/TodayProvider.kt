package dev.bugiel.kiroku.domain.time

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TodayProvider(
    private val dateClock: DateClock,
) {
    private val mutableToday = MutableStateFlow(dateClock.today().toEpochDay())
    val todayEpochDay: StateFlow<Long> = mutableToday.asStateFlow()

    fun refresh() {
        mutableToday.value = dateClock.today().toEpochDay()
    }
}

