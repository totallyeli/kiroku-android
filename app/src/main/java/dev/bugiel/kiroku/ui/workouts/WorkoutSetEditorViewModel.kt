package dev.bugiel.kiroku.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.WorkoutRepository
import dev.bugiel.kiroku.domain.WorkoutValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class WorkoutSetDraft(val key: Int, val weight: String = "", val reps: String = "")

data class WorkoutSetEditorState(
    val title: String = "",
    val rows: List<WorkoutSetDraft> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val showValidation: Boolean = false,
    val isDirty: Boolean = false,
)

class WorkoutSetEditorViewModel(
    private val entryId: Long,
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WorkoutSetEditorState())
    val state = mutableState.asStateFlow()
    private var nextKey = 0

    init { load() }

    fun load() {
        mutableState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            try {
                val entry = requireNotNull(repository.getEntry(entryId))
                val rows = entry.sets.map {
                    WorkoutSetDraft(nextKey++, BigDecimal.valueOf(it.weightKg).stripTrailingZeros().toPlainString(), it.reps.toString())
                }.ifEmpty { listOf(WorkoutSetDraft(nextKey++)) }
                mutableState.value = WorkoutSetEditorState(title = entry.name, rows = rows, isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun changeWeight(key: Int, value: String) = edit { copy(rows = rows.map { if (it.key == key) it.copy(weight = value) else it }) }
    fun changeReps(key: Int, value: String) = edit { copy(rows = rows.map { if (it.key == key) it.copy(reps = value) else it }) }
    fun addSet() = edit { copy(rows = rows + WorkoutSetDraft(nextKey++)) }
    fun removeSet(key: Int) = edit { copy(rows = rows.filterNot { it.key == key }) }
    fun dismissError() = mutableState.update { it.copy(hasError = false) }

    fun save(onSaved: () -> Unit) {
        val snapshot = state.value
        if (snapshot.isLoading || snapshot.isSaving || snapshot.loadFailed) return
        val parsed = snapshot.rows.map { WorkoutValidation.parseSet(it.weight, it.reps) }
        if (parsed.any { it == null }) {
            mutableState.update { it.copy(showValidation = true) }
            return
        }
        mutableState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            try {
                repository.saveSets(entryId, parsed.filterNotNull())
                mutableState.update { it.copy(isSaving = false, isDirty = false) }
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isSaving = false, hasError = true) }
            }
        }
    }

    private fun edit(transform: WorkoutSetEditorState.() -> WorkoutSetEditorState) {
        if (!state.value.isLoading && !state.value.isSaving) {
            mutableState.update { it.transform().copy(isDirty = true, hasError = false) }
        }
    }
}
