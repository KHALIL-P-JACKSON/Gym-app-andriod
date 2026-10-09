import Foundation
import SwiftData

/// Persistence helpers for workout plans (preset splits + user plans).
struct PlanStore {

    /// Inserts missing preset plans and fixes sort order. User edits to
    /// preset plans (rename / exercises) are preserved — only missing
    /// presets are inserted and sort order is repaired.
    static func seedIfNeeded(in context: ModelContext) {
        let existing = (try? context.fetch(FetchDescriptor<WorkoutPlan>())) ?? []
        let existingByName = Dictionary(
            existing.map { ($0.name, $0) },
            uniquingKeysWith: { first, _ in first }
        )
        var madeChanges = false
        for (index, seed) in DefaultPlans.seeds.enumerated() {
            if let installed = existingByName[seed.name] {
                // Preserve user edits: never overwrite name/exercises.
                if installed.sortOrder != index || !installed.isPreset {
                    installed.sortOrder = index
                    installed.isPreset = true
                    madeChanges = true
                }
                // Backfill exercises only if the plan somehow has none.
                if installed.exerciseNames.isEmpty {
                    installed.exerciseNames = seed.exerciseNames
                    madeChanges = true
                }
            } else {
                context.insert(WorkoutPlan(
                    name: seed.name,
                    exerciseNames: seed.exerciseNames,
                    isPreset: true,
                    sortOrder: index
                ))
                madeChanges = true
            }
        }
        // Shift user plans after presets so ordering stays stable.
        let userPlans = existing.filter { !$0.isPreset }.sorted { $0.createdAt < $1.createdAt }
        for (offset, plan) in userPlans.enumerated() {
            let target = DefaultPlans.seeds.count + offset
            if plan.sortOrder != target {
                plan.sortOrder = target
                madeChanges = true
            }
        }
        guard madeChanges else { return }
        try? context.save()
    }

    /// Creates a user plan from the given name + exercise names.
    @discardableResult
    static func create(name: String, exerciseNames: [String], in context: ModelContext) -> WorkoutPlan? {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, !exerciseNames.isEmpty else { return nil }
        let existing = (try? context.fetch(FetchDescriptor<WorkoutPlan>())) ?? []
        let nextOrder = (existing.map(\.sortOrder).max() ?? -1) + 1
        let plan = WorkoutPlan(name: trimmed, exerciseNames: exerciseNames, isPreset: false, sortOrder: nextOrder)
        context.insert(plan)
        try? context.save()
        return plan
    }

    /// Updates a plan's name + exercises and persists the change.
    /// Empty names/exercise lists are rejected.
    @discardableResult
    static func update(_ plan: WorkoutPlan, name: String, exerciseNames: [String], in context: ModelContext) -> Bool {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, !exerciseNames.isEmpty else { return false }
        plan.name = trimmed
        plan.exerciseNames = exerciseNames
        try? context.save()
        return true
    }

    static func delete(_ plan: WorkoutPlan, in context: ModelContext) {
        context.delete(plan)
        try? context.save()
    }

    /// Resolves a plan's exercise names to installed Exercise objects,
    /// preserving the plan's order and skipping missing names.
    static func resolveExercises(for plan: WorkoutPlan, from library: [Exercise]) -> [Exercise] {
        let byName = Dictionary(library.map { ($0.name, $0) }, uniquingKeysWith: { first, _ in first })
        return plan.exerciseNames.compactMap { byName[$0] }
    }
}
