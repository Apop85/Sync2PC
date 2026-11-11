package com.sync2pc.app.sync

import android.content.Context
import android.provider.MediaStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Data
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.l10n.Msg
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Wann wird synchronisiert? (alles akkuschonend über WorkManager/System-Callbacks)
 *  - WLAN verbunden      -> WifiNetworkReceiver -> sofort (expedited)
 *  - neue Fotos/Videos   -> Content-Trigger (gebündelt) -> Sync, sobald im WLAN
 *  - zu junge Dateien    -> Nachhol-Sync nach Ablauf des Mindestalters
 *  - Sicherheitsnetz     -> 6 Stunden nach dem letzten Sync (bei jedem Sync neu gestartet, nur WLAN)
 *  - manuell             -> Button in der App
 */
object SyncScheduler {
    const val WORK_AUTO = "sync-auto"
    const val WORK_MANUAL = "sync-manual"
    const val WORK_FOLLOWUP = "sync-followup"
    const val WORK_CONTENT = "trigger-content"
    /** Bis 0.3.3: starrer 6-h-Auftrag; wird beim Start entfernt (ersetzt durch WORK_SAFETY) */
    const val WORK_PERIODIC = "trigger-periodic"
    const val WORK_RETRY = "sync-share-retry"
    const val WORK_REMOTE_POLL = "trigger-remote-poll"
    const val WORK_RESUME = "sync-resume"
    /** Nachlauf nach einem während eines Laufs verworfenen Start (R-N1): automatisch bzw. manuell */
    const val WORK_DEFERRED_AUTO = "sync-deferred"
    const val WORK_DEFERRED_MANUAL = "sync-deferred-manual"
    /** Folgelauf nach übersprungenen (während des Laufs geänderten) Dateien, SYNC-20261111-173; eigener Name: WORK_FOLLOWUP bleibt unberührt */
    const val WORK_SKIP_FOLLOWUP = "sync-skip-followup"
    val ALL_SYNC_WORK = listOf(
        WORK_AUTO, WORK_MANUAL, WORK_FOLLOWUP, WORK_RETRY, WORK_RESUME, WORK_DEFERRED_AUTO, WORK_DEFERRED_MANUAL,
        WORK_SKIP_FOLLOWUP
    )
    const val KEY_SKIP_FOLLOWUP = "skipFollowUp"
    const val KEY_RESUME = "resume"

    /** Sonderauftrag (Fortsetzen, Wiederholung nach «PC nicht erreichbar», Folgelauf): bei belegter Sperre `Result.retry()` statt Merker */
    fun isSpecialRun(d: Data): Boolean =
        d.getBoolean(KEY_RESUME, false) || d.getBoolean(KEY_RETRY, false) || d.getBoolean(KEY_SKIP_FOLLOWUP, false)

    /** Wartezeit, bevor ein Sonderauftrag bei belegter Sperre erneut versucht wird (linearer Backoff) */
    const val SPECIAL_BACKOFF_SECONDS = 30L

