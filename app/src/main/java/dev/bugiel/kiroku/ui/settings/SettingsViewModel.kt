package dev.bugiel.kiroku.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.bugiel.kiroku.data.repository.SettingsRepository
import dev.bugiel.kiroku.data.repository.ThemeMode
import dev.bugiel.kiroku.data.repository.ThemeSettings
import dev.bugiel.kiroku.update.AppUpdateRepository
import dev.bugiel.kiroku.update.GitHubRelease
import dev.bugiel.kiroku.update.InstallResult
import dev.bugiel.kiroku.update.UpdateCheckResult
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val release: GitHubRelease) : UpdateUiState
    data class Downloading(val release: GitHubRelease, val progress: Float) : UpdateUiState
    data class Downloaded(
        val release: GitHubRelease,
        val file: File,
        val permissionSettingsOpened: Boolean = false,
    ) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    GERMAN("de"),
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val updateRepository: AppUpdateRepository,
) : ViewModel() {
    val themeSettings: StateFlow<ThemeSettings> = settingsRepository.themeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSettings())
    val currentVersionName: String = updateRepository.currentVersionName
    val updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    private val mutableAppLanguage = MutableStateFlow(currentAppLanguage())
    val appLanguage: StateFlow<AppLanguage> = mutableAppLanguage.asStateFlow()

    fun setAppLanguage(language: AppLanguage) {
        if (language == mutableAppLanguage.value) return
        mutableAppLanguage.value = language
        AppCompatDelegate.setApplicationLocales(
            language.languageTag?.let(LocaleListCompat::forLanguageTags)
                ?: LocaleListCompat.getEmptyLocaleList(),
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColors(enabled) }
    }

    fun checkForUpdates() {
        if (updateState.value is UpdateUiState.Checking || updateState.value is UpdateUiState.Downloading) return
        viewModelScope.launch {
            updateState.value = UpdateUiState.Checking
            updateState.value = runCatching { updateRepository.checkForUpdate() }
                .fold(
                    onSuccess = { result ->
                        when (result) {
                            UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                            is UpdateCheckResult.Available -> UpdateUiState.Available(result.release)
                        }
                    },
                    onFailure = { UpdateUiState.Failed(it.message.orEmpty()) },
                )
        }
    }

    fun downloadUpdate(release: GitHubRelease) {
        if (updateState.value is UpdateUiState.Downloading) return
        viewModelScope.launch {
            updateState.value = UpdateUiState.Downloading(release, 0f)
            runCatching {
                updateRepository.download(release) { progress ->
                    updateState.value = UpdateUiState.Downloading(release, progress)
                }
            }.onSuccess { file ->
                updateState.value = UpdateUiState.Downloaded(release, file)
            }.onFailure {
                updateState.value = UpdateUiState.Failed(it.message.orEmpty())
            }
        }
    }

    fun installUpdate(downloaded: UpdateUiState.Downloaded) {
        runCatching { updateRepository.install(downloaded.file) }
            .onSuccess { result ->
                if (result == InstallResult.PERMISSION_SETTINGS_OPENED) {
                    updateState.value = downloaded.copy(permissionSettingsOpened = true)
                }
            }
            .onFailure { updateState.value = UpdateUiState.Failed(it.message.orEmpty()) }
    }

    private fun currentAppLanguage(): AppLanguage {
        val language = AppCompatDelegate.getApplicationLocales()[0]?.language
        return when (language) {
            AppLanguage.ENGLISH.languageTag -> AppLanguage.ENGLISH
            AppLanguage.GERMAN.languageTag -> AppLanguage.GERMAN
            else -> AppLanguage.SYSTEM
        }
    }
}
