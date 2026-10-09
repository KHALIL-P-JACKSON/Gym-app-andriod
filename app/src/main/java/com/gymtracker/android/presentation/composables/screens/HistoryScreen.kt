package com.gymtracker.android.presentation.composables.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.presentation.Formatters
import com.gymtracker.android.presentation.composables.components.*
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun HistoryScreen(
    data: TrackerData, unit: WeightUnit, busy: Boolean,
    onAdd: () -> Unit, onEdit: (Workout) -> Unit, onDelete: (Workout) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Button(onClick = onAdd, enabled = !busy) { Text("+ Log a Past Workout") } }
        if (data.workouts.isEmpty()) item { EmptyState("No workouts yet", "Completed workouts will appear here.") }
        items(data.workouts.sortedByDescending { it.date }, key = { it.id }) { workout ->
            TrackerCard {
                Text(workout.name, style = MaterialTheme.typography.titleLarge)
                Text(Formatters.date(workout.date))
                Text("${workout.exerciseCount} exercises • ${workout.sets.size} sets • ${Formatters.weight(workout.volume, unit)} volume")
                TextButton(onClick = { selectedId = workout.id }) { Text("View Details") }
            }
        }
    }
    data.workouts.find { it.id == selectedId }?.let { workout ->
        AlertDialog(
            onDismissRequest = { selectedId = null }, title = { Text(workout.name) },
            text = { WorkoutDetails(workout, data.exercises, unit) },
            confirmButton = { TextButton(onClick = { selectedId = null; onEdit(workout) }, enabled = !busy) { Text("Edit") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { deleteId = workout.id; selectedId = null }, enabled = !busy) { Text("Delete") }
                    TextButton(onClick = { selectedId = null }) { Text("Close") }
                }
            }
        )
    }
    data.workouts.find { it.id == deleteId }?.let { workout ->
        AlertDialog(
            onDismissRequest = { deleteId = null }, title = { Text("Delete workout?") },
            text = { Text("This workout and its recorded sets will be removed.") },
            confirmButton = { TextButton(onClick = { onDelete(workout); deleteId = null }, enabled = !busy) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } }
        )
    }
}
@Composable
fun WorkoutDetails(workout: Workout, exercises: List<Exercise>, unit: WeightUnit) {
    Column(
        Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(Formatters.date(workout.date))
        if (workout.notes.isNotBlank()) Text(workout.notes)
        workout.sets.sortedBy { it.sortOrder }.groupBy { it.exerciseId }.forEach { (id, sets) ->
            Text(exercises.find { it.id == id }?.name ?: "Exercise", style = MaterialTheme.typography.titleMedium)
            sets.forEach { set -> Text("Set ${set.setNumber}: ${Formatters.weight(set.weightLb, unit)} × ${set.reps}") }
        }
        Text("Total volume: ${Formatters.weight(workout.volume, unit)}")
    }
}
@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() { GymTrackerTheme { HistoryScreen(PreviewData.data, WeightUnit.LB, false, {}, {}, {}) } }
@Preview(showBackground = true)
@Composable
private fun WorkoutDetailsPreview() { GymTrackerTheme { WorkoutDetails(PreviewData.workout, PreviewData.data.exercises, WeightUnit.LB) } }
