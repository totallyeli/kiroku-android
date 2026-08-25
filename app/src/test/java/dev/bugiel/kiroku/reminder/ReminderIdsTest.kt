package dev.bugiel.kiroku.reminder

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReminderIdsTest {
    @Test
    fun `habit and todo identifiers use separate namespaces`() {
        val id = 42L
        val identifiers = listOf(
            ReminderIds.habitNotification(id),
            ReminderIds.todoNotification(id),
            ReminderIds.habitAlarm(id),
            ReminderIds.todoAlarm(id),
            ReminderIds.habitOpen(id),
            ReminderIds.todoOpen(id),
            ReminderIds.todoComplete(id),
            ReminderIds.todoSnoozeAction(id),
            ReminderIds.todoSnoozeAlarm(id),
        )

        assertThat(identifiers.toSet()).hasSize(identifiers.size)
    }
}
