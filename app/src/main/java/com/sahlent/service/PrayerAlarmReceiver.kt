package com.sahlent.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sahlent.data.datastore.SettingsDataStore
import com.sahlent.data.model.Prayer
import com.sahlent.data.model.PrayerName
import com.sahlent.util.DndManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var dndManager: DndManager
    
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        
        Log.d(TAG, "Received action: $action for prayer: $prayerName")
        
        CoroutineScope(Dispatchers.Main).launch {
            val autoSilentEnabled = settingsDataStore.autoSilentEnabled.first()
            if (!autoSilentEnabled) {
                Log.d(TAG, "Auto-silent disabled, ignoring")
                return@launch
            }
            
            when (action) {
                ACTION_PRAYER_START -> {
                    Log.d(TAG, "Starting silent mode for $prayerName")
                    dndManager.enableSilentMode()
                    
                    // Schedule end of silent period
                    val durationMinutes = settingsDataStore.silentDurationMinutes.first()
                    scheduleSilentEnd(context, prayerName, durationMinutes)
                }
                ACTION_PRAYER_END -> {
                    Log.d(TAG, "Ending silent mode for $prayerName")
                    dndManager.disableSilentMode()
                }
            }
        }
    }
    
    private fun scheduleSilentEnd(context: Context, prayerName: String, durationMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val endIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_END
            putExtra(EXTRA_PRAYER_NAME, prayerName)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.hashCode() + 1000, // Different request code for end
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
    
    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        const val ACTION_PRAYER_START = "com.sahlent.ACTION_PRAYER_START"
        const val ACTION_PRAYER_END = "com.sahlent.ACTION_PRAYER_END"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        
        fun schedulePrayerAlarms(context: Context, prayers: List<Prayer>, delayMinutes: Int = 0) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val now = System.currentTimeMillis()
            val delayMs = delayMinutes * 60 * 1000L
            
            prayers.filter { 
                it.name != PrayerName.SUNRISE && (it.timeInMillis + delayMs) > now 
            }.forEach { prayer ->
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_PRAYER_START
                    putExtra(EXTRA_PRAYER_NAME, prayer.name.name)
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    prayer.name.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Add delay offset to prayer time (for congregational prayer)
                val triggerTime = prayer.timeInMillis + delayMs
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                
                Log.d(TAG, "Scheduled alarm for ${prayer.name} at ${prayer.time} + ${delayMinutes}min delay")
            }
        }
        
        fun cancelAllAlarms(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            PrayerName.entries.forEach { prayerName ->
                // Cancel start alarm
                val startIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_PRAYER_START
                }
                val startPendingIntent = PendingIntent.getBroadcast(
                    context,
                    prayerName.hashCode(),
                    startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(startPendingIntent)
                
                // Cancel end alarm
                val endIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_PRAYER_END
                }
                val endPendingIntent = PendingIntent.getBroadcast(
                    context,
                    prayerName.hashCode() + 1000,
                    endIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(endPendingIntent)
            }
        }
    }
}
