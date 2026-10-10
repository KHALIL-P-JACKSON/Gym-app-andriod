package com.gymtracker.android.tracking

import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.repository.RunRepository
import com.gymtracker.android.domain.service.RunCalculator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Application-owned session. All commands and checkpoints are serialized. */
class RunController(private val environment: RunEnvironment, private val repository: RunRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(RunUiState())
    val state = mutableState.asStateFlow()
    private fun boot() = environment.bootCount()
    private var forceGap = false
    private var lastFix = 0L
    private var speaker: ((String) -> Unit)? = null
    private val ready = scope.async {
        try {
            val run = repository.unfinished()
            mutableState.value = RunUiState(run = run, loading = false,
                gpsStatus = if (run != null) "Unfinished run recovered. Resume tracking or end the run." else "Waiting for GPS")
            forceGap = run != null
        } catch (e: Exception) {
            mutableState.value = RunUiState(loading = false, error = "Couldn't restore runs: ${e.message}")
        }
    }
    init {
        scope.launch {
            ready.await()
            var ticks = 0
            while (isActive) {
                delay(1000)
                mutex.withLock {
                    val old = state.value
                    val run = old.run ?: return@withLock
                    if (run.endedAt != null) return@withLock
                    val updated = run.copy(elapsedMillis = run.elapsed(environment.monotonicMillis(), boot()))
                    mutableState.value = old.copy(run = updated,
                        gpsStatus = if (old.tracking && environment.monotonicMillis() - lastFix > 15_000)
                            "GPS signal unavailable. Time continues; distance may be incomplete." else old.gpsStatus)
                    if (old.tracking && ++ticks % 5 == 0) {
                        try { repository.save(updated) }
                        catch (e: Exception) { report("Couldn't checkpoint run: ${e.message}") }
                    }
                }
            }
        }
    }

    fun requestStart(unit: DistanceUnit, goalMeters: Double?, audio: Boolean) {
        scope.launch {
            ready.await()
            mutex.withLock {
                if (state.value.working || state.value.tracking || state.value.run?.endedAt != null) return@withLock
                if (!environment.canTrack()) {
                    report("Enable location and allow precise location before starting.")
                    return@withLock
                }
                mutableState.update { it.copy(working = true, error = null, saved = null) }
                try {
                    environment.startService(unit, goalMeters, audio)
                } catch (e: Exception) {
                    mutableState.update { it.copy(working = false, error = "Couldn't start tracking: ${e.message}") }
                }
            }
        }
    }

    suspend fun activate(unit: DistanceUnit, goalMeters: Double?, audio: Boolean, speak: (String) -> Unit): Boolean {
        ready.await()
        return mutex.withLock {
            if (state.value.tracking) return@withLock true
            if (!state.value.working) return@withLock false
            val old = state.value.run
            if (old?.endedAt != null) return@withLock false
            var run = old ?: RunRecord(startedAt = environment.wallMillis(),
                clockStartMillis = environment.monotonicMillis(), bootCount = boot(),
                unit = unit, goalMeters = goalMeters?.takeIf { it.isFinite() && it > 0 }, audio = audio)
            if (old != null) {
                val elapsed = old.elapsed(environment.monotonicMillis(), boot())
                run = old.copy(clockStartMillis = environment.monotonicMillis() - elapsed,
                    elapsedMillis = elapsed, bootCount = boot(), hasGaps = true)
                forceGap = true
            }
            try {
                repository.save(run)
                speaker = speak
                lastFix = environment.monotonicMillis()
                mutableState.update { it.copy(run = run, tracking = true, working = false, gpsStatus = "Waiting for GPS", error = null) }
                true
            } catch (e: Exception) {
                mutableState.update { it.copy(working = false, error = "Couldn't save new run: ${e.message}") }
                false
            }
        }
    }

    fun onLocation(location: LocationSample) {
        scope.launch {
            mutex.withLock {
                val old = state.value.run ?: return@withLock
                if (!state.value.tracking || old.endedAt != null) return@withLock
                val now = environment.monotonicMillis()
                val fixTime = location.uptimeMillis
                if (now - fixTime !in 0..10_000 || !location.accuracyMeters.isFinite() || location.accuracyMeters !in 0f..25f) {
                    status("GPS accuracy is low. Waiting for a better fix.")
                    return@withLock
                }
                lastFix = now
                val elapsed = old.elapsed(now, boot())
                val current = old.copy(elapsedMillis = elapsed)
                val point = RunPoint(location.latitude, location.longitude,
                    fixTime - current.clockStartMillis, location.accuracyMeters)
                var next = RunCalculator.accept(current, point, forceGap)
                if (next.points.size == current.points.size) return@withLock
                forceGap = false
                val goalReached = next.goalMeters?.let { next.distanceMeters >= it } == true && !next.goalAnnounced
                if (goalReached) next = next.copy(goalAnnounced = true)
                mutableState.update { it.copy(run = next, gpsStatus = "GPS accuracy ±${location.accuracyMeters.toInt()} m") }
                try {
                    repository.save(next)
                    if (next.audio) {
                        if (next.splits.size > old.splits.size) speaker?.invoke(
                            "${RunCalculator.distance(next.distanceMeters, next.unit)}. Time ${RunCalculator.duration(next.elapsedMillis)}. " +
                                "Average pace ${RunCalculator.pace(next.elapsedMillis, next.distanceMeters, next.unit)}")
                        if (goalReached) speaker?.invoke("Distance goal reached. Keep going! End the run when you're ready.")
                    }
                } catch (e: Exception) { report("Couldn't save location: ${e.message}") }
            }
        }
    }

    fun end() {
        scope.launch {
            ready.await()
            mutex.withLock {
                val run = state.value.run ?: return@withLock
                if (state.value.working) return@withLock
                val finished = RunCalculator.finish(run.copy(elapsedMillis = run.elapsed(environment.monotonicMillis(), boot())),
                    environment.wallMillis())
                mutableState.update { it.copy(run = finished, tracking = false, working = true) }
                environment.stopService()
                try {
                    repository.save(finished)
                    mutableState.value = RunUiState(loading = false, saved = finished)
                } catch (e: Exception) {
                    // Retain the frozen result for retry; do not restart the stopwatch.
                    mutableState.update { it.copy(working = false, error = "Couldn't save run. Tap Retry Save. ${e.message}") }
                }
            }
        }
    }

    suspend fun clearForDelete(block: suspend () -> Unit) {
        ready.await()
        mutex.withLock {
            environment.stopService()
            mutableState.update { it.copy(tracking = false, working = true) }
            speaker = null
            try {
                block()
                mutableState.value = RunUiState(loading = false)
            } finally { mutableState.update { it.copy(working = false) } }
        }
    }
    fun status(message: String) { mutableState.update { it.copy(gpsStatus = message) } }
    fun detached() {
        speaker = null
        forceGap = true
        mutableState.update { it.copy(tracking = false, working = false) }
    }
    fun report(message: String) { mutableState.update { it.copy(error = message) } }
    fun dismissError() { mutableState.update { it.copy(error = null) } }
    fun dismissSaved() { mutableState.update { it.copy(saved = null) } }
}
