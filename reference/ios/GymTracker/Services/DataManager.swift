import Foundation
import SwiftData

/// Builds a CSV export of every workout + set, and wipes all local data.
struct DataManager {

    /// All workouts newest-first with their ordered sets, for export.
    static func exportRows(in context: ModelContext) -> [ExportRow] {
        let workouts = ((try? context.fetch(FetchDescriptor<Workout>())) ?? [])
            .sorted { $0.date > $1.date }
        return workouts.flatMap { workout in
            workout.orderedSets.map { set in
                ExportRow(
                    date: workout.date,
                    workoutName: workout.name,
                    exerciseName: set.exercise?.name ?? "Unknown",
                    setNumber: set.setNumber,
                    weightPounds: set.weight,
                    reps: set.reps
                )
            }
        }
    }

    /// CSV text with a header row. Weights are exported in pounds so the
    /// file is stable regardless of the display unit.
    static func csvText(rows: [ExportRow]) -> String {
        var lines = ["date,workout,exercise,set,weight_lb,reps"]
        let formatter = ISO8601DateFormatter()
        for row in rows {
            lines.append([
                formatter.string(from: row.date),
                csvField(row.workoutName),
                csvField(row.exerciseName),
                String(row.setNumber),
                String(row.weightPounds),
                String(row.reps),
            ].joined(separator: ","))
        }
        return lines.joined(separator: "\n")
    }

    /// Deletes every workout, set, plan and custom exercise. Presets are
    /// re-seeded on next launch automatically.
    static func deleteAllData(in context: ModelContext) throws {
        for workout in (try? context.fetch(FetchDescriptor<Workout>())) ?? [] {
            context.delete(workout)
        }
        for plan in (try? context.fetch(FetchDescriptor<WorkoutPlan>())) ?? [] {
            context.delete(plan)
        }
        for exercise in ((try? context.fetch(FetchDescriptor<Exercise>())) ?? []).filter({ !$0.isPreset }) {
            context.delete(exercise)
        }
        // Remove orphaned sets not attached to a workout.
        for set in (try? context.fetch(FetchDescriptor<WorkoutSet>())) ?? [] {
            if set.workout == nil {
                context.delete(set)
            }
        }
        try context.save()
        ExerciseSeeder.seedIfNeeded(in: context)
        PlanStore.seedIfNeeded(in: context)
    }

    private static func csvField(_ value: String) -> String {
        // Quote fields containing commas, quotes or newlines.
        guard value.contains(",") || value.contains("\"") || value.contains("\n") else { return value }
        return "\"\(value.replacingOccurrences(of: "\"", with: "\"\""))\""
    }
}

/// One set flattened for CSV export.
struct ExportRow {
    let date: Date
    let workoutName: String
    let exerciseName: String
    let setNumber: Int
    let weightPounds: Double
    let reps: Int
}
