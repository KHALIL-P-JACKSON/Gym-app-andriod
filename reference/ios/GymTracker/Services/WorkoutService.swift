import Foundation
import SwiftData

/// Handles creating and saving workouts, and loading historical info
/// for an exercise.
struct WorkoutService {

    /// Converts the in-memory session into persisted SwiftData models.
    /// Sets without a weight or reps are skipped so invalid data never
    /// reaches the database.
    /// - Returns: the newly saved Workout.
    func save(session: WorkoutSession, in context: ModelContext) throws -> Workout {
        let workout = Workout(date: session.date, name: cleanedName(session.name))

        var globalOrder = 0
        for draftExercise in session.exercises {
            var setNumber = 0
            for draftSet in draftExercise.sets where draftSet.isValid {
                setNumber += 1
                let set = WorkoutSet(
                    weight: draftSet.weight ?? 0,
                    reps: draftSet.reps ?? 0,
                    setNumber: setNumber,
                    sortOrder: globalOrder
                )
                set.exercise = draftExercise.exercise
                set.workout = workout
                workout.sets.append(set)
                context.insert(set)
                globalOrder += 1
            }
        }

        context.insert(workout)
        try context.save()
        return workout
    }

    /// Returns the sets from the most recent workout that used the given
    /// exercise, newest workout first.
    func previousSets(for exercise: Exercise, limit: Int = 3, in context: ModelContext) -> [WorkoutSet] {
        guard let allSets = try? context.fetch(FetchDescriptor<WorkoutSet>()) else { return [] }

        let exerciseSets = allSets.filter { $0.exercise?.id == exercise.id }
        let sorted = exerciseSets.sorted { lhs, rhs in
            let lhsDate = lhs.workout?.date ?? .distantPast
            let rhsDate = rhs.workout?.date ?? .distantPast
            if lhsDate != rhsDate {
                return lhsDate > rhsDate
            }
            return lhs.sortOrder < rhs.sortOrder
        }

        guard let newestWorkoutID = sorted.first?.workout?.id else { return [] }
        return Array(sorted
            .filter { $0.workout?.id == newestWorkoutID }
            .prefix(limit))
    }

    /// Overwrites a saved workout's name, date, notes and full set list
    /// from edited rows. Rows with invalid weight/reps are dropped; if
    /// every row is invalid the workout keeps its existing sets.
    func update(
        workout: Workout,
        name: String,
        date: Date,
        notes: String?,
        rows: [EditedSetRow],
        in context: ModelContext
    ) throws {
        let validRows = rows.filter { $0.isValid }
        guard !validRows.isEmpty else { return }

        workout.name = cleanedName(name)
        workout.date = date
        let trimmedNotes = (notes ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        workout.notes = trimmedNotes.isEmpty ? nil : trimmedNotes

        // Group valid rows by exercise, preserving first-appearance order.
        var order: [UUID] = []
        var grouped: [UUID: [EditedSetRow]] = [:]
        for row in validRows {
            if grouped[row.exercise.id] == nil {
                order.append(row.exercise.id)
                grouped[row.exercise.id] = []
            }
            grouped[row.exercise.id]?.append(row)
        }

        let existingByID = Dictionary(
            workout.sets.map { ($0.id, $0) },
            uniquingKeysWith: { first, _ in first }
        )
        var seenIDs = Set<UUID>()
        var newSets: [WorkoutSet] = []
        var globalOrder = 0

        for exerciseID in order {
            let exerciseRows = grouped[exerciseID] ?? []
            var setNumber = 0
            for row in exerciseRows {
                setNumber += 1
                if let setID = row.existingSetID,
                   let existing = existingByID[setID] {
                    existing.weight = row.weight ?? 0
                    existing.reps = row.reps ?? 0
                    existing.setNumber = setNumber
                    existing.sortOrder = globalOrder
                    existing.exercise = row.exercise
                    existing.workout = workout
                    newSets.append(existing)
                    seenIDs.insert(setID)
                } else {
                    let set = WorkoutSet(
                        weight: row.weight ?? 0,
                        reps: row.reps ?? 0,
                        setNumber: setNumber,
                        sortOrder: globalOrder
                    )
                    set.exercise = row.exercise
                    set.workout = workout
                    context.insert(set)
                    newSets.append(set)
                }
                globalOrder += 1
            }
        }

        // Delete sets the user removed.
        for set in workout.sets where !seenIDs.contains(set.id) {
            // Only delete sets that were part of the old list (new ones
            // are already in newSets).
            if existingByID[set.id] != nil {
                context.delete(set)
            }
        }

        workout.sets = newSets
        try context.save()
    }

    private func cleanedName(_ name: String) -> String {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "Workout" : trimmed
    }
}