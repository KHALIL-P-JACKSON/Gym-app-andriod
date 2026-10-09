package com.gymtracker.android.presentation.composables.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtracker.android.domain.model.DraftSet
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun TrackerCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    TrackerCard(modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    TrackerCard(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun SearchBar(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query, onValueChange = onQuery, label = { Text("Search exercises") },
        singleLine = true, modifier = modifier.fillMaxWidth()
    )
}
@Composable
fun SetRow(
    number: Int, set: DraftSet, locked: Boolean,
    onChange: (String, String) -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$number", modifier = Modifier.padding(top = 20.dp), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = set.weightText, onValueChange = { onChange(it, set.repsText) },
            label = { Text("Weight (${set.entryUnit.label})") }, enabled = !locked,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = set.repsText, onValueChange = { onChange(set.weightText, it) },
            label = { Text("Reps") }, enabled = !locked,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.weight(0.7f)
        )
        TextButton(onClick = onRemove, enabled = !locked, contentPadding = PaddingValues(4.dp)) { Text("×") }
    }
}

@Preview(showBackground = true, name = "Cards - light")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Cards - dark")
@Composable
private fun CommonComponentsPreview() {
    GymTrackerTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Day Streak", "3")
            EmptyState("No workouts yet", "Start your first workout and track your progress.")
            SearchBar("Bench", {})
            TrackerCard { SetRow(1, DraftSet(weightText = "185", repsText = "8"), false, { _, _ -> }, {}) }
        }
    }
}
