package dev.bugiel.kiroku.data.local.entity

import com.google.common.truth.Truth.assertThat
import dev.bugiel.kiroku.domain.model.HabitColorKey
import dev.bugiel.kiroku.domain.model.HabitIconKey
import dev.bugiel.kiroku.domain.model.Todo
import dev.bugiel.kiroku.domain.model.TodoPriority
import org.junit.Test

class TodoEntityTest {
    @Test
    fun `entity mapping preserves every todo field`() {
        val todo = Todo(
            id = 42,
            title = "Submit report",
            description = "Attach the figures",
            iconKey = HabitIconKey.WORK,
            colorKey = HabitColorKey.BLUE,
            priority = TodoPriority.HIGH,
            dueEpochDay = 20_000,
            dueTimeMinutes = 17 * 60 + 45,
            reminderOffsetMinutes = 30,
            createdAt = 100,
            updatedAt = 200,
            completedAt = 300,
        )

        assertThat(todo.toEntity().toDomain()).isEqualTo(todo)
    }
}
