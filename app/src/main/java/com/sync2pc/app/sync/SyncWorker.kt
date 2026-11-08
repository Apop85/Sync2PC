package com.sync2pc.app.sync

import com.sync2pc.app.data.RunStatus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sync2pc.app.R
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.messageText
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.mirror.MirrorEngine
import com.sync2pc.app.media.MediaScanner
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.smb.SmbTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
            // Fortschrittsanzeige nie stehen lassen (sie würde sonst nach einem Abbruch "hängen")
            Notifications.cancelProgress(applicationContext)
            // Von Android gestoppt (z. B. WLAN weg -> Netzwerk-Bedingung verletzt): beim Wiederverbinden fortsetzen
            // ("Abbrechen" durch den Benutzer ist kein Unterbruch -> nicht fortsetzen)
            if (isStopped && stopReason != WorkInfo.STOP_REASON_CANCELLED_BY_APP &&
                !inputData.getBoolean(SyncScheduler.KEY_MANUAL, false)
            ) markInterrupted()
            mutex.unlock()
            SyncScheduler.armContentTrigger(applicationContext)
            // Sicherheitsnetz: 6 Stunden nach diesem Lauf erneut prüfen (schiebt den Zeitpunkt nach hinten)
            SyncScheduler.scheduleSafetyNet(applicationContext, restart = true)
        }
    }

    private val isResumeRun: Boolean get() = inputData.getBoolean(SyncScheduler.KEY_RESUME, false)

    /**
     * Warum Android den Lauf gestoppt hat (für den Verlauf). null = nicht von Android gestoppt bzw.
     * "Abbrechen" durch den Benutzer – das gilt als echter Abbruch.
     */
    private fun interruptReason(): Msg? {
        if (!isStopped) return null
        return when (val r = stopReason) {
            WorkInfo.STOP_REASON_CANCELLED_BY_APP -> null
            WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> Msg.of(R.string.stop_wifi)
            WorkInfo.STOP_REASON_CONSTRAINT_CHARGING -> Msg.of(R.string.stop_charger)
            WorkInfo.STOP_REASON_TIMEOUT -> Msg.of(R.string.stop_timeout)
            WorkInfo.STOP_REASON_QUOTA, WorkInfo.STOP_REASON_APP_STANDBY,
            WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION, WorkInfo.STOP_REASON_DEVICE_STATE,
            WorkInfo.STOP_REASON_SYSTEM_PROCESSING -> Msg.of(R.string.stop_system, r)
            else -> Msg.of(R.string.stop_android, r)
        }
    }

    /**
     * Sync wurde durch WLAN-Verlust unterbrochen: Fortsetzen auf zwei Wegen sicherstellen
     *  1. WLAN-Broadcast (WifiNetworkReceiver, ohne Drosselung)
     *  2. Auftrag beim Android-Aufgabenplaner (startet die App auch, wenn sie beendet wurde)
     */
    private fun markInterrupted() {
        val ctx = applicationContext
        GlobalSettings(ctx).resumeOnWifi = true
        SyncScheduler.enqueueResume(ctx, 0, appendToCurrent = isResumeRun)
        // "Pausiert" in der Statusleiste – als Vordergrunddienst, der die App bis zum WLAN wach hält
        if (!PauseService.start(ctx, lastProgress, targetWifi)) Notifications.showPaused(ctx, lastProgress, targetWifi)
    }

    /** Letzter Fortschritt (für die Pausiert-Anzeige) */
    @Volatile private var lastProgress: SyncProgress? = null
    /** Namen der Ziel-WLANs der betroffenen Profile */
    @Volatile private var targetWifi: String = ""

    private suspend fun work(): Result {
        val ctx = applicationContext
        val startedAt = System.currentTimeMillis()
        val manual = inputData.getBoolean(SyncScheduler.KEY_MANUAL, false)
        val trigger = SyncScheduler.triggerOf(inputData, Msg.of(R.string.trigger_auto))
        val dao = AppDatabase.get(ctx).dao()
        dao.closeDanglingRuns()

        val settings = GlobalSettings(ctx)
        // Vom Benutzer pausiert: keine automatischen Läufe (ein manueller Start hebt die Pause vorher auf)
        if (!manual && settings.isPaused()) {
            settings.noteSkip(
                if (settings.manualOnly) str(R.string.skip_manual_only)
                else str(R.string.skip_paused_until, java.text.SimpleDateFormat(str(R.string.fmt_day_month) + " HH:mm", Texts.locale).format(java.util.Date(settings.pausedUntil)))
            )
            return Result.success()
        }
        // Neue Profile laufen erst automatisch, nachdem sie einmal von Hand gestartet wurden (Simulation zuerst möglich)
        val awaiting = settings.awaitingFirstStart()
        // Schutzbremse (0.4.1): Profile mit offener Entscheidung ruhen – auch bei "Synchronisieren"
        val blocked = com.sync2pc.app.mirror.GuardStore(ctx).blockedIds()
        val all = dao.profiles().filter {
            it.enabled && it.isConfigured() && (manual || it.id !in awaiting) && it.id !in blocked &&
                // 0.11.0: Profil wird gerade gelöscht (Rückgängig noch möglich) -> nicht synchronisieren
                it.id != com.sync2pc.app.ui.PendingProfileDelete.pending.value?.id
        }
        if (all.isEmpty()) return Result.success()
        targetWifi = all.flatMap { it.ssids() }.distinct().joinToString(", ")

        // --- Ziel-WLAN? Direkt nach dem Verbinden liefert Android den Namen evtl. noch nicht -> kurz nachlesen
        var ssid = WifiHelper.currentSsid(ctx)
        if (!manual && ssid == null && WifiHelper.isOnWifi(ctx)) {
            for (i in 1..3) {
                delay(5_000)
                ssid = WifiHelper.currentSsid(ctx)
                if (ssid != null) break
            }
        }
        val currentSsid = ssid
        // 0.11.5 (DEC-126): über mobile Daten/VPN (Schalter an) gibt es keinen WLAN-Namen -> alle aktiven Profile
        val eligible = if (manual || WifiHelper.offWifiSyncActive(ctx)) all else all.filter { p -> currentSsid != null && p.ssids().contains(currentSsid) }
        if (eligible.isEmpty()) {
            if (!manual && WifiHelper.isOnWifi(ctx)) settings.noteSkip(
                if (currentSsid == null) str(R.string.skip_ssid_unreadable)
                else str(R.string.skip_ssid_unknown, currentSsid)
            )
            // Fortsetzen-Auftrag, aber falsches WLAN: in 15 Minuten erneut schauen (max. 24 Stunden lang)
            if (isResumeRun && settings.resumeOnWifi) {
                if (System.currentTimeMillis() - settings.resumeSince < 24 * 3600_000L)
                    SyncScheduler.enqueueResume(ctx, 15, appendToCurrent = true)
                else { settings.resumeOnWifi = false; PauseService.stop(ctx) } // nach 24 h aufgeben
            }
            return Result.success()
        }

        // --- Ist der PC erreichbar? (kurzer TCP-Test, bevor irgendetwas Teures passiert) -----
        val isRetry = inputData.getBoolean(SyncScheduler.KEY_RETRY, false)
        if (!manual && !isRetry && settings.shareRetryAt > System.currentTimeMillis()) {
            // PC war vor Kurzem nicht erreichbar -> Wartezeit einhalten, die erneute Prüfung ist eingeplant
            return Result.success()
        }
        // Direkt nach dem WLAN-Verbinden ist das Netz oft noch nicht bereit (IP, Routing, PC-Firewall).
        // Deshalb mehrere Versuche über ca. 1 Minute, bevor "nicht erreichbar" gilt.
        // Nur mobile Daten/VPN (0.11.7): höchstens 2 Versuche (Akku, Datenvolumen)
        val waits = NetworkRules.reachabilityWaits(manual, WifiHelper.offWifiSyncActive(ctx))
        val reach = HashMap<String, Boolean>()
        val unreachable = ArrayList<Profile>()
        val runnable = ArrayList<Profile>()
        for (p in eligible) {
            // Akku (0.3.4): Bildordner ohne übertragungsbereite Dateien brauchen den PC nicht – kein
            // Erreichbarkeitstest, keine Verbindung. SyncEngine meldet dann ohne Netzwerk "Nichts zu tun"
            // (und plant für zu junge Fotos den Nachhol-Sync). Ordner-Profile brauchen den PC immer.
            if (p.type() == ProfileType.PHOTOS &&
                PendingCheck.readyFiles(ctx, p, settings.minFileAgeMinutes) == 0
            ) { runnable += p; continue }
            val host = SmbTarget.parse(p.smbPath)?.host
            if (host == null) { runnable += p; continue } // ungültiger Pfad -> Engine meldet den Fehler
            val ok = reach[host.lowercase()] ?: run {
                var r = false
                for ((n, w) in waits.withIndex()) {
                    if (w > 0) {
                        if (!manual && !WifiHelper.onAllowedNetwork(ctx)) break // Netz wieder weg -> aufgeben
                        Notifications.updateProgress(
                            ctx, SyncProgress(p.name, str(R.string.wait_for_pc, host, n + 1, waits.size), 0, 0, 0, 0, null), null
                        )
                        delay(w)
                    }
                    if (SmbSession.isReachable(host)) { r = true; break }
                }
                Notifications.cancelProgress(ctx)
                reach[host.lowercase()] = r
                r
            }
            if (ok) runnable += p else unreachable += p
        }
        if (unreachable.isNotEmpty()) settings.noteSkip(str(R.string.skip_pc_unreachable, unreachable.joinToString(", ") { it.name }))
        if (runnable.isEmpty()) {
            handleUnreachable(unreachable, manual, isRetry)
            return Result.success()
        }
        // Ein unterbrochener Sync wird jetzt fortgesetzt -> Merker, Hinweis und Fortsetzen-Auftrag aufräumen
        if (settings.resumeOnWifi) {
            settings.resumeOnWifi = false
            if (!isResumeRun) WorkManager.getInstance(ctx).cancelUniqueWork(SyncScheduler.WORK_RESUME)
        }
        PauseService.stop(ctx) // "Pausiert" weg, ab jetzt normale Fortschrittsanzeige
        Notifications.cancelInterrupted(ctx)
        if (unreachable.isEmpty()) settings.noteSkip("")

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
        var conflicts = 0
        var spaceShortage = false
        var interruptedMidRun = false
        // WLAN während der Übertragung wach halten (sonst schaltet Android es im Standby in den Stromsparmodus)
        val locks = TransferLocks.acquire(ctx)
        try {
            for (p in runnable) {
                val shouldContinue: () -> Boolean = {
                    manual || NetworkRules.mayContinue(
                        WifiHelper.currentSsid(ctx), p.ssids(), WifiHelper.onAllowedNetwork(ctx)
                    )
                }
                val onProgress: (SyncProgress) -> Unit = progress@{ pr ->
                    if (isStopped) return@progress // nach dem Stopp keine Anzeige mehr neu erzeugen
                    if (pr.totalFiles > 0) lastProgress = pr
                    Notifications.updateProgress(ctx, pr, cancel)
                    setProgressAsync(
                        workDataOf(
                            P_PROFILE to pr.profileName, P_PHASE to pr.phase, P_DONE to pr.doneFiles,
                            P_TOTAL to pr.totalFiles, P_DONE_BYTES to pr.doneBytes,
                            P_TOTAL_BYTES to pr.totalBytes, P_FILE to pr.currentFile
                        )
                    )
                }
                // Bildordner -> sortierte Sicherung, normaler Ordner -> Spiegelung
                val engine: SyncRunner = if (p.type() == ProfileType.MIRROR)
                    MirrorEngine(ctx, p, trigger, shouldContinue = shouldContinue, onProgress = onProgress,
                        interruptReason = ::interruptReason)
                else
                    SyncEngine(ctx, p, trigger, shouldContinue = shouldContinue, onProgress = onProgress,
                        interruptReason = ::interruptReason)
                val r = engine.run()
                if (engine.blocked) {
                    Notifications.guardNeeded(ctx, p.name)
                    continue
                }
                // Erster manueller Start eines neuen Profils -> ab jetzt auch automatisch
                if (manual && p.id in awaiting && r.status != RunStatus.FAILED) settings.setAwaitingFirstStart(p.id, false)
                uploaded += r.uploaded; failed += r.failed; bytes += r.bytes
                if (p.type() == ProfileType.PHOTOS) waiting += r.waiting
                conflicts += engine.newConflicts
                if (engine.spaceShortage) spaceShortage = true
                if (engine.wifiLost && !manual) markInterrupted()
                if (engine.connectionFailed) { unreachable += p; interruptedMidRun = true }
                else if (r.status == RunStatus.FAILED) hardFailure = "${p.name}: ${r.messageText()}"
                if (engine.nextYoungFileAt > 0 && (youngAt == 0L || engine.nextYoungFileAt < youngAt)) youngAt = engine.nextYoungFileAt
            }
        } finally {
            locks.release()
            Notifications.cancelProgress(ctx)
        }

        // --- Abschluss-Benachrichtigungen ----------------------------------------
        // Verbindungsabbrüche sind erwartbar: keine Meldung, der Rest folgt automatisch
        if (hardFailure != null) {
            Notifications.summary(ctx, str(R.string.summary_failed, hardFailure), error = true)
        } else if (interruptedMidRun) {
            // still – Anzeige "Pausiert" bzw. erneuter Versuch übernimmt
        } else if (uploaded > 0 || failed > 0) {
            Notifications.summary(
                ctx,
                plural(R.plurals.summary_transferred, uploaded, uploaded, Notifications.formatBytes(bytes)) +
                    (if (failed > 0) ", " + plural(R.plurals.summary_errors, failed) else ""),
                error = failed > 0
            )
        }
        if (waiting > 0) {
            val groups = dao.decisions().size
            Notifications.decisionNeeded(ctx, groups, waiting)
        }
        if (conflicts > 0) Notifications.conflictsNeeded(ctx, dao.openConflictCount())
        if (spaceShortage) Notifications.summary(ctx, str(R.string.summary_no_space), error = true)
        val toDelete = dao.queuedDeletes().map { it.sourceKey }.distinct().size
        if (toDelete > 0) Notifications.deleteReady(ctx, toDelete)
        // Während des Laufs neu entstandene Fotos/Videos -> nach Mindestalter nachholen
        val minAgeMs = GlobalSettings(ctx).minFileAgeMinutes * 60_000L
        if (MediaScanner.hasAddedSince(ctx, startedAt / 1000)) {
            val at = System.currentTimeMillis() + minAgeMs
            if (youngAt == 0L || at < youngAt) youngAt = at
        }
        if (youngAt > 0) SyncScheduler.enqueueFollowUp(ctx, youngAt)
        // Erfolgreich gesichert -> eine offene Erinnerung "lange nicht gesichert" ist erledigt
        if (hardFailure == null && !interruptedMidRun && unreachable.isEmpty()) {
            Notifications.cancelStale(ctx)
            GlobalSettings(ctx).lastStaleWarnAt = 0
        }

        handleUnreachable(unreachable, manual, isRetry, midRun = interruptedMidRun)
        return Result.success()
    }

    /**
     * Nicht erreichbare Shares: automatisch -> in 30 Minuten erneut prüfen (solange im WLAN),
     * manuell -> Hinweis. Alles erreichbar -> Wartezustand aufheben.
     */
    private fun handleUnreachable(unreachable: List<Profile>, manual: Boolean, isRetry: Boolean, midRun: Boolean = false) {
        val ctx = applicationContext
        val s = GlobalSettings(ctx)
        if (unreachable.isEmpty()) {
            s.shareRetryAt = 0; s.shareUnreachable = ""
            // Die laufende Wiederholung nicht selbst abbrechen
            if (!isRetry) WorkManager.getInstance(ctx).cancelUniqueWork(SyncScheduler.WORK_RETRY)
            return
        }
        val names = unreachable.joinToString(", ") { it.name }
        // Abbruch mitten in der Übertragung behandeln wie automatisch (auch bei manuellem Start): still fortsetzen
        if (manual && !midRun) {
            Notifications.summary(ctx, str(R.string.summary_pc_unreachable, names), error = true)
            return
        }
        // Noch im Ziel-WLAN? Dann ist der PC weg -> in 30 Minuten erneut prüfen.
        // WLAN selbst verloren -> kein Timer, sondern sofort beim nächsten Verbinden (WifiNetworkReceiver).
        val ssid = WifiHelper.currentSsid(ctx)
        val stillOnTargetWifi = if (ssid != null) unreachable.any { it.ssids().contains(ssid) } else WifiHelper.onAllowedNetwork(ctx)
        if (stillOnTargetWifi) {
            // Im WLAN: Abbruch mitten in der Übertragung = meist kurzer Aussetzer -> bald erneut (2 min).
            // Nur mobile Daten/VPN (0.11.7): immer 2 Stunden. Ein erfolgreicher Lauf (auch von Hand) hebt die
            // Wartezeit auf (oben: unreachable.isEmpty()); der manuelle Start ignoriert sie ohnehin.
            val minutes = NetworkRules.retryMinutes(WifiHelper.offWifiSyncActive(ctx), midRun)
            s.shareRetryAt = System.currentTimeMillis() + minutes * 60_000
            s.shareUnreachable = names
            SyncScheduler.enqueueShareRetry(ctx, minutes)
            if (midRun) Notifications.showPaused(ctx, lastProgress, targetWifi, str(R.string.paused_connection_retry, minutes))
        } else {
            s.shareRetryAt = 0; s.shareUnreachable = ""
            if (!isRetry) WorkManager.getInstance(ctx).cancelUniqueWork(SyncScheduler.WORK_RETRY)
            markInterrupted()
        }
    }
}
