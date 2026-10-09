import Foundation
import SwiftData

/// A single recorded set (one exercise, one weight, one rep count).
@Model
final class WorkoutSet {
    @Attribute(.unique) var id: UUID
    var weight: Double
    var reps: Int
    /// 1-based number of this set within its exercise.
    var setNumber: Int
    /// Global order of this set within the workout (used for display order).
    var sortOrder: Int
    var isWarmup: Bool
    var notes: String?

    var exercise: Exercise?
    var workout: Workout?

    init(
        weight: Double,
        reps: Int,
        setNumber: Int,
        sortOrder: Int,
        isWarmup: Bool = false,
        notes: String? = nil
    ) {
        self.id = UUID()
        self.weight = weight
        self.reps = reps
        self.setNumber = setNumber
        self.sortOrder = sortOrder
        self.isWarmup = isWarmup
        self.notes = notes
    }

    /// Epley-formula estimate of a one-rep max for this set.
    /// This is an estimate, never a real tested maximum.
    var estimatedOneRepMax: Double {
        guard weight > 0, reps > 0 else { return 0 }
        return weight * (1.0 + Double(reps) / 30.0)
    }
}