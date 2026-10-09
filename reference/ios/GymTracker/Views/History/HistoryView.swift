import SwiftUI
import SwiftData

/// Chronological list of completed workouts. Also offers "Add Workout"
/// so you can log sessions you did before the app was installed.
struct HistoryView: View {
    @Query(sort: \Workout.date, order: .reverse) private var workouts: [Workout]

    @State private var showWorkoutEditor = false

    var body: some View {
        NavigationStack {
            Group {
                if workouts.isEmpty {
                    VStack(spacing: 0) {
                        header
                        Spacer(minLength: 0)
                        EmptyStateView(
                            iconName: "clock.arrow.circlepath",
                            title: "No workouts yet",
                            message: "Record your first workout and it will show up here."
                        )
                        Spacer(minLength: 0)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .padding(.horizontal, Theme.padding)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 12) {
                            header
                            ForEach(workouts) { workout in
                                NavigationLink {
                                    WorkoutDetailView(workout: workout)
                                } label: {
                                    WorkoutRow(workout: workout)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, Theme.padding)
                        .padding(.bottom, Theme.padding)
                    }
                }
            }
            .background(Theme.background)
            .toolbar(.hidden, for: .navigationBar)
        }
        .sheet(isPresented: $showWorkoutEditor) {
            NavigationStack {
                WorkoutEditor(canPickDate: true) {
                    showWorkoutEditor = false
                }
                .navigationTitle("Add Workout")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { showWorkoutEditor = false }
                    }
                }
            }
            .presentationDetents([.large])
        }
    }

    /// Inline header: the "History" title and the add button side by
    /// side on the same line at the top of the screen.
    private var header: some View {
        HStack(spacing: 12) {
            Text("History")
                .font(.largeTitle.weight(.bold))
            Spacer()
            Button {
                showWorkoutEditor = true
            } label: {
                Image(systemName: "plus.circle.fill")
                    .font(.title)
                    .foregroundStyle(Theme.primary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Add Workout")
        }
        .padding(.vertical, 6)
    }
}

/// A single history entry card.
private struct WorkoutRow: View {
    let workout: Workout

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(workout.name)
                    .font(.headline)
                Spacer()
                Text(AppFormatters.relativeDate(workout.date))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Theme.secondaryText)
            }

            HStack(spacing: 14) {
                Label("\(workout.exerciseCount) exercises", systemImage: "figure.strengthtraining.traditional")
                Label("\(workout.setCount) sets", systemImage: "number")
                Label(AppFormatters.volume(workout.totalVolume), systemImage: "scalemass")
            }
            .font(.caption)
            .foregroundStyle(Theme.secondaryText)
        }
        .padding(Theme.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardStyle()
    }
}

#Preview {
    HistoryView()
        .modelContainer(PreviewData.container)
}