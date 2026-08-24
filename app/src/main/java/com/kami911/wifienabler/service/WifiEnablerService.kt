package com.kami911.wifienabler.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.kami911.wifienabler.WifiEnablerApp
import com.kami911.wifienabler.data.LogEntry
import com.kami911.wifienabler.util.NotificationHelper
import com.kami911.wifienabler.util.PreferencesManager
import com.kami911.wifienabler.util.WifiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WifiEnablerService : LifecycleService() {

    private lateinit var prefs: PreferencesManager
    private lateinit var connectivityManager: ConnectivityManager

    /** True while [cellularCallback] is actually registered with [connectivityManager]. */
    private var cellularCallbackRegistered = false

    /** Wall-clock time of the last log-pruning DELETE; see [appendLog]. */
    private var lastPruneTime = 0L

    /**
     * Reacts live to the "mobile data" trigger being flipped in Settings so the
     * cellular NetworkCallback — an idle binder to ConnectivityService that costs
     * a little battery/memory while held — is only registered when that trigger
     * is actually enabled, without requiring a service restart.
     */
    private val prefsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PreferencesManager.KEY_TRIGGER_MOBILE_DATA) {
                if (prefs.triggerMobileData) registerNetworkCallback() else unregisterNetworkCallback()
            }
        }

    // ── Screen / Unlock receiver (registered dynamically; cannot use manifest for these) ──

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (prefs.triggerScreenOn) handleTrigger(TRIGGER_SCREEN_ON, enable = true)
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (prefs.triggerUnlock) handleTrigger(TRIGGER_UNLOCK, enable = true)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (prefs.autoDisableWifi && (prefs.triggerScreenOn || prefs.triggerUnlock)) {
                        handleTrigger(TRIGGER_SCREEN_OFF, enable = false)
                    }
                }
            }
        }
    }

    // ── Cellular network callback ──

    private val cellularCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (prefs.triggerMobileData) handleTrigger(TRIGGER_MOBILE_DATA, enable = true)
        }

        override fun onLost(network: Network) {
            if (prefs.autoDisableWifi && prefs.triggerMobileData) {
                handleTrigger(TRIGGER_MOBILE_DATA_LOST, enable = false)
            }
        }
    }

    // ── Lifecycle ──

    override fun onCreate() {
        super.onCreate()
        prefs = (application as WifiEnablerApp).preferencesManager
        connectivityManager = getSystemService(ConnectivityManager::class.java)

        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID_SERVICE,
            NotificationHelper.buildServiceNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            else 0
        )

        registerScreenReceiver()
        prefs.registerChangeListener(prefsChangeListener)
        if (prefs.triggerMobileData) registerNetworkCallback()
        isRunning = true
        Log.d(TAG, "Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ENABLE_WIFI -> {
                val trigger = intent.getStringExtra(EXTRA_TRIGGER) ?: "Manual"
                handleTrigger(trigger, enable = true)
            }
            ACTION_DISABLE_WIFI -> {
                val trigger = intent.getStringExtra(EXTRA_TRIGGER) ?: "Manual"
                handleTrigger(trigger, enable = false)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterScreenReceiver()
        prefs.unregisterChangeListener(prefsChangeListener)
        unregisterNetworkCallback()
        isRunning = false
        Log.d(TAG, "Service stopped")
        super.onDestroy()
    }

    // ── Wi-Fi control ──

    private fun handleTrigger(trigger: String, enable: Boolean) {
        val success = if (enable) WifiHelper.enableWifi(this) else WifiHelper.disableWifi(this)

        if (enable && !success) {
            // Programmatic enabling failed (restricted on stock Android 10+).
            // Guide the user via a heads-up notification.
            NotificationHelper.showWifiEnablePrompt(this, trigger)
        }

        Log.d(TAG, "Trigger=$trigger enable=$enable success=$success")
        appendLog(trigger, if (enable) "enable" else "disable", success)
    }

    private fun appendLog(trigger: String, action: String, success: Boolean) {
        val db = (application as WifiEnablerApp).database
        lifecycleScope.launch(Dispatchers.IO) {
            db.logDao().insert(
                LogEntry(
                    timestamp = System.currentTimeMillis(),
                    trigger = trigger,
                    action = action,
                    success = success,
                    message = if (success) "Wi-Fi $action successful" else "Wi-Fi $action failed (may require user action)"
                )
            )
            // Prune entries older than 7 days, but at most once per day — running a
            // DELETE on every single trigger is unnecessary disk churn for a service
            // that's meant to stay resident and idle most of the time.
            val now = System.currentTimeMillis()
            if (now - lastPruneTime >= PRUNE_INTERVAL_MS) {
                db.logDao().deleteOlderThan(now - LOG_RETENTION_MS)
                lastPruneTime = now
            }
        }
    }

    // ── Receiver / callback registration ──

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenStateReceiver, filter)
        }
    }

    private fun unregisterScreenReceiver() {
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: IllegalArgumentException) { /* already unregistered */ }
    }

    private fun registerNetworkCallback() {
        if (cellularCallbackRegistered) return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, cellularCallback)
            cellularCallbackRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        if (!cellularCallbackRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(cellularCallback)
        } catch (_: Exception) { /* already unregistered */ }
        cellularCallbackRegistered = false
    }

    // ── Static helpers ──

    companion object {
        private const val TAG = "WifiEnablerService"
        private const val LOG_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L
        private const val PRUNE_INTERVAL_MS = 24 * 60 * 60 * 1000L

        /**
         * True while the service process is actually alive. Distinct from the
         * "service enabled" preference: the OS (Doze, OEM battery managers) can
         * kill this foreground service without the user's action, leaving the
         * preference stale. Callers should compare against it, not assume the
         * preference reflects reality.
         */
        @Volatile
        var isRunning: Boolean = false
            private set

        const val ACTION_STOP = "com.kami911.wifienabler.ACTION_STOP"
        const val ACTION_ENABLE_WIFI = "com.kami911.wifienabler.ACTION_ENABLE_WIFI"
        const val ACTION_DISABLE_WIFI = "com.kami911.wifienabler.ACTION_DISABLE_WIFI"
        const val EXTRA_TRIGGER = "extra_trigger"

        const val TRIGGER_SCREEN_ON = "Screen ON"
        const val TRIGGER_SCREEN_OFF = "Screen OFF"
        const val TRIGGER_UNLOCK = "Device Unlock"
        const val TRIGGER_MOBILE_DATA = "Mobile Data ON"
        const val TRIGGER_MOBILE_DATA_LOST = "Mobile Data OFF"
        const val TRIGGER_BOOT = "Device Boot"
        const val TRIGGER_GEOFENCE_ENTER = "Geofence Enter"
        const val TRIGGER_GEOFENCE_EXIT = "Geofence Exit"

        fun start(context: Context) {
            val intent = Intent(context, WifiEnablerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, WifiEnablerService::class.java).apply { action = ACTION_STOP }
            )
        }

        fun triggerEnable(context: Context, trigger: String) {
            val intent = Intent(context, WifiEnablerService::class.java).apply {
                action = ACTION_ENABLE_WIFI
                putExtra(EXTRA_TRIGGER, trigger)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
