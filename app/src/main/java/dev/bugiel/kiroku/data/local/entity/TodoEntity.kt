package dev.bugiel.kiroku.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.bugiel.kiroku.domain.model.Todo

@Entity(
    tableName = "todos",
    indices = [
        Index(value = ["completedAt"]),
        Index(value = ["dueEpochDay", "dueTimeMinutes"]),
    ],
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val iconKey: String,
    val colorKey: String,
    val priority: String,
    val dueEpochDay: Long,
    val dueTimeMinutes: Int,
    val reminderOffsetMinutes: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
)

fun TodoEntity.toDomain() = Todo(
    id = id,
    title = title,
    description = description,
    iconKey = iconKey,
    colorKey = colorKey,
    priority = priority,
    dueEpochDay = dueEpochDay,
    dueTimeMinutes = dueTimeMinutes,
    reminderOffsetMinutes = reminderOffsetMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
)

fun Todo.toEntity() = TodoEntity(
    id = id,
    title = title,
    description = description,
    iconKey = iconKey,
    colorKey = colorKey,
    priority = priority,
    dueEpochDay = dueEpochDay,
    dueTimeMinutes = dueTimeMinutes,
    reminderOffsetMinutes = reminderOffsetMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
)
