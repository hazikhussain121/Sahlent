package com.sahlent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sahlent.data.datastore.SettingsDataStore
import com.sahlent.data.repository.PrayerRepository
import com.sahlent.util.LocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var prayerRepository: PrayerRepository
    
    @Inject
    lateinit var locationHelper: LocationHelper
    
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, rescheduling prayer alarms")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val autoSilentEnabled = settingsDataStore.autoSilentEnabled.first()
                    if (!autoSilentEnabled) {
                        Log.d(TAG, "Auto-silent disabled, not scheduling alarms")
                        return@launch
                    }
                    
                    val location = locationHelper.getLastLocation()
                    if (location != null) {
                        val method = settingsDataStore.calculationMethod.first()
                        val result = prayerRepository.getPrayerTimes(
                            location.latitude,
                            location.longitude,
                            method
                        )
                        
                        result.onSuccess { prayers ->
                            val delayMinutes = settingsDataStore.prayerStartDelayMinutes.first()
                            PrayerAlarmReceiver.schedulePrayerAlarms(context, prayers.asList(), delayMinutes)
                            Log.d(TAG, "Prayer alarms rescheduled after boot with ${delayMinutes}min delay")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule alarms: ${e.message}")
                }
            }
        }
    }
    
    companion object {
        private const val TAG = "BootReceiver"
    }
}
