package com.gymtracker.android.domain.model

import java.time.Instant
import java.util.UUID

enum class MuscleGroup(val label: String) {
    CHEST("Chest"), BACK("Back"), SHOULDERS("Shoulders"),
    ARMS("Arms"), LEGS("Legs"), CALISTHENICS("Calisthenics")
}
enum class Category { PUSH, PULL, LEGS }
enum class WeightUnit(val label: String, val poundsPerUnit: Double) {
    LB("lb", 1.0), KG("kg", 2.20462);
    fun fromPounds(pounds: Double) = pounds / poundsPerUnit
    fun toPounds(value: Double) = value * poundsPerUnit
}
enum class Appearance { SYSTEM, LIGHT, DARK }
data class Settings(
    val unit: WeightUnit = WeightUnit.LB,
    val appearance: Appearance = Appearance.SYSTEM,
    val restSeconds: Int = 0,
    val keepAwake: Boolean = false,
    val confirmDiscard: Boolean = true
)
data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String, val muscleGroup: MuscleGroup, val category: Category,
    val isPreset: Boolean = false, val sortOrder: Int = 0
)
data class WorkoutSet(
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String, val weightLb: Double, val reps: Int,
    val setNumber: Int, val sortOrder: Int
) {
    val volume get() = weightLb * reps
    val estimatedOneRepMax get() = weightLb * (1.0 + reps / 30.0)
}
data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Workout", val date: Instant = Instant.now(),
    val notes: String = "", val durationSeconds: Long? = null,
    val sets: List<WorkoutSet> = emptyList()
) {
    val volume get() = sets.sumOf { it.volume }
    val exerciseCount get() = sets.map { it.exerciseId }.distinct().size
}
data class WorkoutPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String, val exerciseIds: List<String>, val isPreset: Boolean = false
)
data class TrackerData(
    val exercises: List<Exercise> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val plans: List<WorkoutPlan> = emptyList()
)
data class DraftSet(
    val id: String = UUID.randomUUID().toString(),
    val weightText: String = "", val repsText: String = "",
    val entryUnit: WeightUnit = WeightUnit.LB
) {
    val weightLb get() = weightText.trim().replace(',', '.').toDoubleOrNull()?.let(entryUnit::toPounds)
    val reps get() = repsText.trim().toIntOrNull()
    val isValid get() = weightLb?.let { it.isFinite() && it > 0 } == true && (reps ?: 0) > 0
    fun inUnit(unit: WeightUnit): DraftSet {
        if (unit == entryUnit) return this
        val pounds = weightLb
        // Keep unparseable text in its original unit until the user fixes it.
        if (pounds == null && weightText.isNotBlank()) return this
        return copy(
            weightText = pounds?.let { unit.fromPounds(it).toString() } ?: "",
            entryUnit = unit
        )
    }
}
data class DraftExercise(
    val exercise: Exercise, val sets: List<DraftSet>,
    val isComplete: Boolean = false
)
data class WorkoutSession(
    val name: String = "Workout", val date: Instant = Instant.now(), val notes: String = "",
    val editingId: String? = null, val exercises: List<DraftExercise> = emptyList()
) {
    val validSetCount get() = exercises.sumOf { it.sets.count(DraftSet::isValid) }
    val invalidSetCount get() = exercises.sumOf { it.sets.count { set -> !set.isValid } }
}
