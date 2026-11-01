package com.sync2pc.app.sync

import android.content.Context
import android.provider.MediaStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sync2pc.app.data.GlobalSettings
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Wann wird synchronisiert? (alles akkuschonend über WorkManager/System-Callbacks)
 *  - WLAN verbunden      -> WifiNetworkReceiver -> sofort (expedited)
 *  - neue Fotos/Videos   -> Content-Trigger (gebündelt) -> Sync, sobald im WLAN
 *  - zu junge Dateien    -> Nachhol-Sync nach Ablauf des Mindestalters
 *  - Sicherheitsnetz     -> alle 6 Stunden (nur WLAN)
 *  - manuell             -> Button in der App
 */
object SyncScheduler {
    const val WORK_AUTO = "sync-auto"
    const val WORK_MANUAL = "sync-manual"
    const val WORK_FOLLOWUP = "sync-followup"
    const val WORK_CONTENT = "trigger-content"
    const val WORK_PERIODIC = "trigger-periodic"
    val ALL_SYNC_WORK = listOf(WORK_AUTO, WORK_MANUAL, WORK_FOLLOWUP)

    const val KEY_TRIGGER = "trigger"
    const val KEY_MANUAL = "manual"

    private fun autoConstraints(context: Context): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(GlobalSettings(context).onlyWhileCharging)
            .build()

    fun enqueueAuto(context: Context, trigger: String) {
        val charging = GlobalSettings(context).onlyWhileCharging
        val b = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(KEY_TRIGGER to trigger, KEY_MANUAL to false))
            .setConstraints(autoConstraints(context))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
        // Expedited erlaubt nur Netzwerk-Constraints
        if (!charging) b.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        // Läuft schon ein Sync, bleibt er bestehen. Fotos, die während des Laufs entstehen,
        // erkennt SyncWorker am Ende und plant einen Nachhol-Sync.
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_AUTO, ExistingWorkPolicy.KEEP, b.build())
    }

    fun enqueueManual(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(KEY_TRIGGER to "Manuell", KEY_MANUAL to true))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_MANUAL, ExistingWorkPolicy.KEEP, req)
    }

    /** Nachhol-Sync, wenn zu junge Dateien übersprungen wurden */
    fun enqueueFollowUp(context: Context, atMillis: Long) {
        val delay = (atMillis - System.currentTimeMillis()).coerceAtLeast(30_000)
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(KEY_TRIGGER to "Nachholen (Mindestalter)", KEY_MANUAL to false))
            .setConstraints(autoConstraints(context))
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_FOLLOWUP, ExistingWorkPolicy.REPLACE, req)
    }

    /** Einmaliger Content-Trigger; wird nach jedem Sync-Lauf neu "scharf" gestellt. */
    fun armContentTrigger(context: Context, replace: Boolean = true) {
        val wm = WorkManager.getInstance(context)
        if (!GlobalSettings(context).syncOnNewMedia) {
            wm.cancelUniqueWork(WORK_CONTENT); return
        }
        val c = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .setTriggerContentUpdateDelay(Duration.ofSeconds(30))
            .setTriggerContentMaxDelay(Duration.ofMinutes(5))
            .build()
        val req = OneTimeWorkRequestBuilder<TriggerWorker>()
            .setInputData(workDataOf(KEY_TRIGGER to "Neue Fotos/Videos"))
            .setConstraints(c)
            .build()
        wm.enqueueUniqueWork(
            WORK_CONTENT,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            req
        )
    }

    fun schedulePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<TriggerWorker>(6, TimeUnit.HOURS)
            .setInputData(workDataOf(KEY_TRIGGER to "Periodisch"))
            .setConstraints(autoConstraints(context))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** Beim App-Start, nach Neustart und nach Änderung der Einstellungen aufrufen. */
    fun setupAll(context: Context) {
        val s = GlobalSettings(context)
        if (s.autoSyncOnWifi) WifiHelper.registerWifiCallback(context) else WifiHelper.unregisterWifiCallback(context)
        armContentTrigger(context, replace = false)
        schedulePeriodic(context)
        com.sync2pc.app.update.UpdateWorker.schedule(context)
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        ALL_SYNC_WORK.forEach { wm.cancelUniqueWork(it) }
    }
}
