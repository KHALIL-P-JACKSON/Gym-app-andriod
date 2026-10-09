import SwiftUI
import SwiftData

/// The workout logging experience: pick exercises, enter sets, finish
/// and save. Used as the Workout tab and, from History, to log a
/// workout that happened on a past day.
struct WorkoutEditor: View {
    /// When true, the user can pick the workout's date before saving
    /// (used when adding past workouts from History).
    var canPickDate: Bool = false
    /// Called after the user taps Done on the completion screen.
    var onFinished: () -> Void = {}
    /// When nil the editor owns a local session (used for one-off
    /// sheets like logging a past workout from History).
    var session: WorkoutSession? = nil

    @State private var localSession = WorkoutSession()

    var body: some View {
        WorkoutEditorContent(
            session: session ?? localSession,
            canPickDate: canPickDate,
            onFinished: onFinished
        )
    }
}

/// Inner view that binds to a shared or local session. Split out so
/// WorkoutEditor can own a fallback session while still using @Bindable.
private struct WorkoutEditorContent: View {
    @Bindable var session: WorkoutSession
    var canPickDate: Bool = false
    var onFinished: () -> Void = {}

    @Environment(\.modelContext) private var context
    @Query(sort: \Exercise.sortOrder) private var libraryExercises: [Exercise]
    @State private var completedWorkout: Workout?
    @State private var alert: ActiveAlert?
    @State private var searchQuery = ""
    enum ActiveAlert: Identifiable {
        case incompleteSets(count: Int)
        case saveFailed

        var id: String {
            switch self {
            case .incompleteSets: return "incompleteSets"
            case .saveFailed: return "saveFailed"
            }
        }
    }

    /// Single source of truth for which field has the keyboard open.
    /// Lives here so the keyboard toolbar ("Done") only exists once.
    @FocusState private var focusedField: SetField?
    // Keep-awake is a live AppStorage read so toggling it in Settings
    // applies to the open workout screen immediately.
    @AppStorage("settings.keepAwakeDuringWorkout") private var keepAwake = false
    @AppStorage("settings.confirmBeforeDiscarding") private var confirmDiscard = true

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header

                let trimmedQuery = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
                let filteredExercises: [DraftExercise] = trimmedQuery.isEmpty
                    ? session.exercises
                    : session.exercises.filter {
                        $0.exercise.name.localizedCaseInsensitiveContains(trimmedQuery)
                    }
                let filteredLibrary: [Exercise] = trimmedQuery.isEmpty
                    ? libraryExercises
                    : libraryExercises.filter {
                        $0.name.localizedCaseInsensitiveContains(trimmedQuery)
                    }

                if session.exercises.isEmpty {
                    EmptyStateView(
                        iconName: "hand.tap",
                        title: "Pick your first exercise",
                        message: "Choose an exercise below to start building your workout."
                    )
                } else if filteredExercises.isEmpty {
                    EmptyStateView(
                        iconName: "magnifyingglass",
                        title: "No exercises found",
                        message: "Try adjusting your search to find matching exercises."
                    )
                } else {
                    ForEach(Array(filteredExercises.enumerated()), id: \.element.exercise.id) { _, draft in
                        ExerciseCardView(
                            draft: draft,
                            onAddSet: { addSet(to: draft) },
                            onComplete: { completeExercise(draft) },
                            onEdit: { editExercise(draft) },
                            onRemoveExercise: { removeDraft(draft) },
                            onRemoveSet: { set in removeSet(set, from: draft) },
                            weightField: $focusedField,
                            repsField: $focusedField,
                            isLastExercise: draft.exercise.id == filteredExercises.last?.exercise.id,
                            canFinishWorkout: session.validSetCount > 0,
                            onFinishWorkout: finishWorkout
                        )
                    }
                }

