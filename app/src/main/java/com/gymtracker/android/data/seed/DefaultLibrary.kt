package com.gymtracker.android.data.seed

import com.gymtracker.android.domain.model.*

/** Exact library and split names from the iOS source at 6f1a03c. Stable IDs make seeding idempotent. */
object DefaultLibrary {
    val exercises = listOf(
        Exercise("preset-0", "Bench Press", MuscleGroup.CHEST, Category.PUSH, true, 0),
        Exercise("preset-1", "Smith Machine Bench Press", MuscleGroup.CHEST, Category.PUSH, true, 1),
        Exercise("preset-2", "Incline Bench Press", MuscleGroup.CHEST, Category.PUSH, true, 2),
        Exercise("preset-3", "Dumbbell Bench Press", MuscleGroup.CHEST, Category.PUSH, true, 3),
        Exercise("preset-4", "Incline Dumbbell Press", MuscleGroup.CHEST, Category.PUSH, true, 4),
        Exercise("preset-5", "Pec Dec", MuscleGroup.CHEST, Category.PUSH, true, 5),
        Exercise("preset-6", "Dips", MuscleGroup.CHEST, Category.PUSH, true, 6),
        Exercise("preset-7", "Lat Pulldown", MuscleGroup.BACK, Category.PULL, true, 7),
        Exercise("preset-8", "Pull Ups", MuscleGroup.BACK, Category.PULL, true, 8),
        Exercise("preset-9", "Seated Cable Row", MuscleGroup.BACK, Category.PULL, true, 9),
        Exercise("preset-10", "Single Arm Seated Cable Row", MuscleGroup.BACK, Category.PULL, true, 10),
        Exercise("preset-11", "Barbell Row", MuscleGroup.BACK, Category.PULL, true, 11),
        Exercise("preset-12", "Rear Delt Flys", MuscleGroup.BACK, Category.PULL, true, 12),
        Exercise("preset-13", "Shoulder Press", MuscleGroup.SHOULDERS, Category.PUSH, true, 13),
        Exercise("preset-14", "Lateral Raise", MuscleGroup.SHOULDERS, Category.PUSH, true, 14),
        Exercise("preset-15", "Bicep Curl", MuscleGroup.ARMS, Category.PULL, true, 15),
        Exercise("preset-16", "Cable Curl", MuscleGroup.ARMS, Category.PULL, true, 16),
        Exercise("preset-17", "Hammer Curl", MuscleGroup.ARMS, Category.PULL, true, 17),
        Exercise("preset-18", "Cable Hammer Curl", MuscleGroup.ARMS, Category.PULL, true, 18),
        Exercise("preset-19", "Tricep Pushdown", MuscleGroup.ARMS, Category.PUSH, true, 19),
        Exercise("preset-20", "Tricep Overhead Extension", MuscleGroup.ARMS, Category.PUSH, true, 20),
        Exercise("preset-21", "Squat", MuscleGroup.LEGS, Category.LEGS, true, 21),
        Exercise("preset-22", "Leg Press", MuscleGroup.LEGS, Category.LEGS, true, 22),
        Exercise("preset-23", "Leg Extension", MuscleGroup.LEGS, Category.LEGS, true, 23),
        Exercise("preset-24", "Leg Curl", MuscleGroup.LEGS, Category.LEGS, true, 24),
        Exercise("preset-25", "Romanian Deadlift", MuscleGroup.LEGS, Category.LEGS, true, 25),
        Exercise("preset-26", "Calf Raise", MuscleGroup.LEGS, Category.LEGS, true, 26),
        Exercise("preset-27", "Push Ups", MuscleGroup.CALISTHENICS, Category.PUSH, true, 27),
        Exercise("preset-28", "Pull Ups (Bodyweight)", MuscleGroup.CALISTHENICS, Category.PULL, true, 28),
        Exercise("preset-29", "Chin Ups", MuscleGroup.CALISTHENICS, Category.PULL, true, 29),
        Exercise("preset-30", "Bodyweight Dips", MuscleGroup.CALISTHENICS, Category.PUSH, true, 30),
        Exercise("preset-31", "Bodyweight Squats", MuscleGroup.CALISTHENICS, Category.LEGS, true, 31),
        Exercise("preset-32", "Lunges", MuscleGroup.CALISTHENICS, Category.LEGS, true, 32),
        Exercise("preset-33", "Plank", MuscleGroup.CALISTHENICS, Category.PUSH, true, 33),
        Exercise("preset-34", "Burpees", MuscleGroup.CALISTHENICS, Category.PUSH, true, 34),
        Exercise("preset-35", "Mountain Climbers", MuscleGroup.CALISTHENICS, Category.LEGS, true, 35),
        Exercise("preset-36", "Hanging Knee Raises", MuscleGroup.CALISTHENICS, Category.PULL, true, 36)
    )
    private fun plan(id: String, name: String, names: List<String>) =
        WorkoutPlan(id, name, names.map { exerciseName ->
            exercises.first { it.name == exerciseName }.id
        }, isPreset = true)
    val plans = listOf(
        plan("plan-0", "Push Day", listOf("Bench Press", "Incline Dumbbell Press", "Shoulder Press", "Lateral Raise", "Tricep Pushdown", "Tricep Overhead Extension")),
        plan("plan-1", "Pull Day", listOf("Pull Ups", "Lat Pulldown", "Barbell Row", "Seated Cable Row", "Bicep Curl", "Hammer Curl")),
        plan("plan-2", "Leg Day", listOf("Squat", "Leg Press", "Romanian Deadlift", "Leg Extension", "Leg Curl", "Calf Raise")),
        plan("plan-3", "Upper Body", listOf("Bench Press", "Seated Cable Row", "Shoulder Press", "Lat Pulldown", "Bicep Curl", "Tricep Pushdown")),
        plan("plan-4", "Full Body", listOf("Squat", "Bench Press", "Barbell Row", "Shoulder Press", "Plank")),
        plan("plan-5", "Calisthenics", listOf("Push Ups", "Pull Ups (Bodyweight)", "Bodyweight Dips", "Bodyweight Squats", "Lunges", "Plank", "Burpees"))
    )
}
