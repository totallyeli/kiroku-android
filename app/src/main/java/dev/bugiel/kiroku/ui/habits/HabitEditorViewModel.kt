package dev.bugiel.kiroku.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.HabitRepository
import dev.bugiel.kiroku.domain.model.Habit
import dev.bugiel.kiroku.domain.model.HabitColorKey
import dev.bugiel.kiroku.domain.model.HabitIconKey
import dev.bugiel.kiroku.domain.time.DateClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HabitEditorState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val iconKey: String = HabitIconKey.STAR,
    val colorKey: String = HabitColorKey.GREEN,
    val createdEpochDay: Long = 0,
    val isLoading: Boolean = true,
    val showNameError: Boolean = false,
    val hasSaveError: Boolean = false,
) {
    fun toHabit() = Habit(
        id = id,
        name = name.trim(),
        description = description.trim(),
        iconKey = iconKey,
        colorKey = colorKey,
        createdEpochDay = createdEpochDay,
    )
}

class HabitEditorViewModel(
    habitId: Long,
    private val repository: HabitRepository,
    private val dateClock: DateClock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HabitEditorState())
    val state: StateFlow<HabitEditorState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val habit = habitId.takeIf { it != 0L }?.let { repository.getHabit(it) }
            mutableState.value = if (habit == null) {
                HabitEditorState(createdEpochDay = dateClock.today().toEpochDay(), isLoading = false)
            } else {
                HabitEditorState(
                    id = habit.id,
                    name = habit.name,
                    description = habit.description,
                    iconKey = habit.iconKey,
                    colorKey = habit.colorKey,
                    createdEpochDay = habit.createdEpochDay,
                    isLoading = false,
                )
            }
        }
    }

    fun setName(value: String) = setState { copy(name = value, showNameError = false, hasSaveError = false) }
    fun setDescription(value: String) = setState { copy(description = value, hasSaveError = false) }
    fun setIcon(key: String) = setState { copy(iconKey = key) }
    fun setColor(key: String) = setState { copy(colorKey = key) }

    fun save(onSaved: (Long) -> Unit) {
        val snapshot = mutableState.value
        if (snapshot.name.isBlank()) {
            mutableState.value = snapshot.copy(showNameError = true)
            return
        }
        viewModelScope.launch {
            runCatching { repository.save(snapshot.toHabit()) }
                .onSuccess(onSaved)
                .onFailure { mutableState.value = mutableState.value.copy(hasSaveError = true) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val snapshot = mutableState.value
            if (snapshot.id != 0L) repository.delete(snapshot.toHabit())
            onDeleted()
        }
    }

    private fun setState(transform: HabitEditorState.() -> HabitEditorState) {
        if (!mutableState.value.isLoading) mutableState.value = mutableState.value.transform()
    }
}