    /**
     * Nach einem WLAN-Unterbruch: Auftrag beim Android-Aufgabenplaner hinterlegen, der startet,
     * sobald wieder ein WLAN da ist – unabhängig davon, ob die App noch läuft
     * (zweiter Weg neben dem WLAN-Broadcast, den manche Hersteller blockieren).
     * Ist es das falsche WLAN, plant der Worker sich mit [delayMinutes] erneut ein.
     */
    fun enqueueResume(context: Context, delayMinutes: Long = 0, appendToCurrent: Boolean = false) {
        val b = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_resume), KEY_MANUAL to false, KEY_RESUME to true))
            .setConstraints(autoConstraints(context))
            .setBackoffCriteria(BackoffPolicy.LINEAR, SPECIAL_BACKOFF_SECONDS, TimeUnit.SECONDS)
        if (delayMinutes > 0) b.setInitialDelay(delayMinutes, TimeUnit.MINUTES)
        // Aus dem laufenden Fortsetzen-Auftrag heraus anhängen statt ersetzen (sonst bricht er sich selbst ab)
        val policy = if (appendToCurrent) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_RESUME, policy, b.build())
    }

    /** Wartezeit, wenn der Share im Ziel-WLAN nicht erreichbar ist */
    const val SHARE_RETRY_MINUTES = 30L

    const val KEY_TRIGGER = "trigger"
    /** Auslöser als Kennung (0.12.0, DEC-159); fehlt sie (bereits eingeplante Aufträge), gilt der Text */
    const val KEY_TRIGGER_CODE = "triggerCode"

    /** Auftragsdaten mit Auslöser: Text (Rückfall) und Kennung */
    fun triggerData(trigger: Msg, vararg more: Pair<String, Any?>): Data {
        val b = Data.Builder().putAll(workDataOf(*more)).putString(KEY_TRIGGER, trigger.text())
        trigger.toCode()?.let { b.putString(KEY_TRIGGER_CODE, it) }
        return b.build()
    }

    /** Auslöser aus den Auftragsdaten; [default] bei fehlendem Eintrag */
    fun triggerOf(data: Data, default: Msg): Msg =
        Msg.fromJson(data.getString(KEY_TRIGGER_CODE)) ?: data.getString(KEY_TRIGGER)?.let { Msg.lit(it) } ?: default
    const val KEY_MANUAL = "manual"
    const val KEY_RETRY = "retry"

    /** Share nicht erreichbar -> in 30 Minuten erneut prüfen (nur im WLAN) */
    fun enqueueShareRetry(context: Context, minutes: Long = SHARE_RETRY_MINUTES) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_retry), KEY_MANUAL to false, KEY_RETRY to true))
            .setConstraints(autoConstraints(context))
            .setBackoffCriteria(BackoffPolicy.LINEAR, SPECIAL_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_RETRY, ExistingWorkPolicy.REPLACE, req)
    }

    fun cancelShareRetry(context: Context) {
        GlobalSettings(context).apply { shareRetryAt = 0; shareUnreachable = "" }
        WorkManager.getInstance(context).cancelUniqueWork(WORK_RETRY)
    }

    /**
     * Der Schalter "auch mobile Daten/VPN" (syncOffWifi) hat gewechselt (Einstellungen oder Sicherung einspielen, 0.11.7):
     * bereits geplante Aufträge tragen die alte Netzwerk-Bedingung -> Sicherheitsnetz neu planen, und die Wartezeit
     * "PC nicht erreichbar" gehört zur alten Netzwerkregel -> verwerfen (Prüfung beim nächsten Auslöser).
     */
    fun onOffWifiChanged(context: Context) {
        scheduleSafetyNet(context, restart = true)
        cancelShareRetry(context)
    }

    private fun autoConstraints(context: Context): Constraints =
        autoSpec(context).let { spec ->
            Constraints.Builder()
                // 0.11.5 (DEC-126): mit dem Schalter "auch mobile Daten/VPN" genügt irgendeine Verbindung
                .setRequiredNetworkType(spec.network)
                .setRequiresCharging(spec.requiresCharging)
                .build()
        }

    private fun autoSpec(context: Context): SyncDecisions.AutoSpec {
        val s = GlobalSettings(context)
        return SyncDecisions.autoSpec(s.syncOffWifi, s.onlyWhileCharging)
    }

    /**
     * Automatischer Start. Läuft gerade ein Sync, wird der Start nicht mehr stumm verworfen (R-N1), sondern vorgemerkt und
     * nach dessen Ende einmal nachgeholt: Rückgabe [StartResult.DEFERRED] (die Oberfläche kann das melden).
     */
    fun enqueueAuto(context: Context, trigger: Msg): StartResult {
        if (gate(context).requestStart(manual = false)) return StartResult.DEFERRED
        // Läuft noch nichts, bleibt ein bereits eingereihter Auftrag bestehen (KEEP). Fotos, die während des Laufs entstehen,
        // erkennt SyncWorker am Ende und plant einen Nachhol-Sync.
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_AUTO, ExistingWorkPolicy.KEEP, autoRequest(context, trigger))
        return StartResult.STARTED
    }

    private fun autoRequest(context: Context, trigger: Msg): OneTimeWorkRequest {
        val spec = autoSpec(context)
        val b = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(triggerData(trigger, KEY_MANUAL to false))
            .setConstraints(autoConstraints(context))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
        // Expedited erlaubt nur Netzwerk-Constraints
        if (spec.expedited) b.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        return b.build()
    }

    /**
     * Manueller Start ("Synchronisieren") – hebt eine Pause auf, wer von Hand startet, will synchronisieren.
     * Läuft gerade ein Sync, wird der Start als manueller vorgemerkt und danach nachgeholt (R-N1): [StartResult.DEFERRED].
     */
    fun enqueueManual(context: Context): StartResult {
        clearPause(context)
        if (gate(context).requestStart(manual = true)) return StartResult.DEFERRED
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_MANUAL, ExistingWorkPolicy.KEEP, manualRequest())
        return StartResult.STARTED
    }

    private fun manualRequest(): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_manual), KEY_MANUAL to true))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

    // ---- Sperre und Nachlauf (SYNC-20261111-143, R-N1/R-BG4/R-BG5) -----------------------------------------------------

    /** Ausgabedaten eines Auftrags, der die Sperre nicht bekam: sein Start wurde vorgemerkt (nicht verworfen) */
    const val KEY_DEFERRED = "deferred"

    @Volatile private var gateInstance: SyncGate? = null

    /** Sperre samt Nachlauf-Merker des Prozesses (Merker persistent in GlobalSettings) */
    internal fun gate(context: Context): SyncGate = gateInstance ?: synchronized(this) {
        gateInstance ?: SyncGate(object : DeferredStore {
            private val settings = GlobalSettings(context.applicationContext)
            override fun read(): DeferredKind? = DeferredKind.fromStore(settings.deferredStart)
            override fun write(kind: DeferredKind?): Boolean = settings.writeDeferredStart(kind?.toStore() ?: "")
        }, onStoreFailure = { android.util.Log.w("SyncGate", it) }).also { gateInstance = it }
    }

    /**
     * Für die Oberfläche: ist ein Nachlauf vorgemerkt ([DeferredState.requested]) und war ein manueller Start dabei
     * ([DeferredState.manual])? Ein StateFlow; wird mit «Pausieren»/«Abbrechen» ([cancelAll]) und nach dem Einreihen
     * des Nachlaufs wieder leer.
     */
    fun deferredState(context: Context): StateFlow<DeferredState> = gate(context).state

    /**
     * Reiht den vorgemerkten Nachlauf genau einmal ein: manuell, wenn ein manueller Start angefordert war, sonst automatisch
     * mit dem Auslöser «nachgeholt». Eigene Auftragsnamen (REPLACE): der laufende Auftrag trägt selbst einen der Namen
     * WORK_AUTO/WORK_MANUAL, ein KEEP dort würde den Nachlauf verwerfen. APPEND_OR_REPLACE statt REPLACE (Review -143): ein Nachlauf, der selbst unter diesem Namen läuft, darf sich beim Einreihen des nächsten nicht selbst abbrechen; er hängt sich hinten an (bei gescheitertem oder abgebrochenem Vorgänger ersetzt er ihn). Ein Nachlauf ersetzt einen noch wartenden
     * gleichartigen (nie zwei).
     */
    internal fun enqueueDeferred(context: Context, kind: DeferredKind) {
        val wm = WorkManager.getInstance(context)
        when (kind) {
            DeferredKind.MANUAL -> wm.enqueueUniqueWork(WORK_DEFERRED_MANUAL, ExistingWorkPolicy.APPEND_OR_REPLACE, manualRequest())
            DeferredKind.AUTO -> wm.enqueueUniqueWork(
                WORK_DEFERRED_AUTO, ExistingWorkPolicy.APPEND_OR_REPLACE,
                autoRequest(context, Msg.of(com.sync2pc.app.R.string.trigger_deferred))
            )
        }
    }

    /**
     * Prozessstart (z. B. nach Prozess-Kill): ein persistenter Merker ohne laufenden Sync wird jetzt abgearbeitet.
     * Scheitert das Einreihen, bleibt der Merker erhalten.
     */
    private fun resumeDeferred(context: Context) {
        val g = gate(context)
        val kind = g.claimIfIdle() ?: return
        try { enqueueDeferred(context, kind) } catch (t: Throwable) { g.restore(kind) }
    }

    /** Nachhol-Sync, wenn zu junge Dateien übersprungen wurden */
    fun enqueueFollowUp(context: Context, atMillis: Long) {
        val delay = (atMillis - System.currentTimeMillis()).coerceAtLeast(30_000)
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_followup), KEY_MANUAL to false))
            .setConstraints(autoConstraints(context))
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_FOLLOWUP, ExistingWorkPolicy.REPLACE, req)
    }

    /**
     * Folgelauf in [SkipFollowUp.DELAY_MINUTES] Minuten (Bedingungen wie automatische Läufe). APPEND_OR_REPLACE: ein gerade laufender Folgelauf (gleicher Name) verwirft den neuen nicht (KEEP täte es);
     * die Begrenzung auf einen pro Stunde liegt bei [SkipFollowUp.mayPlan].
     */
    fun enqueueSkipFollowUp(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_skip_followup), KEY_MANUAL to false, KEY_SKIP_FOLLOWUP to true))
            .setConstraints(autoConstraints(context))
            .setBackoffCriteria(BackoffPolicy.LINEAR, SPECIAL_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInitialDelay(SkipFollowUp.DELAY_MINUTES, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_SKIP_FOLLOWUP, ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }

    /** Einmaliger Content-Trigger; wird nach jedem Sync-Lauf neu "scharf" gestellt. */
    fun armContentTrigger(context: Context, replace: Boolean = true) {
        val wm = WorkManager.getInstance(context)
        val gs = GlobalSettings(context)
        if (!gs.syncOnNewMedia || gs.manualOnly) {
            wm.cancelUniqueWork(WORK_CONTENT); return
        }
        // Akku (0.3.4): erst auslösen, wenn das neueste Foto das Mindestalter erreicht hat. Sonst findet der
        // erste Lauf nur "zu junge" Dateien und ein zweiter Lauf (Nachhol-Sync) wird nötig.
        val minAge = Duration.ofMinutes(GlobalSettings(context).minFileAgeMinutes.toLong())
        val update = maxOf(Duration.ofSeconds(30), minAge.plusSeconds(30))
        val c = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .setTriggerContentUpdateDelay(update)
            // Bei einer Fotoserie spätestens nach dieser Zeit (ältere Bilder der Serie sind dann alt genug)
            .setTriggerContentMaxDelay(maxOf(Duration.ofMinutes(5), update.plusMinutes(2)))
            .build()
        val req = OneTimeWorkRequestBuilder<TriggerWorker>()
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_new_media)))
            .setConstraints(c)
            .build()
        wm.enqueueUniqueWork(
            WORK_CONTENT,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            req
        )
    }

    /**
     * Ordner-Profile: Änderungen auf dem PC melden sich nicht von selbst ->
     * im eingestellten Intervall prüfen (nur WLAN; der Worker prüft zusätzlich das Ziel-WLAN).
     */
    fun scheduleRemotePoll(context: Context) {
        val minutes = GlobalSettings(context).remotePollMinutes
        val wm = WorkManager.getInstance(context)
        if (minutes <= 0 || GlobalSettings(context).manualOnly) { wm.cancelUniqueWork(WORK_REMOTE_POLL); return }
        val req = PeriodicWorkRequestBuilder<TriggerWorker>(minutes.coerceAtLeast(15).toLong(), TimeUnit.MINUTES)
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_poll)))
            .setConstraints(autoConstraints(context))
            .build()
        wm.enqueueUniquePeriodicWork(WORK_REMOTE_POLL, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** Nach Änderungen an Profilen aufrufen: kürzestes Intervall der aktiven Ordner-Profile übernehmen */
    fun updateRemotePoll(context: Context, profiles: List<com.sync2pc.app.data.Profile>) {
        val min = profiles.filter { it.enabled && it.type() == com.sync2pc.app.data.ProfileType.MIRROR }
            .minOfOrNull { it.remotePollMinutes.coerceAtLeast(15) } ?: 0
        GlobalSettings(context).remotePollMinutes = min
        scheduleRemotePoll(context)
    }

    /** Sicherheitsnetz (seit 0.3.4): 6 Stunden nach dem letzten Sync, wird bei jedem Sync nach hinten geschoben */
    const val WORK_SAFETY = "trigger-safety"
    const val SAFETY_HOURS = 6L

    /**
     * Sicherheitsnetz, falls ein Auslöser verloren geht (z. B. WLAN-Meldung vom Hersteller blockiert).
     * [restart] = true (nach jedem Sync-Lauf): Zähler neu starten -> 6 h nach dem letzten Sync.
     * [restart] = false (App-Start): nur anlegen, wenn noch keiner geplant ist.
     */
    fun scheduleSafetyNet(context: Context, restart: Boolean) {
        val wm = WorkManager.getInstance(context)
        // Bis 0.3.3 ein starrer 6-h-Rhythmus (periodischer Auftrag) -> entfernen
        wm.cancelUniqueWork(WORK_PERIODIC)
        if (GlobalSettings(context).manualOnly) { wm.cancelUniqueWork(WORK_SAFETY); return }
        val req = OneTimeWorkRequestBuilder<TriggerWorker>()
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_safety, SAFETY_HOURS)))
            .setConstraints(autoConstraints(context))
            .setInitialDelay(SAFETY_HOURS, TimeUnit.HOURS)
            .build()
        wm.enqueueUniqueWork(WORK_SAFETY, if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, req)
    }

    /** Beim App-Start, nach Neustart und nach Änderung der Einstellungen aufrufen. */
    fun setupAll(context: Context) {
        val s = GlobalSettings(context)
        // "Für immer pausiert": kein WLAN-Auslöser (die übrigen Auslöser prüfen manualOnly selbst)
        if (s.autoSyncOnWifi && !s.manualOnly) WifiHelper.registerWifiCallback(context) else WifiHelper.unregisterWifiCallback(context)
        armContentTrigger(context, replace = false)
        scheduleSafetyNet(context, restart = false)
        scheduleRemotePoll(context)
        com.sync2pc.app.update.UpdateWorker.schedule(context)
        StaleCheck.schedule(context)
        // R-N1: ein nach Prozess-Kill noch vorgemerkter Nachlauf (ohne laufenden Sync) wird jetzt nachgeholt
        resumeDeferred(context)
    }

    const val WORK_PAUSE_END = "pause-end"

    /**
     * Dienst für [hours] Stunden pausieren (0.3.1): laufenden Sync anhalten, automatische Starts unterdrücken.
     * Am Ende der Pause startet ein Auftrag den Sync von selbst wieder.
     */
    fun pause(context: Context, hours: Int) {
        val ms = hours.coerceIn(1, 168) * 3_600_000L
        GlobalSettings(context).pausedUntil = System.currentTimeMillis() + ms
        cancelAll(context)
        val req = OneTimeWorkRequestBuilder<TriggerWorker>()
            .setInputData(triggerData(Msg.of(com.sync2pc.app.R.string.trigger_pause_end)))
            .setInitialDelay(ms, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_PAUSE_END, ExistingWorkPolicy.REPLACE, req)
    }

    /** Pause aufheben (ohne selbst einen Sync zu starten). true = es war pausiert */
    fun clearPause(context: Context): Boolean {
        val s = GlobalSettings(context)
        if (s.pausedUntil == 0L) return false
        s.pausedUntil = 0
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PAUSE_END)
        return true
    }

    /**
     * "Für immer pausieren" (0.3.5): Hintergrund-Syncs ganz abschalten, nur noch manuell.
     * Bleibt bestehen, bis der Benutzer fortsetzt ("Synchronisieren" hebt es nicht auf).
     */
    fun pauseForever(context: Context) {
        val s = GlobalSettings(context)
        s.manualOnly = true
        clearPause(context)
        cancelAll(context)
        setupAll(context) // schaltet WLAN-Auslöser, Foto-Auslöser, Sicherheitsnetz, Ordner-Prüfung und Erinnerung ab
    }

    /** Pause (befristet oder "für immer") sofort beenden und prüfen, ob es etwas zu tun gibt */
    fun resume(context: Context) {
        val s = GlobalSettings(context)
        val wasManualOnly = s.manualOnly
        if (wasManualOnly) { s.manualOnly = false; setupAll(context) }
        if (clearPause(context) || wasManualOnly) enqueueAuto(context, Msg.of(com.sync2pc.app.R.string.trigger_resumed))
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        // Zuerst den vorgemerkten Nachlauf verwerfen: der abgebrochene Lauf soll nach seinem Ende nichts mehr einreihen
        // («Pausieren», «Für immer pausieren» und «Abbrechen» durch den Benutzer, R-N1)
        gate(context).discardDeferred()
        ALL_SYNC_WORK.forEach { wm.cancelUniqueWork(it) }
        // Abbrechen durch den Benutzer: kein automatisches Fortsetzen
        GlobalSettings(context).resumeOnWifi = false
        Notifications.cancelInterrupted(context)
        PauseService.stop(context)
    }
}
