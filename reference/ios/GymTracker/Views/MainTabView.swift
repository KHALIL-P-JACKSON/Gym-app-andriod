import SwiftUI

/// Root tab bar: Home, Workout, Plan, History, Progress, Settings.
/// Owns the shared workout session so the Plan tab can load a full
/// split into the Workout tab all at once.
struct MainTabView: View {
    @State private var selectedTab = 0
    @State private var session = WorkoutSession()
    // Observes the appearance setting so the theme updates live.
    @AppStorage("settings.appearance") private var appearanceRaw = AppearanceSetting.system.rawValue

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(selectedTab: $selectedTab)
                .tabItem { Label("Home", systemImage: "house.fill") }
                .tag(0)

            WorkoutView(session: session)
                .tabItem { Label("Workout", systemImage: "dumbbell.fill") }
                .tag(1)

            PlanView(session: session, selectedTab: $selectedTab)
                .tabItem { Label("Plan", systemImage: "clipboard.fill") }
                .tag(2)

            HistoryView()
                .tabItem { Label("History", systemImage: "clock.arrow.circlepath") }
                .tag(3)

            ProgressScreen()
                .tabItem { Label("Progress", systemImage: "chart.line.uptrend.xyaxis") }
                .tag(4)

            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape.fill") }
                .tag(5)
        }
        .preferredColorScheme(AppearanceSetting(rawValue: appearanceRaw)?.colorScheme)
    }
}

#Preview {
    MainTabView()
        .modelContainer(PreviewData.container)
}