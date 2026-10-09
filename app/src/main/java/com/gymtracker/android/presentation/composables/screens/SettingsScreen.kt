package com.gymtracker.android.presentation.composables.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.presentation.composables.components.*
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun SettingsScreen(
    settings: Settings, busy: Boolean, onChange: (Settings) -> Unit,
    onExport: () -> Unit, onDeleteAll: () -> Unit
) {
    var deleting by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrackerCard {
            Text("Weight Unit", style = MaterialTheme.typography.titleMedium)
            Row {
                WeightUnit.entries.forEach { unit ->
                    TextButton(onClick = { onChange(settings.copy(unit = unit)) }, enabled = !busy) {
                        Text(unit.label + if (settings.unit == unit) " ✓" else "")
                    }
                }
            }
            Text("Weights are stored in pounds and converted for entry and display.", style = MaterialTheme.typography.bodySmall)
        }
        TrackerCard {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row {
                Appearance.entries.forEach { appearance ->
                    TextButton(onClick = { onChange(settings.copy(appearance = appearance)) }, enabled = !busy) {
                        Text(appearance.name.lowercase().replaceFirstChar { it.uppercase() } + if (settings.appearance == appearance) " ✓" else "")
                    }
                }
            }
        }
        TrackerCard {
            Text("Default Rest", style = MaterialTheme.typography.titleMedium)
            Row {
                listOf(0, 60, 90, 120, 180).forEach { seconds ->
                    TextButton(onClick = { onChange(settings.copy(restSeconds = seconds)) }, enabled = !busy,
                        contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text((if (seconds == 0) "Off" else "${seconds}s") + if (settings.restSeconds == seconds) " ✓" else "")
                    }
                }
            }
            Text("Default preference saved for future rest timer support.", style = MaterialTheme.typography.bodySmall)
            SettingsToggle("Keep awake during workouts", settings.keepAwake, !busy) { onChange(settings.copy(keepAwake = it)) }
            SettingsToggle("Confirm before discarding", settings.confirmDiscard, !busy) { onChange(settings.copy(confirmDiscard = it)) }
        }
        TrackerCard {
            Text("Your Data", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onExport, enabled = !busy) { Text("Export CSV") }
            TextButton(onClick = { deleting = true }, enabled = !busy) { Text("Delete All Data", color = MaterialTheme.colorScheme.error) }
        }
        Text("GymTracker 1.0.0 • Offline • Built with Kotlin and Jetpack Compose")
    }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false }, title = { Text("Delete all data?") },
        text = { Text("This removes every workout, custom exercise and plan edit. Preset exercises and plans will be restored.") },
        confirmButton = { TextButton(onClick = { onDeleteAll(); deleting = false }, enabled = !busy) { Text("Delete All") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } }
    )
}
@Composable
fun SettingsToggle(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f).padding(top = 12.dp))
        Switch(checked, onCheckedChange = onChange, enabled = enabled)
    }
}
@Preview(showBackground = true, heightDp = 900)
@Preview(showBackground = true, heightDp = 900, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() { GymTrackerTheme { SettingsScreen(Settings(), false, {}, {}, {}) } }
