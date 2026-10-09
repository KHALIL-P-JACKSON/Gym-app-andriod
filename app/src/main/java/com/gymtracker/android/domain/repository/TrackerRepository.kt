package com.gymtracker.android.domain.repository

import com.gymtracker.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TrackerRepository {
    val data: Flow<TrackerData>
    suspend fun seed()
    suspend fun saveWorkout(workout: Workout)
    suspend fun deleteWorkout(id: String)
    suspend fun saveExercise(exercise: Exercise)
    suspend fun savePlan(plan: WorkoutPlan)
    suspend fun deletePlan(id: String)
    suspend fun deleteAllData()
}
interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun update(settings: Settings)
}
