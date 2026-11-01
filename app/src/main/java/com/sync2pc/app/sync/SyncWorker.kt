package com.sync2pc.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.media.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/** Führt den Sync für alle passenden Profile aus (läuft im Hintergrund, ggf. als Vordergrunddienst). */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        /** Verhindert parallele Läufe (Auto, Manuell, Nachholen) */
        private val mutex = Mutex()

        const val P_PROFILE = "profile"
        const val P_PHASE = "phase"
        const val P_DONE = "done"
        const val P_TOTAL = "total"
        const val P_DONE_BYTES = "doneBytes"
        const val P_TOTAL_BYTES = "totalBytes"
        const val P_FILE = "file"

        fun progressFrom(d: Data): SyncProgress? {
            val profile = d.getString(P_PROFILE) ?: return null
            return SyncProgress(
                profile, d.getString(P_PHASE) ?: "", d.getInt(P_DONE, 0), d.getInt(P_TOTAL, 0),
                d.getLong(P_DONE_BYTES, 0), d.getLong(P_TOTAL_BYTES, 0), d.getString(P_FILE)
            )
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        Notifications.foregroundInfo(applicationContext, null, null)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext Result.success()
        try {
            work()
        } finally {
            mutex.unlock()
            SyncScheduler.armContentTrigger(applicationContext)
        }
    }

    private suspend fun work(): Result {
        val ctx = applicationContext
        val startedAt = System.currentTimeMillis()
        val manual = inputData.getBoolean(SyncScheduler.KEY_MANUAL, false)
        val trigger = inputData.getString(SyncScheduler.KEY_TRIGGER) ?: "Automatisch"
        val dao = AppDatabase.get(ctx).dao()
        dao.closeDanglingRuns(System.currentTimeMillis())

        val all = dao.profiles().filter { it.enabled && it.smbPath.isNotBlank() && it.folders().isNotEmpty() }
        if (all.isEmpty()) return Result.success()
        val ssid = WifiHelper.currentSsid(ctx)
        val eligible = if (manual) all else all.filter { p -> ssid != null && p.ssids().contains(ssid) }
        if (eligible.isEmpty()) return Result.success()

        val cancel = WorkManager.getInstance(ctx).createCancelPendingIntent(id)
        try {
            setForeground(Notifications.foregroundInfo(ctx, null, cancel))
        } catch (e: Exception) {
            // Android 12+: Start aus dem Hintergrund evtl. nicht erlaubt -> normale Benachrichtigung
            Notifications.updateProgress(ctx, null, cancel)
        }

        var uploaded = 0; var failed = 0; var waiting = 0; var bytes = 0L
        var hardFailure: String? = null
        var youngAt = 0L
        try {
            for (p in eligible) {
                val engine = SyncEngine(
                    context = ctx, profile = p, trigger = trigger,
                    shouldContinue = {
                        if (manual) true else {
                            val s = WifiHelper.currentSsid(ctx)
                            if (s != null) p.ssids().contains(s) else WifiHelper.isOnWifi(ctx)
                        }
                    },
                    onProgress = { pr ->
                        Notifications.updateProgress(ctx, pr, cancel)
                        setProgressAsync(
                            workDataOf(
                                P_PROFILE to pr.profileName, P_PHASE to pr.phase, P_DONE to pr.doneFiles,
                                P_TOTAL to pr.totalFiles, P_DONE_BYTES to pr.doneBytes,
                                P_TOTAL_BYTES to pr.totalBytes, P_FILE to pr.currentFile
                            )
                        )
                    },
                )
                val r = engine.run()
                uploaded += r.uploaded; failed += r.failed; waiting += r.waiting; bytes += r.bytes
                if (r.status == "FAILED") hardFailure = "${p.name}: ${r.message}"
                if (engine.nextYoungFileAt > 0 && (youngAt == 0L || engine.nextYoungFileAt < youngAt)) youngAt = engine.nextYoungFileAt
            }
        } finally {
            Notifications.cancelProgress(ctx)
        }

        // --- Abschluss-Benachrichtigungen ----------------------------------------
        if (hardFailure != null) {
            Notifications.summary(ctx, "Sync fehlgeschlagen – $hardFailure", error = true)
        } else if (uploaded > 0 || failed > 0) {
            Notifications.summary(
                ctx,
                "$uploaded Dateien übertragen (${Notifications.formatBytes(bytes)})" +
                    (if (failed > 0) ", $failed Fehler – Details im Verlauf" else ""),
                error = failed > 0
            )
        }
        if (waiting > 0) {
            val groups = dao.decisions().size
            Notifications.decisionNeeded(ctx, groups, waiting)
        }
        val toDelete = dao.queuedDeletes().map { it.sourceKey }.distinct().size
        if (toDelete > 0) Notifications.deleteReady(ctx, toDelete)
        // Während des Laufs neu entstandene Fotos/Videos -> nach Mindestalter nachholen
        val minAgeMs = GlobalSettings(ctx).minFileAgeMinutes * 60_000L
        if (MediaScanner.hasAddedSince(ctx, startedAt / 1000)) {
            val at = System.currentTimeMillis() + minAgeMs
            if (youngAt == 0L || at < youngAt) youngAt = at
        }
        if (youngAt > 0) SyncScheduler.enqueueFollowUp(ctx, youngAt)

        return if (hardFailure != null && !manual && runAttemptCount < 3) Result.retry() else Result.success()
    }
}
