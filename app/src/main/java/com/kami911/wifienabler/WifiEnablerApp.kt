package com.kami911.wifienabler

import android.app.Application
import androidx.room.Room
import com.kami911.wifienabler.data.AppDatabase
import com.kami911.wifienabler.util.NotificationHelper
import com.kami911.wifienabler.util.PreferencesManager

class WifiEnablerApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wifi_enabler_db"
        ).build()
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
