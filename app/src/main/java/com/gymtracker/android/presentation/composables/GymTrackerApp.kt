package com.gymtracker.android.presentation.composables

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtracker.android.presentation.*
import com.gymtracker.android.domain.service.CsvExporter
import com.gymtracker.android.presentation.composables.components.*
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.screens.*
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun GymTrackerApp(model: TrackerViewModel, onExport: (String) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var exportError by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current
    val awake = state.settings.keepAwake && state.tab == Tab.WORKOUT
    DisposableEffect(view, awake) {
        val window = (view.context as? android.app.Activity)?.window
        if (awake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    GymTrackerTheme(state.settings.appearance) {
        TrackerScaffold(state, model::selectTab) {
            if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            else when (state.tab) {
                Tab.HOME -> HomeScreen(state.data, state.settings.unit) { model.selectTab(Tab.WORKOUT) }
                Tab.WORKOUT -> WorkoutScreen(state, WorkoutActions(
                    model::name, model::notes, model::date, model::addExercise, model::removeExercise,
                    model::addSet, model::removeSet, model::editSet, model::completeExercise,
                    model::finish, model::createExercise, model::cancelHistoryEditor
                ))
                Tab.PLAN -> PlanScreen(state.data, state.busy, model::loadPlan, model::savePlan, model::deletePlan)
                Tab.HISTORY -> HistoryScreen(
                    state.data, state.settings.unit, state.busy,
                    { model.startHistoryWorkout() }, { model.startHistoryWorkout(it) }, model::deleteWorkout
                )
                Tab.PROGRESS -> ProgressScreen(state.data, state.settings.unit)
                Tab.SETTINGS -> SettingsScreen(state.settings, state.busy, model::settings, {
                    try { onExport(CsvExporter.export(state.data)) }
                    catch (e: Exception) { exportError = e.message ?: "Couldn't export. Please try again." }
                }, model::deleteAllData)
            }
        }
        if (state.confirmFinish) AlertDialog(
            onDismissRequest = model::dismissFinish, title = { Text("Remove incomplete sets?") },
            text = { Text("${state.session.invalidSetCount} incomplete sets won't be saved.") },
            confirmButton = { TextButton(onClick = model::save, enabled = !state.busy) { Text("Discard & Save") } },
            dismissButton = { TextButton(onClick = model::dismissFinish, enabled = !state.busy) { Text("Keep Editing") } }
        )
        state.completed?.let { workout ->
            AlertDialog(
                onDismissRequest = model::dismissCompleted, title = { Text("Workout Saved") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(workout.name)
                        Text("${workout.exerciseCount} exercises • ${workout.sets.size} sets")
                        Text("Volume: ${Formatters.weight(workout.volume, state.settings.unit)}")
                    }
                },
                confirmButton = { TextButton(onClick = model::dismissCompleted) { Text("Done") } }
            )
        }
        val error = state.error ?: exportError
        if (error != null) AlertDialog(
            onDismissRequest = { model.dismissError(); exportError = null },
            title = { Text("Something went wrong") }, text = { Text(error) },
            confirmButton = { TextButton(onClick = { model.dismissError(); exportError = null }) { Text("OK") } }
        )
    }
    BackHandler(enabled = state.tab != Tab.HOME && !state.historyEditor) { model.selectTab(Tab.HOME) }
}
@Composable
fun TrackerScaffold(state: TrackerUiState, onTab: (Tab) -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            Surface {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
                    Text(state.tab.label, style = MaterialTheme.typography.headlineSmall)
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                LazyRow(
                    Modifier.fillMaxWidth().navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    items(Tab.entries, key = { it.name }) { tab ->
                        TextButton(
                            onClick = { onTab(tab) },
                            enabled = !state.busy && (!state.historyEditor || tab == Tab.WORKOUT),
                            modifier = Modifier.widthIn(min = 64.dp)
                        ) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text(tab.symbol)
                                Text(tab.label, style = MaterialTheme.typography.labelSmall,
                                    color = if (state.tab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    ) { padding -> Box(Modifier.fillMaxSize().padding(padding).imePadding()) { content() } }
}
@Preview(showBackground = true)
@Composable
private fun AppPreview() { GymTrackerTheme { TrackerScaffold(PreviewData.state, {}) {
    HomeScreen(PreviewData.data, PreviewData.state.settings.unit, {})
} } }
