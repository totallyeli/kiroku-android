package dev.bugiel.kiroku.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
fun WorkoutDayScreen(
    planId: Long, sessionId: Long, viewModel: WorkoutsViewModel, onBack: () -> Unit, onOpenEntry: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plans.firstOrNull { it.id == planId }
    val day = plan?.sessions?.firstOrNull { it.id == sessionId }
    val context = LocalContext.current
    var showDelete by rememberSaveable { mutableStateOf(false) }
    if (showDelete && day != null) {
        WorkoutConfirmation(
            title = stringResource(R.string.workout_delete_day), message = stringResource(R.string.workout_delete_day_message),
            action = stringResource(R.string.delete), working = state.isWorking, onDismiss = { showDelete = false },
            onConfirm = { viewModel.deleteDay(day.id) { showDelete = false; onBack() } },
        )
    }
    WorkoutScaffold(
        title = day?.let { formatLongDate(context, LocalDate.ofEpochDay(it.epochDay)) } ?: stringResource(R.string.workout_day),
        state = state, viewModel = viewModel, onBack = onBack,
        actions = {
            if (day != null) IconButton(onClick = { showDelete = true }, enabled = !state.isWorking) {
                Icon(Icons.Default.Delete, stringResource(R.string.workout_delete_day))
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
            items(day.exercises, key = { it.id }) { entry ->
                Card(onClick = { onOpenEntry(entry.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(entry.name, style = MaterialTheme.typography.titleMedium)
                        PeakWeight(plan.peakWeightKg(entry.exerciseId))
                        if (entry.sets.isEmpty()) {
                            Text(stringResource(R.string.workout_tap_to_log), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(stringResource(R.string.workout_logged), style = MaterialTheme.typography.labelLarge)
                            entry.sets.forEachIndexed { index, set ->
                                Text(pluralStringResource(R.plurals.workout_set_summary, set.reps, index + 1, weightText(set.weightKg), set.reps))
                            }
                        }
                    }
                }
            }
        }
    }
}
