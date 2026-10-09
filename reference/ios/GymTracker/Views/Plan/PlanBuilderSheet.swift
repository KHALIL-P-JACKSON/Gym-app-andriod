import SwiftUI
import SwiftData

/// Builder sheet for a plan: name it, then multi-select exercises from
/// the library. Used both for creating a new plan and editing an
/// existing one (preset or custom); pass `plan` to pre-fill.
struct PlanBuilderSheet: View {
    /// When non-nil the sheet edits this plan instead of creating one.
    var plan: WorkoutPlan? = nil
    let exercises: [Exercise]
    var onSave: (String, [String]) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var selectedIDs: Set<UUID> = []
    @State private var didPrefill = false

    private var selectedNames: [String] {
        exercises.filter { selectedIDs.contains($0.id) }.map(\.name)
    }

    var body: some View {
        NavigationStack {
            List {
                Section("Plan name") {
                    TextField("e.g. Push Day Plus", text: $name)
                        .textInputAutocapitalization(.words)
                }
                ForEach(MuscleGroup.allCases) { group in
                    let groupExercises = exercises.filter { $0.muscleGroup == group.rawValue }
                    if !groupExercises.isEmpty {
                        Section(group.displayName) {
                            ForEach(groupExercises) { exercise in
                                Button {
                                    if selectedIDs.contains(exercise.id) {
                                        selectedIDs.remove(exercise.id)
                                    } else {
                                        selectedIDs.insert(exercise.id)
                                    }
                                } label: {
                                    HStack {
                                        Text(exercise.name)
                                            .foregroundStyle(Color.primary)
                                        Spacer()
                                        if selectedIDs.contains(exercise.id) {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundStyle(Theme.primary)
                                        } else {
                                            Image(systemName: "circle")
                                                .foregroundStyle(Theme.secondaryText)
                                        }
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
            .navigationTitle(plan == nil ? "New Plan" : "Edit Plan")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(name, selectedNames)
                    }
                    .disabled(
                        name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                            || selectedIDs.isEmpty
                    )
                }
            }
            .onAppear {
                // Pre-fill name + selection once when editing a plan.
                guard !didPrefill, let plan else { return }
                didPrefill = true
                name = plan.name
                let names = Set(plan.exerciseNames)
                selectedIDs = Set(exercises.filter { names.contains($0.name) }.map(\.id))
            }
        }
    }
}
