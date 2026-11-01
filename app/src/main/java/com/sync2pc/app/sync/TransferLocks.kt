package com.sync2pc.app.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager

/**
 * Hält während einer Übertragung WLAN und CPU wach.
 * Ohne das schaltet Android das WLAN im Standby (Sperrbildschirm) in einen Stromsparmodus oder trennt es,
 * was zu "Software caused connection abort" führt. Die Sperren gelten nur während eines Laufs.
 */
class TransferLocks private constructor(
    private val wifiLocks: List<WifiManager.WifiLock>,
    private val wakeLock: PowerManager.WakeLock?,
) {
    fun release() {
        wifiLocks.forEach { try { if (it.isHeld) it.release() } catch (ignored: Exception) {} }
        try { if (wakeLock?.isHeld == true) wakeLock.release() } catch (ignored: Exception) {}
    }

    companion object {
        /** Sicherheitsgrenze, falls ein Lauf hängen bleibt */
        private const val MAX_HOLD_MS = 4L * 3600 * 1000

        @Suppress("DEPRECATION")
        fun acquire(context: Context): TransferLocks {
            val ctx = context.applicationContext
            val wm = ctx.getSystemService(WifiManager::class.java)
            val wifi = ArrayList<WifiManager.WifiLock>()
            // HIGH_PERF hält das WLAN auch bei ausgeschaltetem Bildschirm aktiv (ältere/die meisten Geräte),
            // LOW_LATENCY ergänzt das ab Android 10, solange die App sichtbar ist.
            for (mode in listOf(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WifiManager.WIFI_MODE_FULL_LOW_LATENCY)) {
                try {
                    wm?.createWifiLock(mode, "LeoSync:transfer:$mode")?.apply {
                        setReferenceCounted(false); acquire()
                    }?.let { wifi += it }
                } catch (ignored: Exception) {}
            }
            val wake = try {
                ctx.getSystemService(PowerManager::class.java)
                    ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LeoSync:transfer")
                    ?.apply { setReferenceCounted(false); acquire(MAX_HOLD_MS) }
            } catch (e: Exception) { null }
            return TransferLocks(wifi, wake)
        }
    }
}
