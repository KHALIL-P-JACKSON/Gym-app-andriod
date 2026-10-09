import SwiftUI

/// A friendly placeholder shown when a screen has no data yet.
struct EmptyStateView: View {
    let iconName: String
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: iconName)
                .font(.system(size: 44, weight: .medium))
                .foregroundStyle(Theme.primary)
                .frame(width: 84, height: 84)
                .background(Circle().fill(Theme.primary.opacity(0.14)))

            Text(title)
                .font(.headline)

            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryText)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)

            if let actionTitle, let action {
                Button {
                    action()
                } label: {
                    Text(actionTitle)
                        .font(.headline)
                        .padding(.horizontal, 4)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.primary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .padding(.horizontal, 20)
    }
}

#Preview {
    EmptyStateView(
        iconName: "dumbbell",
        title: "No workouts yet",
        message: "Start your first workout and begin tracking your progress.",
        actionTitle: "Start Workout",
        action: {}
    )
    .background(Theme.background)
}