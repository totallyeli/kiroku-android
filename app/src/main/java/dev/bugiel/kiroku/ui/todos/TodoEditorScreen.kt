package dev.bugiel.kiroku.ui.todos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.HabitColorKey
import dev.bugiel.kiroku.domain.model.HabitIconKey
import dev.bugiel.kiroku.domain.model.TodoPriority
import dev.bugiel.kiroku.domain.model.TodoReminderOffset
import dev.bugiel.kiroku.domain.time.HabitHistoryDates
import dev.bugiel.kiroku.ui.util.formatLongDate
import dev.bugiel.kiroku.ui.util.formatTime
import dev.bugiel.kiroku.ui.util.habitColor
import dev.bugiel.kiroku.ui.util.habitColorLabel
import dev.bugiel.kiroku.ui.util.habitIcon
import dev.bugiel.kiroku.ui.util.habitIconLabel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditorScreen(
    viewModel: TodoEditorViewModel,
    onSaved: (Long) -> Unit,
    onDeleted: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionMissing by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionMissing = !granted
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveError = stringResource(R.string.todo_save_error)

    LaunchedEffect(state.hasSaveError) {
        if (state.hasSaveError) snackbarHostState.showSnackbar(saveError)
    }
    BackHandler(onBack = onNavigateBack)

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_todo_title)) },
            text = { Text(stringResource(R.string.delete_todo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.delete(onDeleted)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.id == 0L) R.string.new_todo else R.string.edit_todo)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onSaved) }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            TodoEditorContent(
                state = state,
                onTitleChange = viewModel::setTitle,
                onDescriptionChange = viewModel::setDescription,
                onDateChange = viewModel::setDueDate,
                onTimeChange = viewModel::setDueTime,
                onPriorityChange = viewModel::setPriority,
                onReminderEnabled = { enabled ->
                    viewModel.setReminderEnabled(enabled)
                    if (
                        enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        notificationPermissionMissing
                    ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onReminderOffsetChange = viewModel::setReminderOffset,
                onColorChange = viewModel::setColor,
                onIconChange = viewModel::setIcon,
                notificationPermissionMissing = notificationPermissionMissing,
                onDelete = { showDeleteConfirmation = true },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoEditorContent(
    state: TodoEditorState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onTimeChange: (Int) -> Unit,
    onPriorityChange: (String) -> Unit,
    onReminderEnabled: (Boolean) -> Unit,
    onReminderOffsetChange: (Int) -> Unit,
    onColorChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    notificationPermissionMissing: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showAllIcons by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = HabitHistoryDates.utcMillisFromEpochDay(
                state.dueEpochDay ?: LocalDate.now().toEpochDay(),
            ),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        onDateChange(HabitHistoryDates.epochDayFromUtcMillis(it))
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.choose_date)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = dateState, showModeToggle = false) }
    }

    if (showTimePicker) {
        val currentMinutes = state.dueTimeMinutes ?: DEFAULT_DUE_TIME_MINUTES
        val timeState = rememberTimePickerState(
            initialHour = currentMinutes / 60,
            initialMinute = currentMinutes % 60,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.todo_due_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(timeState.hour * 60 + timeState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.choose_time)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showAllIcons) {
        AlertDialog(
            onDismissRequest = { showAllIcons = false },
            title = { Text(stringResource(R.string.choose_todo_icon)) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(HabitIconKey.all, key = { it }) { key ->
                        TodoIconOption(
                            key = key,
                            selected = state.iconKey == key,
                            onClick = {
                                onIconChange(key)
                                showAllIcons = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllIcons = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.todo_title)) },
            placeholder = { Text(stringResource(R.string.todo_title_example)) },
            singleLine = true,
            isError = state.showTitleError,
            supportingText = if (state.showTitleError) {
                { Text(stringResource(R.string.todo_title_required)) }
            } else null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.todo_description)) },
            minLines = 2,
            maxLines = 5,
        )

        SectionTitle(R.string.todo_deadline)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Text(
                    text = state.dueEpochDay?.let { formatLongDate(context, LocalDate.ofEpochDay(it)) }
                        ?: stringResource(R.string.todo_due_date),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Text(
                    text = state.dueTimeMinutes?.let { formatTime(context, it) }
                        ?: stringResource(R.string.todo_due_time),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        if (state.showDueDateError || state.showDueTimeError) {
            Text(
                text = stringResource(R.string.todo_deadline_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.showPastDeadlineError) {
            Text(
                text = stringResource(R.string.todo_deadline_past_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SectionTitle(R.string.todo_priority)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TodoPriority.all.forEach { priority ->
                FilterChip(
                    selected = state.priority == priority,
                    onClick = { onPriorityChange(priority) },
                    label = { Text(stringResource(todoPriorityLabel(priority))) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionTitle(R.string.todo_reminder)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.todo_reminder_enabled), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.todo_reminder_explanation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.reminderOffsetMinutes != null,
                        onCheckedChange = onReminderEnabled,
                    )
                }
                if (state.reminderOffsetMinutes != null) {
                    TodoReminderOffset.all.forEach { offset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReminderOffsetChange(offset) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.reminderOffsetMinutes == offset,
                                onClick = { onReminderOffsetChange(offset) },
                            )
                            Text(stringResource(todoReminderOffsetLabel(offset)))
                        }
                    }
                    if (notificationPermissionMissing) {
                        Text(
                            stringResource(R.string.notifications_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        SectionTitle(R.string.habit_color)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HabitColorKey.all.forEach { key ->
                val color = habitColor(key)
                val colorDescription = stringResource(habitColorLabel(key))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (state.colorKey == key) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else Modifier
                        )
                        .semantics { contentDescription = colorDescription }
                        .clickable { onColorChange(key) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.colorKey == key) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        SectionTitle(R.string.habit_icon)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val quick = if (state.iconKey in HabitIconKey.quick) HabitIconKey.quick else {
                listOf(state.iconKey) + HabitIconKey.quick.take(5)
            }
            quick.forEach { key ->
                TodoIconOption(key, state.iconKey == key) { onIconChange(key) }
            }
        }
        TextButton(onClick = { showAllIcons = true }, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.show_more_icons))
        }

        if (state.id != 0L) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    stringResource(R.string.delete_todo),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(resource: Int) {
    Text(stringResource(resource), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun TodoIconOption(key: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            habitIcon(key),
            contentDescription = stringResource(habitIconLabel(key)),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun todoReminderOffsetLabel(offset: Int): Int = when (offset) {
    TodoReminderOffset.FIVE_MINUTES -> R.string.todo_reminder_5_minutes
    TodoReminderOffset.FIFTEEN_MINUTES -> R.string.todo_reminder_15_minutes
    TodoReminderOffset.THIRTY_MINUTES -> R.string.todo_reminder_30_minutes
    TodoReminderOffset.ONE_HOUR -> R.string.todo_reminder_1_hour
    TodoReminderOffset.ONE_DAY -> R.string.todo_reminder_1_day
    else -> R.string.todo_reminder_at_due
}

private const val DEFAULT_DUE_TIME_MINUTES = 9 * 60
