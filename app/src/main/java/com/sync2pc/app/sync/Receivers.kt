package com.sync2pc.app.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Vom System aufgerufen, wenn ein WLAN verbunden wird. Prüft das WLAN und startet ggf. den Sync. */
class WifiNetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val s = GlobalSettings(ctx)
        if (!s.autoSyncOnWifi) return
        val now = System.currentTimeMillis()
        // Drosselung gegen Mehrfach-Events – aber nie nach einem unterbrochenen Sync (sofort fortsetzen)
        if (!s.resumeOnWifi && now - s.lastWifiTriggerAt < 60_000) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!WifiHelper.isOnWifi(ctx)) return@launch
                val ssid = WifiHelper.currentSsid(ctx)
                val profiles = AppDatabase.get(ctx).dao().profiles().filter { it.enabled }
                // SSID nicht lesbar -> trotzdem einplanen, der Worker prüft erneut
                val match = ssid == null || profiles.any { it.ssids().contains(ssid) }
                if (match) {
                    s.lastWifiTriggerAt = now
                    // Neue WLAN-Verbindung -> PC sofort prüfen, eine laufende 30-Minuten-Wartezeit verfällt
                    s.shareRetryAt = 0; s.shareUnreachable = ""
                    val label = if (s.resumeOnWifi) "Fortsetzen nach WLAN-Unterbruch" else "WLAN verbunden"
                    SyncScheduler.enqueueAuto(ctx, label + (ssid?.let { " ($it)" } ?: ""))
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Nach Neustart/Update: WLAN-Callback und Trigger neu registrieren; nach Selbst-Update App wieder öffnen. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        SyncScheduler.setupAll(ctx)
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val s = GlobalSettings(ctx)
            val version = s.updateInstalling
            if (version.isNotEmpty()) {
                s.updateInstalling = ""
                s.updateVersion = ""
                // Android erlaubt den Start aus dem Hintergrund nicht immer -> zusätzlich Benachrichtigung
                try {
                    ctx.startActivity(
                        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (ignored: Exception) {}
                Notifications.updateInstalled(ctx, version)
            }
        }
    }
}
