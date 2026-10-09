import Foundation
import SwiftData

/// Pure calculations used by the Home and Progress screens.
/// Keeping these separate from views makes them easy to test and extend.
enum ProgressCalculator {

    /// Epley one-rep-max estimate: weight × (1 + reps / 30).
    static func estimatedOneRepMax(weight: Double, reps: Int) -> Double {
        weight * (1.0 + Double(reps) / 30.0)
    }

    /// The heaviest set (by weight) in `sets`.
    static func bestWeight(in sets: [WorkoutSet]) -> Double? {
        sets.map(\.weight).max()
    }

    /// Highest number of reps in a single set in `sets`.
    static func bestReps(in sets: [WorkoutSet]) -> Int? {
        sets.map(\.reps).max()
    }

    /// The "strongest" recorded set, measured by estimated 1RM.
    static func bestSet(in sets: [WorkoutSet]) -> WorkoutSet? {
        sets.max { $0.estimatedOneRepMax < $1.estimatedOneRepMax }
    }

    /// Best estimated 1RM across all sets in `sets`.
    static func bestEstimatedOneRepMax(in sets: [WorkoutSet]) -> Double? {
        bestSet(in: sets)?.estimatedOneRepMax
    }

    // MARK: - History

    /// One workout's worth of performance for a single exercise.
    struct ExercisePerformance: Identifiable {
        var id: Date { date }
        let date: Date
        /// Strongest set of that workout, measured by estimated 1RM.
        let bestSet: WorkoutSet?
        /// Sum of weight × reps for all sets that day.
        let volume: Double

        var estimatedOneRepMax: Double { bestSet?.estimatedOneRepMax ?? 0 }
        var bestWeight: Double { bestSet?.weight ?? 0 }
    }

    /// Groups an exercise's recorded sets by workout, oldest to newest.
    static func performanceHistory(in sets: [WorkoutSet]) -> [ExercisePerformance] {
        let grouped = Dictionary(grouping: sets) { $0.workout?.id ?? UUID() }
        let performances = grouped.compactMap { _, workoutSets -> ExercisePerformance? in
            guard let workout = workoutSets.first?.workout else { return nil }
            let best = bestSet(in: workoutSets)
            let volume = workoutSets.reduce(0) { $0 + $1.weight * Double($1.reps) }
            return ExercisePerformance(date: workout.date, bestSet: best, volume: volume)
        }
        return performances.sorted { $0.date < $1.date }
    }

    /// The best workout and the second-best workout, used for
    /// "current vs previous" comparisons.
    struct BestComparison {
        let currentBest: WorkoutSet?
        let previousBest: WorkoutSet?

        /// Difference in weight between the two best sets.
        var change: Double {
            (currentBest?.weight ?? 0) - (previousBest?.weight ?? 0)
        }
    }

    static func bestComparison(in history: [ExercisePerformance]) -> BestComparison {
        let ranked = history.sorted { lhs, rhs in
            if lhs.bestWeight != rhs.bestWeight {
                return lhs.bestWeight > rhs.bestWeight
            }
            return lhs.estimatedOneRepMax > rhs.estimatedOneRepMax
        }
        return BestComparison(
            currentBest: ranked.first?.bestSet,
            previousBest: ranked.dropFirst().first?.bestSet
        )
    }

    // MARK: - Streaks

    /// Number of consecutive days — ending today or yesterday — that a
    /// workout was recorded.
    static func workoutStreak(workoutDates: [Date], calendar: Calendar = .current) -> Int {
        let workoutDays = Set(workoutDates.map { calendar.startOfDay(for: $0) })

        var cursor = calendar.startOfDay(for: .now)
        if !workoutDays.contains(cursor) {
            guard let yesterday = calendar.date(byAdding: .day, value: -1, to: cursor) else { return 0 }
            cursor = yesterday
        }

        var streak = 0
        while workoutDays.contains(cursor) {
            streak += 1
            guard let previous = calendar.date(byAdding: .day, value: -1, to: cursor) else { break }
            cursor = previous
        }
        return streak
    }
}