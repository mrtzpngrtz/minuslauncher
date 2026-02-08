package com.minimal.launcher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppNotificationListener : NotificationListenerService() {
    companion object {
        private val _activeNotifications = MutableStateFlow<Set<String>>(emptySet())
        val activeNotifications: StateFlow<Set<String>> = _activeNotifications.asStateFlow()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        updateNotifications()
    }
    
    private fun updateNotifications() {
        try {
            val sbnArray = super.getActiveNotifications()
            if (sbnArray != null) {
                val packages = sbnArray.map { it.packageName }.toSet()
                _activeNotifications.value = packages
            }
        } catch (e: Exception) {
            // Handle security exception if service not fully bound
        }
    }
}
