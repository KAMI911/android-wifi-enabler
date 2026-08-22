package com.kami911.wifienabler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.kami911.wifienabler.WifiEnablerApp
import com.kami911.wifienabler.service.WifiEnablerService
import com.kami911.wifienabler.util.WifiHelper

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return

        if (event.hasError()) {
            Log.e(TAG, "Geofence error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }

        val prefs = (context.applicationContext as WifiEnablerApp).preferencesManager

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.d(TAG, "Entered geofence — enabling Wi-Fi")
                if (prefs.triggerLocation) {
                    WifiEnablerService.triggerEnable(context, WifiEnablerService.TRIGGER_GEOFENCE_ENTER)
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "Exited geofence")
                if (prefs.autoDisableWifi && prefs.triggerLocation) {
                    if (WifiHelper.isWifiEnabled(context)) {
                        WifiHelper.disableWifi(context)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
