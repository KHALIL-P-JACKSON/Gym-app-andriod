package com.gymtracker.android.tracking

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.gymtracker.android.domain.model.DistanceUnit

interface RunEnvironment {
    fun monotonicMillis(): Long
    fun wallMillis(): Long
    fun bootCount(): Int
    fun canTrack(): Boolean
    fun startService(unit: DistanceUnit, goalMeters: Double?, audio: Boolean)
    fun stopService()
}

class AndroidRunEnvironment(private val context: Context) : RunEnvironment {
    override fun monotonicMillis() = SystemClock.elapsedRealtime()
    override fun wallMillis() = System.currentTimeMillis()
    override fun bootCount() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
    override fun canTrack() = hasPreciseLocation(context) && locationEnabled(context)
    override fun startService(unit: DistanceUnit, goalMeters: Double?, audio: Boolean) {
        ContextCompat.startForegroundService(context, Intent(context, RunTrackingService::class.java)
            .putExtra("unit", unit.name).putExtra("goal", goalMeters ?: 0.0).putExtra("audio", audio))
    }
    override fun stopService() { context.stopService(Intent(context, RunTrackingService::class.java)) }
}

data class LocationSample(val latitude: Double, val longitude: Double, val uptimeMillis: Long, val accuracyMeters: Float)
