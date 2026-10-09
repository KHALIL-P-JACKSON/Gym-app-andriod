import SwiftUI
import SwiftData

/// Minimal v1 Settings: units, appearance, workout prefs, data + about.
struct SettingsView: View {
    @Environment(\.modelContext) private var context
    @Query private var workouts: [Workout]
    @Query private var allSets: [WorkoutSet]

    // Keys are intentionally literal (not AppSettings.Keys, which is
    // private) — they must match the UserDefaults keys exactly.
    @AppStorage("settings.weightUnit") private var weightUnitRaw = WeightUnit.lb.rawValue
    @AppStorage("settings.appearance") private var appearanceRaw = AppearanceSetting.system.rawValue
    @AppStorage("settings.restSeconds") private var restSeconds = RestTimerOption.off.rawValue
    @AppStorage("settings.keepAwakeDuringWorkout") private var keepAwake = false
    @AppStorage("settings.confirmBeforeDiscarding") private var confirmDiscard = true

    @State private var csvURL: URL?
    @State private var showShareSheet = false
    @State private var showDeleteConfirmation = false
    @State private var showDeleteResult = false
    @State private var deleteError: String?


    private var weightUnit: WeightUnit {
        WeightUnit(rawValue: weightUnitRaw) ?? .lb
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Units") {
                    Picker("Weight unit", selection: $weightUnitRaw) {
                        ForEach(WeightUnit.allCases) { unit in
                            Text(unit.displayName).tag(unit.rawValue)
                        }
                    }
                    Text("Stored values stay in pounds — this only changes entry and display.")
                        .font(.caption)
                        .foregroundStyle(Theme.secondaryText)
                }
                Section("Appearance") {
                    Picker("Theme", selection: $appearanceRaw) {
                        ForEach(AppearanceSetting.allCases) { item in
                            Text(item.displayName).tag(item.rawValue)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                Section("Workout") {
                    Picker("Default rest timer", selection: $restSeconds) {
                        ForEach(RestTimerOption.allCases) { option in
                            Text(option.displayName).tag(option.rawValue)
                        }
                    }
                    Toggle("Keep screen awake during workout", isOn: $keepAwake)
                    Toggle("Confirm before discarding incomplete sets", isOn: $confirmDiscard)
                    if weightUnit == .kg {
                        Text("Enter weights in kg — they convert to lb automatically.")
                            .font(.caption)
                            .foregroundStyle(Theme.secondaryText)
                    }
                }
                Section("Data") {
                    HStack {
                        Text("Workouts")
                        Spacer()
                        Text("\(workouts.count)")
                            .foregroundStyle(Theme.secondaryText)
                    }
                    HStack {
                        Text("Sets")
                        Spacer()
                        Text("\(allSets.count)")
                            .foregroundStyle(Theme.secondaryText)
                    }
                    Button {
                        exportCSV()
                    } label: {
                        Label("Export CSV", systemImage: "square.and.arrow.up")
                    }
                    Button(role: .destructive) {
                        showDeleteConfirmation = true
                    } label: {
                        Label("Delete All Data", systemImage: "trash")
                    }
                }
                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text(appVersion)
                            .foregroundStyle(Theme.secondaryText)
                    }
                    Text("100% offline. No account, no tracking — data never leaves this device.")
                        .font(.caption)
                        .foregroundStyle(Theme.secondaryText)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .confirmationDialog(
                "Delete everything?",
                isPresented: $showDeleteConfirmation,
                titleVisibility: .visible
            ) {
                Button("Delete All Data", role: .destructive) { deleteAll() }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("All workouts, plans and custom exercises will be removed. This cannot be undone.")
            }
            .alert(deleteAlertTitle, isPresented: $showDeleteResult) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(deleteAlertMessage)
            }
            .sheet(isPresented: $showShareSheet) {
                if let csvURL {
                    ShareSheet(activityItems: [csvURL])
                }
            }
        }
    }

    private var appVersion: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "1"
        return "\(version) (\(build))"
    }

    private var deleteAlertTitle: String {
        deleteError == nil ? "Data deleted" : "Couldn't delete data"
    }

    private var deleteAlertMessage: String {
        deleteError ?? "All workouts, plans and custom exercises were removed."
    }

    private func exportCSV() {
        let rows = DataManager.exportRows(in: context)
        let text = DataManager.csvText(rows: rows)
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("gymtracker-export.csv")
        do {
            try text.write(to: url, atomically: true, encoding: .utf8)
            csvURL = url
            showShareSheet = true
        } catch {
            deleteError = error.localizedDescription
            showDeleteResult = true
        }
    }

    private func deleteAll() {
        do {
            try DataManager.deleteAllData(in: context)
            deleteError = nil
        } catch {
            deleteError = error.localizedDescription
        }
        showDeleteResult = true
    }
}

/// UIKit share sheet for the CSV export.
private struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

#Preview {
    SettingsView()
        .modelContainer(PreviewData.container)
}