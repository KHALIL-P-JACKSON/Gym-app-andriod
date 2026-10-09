package com.gymtracker.android.presentation.composables.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.gymtracker.android.presentation.Formatters
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme
import java.time.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateButton(date: Instant, onDate: (Instant) -> Unit) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("Date: ${Formatters.date(date)}") }
    if (open) {
        val localDate = date.atZone(ZoneId.systemDefault()).toLocalDate()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= LocalDate.now()
            }
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    enabled = picker.selectedDateMillis != null,
                    onClick = {
                        picker.selectedDateMillis?.let {
                            val day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            onDate(day.atStartOfDay(ZoneId.systemDefault()).toInstant())
                        }
                        open = false
                    }
                ) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
}
@Preview(showBackground = true)
@Composable
private fun DateButtonPreview() { GymTrackerTheme { DateButton(Instant.parse("2025-03-01T12:00:00Z"), {}) } }
