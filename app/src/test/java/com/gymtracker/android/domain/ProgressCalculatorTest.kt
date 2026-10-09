package com.gymtracker.android.domain

import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.service.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ProgressCalculatorTest {
    @Test fun historyUsesStrongestSetAndRanksSeparateWorkoutBests() {
        val exercise = "bench"
        val first = Workout(id = "first", date = Instant.parse("2025-01-01T12:00:00Z"),
            sets = listOf(WorkoutSet("a", exercise, 185.0, 8, 1, 0), WorkoutSet("b", exercise, 185.0, 6, 2, 1)))
        val second = Workout(id = "second", date = Instant.parse("2025-01-02T12:00:00Z"),
            sets = listOf(WorkoutSet("c", exercise, 175.0, 10, 1, 0)))
        val history = ProgressCalculator.history(listOf(second, first), exercise)
        assertEquals(listOf("first", "second"), history.map { it.workoutId })
        assertEquals(234.333333, history[0].bestSet.estimatedOneRepMax, 0.00001)
        assertEquals(2590.0, history[0].volume, 0.001)
        val comparison = ProgressCalculator.comparison(history)
        assertEquals("a", comparison.current?.id)
        assertEquals("c", comparison.previous?.id)
        assertEquals(10.0, comparison.change, 0.001)
    }
    @Test fun streakDeduplicatesDaysAndCanEndYesterday() {
        val today = LocalDate.of(2025, 3, 2)
        val dates = listOf("2025-03-01T10:00:00Z", "2025-03-01T20:00:00Z", "2025-02-28T12:00:00Z")
            .map(Instant::parse)
        assertEquals(2, ProgressCalculator.streak(dates, today, ZoneOffset.UTC))
        assertEquals(0, ProgressCalculator.streak(dates, today.plusDays(1), ZoneOffset.UTC))
    }
    @Test fun csvEscapesNamesAndAlwaysExportsPounds() {
        val exercise = Exercise("bench", "Bench, Press", MuscleGroup.CHEST, Category.PUSH)
        val workout = Workout(name = "A \"great\" workout", sets = listOf(WorkoutSet(exerciseId = exercise.id,
            weightLb = 185.0, reps = 8, setNumber = 1, sortOrder = 0)))
        val csv = CsvExporter.export(TrackerData(listOf(exercise), listOf(workout)))
        assertTrue(csv.startsWith("date,workout,exercise,set,weight_lb,reps"))
        assertTrue(csv.contains("\"A \"\"great\"\" workout\""))
        assertTrue(csv.contains("\"Bench, Press\",1,185.0,8"))
    }
}
