package dev.bugiel.kiroku.data.repository

import dev.bugiel.kiroku.data.local.dao.WorkoutDao
import dev.bugiel.kiroku.data.local.entity.WorkoutPlanEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutSetEntity
import dev.bugiel.kiroku.domain.WorkoutValidation
import dev.bugiel.kiroku.domain.model.WorkoutPlan
import dev.bugiel.kiroku.domain.model.WorkoutSessionExercise
import dev.bugiel.kiroku.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WorkoutRepository {
    fun observePlans(): Flow<List<WorkoutPlan>>
    suspend fun createPlan(name: String, nowMillis: Long): Long
    suspend fun renamePlan(id: Long, name: String)
    suspend fun deletePlan(id: Long)
    suspend fun addExercise(planId: Long, name: String): Long
    suspend fun renameExercise(id: Long, name: String)
    suspend fun archiveExercise(id: Long, archived: Boolean)
    suspend fun startSession(planId: Long, epochDay: Long, nowMillis: Long): Long
    suspend fun deleteSession(id: Long)
    suspend fun getEntry(id: Long): WorkoutSessionExercise?
    suspend fun saveSets(entryId: Long, sets: List<WorkoutSet>)
}

class OfflineWorkoutRepository(private val dao: WorkoutDao) : WorkoutRepository {
    override fun observePlans() = dao.observePlans().map { plans -> plans.map { it.toDomain() } }
    override suspend fun createPlan(name: String, nowMillis: Long): Long =
        dao.insertPlan(WorkoutPlanEntity(name = checkedName(name), createdAt = nowMillis))
    override suspend fun renamePlan(id: Long, name: String) = dao.renamePlan(id, checkedName(name))
    override suspend fun deletePlan(id: Long) = dao.deletePlan(id)
    override suspend fun addExercise(planId: Long, name: String): Long = dao.addExercise(planId, checkedName(name))
    override suspend fun renameExercise(id: Long, name: String) = dao.renameExercise(id, checkedName(name))
    override suspend fun archiveExercise(id: Long, archived: Boolean) = dao.archiveExercise(id, archived)
    override suspend fun startSession(planId: Long, epochDay: Long, nowMillis: Long): Long =
        dao.startSession(planId, epochDay, nowMillis)
    override suspend fun deleteSession(id: Long) = dao.deleteSession(id)
    override suspend fun getEntry(id: Long) = dao.getEntry(id)?.toDomain()
    override suspend fun saveSets(entryId: Long, sets: List<WorkoutSet>) {
        require(sets.all(WorkoutValidation::isValid))
        dao.replaceSets(entryId, sets.mapIndexed { index, set ->
            WorkoutSetEntity(sessionExerciseId = entryId, position = index, weightKg = set.weightKg, reps = set.reps)
        })
    }

    private fun checkedName(name: String): String = name.trim().also { require(it.isNotEmpty()) }
}
