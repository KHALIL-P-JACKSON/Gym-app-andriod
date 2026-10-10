package com.gymtracker.android.presentation.composables.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun HomeScreen(data: TrackerData, unit: WeightUnit, onProgress: () -> Unit = {}, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(Formatters.greeting(), style = MaterialTheme.typography.headlineLarge)
        Text("I love you, you can do this!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TrackerCard {
            Text("Ready for your workout?", style = MaterialTheme.typography.titleLarge)
            Text("Record your next session.")
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start Workout") }
        }
        val recent = data.workouts.maxByOrNull { it.date }
        if (recent == null) EmptyState("No workouts yet", "Your first workout starts here.")
        else TrackerCard {
            Text("Recent Workout", style = MaterialTheme.typography.labelLarge)
            Text(recent.name, style = MaterialTheme.typography.titleLarge)
            Text(Formatters.date(recent.date))
            val firstExercise = recent.sets.minByOrNull { it.sortOrder }?.exerciseId
            recent.sets.filter { it.exerciseId == firstExercise }.take(3).forEach { set ->
                Text("${data.exercises.find { it.id == set.exerciseId }?.name ?: "Exercise"}: " +
                    "${Formatters.weight(set.weightLb, unit)} × ${set.reps}")
            }
            Text("${recent.exerciseCount} exercises • ${recent.sets.size} sets")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Day Streak", ProgressCalculator.streak(data.workouts.map { it.date }).toString(), Modifier.weight(1f))
            StatCard("Workouts", data.workouts.size.toString(), Modifier.weight(1f))
            StatCard("Volume", Formatters.weight(data.workouts.sumOf { it.volume }, unit), Modifier.weight(1f))
        }
        TrackerCard {
            Text("Strength Progress", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onProgress) { Text("View Progress") }
            val featured = data.workouts.flatMap { it.sets }.groupBy { it.exerciseId }.maxByOrNull { it.value.size }?.key
            if (featured == null) Text("Log a few workouts to compare your top lifts.")
            else {
                val comparison = ProgressCalculator.comparison(ProgressCalculator.history(data.workouts, featured))
                Text(data.exercises.find { it.id == featured }?.name ?: "Exercise")
                Text(if (comparison.previous == null) "New record"
                    else "+${Formatters.weight(comparison.change, unit)} compared with your previous best")
            }
        }
    }
}
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() { GymTrackerTheme { HomeScreen(PreviewData.data, WeightUnit.LB, onStart = {}) } }
