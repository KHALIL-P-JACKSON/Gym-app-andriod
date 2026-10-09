import Foundation
import SwiftData

/// Muscle groups used to organize the exercise picker.
enum MuscleGroup: String, CaseIterable, Identifiable, Hashable {
    case chest, back, shoulders, arms, legs, calisthenics

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .chest: return "Chest"
        case .back: return "Back"
        case .shoulders: return "Shoulders"
        case .arms: return "Arms"
        case .legs: return "Legs"
        case .calisthenics: return "Calisthenics"
        }
    }

    var symbolName: String {
        switch self {
        case .chest: return "figure.strengthtraining.traditional"
        case .back: return "figure.rower"
        case .shoulders: return "figure.arms.open"
        case .arms: return "dumbbell.fill"
        case .legs: return "figure.run"
        case .calisthenics: return "figure.strengthtraining.functional"
        }
    }
}

/// Push / Pull / Legs style category. Stored on the model so future
/// features (templates, day planning) can use it.
enum ExerciseCategory: String, CaseIterable {
    case push, pull, legs

    var displayName: String {
        switch self {
        case .push: return "Push"
        case .pull: return "Pull"
        case .legs: return "Legs"
        }
    }
}

/// Blueprint for a preset exercise.
struct ExerciseSeed {
    let name: String
    let muscleGroup: MuscleGroup
    let category: ExerciseCategory
}

/// The master library of preset exercises.
///
/// These are reference data only — the user's recorded results are never
/// hardcoded here. Every launch the app syncs missing presets onto the
/// device, so adding a new seed below is enough to ship it to existing
/// installs.
enum DefaultExercises {

    static let seeds: [ExerciseSeed] = [
        // Chest
        ExerciseSeed(name: "Bench Press", muscleGroup: .chest, category: .push),
        ExerciseSeed(name: "Smith Machine Bench Press", muscleGroup: .chest, category: .push),
        ExerciseSeed(name: "Incline Bench Press", muscleGroup: .chest, category: .push),
        ExerciseSeed(name: "Dumbbell Bench Press", muscleGroup: .chest, category: .push),
        ExerciseSeed(name: "Incline Dumbbell Press", muscleGroup: .chest, category: .push),
        ExerciseSeed(name: "Pec Dec", muscleGroup: .chest, category: .push),
        ExerciseSeed(name: "Dips", muscleGroup: .chest, category: .push),
        // Back
        ExerciseSeed(name: "Lat Pulldown", muscleGroup: .back, category: .pull),
        ExerciseSeed(name: "Pull Ups", muscleGroup: .back, category: .pull),
        ExerciseSeed(name: "Seated Cable Row", muscleGroup: .back, category: .pull),
        ExerciseSeed(name: "Single Arm Seated Cable Row", muscleGroup: .back, category: .pull),
        ExerciseSeed(name: "Barbell Row", muscleGroup: .back, category: .pull),
        ExerciseSeed(name: "Rear Delt Flys", muscleGroup: .back, category: .pull),
        // Shoulders
        ExerciseSeed(name: "Shoulder Press", muscleGroup: .shoulders, category: .push),
        ExerciseSeed(name: "Lateral Raise", muscleGroup: .shoulders, category: .push),
        // Arms
        ExerciseSeed(name: "Bicep Curl", muscleGroup: .arms, category: .pull),
        ExerciseSeed(name: "Cable Curl", muscleGroup: .arms, category: .pull),
        ExerciseSeed(name: "Hammer Curl", muscleGroup: .arms, category: .pull),
        ExerciseSeed(name: "Cable Hammer Curl", muscleGroup: .arms, category: .pull),
        ExerciseSeed(name: "Tricep Pushdown", muscleGroup: .arms, category: .push),
        ExerciseSeed(name: "Tricep Overhead Extension", muscleGroup: .arms, category: .push),
        // Legs
        ExerciseSeed(name: "Squat", muscleGroup: .legs, category: .legs),
        ExerciseSeed(name: "Leg Press", muscleGroup: .legs, category: .legs),
        ExerciseSeed(name: "Leg Extension", muscleGroup: .legs, category: .legs),
        ExerciseSeed(name: "Leg Curl", muscleGroup: .legs, category: .legs),
        ExerciseSeed(name: "Romanian Deadlift", muscleGroup: .legs, category: .legs),
        ExerciseSeed(name: "Calf Raise", muscleGroup: .legs, category: .legs),
        // Calisthenics
        ExerciseSeed(name: "Push Ups", muscleGroup: .calisthenics, category: .push),
        ExerciseSeed(name: "Pull Ups (Bodyweight)", muscleGroup: .calisthenics, category: .pull),
        ExerciseSeed(name: "Chin Ups", muscleGroup: .calisthenics, category: .pull),
        ExerciseSeed(name: "Bodyweight Dips", muscleGroup: .calisthenics, category: .push),
        ExerciseSeed(name: "Bodyweight Squats", muscleGroup: .calisthenics, category: .legs),
        ExerciseSeed(name: "Lunges", muscleGroup: .calisthenics, category: .legs),
        ExerciseSeed(name: "Plank", muscleGroup: .calisthenics, category: .push),
        ExerciseSeed(name: "Burpees", muscleGroup: .calisthenics, category: .push),
        ExerciseSeed(name: "Mountain Climbers", muscleGroup: .calisthenics, category: .legs),
        ExerciseSeed(name: "Hanging Knee Raises", muscleGroup: .calisthenics, category: .pull),
    ]
}

/// Inserts the preset exercises the first time the app is launched.
enum ExerciseSeeder {

    static func seedIfNeeded(in context: ModelContext) {
        // Load what's already on the device so we only add missing presets.
        // Recorded history is never touched.
        let existing = (try? context.fetch(FetchDescriptor<Exercise>())) ?? []
        let existingByName = Dictionary(
            existing.map { ($0.name, $0) },
            uniquingKeysWith: { first, _ in first }
        )

        var madeChanges = false

        // Insert any preset that isn't installed yet, and line the preset
        // sort order up with the master list above.
        for (index, seed) in DefaultExercises.seeds.enumerated() {
            if let installed = existingByName[seed.name] {
                if installed.sortOrder != index {
                    installed.sortOrder = index
                    madeChanges = true
                }
            } else {
                let exercise = Exercise(
                    name: seed.name,
                    muscleGroup: seed.muscleGroup.rawValue,
                    category: seed.category.rawValue,
                    isPreset: true,
                    sortOrder: index
                )
                context.insert(exercise)
                madeChanges = true
            }
        }

        // User-created exercises (a future feature) are never removed or
        // reordered here.

        guard madeChanges else { return }
        do {
            try context.save()
        } catch {
            print("Failed to sync preset exercise library: \(error)")
        }
    }
}