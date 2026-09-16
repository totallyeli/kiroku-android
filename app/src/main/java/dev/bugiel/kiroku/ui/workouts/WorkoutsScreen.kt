package dev.bugiel.kiroku.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R

@Composable
fun WorkoutsScreen(viewModel: WorkoutsViewModel, onOpenPlan: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    if (showCreate) {
        WorkoutNameDialog(
            title = stringResource(R.string.workout_new_plan), label = stringResource(R.string.workout_plan_name),
            working = state.isWorking, onDismiss = { showCreate = false },
            onSave = { name -> viewModel.createPlan(name) { id -> showCreate = false; onOpenPlan(id) } },
        )
    }
    WorkoutScaffold(
        title = stringResource(R.string.workouts), state = state, viewModel = viewModel,
        actions = {
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, stringResource(R.string.settings)) }
        },
        floatingAction = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true }, icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.workout_new_plan)) },
            )
        },
    ) {
        item { Text(stringResource(R.string.workout_intro), style = MaterialTheme.typography.bodyLarge) }
        if (state.plans.isEmpty()) item { WorkoutEmpty(stringResource(R.string.workout_no_plans)) }
        items(state.plans, key = { it.id }) { plan ->
            Card(onClick = { onOpenPlan(plan.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(plan.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.workout_plan_summary, plan.activeExercises.size, plan.sessions.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
