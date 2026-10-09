package com.gymtracker.android.presentation

import com.gymtracker.android.domain.model.*
import java.time.Instant

data class WorkoutActions(
    val name: (String) -> Unit = {},
    val notes: (String) -> Unit = {},
    val date: (Instant) -> Unit = {},
    val addExercise: (Exercise) -> Unit = {},
    val removeExercise: (String) -> Unit = {},
    val addSet: (String) -> Unit = {},
    val removeSet: (String, String) -> Unit = { _, _ -> },
    val editSet: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    val complete: (String) -> Unit = {},
    val finish: () -> Unit = {},
    val customExercise: (String, MuscleGroup) -> Unit = { _, _ -> },
    val cancel: () -> Unit = {}
)
