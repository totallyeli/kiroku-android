package dev.bugiel.kiroku.data.repository

import com.google.common.truth.Truth.assertThat
import dev.bugiel.kiroku.data.local.dao.WorkoutDao
import dev.bugiel.kiroku.data.local.entity.*
import dev.bugiel.kiroku.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkoutRepositoryTest {
    private val dao = MemoryWorkoutDao()
    private val repository = OfflineWorkoutRepository(dao)
    private val plan = runBlocking { repository.createPlan("Push", 100) }

    @Test fun `starting the same plan and day resumes without replacing saved sets`() = runBlocking {
        repository.addExercise(plan, "Bench press")
        val id = repository.startSession(plan, 20000, 123)
        repository.saveSets(dao.entries.single().id, listOf(WorkoutSet(80.0, 8)))
        val resumed = repository.startSession(plan, 20000, 456)
        assertThat(resumed).isEqualTo(id)
        assertThat(dao.sessions).hasSize(1)
        assertThat(dao.sets.single().weightKg).isEqualTo(80.0)
        assertThat(repository.startSession(plan, 20001, 789)).isNotEqualTo(id)
    }

    @Test fun `routine edits affect new days while earlier days keep their exercise snapshot`() = runBlocking {
        val bench = repository.addExercise(plan, "Bench press")
        repository.startSession(plan, 20000, 123)
        repository.renameExercise(bench, "Incline bench")
        val fly = repository.addExercise(plan, "Fly")
        repository.archiveExercise(fly, true)
        repository.startSession(plan, 20001, 456)
        assertThat(dao.entries.map { it.name }).containsExactly("Bench press", "Incline bench").inOrder()
        repository.archiveExercise(fly, false)
        repository.startSession(plan, 20002, 789)
        assertThat(dao.entries.takeLast(2).map { it.name }).containsExactly("Incline bench", "Fly").inOrder()
    }

    @Test fun `empty routine creates an editable empty workout and resumes it`() = runBlocking {
        val session = repository.startSession(plan, 20000, 123)
        assertThat(dao.sessions.single().id).isEqualTo(session)
        assertThat(dao.entries).isEmpty()
        val bench = repository.createExercise("Bench")
        val entry = repository.addSessionExercise(session, bench)
        repository.saveSets(entry, listOf(WorkoutSet(80.0, 8)))
        assertThat(repository.startSession(plan, 20000, 456)).isEqualTo(session)
        assertThat(repository.getEntry(entry)?.sets).containsExactly(WorkoutSet(80.0, 8))
        assertThat(dao.assignments).isEmpty()
    }

    @Test fun `invalid set replacement never removes previously saved sets`() = runBlocking {
        repository.addExercise(plan, "Bench")
        repository.startSession(plan, 20000, 123)
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

    @Test fun `plan and library names are trimmed and blank names rejected before mutation`() = runBlocking {
        repository.renamePlan(plan, " Push ")
        repository.createExercise(" Bench ")
        assertThat(dao.plans.single().name).isEqualTo("Push")
        assertThat(dao.exerciseRows.single().name).isEqualTo("Bench")
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.renamePlan(plan, "  ") } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.createExercise("  ") } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.addExercise(plan, "  ") } }
        assertThat(dao.exerciseRows).hasSize(1)
        assertThat(dao.assignments).isEmpty()
    }

    @Test fun `one library identity can be reused in two plans without copying it`(): Unit = runBlocking {
        val otherPlan = repository.createPlan("Full body", 200)
        val bench = repository.createExercise("Bench")
        repository.assignExercise(plan, bench)
        repository.assignExercise(otherPlan, bench)
        repository.assignExercise(plan, bench)
        repository.startSession(plan, 20000, 123)
        repository.startSession(otherPlan, 20001, 456)
        assertThat(repository.observeExercises().first().map { it.id }).containsExactly(bench)
        assertThat(dao.assignments).hasSize(2)
        assertThat(repository.observePlans().first().map { it.exercises.single().id }).containsExactly(bench, bench)
        assertThat(dao.entries.map { it.exerciseId }).containsExactly(bench, bench)
    }

    @Test fun `same named exercises keep distinct identities and add convenience creates and assigns`() = runBlocking {
        val libraryOnly = repository.createExercise("Bench")
        val assigned = repository.addExercise(plan, "Bench")
        assertThat(assigned).isNotEqualTo(libraryOnly)
        assertThat(dao.exerciseRows).hasSize(2)
        assertThat(dao.assignments.single().exerciseId).isEqualTo(assigned)
        repository.startSession(plan, 20000, 123)
        assertThat(dao.entries.single().exerciseId).isEqualTo(assigned)
    }

    @Test fun `unassignment and reassignment never erase logged day history`() = runBlocking {
        val bench = repository.addExercise(plan, "Bench")
        val session = repository.startSession(plan, 20000, 123)
        val entry = dao.entries.single().id
        repository.saveSets(entry, listOf(WorkoutSet(90.0, 6)))
        repository.unassignExercise(plan, bench)
        repository.unassignExercise(plan, bench)
        assertThat(repository.observePlans().first().single().exercises).isEmpty()
        assertThat(repository.getEntry(entry)?.sets).containsExactly(WorkoutSet(90.0, 6))
        assertThat(repository.observeExerciseHistory(bench).first().single().sessionId).isEqualTo(session)
        repository.startSession(plan, 20001, 456)
        assertThat(dao.entries).hasSize(1)
        repository.assignExercise(plan, bench)
        repository.startSession(plan, 20002, 789)
        assertThat(dao.entries).hasSize(2)
        assertThat(repository.observeExerciseHistory(bench).first()).hasSize(1)
    }

    @Test fun `day add swap and remove never change templates library or other days`() = runBlocking {
        val bench = repository.addExercise(plan, "Bench")
        val row = repository.createExercise("Row")
        val press = repository.createExercise("Press")
        val first = repository.startSession(plan, 20000, 123)
        val second = repository.startSession(plan, 20001, 456)
        val firstBench = dao.entries.single { it.sessionId == first }
        val secondBench = dao.entries.single { it.sessionId == second }
        repository.saveSets(firstBench.id, listOf(WorkoutSet(80.0, 8)))
        repository.saveSets(secondBench.id, listOf(WorkoutSet(85.0, 6)))
        val template = dao.assignments.toList()
        val library = dao.exerciseRows.toList()
        val added = repository.addSessionExercise(first, row)
        repository.saveSets(added, listOf(WorkoutSet(40.0, 12)))
        assertThat(repository.replaceSessionExercise(firstBench.id, press)).isEqualTo(firstBench.id)
        val replaced = dao.entries.single { it.id == firstBench.id }
        assertThat(replaced.position).isEqualTo(firstBench.position)
        assertThat(repository.getEntry(firstBench.id)?.exerciseId).isEqualTo(press)
        assertThat(repository.getEntry(firstBench.id)?.name).isEqualTo("Press")
        assertThat(repository.getEntry(firstBench.id)?.sets).isEmpty()
        repository.deleteSessionExercise(added)
        assertThat(repository.getEntry(added)).isNull()
        assertThat(dao.sets.none { it.sessionExerciseId == added }).isTrue()
        assertThat(dao.assignments).containsExactlyElementsIn(template)
        assertThat(dao.exerciseRows).containsExactlyElementsIn(library)
        assertThat(dao.entries.single { it.sessionId == second }).isEqualTo(secondBench)
        assertThat(repository.getEntry(secondBench.id)?.sets).containsExactly(WorkoutSet(85.0, 6))
        val third = repository.startSession(plan, 20002, 789)
        assertThat(dao.entries.single { it.sessionId == third }.exerciseId).isEqualTo(bench)
    }

    @Test fun `duplicate day additions resume existing entry including archived entries and preserve sets`(): Unit = runBlocking {
        val bench = repository.createExercise("Bench")
        val session = repository.startSession(plan, 20000, 123)
        val entry = repository.addSessionExercise(session, bench)
        repository.saveSets(entry, listOf(WorkoutSet(80.0, 8)))
        assertThat(repository.addSessionExercise(session, bench)).isEqualTo(entry)
        repository.archiveExercise(bench, true)
        assertThat(repository.addSessionExercise(session, bench)).isEqualTo(entry)
        assertThat(repository.replaceSessionExercise(entry, bench)).isEqualTo(entry)
        assertThat(dao.entries).hasSize(1)
        assertThat(repository.getEntry(entry)?.sets).containsExactly(WorkoutSet(80.0, 8))
    }

    @Test fun `archived exercise rejects new assignments additions and replacements without deleting sets`() = runBlocking {
        val bench = repository.addExercise(plan, "Bench")
        val archived = repository.createExercise("Archived")
        val session = repository.startSession(plan, 20000, 123)
        val entry = dao.entries.single().id
        repository.saveSets(entry, listOf(WorkoutSet(80.0, 8)))
        repository.archiveExercise(archived, true)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.assignExercise(plan, archived) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.addSessionExercise(session, archived) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.replaceSessionExercise(entry, archived) } }
        assertThat(repository.getEntry(entry)?.exerciseId).isEqualTo(bench)
        assertThat(repository.getEntry(entry)?.sets).containsExactly(WorkoutSet(80.0, 8))
        assertThat(dao.assignments).hasSize(1)
        assertThat(dao.entries).hasSize(1)
        assertThat(repository.observeExercises().first().single { it.id == archived }.isArchived).isTrue()
    }

    @Test fun `duplicate and nonexistent replacement targets preserve original identity position and sets`() = runBlocking {
        repository.addExercise(plan, "Bench")
        val row = repository.addExercise(plan, "Row")
        repository.startSession(plan, 20000, 123)
        val original = dao.entries.first()
        repository.saveSets(original.id, listOf(WorkoutSet(80.0, 8)))
        for (target in listOf(row, Long.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.replaceSessionExercise(original.id, target) } }
            assertThat(dao.entries.first()).isEqualTo(original)
            assertThat(repository.getEntry(original.id)?.sets).containsExactly(WorkoutSet(80.0, 8))
        }
    }

    @Test fun `global history and peak follow identity across plans even after rename archive and unassign`() = runBlocking {
        val other = repository.createPlan("Full body", 200)
        val bench = repository.addExercise(plan, "Bench")
        repository.assignExercise(other, bench)
        val first = repository.startSession(plan, 20000, 123)
        repository.saveSets(dao.entries.single().id, listOf(WorkoutSet(80.0, 8)))
        repository.renameExercise(bench, "Incline bench")
        val second = repository.startSession(other, 20002, 456)
        repository.saveSets(dao.entries.last().id, listOf(WorkoutSet(100.0, 3), WorkoutSet(85.0, 8)))
        repository.startSession(plan, 20001, 789) // Unlogged entries do not count as history.
        repository.archiveExercise(bench, true)
        repository.unassignExercise(plan, bench)
        repository.unassignExercise(other, bench)
        val history = repository.observeExerciseHistory(bench).first()
        assertThat(history.map { it.sessionId }).containsExactly(second, first).inOrder()
        assertThat(history.map { it.planId }).containsExactly(other, plan).inOrder()
        assertThat(history.map { it.exercise.name }).containsExactly("Incline bench", "Bench").inOrder()
        assertThat(history.flatMap { it.exercise.sets }.maxOf { it.weightKg }).isEqualTo(100.0)
        assertThat(repository.observeExerciseHistory(Long.MAX_VALUE).first()).isEmpty()
    }

    @Test fun `deleting a plan keeps global library and shared exercise history in other plans`() = runBlocking {
        val other = repository.createPlan("Full body", 200)
        val bench = repository.addExercise(plan, "Bench")
        val unused = repository.addExercise(plan, "Unused")
        repository.assignExercise(other, bench)
        val first = repository.startSession(plan, 20000, 123)
        repository.saveSets(dao.entries.first().id, listOf(WorkoutSet(80.0, 8)))
        val second = repository.startSession(other, 20001, 456)
        val surviving = dao.entries.single { it.sessionId == second }.id
        repository.saveSets(surviving, listOf(WorkoutSet(95.0, 5)))
        repository.deletePlan(plan)
        assertThat(repository.observeExercises().first().map { it.id }).containsExactly(bench, unused)
        assertThat(dao.sessions.map { it.id }).doesNotContain(first)
        assertThat(repository.observeExerciseHistory(bench).first().single().sessionId).isEqualTo(second)
        assertThat(repository.getEntry(surviving)?.sets).containsExactly(WorkoutSet(95.0, 5))
        assertThat(dao.assignments.single().planId).isEqualTo(other)
    }

    @Test fun `missing plan session exercise and entry fail before creating partial data`() = runBlocking {
        val bench = repository.createExercise("Bench")
        val missing = Long.MAX_VALUE
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.startSession(missing, 20000, 123) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.addExercise(missing, "Orphan") } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.assignExercise(missing, bench) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.assignExercise(plan, missing) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.addSessionExercise(missing, bench) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.replaceSessionExercise(missing, bench) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.deleteSessionExercise(missing) } }
        assertThat(dao.exerciseRows.map { it.id }).containsExactly(bench)
        assertThat(dao.assignments).isEmpty()
        assertThat(dao.sessions).isEmpty()
        assertThat(dao.entries).isEmpty()
        assertThat(dao.sets).isEmpty()
    }

    @Test fun `deleting one day cascades sets but leaves template and other days`() = runBlocking {
        val bench = repository.addExercise(plan, "Bench")
        val first = repository.startSession(plan, 20000, 123)
        repository.saveSets(dao.entries.single().id, listOf(WorkoutSet(80.0, 8)))
        val second = repository.startSession(plan, 20001, 456)
        repository.saveSets(dao.entries.last().id, listOf(WorkoutSet(90.0, 5)))
        repository.deleteSession(first)
        assertThat(dao.entries.single().sessionId).isEqualTo(second)
        assertThat(dao.sets.single().weightKg).isEqualTo(90.0)
        assertThat(dao.assignments.single().exerciseId).isEqualTo(bench)
    }
}

