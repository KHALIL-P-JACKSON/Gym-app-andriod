import SwiftUI
import SwiftData

@main
struct GymTrackerApp: App {

    /// The app's persistent container. Created once and shared with the
    /// whole view hierarchy through `.modelContainer(container)`.
    let container: ModelContainer

    init() {
        do {
            container = try ModelContainer(
                for: Workout.self, WorkoutSet.self, Exercise.self, WorkoutPlan.self
            )
        } catch {
            fatalError("Failed to create the SwiftData container: \(error)")
        }

        // Seed the preset exercise library the first time the app runs.
        ExerciseSeeder.seedIfNeeded(in: container.mainContext)
        PlanStore.seedIfNeeded(in: container.mainContext)
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .tint(Theme.primary)
        }
        .modelContainer(container)
    }
}