import SwiftUI
import SwiftData

/// A card for one exercise being logged in the current workout.
/// Shows the user's last performance, set inputs, Add Set / Complete /
/// Edit controls, and an inline Finish Workout button when this is the
/// most recently added exercise.
struct ExerciseCardView: View {
    @Bindable var draft: DraftExercise
    var onAddSet: () -> Void
    var onComplete: () -> Void
    var onEdit: () -> Void
    var onRemoveExercise: () -> Void
    var onRemoveSet: (DraftSet) -> Void
    var weightField: FocusState<SetField?>.Binding
    var repsField: FocusState<SetField?>.Binding
    /// When true this card is the last exercise in the session, so it
    /// hosts the inline Finish Workout button directly underneath.
    var isLastExercise: Bool = false
    var canFinishWorkout: Bool = false
    var onFinishWorkout: () -> Void = {}

    @State private var pendingFocus: UUID?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header
            if !draft.previousSets.isEmpty {
                lastWorkoutStrip
            }
            columnHeader
            ForEach(Array(draft.sets.enumerated()), id: \.element.id) { index, set in
                if draft.isComplete {
                    lockedSetRow(set: set, setNumber: index + 1)
                } else {
                    SetRow(
                        set: set,
                        setNumber: index + 1,
                        onRemove: { onRemoveSet(set) },
                        weightField: weightField,
                        repsField: repsField
                    )
                    .onAppear {
                        if pendingFocus == set.id {
                            weightField.wrappedValue = .weight(set.id)
                            pendingFocus = nil
                        }
                    }
                }
            }
            actionButtons
            if isLastExercise {
                inlineFinishButton
            }
        }
        .padding(Theme.padding)
        .cardStyle()
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(draft.exercise.name)
                        .font(.headline)
                    if draft.isComplete {
                        Label("Done", systemImage: "checkmark.circle.fill")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Theme.success)
                    }
                }
                Text(draft.exercise.muscleGroup.capitalized)
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryText)
            }
            Spacer()
            Button(action: onRemoveExercise) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title3)
                    .foregroundStyle(Theme.secondaryText)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Remove \(draft.exercise.name)")
        }
    }

    /// Summary of the user's most recent session with this exercise.
    /// Previous weights are stored in pounds and converted for display.
    private var lastWorkoutStrip: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Last Workout")
                    .sectionHeaderStyle()
                Spacer()
                if let date = draft.previousSets.first?.workout?.date {
                    Text(AppFormatters.shortDate(date))
                        .font(.caption)
                        .foregroundStyle(Theme.secondaryText)
                }
            }
            ForEach(draft.previousSets.prefix(3)) { set in
                HStack {
                    Text("Set \(set.setNumber)")
                        .foregroundStyle(Theme.secondaryText)
                    Spacer()
                    Text("\(AppFormatters.weightWithUnit(set.weight)) × \(set.reps)")
                        .monospacedDigit()
                }
                .font(.subheadline)
            }
        }
        .padding(12)
        .background(
            Theme.primary.opacity(0.10),
            in: RoundedRectangle(cornerRadius: 12, style: .continuous)
        )
    }

    private var columnHeader: some View {
        HStack(spacing: 8) {
            Text("Set")
                .frame(width: 24, alignment: .leading)
            Text("Weight (\(AppSettings.weightUnit.abbreviation))")
                .frame(maxWidth: .infinity)
            Text("Reps")
                .frame(maxWidth: .infinity)
            Text("")
                .frame(width: 28)
        }
        .font(.caption.weight(.semibold))
        .foregroundStyle(Theme.secondaryText)
    }

    /// Locked rows show the entered value in its entry unit (so what the
    /// user sees matches what they typed, even in kg mode).
    private func lockedSetRow(set: DraftSet, setNumber: Int) -> some View {
        HStack(spacing: 8) {
            Text("\(setNumber)")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.secondaryText)
                .frame(width: 24, height: 24)
                .background(Circle().fill(Theme.fieldBackground))
            Text(AppFormatters.weight(set.weight ?? 0, unit: set.entryUnit))
                .frame(maxWidth: .infinity)
                .font(.title3.weight(.medium).monospacedDigit())
                .padding(.vertical, 8)
                .background(Theme.fieldBackground, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            Text("\(set.reps ?? 0)")
                .frame(maxWidth: .infinity)
                .font(.title3.weight(.medium).monospacedDigit())
                .padding(.vertical, 8)
                .background(Theme.fieldBackground, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            Image(systemName: "lock.fill")
                .font(.caption)
                .foregroundStyle(Theme.secondaryText)
                .frame(width: 28)
        }
    }

    /// Add Set + Complete (or Edit when locked) side by side.
    private var actionButtons: some View {
        HStack(spacing: 10) {
            if draft.isComplete {
                Button {
                    onEdit()
                } label: {
                    Label("Edit", systemImage: "pencil")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                }
                .buttonStyle(.bordered)
                .tint(Theme.primary)
            } else {
                Button {
                    onAddSet()
                    pendingFocus = draft.sets.last?.id
                } label: {
                    Label("Add Set", systemImage: "plus")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                }
                .buttonStyle(.bordered)
                .tint(Theme.primary)
                Button(action: onComplete) {
                    Label("Complete", systemImage: "checkmark")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.success)
                .disabled(!draft.hasValidSets)
            }
        }
    }

    /// Finish Workout anchored right below the most recently added
    /// exercise, so it never jumps around as the picker grows.
    @ViewBuilder
    private var inlineFinishButton: some View {
        Button(action: onFinishWorkout) {
            Label("Finish Workout", systemImage: "checkmark")
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
        }
        .buttonStyle(.borderedProminent)
        .tint(Theme.primary)
        .disabled(!canFinishWorkout)
        .padding(.top, 2)
    }
}