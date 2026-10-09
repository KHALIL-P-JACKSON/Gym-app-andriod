import SwiftUI

/// Sheet for creating a custom exercise (name + muscle group).
struct CustomExerciseSheet: View {
    var onSave: (String, MuscleGroup) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var group: MuscleGroup = .chest

    var body: some View {
        NavigationStack {
            Form {
                TextField("Exercise name", text: $name)
                    .textInputAutocapitalization(.words)
                Picker("Muscle group", selection: $group) {
                    ForEach(MuscleGroup.allCases) { item in
                        Text(item.displayName).tag(item)
                    }
                }
            }
            .navigationTitle("Custom Exercise")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onSave(name, group)
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}