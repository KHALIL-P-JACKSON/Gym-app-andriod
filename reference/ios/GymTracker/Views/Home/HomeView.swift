import SwiftUI
import SwiftData

/// The dashboard: greeting, start-workout shortcut, recent workout,
/// streak/volume stats and a strength hint.
struct HomeView: View {
    @Binding var selectedTab: Int

    @Query(sort: \Workout.date, order: .reverse) private var workouts: [Workout]
    @Query private var allSets: [WorkoutSet]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    greeting
                    startWorkoutCard
                    if let recent = workouts.first {
                        recentWorkoutCard(recent)
                    } else {
                        EmptyStateView(
                            iconName: "dumbbell",
                            title: "No workouts yet",
                            message: "Start your first workout and begin tracking your progress.",
                            actionTitle: "Start Workout",
                            action: { selectedTab = 1 }
                        )
                    }
                    statsRow
                    strengthCard
                }
                .padding(Theme.padding)
            }
            .background(Theme.background)
            .navigationTitle("Home")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    // MARK: - Sections

    private var greeting: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(AppFormatters.greeting())
                .font(.largeTitle.weight(.bold))
            if let recent = workouts.first {
                Text("Last workout \(AppFormatters.relativeDate(recent.date).lowercased()) — keep it going.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryText)
            } else {
                Text("Let's get started.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryText)
            }
        }
    }

    private var startWorkoutCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Ready for your workout?")
                .font(.title3.weight(.semibold))
            Text("Tap start to record your next session.")
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryText)
            Button {
                selectedTab = 1
            } label: {
                Label("Start Workout", systemImage: "dumbbell.fill")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.primary)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [Theme.primary.opacity(0.25), Theme.cardBackground],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: Theme.cardCornerRadius, style: .continuous))
    }

    private func recentWorkoutCard(_ workout: Workout) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Recent Workout")
                    .sectionHeaderStyle()
                Spacer()
                Text(AppFormatters.relativeDate(workout.date))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Theme.secondaryText)
            }

            Text(workout.name)
                .font(.title3.weight(.semibold))

            if let firstGroup = workout.exerciseGroups.first {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(firstGroup.sets.prefix(3)) { set in
                        if let exerciseName = set.exercise?.name {
                            HStack {
                                Text(exerciseName)
                                    .font(.subheadline)
                                Spacer()
                                Text("\(AppFormatters.weightWithUnit(set.weight)) × \(set.reps)")
                                    .font(.subheadline.weight(.semibold))
                                    .monospacedDigit()
                            }
                        }
                    }
                }
            }

            Text("\(workout.exerciseCount) exercises • \(workout.setCount) sets")
                .font(.caption)
                .foregroundStyle(Theme.secondaryText)
        }
        .padding(Theme.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardStyle()
    }

    private var statsRow: some View {
        HStack(spacing: 10) {
            StatCard(
                title: "Day Streak",
                value: "\(streak)",
                iconName: "flame.fill",
                accentColor: Theme.warning
            )
            StatCard(
                title: "Workouts",
                value: "\(workouts.count)",
                iconName: "dumbbell.fill"
            )
            StatCard(
                title: "Volume",
                value: AppFormatters.volume(totalVolume),
                iconName: "scalemass"
            )
        }
    }

    private var strengthCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Strength Progress")
                .sectionHeaderStyle()

            if let featured = featuredStrength {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(featured.exerciseName)
                            .font(.headline)
                        HStack(spacing: 6) {
                            Image(systemName: changeIcon(featured.comparison))
                                .foregroundStyle(changeColor(featured.comparison))
                            Text(changeLabel(featured.comparison))
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(changeColor(featured.comparison))
                        }
                    }
                    Spacer()
                    Image(systemName: "chart.line.uptrend.xyaxis")
                        .font(.title2)
                        .foregroundStyle(Theme.primary)
                }
            } else {
                Text("Log a few workouts and this card will show how your top lifts are improving.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryText)
            }
        }
        .padding(Theme.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardStyle()
    }

    // MARK: - Calculations

    private var streak: Int {
        ProgressCalculator.workoutStreak(workoutDates: workouts.map(\.date))
    }

    private var totalVolume: Double {
        allSets.reduce(0) { $0 + $1.weight * Double($1.reps) }
    }

    private struct FeaturedStrength {
        let exerciseName: String
        let comparison: ProgressCalculator.BestComparison
    }

    /// Picks the exercise with the most recorded sets and compares its
    /// best against its previous best.
    private var featuredStrength: FeaturedStrength? {
        let groups = Dictionary(grouping: allSets) { $0.exercise?.id }
        guard let mostLogged = groups.max(by: { $0.value.count < $1.value.count }),
              let exercise = mostLogged.value.first?.exercise else { return nil }

        let history = ProgressCalculator.performanceHistory(in: mostLogged.value)
        let comparison = ProgressCalculator.bestComparison(in: history)
        guard comparison.currentBest != nil else { return nil }

        return FeaturedStrength(exerciseName: exercise.name, comparison: comparison)
    }

    private func changeLabel(_ comparison: ProgressCalculator.BestComparison) -> String {
        guard comparison.currentBest != nil else { return "No data yet" }
        guard comparison.previousBest != nil else { return "New record" }
        let delta = comparison.change
        if delta > 0 {
            return "+\(AppFormatters.weightWithUnit(delta))"
        } else if delta < 0 {
            return "\(AppFormatters.weightWithUnit(delta))"
        }
        return "No change yet"
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