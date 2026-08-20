package dev.bugiel.kiroku

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.bugiel.kiroku.data.repository.ThemeMode
import dev.bugiel.kiroku.data.repository.ThemeSettings
import dev.bugiel.kiroku.di.AppContainer
import dev.bugiel.kiroku.ui.habits.HabitDetailScreen
import dev.bugiel.kiroku.ui.habits.HabitDetailViewModel
import dev.bugiel.kiroku.ui.habits.HabitEditorScreen
import dev.bugiel.kiroku.ui.habits.HabitEditorViewModel
import dev.bugiel.kiroku.ui.habits.HabitsScreen
import dev.bugiel.kiroku.ui.habits.HabitsViewModel
import dev.bugiel.kiroku.ui.notes.NoteEditorScreen
import dev.bugiel.kiroku.ui.notes.NoteEditorViewModel
import dev.bugiel.kiroku.ui.notes.NotesScreen
import dev.bugiel.kiroku.ui.notes.NotesViewModel
import dev.bugiel.kiroku.ui.settings.SettingsDialog
import dev.bugiel.kiroku.ui.settings.SettingsViewModel
import dev.bugiel.kiroku.ui.theme.KirokuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {
    private val reminderHabitId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderHabitId.value = intent.getLongExtra(EXTRA_REMINDER_HABIT_ID, 0L).takeIf { it != 0L }
        enableEdgeToEdge()
        setContent {
            val openHabitId by reminderHabitId.collectAsStateWithLifecycle()
            KirokuRoot(
                container = (application as KirokuApplication).container,
                openHabitId = openHabitId,
                onHabitOpened = { reminderHabitId.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        reminderHabitId.value = intent.getLongExtra(EXTRA_REMINDER_HABIT_ID, 0L).takeIf { it != 0L }
    }

    companion object {
        const val EXTRA_REMINDER_HABIT_ID = "dev.bugiel.kiroku.extra.OPEN_REMINDER_HABIT_ID"
    }
}

@Composable
private fun KirokuRoot(
    container: AppContainer,
    openHabitId: Long?,
    onHabitOpened: () -> Unit,
) {
    val settings by container.settingsRepository.themeSettings.collectAsStateWithLifecycle(ThemeSettings())
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    updateRepository = container.appUpdateRepository,
                )
            }
        },
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    SideEffect {
        WindowCompat.getInsetsController((view.context as MainActivity).window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) container.todayProvider.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(container.todayProvider) {
        while (true) {
            delay(60_000)
            container.todayProvider.refresh()
        }
    }

    KirokuTheme(darkTheme = dark, dynamicColor = settings.dynamicColors) {
        KirokuNavigation(
            container = container,
            openHabitId = openHabitId,
            onHabitOpened = onHabitOpened,
            onOpenSettings = { showSettingsDialog = true },
        )
        if (showSettingsDialog) {
            SettingsDialog(
                viewModel = settingsViewModel,
                onDismiss = { showSettingsDialog = false },
            )
        }
    }
}

@Composable
private fun KirokuNavigation(
    container: AppContainer,
    openHabitId: Long?,
    onHabitOpened: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val rootRoutes = setOf("notes", "habits")

    LaunchedEffect(openHabitId) {
        openHabitId?.let { habitId ->
            navController.navigate("habit/$habitId") { launchSingleTop = true }
            onHabitOpened()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in rootRoutes) {
                NavigationBar {
                    listOf(
                        Triple("notes", R.string.notes, Icons.AutoMirrored.Filled.Notes),
                        Triple("habits", R.string.habits, Icons.Default.CheckCircle),
                    ).forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
        },
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = "notes",
            modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding()),
        ) {
            composable("notes") {
                val notesViewModel: NotesViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { NotesViewModel(container.noteRepository, container.dateClock) }
                    },
                )
                NotesScreen(
                    viewModel = notesViewModel,
                    onCreateNote = { navController.navigate("note/0") },
                    onOpenNote = { navController.navigate("note/$it") },
                    onOpenSettings = onOpenSettings,
                )
            }
            composable("note/{noteId}") { entry ->
                val noteId = entry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
                val editorViewModel: NoteEditorViewModel = viewModel(
                    key = "note-$noteId",
                    factory = viewModelFactory {
                        initializer {
                            NoteEditorViewModel(
                                noteId = noteId,
                                repository = container.noteRepository,
                                attachmentRepository = container.attachmentRepository,
                                dateClock = container.dateClock,
                            )
                        }
                    },
                )
                NoteEditorScreen(viewModel = editorViewModel, onNavigateBack = navController::popBackStack)
            }
            composable("habits") {
                val habitsViewModel: HabitsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            HabitsViewModel(
                                container.habitRepository,
                                container.todayProvider,
                                container.dateClock,
                            )
                        }
                    },
                )
                HabitsScreen(
                    viewModel = habitsViewModel,
                    onAddHabit = { navController.navigate("habit-edit/0") },
                    onOpenHabit = { navController.navigate("habit/$it") },
                    onOpenSettings = onOpenSettings,
                )
            }
            composable("habit/{habitId}") { entry ->
                val habitId = entry.arguments?.getString("habitId")?.toLongOrNull() ?: return@composable
                val detailViewModel: HabitDetailViewModel = viewModel(
                    key = "habit-detail-$habitId",
                    factory = viewModelFactory {
                        initializer {
                            HabitDetailViewModel(
                                habitId,
                                container.habitRepository,
                                container.todayProvider,
                                container.dateClock,
                            )
                        }
                    },
                )
                HabitDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = navController::popBackStack,
                    onEdit = { navController.navigate("habit-edit/$it") },
                )
            }
            composable("habit-edit/{habitId}") { entry ->
                val habitId = entry.arguments?.getString("habitId")?.toLongOrNull() ?: 0L
                val editorViewModel: HabitEditorViewModel = viewModel(
                    key = "habit-editor-$habitId",
                    factory = viewModelFactory {
                        initializer {
                            HabitEditorViewModel(
                                habitId = habitId,
                                repository = container.habitRepository,
                                dateClock = container.dateClock,
                                reminderScheduler = container.habitReminderScheduler,
                            )
                        }
                    },
                )
                HabitEditorScreen(
                    viewModel = editorViewModel,
                    onSaved = { navController.popBackStack() },
                    onDeleted = {
                        navController.navigate("habits") {
                            popUpTo("habits") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = navController::popBackStack,
                )
            }
        }
    }
}
