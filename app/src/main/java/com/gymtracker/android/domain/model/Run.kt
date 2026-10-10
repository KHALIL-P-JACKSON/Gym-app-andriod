package com.gymtracker.android.domain.model

import java.util.UUID

enum class DistanceUnit(val label: String, val meters: Double) {
    MILES("mi", 1609.344), KM("km", 1000.0)
}

data class RunPoint(
    val latitude: Double, val longitude: Double, val elapsedMillis: Long,
    val accuracyMeters: Float, val segment: Int = 0
)

data class RunSplit(val number: Int, val distanceMeters: Double, val durationMillis: Long, val partial: Boolean = false)

data class RunRecord(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long, val clockStartMillis: Long, val bootCount: Int,
    val elapsedMillis: Long = 0, val endedAt: Long? = null,
    val distanceMeters: Double = 0.0, val unit: DistanceUnit = DistanceUnit.MILES,
    val goalMeters: Double? = null, val audio: Boolean = false,
    val goalAnnounced: Boolean = false, val hasGaps: Boolean = false,
    val points: List<RunPoint> = emptyList(), val splits: List<RunSplit> = emptyList()
) {
    fun elapsed(now: Long, currentBoot: Int): Long =
        if (endedAt != null || currentBoot != bootCount) elapsedMillis
        else (now - clockStartMillis).coerceAtLeast(elapsedMillis)
}

data class RunUiState(
    val run: RunRecord? = null, val tracking: Boolean = false,
    val loading: Boolean = true, val working: Boolean = false,
    val gpsStatus: String = "Waiting for GPS", val error: String? = null,
    val saved: RunRecord? = null
)
