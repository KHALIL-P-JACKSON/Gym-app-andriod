package com.gymtracker.android.presentation.composables.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.service.RunCalculator
import com.gymtracker.android.presentation.composables.components.*
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme
import com.gymtracker.android.tracking.*
import kotlinx.coroutines.CancellationException

@Composable
fun RunScreen(state: RunUiState, settings: Settings, onSettings: (Settings) -> Unit,
    onStart: (DistanceUnit, Double?, Boolean) -> Unit, onEnd: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var precise by remember { mutableStateOf(hasPreciseLocation(context)) }
    var enabled by remember { mutableStateOf(locationEnabled(context)) }
    var preview by remember { mutableStateOf<RunPoint?>(null) }
    var previewStatus by remember { mutableStateOf("Allow precise location to show your position.") }
    var goalText by rememberSaveable { mutableStateOf("") }
    var pendingStart by rememberSaveable { mutableStateOf(false) }
    val goalValue = goalText.replace(',', '.').toDoubleOrNull()
    val validGoal = goalText.isBlank() || goalValue?.let { it.isFinite() && it > 0 && it <= 10000 } == true
    val start by rememberUpdatedState(newValue = {
        if (locationEnabled(context) && hasPreciseLocation(context))
            onStart(settings.distanceUnit, goalValue?.times(settings.distanceUnit.meters), settings.runAudio)
    })
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (pendingStart) { pendingStart = false; start() }
    }
    val continueStart by rememberUpdatedState(newValue = {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        else { pendingStart = false; start() }
    })
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        precise = hasPreciseLocation(context)
        if (precise && pendingStart) continueStart() else pendingStart = false
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumed = true; precise = hasPreciseLocation(context); enabled = locationEnabled(context)
            }
            if (event == Lifecycle.Event.ON_PAUSE) resumed = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(precise, resumed, state.tracking, enabled) {
        if (!precise || !resumed || state.tracking || !enabled) return@LaunchedEffect
        try {
            AndroidLocationSource(context).updates().collect { update ->
                when (update) {
                    is LocationUpdate.Status -> previewStatus = update.message
                    is LocationUpdate.Fix -> {
                        val fix = update.location
                        val age = SystemClock.elapsedRealtime() - fix.elapsedRealtimeNanos / 1_000_000
                        if (age in 0..10000 && fix.hasAccuracy()) {
                            preview = RunPoint(fix.latitude, fix.longitude, 0, fix.accuracy)
                            previewStatus = "Location accuracy ±${fix.accuracy.toInt()} m"
                        }
                    }
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { previewStatus = e.message ?: "Location unavailable" }
    }
    val run = state.run
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (run == null) {
                Text("Ready for your run?", style = MaterialTheme.typography.titleLarge)
                Text("Track your route, time, distance and pace.")
            }
            RunMap(run?.points.orEmpty(), if (state.tracking) run?.points?.lastOrNull() else preview ?: run?.points?.lastOrNull(),
                Modifier.fillMaxWidth().height(300.dp))
            Text(if (state.tracking || run != null) state.gpsStatus else previewStatus, style = MaterialTheme.typography.bodySmall)
            TrackerCard {
                Text(RunCalculator.duration(run?.elapsedMillis ?: 0), style = MaterialTheme.typography.displaySmall)
                Text(RunCalculator.distance(run?.distanceMeters ?: 0.0, run?.unit ?: settings.distanceUnit),
                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("Current pace: ${run?.takeIf { state.tracking }?.let(RunCalculator::currentPace) ?: "—"}")
                Text("Average pace: ${run?.let { RunCalculator.pace(it.elapsedMillis, it.distanceMeters, it.unit) } ?: "—"}")
                run?.splits?.lastOrNull()?.let { Text("Split ${it.number}: ${RunCalculator.duration(it.durationMillis)}") }
                run?.goalMeters?.let { goal ->
                    LinearProgressIndicator(progress = { (run.distanceMeters / goal).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("Goal: ${RunCalculator.distance(goal, run.unit)}" + if (run.distanceMeters >= goal) " • Reached!" else "")
                }
                if (run?.hasGaps == true) Text("This route has recording gaps; distance may be incomplete.", style = MaterialTheme.typography.bodySmall)
            }
            if (run == null) TrackerCard {
                Text("Run preferences", style = MaterialTheme.typography.titleMedium)
                Row {
                    DistanceUnit.entries.forEach { unit ->
                        TextButton(onClick = { onSettings(settings.copy(distanceUnit = unit)) }, enabled = !state.working) {
                            Text(unit.label + if (settings.distanceUnit == unit) " ✓" else "")
                        }
                    }
                }
                OutlinedTextField(goalText, { goalText = it }, label = { Text("Distance goal (${settings.distanceUnit.label}), optional") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, isError = !validGoal,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (!validGoal) Text("Enter a positive distance up to 10,000.", color = MaterialTheme.colorScheme.error)
                SettingsToggle("Spoken splits and goal updates", settings.runAudio, !state.working) { onSettings(settings.copy(runAudio = it)) }
            }
            if (!precise) {
                Text("Precise location is required to record distance accurately.")
                TextButton(onClick = { pendingStart = false; permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Allow Location") }
                TextButton(onClick = { context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open App Permissions") }
            }
            if (!enabled) TextButton(onClick = { context.startActivity(Intent(AndroidSettings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("Enable Phone Location") }
            if (run != null && !state.tracking && run.endedAt == null) {
                Text("Recording was interrupted. The saved route is safe; missing locations cannot be recovered.")
                OutlinedButton(onClick = {
                    pendingStart = true
                    if (!precise) permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    else continueStart()
                }, enabled = !state.working && enabled, modifier = Modifier.fillMaxWidth()) { Text("Resume Tracking") }
            }
        }
        Button(onClick = {
            if (run != null) onEnd()
            else {
                pendingStart = true
                if (!precise) permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                else continueStart()
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !state.loading && !state.working && !pendingStart && (run != null || validGoal && enabled)) {
            Text(if (state.working) "Saving…" else if (run?.endedAt != null) "Retry Save" else if (run != null) "End Run" else "Start Run")
        }
    }
}

@Composable
fun RunDetails(run: RunRecord) {
    Column(Modifier.heightIn(max = 550.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(com.gymtracker.android.presentation.Formatters.date(java.time.Instant.ofEpochMilli(run.startedAt)))
        RunMap(run.points, null, Modifier.fillMaxWidth().height(250.dp), live = false)
        Text("Time: ${RunCalculator.duration(run.elapsedMillis)}")
        Text("Distance: ${RunCalculator.distance(run.distanceMeters, run.unit)}")
        Text("Average pace: ${RunCalculator.pace(run.elapsedMillis, run.distanceMeters, run.unit)}")
        run.goalMeters?.let { Text("Goal: ${RunCalculator.distance(it, run.unit)}") }
        if (run.hasGaps) Text("Recording gaps: distance may be incomplete.")
        run.splits.forEach { split ->
            Text("${if (split.partial) "Final partial split" else "Split ${split.number}"}: " +
                "${RunCalculator.distance(split.distanceMeters, run.unit)} • ${RunCalculator.duration(split.durationMillis)} • " +
                RunCalculator.pace(split.durationMillis, split.distanceMeters, run.unit))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RunPreview() { GymTrackerTheme { RunScreen(RunUiState(loading = false), Settings(), {}, { _, _, _ -> }, {}) } }
