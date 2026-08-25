package dev.bugiel.kiroku.data.repository

import dev.bugiel.kiroku.data.local.dao.TodoDao
import dev.bugiel.kiroku.data.local.entity.toDomain
import dev.bugiel.kiroku.data.local.entity.toEntity
import dev.bugiel.kiroku.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TodoRepository {
    fun observeAll(): Flow<List<Todo>>
    fun observeTodo(id: Long): Flow<Todo?>
    suspend fun getTodo(id: Long): Todo?
    suspend fun getIncompleteWithReminders(): List<Todo>
    suspend fun save(todo: Todo): Long
    suspend fun delete(todo: Todo)
    suspend fun setCompleted(id: Long, completedAt: Long?, updatedAt: Long)
}

class OfflineTodoRepository(
    private val dao: TodoDao,
) : TodoRepository {
    override fun observeAll(): Flow<List<Todo>> = dao.observeAll().map { todos -> todos.map { it.toDomain() } }

    override fun observeTodo(id: Long): Flow<Todo?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun getTodo(id: Long): Todo? = dao.getById(id)?.toDomain()

    override suspend fun getIncompleteWithReminders(): List<Todo> =
        dao.getIncompleteWithReminders().map { it.toDomain() }

    override suspend fun save(todo: Todo): Long = if (todo.id == 0L) {
        dao.insert(todo.toEntity())
    } else {
        dao.update(todo.toEntity())
        todo.id
    }

    override suspend fun delete(todo: Todo) = dao.delete(todo.toEntity())

    override suspend fun setCompleted(id: Long, completedAt: Long?, updatedAt: Long) {
        dao.setCompleted(id, completedAt, updatedAt)
    }
}
