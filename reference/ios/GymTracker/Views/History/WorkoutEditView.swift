import SwiftUI
import SwiftData

/// Edit screen for a saved workout from History. Lets the user change
/// the name, date, notes, every set's weight/reps, add new sets to any
/// exercise, add new exercises, and remove sets/exercises.
struct WorkoutEditView: View {
    let workout: Workout
    var onDone: () -> Void = {}

    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Exercise.sortOrder) private var libraryExercises: [Exercise]

    @State private var name: String = ""
    @State private var date: Date = .now
    @State private var notes: String = ""
    @State private var rows: [EditedSetRow] = []
    @State private var showAddExercise = false
    @State private var saveFailed = false
    @State private var didLoad = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Workout") {
                    TextField("Workout name", text: $name)
                    DatePicker("Date", selection: $date, in: ...Date(), displayedComponents: .date)
                    TextField("Notes (optional)", text: $notes)
                }
                ForEach(exerciseIDs, id: \.self) { exerciseID in
                    Section(header: exerciseHeader(for: exerciseID)) {
                        ForEach(rowIndices(for: exerciseID), id: \.self) { rowIndex in
                            setRow(at: rowIndex)
                        }
                        Button {
                            addSet(to: exerciseID)
                        } label: {
                            Label("Add Set", systemImage: "plus")
                        }
                    }
                }
                if rows.isEmpty {
                    Text("This workout has no editable sets.")
                        .foregroundStyle(Theme.secondaryText)
                }
                Section {
                    Button {
                        showAddExercise = true
                    } label: {
                        Label("Add Exercise", systemImage: "plus.circle")
                    }
                }
            }
            .navigationTitle("Edit Workout")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .disabled(!canSave)
                }
            }
            .sheet(isPresented: $showAddExercise) {
                exercisePickerSheet
                    .presentationDetents([.large])
            }
            .alert("Couldn't save", isPresented: $saveFailed) {
                Button("OK", role: .cancel) {}
            }
            .onAppear {
                guard !didLoad else { return }
                didLoad = true
                name = workout.name
                date = workout.date
                notes = workout.notes ?? ""
                rows = workout.orderedSets.compactMap { EditedSetRow.from(set: $0) }
            }
        }
    }

    // MARK: - Grouping

    /// Exercise IDs in first-appearance order.
    private var exerciseIDs: [UUID] {
        var seen: [UUID] = []
        for row in rows where !seen.contains(row.exercise.id) {
            seen.append(row.exercise.id)
        }
        return seen
    }

    private func exercise(for id: UUID) -> Exercise? {
        rows.first { $0.exercise.id == id }?.exercise
    }

    private func rowIndices(for exerciseID: UUID) -> [Int] {
        rows.indices.filter { rows[$0].exercise.id == exerciseID }
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && rows.contains { $0.isValid }
    }

    // MARK: - Rows

    private func exerciseHeader(for exerciseID: UUID) -> some View {
        HStack {
            Text(exercise(for: exerciseID)?.name ?? "Exercise")
            Spacer()
            Button(role: .destructive) {
                removeExercise(exerciseID)
            } label: {
                Image(systemName: "trash")
                    .font(.caption)
            }
            .buttonStyle(.plain)
        }
    }

    private func setRow(at index: Int) -> some View {
        HStack(spacing: 10) {
            TextField(AppSettings.weightUnit.abbreviation, text: $rows[index].weightText)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.center)
            Text("x")
                .foregroundStyle(Theme.secondaryText)
            TextField("reps", text: $rows[index].repsText)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
            Button(role: .destructive) {
                rows.remove(at: index)
            } label: {
                Image(systemName: "minus.circle.fill")
                    .foregroundStyle(Theme.secondaryText)
            }
            .buttonStyle(.plain)
        }
    }

    private var exercisePickerSheet: some View {
        NavigationStack {
            List {
                ForEach(MuscleGroup.allCases) { group in
                    let groupExercises = libraryExercises.filter { $0.muscleGroup == group.rawValue }
                    if !groupExercises.isEmpty {
                        Section(group.displayName) {
                            ForEach(groupExercises) { exercise in
                                Button {
                                    addExercise(exercise)
                                } label: {
                                    HStack {
                                        Text(exercise.name)
                                            .foregroundStyle(Color.primary)
                                        Spacer()
                                        if rows.contains(where: { $0.exercise.id == exercise.id }) {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundStyle(Theme.primary)
                                        }
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Add Exercise")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { showAddExercise = false }
                }
            }
        }
    }

    // MARK: - Actions

    private func addExercise(_ exercise: Exercise) {
        rows.append(.blank(exercise: exercise))
        showAddExercise = false
    }

    private func addSet(to exerciseID: UUID) {
        guard let exercise = exercise(for: exerciseID) else { return }
        rows.append(.blank(exercise: exercise))
    }

    private func removeExercise(_ exerciseID: UUID) {
        rows.removeAll { $0.exercise.id == exerciseID }
    }

    private func save() {
        do {
            try WorkoutService().update(
                workout: workout,
                name: name,
                date: date,
                notes: notes,
                rows: rows,
                in: context
            )
            dismiss()
            onDone()
        } catch {
            saveFailed = true
        }
    }
}
