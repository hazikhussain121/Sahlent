package com.sahlent.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sahlent.data.model.CalculationMethod
import com.sahlent.data.model.Madhab
import com.sahlent.data.model.PrayerName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sahlent_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val AUTO_SILENT_ENABLED = booleanPreferencesKey("auto_silent_enabled")
        val CALCULATION_METHOD = intPreferencesKey("calculation_method")
        val MADHAB = intPreferencesKey("madhab") // Juristic method for Asr
        val SILENT_DURATION_MINUTES = intPreferencesKey("silent_duration_minutes")
        val PRAYER_START_DELAY_MINUTES = intPreferencesKey("prayer_start_delay_minutes")
        val TASBIH_COUNT = intPreferencesKey("tasbih_count")
        val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        val TASBIH_TOTAL_COUNT = longPreferencesKey("tasbih_total_count")
        
        // Per-prayer silent settings
        val SILENT_FAJR = booleanPreferencesKey("silent_fajr")
        val SILENT_DHUHR = booleanPreferencesKey("silent_dhuhr")
        val SILENT_ASR = booleanPreferencesKey("silent_asr")
        val SILENT_MAGHRIB = booleanPreferencesKey("silent_maghrib")
        val SILENT_ISHA = booleanPreferencesKey("silent_isha")
    }
    
    // Auto-silent settings
    val autoSilentEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SILENT_ENABLED] ?: true }
    
    suspend fun setAutoSilentEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SILENT_ENABLED] = enabled }
    }
    
    // Calculation method
    val calculationMethod: Flow<CalculationMethod> = context.dataStore.data.map { 
        CalculationMethod.fromId(it[Keys.CALCULATION_METHOD] ?: CalculationMethod.MWL.id)
    }
    
    suspend fun setCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { it[Keys.CALCULATION_METHOD] = method.id }
    }
    
    // Madhab (juristic method for Asr timing)
    val madhab: Flow<Madhab> = context.dataStore.data.map { 
        Madhab.fromId(it[Keys.MADHAB] ?: Madhab.SHAFII.id)
    }
    
    suspend fun setMadhab(madhab: Madhab) {
        context.dataStore.edit { it[Keys.MADHAB] = madhab.id }
    }
    
    // Silent duration (how long to stay silent)
    val silentDurationMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.SILENT_DURATION_MINUTES] ?: 20 }
    
    suspend fun setSilentDurationMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SILENT_DURATION_MINUTES] = minutes }
    }
    
    // Prayer start delay (minutes after azan to start silent mode)
    val prayerStartDelayMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.PRAYER_START_DELAY_MINUTES] ?: 0 }
    
    suspend fun setPrayerStartDelayMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.PRAYER_START_DELAY_MINUTES] = minutes }
    }
    
    // Tasbih count (current session)
    val tasbihCount: Flow<Int> = context.dataStore.data.map { it[Keys.TASBIH_COUNT] ?: 0 }
    
    suspend fun setTasbihCount(count: Int) {
        context.dataStore.edit { it[Keys.TASBIH_COUNT] = count }
    }
    
    suspend fun incrementTasbih(): Int {
        var newCount = 0
        context.dataStore.edit { prefs ->
            newCount = (prefs[Keys.TASBIH_COUNT] ?: 0) + 1
            prefs[Keys.TASBIH_COUNT] = newCount
            prefs[Keys.TASBIH_TOTAL_COUNT] = (prefs[Keys.TASBIH_TOTAL_COUNT] ?: 0L) + 1
        }
        return newCount
    }
    
    // Tasbih target
    val tasbihTarget: Flow<Int> = context.dataStore.data.map { it[Keys.TASBIH_TARGET] ?: 33 }
    
    suspend fun setTasbihTarget(target: Int) {
        context.dataStore.edit { it[Keys.TASBIH_TARGET] = target }
    }
    
    // Total tasbih count (lifetime)
    val tasbihTotalCount: Flow<Long> = context.dataStore.data.map { it[Keys.TASBIH_TOTAL_COUNT] ?: 0L }
    
    // Per-prayer silent settings
    fun isPrayerSilentEnabled(prayer: PrayerName): Flow<Boolean> {
        val key = when (prayer) {
            PrayerName.FAJR -> Keys.SILENT_FAJR
            PrayerName.DHUHR -> Keys.SILENT_DHUHR
            PrayerName.ASR -> Keys.SILENT_ASR
            PrayerName.MAGHRIB -> Keys.SILENT_MAGHRIB
            PrayerName.ISHA -> Keys.SILENT_ISHA
            PrayerName.SUNRISE -> return kotlinx.coroutines.flow.flowOf(false) // Sunrise is not a prayer
        }
        return context.dataStore.data.map { it[key] ?: true }
    }
    
    suspend fun setPrayerSilentEnabled(prayer: PrayerName, enabled: Boolean) {
        val key = when (prayer) {
            PrayerName.FAJR -> Keys.SILENT_FAJR
            PrayerName.DHUHR -> Keys.SILENT_DHUHR
            PrayerName.ASR -> Keys.SILENT_ASR
            PrayerName.MAGHRIB -> Keys.SILENT_MAGHRIB
            PrayerName.ISHA -> Keys.SILENT_ISHA
            PrayerName.SUNRISE -> return
        }
        context.dataStore.edit { it[key] = enabled }
    }
}
