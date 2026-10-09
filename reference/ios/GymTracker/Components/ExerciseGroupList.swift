import SwiftUI

/// Displays a workout's exercises and their sets. Used on the workout
/// completion screen and the history detail screen.
struct ExerciseGroupList: View {
    let groups: [WorkoutExerciseGroup]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ForEach(groups) { group in
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text(group.exercise.name)
                            .font(.headline)
                        Spacer()
                        Text("\(group.sets.count) \(group.sets.count == 1 ? "set" : "sets")")
                            .font(.caption)
                            .foregroundStyle(Theme.secondaryText)
                    }
                    ForEach(group.sets) { set in
                        HStack {
                            Text("\(set.setNumber)")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Theme.secondaryText)
                                .frame(width: 26, height: 26)
                                .background(Circle().fill(Theme.fieldBackground))
                            Spacer()
                            Text("\(AppFormatters.weightWithUnit(set.weight)) × \(set.reps)")
                                .font(.subheadline.weight(.medium))
                                .monospacedDigit()
                        }
                    }
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .cardStyle(cornerRadius: Theme.cornerRadius)
            }
        }
    }
}