                ExercisePickerView(
                    exercises: filteredLibrary,
                    isAdded: { session.contains($0) },
                    onSelect: addExercise,
                    onCreateCustom: createCustomExercise
                )
            }
            .padding(.top, 4)
            .padding(.horizontal, Theme.padding)
            .padding(.bottom, 8)
        }
        .background(Theme.background)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") { focusedField = nil }
            }
        }
        .safeAreaInset(edge: .bottom) { summaryBar }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = keepAwake }
        .onChange(of: keepAwake) { _, newValue in
            UIApplication.shared.isIdleTimerDisabled = newValue
        }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .fullScreenCover(item: $completedWorkout) { workout in
            WorkoutCompleteView(workout: workout) {
                completedWorkout = nil
                onFinished()
            }
        }
        .alert(item: $alert) { alert in
            switch alert {
            case .incompleteSets(let count):
                return Alert(
                    title: Text("Remove incomplete sets?"),
                    message: Text("\(count) set\(count == 1 ? "" : "s") \(count == 1 ? "has" : "have") no weight or reps and won't be saved."),
                    primaryButton: .destructive(Text("Discard & Save")) { saveWorkout() },
                    secondaryButton: .cancel(Text("Keep Editing"))
                )
            case .saveFailed:
                return Alert(
                    title: Text("Couldn't save workout"),
                    message: Text("Something went wrong while saving. Nothing was lost — please try again."),
                    dismissButton: .default(Text("OK"))
                )
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            SearchBar(
                placeholder: "Search exercises",
                onSubmit: onSearchSubmit,
                onClear: onSearchClear,
                text: $searchQuery
            )
            if canPickDate {
                DatePicker(
                    "Workout Date",
                    selection: $session.date,
                    in: ...Date(),
                    displayedComponents: .date
                )
            } else {
                Text(AppFormatters.longDate(session.date))
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryText)
            }

            TextField("Workout name", text: $session.name)
                .font(.title.bold())
                .textFieldStyle(.plain)
        }
    }

    // MARK: - Summary bar

    /// Compact summary pinned at the bottom. The Finish Workout action
    /// itself lives inline below the most recently added exercise so it
    /// stays anchored instead of jumping as content grows.
    private var summaryBar: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(session.exercises.count) \(session.exercises.count == 1 ? "exercise" : "exercises")")
                    .font(.subheadline.weight(.semibold))
                Text("\(session.validSetCount) valid set\(session.validSetCount == 1 ? "" : "s")")
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryText)
            }
            Spacer()
            Text("Finish in last card ↑")
                .font(.caption)
                .foregroundStyle(Theme.secondaryText)
        }
        .padding(.horizontal, Theme.padding)
        .padding(.vertical, 10)
        .background(.bar, ignoresSafeAreaEdges: [.bottom, .horizontal])
    }

    // MARK: - Plan support

    /// Adds every exercise from a plan that isn't already in the session.
    func applyPlan(exercises: [Exercise]) {
        for exercise in exercises where !session.contains(exercise) {
            session.addExercise(exercise)
            session.exercises.last?.previousSets = WorkoutService().previousSets(for: exercise, in: context)
        }
    }

    // MARK: - Actions

    private func addExercise(_ exercise: Exercise) {
        guard !session.contains(exercise) else { return }
        session.addExercise(exercise)
        // Show the user's last recorded sets for this exercise.
        session.exercises.last?.previousSets = WorkoutService().previousSets(for: exercise, in: context)
    }

    /// Creates a custom exercise, adds it to the library, and puts it
    /// straight into the current session.
    private func createCustomExercise(name: String, muscleGroup: MuscleGroup) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let existing = libraryExercises.map(\.sortOrder).max() ?? -1
        let exercise = Exercise(
            name: trimmed,
            muscleGroup: muscleGroup.rawValue,
            category: ExerciseCategory.push.rawValue,
            isPreset: false,
            sortOrder: existing + 1
        )
        context.insert(exercise)
        try? context.save()
        addExercise(exercise)
    }

    private func addSet(to draft: DraftExercise) {
        focusedField = nil
        withAnimation(.snappy) {
            draft.isComplete = false
            draft.sets.append(DraftSet(entryUnit: AppSettings.weightUnit))
        }
    }

    private func removeSet(_ set: DraftSet, from draft: DraftExercise) {
        withAnimation(.snappy) {
            draft.isComplete = false
            draft.sets.removeAll { $0.id == set.id }
            if draft.sets.isEmpty {
                draft.sets.append(DraftSet(entryUnit: AppSettings.weightUnit))
            }
        }
    }

    /// Removes a draft by identity (not filtered-list index) so deleting
    /// while searching removes the correct exercise.
    private func removeDraft(_ draft: DraftExercise) {
        if let index = session.exercises.firstIndex(where: { $0.exercise.id == draft.exercise.id }) {
            session.removeExercise(at: index)
        }
    }

    /// Locks one exercise's values so the user can move on to the next.
    private func completeExercise(_ draft: DraftExercise) {
        guard draft.hasValidSets else { return }
        focusedField = nil
        withAnimation(.snappy) {
            draft.isComplete = true
        }
    }

    private func editExercise(_ draft: DraftExercise) {
        withAnimation(.snappy) {
            draft.isComplete = false
        }
    }

    private func finishWorkout() {
        guard session.validSetCount > 0 else { return }

        let incompleteCount = session.exercises
            .flatMap { $0.sets }
            .filter { !$0.isValid }
            .count

        // When the user disabled the discard prompt, save straight away
        // (invalid sets are still skipped by WorkoutService).
        if incompleteCount > 0, confirmDiscard {
            alert = .incompleteSets(count: incompleteCount)
        } else {
            saveWorkout()
        }
    }

    /// Saves the current session, then closes it and shows the summary.
    private func saveWorkout() {
        do {
            let saved = try WorkoutService().save(session: session, in: context)
            completedWorkout = saved
            session.reset()
        } catch {
            alert = .saveFailed
        }
    }

    /// Handles submit and clear events for the search bar.
    private func onSearchSubmit() {
        // The query is bound live; filtered exercises update immediately.
    }

    private func onSearchClear() {
        searchQuery = ""
    }
}
