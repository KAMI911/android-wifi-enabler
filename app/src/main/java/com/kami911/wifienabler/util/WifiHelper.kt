package com.kami911.wifienabler.util

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log

object WifiHelper {

    private const val TAG = "WifiHelper"

    fun isWifiEnabled(context: Context): Boolean {
        val wm = wifiManager(context)
        return wm.isWifiEnabled
    }

    /**
     * Attempts to enable Wi-Fi programmatically.
     *
     * On Android 10 (API 29)+ stock AOSP, [WifiManager.setWifiEnabled] is restricted
     * for third-party apps and always returns false. However, many OEM ROMs (Samsung,
     * Xiaomi, etc.) still honour it. The method is tried first; if it fails the caller
     * should invoke [showWifiSettingsPanel] to guide the user.
     *
     * @return true if Wi-Fi is already enabled or was enabled successfully.
     */
    @Suppress("DEPRECATION")
    fun enableWifi(context: Context): Boolean {
        val wm = wifiManager(context)
        if (wm.isWifiEnabled) return true
        return try {
            val success = wm.setWifiEnabled(true)
            Log.d(TAG, "setWifiEnabled(true) = $success")
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException enabling Wi-Fi", e)
            false
        }
    }

    /**
     * Attempts to disable Wi-Fi programmatically.
     * Subject to the same Android 10+ restrictions as [enableWifi].
     *
     * @return true if Wi-Fi was already disabled or was disabled successfully.
     */
    @Suppress("DEPRECATION")
    fun disableWifi(context: Context): Boolean {
        val wm = wifiManager(context)
        if (!wm.isWifiEnabled) return true
        return try {
            val success = wm.setWifiEnabled(false)
            Log.d(TAG, "setWifiEnabled(false) = $success")
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException disabling Wi-Fi", e)
            false
        }
    }

    /**
     * Opens the system Wi-Fi settings panel (Android 10+ bottom-sheet) or the full
     * Wi-Fi settings page on older versions. Requires [Intent.FLAG_ACTIVITY_NEW_TASK]
     * when called from a non-Activity context.
     */
    fun showWifiSettingsPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun wifiManager(context: Context): WifiManager =
        context.applicationContext.getSystemService(WifiManager::class.java)
}
