package dev.bugiel.kiroku.reminder

object ReminderIds {
    fun habitNotification(id: Long): Int = namespace(id, HABIT_NOTIFICATION)
    fun todoNotification(id: Long): Int = namespace(id, TODO_NOTIFICATION)
    fun habitAlarm(id: Long): Int = namespace(id, HABIT_ALARM)
    fun todoAlarm(id: Long): Int = namespace(id, TODO_ALARM)
    fun habitOpen(id: Long): Int = namespace(id, HABIT_OPEN)
    fun todoOpen(id: Long): Int = namespace(id, TODO_OPEN)
    fun todoComplete(id: Long): Int = namespace(id, TODO_COMPLETE)
    fun todoSnoozeAction(id: Long): Int = namespace(id, TODO_SNOOZE_ACTION)
    fun todoSnoozeAlarm(id: Long): Int = namespace(id, TODO_SNOOZE_ALARM)

    private fun namespace(id: Long, prefix: Int): Int =
        prefix or ((id xor (id ushr 32)).toInt() and ID_MASK)

    private const val ID_MASK = 0x00ff_ffff
    private const val HABIT_NOTIFICATION = 0x1100_0000
    private const val TODO_NOTIFICATION = 0x2200_0000
    private const val HABIT_ALARM = 0x3300_0000
    private const val TODO_ALARM = 0x4400_0000
    private const val HABIT_OPEN = 0x5500_0000
    private const val TODO_OPEN = 0x6600_0000
    private const val TODO_COMPLETE = 0x7700_0000
    private const val TODO_SNOOZE_ACTION = 0x0800_0000
    private const val TODO_SNOOZE_ALARM = 0x0900_0000
}
