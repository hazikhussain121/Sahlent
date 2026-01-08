package com.sahlent.util

import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DndManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    /**
     * Check if the app has permission to modify Do Not Disturb settings
     */
    fun hasPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }
    
    /**
     * Enable silent/DND mode - no interruptions
     */
    fun enableSilentMode() {
        if (hasPermission()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }
    
    /**
     * Disable silent mode - allow all notifications
     */
    fun disableSilentMode() {
        if (hasPermission()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
    
    /**
     * Enable priority only mode - allows alarms and priority notifications
     */
    fun enablePriorityMode() {
        if (hasPermission()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }
    
    /**
     * Get the current interruption filter
     */
    fun getCurrentFilter(): Int {
        return notificationManager.currentInterruptionFilter
    }
    
    /**
     * Check if currently in silent/DND mode
     */
    fun isSilentModeActive(): Boolean {
        return notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
    }
}
