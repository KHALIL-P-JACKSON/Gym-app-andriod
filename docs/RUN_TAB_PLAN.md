# Run tab implementation plan

Status: implemented in the Android app.

Validation: 20 JVM tests pass, including GPS calculations, splits, clock behavior, recovery, goal announcements, duplicate actions, and frozen save retries. Six isolated database/UI tests pass on the connected Samsung SM-F776U (API 37). Android lint and the architecture check pass; the free map was visually confirmed on the Samsung. Device tests use separate databases and UI fixtures to preserve the owner's data. Real outdoor accuracy, audible updates, and a GPS run with the screen locked still need a practical field check.

## Agreed scope

- Replace the Progress bottom tab with Run and keep all six tabs visible.
- Preserve strength Progress as a separate screen opened from a link on Home.
- Keep the pink light/dark theme.
- Track elapsed time, distance, current pace, average pace, and the route.
- Start Run starts the stopwatch; End Run is the only normal action that stops it. No pause or automatic pause.
- Continue tracking with the screen locked, on another tab, or in another app.
- Support miles/km, mile/km splits, optional distance goals, and optional spoken updates.
- Save completed runs, including their routes and splits, in History.

## Map provider

Use MapLibre Native Android with OpenFreeMap's hosted OpenStreetMap-based vector tiles. These services need no paid map account or API key. Validate a small Android map integration first, including light/dark styles, attribution, current-position marker, route overlays, and lifecycle handling in Compose.

Keep the provider configuration separate from run recording so it can be changed later. Preserve visible attribution. New map tiles need network access; GPS recording, timing, distance, and local saving must continue if tiles cannot load. Dedicated offline map downloads are outside this initial scope. OpenFreeMap has no SLA.

Sources:
- https://maplibre.org/projects/native/
- https://openfreemap.org/
- https://openfreemap.org/quick_start/

## Page and navigation

Before a run: local map with position and accuracy indicator, recenter button, zeroed timer/distance, distance-unit selector, optional goal, audio-update toggle, and a full-width Start Run button styled like Start Workout.

During a run: prominent stopwatch and distance, current/average pace, a pink route line with a current-position marker, goal progress, latest split, recenter control, and End Run. Let users pan the map without the camera constantly overriding them. Recenter restores following. Keep End Run outside the map gestures and visible on small screens.

After End Run: freeze the end timestamp immediately, stop location recording, save the summary/route/splits transactionally, and show the saved run summary. Saving failure preserves the completed session for retry without restarting the timer. Guard repeated Start/End taps.

Move strength Progress behind the Home Strength Progress card's View Progress link. Back returns to Home; this screen does not add a seventh bottom tab. An active run continues while strength progress or another tab is displayed.

## Tracking architecture

Introduce a RunTrackingService, run repository, location-source abstraction, and RunViewModel/state stream separate from the strength-workout session. Start a location foreground service from the visible Start Run action after permission checks. Its ongoing notification displays run status and opens the active run.

Request foreground precise/coarse location permissions and handle approximate-only access with a clear precision requirement for reliable tracking. Declare the location foreground-service type and permissions; handle notification permission on applicable Android versions. Preview location updates stop when the idle Run page is no longer visible; active tracking belongs to the service.

Use Android's monotonic elapsed clock for the stopwatch rather than counting UI ticks. Screen recomposition, navigation, locking, or wall-clock changes must not reset time. A goal or lack of movement must not stop the timer.

Checkpoint the active session and accepted location samples in Room. Reconnect the UI after activity recreation. Recover after process interruption where Android allows it, record coverage gaps, and never invent missing GPS data. Force-stop, reboot, or system termination can interrupt tracking; preserve the unfinished run and offer recovery on reopening instead of promising uninterrupted tracking in those cases.

Android reference: https://developer.android.com/develop/background-work/services/fgs/service-types

## Distance, pace, splits, and goals

Store canonical distance in meters and display miles or kilometers independently of the existing weight preference. Default to miles; allow changing units before a run. Fix the split unit for each run and save it with the session.

Calculate distance from successive accepted GPS fixes. Reject stale/out-of-order samples, inaccurate fixes, and implausible jumps, and use a tested noise filter to reduce distance accumulation while stationary. Do not bridge a long GPS outage as though the intervening route were known; mark route gaps and show GPS status. Stopwatch continues during an outage.

Show current pace from a smoothed recent window, with an unavailable indicator when stopped or accuracy is insufficient. Average pace uses total elapsed time divided by recorded distance, including stationary time. Handle zero distance safely.

Generate mile/km splits at distance crossings with interpolated crossing times, and save the final partial split separately. Optional positive distance goals show progress and announce completion once; reaching a goal does not end the run.

Use Android text-to-speech for optional split updates (distance, elapsed time, and average pace) and goal completion. Default audio off; avoid duplicate announcements after screen recreation or service recovery. Audio failure does not interrupt tracking.

## Persistence and History

Add run and ordered route-point tables with timestamps, accuracy, segment identifiers, and split records. Keep run records separate from strength workouts. Store start/end timestamps, elapsed duration, total distance, goal, split unit, and completion/interruption state. Average pace can be derived from saved totals.

Create and test a Room migration from version 1 to version 2, register it in GymTrackerApplication, and commit the exported schema. Existing workouts, plans, exercises, and settings must survive.

Add All / Workouts / Runs filters to History and run detail pages with the route, date, duration, distance, average pace, and splits. Support confirmed run deletion. Extend Delete All Data to run data and safely terminate an active session before clearing it. Extend CSV export with a documented run-summary export; route coordinates remain available in the app rather than being mixed into workout rows.

## Delivery order and validation

1. Validate MapLibre/OpenFreeMap rendering and marker/route support on Android.
2. Add run models, settings, Room migration, and repository with persistence tests.
3. Implement clock, location filtering, distance/pace/splits, and foreground tracking service.
4. Build Run UI, replace the tab, and add Home-to-Progress navigation.
5. Add goals, spoken updates, saved-run History/detail, deletion, and export integration.
6. Run compile, unit tests, migration/repository tests, UI checks, and a physical-device outdoor trial.

Test known GPS tracks, stationary drift, poor accuracy, duplicate fixes, route gaps, unit conversions, split interpolation, zero distance, clock changes, repeated taps, and save retries. Verify location denial/approximate permission, GPS disabled/lost, notification denial, map failure, tab switching, rotation, process recovery, screen lock, and app backgrounding. Verify audio/split/goal events fire once and goals never stop timing. Check small screens and larger text so the six-tab bar and End Run remain accessible. A real-device lock-screen run is required to validate background behavior and practical distance accuracy.
