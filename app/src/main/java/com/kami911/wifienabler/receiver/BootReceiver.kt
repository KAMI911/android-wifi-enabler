package com.kami911.wifienabler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kami911.wifienabler.WifiEnablerApp
import com.kami911.wifienabler.service.WifiEnablerService
import com.kami911.wifienabler.util.WifiHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val prefs = (context.applicationContext as WifiEnablerApp).preferencesManager

        if (!prefs.serviceEnabled) {
            Log.d(TAG, "Service disabled by user, skipping boot start")
            return
        }

        Log.d(TAG, "Boot completed — starting WifiEnablerService")
        WifiEnablerService.start(context)

        // Optionally enable Wi-Fi immediately on boot if trigger is configured
        if (prefs.triggerBoot && !WifiHelper.isWifiEnabled(context)) {
            WifiEnablerService.triggerEnable(context, WifiEnablerService.TRIGGER_BOOT)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
