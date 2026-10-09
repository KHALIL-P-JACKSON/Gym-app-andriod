package com.gymtracker.android.domain

import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.service.WorkoutService
import org.junit.Assert.*
import org.junit.Test

class WorkoutServiceTest {
    private val bench = Exercise("bench", "Bench", MuscleGroup.CHEST, Category.PUSH)
    private val row = Exercise("row", "Row", MuscleGroup.BACK, Category.PULL)
    @Test fun canonicalPoundsAndValidRowsPreserveExerciseOrder() {
        val session = WorkoutSession(name = "  Push  ", exercises = listOf(
            DraftExercise(bench, listOf(
                DraftSet(weightText = "100", repsText = "8", entryUnit = WeightUnit.KG),
                DraftSet(weightText = "", repsText = "10")
            )),
            DraftExercise(row, listOf(DraftSet(weightText = "135", repsText = "5")))
        ))
        val workout = WorkoutService.build(session)
        assertEquals("Push", workout.name)
        assertEquals(2, workout.sets.size)
        assertEquals(220.462, workout.sets[0].weightLb, 0.0001)
        assertEquals(listOf(0, 1), workout.sets.map { it.sortOrder })
        assertEquals(listOf(1, 1), workout.sets.map { it.setNumber })
        assertEquals(listOf("bench", "row"), workout.sets.map { it.exerciseId })
    }
    @Test fun changingDisplayUnitPreservesStoredWeight() {
        val lb = DraftSet(weightText = "185", repsText = "8")
        val kg = lb.inUnit(WeightUnit.KG)
        assertEquals(WeightUnit.KG, kg.entryUnit)
        assertEquals(requireNotNull(lb.weightLb), requireNotNull(kg.weightLb), 0.0000001)
        assertEquals(requireNotNull(lb.weightLb), requireNotNull(kg.inUnit(WeightUnit.LB).weightLb), 0.0000001)
    }
    @Test fun invalidNumbersCannotBeSaved() {
        listOf("", "0", "-5", "NaN", "Infinity", "oops").forEach { weight ->
            assertFalse(DraftSet(weightText = weight, repsText = "8").isValid)
        }
        assertFalse(DraftSet(weightText = "185", repsText = "0").isValid)
    }
    @Test(expected = IllegalArgumentException::class)
    fun emptyWorkoutIsRejected() { WorkoutService.build(WorkoutSession()) }
    @Test fun editingKeepsIdentityAndPreviousSetsComeFromOneWorkout() {
        val old = Workout(name = "Old", date = java.time.Instant.parse("2025-01-01T00:00:00Z"),
            sets = listOf(WorkoutSet("a", bench.id, 135.0, 8, 1, 0)))
        val latest = Workout(name = "Latest", date = java.time.Instant.parse("2025-01-02T00:00:00Z"),
            sets = listOf(WorkoutSet("b", bench.id, 185.0, 8, 1, 0), WorkoutSet("c", row.id, 135.0, 8, 1, 1)))
        val edited = WorkoutService.build(WorkoutService.edit(latest, listOf(bench, row), WeightUnit.KG))
        assertEquals(latest.id, edited.id)
        assertEquals(latest.sets.map { it.id }, edited.sets.map { it.id })
        assertEquals(listOf("b"), WorkoutService.previousSets(listOf(old, latest), bench.id).map { it.id })
    }
}
