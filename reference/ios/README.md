# GymTracker

A modern, native iPhone workout tracker built with **SwiftUI**, **SwiftData** and **Swift Charts**. Single-user, fully offline, no backend.

## Requirements

- **Xcode 16 or later** (the project uses Xcode 16's folder-synchronized format, so new files added inside `GymTracker/` are picked up automatically)
- **iOS 17 or later**
- A Mac with Xcode installed

## Run it

1. Open `GymTracker.xcodeproj` in Xcode.
2. Select the **GymTracker** scheme and an iPhone simulator (or your device).
3. Press **Run** (⌘R). The preset exercise library is seeded automatically on first launch.
4. You can use your own code signing team later — for the simulator, none is needed.

> Everything is stored locally in the app's SwiftData store. Wi-Fi and cellular are never required.

## Primary workflow

```
Open app
  → Tap Workout tab
  → Tap "Bench Press" (and any other exercises you want)
  → Enter weight and reps
  → Tap "+ Add Set" for more sets (partial sets are discarded on save)
  → Tap "Finish Workout"
  → See the summary, tap Done
  → Close and reopen the app → History tab → the workout is still there
```

Each exercise card also shows your **last recorded sets** for that exercise (`Last Workout`), so you know what you did before.

## Screens

| Tab | Purpose |
| --- | --- |
| **Home** | Time-of-day greeting, Start Workout shortcut, recent workout, day streak / total workouts / total volume, and a strength card comparing your top lift's current best vs previous best. |
| **Workout** | The logging screen. Preset exercises organized by muscle group (Chest / Back / Shoulders / Arms / Legs / Calisthenics), plus custom exercises. Fast numeric set entry, add/remove sets, per-exercise Complete (lock) / Edit, a search bar to filter exercises, and an inline Finish Workout button anchored below the most recently added exercise. |
| **Plan** | Preset workout splits (Push / Pull / Legs, Upper, Full Body, Calisthenics) that load all exercises into the Workout tab at once. Create custom plans (name + multi-select), edit any plan's name/exercises (pencil), delete custom plans (trash). |
| **History** | Completed workouts, newest first. A **+** button lets you log a workout for any day (including past days you did before the app was installed). Tap a workout for full details; pencil edits name/date/notes/sets (add/remove sets + exercises), trash deletes it (and its sets). |
| **Progress** | Pick any exercise to see current best, change vs previous workout-best, estimated 1RM, and a Swift Charts line chart of estimated 1RM over time. |
| **Settings** | Weight unit (lb/kg, stored values stay in lb), theme (system/light/dark), default rest timer, keep-awake during workout, discard-confirmation toggle, CSV export, delete-all-data, version/about. |

## Architecture

```
GymTracker/
├── App/            GymTrackerApp (SwiftData container + seeding), Theme (design system),
│                   AppSettings (units/appearance/rest/keep-awake/confirm prefs)
├── Models/         Exercise, Workout, WorkoutSet, WorkoutPlan (SwiftData @Model)
├── Data/           DefaultExercises (preset library + seeder), DefaultPlans (preset splits),
│                   PreviewData (previews only)
├── Services/       WorkoutService (saving / previous performance),
│                   ProgressCalculator (best lifts, estimated 1RM, streaks, history),
│                   WorkoutSession (in-memory draft of the workout being logged),
│                   PlanStore (preset/custom plan persistence),
│                   DataManager (CSV export + delete-all)
├── Views/          MainTabView, Home/, Workout/, Plan/, History/, Progress/, Settings/
├── Components/     StatCard, SetRow, EmptyStateView, ExerciseGroupList
└── Utilities/      AppFormatters (shared date/number formatting)
```

### Data model

```
Workout 1 ── * WorkoutSet * ── 1 Exercise
```

- A **Workout** has a date, name, optional duration/notes, and many sets.
- A **WorkoutSet** holds weight, reps, set number, and a global sort order so display order is preserved.
- An **Exercise** is shared reference data (e.g. "Bench Press"); every recorded set points at it. That makes "all Bench Press sets ordered by date" a simple query — the basis for the Progress screen.

Deletion rules: deleting a workout cascades to its sets; deleting an exercise never deletes recorded history.

### Estimated 1RM

Uses the **Epley formula**: `weight × (1 + reps / 30)`. It is always labeled as an estimate in the UI, never a tested maximum. All strength math lives in `ProgressCalculator` so it is trivially unit-testable and easy to extend.

## Manual test checklist

- [ ] First launch shows the full preset exercise library (37 exercises in 6 muscle groups; existing installs get new presets added automatically on the next launch).
- [ ] Plan tab shows 6 preset splits; tapping "Add to Workout" loads all exercises into the Workout tab and jumps there.
- [ ] Plan tab → Create Custom Plan → name + pick exercises → Save → appears under My Plans; trash deletes it.
- [ ] Plan tab → pencil on any plan (preset or custom) → rename + change exercises → Save → card updates; relaunch keeps edits.
- [ ] Settings → switch to kg → Workout entry, Last Workout, History, Progress and volume all show kg; switch back shows lb with same stored values.
- [ ] Settings → Theme light/dark applies immediately; Export CSV shares a file; Delete All Data wipes workouts/plans/customs and re-seeds presets.
- [ ] History → tap workout → pencil → rename, change date/notes, edit weight/reps, add/remove sets, add exercise → Save → detail + list update.
- [ ] Workout → Custom button → name + muscle group → exercise appears in the picker (under its group) and is added to the session.
- [ ] Log a workout: Bench Press → 185 → 8 → Complete locks the card → Edit unlocks → Add Set unlocks and appends a row.
- [ ] Finish Workout appears inline below the most recently added exercise (disabled until at least one valid set); bottom bar is a summary only.
- [ ] Log a workout: Bench Press → 185 → 8 → Add Set → 185 → 8 → Finish Workout → summary appears.
- [ ] Kill and relaunch the app → History shows the workout; tap it to see all sets.
- [ ] History → **+** → pick a past date → log a few sets → Finish → the workout appears in History under that date.
- [ ] Delete a workout from its detail screen → it disappears from History.
- [ ] Start a new workout, tap Bench Press again → the card shows *Last Workout* 185 lb × 8 rows.
- [ ] Finish with an empty/incomplete set → you're asked to discard it; it is never saved.
- [ ] Progress tab → pick Bench Press → current best, change, est. 1RM and chart appear after 2+ workouts.
- [ ] Toggle between Light and Dark mode → the app looks correct in both.
- [ ] Turn on Airplane Mode → everything still works.

## Roadmap hooks (not implemented yet)

The architecture leaves room for: iCloud sync (SwiftData's CloudKit support), HealthKit integration (HKWorkout data is derivable from `Workout`), custom exercise templates (Push/Pull/Legs from the stored `category`), rest timers, personal records, bodyweight/measurement tracking, CSV export, and richer Progress calculations — all without restructuring the models or services.