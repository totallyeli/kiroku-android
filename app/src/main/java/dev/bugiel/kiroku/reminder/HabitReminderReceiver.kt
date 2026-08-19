package dev.bugiel.kiroku.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.bugiel.kiroku.KirokuApplication
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
        if (habitId == 0L) return

        val application = context.applicationContext as KirokuApplication
        val pendingResult = goAsync()
        application.applicationScope.launch {
            try {
                val habit = application.container.habitRepository.getHabit(habitId) ?: return@launch
                if (!habit.isActive || habit.dueTimeMinutes == null) {
                    application.container.habitReminderScheduler.cancel(habitId)
                    return@launch
                }

                val today = application.container.dateClock.today().toEpochDay()
                if (!application.container.habitRepository.isCompleted(habitId, today)) {
                    HabitReminderNotifications.show(context, habit)
                }
                application.container.habitReminderScheduler.schedule(habit)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "dev.bugiel.kiroku.extra.HABIT_ID"
    }
}
