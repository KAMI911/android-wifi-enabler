package com.kami911.wifienabler.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kami911.wifienabler.WifiEnablerApp
import com.kami911.wifienabler.geofence.GeofenceManager
import com.kami911.wifienabler.service.WifiEnablerService
import com.kami911.wifienabler.util.PreferencesManager
import com.kami911.wifienabler.util.WifiHelper

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs: PreferencesManager = (app as WifiEnablerApp).preferencesManager
    private val geofenceManager = GeofenceManager(app.applicationContext)

    private val _serviceEnabled = MutableLiveData(prefs.serviceEnabled)
    val serviceEnabled: LiveData<Boolean> = _serviceEnabled

    private val _wifiCurrentlyEnabled = MutableLiveData(WifiHelper.isWifiEnabled(app))
    val wifiCurrentlyEnabled: LiveData<Boolean> = _wifiCurrentlyEnabled

    private val _snackMessage = MutableLiveData<String?>()
    val snackMessage: LiveData<String?> = _snackMessage

    // Expose preferences as properties (read by Fragment on resume)
    val triggerScreenOn: Boolean get() = prefs.triggerScreenOn
    val triggerUnlock: Boolean get() = prefs.triggerUnlock
    val triggerLocation: Boolean get() = prefs.triggerLocation
    val triggerMobileData: Boolean get() = prefs.triggerMobileData
    val triggerBoot: Boolean get() = prefs.triggerBoot
    val autoDisable: Boolean get() = prefs.autoDisableWifi
    val debugMode: Boolean get() = prefs.debugMode
    val geofenceSet: Boolean get() = prefs.geofenceSet
    val geofenceLat: Double get() = prefs.geofenceLatitude
    val geofenceLng: Double get() = prefs.geofenceLongitude
    val geofenceRadius: Float get() = prefs.geofenceRadius

    fun setServiceEnabled(enabled: Boolean, context: Context) {
        prefs.serviceEnabled = enabled
        _serviceEnabled.value = enabled
        if (enabled) WifiEnablerService.start(context) else WifiEnablerService.stop(context)
    }

    fun setTriggerScreenOn(enabled: Boolean) { prefs.triggerScreenOn = enabled }
    fun setTriggerUnlock(enabled: Boolean) { prefs.triggerUnlock = enabled }
    fun setTriggerMobileData(enabled: Boolean) { prefs.triggerMobileData = enabled }
    fun setTriggerBoot(enabled: Boolean) { prefs.triggerBoot = enabled }
    fun setAutoDisable(enabled: Boolean) { prefs.autoDisableWifi = enabled }
    fun setDebugMode(enabled: Boolean) { prefs.debugMode = enabled }

    fun setTriggerLocation(enabled: Boolean, context: Context) {
        prefs.triggerLocation = enabled
        if (!enabled) geofenceManager.removeGeofence()
    }

    fun saveGeofence(
        lat: Double,
        lng: Double,
        radius: Float,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        geofenceManager.addGeofence(
            latitude = lat,
            longitude = lng,
            radiusMeters = radius,
            onSuccess = {
                prefs.geofenceLatitude = lat
                prefs.geofenceLongitude = lng
                prefs.geofenceRadius = radius
                prefs.geofenceSet = true
                prefs.triggerLocation = true
                onSuccess()
            },
            onFailure = { e ->
                onFailure(e.localizedMessage ?: "Unknown error")
            }
        )
    }

    fun clearGeofence() {
        geofenceManager.removeGeofence()
        prefs.geofenceSet = false
        prefs.triggerLocation = false
    }

    fun refreshWifiState() {
        _wifiCurrentlyEnabled.value = WifiHelper.isWifiEnabled(getApplication())
    }

    fun snackMessageShown() {
        _snackMessage.value = null
    }
}
