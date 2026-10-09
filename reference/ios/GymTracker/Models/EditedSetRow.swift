import Foundation

/// One editable row in the history edit screen. Backed by plain text so
/// the user can type freely; weight/reps parse on save.
///
/// `weightText` is in `entryUnit`; `weight` converts to stored pounds.
struct EditedSetRow: Identifiable {
    let id = UUID()
    /// Non-nil when this row edits an already-saved set.
    var existingSetID: UUID?
    var exercise: Exercise
    var weightText: String
    var repsText: String
    var entryUnit: WeightUnit

    /// Parsed weight converted to stored pounds.
    var weight: Double? {
        guard let value = Double(weightText) else { return nil }
        return entryUnit.toPounds(value)
    }
    var reps: Int? { Int(repsText) }

    var isValid: Bool {
        (weight ?? 0) > 0 && (reps ?? 0) > 0
    }

    static func from(set: WorkoutSet, unit: WeightUnit = AppSettings.weightUnit) -> EditedSetRow? {
        guard let exercise = set.exercise else { return nil }
        let display = unit.fromPounds(set.weight)
        return EditedSetRow(
            existingSetID: set.id,
            exercise: exercise,
            weightText: display == display.rounded()
                ? String(Int(display)) : String(display),
            repsText: String(set.reps),
            entryUnit: unit
        )
    }

    static func blank(exercise: Exercise, unit: WeightUnit = AppSettings.weightUnit) -> EditedSetRow {
        EditedSetRow(exercise: exercise, weightText: "", repsText: "", entryUnit: unit)
    }
}
