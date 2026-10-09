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
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.service.ProgressCalculator
import com.gymtracker.android.presentation.Formatters
import com.gymtracker.android.presentation.composables.components.*
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun ProgressScreen(data: TrackerData, unit: WeightUnit) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selecting by remember { mutableStateOf(false) }
    val exercise = data.exercises.find { it.id == selectedId } ?: data.exercises.firstOrNull()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = { selecting = true }, modifier = Modifier.fillMaxWidth()) { Text(exercise?.name ?: "Select Exercise") }
        val history = exercise?.let { ProgressCalculator.history(data.workouts, it.id) }.orEmpty()
        if (history.isEmpty()) EmptyState("No progress data yet", "Log a workout with this exercise to see your progress.")
        else {
            val comparison = ProgressCalculator.comparison(history)
            val best = comparison.current
            StatCard("Current Best", best?.let { "${Formatters.weight(it.weightLb, unit)} × ${it.reps}" } ?: "—")
            StatCard("Change vs Previous Best", if (comparison.previous == null) "New" else "+${Formatters.weight(comparison.change, unit)}")
            StatCard("Estimated 1RM", best?.let { Formatters.weight(it.estimatedOneRepMax, unit) } ?: "—")
            ProgressChart(history, unit)
            Text("Epley estimate: weight × (1 + reps ÷ 30). This is an estimate, not a tested maximum.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (selecting) ExerciseSelectionDialog(data.exercises, onDismiss = { selecting = false }) {
        selectedId = it.id; selecting = false
    }
}
@Composable
fun ExerciseSelectionDialog(exercises: List<Exercise>, onDismiss: () -> Unit, onSelect: (Exercise) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Select Exercise") },
        text = {
            Column {
                SearchBar(query, { query = it })
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    exercises.filter { it.name.contains(query, true) }.forEach { exercise ->
                        item(key = exercise.id) { TextButton(onClick = { onSelect(exercise) }) { Text(exercise.name) } }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
@Preview(showBackground = true, heightDp = 950)
@Composable
private fun ProgressScreenPreview() { GymTrackerTheme { ProgressScreen(PreviewData.data, WeightUnit.LB) } }
