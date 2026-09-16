package dev.bugiel.kiroku.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bugiel.kiroku.R
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutScaffold(
    title: String,
    state: WorkoutsState,
    viewModel: WorkoutsViewModel,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    floatingAction: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val error = stringResource(R.string.workout_operation_error)
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbar.showSnackbar(error)
            viewModel.dismissError()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack, enabled = !state.isWorking) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = { actions() },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = floatingAction,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.isLoading -> item { CircularProgressIndicator(Modifier.padding(24.dp)) }
                state.loadFailed -> item {
                    WorkoutEmpty(stringResource(R.string.workout_load_error))
                    TextButton(onClick = viewModel::retryLoading) { Text(stringResource(R.string.workout_retry)) }
                }
                else -> content()
            }
        }
    }
}

@Composable
internal fun WorkoutEmpty(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun WorkoutNameDialog(
    title: String,
    label: String,
    initial: String = "",
    working: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text(label) },
                singleLine = true, enabled = !working, modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank() && !working) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun WorkoutConfirmation(
    title: String,
    message: String,
    action: String,
    working: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(title) }, text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !working) {
                Text(action, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun weightText(kilograms: Double): String {
    val locale = LocalConfiguration.current.locales[0]
    val number = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 3 } }
    return stringResource(R.string.workout_weight_value, number.format(kilograms))
}

@Composable
internal fun PeakWeight(kilograms: Double?) {
    Text(
        text = kilograms?.let { stringResource(R.string.workout_peak_weight, weightText(it)) }
            ?: stringResource(R.string.workout_no_record),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
