import Foundation
import SwiftData

/// A liftable exercise. Preset examples are seeded on first launch;
/// the model also supports user-created exercises in a future version.
@Model
final class Exercise {
    @Attribute(.unique) var id: UUID
    var name: String
    var muscleGroup: String
    var category: String
    var isPreset: Bool
    var sortOrder: Int

    /// Every recorded set that used this exercise.
    @Relationship(deleteRule: .nullify, inverse: \WorkoutSet.exercise)
    var workoutSets: [WorkoutSet] = []

    init(
        name: String,
        muscleGroup: String,
        category: String,
        isPreset: Bool,
        sortOrder: Int = 0
    ) {
        self.id = UUID()
        self.name = name
        self.muscleGroup = muscleGroup
        self.category = category
        self.isPreset = isPreset
        self.sortOrder = sortOrder
    }
}