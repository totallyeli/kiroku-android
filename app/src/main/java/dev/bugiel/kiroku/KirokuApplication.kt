package dev.bugiel.kiroku

import android.app.Application
import dev.bugiel.kiroku.di.AppContainer
import dev.bugiel.kiroku.reminder.HabitReminderNotifications
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
        rescheduleHabitReminders()
    }

    fun rescheduleHabitReminders(onComplete: () -> Unit = {}) {
        applicationScope.launch {
            try {
                container.habitRepository.getActiveWithReminders().forEach {
                    container.habitReminderScheduler.schedule(it)
                }
            } finally {
                onComplete()
            }
        }
    }
}
