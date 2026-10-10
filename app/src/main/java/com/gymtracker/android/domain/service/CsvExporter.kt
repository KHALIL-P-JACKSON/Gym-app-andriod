package com.gymtracker.android.domain.service

import com.gymtracker.android.domain.model.*

object CsvExporter {
    fun exportRuns(runs: List<RunRecord>): String {
        val header = "start,end,duration_seconds,distance_meters,average_pace_seconds_per_unit,unit,goal_meters,recording_gaps"
        val rows = runs.filter { it.endedAt != null }.sortedByDescending { it.startedAt }.map { run ->
            listOf(java.time.Instant.ofEpochMilli(run.startedAt).toString(),
                java.time.Instant.ofEpochMilli(run.endedAt!!).toString(), (run.elapsedMillis / 1000.0).toString(),
                run.distanceMeters.toString(), if (run.distanceMeters > 0) (run.elapsedMillis / 1000.0 * run.unit.meters / run.distanceMeters).toString() else "",
                run.unit.label, run.goalMeters?.toString() ?: "", run.hasGaps.toString()).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }
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
