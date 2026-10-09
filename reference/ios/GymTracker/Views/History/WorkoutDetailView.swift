import SwiftUI
import SwiftData

/// Detailed view of a single workout, with edit + delete support.
struct WorkoutDetailView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss

    let workout: Workout

    @State private var showDeleteConfirmation = false
    @State private var showEditSheet = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text(AppFormatters.longDate(workout.date))
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryText)
                    .padding(.top, 8)

                if let notes = workout.notes, !notes.isEmpty {
                    Text(notes)
                        .font(.subheadline)
                        .padding(Theme.padding)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .cardStyle(cornerRadius: Theme.cornerRadius)
                }

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

                ExerciseGroupList(groups: workout.exerciseGroups)
            }
            .padding(Theme.padding)
        }
        .background(Theme.background)
        .navigationTitle(workout.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showEditSheet = true
                } label: {
                    Image(systemName: "pencil")
                }
                .accessibilityLabel("Edit workout")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button(role: .destructive) {
                    showDeleteConfirmation = true
                } label: {
                    Image(systemName: "trash")
                }
            }
        }
        .sheet(isPresented: $showEditSheet) {
            WorkoutEditView(workout: workout)
                .presentationDetents([.large])
        }
        .confirmationDialog(
            "Delete this workout?",
            isPresented: $showDeleteConfirmation,
            titleVisibility: .visible
        ) {
            Button("Delete Workout", role: .destructive) { deleteWorkout() }
        } message: {
            Text("All its sets will be removed. This cannot be undone.")
        }
    }

    private func deleteWorkout() {
        context.delete(workout)
        try? context.save()
        dismiss()
    }
}