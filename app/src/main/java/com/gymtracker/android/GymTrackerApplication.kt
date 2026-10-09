package com.gymtracker.android

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.gymtracker.android.data.local.TrackerDatabase
import com.gymtracker.android.data.repository.*

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Small composition root: no service locator or DI framework inside the three layers. */
class GymTrackerApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, TrackerDatabase::class.java, "gymtracker.db").build()
    }
    val trackerRepository by lazy { RoomTrackerRepository(database) }
    val settingsRepository by lazy { DataStoreSettingsRepository(settingsStore) }
}
