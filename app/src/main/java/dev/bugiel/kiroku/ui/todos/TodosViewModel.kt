package dev.bugiel.kiroku.ui.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.TodoRepository
import dev.bugiel.kiroku.domain.TodoOrganizer
import dev.bugiel.kiroku.domain.TodoSections
import dev.bugiel.kiroku.domain.TodoStatusFilter
import dev.bugiel.kiroku.domain.model.Todo
import dev.bugiel.kiroku.domain.time.DateClock
import dev.bugiel.kiroku.reminder.TodoReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodosUiState(
    val sections: TodoSections = TodoSections(),
    val query: String = "",
    val filter: TodoStatusFilter = TodoStatusFilter.ALL,
)

sealed interface TodoEvent {
    data class Completed(val todoId: Long) : TodoEvent
}

class TodosViewModel(
    private val repository: TodoRepository,
    private val dateClock: DateClock,
    private val reminderScheduler: TodoReminderScheduler,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TodoStatusFilter.ALL)
    private val nowMillis = MutableStateFlow(dateClock.nowMillis())
    private val mutableEvents = MutableSharedFlow<TodoEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    val state: StateFlow<TodosUiState> = combine(
        repository.observeAll(),
        query,
        filter,
        nowMillis,
    ) { todos, currentQuery, currentFilter, now ->
        TodosUiState(
            sections = TodoOrganizer.organize(todos, currentQuery, currentFilter, now),
            query = currentQuery,
            filter = currentFilter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodosUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: TodoStatusFilter) {
        filter.value = value
    }

    fun refreshTime() {
        nowMillis.value = dateClock.nowMillis()
    }

    fun complete(todo: Todo) {
        if (todo.isCompleted) return
        viewModelScope.launch {
            val now = dateClock.nowMillis()
            repository.setCompleted(todo.id, now, now)
            reminderScheduler.cancel(todo.id)
            mutableEvents.emit(TodoEvent.Completed(todo.id))
        }
    }

    fun reopen(todo: Todo) {
        reopen(todo.id)
    }

    fun undoCompletion(todoId: Long) {
        reopen(todoId)
    }

    private fun reopen(todoId: Long) {
        viewModelScope.launch {
            val todo = repository.getTodo(todoId) ?: return@launch
            val now = dateClock.nowMillis()
            repository.setCompleted(todoId, null, now)
            reminderScheduler.schedule(todo.copy(completedAt = null, updatedAt = now))
        }
    }
}
