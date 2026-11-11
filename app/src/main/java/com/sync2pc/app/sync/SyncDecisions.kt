package com.sync2pc.app.sync

import androidx.work.NetworkType
import androidx.work.WorkInfo
import com.sync2pc.app.R

/**
 * Reine Entscheidungen aus SyncWorker, WifiNetworkReceiver und SyncScheduler (SYNC-20261111-163, Stufe 1).
 * Verhaltensgleich ausgelagert, damit sie ohne Android-Laufzeit testbar sind; die Aufrufer behalten ihre Logik.
 */
object SyncDecisions {
    /**
     * Nach einem Android-Stopp (z. B. WLAN weg) beim Wiederverbinden fortsetzen? «Abbrechen» durch die App/den Benutzer
     * ([WorkInfo.STOP_REASON_CANCELLED_BY_APP]) ist kein Unterbruch, ebenso ein manueller Lauf.
     */
    fun resumeAfterStop(isStopped: Boolean, stopReason: Int, manual: Boolean): Boolean =
        isStopped && stopReason != WorkInfo.STOP_REASON_CANCELLED_BY_APP && !manual

    /** Text für den Verlauf: [res] mit optionalem Zahlenargument [arg] (Stoppgrund) */
    data class InterruptText(val res: Int, val arg: Int? = null)

    /** Warum Android den Lauf gestoppt hat; null = nicht gestoppt oder «Abbrechen» durch den Benutzer (echter Abbruch) */
    fun interruptText(isStopped: Boolean, stopReason: Int): InterruptText? {
        if (!isStopped) return null
        return when (stopReason) {
            WorkInfo.STOP_REASON_CANCELLED_BY_APP -> null
            WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> InterruptText(R.string.stop_wifi)
            WorkInfo.STOP_REASON_CONSTRAINT_CHARGING -> InterruptText(R.string.stop_charger)
            WorkInfo.STOP_REASON_TIMEOUT -> InterruptText(R.string.stop_timeout)
            WorkInfo.STOP_REASON_QUOTA, WorkInfo.STOP_REASON_APP_STANDBY,
            WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION, WorkInfo.STOP_REASON_DEVICE_STATE,
            WorkInfo.STOP_REASON_SYSTEM_PROCESSING -> InterruptText(R.string.stop_system, stopReason)
            else -> InterruptText(R.string.stop_android, stopReason)
        }
    }

    /** Mindestabstand zwischen zwei WLAN-Auslösern (gegen Mehrfach-Events) */
    const val WIFI_THROTTLE_MS = 60_000L

    /** WLAN-Auslöser verwerfen? Nie nach einem unterbrochenen Sync ([resumeOnWifi]: sofort fortsetzen) */
    fun wifiTriggerThrottled(resumeOnWifi: Boolean, now: Long, lastTriggerAt: Long): Boolean =
        !resumeOnWifi && now - lastTriggerAt < WIFI_THROTTLE_MS

    /** Auftragsbeschreibung des automatischen Starts: Netztyp, Laden, Expedited (nur ohne Lade-Bedingung erlaubt) */
    data class AutoSpec(val network: NetworkType, val requiresCharging: Boolean, val expedited: Boolean)

    fun autoSpec(syncOffWifi: Boolean, onlyWhileCharging: Boolean): AutoSpec = AutoSpec(
        network = if (syncOffWifi) NetworkType.CONNECTED else NetworkType.UNMETERED,
        requiresCharging = onlyWhileCharging,
        expedited = !onlyWhileCharging,
    )
}