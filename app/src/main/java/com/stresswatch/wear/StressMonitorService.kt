package com.stresswatch.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground Service opzionale — tiene vivo il monitoraggio
 * anche quando l'utente abbassa il polso.
 */
class StressMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "stress_monitor_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoraggio Stress",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitoraggio stress in tempo reale"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("StressWatch attivo")
            .setContentText("Monitoraggio stress in corso…")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
}
