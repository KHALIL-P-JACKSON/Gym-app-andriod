import Foundation
import SwiftData

/// Sample data used only by Xcode previews. Never used at runtime.
@MainActor
enum PreviewData {

    /// In-memory container pre-populated with the preset exercises.
    static let container: ModelContainer = {
        let configuration = ModelConfiguration(isStoredInMemoryOnly: true)
        let container = try! ModelContainer(
            for: Workout.self, WorkoutSet.self, Exercise.self, WorkoutPlan.self,
            configurations: configuration
        )
        ExerciseSeeder.seedIfNeeded(in: container.mainContext)
        PlanStore.seedIfNeeded(in: container.mainContext)
        return container
    }()

    /// Builds a completed "Push Day" workout with sample sets,
    /// inserted into the in-memory container.
    static func makeSampleWorkout() -> Workout {
        let context = container.mainContext
        let all = (try? context.fetch(FetchDescriptor<Exercise>())) ?? []

        let bench = all.first { $0.name == "Bench Press" } ?? all.first!
        let shoulder = all.first { $0.name == "Shoulder Press" } ?? all.last!
        let triceps = all.first { $0.name == "Tricep Pushdown" } ?? all.first!

        let workout = Workout(date: .now.addingTimeInterval(-86_400), name: "Push Day")
        var order = 0

        let benchSets: [(Double, Int)] = [(185, 8), (185, 8), (175, 8)]
        let shoulderSets: [(Double, Int)] = [(95, 10), (95, 10)]
        let tricepsSets: [(Double, Int)] = [(60, 12), (70, 10)]

        for (exercise, pairs) in [(bench, benchSets), (shoulder, shoulderSets), (triceps, tricepsSets)] {
            for (number, pair) in pairs.enumerated() {
                let set = WorkoutSet(
                    weight: pair.0,
                    reps: pair.1,
                    setNumber: number + 1,
                    sortOrder: order
                )
                set.exercise = exercise
                set.workout = workout
                workout.sets.append(set)
                order += 1
            }
        }

        context.insert(workout)
        try? context.save()
        return workout
    }
}
