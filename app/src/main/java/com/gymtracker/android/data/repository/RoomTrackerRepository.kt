package com.gymtracker.android.data.repository

import androidx.room.withTransaction
import com.gymtracker.android.data.local.*
import com.gymtracker.android.data.seed.DefaultLibrary
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.combine
import java.time.Instant

class RoomTrackerRepository(private val database: TrackerDatabase) : TrackerRepository {
    private val dao = database.dao()
    override val data = combine(dao.exercises(), dao.workouts(), dao.plans()) { exercises, workouts, plans ->
        TrackerData(
            exercises.map { it.model() },
            workouts.map { row ->
                row.workout.let { w ->
                    Workout(w.id, w.name, Instant.ofEpochMilli(w.dateMillis), w.notes, w.durationSeconds,
                        row.sets.sortedBy { it.sortOrder }.map {
                            WorkoutSet(it.id, it.exerciseId, it.weightLb, it.reps, it.setNumber, it.sortOrder)
                        })
                }
            },
            plans.map { WorkoutPlan(it.id, it.name, it.exerciseIds.split('|').filter(String::isNotEmpty), it.isPreset) }
        )
    }
    override suspend fun seed() = database.withTransaction {
        dao.seedExercises(DefaultLibrary.exercises.map { it.entity() })
        dao.seedPlans(DefaultLibrary.plans.map { it.entity() })
    }
    override suspend fun saveWorkout(workout: Workout) = database.withTransaction {
        require(workout.sets.isNotEmpty()) { "A workout needs at least one valid set." }
        require(workout.sets.all { it.weightLb.isFinite() && it.weightLb > 0 && it.reps > 0 })
        dao.saveWorkout(WorkoutEntity(workout.id, workout.name, workout.date.toEpochMilli(), workout.notes, workout.durationSeconds))
        dao.clearSets(workout.id)
        dao.saveSets(workout.sets.map {
            SetEntity(it.id, workout.id, it.exerciseId, it.weightLb, it.reps, it.setNumber, it.sortOrder)
        })
    }
    override suspend fun deleteWorkout(id: String) = dao.deleteWorkout(id)
    override suspend fun saveExercise(exercise: Exercise) = dao.saveExercise(exercise.entity())
    override suspend fun savePlan(plan: WorkoutPlan) = dao.savePlan(plan.entity())
    override suspend fun deletePlan(id: String) = dao.deletePlan(id)
    override suspend fun deleteAllData() = database.withTransaction {
        dao.clearWorkouts()
        dao.clearPlans()
        dao.clearCustomExercises()
        dao.seedExercises(DefaultLibrary.exercises.map { it.entity() })
        dao.seedPlans(DefaultLibrary.plans.map { it.entity() })
    }
}
private fun ExerciseEntity.model() = Exercise(
    id, name, MuscleGroup.valueOf(muscleGroup), Category.valueOf(category), isPreset, sortOrder
)
private fun Exercise.entity() = ExerciseEntity(id, name, muscleGroup.name, category.name, isPreset, sortOrder)
private fun WorkoutPlan.entity() = PlanEntity(id, name, exerciseIds.joinToString("|"), isPreset)
