# GymTracker for Android

Native, single-user workout and run tracker built with Kotlin, Jetpack Compose, Room and DataStore. Records are saved locally; maps use the internet.
Android port of [Gym-App](https://github.com/KHALIL-P-JACKSON/Gym-App), based on its master commit
`6f1a03cfb22133d1ce343ebc2a3c95af30358d5e`.

## Learn the codebase

Open the [guided demonstration](docs/CODEBASE_DEMO.md) to trace a workout through Compose, the ViewModel, domain rules and Room.
For an interactive walkthrough, download [codebase-demo.html](docs/codebase-demo.html) and open it in a browser. It runs offline.
The guide also includes a runnable Kotlin example using the actual domain services.

## Run
1. Open this repository in Android Studio Meerkat or newer.
2. Use JDK 17; install Android SDK 35.
3. Sync Gradle, then run the app on Android 8.0 / API 26 or newer.

The standard Gradle 8.13 wrapper is committed:
```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
python3 scripts/check_architecture.py
```
The second command needs a running emulator/device. Initial build downloads dependencies. Workout features and run recording work offline; loading new map areas needs internet.

## Screens
- **Home:** greeting, start workout, recent workout, streak, total volume and featured strength comparison.
- **Workout:** search, grouped preset/custom exercises, previous sets, weight/reps entry, add/remove rows, Complete/Edit locks, notes, date and inline Finish Workout. Incomplete sets are dropped only after confirmation when enabled.
- **Plan:** six preset splits, custom plans, edit names/exercises, delete custom plans and load into the workout.
- **History:** All/Workouts/Runs filters, newest first, workout editing and run details with saved routes and splits. Confirmed deletion removes associated records.
- **Run:** precise GPS position, pink route, Start Run/End Run, stopwatch, miles/km, live/average pace, splits, optional distance goal and spoken updates. A location foreground service continues with the screen locked. Force-stop/reboot can interrupt recording; unfinished sessions can be recovered with gaps identified.
- **Strength Progress (from Home):** exercise selection, workout-best comparisons and estimated Epley 1RM chart with accessible dated values.
- **Settings:** lb/kg, system/light/dark, default rest preference, keep awake, discard confirmation, CSV share and confirmed delete-all/reseed.

The exact 37 exercises and six splits are seeded using stable IDs. Existing preset plan edits survive relaunch/seeding.
Weights are persisted in pounds, regardless of display unit. Like iOS, a valid set needs positive finite weight and positive reps; zero-weight bodyweight entry is not supported.
Strength drafts live in the ViewModel across configuration changes. Active runs checkpoint to disk; completed runs include their route and splits.
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
      screens/                Home, Workout, Plan, History, Run, strength Progress, Settings
      theme/                  Pink design system and light/dark colors
      previews/               Pure fixtures; no database/ViewModel required
  tracking/                   Android location source, run controller and foreground service
```

Data implements domain interfaces; presentation depends on domain. The Application/Activity compose concrete dependencies.
One Gradle app module and manual constructor injection keep debugging simple. All composables are in `presentation/composables`, including light/dark and screen/component previews.

Room transactions replace edited set lists atomically. Deleting a workout cascades to its sets.
Exercise foreign keys prevent removal of referenced history. Delete-all removes workouts before custom exercises and restores all default plans.
The database is version 2, with an explicit version 1→2 migration that preserves strength data and adds run/route/split tables. Both schemas are in `app/schemas`. Retain old schemas and add migrations for future changes.

Run maps use MapLibre Native with OpenFreeMap, with no map API key or billing setup. Attribution remains visible. Map failures do not stop recording. Settings exports separate workout and run-summary CSVs; run CSV uses meters and seconds, with pace per the recorded unit. Route coordinates stay in the app. See [Run plan](docs/RUN_TAB_PLAN.md) for design and validation cases.

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
- Open View Progress on Home after two workouts; estimated 1RM is clearly labeled.
- Start a run, lock the screen, move outdoors, reopen and End Run; inspect route/time/distance/splits in History.
- Check miles/km, distance goals, spoken updates, GPS loss, denied precise location and map connectivity loss.
- Share CSV; confirm pounds and escaped names.
- Change theme; inspect previews on small/large screens.
- Enable keep-awake on Workout; switch tabs and verify normal screen timeout returns.
- Delete all data; presets return and history/customs are removed.
- Repeat the main workflow in airplane mode.
