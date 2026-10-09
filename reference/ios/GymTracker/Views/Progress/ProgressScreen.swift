import SwiftUI
import SwiftData
import Charts

/// Strength progression: pick an exercise and see your best lifts,
/// estimated 1RM, and a chart over time.
struct ProgressScreen: View {
    @Query(sort: \Exercise.sortOrder) private var exercises: [Exercise]
    @Query private var allSets: [WorkoutSet]

    @State private var selectedExercise: Exercise?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    exercisePicker

                    if let exercise = selectedExercise {
                        content(for: exercise)
                    } else {
                        EmptyStateView(
                            iconName: "chart.line.uptrend.xyaxis",
                            title: "Choose an exercise",
                            message: "Pick an exercise to see how your strength is trending."
                        )
                    }
                }
                .padding(Theme.padding)
            }
            .background(Theme.background)
            .navigationTitle("Progress")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if selectedExercise == nil {
                    selectedExercise = exercises.first
                }
            }
        }
    }

    // MARK: - Exercise picker

    private var exercisePicker: some View {
        Menu {
            ForEach(MuscleGroup.allCases) { group in
                let groupExercises = exercises.filter { $0.muscleGroup == group.rawValue }
                if !groupExercises.isEmpty {
                    Section {
                        ForEach(groupExercises) { exercise in
                            Button {
                                selectedExercise = exercise
                            } label: {
                                Text(exercise.name)
                            }
                        }
                    } header: {
                        Text(group.displayName)
                    }
                }
            }
        } label: {
            HStack {
                Text(selectedExercise?.name ?? "Select Exercise")
                    .font(.headline)
                    .foregroundStyle(.primary)
                Spacer()
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.secondaryText)
            }
            .padding(Theme.padding)
            .cardStyle()
        }
    }

    // MARK: - Content

    private func content(for exercise: Exercise) -> some View {
        let sets = allSets.filter { $0.exercise?.id == exercise.id }
        let history = ProgressCalculator.performanceHistory(in: sets)
        let comparison = ProgressCalculator.bestComparison(in: history)

        return VStack(alignment: .leading, spacing: 20) {
            if history.isEmpty {
                EmptyStateView(
                    iconName: "dumbbell",
                    title: "No data for \(exercise.name)",
                    message: "Log a workout with this exercise and your progress will show up here."
                )
            } else {
                statCards(exercise: exercise, comparison: comparison)
                chart(history: history)
            }
        }
    }

    private func statCards(
        exercise: Exercise,
        comparison: ProgressCalculator.BestComparison
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(exercise.name)
                .font(.title2.weight(.bold))

            HStack(spacing: 10) {
                StatCard(
                    title: "Current Best",
                    value: setSummary(comparison.currentBest),
                    iconName: "trophy.fill",
                    accentColor: Theme.warning
                )
                StatCard(
                    title: "Change",
                    value: changeSummary(comparison),
                    iconName: changeIcon(comparison),
                    accentColor: changeColor(comparison)
                )
                StatCard(
                    title: "Est. 1RM",
                    value: oneRepMaxSummary(comparison.currentBest),
                    iconName: "bolt.fill"
                )
            }
        }
    }

    private func chart(history: [ProgressCalculator.ExercisePerformance]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Estimated 1RM Over Time")
                .font(.headline)

            Chart(history) { performance in
                LineMark(
                    x: .value("Date", performance.date),
                    y: .value("Est. 1RM (\(AppSettings.weightUnit.abbreviation))", performance.estimatedOneRepMax)
                )
                .interpolationMethod(.catmullRom)
                .foregroundStyle(Theme.primary)

                PointMark(
                    x: .value("Date", performance.date),
                    y: .value("Est. 1RM (\(AppSettings.weightUnit.abbreviation))", performance.estimatedOneRepMax)
                )
                .symbolSize(40)
                .foregroundStyle(Theme.primary)
            }
            .chartXAxis {
                AxisMarks(values: .automatic(desiredCount: 4)) { _ in
                    AxisValueLabel(format: .dateTime.month(.abbreviated).day())
                }
            }
            .chartYAxis {
                AxisMarks(position: .leading)
            }
            .frame(height: 240)
            .padding(12)
            .cardStyle()

            Text("Estimated 1RM uses the Epley formula (weight × (1 + reps ÷ 30)). It is an estimate, not a tested maximum.")
                .font(.caption)
                .foregroundStyle(Theme.secondaryText)
        }
    }

    // MARK: - Formatting helpers

    private func setSummary(_ set: WorkoutSet?) -> String {
        guard let set else { return "—" }
        return "\(AppFormatters.weightWithUnit(set.weight)) × \(set.reps)"
    }

    private func oneRepMaxSummary(_ set: WorkoutSet?) -> String {
        guard let set else { return "—" }
        return AppFormatters.weightWithUnit(set.estimatedOneRepMax)
    }

    private func changeSummary(_ comparison: ProgressCalculator.BestComparison) -> String {
        guard comparison.currentBest != nil else { return "—" }
        guard comparison.previousBest != nil else { return "New" }
        let delta = comparison.change
        if delta > 0 { return "+\(AppFormatters.weightWithUnit(delta))" }
        if delta < 0 { return "\(AppFormatters.weightWithUnit(delta))" }
        return "0 \(AppSettings.weightUnit.abbreviation)"
    }

    private func changeIcon(_ comparison: ProgressCalculator.BestComparison) -> String {
        guard comparison.previousBest != nil else { return "star.fill" }
        let delta = comparison.change
        if delta > 0 { return "arrow.up.right.circle.fill" }
        if delta < 0 { return "arrow.down.right.circle.fill" }
        return "equal.circle.fill"
    }

    private func changeColor(_ comparison: ProgressCalculator.BestComparison) -> Color {
        guard comparison.previousBest != nil else { return Theme.primary }
        let delta = comparison.change
        if delta > 0 { return Theme.success }
        if delta < 0 { return Theme.warning }
        return Theme.secondaryText
    }
}

#Preview {
    ProgressScreen()
        .modelContainer(PreviewData.container)
}