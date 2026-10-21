package dev.bugiel.kiroku.ui.workouts

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.bugiel.kiroku.di.AppContainer

fun NavGraphBuilder.workoutDestinations(
    container: AppContainer,
    workouts: WorkoutsViewModel,
    navController: NavHostController,
    onOpenSettings: () -> Unit,
) {
    composable("workouts") {
        WorkoutsScreen(
            workouts, onOpenPlan = { navController.navigate("workout-plan/$it") }, onOpenSettings = onOpenSettings,
            onOpenLibrary = { navController.navigate("workout-library") },
        )
    }
    composable("workout-library") {
        WorkoutLibraryScreen(workouts, onBack = { navController.popBackStack() },
            onOpenExercise = { navController.navigate("workout-exercise/$it") })
    }
    composable("workout-exercise/{exerciseId}") { entry ->
        val exerciseId = entry.arguments?.getString("exerciseId")?.toLongOrNull() ?: return@composable
        WorkoutExerciseScreen(
            exerciseId = exerciseId, viewModel = workouts, onBack = { navController.popBackStack() },
            onOpenEntry = { navController.navigate("workout-sets/$it") },
        )
    }
    composable("workout-plan/{planId}") { entry ->
        val planId = entry.arguments?.getString("planId")?.toLongOrNull() ?: return@composable
        WorkoutPlanScreen(
            planId, workouts, onBack = { navController.popBackStack() },
            onOpenDay = { navController.navigate("workout-day/$planId/$it") },
            onOpenExercise = { navController.navigate("workout-exercise/$planId/$it") },
        )
    }
    composable("workout-day/{planId}/{sessionId}") { entry ->
        val planId = entry.arguments?.getString("planId")?.toLongOrNull() ?: return@composable
        val sessionId = entry.arguments?.getString("sessionId")?.toLongOrNull() ?: return@composable
        WorkoutDayScreen(
            planId, sessionId, workouts, onBack = { navController.popBackStack() },
            onOpenEntry = { navController.navigate("workout-sets/$it") },
        )
    }
    composable("workout-exercise/{planId}/{exerciseId}") { entry ->
        val planId = entry.arguments?.getString("planId")?.toLongOrNull() ?: return@composable
        val exerciseId = entry.arguments?.getString("exerciseId")?.toLongOrNull() ?: return@composable
        WorkoutExerciseScreen(
            exerciseId, workouts, planId = planId, onBack = { navController.popBackStack() },
            onOpenEntry = { navController.navigate("workout-sets/$it") },
        )
    }
    composable("workout-sets/{entryId}") { entry ->
        val entryId = entry.arguments?.getString("entryId")?.toLongOrNull() ?: return@composable
        val editor: WorkoutSetEditorViewModel = viewModel(
            key = "workout-sets-$entryId",
            factory = viewModelFactory {
                initializer { WorkoutSetEditorViewModel(entryId, container.workoutRepository) }
            },
        )
        WorkoutSetEditorScreen(editor, onBack = { navController.popBackStack() })
    }
}
