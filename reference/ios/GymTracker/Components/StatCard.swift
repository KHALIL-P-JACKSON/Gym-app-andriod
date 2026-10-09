import SwiftUI

/// A compact value + label tile used on dashboard screens.
struct StatCard: View {
    let title: String
    let value: String
    var iconName: String?
    var accentColor: Color = Theme.primary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let iconName {
                Image(systemName: iconName)
                    .font(.subheadline)
                    .foregroundStyle(accentColor)
            }
            Text(value)
                .font(.title3.weight(.bold))
                .monospacedDigit()
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            Text(title)
                .font(.caption)
                .foregroundStyle(Theme.secondaryText)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .cardStyle(cornerRadius: Theme.cornerRadius)
    }
}

#Preview {
    HStack(spacing: 10) {
        StatCard(title: "Day Streak", value: "3", iconName: "flame.fill", accentColor: Theme.warning)
        StatCard(title: "Workouts", value: "7", iconName: "dumbbell.fill")
        StatCard(title: "Volume", value: "12,340 lb", iconName: "scalemass")
    }
    .padding()
    .background(Theme.background)
}