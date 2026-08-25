package dev.bugiel.kiroku

import android.app.Application
import dev.bugiel.kiroku.di.AppContainer
import dev.bugiel.kiroku.reminder.HabitReminderNotifications
import dev.bugiel.kiroku.reminder.TodoReminderNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KirokuApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        HabitReminderNotifications.createChannel(this)
        TodoReminderNotifications.createChannel(this)
        rescheduleReminders()
    }

    fun rescheduleReminders(onComplete: () -> Unit = {}) {
        applicationScope.launch {
            try {
                container.habitRepository.getActiveWithReminders().forEach {
                    container.habitReminderScheduler.schedule(it)
                }
                container.todoRepository.getIncompleteWithReminders().forEach {
                    container.todoReminderScheduler.schedule(it)
                }
            } finally {
                onComplete()
            }
        }
    }
}
