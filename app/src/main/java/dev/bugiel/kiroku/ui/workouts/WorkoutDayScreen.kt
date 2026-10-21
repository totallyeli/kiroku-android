package dev.bugiel.kiroku.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun WorkoutDayScreen(
    planId: Long, sessionId: Long, viewModel: WorkoutsViewModel, onBack: () -> Unit, onOpenEntry: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plans.firstOrNull { it.id == planId }
    val day = plan?.sessions?.firstOrNull { it.id == sessionId }
    val context = LocalContext.current
    var managing by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var targetEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var replacementId by rememberSaveable { mutableStateOf<Long?>(null) }
    val target = day?.exercises?.firstOrNull { it.id == targetEntryId }
    if (day != null) {
        when (dialog) {
            "delete" -> WorkoutConfirmation(
                title = stringResource(R.string.workout_delete_day), message = stringResource(R.string.workout_delete_day_message),
                action = stringResource(R.string.delete), working = state.isWorking, onDismiss = { dialog = null },
                onConfirm = { viewModel.deleteDay(day.id) { dialog = null; onBack() } },
            )
            "add", "replace" -> WorkoutExercisePicker(
                title = stringResource(if (dialog == "add") R.string.workout_add_day_exercise else R.string.workout_replace_day_exercise),
                state = state, viewModel = viewModel, excludedIds = day.exercises.map { it.exerciseId }.toSet(),
                onDismiss = { dialog = null },
                onSelect = { exerciseId ->
                    if (dialog == "add") {
                        viewModel.addSessionExercise(day.id, exerciseId) { dialog = null }
                    } else if (target != null) {
                        if (target.sets.isEmpty()) {
                            viewModel.replaceSessionExercise(target.id, exerciseId) { dialog = null }
                        } else {
                            replacementId = exerciseId
                            dialog = "confirmReplace"
                        }
                    }
                },
            )
            "confirmReplace", "confirmRemove" -> if (target != null) WorkoutConfirmation(
                title = stringResource(if (dialog == "confirmReplace") R.string.workout_replace_day_exercise else R.string.workout_remove_day_exercise),
                message = stringResource(
                    if (dialog == "confirmReplace") R.string.workout_replace_sets_warning else R.string.workout_remove_sets_warning,
                    target.name,
                ),
                action = stringResource(if (dialog == "confirmReplace") R.string.workout_replace_action else R.string.workout_remove_action),
                working = state.isWorking, onDismiss = { dialog = null },
                onConfirm = {
                    if (dialog == "confirmReplace") {
                        replacementId?.let { viewModel.replaceSessionExercise(target.id, it) { dialog = null } }
                    } else {
                        viewModel.deleteSessionExercise(target.id) { dialog = null }
                    }
                },
            )
        }
    }
    WorkoutScaffold(
        title = day?.let { formatLongDate(context, LocalDate.ofEpochDay(it.epochDay)) } ?: stringResource(R.string.workout_day),
        state = state, viewModel = viewModel, onBack = onBack,
        actions = {
            if (day != null) {
                IconButton(onClick = { managing = !managing }, enabled = !state.isWorking) {
                    Icon(Icons.Default.Settings, stringResource(R.string.workout_manage_day))
                }
                IconButton(onClick = { dialog = "delete" }, enabled = !state.isWorking) {
                    Icon(Icons.Default.Delete, stringResource(R.string.workout_delete_day))
                }
            }
        },
    ) {
        if (day == null) {
            item { WorkoutEmpty(stringResource(R.string.workout_not_found)) }
        } else {
            item {
                Text(plan.name, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.workout_day_progress, day.loggedExercises, day.exercises.size), modifier = Modifier.padding(top = 8.dp))
            }
            if (managing) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.workout_day_only_hint), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { dialog = "add" }, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.workout_add_day_exercise))
                    }
                    TextButton(onClick = { managing = false }, enabled = !state.isWorking) {
                        Text(stringResource(R.string.workout_finish_managing))
                    }
                }
            }
            if (day.exercises.isEmpty()) item { WorkoutEmpty(stringResource(R.string.workout_day_empty)) }
            items(day.exercises, key = { it.id }) { entry ->
                Card(onClick = { if (!state.isWorking) onOpenEntry(entry.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(entry.name, style = MaterialTheme.typography.titleMedium)
                        PeakWeight(state.peakWeightKg(entry.exerciseId))
                        if (entry.sets.isEmpty()) {
                            Text(stringResource(R.string.workout_tap_to_log), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(stringResource(R.string.workout_logged), style = MaterialTheme.typography.labelLarge)
                            entry.sets.forEachIndexed { index, set ->
                                Text(pluralStringResource(R.plurals.workout_set_summary, set.reps, index + 1, weightText(set.weightKg), set.reps))
                            }
                        }
                        if (managing) {
                            OutlinedButton(
                                onClick = { targetEntryId = entry.id; dialog = "replace" }, enabled = !state.isWorking,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.workout_replace_day_exercise)) }
                            TextButton(
                                onClick = {
                                    targetEntryId = entry.id
                                    if (entry.sets.isEmpty()) {
                                        viewModel.deleteSessionExercise(entry.id) { targetEntryId = null }
                                    } else dialog = "confirmRemove"
                                }, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.workout_remove_day_exercise), color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}
