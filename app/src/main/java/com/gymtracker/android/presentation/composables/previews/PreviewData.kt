package com.gymtracker.android.presentation.composables.previews

import com.gymtracker.android.domain.model.*
import com.gymtracker.android.presentation.TrackerUiState
import java.time.Instant

/** Pure preview fixtures: previews never open a database or construct a ViewModel. */
object PreviewData {
    val bench = Exercise("bench", "Bench Press", MuscleGroup.CHEST, Category.PUSH, true)
    val row = Exercise("row", "Barbell Row", MuscleGroup.BACK, Category.PULL, true)
    val workout = Workout(
        id = "sample", name = "Push Day", date = Instant.parse("2025-03-01T12:00:00Z"),
        sets = listOf(WorkoutSet("s1", bench.id, 185.0, 8, 1, 0), WorkoutSet("s2", bench.id, 185.0, 6, 2, 1))
    )
    val data = TrackerData(
        exercises = listOf(bench, row),
        workouts = listOf(workout, workout.copy(
            id = "older", date = Instant.parse("2025-02-20T12:00:00Z"),
            sets = listOf(WorkoutSet("s3", bench.id, 175.0, 8, 1, 0))
        )),
        plans = listOf(WorkoutPlan("push", "Push Day", listOf(bench.id), true))
    )
    val state = TrackerUiState(
        data = data, loading = false, session = WorkoutSession(
            name = "Push Day", exercises = listOf(DraftExercise(
                bench, listOf(DraftSet(weightText = "185", repsText = "8"))
            ))
        )
    )
}