// Executes real DAO transaction orchestration. SQL/foreign-key constraints are checked in Python.
// Flows resolve fresh snapshots on collection; this fake does not simulate Room invalidation.
private class MemoryWorkoutDao : WorkoutDao() {
    val plans = mutableListOf<WorkoutPlanEntity>()
    val exerciseRows = mutableListOf<WorkoutExerciseEntity>()
    val assignments = mutableListOf<WorkoutPlanExerciseEntity>()
    val sessions = mutableListOf<WorkoutSessionEntity>()
    val entries = mutableListOf<WorkoutSessionExerciseEntity>()
    val sets = mutableListOf<WorkoutSetEntity>()
    private var nextId = 1L

    private fun entryWithSets(entry: WorkoutSessionExerciseEntity) = WorkoutEntryWithSets(entry, sets.filter { it.sessionExerciseId == entry.id })
    override fun observePlans(): Flow<List<WorkoutPlanWithHistory>> = flow {
        emit(plans.sortedBy { it.createdAt }.map { plan ->
            WorkoutPlanWithHistory(plan,
                assignments.filter { it.planId == plan.id }.map { assignment ->
                    WorkoutAssignedExercise(assignment, exerciseRows.single { it.id == assignment.exerciseId })
                },
                sessions.filter { it.planId == plan.id }.map { session ->
                    WorkoutSessionWithEntries(session, entries.filter { it.sessionId == session.id }.map(::entryWithSets))
                },
            )
        })
    }
    override fun observeExercises(): Flow<List<WorkoutExerciseEntity>> = flow {
        emit(exerciseRows.sortedWith(compareBy({ it.name.lowercase() }, { it.id })))
    }
    override fun observeExerciseHistory(exerciseId: Long): Flow<List<WorkoutExerciseHistoryRow>> = flow {
        emit(entries.filter { entry -> entry.exerciseId == exerciseId && sets.any { it.sessionExerciseId == entry.id } }
            .map { entry ->
                val session = sessions.single { it.id == entry.sessionId }
                WorkoutExerciseHistoryRow(session.epochDay, session.planId, entryWithSets(entry))
            }.sortedWith(compareByDescending<WorkoutExerciseHistoryRow> { it.epochDay }
                .thenByDescending { it.entry.entry.sessionId }.thenByDescending { it.entry.entry.id }))
    }
    override suspend fun insertPlan(plan: WorkoutPlanEntity): Long = nextId++.also { plans += plan.copy(id = it) }
    override suspend fun renamePlan(id: Long, name: String) { plans.replaceAll { if (it.id == id) it.copy(name = name) else it } }
    override suspend fun deletePlan(id: Long) {
        sessions.filter { it.planId == id }.map { it.id }.forEach { deleteSession(it) }
        assignments.removeAll { it.planId == id }
        plans.removeAll { it.id == id }
    }
    override suspend fun planExists(planId: Long) = plans.any { it.id == planId }
    override suspend fun exercises(planId: Long) = assignments.filter { it.planId == planId }.sortedBy { it.position }
        .map { assignment -> exerciseRows.single { it.id == assignment.exerciseId } }
    override suspend fun exercise(exerciseId: Long) = exerciseRows.firstOrNull { it.id == exerciseId }
    override suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long = nextId++.also { exerciseRows += exercise.copy(id = it) }
    override suspend fun updateExerciseName(id: Long, name: String) { exerciseRows.replaceAll { if (it.id == id) it.copy(name = name) else it } }
    override suspend fun updateExerciseArchived(id: Long, archived: Boolean) { exerciseRows.replaceAll { if (it.id == id) it.copy(isArchived = archived) else it } }
    override suspend fun assignment(planId: Long, exerciseId: Long) = assignments.firstOrNull { it.planId == planId && it.exerciseId == exerciseId }
    override suspend fun nextPlanPosition(planId: Long) = (assignments.filter { it.planId == planId }.maxOfOrNull { it.position } ?: -1) + 1
    override suspend fun insertAssignment(assignment: WorkoutPlanExerciseEntity) { assignments += assignment }
    override suspend fun deleteAssignment(planId: Long, exerciseId: Long) { assignments.removeAll { it.planId == planId && it.exerciseId == exerciseId } }
    override suspend fun sessionForDay(planId: Long, epochDay: Long) = sessions.firstOrNull { it.planId == planId && it.epochDay == epochDay }?.id
    override suspend fun sessionExists(sessionId: Long) = sessions.any { it.id == sessionId }
    override suspend fun insertSession(session: WorkoutSessionEntity): Long {
        if (sessionForDay(session.planId, session.epochDay) != null) return -1
        return nextId++.also { sessions += session.copy(id = it) }
    }
    override suspend fun insertEntries(entries: List<WorkoutSessionExerciseEntity>) { entries.forEach { insertEntry(it) } }
    override suspend fun insertEntry(entry: WorkoutSessionExerciseEntity): Long = nextId++.also { entries += entry.copy(id = it) }
    override suspend fun sessionEntry(sessionId: Long, exerciseId: Long) = entries.firstOrNull { it.sessionId == sessionId && it.exerciseId == exerciseId }?.id
    override suspend fun nextSessionPosition(sessionId: Long) = (entries.filter { it.sessionId == sessionId }.maxOfOrNull { it.position } ?: -1) + 1
    override suspend fun updateEntryExercise(entryId: Long, exerciseId: Long, name: String) { entries.replaceAll { if (it.id == entryId) it.copy(exerciseId = exerciseId, name = name) else it } }
    override suspend fun deleteEntry(entryId: Long) {
        deleteSets(entryId)
        entries.removeAll { it.id == entryId }
    }
    override suspend fun deleteSession(id: Long) {
        entries.filter { it.sessionId == id }.map { it.id }.forEach { deleteEntry(it) }
        sessions.removeAll { it.id == id }
    }
    override suspend fun getEntry(id: Long) = entries.firstOrNull { it.id == id }?.let(::entryWithSets)
    override suspend fun deleteSets(entryId: Long) { sets.removeAll { it.sessionExerciseId == entryId } }
    override suspend fun insertSets(sets: List<WorkoutSetEntity>) { this.sets += sets.map { it.copy(id = nextId++) } }
}
