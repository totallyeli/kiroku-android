package dev.bugiel.kiroku.ui.workouts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.WorkoutValidation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSetEditorScreen(viewModel: WorkoutSetEditorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var discard by rememberSaveable { mutableStateOf(false) }
    val goBack = {
        if (!state.isSaving) {
            if (state.isDirty) discard = true else onBack()
        }
    }
    BackHandler(onBack = goBack)
    val snackbar = remember { SnackbarHostState() }
    val error = stringResource(R.string.workout_operation_error)
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbar.showSnackbar(error)
            viewModel.dismissError()
        }
    }
    if (discard) {
        WorkoutConfirmation(
            title = stringResource(R.string.workout_discard_title), message = stringResource(R.string.workout_discard_message),
            action = stringResource(R.string.workout_discard), onDismiss = { discard = false }, onConfirm = onBack,
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { stringResource(R.string.workout_log_sets) }, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = goBack, enabled = !state.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onBack) }, enabled = !state.isLoading && !state.isSaving && !state.loadFailed) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.isLoading -> item { CircularProgressIndicator() }
                state.loadFailed -> item {
                    WorkoutEmpty(stringResource(R.string.workout_load_error))
                    TextButton(onClick = viewModel::load) { Text(stringResource(R.string.workout_retry)) }
                }
                else -> {
                    item { Text(stringResource(R.string.workout_sets_hint), style = MaterialTheme.typography.bodyMedium) }
                    itemsIndexed(state.rows, key = { _, row -> row.key }) { index, row ->
                        val invalid = state.showValidation && WorkoutValidation.parseSet(row.weight, row.reps) == null
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.workout_set_number, index + 1), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { viewModel.removeSet(row.key) }, enabled = !state.isSaving) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.workout_remove_set, index + 1))
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = row.weight, onValueChange = { viewModel.changeWeight(row.key, it) },
                                        label = { Text(stringResource(R.string.workout_weight_kg)) },
                                        modifier = Modifier.weight(1f), singleLine = true, isError = invalid, enabled = !state.isSaving,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    )
                                    OutlinedTextField(
                                        value = row.reps, onValueChange = { viewModel.changeReps(row.key, it) },
                                        label = { Text(stringResource(R.string.workout_reps)) },
                                        modifier = Modifier.weight(1f), singleLine = true, isError = invalid, enabled = !state.isSaving,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    )
                                }
                                if (invalid) Text(
                                    stringResource(R.string.workout_set_invalid), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    if (state.rows.isEmpty()) item { Text(stringResource(R.string.workout_empty_sets)) }
                    item {
                        OutlinedButton(onClick = viewModel::addSet, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null)
                            Text(stringResource(R.string.workout_add_set))
                        }
                    }
                }
            }
        }
    }
}
