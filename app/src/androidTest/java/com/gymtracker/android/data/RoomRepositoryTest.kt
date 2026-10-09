package com.gymtracker.android.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtracker.android.data.local.TrackerDatabase
import com.gymtracker.android.data.repository.RoomTrackerRepository
import com.gymtracker.android.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRepositoryTest {
    @Test fun persistReopenEditCascadeAndReseed() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filename = "repository-test.db"
        context.deleteDatabase(filename)
        fun open() = Room.databaseBuilder(context, TrackerDatabase::class.java, filename).build()
        var db = open()
        try {
            var repository = RoomTrackerRepository(db)
            repository.seed(); repository.seed()
            val seeded = repository.data.first()
            assertEquals(37, seeded.exercises.size)
            assertEquals(6, seeded.plans.size)
            val bench = seeded.exercises.first()
            val workout = Workout(name = "Persistence", sets = listOf(
                WorkoutSet("set", bench.id, 185.0, 8, 1, 0)
            ))
            repository.saveWorkout(workout)
            repository.savePlan(seeded.plans.first().copy(name = "Edited Preset"))
            db.close()
            db = open()
            repository = RoomTrackerRepository(db)
            repository.seed()
            val reopened = repository.data.first()
            assertEquals("Persistence", reopened.workouts.single().name)
            assertEquals(185.0, reopened.workouts.single().sets.single().weightLb, 0.0001)
            assertTrue(reopened.plans.any { it.name == "Edited Preset" })
            repository.saveWorkout(workout.copy(name = "Edited", sets = listOf(
                workout.sets.single().copy(weightLb = 195.0)
            )))
            assertEquals(195.0, repository.data.first().workouts.single().sets.single().weightLb, 0.0001)
            repository.deleteWorkout(workout.id)
            assertTrue(repository.data.first().workouts.isEmpty())
            repository.saveWorkout(workout)
            val custom = Exercise(name = "Custom", muscleGroup = MuscleGroup.CHEST, category = Category.PUSH)
            repository.saveExercise(custom)
            repository.deleteAllData()
            val wiped = repository.data.first()
            assertTrue(wiped.workouts.isEmpty())
            assertEquals(37, wiped.exercises.size)
            assertEquals(6, wiped.plans.size)
            assertFalse(wiped.plans.any { it.name == "Edited Preset" })
        } finally {
            db.close()
            context.deleteDatabase(filename)
        }
    }
}
