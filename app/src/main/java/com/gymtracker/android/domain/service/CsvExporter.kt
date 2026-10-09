package com.gymtracker.android.domain.service

import com.gymtracker.android.domain.model.*

object CsvExporter {
    private fun field(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + value.replace("\"", "\"\"") + "\"" else value

    fun export(data: TrackerData): String {
        val names = data.exercises.associate { it.id to it.name }
        val rows = data.workouts.sortedByDescending { it.date }.flatMap { workout ->
            workout.sets.sortedBy { it.sortOrder }.map { set ->
                listOf(workout.date.toString(), field(workout.name), field(names[set.exerciseId] ?: "Unknown"),
                    set.setNumber.toString(), set.weightLb.toString(), set.reps.toString()).joinToString(",")
            }
        }
        return (listOf("date,workout,exercise,set,weight_lb,reps") + rows).joinToString("\n")
    }
}
