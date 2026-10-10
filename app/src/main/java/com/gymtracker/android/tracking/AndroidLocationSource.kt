package com.gymtracker.android.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface LocationUpdate {
    data class Fix(val location: Location) : LocationUpdate
    data class Status(val message: String) : LocationUpdate
}

interface LocationSource { fun updates(): Flow<LocationUpdate> }

class AndroidLocationSource(private val context: Context) : LocationSource {
    override fun updates(): Flow<LocationUpdate> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { trySend(LocationUpdate.Fix(location)) }
            override fun onProviderEnabled(provider: String) { trySend(LocationUpdate.Status("Waiting for GPS")) }
            override fun onProviderDisabled(provider: String) { trySend(LocationUpdate.Status("Location disabled or signal unavailable")) }
            @Deprecated("Legacy callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        try {
            check(hasPreciseLocation(context)) { "Allow precise location to track your run." }
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { it in manager.allProviders }
            check(providers.isNotEmpty()) { "This device has no location provider." }
            providers.forEach { manager.requestLocationUpdates(it, 2000L, 0f, listener, Looper.getMainLooper()) }
        } catch (e: SecurityException) {
            close(IllegalStateException("Location permission was revoked. Allow precise location and resume tracking.", e))
        } catch (e: Exception) { close(e) }
        awaitClose { manager.removeUpdates(listener) }
    }
}

fun hasPreciseLocation(context: Context) = ContextCompat.checkSelfPermission(context,
    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun locationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(LocationManager::class.java)
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}
