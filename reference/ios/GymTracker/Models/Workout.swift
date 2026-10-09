import Foundation
import SwiftData

/// A single completed training session.
@Model
final class Workout {
    @Attribute(.unique) var id: UUID
    var date: Date
    var name: String
    var duration: TimeInterval?
    var notes: String?

    /// All sets recorded in this workout.
    /// Deleting a workout cascades to its sets.
    @Relationship(deleteRule: .cascade, inverse: \WorkoutSet.workout)
    var sets: [WorkoutSet] = []

    init(
        date: Date = .now,
        name: String,
        duration: TimeInterval? = nil,
        notes: String? = nil
    ) {
        self.id = UUID()
        self.date = date
        self.name = name
        self.duration = duration
        self.notes = notes
    }

    // MARK: - Convenience

    /// Sets ordered by the order they were logged in.
    var orderedSets: [WorkoutSet] {
        sets.sorted { $0.sortOrder < $1.sortOrder }
    }

    /// Number of distinct exercises in this workout.
    var exerciseCount: Int {
        Set(orderedSets.compactMap { $0.exercise?.id }).count
    }

    /// Total number of sets.
    var setCount: Int {
        orderedSets.count
    }

    /// Sum of weight × reps across all sets.
    var totalVolume: Double {
        orderedSets.reduce(0) { $0 + $1.weight * Double($1.reps) }
    }

    /// Exercises grouped in the order they were performed.
    var exerciseGroups: [WorkoutExerciseGroup] {
        var groups: [WorkoutExerciseGroup] = []
        var lastExerciseID: UUID?
        for set in orderedSets {
            guard let exercise = set.exercise else { continue }
            if let lastID = lastExerciseID, lastID == exercise.id {
                groups[groups.count - 1].sets.append(set)
            } else {
                groups.append(WorkoutExerciseGroup(exercise: exercise, sets: [set]))
                lastExerciseID = exercise.id
            }
        }
        return groups
    }
}

/// One exercise and its sets within a workout, in display order.
struct WorkoutExerciseGroup: Identifiable {
    var id: UUID { exercise.id }
    let exercise: Exercise
    var sets: [WorkoutSet]
}