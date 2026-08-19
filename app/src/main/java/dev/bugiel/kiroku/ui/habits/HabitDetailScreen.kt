package dev.bugiel.kiroku.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.Habit
import dev.bugiel.kiroku.domain.model.HabitStats
import dev.bugiel.kiroku.ui.util.formatLongDate
import dev.bugiel.kiroku.ui.util.formatMonth
import dev.bugiel.kiroku.ui.util.habitColor
import dev.bugiel.kiroku.ui.util.habitIcon
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: HabitDetailViewModel,
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val habit = state.habit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (habit != null) {
                        IconButton(onClick = { onEdit(habit.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_habit))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (habit == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            HabitDetailContent(
                habit = habit,
                stats = state.stats,
                completedDays = state.completedDays,
                todayEpochDay = state.todayEpochDay,
                month = state.visibleMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onToggleDate = viewModel::toggleDate,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HabitDetailContent(
    habit: Habit,
    stats: HabitStats,
    completedDays: Set<Long>,
    todayEpochDay: Long,
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleDate: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = habitColor(habit.colorKey)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).background(color, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(habitIcon(habit.iconKey), contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(habit.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (habit.description.isNotBlank()) {
                            Text(habit.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        habit.dueTimeMinutes?.let { minutes ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = stringResource(R.string.habit_reminder_at, formatDueTime(minutes)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell(R.string.current_streak, stats.currentStreak, Modifier.weight(1f))
                    StatCell(R.string.longest_streak, stats.longestStreak, Modifier.weight(1f))
                    StatCell(R.string.total_days, stats.totalCompletedDays, Modifier.weight(1f))
                }
            }
        }

        CalendarCard(
            habit = habit,
            completedDays = completedDays,
            todayEpochDay = todayEpochDay,
            month = month,
            color = color,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onToggleDate = onToggleDate,
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatCell(label: Int, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            pluralStringResource(R.plurals.streak_days, value, value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CalendarCard(
    habit: Habit,
    completedDays: Set<Long>,
    todayEpochDay: Long,
    month: YearMonth,
    color: Color,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleDate: (Long) -> Unit,
) {
    val context = LocalContext.current
    val todayMonth = YearMonth.from(LocalDate.ofEpochDay(todayEpochDay))
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_month),
                    )
                }
                Text(formatMonth(context, month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNextMonth, enabled = month < todayMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_month),
                    )
                }
            }

            val weekdayLabels = listOf(
                R.string.weekday_monday,
                R.string.weekday_tuesday,
                R.string.weekday_wednesday,
                R.string.weekday_thursday,
                R.string.weekday_friday,
                R.string.weekday_saturday,
                R.string.weekday_sunday,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayLabels.forEach { label ->
                    Text(
                        text = stringResource(label),
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val offset = month.atDay(1).dayOfWeek.value - 1
            val cells = MutableList<LocalDate?>(offset) { null }.apply {
                repeat(month.lengthOfMonth()) { add(month.atDay(it + 1)) }
                while (size % 7 != 0) add(null)
            }
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            CalendarDay(
                                date = date,
                                completed = date.toEpochDay() in completedDays,
                                todayEpochDay = todayEpochDay,
                                createdEpochDay = habit.createdEpochDay,
                                color = color,
                                onClick = { onToggleDate(date.toEpochDay()) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Legend(color, R.string.calendar_completed, filled = true)
                Legend(color, R.string.calendar_today, filled = false)
                Legend(MaterialTheme.colorScheme.outlineVariant, R.string.calendar_future, filled = true)
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    completed: Boolean,
    todayEpochDay: Long,
    createdEpochDay: Long,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val epochDay = date.toEpochDay()
    val isToday = epochDay == todayEpochDay
    val isFuture = epochDay > todayEpochDay
    val beforeCreation = epochDay < createdEpochDay
    val enabled = !isFuture && !beforeCreation
    val formatted = formatLongDate(context, date)
    val description = stringResource(
        when {
            isFuture -> R.string.calendar_day_future
            beforeCreation -> R.string.calendar_before_creation
            completed -> R.string.calendar_day_completed
            else -> R.string.calendar_day_open
        },
        formatted,
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .then(if (completed) Modifier.background(color) else Modifier)
            .then(if (isToday) Modifier.border(2.dp, color, CircleShape) else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (completed) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isToday || completed) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun Legend(color: Color, label: Int, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .then(if (filled) Modifier.background(color, CircleShape) else Modifier.border(2.dp, color, CircleShape)),
        )
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

private fun formatDueTime(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)
