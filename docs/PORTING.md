# Porting notes

Source: KHALIL-P-JACKSON/Gym-App master at 6f1a03cfb22133d1ce343ebc2a3c95af30358d5e.
The source develop branch was older. Master includes Settings, CSV export and history editing, so master is the port baseline.

| iOS area | Android equivalent |
| --- | --- |
| App / Theme / AppSettings | Application composition root, Compose theme, DataStore Settings |
| Models | Domain models + explicit Room entities |
| DefaultExercises / DefaultPlans | DefaultLibrary, exact names/categories/order and splits |
| WorkoutService / WorkoutSession | Pure WorkoutService + immutable draft session + ViewModel |
| ProgressCalculator | Pure domain calculator with Epley estimate, per-workout ranking and local-day streak |
| PlanStore | Room plans; seed-ignore preserves edits |
| DataManager | CsvExporter + atomic repository wipe/reseed + FileProvider share |
| Views | Stateless composable screens driven by observable state/actions |
| Components / PreviewData | Separate composables/components and pure preview fixtures |
| Swift Charts | Compose Canvas line chart with date-based x axis and textual values |

## Deliberate platform differences
- One small Android module with three packages, rather than separate Gradle modules or a DI framework.
- Navigation uses a horizontal row of six tabs that scrolls on narrow devices.
- Android Material date/input/dialog controls and a green theme approximate the source presentation.
- History editing reuses the workout editor. It preserves the active workout while editing/backfilling.
- Notes/date fields are also available in the main workout editor.
- CSV exports use Android's share sheet and a scoped cache FileProvider.
- No cloud/backend, network permission, sync, Health integration, actual rest countdown or additional tracking features.
- Source Swift/Xcode files and the original icon are archived unchanged under reference/ios.

## Data behavior
- Canonical pound storage and 2.20462 pounds per kg match iOS.
- Positive finite weights and integer positive reps are required, including preset calisthenics.
- Empty/incomplete sets never reach Room. An all-invalid edit keeps the existing workout because saving is disabled.
- Edited workouts preserve workout and existing set IDs; set order is explicit.
- Plans reference exercise IDs; stable preset IDs permit idempotent seeding.
- Workouts/sets save and edit in a transaction; workouts cascade-delete sets.
- Preferences persist separately from workout deletion.
- In-memory drafts survive configuration changes, but not process termination, as source drafts are not saved.

## Validation
CI is the executable validation environment for this port. It runs domain tests, Android lint, APK compilation,
a disk-based Room lifecycle test and a Compose workout flow on API 35.
Preview functions exist for screens and reusable components; visual comparison with the iOS app remains a manual device task.
Generated Room schemas are included in CI's android-build-reports artifact; commit the version-1 schema before a database migration.

## Release setup
The release workflow produces unsigned APKs. Signing secrets and Play Console credentials are intentionally not fabricated.
A v* tag creates a prerelease; workflow_dispatch only uploads the unsigned artifact.
For production, configure your signing key, increment versionCode/versionName and add your preferred distribution target.
