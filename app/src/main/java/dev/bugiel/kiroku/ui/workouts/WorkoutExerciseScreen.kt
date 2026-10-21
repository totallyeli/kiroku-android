package dev.bugiel.kiroku.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.ui.util.formatLongDate
import java.time.LocalDate

@Composable
fun WorkoutExerciseScreen(
    exerciseId: Long, viewModel: WorkoutsViewModel, onBack: () -> Unit, onOpenEntry: (Long) -> Unit,
    planId: Long? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plans.firstOrNull { it.id == planId }
    val exercise = state.library.firstOrNull { it.id == exerciseId }
    val assigned = plan?.exercises?.any { it.id == exerciseId } == true
    val context = LocalContext.current
    var rename by rememberSaveable { mutableStateOf(false) }
    var archive by rememberSaveable { mutableStateOf(false) }
    if (rename && exercise != null) {
        WorkoutNameDialog(
            title = stringResource(R.string.workout_rename_exercise), label = stringResource(R.string.workout_exercise_name),
            initial = exercise.name, working = state.isWorking, onDismiss = { rename = false },
            onSave = { viewModel.renameExercise(exercise.id, it) { rename = false } },
        )
    }
    if (archive && exercise != null) {
        WorkoutConfirmation(
            title = stringResource(R.string.workout_archive_exercise), message = stringResource(R.string.workout_archive_message),
            action = stringResource(R.string.workout_archive), working = state.isWorking, onDismiss = { archive = false },
            onConfirm = { viewModel.archiveExercise(exercise.id, true) { archive = false } },
        )
    }
    WorkoutScaffold(
        title = exercise?.name ?: stringResource(R.string.workout_exercises), state = state, viewModel = viewModel, onBack = onBack,
        actions = {
            if (exercise != null) IconButton(onClick = { rename = true }, enabled = !state.isWorking) {
                Icon(Icons.Default.Edit, stringResource(R.string.workout_rename_exercise))
            }
        },
    ) {
        if (exercise == null) {
            item { WorkoutEmpty(stringResource(R.string.workout_not_found)) }
        } else {
            item {
                PeakWeight(state.peakWeightKg(exercise.id))
                Text(stringResource(R.string.workout_global_history_hint), style = MaterialTheme.typography.bodySmall)
                if (assigned) {
                    OutlinedButton(
                        onClick = { viewModel.unassignExercise(plan.id, exercise.id) { onBack() } },
                        enabled = !state.isWorking, modifier = Modifier.padding(top = 12.dp),
                    ) { Text(stringResource(R.string.workout_unassign_exercise)) }
                    Text(stringResource(R.string.workout_unassign_hint), style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = { if (exercise.isArchived) viewModel.archiveExercise(exercise.id, false) else archive = true },
                    enabled = !state.isWorking, modifier = Modifier.padding(top = 12.dp),
                ) { Text(stringResource(if (exercise.isArchived) R.string.workout_restore else R.string.workout_archive_exercise)) }
            }
            item { Text(stringResource(R.string.workout_exercise_history), style = MaterialTheme.typography.titleMedium) }
            val history = state.history(exercise.id)
            if (history.isEmpty()) item { WorkoutEmpty(stringResource(R.string.workout_no_history)) }
            items(history, key = { it.exercise.id }) { record ->
                Card(onClick = { onOpenEntry(record.exercise.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatLongDate(context, LocalDate.ofEpochDay(record.epochDay)), style = MaterialTheme.typography.titleMedium)
                        Text(record.exercise.name, style = MaterialTheme.typography.bodyLarge)
                        state.plans.firstOrNull { candidate -> candidate.sessions.any { it.id == record.sessionId } }?.let { sourcePlan ->
                            Text(sourcePlan.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        record.exercise.sets.forEachIndexed { index, set ->
                            Text(pluralStringResource(R.plurals.workout_set_summary, set.reps, index + 1, weightText(set.weightKg), set.reps))
                        }
                    }
                }
            }
        }
    }
}
