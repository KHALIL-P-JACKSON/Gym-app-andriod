package com.gymtracker.android.domain.service

import com.gymtracker.android.domain.model.*
import java.util.UUID

/** Maps valid draft rows to canonical pounds and preserves exercise/set order. */
object WorkoutService {
    fun build(session: WorkoutSession): Workout {
        require(session.validSetCount > 0) { "Enter at least one valid set." }
        var order = 0
        val sets = session.exercises.flatMap { draft ->
            draft.sets.filter { it.isValid }.mapIndexed { index, set ->
                WorkoutSet(
                    id = set.id, exerciseId = draft.exercise.id,
                    weightLb = requireNotNull(set.weightLb), reps = requireNotNull(set.reps),
                    setNumber = index + 1, sortOrder = order++
                )
            }
        }
        return Workout(
            id = session.editingId ?: UUID.randomUUID().toString(),
            name = session.name.trim().ifBlank { "Workout" }, date = session.date,
            notes = session.notes.trim(), sets = sets
        )
    }
    fun previousSets(workouts: List<Workout>, exerciseId: String, limit: Int = 3): List<WorkoutSet> =
        workouts.sortedByDescending { it.date }
            .firstOrNull { workout -> workout.sets.any { it.exerciseId == exerciseId } }
            ?.sets?.filter { it.exerciseId == exerciseId }?.sortedBy { it.sortOrder }?.take(limit).orEmpty()

    fun edit(workout: Workout, exercises: List<Exercise>, unit: WeightUnit): WorkoutSession {
        val library = exercises.associateBy { it.id }
        return WorkoutSession(
            name = workout.name, date = workout.date, notes = workout.notes, editingId = workout.id,
            exercises = workout.sets.sortedBy { it.sortOrder }.groupBy { it.exerciseId }.map { (id, sets) ->
                DraftExercise(
                    exercise = requireNotNull(library[id]),
                    sets = sets.map { DraftSet(it.id, unit.fromPounds(it.weightLb).toString(), it.reps.toString(), unit) }
                )
            }
        )
    }
}
