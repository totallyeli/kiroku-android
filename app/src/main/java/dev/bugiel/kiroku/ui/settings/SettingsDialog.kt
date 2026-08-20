package dev.bugiel.kiroku.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.data.repository.ThemeMode

@Composable
fun SettingsDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val settings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (updateState is UpdateUiState.Idle) viewModel.checkForUpdates()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 580.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                AppLanguage.entries.forEach { language ->
                    val label = when (language) {
                        AppLanguage.SYSTEM -> R.string.language_system
                        AppLanguage.ENGLISH -> R.string.language_english
                        AppLanguage.GERMAN -> R.string.language_german
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAppLanguage(language) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = appLanguage == language,
                            onClick = { viewModel.setAppLanguage(language) },
                        )
                        Text(text = stringResource(label), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Text(
                    text = stringResource(R.string.theme_settings),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = settings.mode == mode, onClick = { viewModel.setThemeMode(mode) })
                        Text(text = stringResource(label), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDynamicColors(!settings.dynamicColors) }
                            .padding(top = 6.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.dynamic_colors))
                        Switch(
                            checked = settings.dynamicColors,
                            onCheckedChange = viewModel::setDynamicColors,
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Text(
                    text = stringResource(R.string.updates),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.current_version, viewModel.currentVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                UpdateSection(
                    state = updateState,
                    onCheck = viewModel::checkForUpdates,
                    onDownload = viewModel::downloadUpdate,
                    onInstall = viewModel::installUpdate,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun UpdateSection(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onDownload: (dev.bugiel.kiroku.update.GitHubRelease) -> Unit,
    onInstall: (UpdateUiState.Downloaded) -> Unit,
) {
    when (state) {
        UpdateUiState.Idle -> TextButton(onClick = onCheck) { Text(stringResource(R.string.check_for_updates)) }
        UpdateUiState.Checking -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.checking_for_updates), modifier = Modifier.padding(top = 8.dp))
        }
        UpdateUiState.UpToDate -> {
            Text(stringResource(R.string.app_up_to_date))
            TextButton(onClick = onCheck) { Text(stringResource(R.string.check_again)) }
        }
        is UpdateUiState.Available -> {
            Text(
                text = stringResource(R.string.update_available, state.release.version.toString()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.release.notes.isNotBlank()) {
                Text(
                    text = state.release.notes,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            TextButton(onClick = { onDownload(state.release) }) {
                Text(stringResource(R.string.download_update))
            }
        }
        is UpdateUiState.Downloading -> {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.downloading_update, (state.progress * 100).toInt()),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        is UpdateUiState.Downloaded -> {
            Text(stringResource(R.string.update_ready))
            if (state.permissionSettingsOpened) {
                Text(
                    text = stringResource(R.string.allow_unknown_apps_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            TextButton(onClick = { onInstall(state) }) { Text(stringResource(R.string.install_update)) }
        }
        is UpdateUiState.Failed -> {
            Text(
                text = state.message.ifBlank { stringResource(R.string.update_error) },
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onCheck) { Text(stringResource(R.string.try_again)) }
        }
    }
}
