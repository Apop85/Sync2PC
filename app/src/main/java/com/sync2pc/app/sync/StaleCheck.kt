package com.sync2pc.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.ProfileType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Erinnerung "lange nicht gesichert" (0.2.0): einmal täglich prüfen, ob ein aktives Profil seit
 * [GlobalSettings.staleWarnDays] Tagen keinen erfolgreichen Lauf hatte.
 * Bildordner melden sich nur, wenn tatsächlich neue Fotos/Videos warten (kein Fehlalarm ohne neue Bilder).
 * Die Erinnerung wiederholt sich alle [REPEAT_DAYS] Tage, solange der Zustand anhält.
 */
object StaleCheck {
    const val WORK = "stale-check"
    const val REPEAT_DAYS = 3
    private const val DAY = 24L * 3600 * 1000

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        // Nur manuell synchronisieren = bewusst keine Automatik -> auch keine Erinnerung
        if (!GlobalSettings(context).staleWarnEnabled || GlobalSettings(context).manualOnly) {
            wm.cancelUniqueWork(WORK)
            Notifications.cancelStale(context)
            return
        }
        // Keine Bedingungen: Die Prüfung ist rein lokal und kurz
        val req = PeriodicWorkRequestBuilder<StaleCheckWorker>(24, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** true, wenn die nächste Erinnerung fällig ist (erste oder seit [REPEAT_DAYS] Tagen keine mehr) */
    fun shouldNotify(now: Long, lastWarnAt: Long): Boolean = lastWarnAt <= 0 || now - lastWarnAt >= REPEAT_DAYS * DAY

    suspend fun run(context: Context) {
        val s = GlobalSettings(context)
        if (!s.staleWarnEnabled || s.manualOnly) return
        val dao = AppDatabase.get(context).dao()
        val now = System.currentTimeMillis()
        val days = s.staleWarnDays
        val limit = now - days * DAY
        val baseline = s.staleBaseline()
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
        val lines = ArrayList<String>()

        for (p in dao.profiles().filter { it.enabled && it.isConfigured() }) {
            val last = dao.lastSuccessAt(p.id) ?: baseline
            if (last > limit) continue
            if (p.type() == ProfileType.PHOTOS) {
                val pending = PendingCheck.readyFiles(context, p, s.minFileAgeMinutes)
                if (pending == 0) continue
                lines += "${p.name}: $pending neue Dateien warten, letzte Sicherung ${date.format(Date(last))}"
            } else {
                lines += "${p.name}: letzte Sicherung ${date.format(Date(last))}"
            }
        }

        if (lines.isEmpty()) {
            s.lastStaleWarnAt = 0
            Notifications.cancelStale(context)
            return
        }
        if (!shouldNotify(now, s.lastStaleWarnAt)) return
        Notifications.staleWarning(context, days, lines)
        s.lastStaleWarnAt = now
    }

}

class StaleCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            StaleCheck.run(applicationContext)
        } catch (e: Exception) {
            // Eine Erinnerung ist nie wichtig genug, um Fehler zu werfen
        }
        return Result.success()
    }
}
