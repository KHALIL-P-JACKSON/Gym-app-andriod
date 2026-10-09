import SwiftUI
import SwiftData

/// The grid of preset exercise buttons, grouped by muscle group.
/// Also lets the user create a custom exercise inline.
struct ExercisePickerView: View {
    let exercises: [Exercise]
    let isAdded: (Exercise) -> Bool
    let onSelect: (Exercise) -> Void
    let onCreateCustom: (String, MuscleGroup) -> Void

    @State private var showingCustomSheet = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Select Exercise")
                    .font(.title3.weight(.semibold))
                Spacer()
                Button {
                    showingCustomSheet = true
                } label: {
                    Label("Custom", systemImage: "plus.circle")
                        .font(.subheadline.weight(.semibold))
                }
                .buttonStyle(.bordered)
                .tint(Theme.primary)
            }

            if exercises.isEmpty {
                Text("No exercises available. Restart the app to restore the preset library.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryText)
            } else {
                ForEach(MuscleGroup.allCases) { group in
                    let groupExercises = exercises.filter { $0.muscleGroup == group.rawValue }
                    if !groupExercises.isEmpty {
                        section(group: group, exercises: groupExercises)
                    }
                }
            }
        }
        .sheet(isPresented: $showingCustomSheet) {
            CustomExerciseSheet { name, group in
                onCreateCustom(name, group)
                showingCustomSheet = false
            }
            .presentationDetents([.medium])
        }
    }

    private func section(group: MuscleGroup, exercises: [Exercise]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: group.symbolName)
                    .font(.caption)
                Text(group.displayName)
                    .sectionHeaderStyle()
            }

            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 150), spacing: 10)],
                alignment: .leading,
                spacing: 10
            ) {
                ForEach(exercises) { exercise in
                    exerciseButton(exercise)
                }
            }
        }
    }

    private func exerciseButton(_ exercise: Exercise) -> some View {
        let added = isAdded(exercise)
        return Button {
            onSelect(exercise)
        } label: {
            ZStack(alignment: .leading) {
                // Hidden two-line reference so every button is exactly
                // the size of a two-line name. Short names don't shrink
                // the button, and names longer than two lines are
                // truncated to two lines with an ellipsis.
                Text("Hidden\nReference")
                    .font(.subheadline.weight(.medium))
                    .hidden()

                HStack(spacing: 8) {
                    Image(systemName: added ? "checkmark.circle.fill" : "plus.circle.fill")
                        .foregroundStyle(added ? Theme.success : Theme.primary)
                    Text(exercise.name)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(added ? Color.secondary : Color.primary)
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)
                        .truncationMode(.tail)
                    Spacer(minLength: 0)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(added ? Theme.fieldBackground : Theme.cardBackground)
            )
        }
        .buttonStyle(.plain)
        .disabled(added)
        .accessibilityLabel(exercise.name)
    }
}