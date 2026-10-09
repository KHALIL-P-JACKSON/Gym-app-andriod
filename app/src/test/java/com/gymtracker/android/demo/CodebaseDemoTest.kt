package com.gymtracker.android.demo

import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.service.CsvExporter
import com.gymtracker.android.domain.service.ProgressCalculator
import com.gymtracker.android.domain.service.WorkoutService
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Executable teaching example. Uses the real domain code and prints a deterministic story.
 * Run: ./gradlew testDebugUnitTest --tests 'com.gymtracker.android.demo.CodebaseDemoTest' --info --rerun-tasks
 * Room and Compose are exercised separately by the existing Android device tests.
 */
class CodebaseDemoTest {
    @Test fun followOneWorkoutThroughTheDomain() {
        val bench = Exercise(
            id = "demo-bench", name = "Bench Press",
            muscleGroup = MuscleGroup.CHEST, category = Category.PUSH
        )
        val enteredSet = DraftSet(id = "demo-set", weightText = "185", repsText = "8", entryUnit = WeightUnit.LB)
        val session = WorkoutSession(
            name = "  Demo workout  ", date = Instant.parse("2026-10-01T12:00:00Z"),
            exercises = listOf(DraftExercise(bench, listOf(enteredSet, DraftSet(id = "incomplete", repsText = "8"))))
        )

        println("\n[1] PRESENTATION HAND-OFF: text fields become a WorkoutSession")
        println("    weightText=${enteredSet.weightText}, repsText=${enteredSet.repsText}, entryUnit=${enteredSet.entryUnit}")
        println("    valid=${session.validSetCount}, incomplete=${session.invalidSetCount}")
        assertEquals(1, session.validSetCount)
        assertEquals(1, session.invalidSetCount)

        println("[2] FINISH: the app's ViewModel asks to discard the incomplete row")
        println("    This example represents choosing Discard & Save.")
        val saved = WorkoutService.build(session)
        assertEquals("Demo workout", saved.name)
        assertEquals(1, saved.sets.size)
        assertEquals(185.0, saved.sets.single().weightLb, 0.000001)
        assertEquals(1, saved.sets.single().setNumber)
        assertEquals(0, saved.sets.single().sortOrder)
        println("[3] DOMAIN: WorkoutService.build() returns ${saved.sets.size} valid set")
        println("    canonicalWeight=${saved.sets.single().weightLb} lb, volume=${saved.volume} lb")
        assertEquals(1480.0, saved.volume, 0.000001)

        println("[4] DATA HAND-OFF: the app passes this Workout to TrackerRepository.saveWorkout()")
        println("    RoomTrackerRepository writes workout + set rows in one transaction.")
        println("    This JVM example doesn't open Room; RoomRepositoryTest verifies disk persistence.")

        val kilograms = enteredSet.inUnit(WeightUnit.KG)
        assertEquals(WeightUnit.KG, kilograms.entryUnit)
        assertEquals(185.0, requireNotNull(kilograms.weightLb), 0.000001)
        println("[5] UNIT CHANGE: kg entry text changes while stored pounds stay equal")
        println("    entry=${kilograms.weightText} kg, canonical=${kilograms.weightLb} lb")

        val editing = WorkoutService.edit(saved, listOf(bench), WeightUnit.KG)
        val exerciseDraft = editing.exercises.single()
        val changedRow = exerciseDraft.sets.single().copy(
            weightText = WeightUnit.KG.fromPounds(195.0).toString(), repsText = "8"
        )
        val edited = WorkoutService.build(editing.copy(
            notes = "  Added weight  ",
            exercises = listOf(exerciseDraft.copy(sets = listOf(changedRow)))
        ))
        assertEquals(saved.id, edited.id)
        assertEquals(saved.sets.single().id, edited.sets.single().id)
        assertEquals("Added weight", edited.notes)
        assertEquals(195.0, edited.sets.single().weightLb, 0.000001)
        println("[6] EDIT: workout/set IDs survive; canonical weight becomes 195 lb")

        val later = edited.copy(
            id = "later-workout", date = Instant.parse("2026-10-02T12:00:00Z"),
            sets = edited.sets.map { it.copy(id = "later-set") }
        )
        val data = TrackerData(exercises = listOf(bench), workouts = listOf(saved, later))
        val history = ProgressCalculator.history(data.workouts, bench.id)
        val comparison = ProgressCalculator.comparison(history)
        assertEquals(2, history.size)
        assertEquals(10.0, comparison.change, 0.000001)
        assertEquals(234.3333333333, saved.sets.single().estimatedOneRepMax, 0.000001)
        assertEquals(247.0, requireNotNull(comparison.current).estimatedOneRepMax, 0.000001)
        println("[7] PROGRESS: best 195 lb vs previous 185 lb; change +10 lb")
        println("    Epley estimate: ${comparison.current?.estimatedOneRepMax} lb")
        println("    The estimate is derived from saved models.")

        val csv = CsvExporter.export(data)
        assertTrue(csv.startsWith("date,workout,exercise,set,weight_lb,reps"))
        assertTrue(csv.contains("Bench Press,1,195.0,8"))
        println("[8] EXPORT: CSV always uses canonical pounds\n$csv")

        val invalid = WorkoutSession(exercises = listOf(
            DraftExercise(bench, listOf(DraftSet(weightText = "185", repsText = "")))
        ))
        assertEquals(0, invalid.validSetCount)
        val error = runCatching { WorkoutService.build(invalid) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals("Enter at least one valid set.", error?.message)
        println("[9] INVALID INPUT: empty reps cannot build a saved workout")
        println("    message=${error?.message}\n")
    }
}
