package com.sync2pc.app.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sync2pc.app.data.GlobalSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Pausiert"-Zustand nach einem WLAN-Unterbruch.
 *
 * Läuft als Vordergrunddienst mit der Anzeige "Pausiert – wartet auf WLAN …" in der Statusleiste.
 * So hält Android die App sicher am Leben (manche Hersteller beenden Hintergrund-Apps sonst so
 * gründlich, dass weder WLAN-Broadcast noch Aufgabenplaner sie wieder wecken).
 * Der Dienst wartet nur (kein Rechnen, kein Netzwerkverkehr) und startet den Sync, sobald
 * eines der Ziel-WLANs wieder verbunden ist.
 */
class PauseService : Service() {

    companion object {
        private const val EXTRA_PROFILE = "profile"
        private const val EXTRA_DONE = "done"
        private const val EXTRA_TOTAL = "total"
        private const val EXTRA_DONE_BYTES = "doneBytes"
        private const val EXTRA_TOTAL_BYTES = "totalBytes"
        private const val EXTRA_WIFI = "wifi"

        /** true = Dienst gestartet; false = Android erlaubt den Start gerade nicht (Fallback nötig) */
        fun start(context: Context, last: SyncProgress?, wifiNames: String): Boolean {
            val i = Intent(context, PauseService::class.java).apply {
                putExtra(EXTRA_WIFI, wifiNames)
                if (last != null) {
                    putExtra(EXTRA_PROFILE, last.profileName)
                    putExtra(EXTRA_DONE, last.doneFiles); putExtra(EXTRA_TOTAL, last.totalFiles)
                    putExtra(EXTRA_DONE_BYTES, last.doneBytes); putExtra(EXTRA_TOTAL_BYTES, last.totalBytes)
                }
            }
            return try {
                ContextCompat.startForegroundService(context, i); true
            } catch (e: Exception) {
                false // z. B. ForegroundServiceStartNotAllowedException (Android 12+, App im Hintergrund)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PauseService::class.java))
            Notifications.cancelPaused(context)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var checkJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val last = intent?.getStringExtra(EXTRA_PROFILE)?.let {
            SyncProgress(
                it, com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.log_status_paused), intent.getIntExtra(EXTRA_DONE, 0), intent.getIntExtra(EXTRA_TOTAL, 0),
                intent.getLongExtra(EXTRA_DONE_BYTES, 0), intent.getLongExtra(EXTRA_TOTAL_BYTES, 0), null
            )
        }
        val wifi = intent?.getStringExtra(EXTRA_WIFI) ?: ""
        try {
            ServiceCompat.startForeground(
                this, Notifications.ID_PAUSED, Notifications.pausedNotification(this, last, wifi),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } catch (e: Exception) {
            // Vordergrund nicht erlaubt -> normale Anzeige, Fortsetzen übernimmt der Aufgabenplaner
            Notifications.showPaused(this, last, wifi)
            stopSelf()
            return START_NOT_STICKY
        }
        registerWifiWatch()
        return START_NOT_STICKY
    }

    private fun registerWifiWatch() {
        if (callback != null) return
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = scheduleCheck()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = scheduleCheck()
        }
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try { cm.registerNetworkCallback(req, cb); callback = cb } catch (ignored: Exception) {}
        scheduleCheck() // falls das WLAN schon wieder da ist
    }

    /** Prüft (gebündelt) ob ein Ziel-WLAN verbunden ist; der Name kommt oft erst nach ein paar Sekunden. */
    private fun scheduleCheck() {
        if (checkJob?.isActive == true) return
        checkJob = scope.launch {
            val ctx = applicationContext
            for (i in 0 until 6) {
                if (!GlobalSettings(ctx).resumeOnWifi) { stopSelf(); return@launch } // anderweitig fortgesetzt/abgebrochen
                if (WifiHelper.isOnWifi(ctx)) {
                    val ssid = WifiHelper.currentSsid(ctx)
                    val targets = com.sync2pc.app.data.AppDatabase.get(ctx).dao().profiles()
                        .filter { it.enabled }.flatMap { it.ssids() }
                    if (ssid != null && ssid in targets) {
                        SyncScheduler.enqueueAuto(ctx, com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.trigger_resume_wifi, ssid))
                        stopSelf()
                        return@launch
                    }
                    if (ssid != null) return@launch // anderes WLAN -> weiter warten
                } else return@launch
                delay(3_000)
            }
        }
    }

    /** Android 15+: Zeitlimit für dataSync-Dienste erreicht -> Aufgabenplaner übernimmt */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        callback?.let { cb ->
            try { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(cb) } catch (ignored: Exception) {}
        }
        callback = null
        scope.cancel()
        super.onDestroy()
    }
}
