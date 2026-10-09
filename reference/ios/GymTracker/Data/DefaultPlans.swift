import Foundation

/// Preset workout splits that ship with the app. Plans reference
/// exercises by name so they resolve against whatever is installed in
/// the exercise library.
struct PlanSeed {
    let name: String
    let exerciseNames: [String]
}

enum DefaultPlans {

    static let seeds: [PlanSeed] = [
        PlanSeed(
            name: "Push Day",
            exerciseNames: ["Bench Press", "Incline Dumbbell Press", "Shoulder Press", "Lateral Raise", "Tricep Pushdown", "Tricep Overhead Extension"]
        ),
        PlanSeed(
            name: "Pull Day",
            exerciseNames: ["Pull Ups", "Lat Pulldown", "Barbell Row", "Seated Cable Row", "Bicep Curl", "Hammer Curl"]
        ),
        PlanSeed(
            name: "Leg Day",
            exerciseNames: ["Squat", "Leg Press", "Romanian Deadlift", "Leg Extension", "Leg Curl", "Calf Raise"]
        ),
        PlanSeed(
            name: "Upper Body",
            exerciseNames: ["Bench Press", "Seated Cable Row", "Shoulder Press", "Lat Pulldown", "Bicep Curl", "Tricep Pushdown"]
        ),
        PlanSeed(
            name: "Full Body",
            exerciseNames: ["Squat", "Bench Press", "Barbell Row", "Shoulder Press", "Plank"]
        ),
        PlanSeed(
            name: "Calisthenics",
            exerciseNames: ["Push Ups", "Pull Ups (Bodyweight)", "Bodyweight Dips", "Bodyweight Squats", "Lunges", "Plank", "Burpees"]
        ),
    ]
}
