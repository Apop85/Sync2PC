package com.sync2pc.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Leichtgewichtiger Auslöser (neue Medien / periodisch): stellt nur einen Sync-Lauf ein. */
class TriggerWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val trigger = inputData.getString(SyncScheduler.KEY_TRIGGER) ?: "Auslöser"
        SyncScheduler.enqueueAuto(applicationContext, trigger)
        return Result.success()
    }
}
