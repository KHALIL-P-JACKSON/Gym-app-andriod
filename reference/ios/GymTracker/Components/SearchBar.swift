import SwiftUI

/// A themed search field used to filter the exercises in a workout.
///
/// Rounds the field to a pill shape and adapts to light/dark mode via
/// the shared `Theme` system colors, matching the app's control look.
struct SearchBar: View {
    let placeholder: String
    let onSubmit: () -> Void
    let onClear: () -> Void
    @Binding var text: String

    var body: some View {
        HStack(spacing: 8) {
            TextField(text: $text) {
                Text(placeholder)
            }
            .onSubmit { onSubmit() }
            .textFieldStyle(.plain)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 11)
            .padding(.vertical, 7)
            .background(RoundedRectangle(cornerRadius: 999, style: .continuous).fill(Theme.fieldBackground))
            .overlay(RoundedRectangle(cornerRadius: 999, style: .continuous).stroke(Theme.primary.opacity(0.35), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 999, style: .continuous))

            if !text.isEmpty {
                Button {
                    onClear()
                } label: {
                    Image(systemName: "xmark.circle")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Theme.primary)
                }
                .frame(width: 30, height: 30)
                .background {
                    RoundedRectangle(cornerRadius: 999, style: .continuous)
                        .fill(Theme.fieldBackground)
                }
            } else {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Theme.primary.opacity(0.45))
            }
        }
        .padding(.horizontal, 2)
    }
}

#Preview {
    SearchBar(
        placeholder: "Search exercises",
        onSubmit: {},
        onClear: {},
        text: .constant("")
    )
    .background(Theme.background)
}
