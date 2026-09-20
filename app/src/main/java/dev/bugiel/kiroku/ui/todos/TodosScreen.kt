package dev.bugiel.kiroku.ui.todos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.TodoSections
import dev.bugiel.kiroku.domain.TodoStatusFilter
import dev.bugiel.kiroku.domain.model.Todo
import dev.bugiel.kiroku.domain.model.TodoPriority
import dev.bugiel.kiroku.ui.util.formatLongDate
import dev.bugiel.kiroku.ui.util.formatTime
import dev.bugiel.kiroku.ui.util.habitColor
import dev.bugiel.kiroku.ui.util.habitIcon
import java.time.LocalDate
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosScreen(
    viewModel: TodosViewModel,
    onCreateTodo: () -> Unit,
    onOpenTodo: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val completedMessage = stringResource(R.string.todo_completed_message)
    val undoLabel = stringResource(R.string.undo)
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    var completedShown by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is TodoEvent.Completed) {
                val result = snackbarHostState.showSnackbar(
                    message = completedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.undoCompletion(event.todoId)
            }
        }
    }
    LaunchedEffect(viewModel) {
        while (true) {
            viewModel.refreshTime()
            delay(60_000)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.todos), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.open_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTodo) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_todo))
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_todos)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    AnimatedVisibility(state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            TodoFilters(selected = state.filter, onSelect = viewModel::setFilter)

            if (state.sections.isEmpty) {
                TodoEmptyState(
                    hasQuery = state.query.isNotBlank(),
                    filter = state.filter,
                    modifier = Modifier.weight(1f),
                )
            } else {
                TodoList(
                    sections = state.sections,
                    completedExpanded = when (state.filter) {
                        TodoStatusFilter.COMPLETED -> completedShown
                        else -> completedExpanded
                    },
                    onToggleCompleted = {
                        if (state.filter == TodoStatusFilter.COMPLETED) completedShown = !completedShown
                        else completedExpanded = !completedExpanded
                    },
                    onOpen = onOpenTodo,
                    onComplete = viewModel::complete,
                    onReopen = viewModel::reopen,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TodoFilters(selected: TodoStatusFilter, onSelect: (TodoStatusFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TodoStatusFilter.entries.forEach { filter ->
            val label = when (filter) {
                TodoStatusFilter.OPEN -> R.string.todo_filter_open
                TodoStatusFilter.COMPLETED -> R.string.todo_filter_completed
                TodoStatusFilter.ALL -> R.string.todo_filter_all
            }
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun TodoList(
    sections: TodoSections,
    completedExpanded: Boolean,
    onToggleCompleted: () -> Unit,
    onOpen: (Long) -> Unit,
    onComplete: (Todo) -> Unit,
    onReopen: (Todo) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        todoSection(R.string.todo_section_overdue, sections.overdue, isOverdue = true, onOpen, onComplete)
        todoSection(R.string.todo_section_today, sections.today, isOverdue = false, onOpen, onComplete)
        todoSection(R.string.todo_section_tomorrow, sections.tomorrow, isOverdue = false, onOpen, onComplete)
        todoSection(R.string.todo_section_upcoming, sections.upcoming, isOverdue = false, onOpen, onComplete)

        if (sections.completed.isNotEmpty()) {
            item(key = "completed-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleCompleted)
                        .padding(top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.todo_section_completed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = sections.completed.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        if (completedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            if (completedExpanded) R.string.collapse_completed else R.string.expand_completed,
                        ),
                    )
                }
            }
            if (completedExpanded) {
                items(sections.completed, key = { "completed-${it.id}" }) { todo ->
                    TodoCard(todo, isOverdue = false, onOpen = { onOpen(todo.id) }, onAction = { onReopen(todo) })
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.todoSection(
    title: Int,
    todos: List<Todo>,
    isOverdue: Boolean,
    onOpen: (Long) -> Unit,
    onComplete: (Todo) -> Unit,
) {
    if (todos.isEmpty()) return
    item(key = "section-$title") {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        )
    }
    items(todos, key = { "open-${it.id}" }) { todo ->
        TodoCard(todo, isOverdue, onOpen = { onOpen(todo.id) }, onAction = { onComplete(todo) })
    }
}

@Composable
private fun TodoCard(todo: Todo, isOverdue: Boolean, onOpen: () -> Unit, onAction: () -> Unit) {
    val context = LocalContext.current
    val color = habitColor(todo.colorKey)
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(habitIcon(todo.iconKey), contentDescription = null, tint = color)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (todo.description.isNotBlank()) {
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.todo_due_date_time,
                        formatLongDate(context, LocalDate.ofEpochDay(todo.dueEpochDay)),
                        formatTime(context, todo.dueTimeMinutes),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.todo_priority_value,
                        stringResource(todoPriorityLabel(todo.priority)),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (todo.priority == TodoPriority.HIGH) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(
                onClick = onAction,
                modifier = Modifier.size(48.dp),
                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (todo.isCompleted) MaterialTheme.colorScheme.secondaryContainer else color,
                    contentColor = if (todo.isCompleted) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                ),
            ) {
                Icon(
                    if (todo.isCompleted) Icons.Default.Replay else Icons.Default.Check,
                    contentDescription = stringResource(
                        if (todo.isCompleted) R.string.reopen_todo else R.string.complete_todo,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TodoEmptyState(
    hasQuery: Boolean,
    filter: TodoStatusFilter,
    modifier: Modifier = Modifier,
) {
    val title = when {
        hasQuery -> R.string.no_search_results_title
        filter == TodoStatusFilter.COMPLETED -> R.string.no_completed_todos_title
        else -> R.string.no_todos_title
    }
    val message = when {
        hasQuery -> R.string.no_search_results_message
        filter == TodoStatusFilter.COMPLETED -> R.string.no_completed_todos_message
        else -> R.string.no_todos_message
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (hasQuery) Icons.Default.Search else Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(0.7f),
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun todoPriorityLabel(priority: String): Int = when (priority) {
    TodoPriority.LOW -> R.string.todo_priority_low
    TodoPriority.MEDIUM -> R.string.todo_priority_medium
    TodoPriority.HIGH -> R.string.todo_priority_high
    else -> R.string.todo_priority_none
}
