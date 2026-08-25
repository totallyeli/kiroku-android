package dev.bugiel.kiroku.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.bugiel.kiroku.KirokuApplication
import kotlinx.coroutines.launch

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, 0L)
        if (todoId == 0L) return

        val application = context.applicationContext as KirokuApplication
        val pendingResult = goAsync()
        application.applicationScope.launch {
            try {
                val todo = application.container.todoRepository.getTodo(todoId) ?: return@launch
                if (todo.isCompleted) {
                    application.container.todoReminderScheduler.cancel(todoId)
                    return@launch
                }
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        val now = application.container.dateClock.nowMillis()
                        application.container.todoRepository.setCompleted(todoId, now, now)
                        application.container.todoReminderScheduler.cancel(todoId)
                    }
                    ACTION_SNOOZE -> {
                        TodoReminderNotifications.cancel(context, todoId)
                        application.container.todoReminderScheduler.snooze(
                            todoId,
                            application.container.dateClock.nowMillis(),
                        )
                    }
                    else -> TodoReminderNotifications.show(context, todo)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TODO_ID = "dev.bugiel.kiroku.extra.TODO_ID"
        const val ACTION_SHOW = "dev.bugiel.kiroku.action.SHOW_TODO_REMINDER"
        const val ACTION_COMPLETE = "dev.bugiel.kiroku.action.COMPLETE_TODO"
        const val ACTION_SNOOZE = "dev.bugiel.kiroku.action.SNOOZE_TODO"
    }
}
