package com.sahlent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sahlent.MainActivity
import com.sahlent.R
import com.sahlent.util.DndManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SilentModeService : Service() {
    // foreground service for silent mode during prayers
    // god i hate foreground services, so much boilerplate
    
    @Inject
    lateinit var dndManager: DndManager
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            ACTION_START_SILENT -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
                startForeground(NOTIFICATION_ID, createNotification(prayerName))
                dndManager.enableSilentMode()
            }
            ACTION_STOP_SILENT -> {
                dndManager.disableSilentMode()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer Silent Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when your phone is silenced for prayer"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(prayerName: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, SilentModeService::class.java).apply {
            action = ACTION_STOP_SILENT
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔕 Silent Mode Active")
            .setContentText("Phone silenced for $prayerName prayer")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "End Silent", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    companion object {
        private const val CHANNEL_ID = "silent_mode_channel"
        private const val NOTIFICATION_ID = 1001 // random id, doesnt conflict with anything
        const val ACTION_START_SILENT = "com.sahlent.START_SILENT"
        const val ACTION_STOP_SILENT = "com.sahlent.STOP_SILENT"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        
        fun startSilentMode(context: Context, prayerName: String) {
            val intent = Intent(context, SilentModeService::class.java).apply {
                action = ACTION_START_SILENT
                putExtra(EXTRA_PRAYER_NAME, prayerName)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopSilentMode(context: Context) {
            val intent = Intent(context, SilentModeService::class.java).apply {
                action = ACTION_STOP_SILENT
            }
            context.startService(intent)
        }
    }
}
