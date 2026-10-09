package com.gymtracker.android.presentation.composables.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.presentation.composables.components.*
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun PlanScreen(
    data: TrackerData, busy: Boolean, onLoad: (WorkoutPlan) -> Unit,
    onSave: (WorkoutPlan) -> Unit, onDelete: (WorkoutPlan) -> Unit
) {
    var editor by remember { mutableStateOf<WorkoutPlan?>(null) }
    var deleting by remember { mutableStateOf<WorkoutPlan?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Button(onClick = { editor = WorkoutPlan(name = "", exerciseIds = emptyList()) }, enabled = !busy) { Text("Create Custom Plan") }
        }
        items(data.plans, key = { it.id }) { plan ->
            TrackerCard {
                Text(if (plan.isPreset) "Preset Split" else "My Plan", style = MaterialTheme.typography.labelLarge)
                Text(plan.name, style = MaterialTheme.typography.titleLarge)
                Text(plan.exerciseIds.mapNotNull { id -> data.exercises.find { it.id == id }?.name }.joinToString(" • "))
                Button(onClick = { onLoad(plan) }, enabled = !busy) { Text("Add to Workout") }
                Row {
                    TextButton(onClick = { editor = plan }, enabled = !busy) { Text("Edit") }
                    if (!plan.isPreset) TextButton(onClick = { deleting = plan }, enabled = !busy) { Text("Delete") }
                }
            }
        }
    }
    editor?.let { plan ->
        key(plan.id) { PlanEditorDialog(plan, data.exercises, onDismiss = { editor = null }, onSave = {
            onSave(it); editor = null
        }) }
    }
    deleting?.let { plan ->
        AlertDialog(
            onDismissRequest = { deleting = null }, title = { Text("Delete ${plan.name}?") },
            text = { Text("Your recorded workouts will stay in History.") },
            confirmButton = { TextButton(onClick = { onDelete(plan); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}
@Composable
fun PlanEditorDialog(plan: WorkoutPlan, exercises: List<Exercise>, onDismiss: () -> Unit, onSave: (WorkoutPlan) -> Unit) {
    var name by rememberSaveable(plan.id) { mutableStateOf(plan.name) }
    var selected by rememberSaveable(plan.id) { mutableStateOf(plan.exerciseIds) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(if (plan.name.isEmpty()) "Create Plan" else "Edit Plan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Plan name") }, singleLine = true)
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    MuscleGroup.entries.forEach { group ->
                        val matching = exercises.filter { it.muscleGroup == group }
                        if (matching.isNotEmpty()) {
                            item { Text(group.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp)) }
                            items(matching, key = { it.id }) { exercise ->
                                val checked = exercise.id in selected
                                Row(
                                    Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox) {
                                        selected = if (checked) selected - exercise.id else selected + exercise.id
                                    }.padding(vertical = 4.dp)
                                ) {
                                    Checkbox(checked, onCheckedChange = null)
                                    Text(exercise.name, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(
            onClick = { onSave(plan.copy(name = name.trim(), exerciseIds = selected)) },
            enabled = name.isNotBlank() && selected.isNotEmpty()
        ) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PlanScreenPreview() { GymTrackerTheme { PlanScreen(PreviewData.data, false, {}, {}, {}) } }
@Preview(showBackground = true)
@Composable
private fun PlanEditorPreview() { GymTrackerTheme { PlanEditorDialog(PreviewData.data.plans.first(), PreviewData.data.exercises, {}, {}) } }
