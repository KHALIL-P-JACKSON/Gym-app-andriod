import Foundation
import Observation
import SwiftData

/// In-memory representation of the workout currently being logged.
///
/// Nothing is written to disk until the user finishes the workout, which
/// keeps the database clean and makes deleting a set or exercise free.
@Observable
final class WorkoutSession {
    var name: String
    /// The date this workout took place. Normally "now", but can be a
    /// past day when backfilling earlier workouts from History.
    var date: Date
    var exercises: [DraftExercise]

    init(name: String = "Workout", date: Date = .now, exercises: [DraftExercise] = []) {
        self.name = name
        self.date = date
        self.exercises = exercises
    }

    var isEmpty: Bool { exercises.isEmpty }

    func contains(_ exercise: Exercise) -> Bool {
        exercises.contains { $0.exercise.id == exercise.id }
    }

    func addExercise(_ exercise: Exercise) {
        guard !contains(exercise) else { return }
        exercises.append(DraftExercise(exercise: exercise))
    }

    /// Adds every exercise in the list that isn't already in the session.
    /// Returns the number of exercises actually added.
    @discardableResult
    func addExercises(_ exercisesToAdd: [Exercise]) -> Int {
        var added = 0
        for exercise in exercisesToAdd where !contains(exercise) {
            exercises.append(DraftExercise(exercise: exercise))
            added += 1
        }
        return added
    }

    func removeExercise(at index: Int) {
        guard exercises.indices.contains(index) else { return }
        exercises.remove(at: index)
    }

    func reset() {
        name = "Workout"
        date = .now
        exercises = []
    }

    /// Sets that have both a weight and a rep count.
    var validSets: [DraftSet] {
        exercises.flatMap { $0.sets }.filter { $0.isValid }
    }

    var validSetCount: Int { validSets.count }
}

/// A preset exercise that was added to the current workout session.
@Observable
final class DraftExercise {
    let exercise: Exercise
    var sets: [DraftSet]
    /// Sets from the user's most recent workout with this exercise.
    var previousSets: [WorkoutSet] = []
    /// When true the exercise is locked: values can't be edited until
    /// the user taps Edit again.
    var isComplete: Bool = false

    init(exercise: Exercise, sets: [DraftSet] = []) {
        self.exercise = exercise
        self.sets = sets.isEmpty ? [DraftSet()] : sets
    }

    /// All sets have a weight and rep count (and at least one set exists).
    var hasValidSets: Bool {
        !sets.isEmpty && sets.allSatisfy { $0.isValid }
    }
}

/// A partially entered set. Weight and reps are kept as text so the user
/// can type freely; they are parsed and validated when the workout saves.
///
/// `weightText` is always in `entryUnit`; `weight` converts back to
/// stored pounds.
@Observable
final class DraftSet: Identifiable {
    let id = UUID()
    var weightText: String
    var repsText: String
    /// Unit the weight text was entered in (snapshot of the setting when
    /// the row was created).
    var entryUnit: WeightUnit

    init(weightText: String = "", repsText: String = "", entryUnit: WeightUnit = AppSettings.weightUnit) {
        self.weightText = weightText
        self.repsText = repsText
        self.entryUnit = entryUnit
    }

    /// Parsed weight converted to stored pounds.
    var weight: Double? {
        guard let value = Double(weightText) else { return nil }
        return entryUnit.toPounds(value)
    }
    var reps: Int? { Int(repsText) }

    var isValid: Bool {
        (weight ?? 0) > 0 && (reps ?? 0) > 0
    }
}