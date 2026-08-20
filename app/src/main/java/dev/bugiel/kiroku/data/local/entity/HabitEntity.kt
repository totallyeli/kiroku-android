package dev.bugiel.kiroku.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import dev.bugiel.kiroku.domain.model.Habit

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconKey: String,
    val colorKey: String,
    val createdEpochDay: Long,
    val isActive: Boolean,
    val dueTimeMinutes: Int?,
    @ColumnInfo(defaultValue = "'daily'")
    val repeatType: String = "daily",
    @ColumnInfo(defaultValue = "1")
    val repeatIntervalDays: Int = 1,
    @ColumnInfo(defaultValue = "127")
    val repeatWeekdaysMask: Int = 127,
)

fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    description = description,
    iconKey = iconKey,
    colorKey = colorKey,
    createdEpochDay = createdEpochDay,
    isActive = isActive,
    dueTimeMinutes = dueTimeMinutes,
    repeatType = repeatType,
    repeatIntervalDays = repeatIntervalDays,
    repeatWeekdaysMask = repeatWeekdaysMask,
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    description = description,
    iconKey = iconKey,
    colorKey = colorKey,
    createdEpochDay = createdEpochDay,
    isActive = isActive,
    dueTimeMinutes = dueTimeMinutes,
    repeatType = repeatType,
    repeatIntervalDays = repeatIntervalDays,
    repeatWeekdaysMask = repeatWeekdaysMask,
)
