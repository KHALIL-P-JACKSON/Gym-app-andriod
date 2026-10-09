package com.gymtracker.android.presentation.composables.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun ExercisePicker(
    exercises: List<Exercise>, added: Set<String>, onSelect: (Exercise) -> Unit,
    onCustom: (String, MuscleGroup) -> Unit
) {
    var custom by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Exercise Library", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        TextButton(onClick = { custom = true }) { Text("Custom") }
    }
    MuscleGroup.entries.forEach { group ->
        val matches = exercises.filter { it.muscleGroup == group }
        if (matches.isNotEmpty()) TrackerCard {
            Text(group.label, style = MaterialTheme.typography.titleMedium)
            matches.forEach { exercise ->
                TextButton(
                    onClick = { onSelect(exercise) }, enabled = exercise.id !in added,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(exercise.name + if (exercise.id in added) " ✓" else "") }
            }
        }
    }
    if (custom) CustomExerciseDialog(onDismiss = { custom = false }, onSave = { name, group ->
        onCustom(name, group); custom = false
    })
}
@Composable
fun CustomExerciseDialog(onDismiss: () -> Unit, onSave: (String, MuscleGroup) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var groupIndex by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Custom Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Exercise name") }, singleLine = true)
                Box {
                    TextButton(onClick = { menu = true }) { Text(MuscleGroup.entries[groupIndex].label) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        MuscleGroup.entries.forEachIndexed { index, group ->
                            DropdownMenuItem(text = { Text(group.label) }, onClick = { groupIndex = index; menu = false })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim(), MuscleGroup.entries[groupIndex]) }, enabled = name.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
@Preview(showBackground = true)
@Composable
private fun ExercisePickerPreview() { GymTrackerTheme { Column { ExercisePicker(PreviewData.data.exercises, emptySet(), {}, { _, _ -> }) } } }
