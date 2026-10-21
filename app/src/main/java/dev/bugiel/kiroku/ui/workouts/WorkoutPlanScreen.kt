package dev.bugiel.kiroku.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.WorkoutExercise
import dev.bugiel.kiroku.ui.util.formatLongDate
import java.time.LocalDate

@Composable
fun WorkoutPlanScreen(
    planId: Long,
    viewModel: WorkoutsViewModel,
    onBack: () -> Unit,
    onOpenDay: (Long) -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plans.firstOrNull { it.id == planId }
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    if (plan != null) {
        when (dialog) {
            "rename" -> WorkoutNameDialog(
                title = stringResource(R.string.workout_rename_plan), label = stringResource(R.string.workout_plan_name),
                initial = plan.name, working = state.isWorking, onDismiss = { dialog = null },
                onSave = { viewModel.renamePlan(plan.id, it) { dialog = null } },
            )
            "exercise" -> WorkoutExercisePicker(
                title = stringResource(R.string.workout_assign_exercise), state = state, viewModel = viewModel,
                excludedIds = plan.exercises.map { it.id }.toSet(), onDismiss = { dialog = null },
                onSelect = { viewModel.assignExercise(plan.id, it) { dialog = null } },
            )
            "delete" -> WorkoutConfirmation(
                title = stringResource(R.string.workout_delete_plan), message = stringResource(R.string.workout_delete_plan_message),
                action = stringResource(R.string.delete), working = state.isWorking, onDismiss = { dialog = null },
                onConfirm = { viewModel.deletePlan(plan.id) { dialog = null; onBack() } },
            )
        }
    }
    WorkoutScaffold(
        title = plan?.name ?: stringResource(R.string.workouts), state = state, viewModel = viewModel, onBack = onBack,
        actions = {
            if (plan != null) {
                IconButton(onClick = { dialog = "rename" }, enabled = !state.isWorking) {
                    Icon(Icons.Default.Edit, stringResource(R.string.workout_rename_plan))
                }
                IconButton(onClick = { dialog = "delete" }, enabled = !state.isWorking) {
                    Icon(Icons.Default.Delete, stringResource(R.string.workout_delete_plan))
                }
            }
        },
    ) {
        if (plan == null) {
            item { WorkoutEmpty(stringResource(R.string.workout_not_found)) }
        } else {
            item {
                Button(
                    onClick = { viewModel.startDay(plan.id, onOpenDay) }, modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isWorking,
                ) {
                    Text(stringResource(R.string.workout_start_today))
                }
                Text(
                    stringResource(R.string.workout_day_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.workout_exercises), style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { dialog = "exercise" }, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null)
                        Text(stringResource(R.string.workout_assign_exercise), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (plan.activeExercises.isEmpty()) item { WorkoutEmpty(stringResource(R.string.workout_no_exercises)) }
            items(plan.activeExercises, key = { "exercise-${it.id}" }) { exercise ->
                ExerciseCard(exercise, state.peakWeightKg(exercise.id)) { onOpenExercise(exercise.id) }
            }
            val archived = plan.exercises.filter { it.isArchived }
            if (archived.isNotEmpty()) {
                item {
                    OutlinedButton(onClick = { showArchived = !showArchived }) {
                        Text(stringResource(if (showArchived) R.string.workout_hide_archived else R.string.workout_show_archived))
                    }
                }
                if (showArchived) items(archived, key = { "archived-${it.id}" }) { exercise ->
                    ExerciseCard(exercise, state.peakWeightKg(exercise.id)) { onOpenExercise(exercise.id) }
                }
            }
            item { Text(stringResource(R.string.workout_days), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            if (plan.sessions.isEmpty()) item { WorkoutEmpty(stringResource(R.string.workout_no_days)) }
            items(plan.sessions, key = { "day-${it.id}" }) { day ->
                Card(onClick = { onOpenDay(day.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatLongDate(context, LocalDate.ofEpochDay(day.epochDay)), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.workout_day_progress, day.loggedExercises, day.exercises.size), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: WorkoutExercise, peak: Double?, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            PeakWeight(peak)
            if (exercise.isArchived) Text(stringResource(R.string.workout_archived), style = MaterialTheme.typography.labelMedium)
        }
    }
}
