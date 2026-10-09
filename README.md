# GymTracker for Android

Native, single-user, offline workout tracker built with Kotlin, Jetpack Compose, Room and DataStore.
Android port of [Gym-App](https://github.com/KHALIL-P-JACKSON/Gym-App), based on its master commit
`6f1a03cfb22133d1ce343ebc2a3c95af30358d5e`.

## Run
1. Open this repository in Android Studio Meerkat or newer.
2. Use JDK 17; install Android SDK 35.
3. Sync Gradle, then run the app on Android 8.0 / API 26 or newer.

The standard Gradle 8.11.1 wrapper is committed:
```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
python3 scripts/check_architecture.py
```
The second command needs a running emulator/device. Initial build downloads dependencies; the installed app needs no network.

## Screens
- **Home:** greeting, start workout, recent workout, streak, total volume and featured strength comparison.
- **Workout:** search, grouped preset/custom exercises, previous sets, weight/reps entry, add/remove rows, Complete/Edit locks, notes, date and inline Finish Workout. Incomplete sets are dropped only after confirmation when enabled.
- **Plan:** six preset splits, custom plans, edit names/exercises, delete custom plans and load into the workout.
- **History:** newest first, details, delete, log a past workout and edit its name/date/notes/exercises/sets. Cancel restores your ongoing workout.
- **Progress:** exercise selection, workout-best comparisons and estimated Epley 1RM chart with accessible dated values.
- **Settings:** lb/kg, system/light/dark, default rest preference, keep awake, discard confirmation, CSV share and confirmed delete-all/reseed.

The exact 37 exercises and six splits are seeded using stable IDs. Existing preset plan edits survive relaunch/seeding.
Weights are persisted in pounds, regardless of display unit. Like iOS, a valid set needs positive finite weight and positive reps; zero-weight bodyweight entry is not supported.
Drafts live in the ViewModel across configuration changes; only finished workouts are saved to disk.
The rest setting is a preference, as in the source; there is no running timer.

## Three layers
```text
app/src/main/java/com/gymtracker/android/
  data/                       Room entities, DAO, repository adapters, DataStore, preset seeds
  domain/
    model/                    Plain Kotlin exercise/workout/plan/draft/settings models
    repository/               Storage interfaces
    service/                  Saving/validation, strength/streak math, CSV export
  presentation/
    TrackerViewModel.kt       Observable state and user actions
    Formatters.kt             Shared formatting
    composables/
      components/             Reusable cards, entry rows, picker, date selector and chart
      screens/                Home, Workout, Plan, History, Progress, Settings
      theme/                  Green design system and light/dark colors
      previews/               Pure fixtures; no database/ViewModel required
```

Data implements domain interfaces; presentation depends on domain. The Application/Activity compose concrete dependencies.
One Gradle app module and manual constructor injection keep debugging simple. All composables are in `presentation/composables`, including light/dark and screen/component previews.

Room transactions replace edited set lists atomically. Deleting a workout cascades to its sets.
Exercise foreign keys prevent removal of referenced history. Delete-all removes workouts before custom exercises and restores all default plans.
The database starts at version 1. The generated version-1 schema is committed in `app/schemas`; CI also uploads schemas with reports. Add an explicit migration and commit the new generated schema before changing stored fields.

## Branches and automation
- `master`: Android baseline.
- `develop`: ongoing development; Dependabot PRs target this branch.
- `main`: kept usable because GitHub created it as the default branch. This session cannot change the repository's default-branch setting.
- CI builds, tests, runs lint and checks layer boundaries on pushes/PRs.
- Device CI checks disk persistence/edit/delete/reseed and logs a workout through Compose.
- Weekly Dependabot covers Gradle and GitHub Actions.
- Release workflow packages unsigned APKs on `v*` tags or manual dispatch. Tags create a GitHub prerelease with the unsigned APK. Sign with your own key before installation/distribution; Play Store publication is not configured.

There were no Dependabot, GitHub Actions, package lockfiles or third-party package dependencies in either iOS source branch.
Android uses native equivalents: SwiftUI → Compose, SwiftData → Room, UserDefaults → DataStore, Swift Charts → a small Compose Canvas chart.
All dependency versions are in `gradle/libs.versions.toml`.

## Source preservation and parity
The complete original master source, Xcode project, README, gitignore and assets are copied unchanged to `reference/ios`.
They are reference material and are not part of the Android build. See [port notes](docs/PORTING.md) for mappings and validation.
Android uses Material controls rather than pixel-identical SwiftUI controls; the data/workflow stays as close to iOS as practical.

## Manual checks
- First launch: 37 exercises and six plans; relaunch doesn't duplicate them.
- Log Bench Press 185 × 8, add another set, Complete/Edit, Finish, then check History after relaunch.
- Load a plan and create/edit/delete a custom plan.
- Change units with an open draft; stored weights/history remain equivalent.
- Backfill a workout date, edit its name/notes/sets, then delete it.
- Check Progress after two workouts; estimated 1RM is clearly labeled.
- Share CSV; confirm pounds and escaped names.
- Change theme; inspect previews on small/large screens.
- Enable keep-awake on Workout; switch tabs and verify normal screen timeout returns.
- Delete all data; presets return and history/customs are removed.
- Repeat the main workflow in airplane mode.
