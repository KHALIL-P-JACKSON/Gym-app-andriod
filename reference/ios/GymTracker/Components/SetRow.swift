import SwiftUI

/// A field position that can receive keyboard focus within a set row.
enum SetField: Hashable {
    case weight(UUID)
    case reps(UUID)
}

/// One row in an exercise card: set number, weight, reps, remove button.
///
/// Weight and reps are plain text fields with numeric keyboards so the
/// user can type quickly between sets.
struct SetRow: View {
    @Bindable var set: DraftSet
    let setNumber: Int
    var onRemove: () -> Void
    var weightField: FocusState<SetField?>.Binding
    var repsField: FocusState<SetField?>.Binding

    var body: some View {
        HStack(spacing: 8) {
            Text("\(setNumber)")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.secondaryText)
                .frame(width: 24, height: 24)
                .background(Circle().fill(Theme.fieldBackground))

            TextField("0", text: $set.weightText)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.center)
                .font(.title3.weight(.medium).monospacedDigit())
                .padding(.vertical, 8)
                .background(Theme.fieldBackground, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .focused(weightField, equals: .weight(set.id))

            TextField("0", text: $set.repsText)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.title3.weight(.medium).monospacedDigit())
                .padding(.vertical, 8)
                .background(Theme.fieldBackground, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .focused(repsField, equals: .reps(set.id))

            Button(action: onRemove) {
                Image(systemName: "minus.circle.fill")
                    .font(.title3)
                    .foregroundStyle(Theme.secondaryText)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Remove set")
        }
    }
}