package dev.bugiel.kiroku.ui.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.TodoRepository
import dev.bugiel.kiroku.domain.TodoValidation
import dev.bugiel.kiroku.domain.model.HabitColorKey
import dev.bugiel.kiroku.domain.model.HabitIconKey
import dev.bugiel.kiroku.domain.model.Todo
import dev.bugiel.kiroku.domain.model.TodoPriority
import dev.bugiel.kiroku.domain.model.TodoReminderOffset
import dev.bugiel.kiroku.domain.time.DateClock
import dev.bugiel.kiroku.reminder.TodoReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TodoEditorState(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val iconKey: String = HabitIconKey.STAR,
    val colorKey: String = HabitColorKey.GREEN,
    val priority: String = TodoPriority.NONE,
    val dueEpochDay: Long? = null,
    val dueTimeMinutes: Int? = null,
    val reminderOffsetMinutes: Int? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val completedAt: Long? = null,
    val isLoading: Boolean = true,
    val showTitleError: Boolean = false,
    val showDueDateError: Boolean = false,
    val showDueTimeError: Boolean = false,
    val showPastDeadlineError: Boolean = false,
    val hasSaveError: Boolean = false,
)

class TodoEditorViewModel(
    todoId: Long,
    private val repository: TodoRepository,
    private val dateClock: DateClock,
    private val reminderScheduler: TodoReminderScheduler,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TodoEditorState())
    val state: StateFlow<TodoEditorState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val todo = todoId.takeIf { it != 0L }?.let { repository.getTodo(it) }
            mutableState.value = if (todo == null) {
                TodoEditorState(isLoading = false)
            } else {
                TodoEditorState(
                    id = todo.id,
                    title = todo.title,
                    description = todo.description,
                    iconKey = todo.iconKey,
                    colorKey = todo.colorKey,
                    priority = todo.priority,
                    dueEpochDay = todo.dueEpochDay,
                    dueTimeMinutes = todo.dueTimeMinutes,
                    reminderOffsetMinutes = todo.reminderOffsetMinutes,
                    createdAt = todo.createdAt,
                    updatedAt = todo.updatedAt,
                    completedAt = todo.completedAt,
                    isLoading = false,
                )
            }
        }
    }

    fun setTitle(value: String) = setState {
        copy(title = value, showTitleError = false, hasSaveError = false)
    }

    fun setDescription(value: String) = setState { copy(description = value, hasSaveError = false) }
    fun setIcon(key: String) = setState { copy(iconKey = key) }
    fun setColor(key: String) = setState { copy(colorKey = key) }
    fun setPriority(value: String) = setState { copy(priority = value) }
    fun setDueDate(epochDay: Long) = setState {
        copy(dueEpochDay = epochDay, showDueDateError = false, showPastDeadlineError = false)
    }
    fun setDueTime(minutes: Int) = setState {
        copy(dueTimeMinutes = minutes, showDueTimeError = false, showPastDeadlineError = false)
    }
    fun setReminderEnabled(enabled: Boolean) = setState {
        copy(reminderOffsetMinutes = if (enabled) reminderOffsetMinutes ?: TodoReminderOffset.AT_DUE_TIME else null)
    }
    fun setReminderOffset(minutes: Int) = setState { copy(reminderOffsetMinutes = minutes) }

    fun save(onSaved: (Long) -> Unit) {
        val snapshot = mutableState.value
        val missingTitle = snapshot.title.isBlank()
        val missingDate = snapshot.dueEpochDay == null
        val missingTime = snapshot.dueTimeMinutes == null
        val validDeadline = TodoValidation.isNewDeadlineValid(
            todoId = snapshot.id,
            dueEpochDay = snapshot.dueEpochDay,
            dueTimeMinutes = snapshot.dueTimeMinutes,
            nowMillis = dateClock.nowMillis(),
        )
        if (missingTitle || missingDate || missingTime || !validDeadline) {
            mutableState.value = snapshot.copy(
                showTitleError = missingTitle,
                showDueDateError = missingDate,
                showDueTimeError = missingTime,
                showPastDeadlineError = !missingDate && !missingTime && !validDeadline,
            )
            return
        }

        viewModelScope.launch {
            val now = dateClock.nowMillis()
            val todo = Todo(
                id = snapshot.id,
                title = snapshot.title.trim(),
                description = snapshot.description.trim(),
                iconKey = snapshot.iconKey,
                colorKey = snapshot.colorKey,
                priority = snapshot.priority,
                dueEpochDay = requireNotNull(snapshot.dueEpochDay),
                dueTimeMinutes = requireNotNull(snapshot.dueTimeMinutes),
                reminderOffsetMinutes = snapshot.reminderOffsetMinutes,
                createdAt = snapshot.createdAt.takeIf { it != 0L } ?: now,
                updatedAt = now,
                completedAt = snapshot.completedAt,
            )
            runCatching { repository.save(todo) }
                .onSuccess { id ->
                    reminderScheduler.cancel(id)
                    reminderScheduler.schedule(todo.copy(id = id))
                    onSaved(id)
                }
                .onFailure { mutableState.value = mutableState.value.copy(hasSaveError = true) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val snapshot = mutableState.value
            if (snapshot.id != 0L) {
                repository.getTodo(snapshot.id)?.let { repository.delete(it) }
                reminderScheduler.cancel(snapshot.id)
            }
            onDeleted()
        }
    }

    private fun setState(transform: TodoEditorState.() -> TodoEditorState) {
        if (!mutableState.value.isLoading) mutableState.value = mutableState.value.transform()
    }
}
