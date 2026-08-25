package dev.bugiel.kiroku.domain

import dev.bugiel.kiroku.domain.model.Todo
import dev.bugiel.kiroku.domain.model.TodoPriority
import java.time.Instant
import java.time.ZoneId

enum class TodoStatusFilter { OPEN, COMPLETED, ALL }

data class TodoSections(
    val overdue: List<Todo> = emptyList(),
    val today: List<Todo> = emptyList(),
    val tomorrow: List<Todo> = emptyList(),
    val upcoming: List<Todo> = emptyList(),
    val completed: List<Todo> = emptyList(),
) {
    val openCount: Int get() = overdue.size + today.size + tomorrow.size + upcoming.size
    val isEmpty: Boolean get() = openCount == 0 && completed.isEmpty()
}

object TodoOrganizer {
    fun organize(
        todos: Iterable<Todo>,
        query: String,
        filter: TodoStatusFilter,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): TodoSections {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val todayEpochDay = now.toLocalDate().toEpochDay()
        val currentMinutes = now.hour * 60 + now.minute
        val normalizedQuery = query.trim()
        val matching = todos.filter { todo ->
            normalizedQuery.isEmpty() ||
                todo.title.contains(normalizedQuery, ignoreCase = true) ||
                todo.description.contains(normalizedQuery, ignoreCase = true)
        }
        val open = if (filter == TodoStatusFilter.COMPLETED) {
            emptyList()
        } else {
            matching.filterNot(Todo::isCompleted).sortedWith(openComparator)
        }
        val completed = if (filter == TodoStatusFilter.OPEN) {
            emptyList()
        } else {
            matching.filter(Todo::isCompleted).sortedWith(
                compareByDescending<Todo> { it.completedAt ?: Long.MIN_VALUE }
                    .thenByDescending { it.updatedAt },
            )
        }

        return TodoSections(
            overdue = open.filter { todo ->
                todo.dueEpochDay < todayEpochDay ||
                    (todo.dueEpochDay == todayEpochDay && todo.dueTimeMinutes < currentMinutes)
            },
            today = open.filter { todo ->
                todo.dueEpochDay == todayEpochDay && todo.dueTimeMinutes >= currentMinutes
            },
            tomorrow = open.filter { it.dueEpochDay == todayEpochDay + 1 },
            upcoming = open.filter { it.dueEpochDay > todayEpochDay + 1 },
            completed = completed,
        )
    }

    private val openComparator = compareBy<Todo>({ it.dueEpochDay }, { it.dueTimeMinutes })
        .thenByDescending { TodoPriority.rank(it.priority) }
        .thenBy { it.title.lowercase() }
        .thenBy { it.id }
}
