package dev.bugiel.kiroku.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bugiel.kiroku.R

/** Shared by plan assignments and day-only edits. Existing entries are never selectable twice. */
@Composable
internal fun WorkoutExercisePicker(
    title: String,
    state: WorkoutsState,
    viewModel: WorkoutsViewModel,
    excludedIds: Set<Long>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }
    if (create) {
        WorkoutNameDialog(
            title = stringResource(R.string.workout_create_library_exercise),
            label = stringResource(R.string.workout_exercise_name), initial = query,
            working = state.isWorking, onDismiss = { create = false },
            onSave = { name -> viewModel.createExercise(name) { query = ""; create = false } },
        )
        return
    }
    val choices = state.library.filter {
        !it.isArchived && it.id !in excludedIds && it.name.contains(query.trim(), ignoreCase = true)
    }.sortedBy { it.name.lowercase() }
    AlertDialog(
        onDismissRequest = { if (!state.isWorking) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text(stringResource(R.string.workout_search_exercises)) },
                    enabled = !state.isWorking, modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp).weight(1f, fill = false)) {
                    if (choices.isEmpty()) item {
                        Text(stringResource(R.string.workout_no_available_exercises), style = MaterialTheme.typography.bodyMedium)
                    }
                    items(choices, key = { it.id }) { exercise ->
                        TextButton(
                            onClick = { onSelect(exercise.id) }, enabled = !state.isWorking,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(exercise.name)
                                PeakWeight(state.peakWeightKg(exercise.id))
                                val plans = state.plans.filter { plan -> plan.exercises.any { it.id == exercise.id } }
                                if (plans.isNotEmpty()) {
                                    Text(plans.joinToString { it.name }, style = MaterialTheme.typography.bodySmall)
                                }
                                // Names are not identities: migration deliberately preserves same-named exercises.
                                if (state.library.count { it.name.equals(exercise.name, ignoreCase = true) } > 1) {
                                    Text(stringResource(R.string.workout_exercise_identity, exercise.id), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { create = true }, enabled = !state.isWorking) {
                Text(stringResource(R.string.workout_create_library_exercise))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isWorking) { Text(stringResource(R.string.cancel)) }
        },
    )
}
