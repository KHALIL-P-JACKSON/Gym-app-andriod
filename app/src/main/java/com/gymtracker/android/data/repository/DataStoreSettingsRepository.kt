package com.gymtracker.android.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    private val unit = stringPreferencesKey("weightUnit")
    private val appearance = stringPreferencesKey("appearance")
    private val rest = intPreferencesKey("restSeconds")
    private val awake = booleanPreferencesKey("keepAwake")
    private val discard = booleanPreferencesKey("confirmDiscard")
    private val distance = stringPreferencesKey("distanceUnit")
    private val runAudio = booleanPreferencesKey("runAudio")
    override val settings = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { prefs ->
        Settings(
            WeightUnit.entries.firstOrNull { it.name == prefs[unit] } ?: WeightUnit.LB,
            Appearance.entries.firstOrNull { it.name == prefs[appearance] } ?: Appearance.SYSTEM,
            prefs[rest] ?: 0, prefs[awake] ?: false, prefs[discard] ?: true,
            DistanceUnit.entries.firstOrNull { it.name == prefs[distance] } ?: DistanceUnit.MILES,
            prefs[runAudio] ?: false
        )
    }
    override suspend fun update(settings: Settings) {
        store.edit {
            it[unit] = settings.unit.name
            it[appearance] = settings.appearance.name
            it[rest] = settings.restSeconds
            it[awake] = settings.keepAwake
            it[discard] = settings.confirmDiscard
            it[distance] = settings.distanceUnit.name
            it[runAudio] = settings.runAudio
        }
    }
}
