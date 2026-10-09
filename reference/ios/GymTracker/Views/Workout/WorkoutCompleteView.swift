import SwiftUI
import SwiftData

/// Full-screen summary shown after a workout is saved successfully.
struct WorkoutCompleteView: View {
    let workout: Workout
    var onDone: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    header
                    statsRow
                    ExerciseGroupList(groups: workout.exerciseGroups)
                    doneButton
                }
                .padding(Theme.padding)
            }
            .background(Theme.background)
            .navigationTitle("Workout Complete")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", action: onDone)
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private var header: some View {
        VStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 52))
                .foregroundStyle(Theme.success)
            Text(workout.name)
                .font(.title2.weight(.bold))
            Text(AppFormatters.longDate(workout.date))
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryText)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 16)
    }

    private var statsRow: some View {
        HStack(spacing: 10) {
            StatCard(
                title: "Exercises",
                value: "\(workout.exerciseCount)",
                iconName: "figure.strengthtraining.traditional"
            )
            StatCard(
                title: "Sets",
                value: "\(workout.setCount)",
                iconName: "number"
            )
            StatCard(
                title: "Volume",
                value: AppFormatters.volume(workout.totalVolume),
                iconName: "scalemass"
            )
        }
    }

    private var doneButton: some View {
        Button(action: onDone) {
            Text("Done")
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
        }
        .buttonStyle(.borderedProminent)
        .tint(Theme.primary)
        .padding(.top, 4)
    }
}