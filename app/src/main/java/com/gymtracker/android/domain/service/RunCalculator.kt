package com.gymtracker.android.domain.service

import com.gymtracker.android.domain.model.*
import java.util.Locale
import kotlin.math.*

/** Pure GPS processing; map rendering and Android lifecycle never own run distance. */
object RunCalculator {
    fun meters(a: RunPoint, b: RunPoint): Double {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
            cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
        return 6371000.0 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun accept(run: RunRecord, point: RunPoint, forceGap: Boolean = false): RunRecord {
        if (run.endedAt != null || !point.latitude.isFinite() || !point.longitude.isFinite() ||
            point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0 ||
            !point.accuracyMeters.isFinite() || point.accuracyMeters !in 0f..25f ||
            point.elapsedMillis < 0 || point.elapsedMillis > run.elapsedMillis + 2000) return run
        val previous = run.points.lastOrNull()
        if (previous != null && point.elapsedMillis <= previous.elapsedMillis) return run
        val gap = previous != null && (forceGap || point.elapsedMillis - previous.elapsedMillis > 30_000)
        val step = if (previous == null || gap) 0.0 else meters(previous, point)
        if (previous != null && !gap) {
            val seconds = (point.elapsedMillis - previous.elapsedMillis) / 1000.0
            if (step / seconds > 12.0) return run
            // Accumulate against the last accepted point, so slow movement is retained.
            if (step < max(3.0, (previous.accuracyMeters + point.accuracyMeters) * 0.35)) return run
        }
        val nextDistance = run.distanceMeters + step
        val splits = run.splits.toMutableList()
        var boundary = (splits.count { !it.partial } + 1) * run.unit.meters
        while (step > 0 && nextDistance >= boundary) {
            val fraction = (boundary - run.distanceMeters) / step
            val crossing = previous!!.elapsedMillis + ((point.elapsedMillis - previous.elapsedMillis) * fraction).roundToLong()
            val priorTime = splits.sumOf { it.durationMillis }
            splits += RunSplit(splits.size + 1, run.unit.meters, (crossing - priorTime).coerceAtLeast(0))
            boundary += run.unit.meters
        }
        return run.copy(distanceMeters = nextDistance, hasGaps = run.hasGaps || gap,
            points = run.points + point.copy(segment = (previous?.segment ?: 0) + if (gap) 1 else 0),
            splits = splits)
    }

    fun finish(run: RunRecord, endedAt: Long): RunRecord {
        if (run.endedAt != null) return run
        val remainder = run.distanceMeters - run.splits.sumOf { it.distanceMeters }
        val splits = if (remainder > 0.01) run.splits + RunSplit(run.splits.size + 1, remainder,
            (run.elapsedMillis - run.splits.sumOf { it.durationMillis }).coerceAtLeast(0), true) else run.splits
        return run.copy(endedAt = endedAt, splits = splits)
    }

    fun currentPace(run: RunRecord): String {
        val last = run.points.lastOrNull() ?: return "—"
        if (run.elapsedMillis - last.elapsedMillis > 10_000) return "—"
        val recent = run.points.filter { it.segment == last.segment && it.elapsedMillis >= last.elapsedMillis - 20_000 }
        if (recent.size < 2) return "—"
        val distance = recent.zipWithNext().sumOf { (a, b) -> meters(a, b) }
        return pace(last.elapsedMillis - recent.first().elapsedMillis, distance, run.unit)
    }

    fun duration(millis: Long): String {
        val seconds = millis.coerceAtLeast(0) / 1000
        return if (seconds >= 3600) String.format(Locale.US, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
        else String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
    }
    fun distance(meters: Double, unit: DistanceUnit) = String.format(Locale.US, "%.2f %s", meters / unit.meters, unit.label)
    fun pace(millis: Long, meters: Double, unit: DistanceUnit): String {
        if (meters < 3 || millis <= 0) return "—"
        val seconds = (millis / 1000.0 / (meters / unit.meters)).roundToLong()
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} /${unit.label}"
    }
}
