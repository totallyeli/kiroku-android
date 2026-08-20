package dev.bugiel.kiroku.ui.habits

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.HabitColorKey
import dev.bugiel.kiroku.domain.model.HabitIconKey
import dev.bugiel.kiroku.domain.model.HabitRepeatType
import dev.bugiel.kiroku.domain.model.HabitWeekdays
import dev.bugiel.kiroku.ui.util.formatLongDate
import dev.bugiel.kiroku.ui.util.habitColor
import dev.bugiel.kiroku.ui.util.habitColorLabel
import dev.bugiel.kiroku.ui.util.habitIcon
import dev.bugiel.kiroku.ui.util.habitIconLabel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    viewModel: HabitEditorViewModel,
    onSaved: (Long) -> Unit,
    onDeleted: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var notificationPermissionMissing by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationPermissionMissing = !granted }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.habit_save_error)

    LaunchedEffect(state.hasSaveError) {
        if (state.hasSaveError) snackbarHostState.showSnackbar(errorMessage)
    }
    BackHandler(onBack = onNavigateBack)

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_habit_title)) },
            text = { Text(stringResource(R.string.delete_habit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.delete(onDeleted)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (state.id == 0L) R.string.new_habit else R.string.edit_habit),
                    )
                },
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
            HabitEditorContent(
                state = state,
                onNameChange = viewModel::setName,
                onDescriptionChange = viewModel::setDescription,
                onIconChange = viewModel::setIcon,
                onColorChange = viewModel::setColor,
                onDueTimeChange = { minutes ->
                    viewModel.setDueTime(minutes)
                    if (
                        minutes != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        notificationPermissionMissing
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRepeatTypeChange = viewModel::setRepeatType,
                onRepeatIntervalChange = viewModel::setRepeatInterval,
                onToggleWeekday = viewModel::toggleWeekday,
                notificationPermissionMissing = notificationPermissionMissing,
                onDelete = { showDeleteConfirmation = true },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HabitEditorContent(
    state: HabitEditorState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onDueTimeChange: (Int?) -> Unit,
    onRepeatTypeChange: (String) -> Unit,
    onRepeatIntervalChange: (Int) -> Unit,
    onToggleWeekday: (Int) -> Unit,
    notificationPermissionMissing: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showAllIcons by rememberSaveable { mutableStateOf(false) }
    val quickIcons = if (state.iconKey in HabitIconKey.quick) {
        HabitIconKey.quick
    } else {
        listOf(state.iconKey) + HabitIconKey.quick.take(5)
    }

    if (showAllIcons) {
        AlertDialog(
            onDismissRequest = { showAllIcons = false },
            title = { Text(stringResource(R.string.choose_habit_icon)) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(HabitIconKey.all, key = { it }) { key ->
                        HabitIconOption(
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
    ) {
        Text(
            text = stringResource(
                R.string.created_on,
                formatLongDate(context, LocalDate.ofEpochDay(state.createdEpochDay)),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.habit_name)) },
            placeholder = { Text(stringResource(R.string.habit_name_example)) },
            isError = state.showNameError,
            supportingText = if (state.showNameError) {
                { Text(stringResource(R.string.habit_name_required)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.habit_description)) },
            placeholder = { Text(stringResource(R.string.habit_description_example)) },
            minLines = 2,
            maxLines = 4,
        )

        SelectionTitle(R.string.habit_icon)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            quickIcons.forEach { key ->
                HabitIconOption(
                    key = key,
                    selected = state.iconKey == key,
                    onClick = { onIconChange(key) },
                )
            }
        }
        TextButton(
            onClick = { showAllIcons = true },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.show_more_icons))
        }

        SelectionTitle(R.string.habit_color)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HabitColorKey.all.forEach { key ->
                val color = habitColor(key)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (state.colorKey == key) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else Modifier
                        )
                        .clickable(
                            onClickLabel = stringResource(habitColorLabel(key)),
                            onClick = { onColorChange(key) },
                        ),
                )
            }
        }

        HabitMoreOptions(
            dueTimeMinutes = state.dueTimeMinutes,
            repeatType = state.repeatType,
            repeatIntervalDays = state.repeatIntervalDays,
            repeatWeekdaysMask = state.repeatWeekdaysMask,
            showScheduleError = state.showScheduleError,
            notificationPermissionMissing = notificationPermissionMissing,
            onDueTimeChange = onDueTimeChange,
            onRepeatTypeChange = onRepeatTypeChange,
            onRepeatIntervalChange = onRepeatIntervalChange,
            onToggleWeekday = onToggleWeekday,
        )

        if (state.id != 0L) {
            Spacer(Modifier.height(40.dp))
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = stringResource(R.string.delete_habit),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun HabitIconOption(
    key: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            )
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier,
            )
            .clickable(
                onClickLabel = stringResource(habitIconLabel(key)),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            habitIcon(key),
            contentDescription = stringResource(habitIconLabel(key)),
            tint = if (selected) MaterialTheme.colorScheme.primary else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitMoreOptions(
    dueTimeMinutes: Int?,
    repeatType: String,
    repeatIntervalDays: Int,
    repeatWeekdaysMask: Int,
    showScheduleError: Boolean,
    notificationPermissionMissing: Boolean,
    onDueTimeChange: (Int?) -> Unit,
    onRepeatTypeChange: (String) -> Unit,
    onRepeatIntervalChange: (Int) -> Unit,
    onToggleWeekday: (Int) -> Unit,
) {
    var expanded by rememberSaveable {
        mutableStateOf(dueTimeMinutes != null || repeatType != HabitRepeatType.DAILY)
    }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    HorizontalDivider(modifier = Modifier.padding(top = 28.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.more_options),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = repeatSummary(repeatType, repeatIntervalDays, repeatWeekdaysMask),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = stringResource(
                if (expanded) R.string.collapse_more_options else R.string.expand_more_options,
            ),
        )
    }

    AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(
                text = stringResource(R.string.repeat_schedule),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.repeat_schedule_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            HabitRepeatType.all.forEach { type ->
                val label = when (type) {
                    HabitRepeatType.WEEKLY -> R.string.repeat_weekly
                    HabitRepeatType.INTERVAL -> R.string.repeat_interval
                    else -> R.string.repeat_daily
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRepeatTypeChange(type) }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = repeatType == type,
                        onClick = { onRepeatTypeChange(type) },
                    )
                    Text(stringResource(label), modifier = Modifier.padding(start = 6.dp))
                }
            }

            if (repeatType == HabitRepeatType.WEEKLY) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(HabitWeekdays.ordered, key = { it }) { weekday ->
                        FilterChip(
                            selected = repeatWeekdaysMask and weekday != 0,
                            onClick = { onToggleWeekday(weekday) },
                            label = { Text(stringResource(weekdayLabel(weekday))) },
                        )
                    }
                }
                if (showScheduleError) {
                    Text(
                        text = stringResource(R.string.choose_at_least_one_weekday),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (repeatType == HabitRepeatType.INTERVAL) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { onRepeatIntervalChange(repeatIntervalDays - 1) },
                        enabled = repeatIntervalDays > HabitEditorViewModel.MIN_REPEAT_INTERVAL,
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease_interval))
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.repeat_every_days,
                            repeatIntervalDays,
                            repeatIntervalDays,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(
                        onClick = { onRepeatIntervalChange(repeatIntervalDays + 1) },
                        enabled = repeatIntervalDays < HabitEditorViewModel.MAX_REPEAT_INTERVAL,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase_interval))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = stringResource(R.string.habit_reminder),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.habit_reminder_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            if (dueTimeMinutes == null) {
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Text(
                        text = stringResource(R.string.choose_reminder_time),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.habit_reminder_at, formatDueTime(dueTimeMinutes)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Text(stringResource(R.string.change_reminder_time))
                    }
                    TextButton(onClick = { onDueTimeChange(null) }) {
                        Text(stringResource(R.string.remove_reminder))
                    }
                }
                if (notificationPermissionMissing) {
                    Text(
                        text = stringResource(R.string.notifications_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        HabitTimePickerDialog(
            initialMinutes = dueTimeMinutes ?: DEFAULT_REMINDER_TIME,
            onConfirm = { minutes ->
                showTimePicker = false
                onDueTimeChange(minutes)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun repeatSummary(type: String, intervalDays: Int, weekdaysMask: Int): String {
    return when (type) {
        HabitRepeatType.WEEKLY -> {
            val selected = HabitWeekdays.ordered.filter { weekdaysMask and it != 0 }
            if (selected.isEmpty()) {
                stringResource(R.string.repeat_weekly)
            } else {
                selected.map { stringResource(weekdayLabel(it)) }.joinToString(", ")
            }
        }
        HabitRepeatType.INTERVAL -> pluralStringResource(
            R.plurals.repeat_every_days,
            intervalDays,
            intervalDays,
        )
        else -> stringResource(R.string.repeat_daily)
    }
}

private fun weekdayLabel(dayMask: Int): Int = when (dayMask) {
    HabitWeekdays.MONDAY -> R.string.weekday_monday
    HabitWeekdays.TUESDAY -> R.string.weekday_tuesday
    HabitWeekdays.WEDNESDAY -> R.string.weekday_wednesday
    HabitWeekdays.THURSDAY -> R.string.weekday_thursday
    HabitWeekdays.FRIDAY -> R.string.weekday_friday
    HabitWeekdays.SATURDAY -> R.string.weekday_saturday
    else -> R.string.weekday_sunday
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitTimePickerDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_reminder_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun formatDueTime(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)

private const val DEFAULT_REMINDER_TIME = 9 * 60

@Composable
private fun SelectionTitle(resource: Int) {
    Text(
        text = stringResource(resource),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 26.dp, bottom = 12.dp),
    )
}
