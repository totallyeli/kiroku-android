package dev.bugiel.kiroku.domain

import com.google.common.truth.Truth.assertThat
import dev.bugiel.kiroku.domain.model.Todo
import dev.bugiel.kiroku.domain.model.TodoPriority
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class TodoOrganizerTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = ZonedDateTime.of(2026, 8, 26, 12, 0, 0, 0, zone)
    private val today = now.toLocalDate().toEpochDay()

    @Test
    fun `groups open todos by deadline`() {
        val sections = TodoOrganizer.organize(
            todos = listOf(
                todo(1, "Yesterday", today - 1, 10 * 60),
                todo(2, "Earlier today", today, 10 * 60),
                todo(3, "Later today", today, 13 * 60),
                todo(4, "Tomorrow", today + 1, 9 * 60),
                todo(5, "Upcoming", today + 2, 9 * 60),
            ),
            query = "",
            filter = TodoStatusFilter.OPEN,
            nowMillis = now.toInstant().toEpochMilli(),
            zoneId = zone,
        )

        assertThat(sections.overdue.map { it.id }).containsExactly(1L, 2L).inOrder()
        assertThat(sections.today.map { it.id }).containsExactly(3L)
        assertThat(sections.tomorrow.map { it.id }).containsExactly(4L)
        assertThat(sections.upcoming.map { it.id }).containsExactly(5L)
    }

    @Test
    fun `sorts by deadline then higher priority`() {
        val sections = TodoOrganizer.organize(
            todos = listOf(
                todo(1, "Low", today + 1, 10 * 60, TodoPriority.LOW),
                todo(2, "High", today + 1, 10 * 60, TodoPriority.HIGH),
                todo(3, "Earlier", today + 1, 9 * 60, TodoPriority.NONE),
            ),
            query = "",
            filter = TodoStatusFilter.OPEN,
            nowMillis = now.toInstant().toEpochMilli(),
            zoneId = zone,
        )

        assertThat(sections.tomorrow.map { it.id }).containsExactly(3L, 2L, 1L).inOrder()
    }

    @Test
    fun `searches title and description case insensitively`() {
        val todos = listOf(
            todo(1, "Buy milk", today + 1, 600),
            todo(2, "Errands", today + 1, 600, description = "Visit the BAKERY"),
            todo(3, "Call Alex", today + 1, 600),
        )

        val milk = TodoOrganizer.organize(todos, "MILK", TodoStatusFilter.ALL, now.toInstant().toEpochMilli(), zone)
        val bakery = TodoOrganizer.organize(todos, "bakery", TodoStatusFilter.ALL, now.toInstant().toEpochMilli(), zone)

        assertThat(milk.tomorrow.map { it.id }).containsExactly(1L)
        assertThat(bakery.tomorrow.map { it.id }).containsExactly(2L)
    }

    @Test
    fun `status filters separate open and completed todos`() {
        val open = todo(1, "Open", today + 1, 600)
        val completed = todo(2, "Done", today + 1, 600).copy(completedAt = now.toInstant().toEpochMilli())

        val openOnly = TodoOrganizer.organize(listOf(open, completed), "", TodoStatusFilter.OPEN, now.toInstant().toEpochMilli(), zone)
        val completedOnly = TodoOrganizer.organize(listOf(open, completed), "", TodoStatusFilter.COMPLETED, now.toInstant().toEpochMilli(), zone)
        val all = TodoOrganizer.organize(listOf(open, completed), "", TodoStatusFilter.ALL, now.toInstant().toEpochMilli(), zone)

        assertThat(openOnly.openCount).isEqualTo(1)
        assertThat(openOnly.completed).isEmpty()
        assertThat(completedOnly.openCount).isEqualTo(0)
        assertThat(completedOnly.completed.map { it.id }).containsExactly(2L)
        assertThat(all.openCount).isEqualTo(1)
        assertThat(all.completed).hasSize(1)
    }

    @Test
    fun `reopened todo returns to its open deadline section`() {
        val completed = todo(1, "Done", today + 1, 600).copy(completedAt = now.toInstant().toEpochMilli())
        val reopened = completed.copy(completedAt = null)

        val before = TodoOrganizer.organize(listOf(completed), "", TodoStatusFilter.ALL, now.toInstant().toEpochMilli(), zone)
        val after = TodoOrganizer.organize(listOf(reopened), "", TodoStatusFilter.ALL, now.toInstant().toEpochMilli(), zone)

        assertThat(before.completed).hasSize(1)
        assertThat(after.completed).isEmpty()
        assertThat(after.tomorrow).hasSize(1)
    }

    private fun todo(
        id: Long,
        title: String,
        dueEpochDay: Long,
        dueTimeMinutes: Int,
        priority: String = TodoPriority.NONE,
        description: String = "",
    ) = Todo(
        id = id,
        title = title,
        description = description,
        dueEpochDay = dueEpochDay,
        dueTimeMinutes = dueTimeMinutes,
        priority = priority,
        createdAt = 1,
        updatedAt = 1,
    )
}
