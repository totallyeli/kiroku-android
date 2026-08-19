package dev.bugiel.kiroku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.bugiel.kiroku.data.repository.ThemeMode
import dev.bugiel.kiroku.data.repository.ThemeSettings
import dev.bugiel.kiroku.di.AppContainer
import dev.bugiel.kiroku.ui.components.ThemeSettingsDialog
import dev.bugiel.kiroku.ui.notes.NoteEditorScreen
import dev.bugiel.kiroku.ui.notes.NoteEditorViewModel
import dev.bugiel.kiroku.ui.notes.NotesScreen
import dev.bugiel.kiroku.ui.notes.NotesViewModel
import dev.bugiel.kiroku.ui.theme.KirokuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KirokuRoot((application as KirokuApplication).container)
        }
    }
}

@Composable
private fun KirokuRoot(container: AppContainer) {
    val settings by container.settingsRepository.themeSettings.collectAsStateWithLifecycle(ThemeSettings())
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    var showThemeDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
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
        NotesNavigation(container = container, onOpenSettings = { showThemeDialog = true })
        if (showThemeDialog) {
            ThemeSettingsDialog(
                settings = settings,
                onModeChange = { scope.launch { container.settingsRepository.setThemeMode(it) } },
                onDynamicColorsChange = {
                    scope.launch { container.settingsRepository.setDynamicColors(it) }
                },
                onDismiss = { showThemeDialog = false },
            )
        }
    }
}

@Composable
private fun NotesNavigation(container: AppContainer, onOpenSettings: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "notes") {
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
                        NoteEditorViewModel(noteId, container.noteRepository, container.dateClock)
                    }
                },
            )
            NoteEditorScreen(viewModel = editorViewModel, onNavigateBack = navController::popBackStack)
        }
    }
}
