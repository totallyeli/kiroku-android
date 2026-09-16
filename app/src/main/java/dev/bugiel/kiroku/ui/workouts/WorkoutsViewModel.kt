package dev.bugiel.kiroku.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.WorkoutRepository
import dev.bugiel.kiroku.domain.model.WorkoutPlan
import dev.bugiel.kiroku.domain.time.DateClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutsState(
    val plans: List<WorkoutPlan> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isWorking: Boolean = false,
    val hasError: Boolean = false,
)

class WorkoutsViewModel(
    private val repository: WorkoutRepository,
    private val dateClock: DateClock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WorkoutsState())
    val state = mutableState.asStateFlow()

    init { load() }

    fun retryLoading() {
        if (state.value.loadFailed) load()
    }

    private fun load() {
        mutableState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            try {
                repository.observePlans().collect { plans ->
                    mutableState.update { it.copy(plans = plans, isLoading = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun dismissError() = mutableState.update { it.copy(hasError = false) }

    fun createPlan(name: String, onCreated: (Long) -> Unit) = mutate {
        onCreated(repository.createPlan(name, dateClock.nowMillis()))
    }
    fun renamePlan(id: Long, name: String, onSaved: () -> Unit) = mutate {
        repository.renamePlan(id, name)
        onSaved()
    }
    fun deletePlan(id: Long, onDeleted: () -> Unit) = mutate {
        repository.deletePlan(id)
        onDeleted()
    }
    fun addExercise(planId: Long, name: String, onSaved: () -> Unit) = mutate {
        repository.addExercise(planId, name)
        onSaved()
    }
    fun renameExercise(id: Long, name: String, onSaved: () -> Unit) = mutate {
        repository.renameExercise(id, name)
        onSaved()
    }
    fun archiveExercise(id: Long, archived: Boolean, onSaved: () -> Unit = {}) = mutate {
        repository.archiveExercise(id, archived)
        onSaved()
    }
    fun startDay(planId: Long, onStarted: (Long) -> Unit) = mutate {
        onStarted(repository.startSession(planId, dateClock.today().toEpochDay(), dateClock.nowMillis()))
    }
    fun deleteDay(id: Long, onDeleted: () -> Unit) = mutate {
        repository.deleteSession(id)
        onDeleted()
    }

    private fun mutate(block: suspend () -> Unit) {
        if (state.value.isWorking) return
        mutableState.update { it.copy(isWorking = true, hasError = false) }
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(hasError = true) }
            } finally {
                mutableState.update { it.copy(isWorking = false) }
            }
        }
    }
}
