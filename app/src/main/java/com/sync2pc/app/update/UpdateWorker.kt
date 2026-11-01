package com.sync2pc.app.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.sync.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Tägliche Update-Prüfung (nur im WLAN). Meldet eine neue Version einmal per Benachrichtigung. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val s = GlobalSettings(ctx)
        if (!s.autoUpdateCheck || UpdateChecker.normalizeRepo(s.updateRepo) == null) return@withContext Result.success()
        try {
            val info = UpdateChecker.checkAndStore(ctx)
            if (info != null && s.updateNotifiedVersion != info.version) {
                Notifications.updateAvailable(ctx, info.version)
                s.updateNotifiedVersion = info.version
            }
            Result.success()
        } catch (e: Exception) {
            Result.success() // nächster Versuch beim nächsten Intervall
        }
    }

    companion object {
        private const val NAME = "update-check"

        fun schedule(context: Context) {
            val s = GlobalSettings(context)
            val wm = WorkManager.getInstance(context)
            if (!s.autoUpdateCheck || UpdateChecker.normalizeRepo(s.updateRepo) == null) {
                wm.cancelUniqueWork(NAME); return
            }
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
