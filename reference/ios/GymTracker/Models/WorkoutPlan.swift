import Foundation
import SwiftData

/// A reusable workout template: a named list of exercises that can be
/// applied to the Workout tab all at once.
///
/// `exerciseNames` stores exercise names (not IDs) so preset plans keep
/// resolving even if the exercise library is re-seeded. Custom plans
/// created by the user work the same way.
@Model
final class WorkoutPlan {
    @Attribute(.unique) var id: UUID
    var name: String
    var exerciseNames: [String]
    var isPreset: Bool
    var sortOrder: Int
    var createdAt: Date

    init(
        name: String,
        exerciseNames: [String],
        isPreset: Bool,
        sortOrder: Int = 0,
        createdAt: Date = .now
    ) {
        self.id = UUID()
        self.name = name
        self.exerciseNames = exerciseNames
        self.isPreset = isPreset
        self.sortOrder = sortOrder
        self.createdAt = createdAt
    }
}
