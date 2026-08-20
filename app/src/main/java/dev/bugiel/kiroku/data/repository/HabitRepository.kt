package dev.bugiel.kiroku.data.repository

import dev.bugiel.kiroku.data.local.dao.HabitDao
import dev.bugiel.kiroku.data.local.entity.HabitCompletionEntity
import dev.bugiel.kiroku.data.local.entity.toDomain
import dev.bugiel.kiroku.data.local.entity.toEntity
import dev.bugiel.kiroku.domain.StreakCalculator
import dev.bugiel.kiroku.domain.model.Habit
import dev.bugiel.kiroku.domain.model.HabitWithStatus
import dev.bugiel.kiroku.domain.model.isScheduledOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface HabitRepository {
    fun observeOverview(todayEpochDay: Long): Flow<List<HabitWithStatus>>
    fun observeHabit(id: Long): Flow<Habit?>
    fun observeCompletionDays(habitId: Long): Flow<Set<Long>>
    suspend fun getHabit(id: Long): Habit?
    suspend fun getActiveWithReminders(): List<Habit>
    suspend fun isCompleted(habitId: Long, epochDay: Long): Boolean
    suspend fun save(habit: Habit): Long
    suspend fun delete(habit: Habit)
    suspend fun setCompletion(habitId: Long, epochDay: Long, completed: Boolean, timestamp: Long)
}

class OfflineHabitRepository(
    private val dao: HabitDao,
) : HabitRepository {
    override fun observeOverview(todayEpochDay: Long): Flow<List<HabitWithStatus>> = combine(
        dao.observeActive(),
        dao.observeAllCompletions(),
    ) { habits, completions ->
        val byHabit = completions.groupBy { it.habitId }
        habits.map { entity ->
            val habit = entity.toDomain()
            val days = byHabit[entity.id].orEmpty().mapTo(mutableSetOf()) { it.epochDay }
            HabitWithStatus(
                habit = habit,
                isCompletedToday = todayEpochDay in days,
                stats = StreakCalculator.calculateScheduled(
                    completedEpochDays = days,
                    todayEpochDay = todayEpochDay,
                    createdEpochDay = habit.createdEpochDay,
                    isScheduledOn = habit::isScheduledOn,
                ),
                isScheduledToday = habit.isScheduledOn(todayEpochDay),
                isScheduledYesterday = habit.isScheduledOn(todayEpochDay - 1),
                isCompletedYesterday = todayEpochDay - 1 in days,
            )
        }
    }

    override fun observeHabit(id: Long): Flow<Habit?> = dao.observeById(id).map { it?.toDomain() }

    override fun observeCompletionDays(habitId: Long): Flow<Set<Long>> =
        dao.observeCompletionDays(habitId).map { it.toSet() }

    override suspend fun getHabit(id: Long): Habit? = dao.getById(id)?.toDomain()

    override suspend fun getActiveWithReminders(): List<Habit> =
        dao.getActiveWithReminders().map { it.toDomain() }

    override suspend fun isCompleted(habitId: Long, epochDay: Long): Boolean =
        dao.isCompleted(habitId, epochDay)

    override suspend fun save(habit: Habit): Long {
        return if (habit.id == 0L) {
            dao.insert(habit.toEntity())
        } else {
            dao.update(habit.toEntity())
            habit.id
        }
    }

    override suspend fun delete(habit: Habit) = dao.delete(habit.toEntity())

    override suspend fun setCompletion(
        habitId: Long,
        epochDay: Long,
        completed: Boolean,
        timestamp: Long,
    ) {
        if (completed) {
            dao.insertCompletion(
                HabitCompletionEntity(
                    habitId = habitId,
                    epochDay = epochDay,
                    completedAt = timestamp,
                ),
            )
        } else {
            dao.deleteCompletion(habitId, epochDay)
        }
    }
}
