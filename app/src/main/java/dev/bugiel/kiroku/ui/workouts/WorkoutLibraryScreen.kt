package dev.bugiel.kiroku.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R

@Composable
fun WorkoutLibraryScreen(viewModel: WorkoutsViewModel, onBack: () -> Unit, onOpenExercise: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var create by rememberSaveable { mutableStateOf(false) }
    if (create) WorkoutNameDialog(
        title = stringResource(R.string.workout_create_library_exercise), label = stringResource(R.string.workout_exercise_name),
        working = state.isWorking, onDismiss = { create = false },
        onSave = { name -> viewModel.createExercise(name) { create = false; onOpenExercise(it) } },
    )
    val exercises = state.library.filter {
        (showArchived || !it.isArchived) && it.name.contains(query.trim(), ignoreCase = true)
    }.sortedWith(compareBy({ it.isArchived }, { it.name.lowercase() }))
    WorkoutScaffold(
        title = stringResource(R.string.workout_exercise_library), state = state, viewModel = viewModel, onBack = onBack,
        floatingAction = {
            ExtendedFloatingActionButton(
                onClick = { if (!state.isWorking) create = true }, icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.workout_create_library_exercise)) },
            )
        },
    ) {
        item { Text(stringResource(R.string.workout_library_hint), style = MaterialTheme.typography.bodyLarge) }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text(stringResource(R.string.workout_search_exercises)) }, modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedButton(onClick = { showArchived = !showArchived }) {
                Text(stringResource(if (showArchived) R.string.workout_hide_archived else R.string.workout_show_archived))
            }
        }
        if (exercises.isEmpty()) item {
            WorkoutEmpty(stringResource(if (state.library.isEmpty()) R.string.workout_library_empty else R.string.workout_no_matching_exercises))
        }
        items(exercises, key = { it.id }) { exercise ->
            Card(onClick = { onOpenExercise(exercise.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    PeakWeight(state.peakWeightKg(exercise.id))
                    val plans = state.plans.filter { plan -> plan.exercises.any { it.id == exercise.id } }
                    if (plans.isNotEmpty()) Text(plans.joinToString { it.name }, style = MaterialTheme.typography.bodySmall)
                    if (state.library.count { it.name.equals(exercise.name, ignoreCase = true) } > 1) {
                        Text(stringResource(R.string.workout_exercise_identity, exercise.id), style = MaterialTheme.typography.labelSmall)
                    }
                    if (exercise.isArchived) Text(stringResource(R.string.workout_archived), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
