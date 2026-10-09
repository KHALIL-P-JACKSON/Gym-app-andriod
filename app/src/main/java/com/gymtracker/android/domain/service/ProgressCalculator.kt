package com.gymtracker.android.domain.service

import com.gymtracker.android.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ProgressCalculator {
    data class Performance(val workoutId: String, val date: Instant, val bestSet: WorkoutSet, val volume: Double)
    data class Comparison(val current: WorkoutSet?, val previous: WorkoutSet?) {
        val change get() = (current?.weightLb ?: 0.0) - (previous?.weightLb ?: 0.0)
    }
    fun history(workouts: List<Workout>, exerciseId: String): List<Performance> =
        workouts.mapNotNull { workout ->
            val sets = workout.sets.filter { it.exerciseId == exerciseId }
            val best = sets.maxByOrNull { it.estimatedOneRepMax } ?: return@mapNotNull null
            Performance(workout.id, workout.date, best, sets.sumOf { it.volume })
        }.sortedBy { it.date }

    /** Matches iOS: rank workout-best sets by weight, breaking ties by estimated 1RM. */
    fun comparison(history: List<Performance>): Comparison {
        val ranked = history.sortedWith(
            compareByDescending<Performance> { it.bestSet.weightLb }.thenByDescending { it.bestSet.estimatedOneRepMax }
        )
        return Comparison(ranked.firstOrNull()?.bestSet, ranked.getOrNull(1)?.bestSet)
    }
    fun streak(
        dates: List<Instant>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()
    ): Int {
        val days = dates.map { it.atZone(zone).toLocalDate() }.toSet()
        var cursor = if (today in days) today else today.minusDays(1)
        var count = 0
        while (cursor in days) { count++; cursor = cursor.minusDays(1) }
        return count
    }
}
