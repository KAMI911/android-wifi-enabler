package com.kami911.wifienabler.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kami911.wifienabler.MainActivity
import com.kami911.wifienabler.R

object NotificationHelper {

    const val CHANNEL_SERVICE = "wifi_enabler_service"
    const val CHANNEL_WIFI_ACTION = "wifi_action"

    const val NOTIFICATION_ID_SERVICE = 1001
    const val NOTIFICATION_ID_WIFI_PROMPT = 1002

    fun createNotificationChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            context.getString(R.string.notification_channel_service),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_service_desc)
            setShowBadge(false)
        }

        val wifiActionChannel = NotificationChannel(
            CHANNEL_WIFI_ACTION,
            context.getString(R.string.notification_channel_wifi_action),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_wifi_action_desc)
        }

        nm.createNotificationChannel(serviceChannel)
        nm.createNotificationChannel(wifiActionChannel)
    }

    fun buildServiceNotification(context: Context): Notification {
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_wifi_notification)
            .setContentTitle(context.getString(R.string.notification_service_title))
            .setContentText(context.getString(R.string.notification_service_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    /**
     * Shows a heads-up notification prompting the user to enable Wi-Fi manually.
     * This is shown when programmatic Wi-Fi enabling fails (typically stock Android 10+).
     */
    fun showWifiEnablePrompt(context: Context, trigger: String) {
        val wifiIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, wifiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WIFI_ACTION)
            .setSmallIcon(R.drawable.ic_wifi_notification)
            .setContentTitle(context.getString(R.string.notification_wifi_prompt_title))
            .setContentText(context.getString(R.string.notification_wifi_prompt_text, trigger))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_wifi_notification,
                context.getString(R.string.notification_enable_wifi),
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WIFI_PROMPT, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted; silently ignore
        }
    }
}
