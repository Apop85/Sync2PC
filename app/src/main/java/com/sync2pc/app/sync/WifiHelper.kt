package com.sync2pc.app.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

object WifiHelper {
    private const val UNKNOWN = "<unknown ssid>"

    /**
     * Name des aktuell verbundenen WLANs oder null.
     * Benötigt Standort-Berechtigung + aktivierten Standortdienst (Android-Vorgabe),
     * im Hintergrund zusätzlich "Standort immer erlauben".
     */
    @Suppress("DEPRECATION")
    fun currentSsid(context: Context): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val net = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(net) ?: return null
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        var ssid: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ssid = (caps.transportInfo as? WifiInfo)?.ssid
        }
        if (ssid == null || ssid == UNKNOWN) {
            val wm = context.applicationContext.getSystemService(WifiManager::class.java)
            ssid = wm?.connectionInfo?.ssid
        }
        if (ssid == null || ssid == UNKNOWN) return null
        return ssid.removeSurrounding("\"")
    }

    fun isOnWifi(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isLocationEnabled(context: Context): Boolean =
        context.getSystemService(LocationManager::class.java)?.isLocationEnabled == true

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, WifiNetworkReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

    /**
     * Registriert beim System einen Callback für WLAN-Verbindungen.
     * Die App muss dafür NICHT laufen – das System weckt sie per Broadcast (akkuschonend).
     */
    fun registerWifiCallback(context: Context) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val pi = pendingIntent(context)
        try { cm.unregisterNetworkCallback(pi) } catch (ignored: Exception) {}
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try { cm.registerNetworkCallback(request, pi) } catch (ignored: Exception) {}
    }

    fun unregisterWifiCallback(context: Context) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        try { cm.unregisterNetworkCallback(pendingIntent(context)) } catch (ignored: Exception) {}
    }
}
