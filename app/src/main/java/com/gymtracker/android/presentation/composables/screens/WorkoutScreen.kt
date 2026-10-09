package com.gymtracker.android.presentation.composables.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtracker.android.domain.service.WorkoutService
import com.gymtracker.android.presentation.*
import com.gymtracker.android.presentation.composables.components.*
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun WorkoutScreen(state: TrackerUiState, actions: WorkoutActions) {
    var query by rememberSaveable { mutableStateOf("") }
    var cancelConfirm by remember { mutableStateOf(false) }
    val session = state.session
    val filtered = session.exercises.filter { it.exercise.name.contains(query.trim(), ignoreCase = true) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.historyEditor) TextButton(
                onClick = {
                    if (state.settings.confirmDiscard && session.exercises.isNotEmpty()) cancelConfirm = true
                    else actions.cancel()
                }, enabled = !state.busy
            ) { Text("Cancel history editor") }
            SearchBar(query, { query = it })
            DateButton(session.date, actions.date)
            OutlinedTextField(session.name, actions.name, label = { Text("Workout name") },
                enabled = !state.busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(session.notes, actions.notes, label = { Text("Notes (optional)") },
                enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            if (session.exercises.isEmpty()) EmptyState("Pick your first exercise", "Choose an exercise below to build your workout.")
            else if (filtered.isEmpty()) EmptyState("No exercises found", "Try a different search.")
            filtered.forEachIndexed { index, draft ->
                key(draft.exercise.id) {
                    ExerciseCard(
                        draft, WorkoutService.previousSets(state.data.workouts, draft.exercise.id),
                        state.settings.unit, actions, state.busy
                    )
                    if (index == filtered.lastIndex) Button(
                        onClick = actions.finish,
                        enabled = session.validSetCount > 0 && !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (session.editingId == null) "Finish Workout" else "Save Changes") }
                }
            }
            ExercisePicker(
                state.data.exercises.filter { it.name.contains(query.trim(), ignoreCase = true) },
                session.exercises.map { it.exercise.id }.toSet(), actions.addExercise, actions.customExercise
            )
        }
        Surface(tonalElevation = 3.dp) {
            Text(
                "${session.exercises.size} exercises • ${session.validSetCount} valid sets",
                modifier = Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.labelLarge
            )
        }
    }
    if (cancelConfirm) AlertDialog(
        onDismissRequest = { cancelConfirm = false }, title = { Text("Discard these changes?") },
        text = { Text("The saved workout and your ongoing workout will be kept.") },
        confirmButton = { TextButton(onClick = { cancelConfirm = false; actions.cancel() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { cancelConfirm = false }) { Text("Keep Editing") } }
    )
}
@Preview(showBackground = true, heightDp = 1000)
@Preview(showBackground = true, heightDp = 1000, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkoutScreenPreview() { GymTrackerTheme { WorkoutScreen(PreviewData.state, WorkoutActions()) } }
