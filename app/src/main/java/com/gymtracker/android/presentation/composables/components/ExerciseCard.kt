package com.gymtracker.android.presentation.composables.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.presentation.*
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun ExerciseCard(draft: DraftExercise, previousSets: List<WorkoutSet>, unit: WeightUnit, actions: WorkoutActions, busy: Boolean) {
    val id = draft.exercise.id
    TrackerCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(draft.exercise.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { actions.removeExercise(id) }, enabled = !busy) { Text("Remove") }
        }
        if (previousSets.isNotEmpty()) {
            Text("Last Workout", style = MaterialTheme.typography.labelLarge)
            Text(previousSets.joinToString(" • ") { "${Formatters.weight(it.weightLb, unit)} × ${it.reps}" })
        }
        draft.sets.forEachIndexed { index, set ->
            SetRow(index + 1, set, draft.isComplete || busy,
                onChange = { weight, reps -> actions.editSet(id, set.id, weight, reps) },
                onRemove = { actions.removeSet(id, set.id) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { actions.addSet(id) }, enabled = !busy) { Text("+ Add Set") }
            TextButton(
                onClick = { actions.complete(id) },
                enabled = !busy && (draft.isComplete || (draft.sets.isNotEmpty() && draft.sets.all { it.isValid }))
            ) { Text(if (draft.isComplete) "Edit" else "Complete") }
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun ExerciseCardPreview() { GymTrackerTheme { ExerciseCard(
    PreviewData.state.session.exercises.first(), PreviewData.workout.sets, WeightUnit.LB, WorkoutActions(), false
) } }
