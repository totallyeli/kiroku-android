package dev.bugiel.kiroku.data.repository

import com.google.common.truth.Truth.assertThat
import dev.bugiel.kiroku.data.local.dao.WorkoutDao
import dev.bugiel.kiroku.data.local.entity.*
import dev.bugiel.kiroku.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkoutRepositoryTest {
    private val dao = MemoryWorkoutDao()
    private val repository = OfflineWorkoutRepository(dao)

    @Test fun `starting the same plan and day resumes without replacing saved sets`() = runBlocking {
        repository.addExercise(1, "Bench press")
        val id = repository.startSession(1, 20000, 123)
        repository.saveSets(dao.entries.single().id, listOf(WorkoutSet(80.0, 8)))
        val resumed = repository.startSession(1, 20000, 456)
        assertThat(resumed).isEqualTo(id)
        assertThat(dao.sessions).hasSize(1)
        assertThat(dao.sets.single().weightKg).isEqualTo(80.0)
        assertThat(repository.startSession(1, 20001, 789)).isNotEqualTo(id)
    }

    @Test fun `routine edits affect new days while earlier days keep their exercise snapshot`() = runBlocking {
        val bench = repository.addExercise(1, "Bench press")
        repository.startSession(1, 20000, 123)
        repository.renameExercise(bench, "Incline bench")
        val fly = repository.addExercise(1, "Fly")
        repository.archiveExercise(fly, true)
        repository.startSession(1, 20001, 456)
        assertThat(dao.entries.map { it.name }).containsExactly("Bench press", "Incline bench").inOrder()
        repository.archiveExercise(fly, false)
        repository.startSession(1, 20002, 789)
        assertThat(dao.entries.takeLast(2).map { it.name }).containsExactly("Incline bench", "Fly").inOrder()
    }

    @Test fun `empty routine cannot create an empty workout day`() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.startSession(1, 20000, 123) } }
        assertThat(dao.sessions).isEmpty()
    }

    @Test fun `invalid replacement never removes previously saved sets`() = runBlocking {
        repository.addExercise(1, "Bench")
        repository.startSession(1, 20000, 123)
        val entryId = dao.entries.single().id
        repository.saveSets(entryId, listOf(WorkoutSet(80.0, 8)))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.saveSets(entryId, listOf(WorkoutSet(-10.0, 0))) }
        }
        assertThat(repository.getEntry(entryId)?.sets).containsExactly(WorkoutSet(80.0, 8))
        repository.saveSets(entryId, listOf(WorkoutSet(60.0, 12), WorkoutSet(70.0, 10)))
        assertThat(repository.getEntry(entryId)?.sets).containsExactly(WorkoutSet(60.0, 12), WorkoutSet(70.0, 10)).inOrder()
        repository.saveSets(entryId, emptyList())
        assertThat(repository.getEntry(entryId)?.sets).isEmpty()
    }

    @Test fun `plan and exercise names are trimmed and blank names rejected`() = runBlocking {
        repository.createPlan(" Push ", 123)
        repository.addExercise(1, " Bench ")
        assertThat(dao.plans.single().name).isEqualTo("Push")
        assertThat(dao.exerciseRows.single().name).isEqualTo("Bench")
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.renamePlan(1, "  ") } }
        assertThat(dao.plans.single().name).isEqualTo("Push")
    }
}

// Exercises the real repository and DAO orchestration; SQLite constraints are checked separately.
private class MemoryWorkoutDao : WorkoutDao() {
    val plans = mutableListOf<WorkoutPlanEntity>()
    val exerciseRows = mutableListOf<WorkoutExerciseEntity>()
    val sessions = mutableListOf<WorkoutSessionEntity>()
    val entries = mutableListOf<WorkoutSessionExerciseEntity>()
    val sets = mutableListOf<WorkoutSetEntity>()
    private var nextId = 1L

    override fun observePlans(): Flow<List<WorkoutPlanWithHistory>> = flowOf(emptyList())
    override suspend fun insertPlan(plan: WorkoutPlanEntity): Long = nextId++.also { plans += plan.copy(id = it) }
    override suspend fun renamePlan(id: Long, name: String) { plans.replaceAll { if (it.id == id) it.copy(name = name) else it } }
    override suspend fun deletePlan(id: Long) { plans.removeAll { it.id == id } }
    override suspend fun exercises(planId: Long) = exerciseRows.filter { it.planId == planId }.sortedBy { it.position }
    override suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long = nextId++.also { exerciseRows += exercise.copy(id = it) }
    override suspend fun renameExercise(id: Long, name: String) { exerciseRows.replaceAll { if (it.id == id) it.copy(name = name) else it } }
    override suspend fun archiveExercise(id: Long, archived: Boolean) { exerciseRows.replaceAll { if (it.id == id) it.copy(isArchived = archived) else it } }
    override suspend fun sessionForDay(planId: Long, epochDay: Long) = sessions.firstOrNull { it.planId == planId && it.epochDay == epochDay }?.id
    override suspend fun insertSession(session: WorkoutSessionEntity): Long = nextId++.also { sessions += session.copy(id = it) }
    override suspend fun insertEntries(entries: List<WorkoutSessionExerciseEntity>) { this.entries += entries.map { it.copy(id = nextId++) } }
    override suspend fun deleteSession(id: Long) { sessions.removeAll { it.id == id } }
    override suspend fun getEntry(id: Long) = entries.firstOrNull { it.id == id }?.let { WorkoutEntryWithSets(it, sets.filter { set -> set.sessionExerciseId == id }) }
    override suspend fun deleteSets(entryId: Long) { sets.removeAll { it.sessionExerciseId == entryId } }
    override suspend fun insertSets(sets: List<WorkoutSetEntity>) { this.sets += sets.map { it.copy(id = nextId++) } }
}
