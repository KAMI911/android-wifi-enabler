package com.kami911.wifienabler.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serviceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_SERVICE_ENABLED, value) }

    var triggerScreenOn: Boolean
        get() = prefs.getBoolean(KEY_TRIGGER_SCREEN_ON, true)
        set(value) = prefs.edit { putBoolean(KEY_TRIGGER_SCREEN_ON, value) }

    var triggerUnlock: Boolean
        get() = prefs.getBoolean(KEY_TRIGGER_UNLOCK, false)
        set(value) = prefs.edit { putBoolean(KEY_TRIGGER_UNLOCK, value) }

    var triggerLocation: Boolean
        get() = prefs.getBoolean(KEY_TRIGGER_LOCATION, false)
        set(value) = prefs.edit { putBoolean(KEY_TRIGGER_LOCATION, value) }

    var triggerMobileData: Boolean
        get() = prefs.getBoolean(KEY_TRIGGER_MOBILE_DATA, false)
        set(value) = prefs.edit { putBoolean(KEY_TRIGGER_MOBILE_DATA, value) }

    var triggerBoot: Boolean
        get() = prefs.getBoolean(KEY_TRIGGER_BOOT, false)
        set(value) = prefs.edit { putBoolean(KEY_TRIGGER_BOOT, value) }

    var autoDisableWifi: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DISABLE, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_DISABLE, value) }

    var debugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DEBUG_MODE, value) }

    var geofenceLatitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_GEOFENCE_LAT, 0L))
        set(value) = prefs.edit { putLong(KEY_GEOFENCE_LAT, java.lang.Double.doubleToLongBits(value)) }

    var geofenceLongitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_GEOFENCE_LNG, 0L))
        set(value) = prefs.edit { putLong(KEY_GEOFENCE_LNG, java.lang.Double.doubleToLongBits(value)) }

    var geofenceRadius: Float
        get() = prefs.getFloat(KEY_GEOFENCE_RADIUS, DEFAULT_GEOFENCE_RADIUS)
        set(value) = prefs.edit { putFloat(KEY_GEOFENCE_RADIUS, value) }

    var geofenceSet: Boolean
        get() = prefs.getBoolean(KEY_GEOFENCE_SET, false)
        set(value) = prefs.edit { putBoolean(KEY_GEOFENCE_SET, value) }

    companion object {
        private const val PREFS_NAME = "wifi_enabler_prefs"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_TRIGGER_SCREEN_ON = "trigger_screen_on"
        private const val KEY_TRIGGER_UNLOCK = "trigger_unlock"
        private const val KEY_TRIGGER_LOCATION = "trigger_location"
        private const val KEY_TRIGGER_MOBILE_DATA = "trigger_mobile_data"
        private const val KEY_TRIGGER_BOOT = "trigger_boot"
        private const val KEY_AUTO_DISABLE = "auto_disable"
        private const val KEY_DEBUG_MODE = "debug_mode"
        private const val KEY_GEOFENCE_LAT = "geofence_lat"
        private const val KEY_GEOFENCE_LNG = "geofence_lng"
        private const val KEY_GEOFENCE_RADIUS = "geofence_radius"
        private const val KEY_GEOFENCE_SET = "geofence_set"
        const val DEFAULT_GEOFENCE_RADIUS = 500f // metres
    }
}
