import SwiftUI
import SwiftData

/// The Plan tab: preset workout splits that can be applied to the
/// Workout tab all at once, plus user-created custom plans.
struct PlanView: View {
    /// Shared session owned by MainTabView so applied plans land in the
    /// Workout tab. Switching tabs is driven by this binding.
    @Bindable var session: WorkoutSession
    @Binding var selectedTab: Int

    @Environment(\.modelContext) private var context
    @Query(sort: \Exercise.sortOrder) private var libraryExercises: [Exercise]
    @Query(sort: \WorkoutPlan.sortOrder) private var plans: [WorkoutPlan]

    @State private var showingCreateSheet = false
    @State private var editingPlan: WorkoutPlan?
    @State private var appliedMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    if !presetPlans.isEmpty {
                        planSection(title: "Preset Splits", plans: presetPlans)
                    }
                    if !customPlans.isEmpty {
                        planSection(title: "My Plans", plans: customPlans, canDelete: true)
                    }
                    if plans.isEmpty {
                        EmptyStateView(
                            iconName: "clipboard",
                            title: "No plans yet",
                            message: "Create your first plan to load a full workout in one tap."
                        )
                    }
                    createButton
                    if let appliedMessage {
                        Text(appliedMessage)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Theme.success)
                    }
                }
                .padding(Theme.padding)
            }
            .background(Theme.background)
            .navigationTitle("Plan")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showingCreateSheet) {
                PlanBuilderSheet(plan: nil, exercises: libraryExercises) { name, selected in
                    PlanStore.create(name: name, exerciseNames: selected, in: context)
                    showingCreateSheet = false
                }
                .presentationDetents([.large])
            }
            .sheet(item: $editingPlan) { plan in
                PlanBuilderSheet(plan: plan, exercises: libraryExercises) { name, selected in
                    if PlanStore.update(plan, name: name, exerciseNames: selected, in: context) {
                        editingPlan = nil
                    }
                }
                .presentationDetents([.large])
            }
        }
    }

    private var presetPlans: [WorkoutPlan] { plans.filter(\.isPreset) }
    private var customPlans: [WorkoutPlan] { plans.filter { !$0.isPreset } }

    private func planSection(title: String, plans: [WorkoutPlan], canDelete: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.title3.weight(.semibold))
            ForEach(plans) { plan in
                planCard(plan, canDelete: canDelete)
            }
        }
    }

    private func planCard(_ plan: WorkoutPlan, canDelete: Bool) -> some View {
        let resolved = PlanStore.resolveExercises(for: plan, from: libraryExercises)
        return VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(plan.name)
                        .font(.headline)
                    Text("\(resolved.count) exercise\(resolved.count == 1 ? "" : "s")")
                        .font(.caption)
                        .foregroundStyle(Theme.secondaryText)
                }
                Spacer()
                // Every plan (preset or custom) is editable; only custom
                // plans can be deleted.
                Button {
                    editingPlan = plan
                } label: {
                    Image(systemName: "pencil")
                        .foregroundStyle(Theme.primary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Edit \(plan.name)")
                if canDelete {
                    Button(role: .destructive) {
                        PlanStore.delete(plan, in: context)
                    } label: {
                        Image(systemName: "trash")
                            .foregroundStyle(Theme.destructive)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Delete \(plan.name)")
                }
            }
            Text(resolved.map(\.name).joined(separator: " • "))
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryText)
                .lineLimit(3)
            Button {
                applyPlan(plan, resolved: resolved)
            } label: {
                Label("Add to Workout", systemImage: "plus.circle.fill")
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.primary)
            .disabled(resolved.isEmpty)
        }
        .padding(Theme.padding)
        .cardStyle()
    }

    private var createButton: some View {
        Button {
            showingCreateSheet = true
        } label: {
            Label("Create Custom Plan", systemImage: "plus")
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
        }
        .buttonStyle(.bordered)
        .tint(Theme.primary)
        .disabled(libraryExercises.isEmpty)
    }

    /// Adds all of the plan's exercises to the shared session and jumps
    /// to the Workout tab.
    private func applyPlan(_ plan: WorkoutPlan, resolved: [Exercise]) {
        let added = session.addExercises(resolved)
        session.name = plan.name
        let service = WorkoutService()
        for draft in session.exercises where resolved.contains(where: { $0.id == draft.exercise.id }) {
            if draft.previousSets.isEmpty {
                draft.previousSets = service.previousSets(for: draft.exercise, in: context)
            }
        }
        appliedMessage = added > 0
            ? "Added \(added) exercise\(added == 1 ? "" : "s") to your workout."
            : "That plan is already in your workout."
        selectedTab = 1
    }
}
