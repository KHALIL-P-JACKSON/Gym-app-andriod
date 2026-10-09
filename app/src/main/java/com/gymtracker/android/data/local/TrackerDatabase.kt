package com.gymtracker.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String, val name: String, val muscleGroup: String,
    val category: String, val isPreset: Boolean, val sortOrder: Int
)
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String, val name: String, val dateMillis: Long,
    val notes: String, val durationSeconds: Long?
)
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"],
            childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"],
            childColumns = ["exerciseId"], onDelete = ForeignKey.NO_ACTION)
    ],
    indices = [Index("workoutId"), Index("exerciseId")]
)
data class SetEntity(
    @PrimaryKey val id: String, val workoutId: String, val exerciseId: String,
    val weightLb: Double, val reps: Int, val setNumber: Int, val sortOrder: Int
)
@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String, val name: String, val exerciseIds: String, val isPreset: Boolean
)
data class WorkoutWithSets(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId") val sets: List<SetEntity>
)
@Dao
interface TrackerDao {
    @Query("SELECT * FROM exercises ORDER BY sortOrder, name")
    fun exercises(): Flow<List<ExerciseEntity>>
    @Transaction @Query("SELECT * FROM workouts ORDER BY dateMillis DESC")
    fun workouts(): Flow<List<WorkoutWithSets>>
    @Query("SELECT * FROM plans ORDER BY isPreset DESC, name")
    fun plans(): Flow<List<PlanEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedExercises(exercises: List<ExerciseEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedPlans(plans: List<PlanEntity>)
    @Upsert suspend fun saveExercise(exercise: ExerciseEntity)
    @Upsert suspend fun saveWorkout(workout: WorkoutEntity)
    @Insert suspend fun saveSets(sets: List<SetEntity>)
    @Upsert suspend fun savePlan(plan: PlanEntity)
    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId")
    suspend fun clearSets(workoutId: String)
    @Query("DELETE FROM workouts WHERE id = :id") suspend fun deleteWorkout(id: String)
    @Query("DELETE FROM plans WHERE id = :id AND isPreset = 0") suspend fun deletePlan(id: String)
    @Query("DELETE FROM workouts") suspend fun clearWorkouts()
    @Query("DELETE FROM plans") suspend fun clearPlans()
    @Query("DELETE FROM exercises WHERE isPreset = 0") suspend fun clearCustomExercises()
}
@Database(
    entities = [ExerciseEntity::class, WorkoutEntity::class, SetEntity::class, PlanEntity::class],
    version = 1, exportSchema = true
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun dao(): TrackerDao
}
