package com.sync2pc.app.sync

import com.sync2pc.app.data.RunStatus

import android.annotation.SuppressLint
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
import com.sync2pc.app.smb.PcSeen
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.smb.SmbTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Führt den Sync für alle passenden Profile aus (läuft im Hintergrund, ggf. als Vordergrunddienst). */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        // Verhinderung paralleler Läufe (Auto, Manuell, Nachholen): SyncScheduler.gate (SyncGate), siehe doWork

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
        val ctx = applicationContext
        val gate = SyncScheduler.gate(ctx)
        // R-N1: Bekommt dieser Auftrag die Sperre nicht, ist ein Lauf aktiv. Der Start wird dann nicht mehr stumm verworfen,
        // sondern atomar vorgemerkt (Merker im Speicher und in GlobalSettings); der Lauf-Inhaber reiht nach seiner Freigabe
        // genau einen Nachlauf ein. Die Oberfläche sieht es an deferred = true bzw. an SyncScheduler.deferredState.
        // SYNC-20261111-172 (6): Sonderaufträge (Fortsetzen, Wiederholung, Folgelauf) wandeln sich nicht in einen
        // Nachlauf-Merker um (der hätte weder KEY_RESUME noch KEY_RETRY): bei belegter Sperre bleibt der Auftrag mit seinen
        // Eingabedaten bei WorkManager und versucht es nach dem Backoff erneut.
        if (SyncScheduler.isSpecialRun(inputData)) {
            if (!gate.tryAcquire()) return@withContext Result.retry()
        } else if (!gate.tryAcquireOrDefer(manual = inputData.getBoolean(SyncScheduler.KEY_MANUAL, false)))
            return@withContext Result.success(workDataOf(SyncScheduler.KEY_DEFERRED to true))
        // Zwischen tryAcquireOrDefer und try darf nichts stehen: die Sperre muss in jedem Fall wieder frei werden (R-BG4)
        // -157: Scheitert work() UND der Abschluss, bleibt die Ursache aus work() erhalten (die zweite Ausnahme wird nur
        // angehängt); ein Ausdruck try/finally würde sie überschreiben.
        var workFailure: Throwable? = null
        val workResult: Result? = try { work() } catch (t: Throwable) { workFailure = t; null }
        try {
            try {
                // finishRun: Aufräumen darf scheitern, die Sperre wird trotzdem freigegeben und ein vorgemerkter Nachlauf
                // eingereiht. Freigabe NACH markInterrupted (Zustand "Pausiert" gehört noch zu diesem Lauf); ein sofort
                // startender Fortsetzen-Auftrag (R-BG5) wird nicht verworfen, sondern vorgemerkt und nachgeholt.
                gate.finishRun(
                    cleanup = {
                        // Fortschrittsanzeige nie stehen lassen (sie würde sonst nach einem Abbruch "hängen")
                        try { Notifications.cancelProgress(ctx) } catch (ignored: Exception) {}
                        // Von Android gestoppt (z. B. WLAN weg -> Netzwerk-Bedingung verletzt): beim Wiederverbinden fortsetzen
                        // ("Abbrechen" durch den Benutzer ist kein Unterbruch -> nicht fortsetzen)
                        if (SyncDecisions.resumeAfterStop(isStopped, stopReasonCompat(), inputData.getBoolean(SyncScheduler.KEY_MANUAL, false)))
                            markInterrupted()
                    },
                    followUp = { kind -> SyncScheduler.enqueueDeferred(ctx, kind) }
                )
            } finally {
                try {
                    // Nur scharf stellen, wenn keiner wartet (KEEP): ein bereits wartender Auslöser behält seine Frist und wird
                    // von einem Lauf ohne neue Dateien nicht erneut um Mindestalter + 30 s nach hinten geschoben.
                    // Neue Medien verlängern die Frist von selbst (Update-Delay des Content-Triggers).
                    SyncScheduler.armContentTrigger(ctx, replace = false)
                } finally {
                    // Sicherheitsnetz: 6 Stunden nach diesem Lauf erneut prüfen (schiebt den Zeitpunkt nach hinten)
                    SyncScheduler.scheduleSafetyNet(ctx, restart = true)
                }
            }
        } catch (t: Throwable) {
            throw SyncDecisions.mergeFailures(workFailure, t)
        }
        workFailure?.let { throw it }
        workResult!!
    }

    private val isResumeRun: Boolean get() = inputData.getBoolean(SyncScheduler.KEY_RESUME, false)

    /**
     * Stoppgrund (SYNC-20261109-063): `getStopReason()` ist in WorkManager 2.10 mit @RequiresApi(31) markiert, liest
     * aber nur einen intern gesetzten Wert (kein Framework-Aufruf, daher auch unter API 29/30 ohne Absturz).
     * "Abbrechen" durch den Benutzer meldet WorkManager selbst als STOP_REASON_CANCELLED_BY_APP (auf allen API-Stufen);
     * Systemstopps liefern unter API < 31 STOP_REASON_UNKNOWN. Bewusst KEINE SDK_INT-Abfrage: sie würde unter API < 31
     * einen Benutzerabbruch als Unterbruch werten (ungewolltes Fortsetzen).
     */
    @SuppressLint("NewApi")
    private fun stopReasonCompat(): Int = stopReason

    /**
     * Warum Android den Lauf gestoppt hat (für den Verlauf). null = nicht von Android gestoppt bzw.
     * "Abbrechen" durch den Benutzer – das gilt als echter Abbruch.
     */
    private fun interruptReason(): Msg? {
        val t = SyncDecisions.interruptText(isStopped, stopReasonCompat()) ?: return null
        return if (t.arg != null) Msg.of(t.res, t.arg) else Msg.of(t.res)
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
                else str(R.string.skip_paused_until, com.sync2pc.app.l10n.TimeFormat.dayMonth(settings.pausedUntil) + " " + com.sync2pc.app.l10n.TimeFormat.clock(settings.pausedUntil))
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
        // Nur warten, wenn ein Profil den Namen braucht (PC-Erkennung GUID kommt ohne aus)
        if (!manual && ssid == null && WifiHelper.isOnWifi(ctx) && all.any { NetworkRules.needsWifiName(it.pcMode()) }) {
            for (i in 1..3) {
                delay(5_000)
                ssid = WifiHelper.currentSsid(ctx)
                if (ssid != null) break
            }
        }
        val currentSsid = ssid
        // 0.11.5 (DEC-126): über mobile Daten/VPN (Schalter an) gibt es keinen WLAN-Namen -> alle aktiven Profile
        // Je PC-Erkennung (Stufe 2): GUID = jede erlaubte Netzart, WIFI/BOTH = Name in der Liste
        val offWifi = WifiHelper.offWifiSyncActive(ctx)
        val onAllowed = WifiHelper.onAllowedNetwork(ctx)
        val eligible = if (manual) all else all.filter { p ->
            NetworkRules.eligible(p.pcMode(), currentSsid, p.ssids(), onAllowed, offWifi)
        }
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
        val rejected = ArrayList<PcRejection>()
        // Vorab-Prüfung der Kennung je Host und Lauf nur einmal aushandeln (F5, nur im Speicher); entschieden wird je Profil
        val pcSeen = PcSeen()
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
            if (!ok) { unreachable += p; continue }
            // PC-Erkennung GUID/BOTH: Kennung VOR jeder Anmeldung prüfen (nur Aushandeln, keine Zugangsdaten).
            // WIFI: keine Prüfung, wie bisher (die Prüfung in SmbSession.open lernt dort still, ohne Vertrauen zu erzeugen).
            if (!p.pcMode().checksGuid) { runnable += p; continue }
            when (val pre = precheckPc(p, manual, pcSeen)) {
                PcPre.Run -> runnable += p
                PcPre.Unreachable -> unreachable += p
                is PcPre.Rejected -> rejected += PcRejection(p, pre.decision, host)
                // F3: Fehler der Aushandlung (nur SMB1, unerwarteter Status, Kennung nicht lesbar) wie «abgewiesen»:
                // eine Meldung, kein Timer, keine Dauerbenachrichtigung, kein stilles Überspringen
                is PcPre.Failed -> rejected += PcRejection(p, null, host, pre.text)
            }
        }
        // PC erkannt/nicht mehr abgewiesen -> Benachrichtigungsmerker löschen (neue Meldung bei erneutem Wechsel)
        for (p in runnable) if (p.pcMode().checksGuid) settings.setPcNotified(p.id, false)
        if (unreachable.isNotEmpty()) settings.noteSkip(str(R.string.skip_pc_unreachable, unreachable.joinToString(", ") { it.name }), manual)
        if (rejected.isNotEmpty()) {
            // -157: Aushandlungsfehler (z. B. Verschlüsselung erforderlich, aber nicht verfügbar) sind keine «nicht erkannten» PCs
            val failedNeg = rejected.filter { it.decision == null }
            val notRecognised = rejected.filter { it.decision != null }
            settings.noteSkip(listOfNotNull(
                notRecognised.takeIf { it.isNotEmpty() }?.let { str(R.string.skip_pc_not_recognised, it.joinToString(", ") { r -> r.profile.name }) },
                failedNeg.takeIf { it.isNotEmpty() }?.let { str(R.string.skip_pc_failed, it.joinToString(", ") { r -> r.profile.name }) },
            ).joinToString(" · "), manual)
            if (manual) {
                Notifications.summary(ctx, rejected.joinToString("\n") { "${it.profile.name}: ${it.message()}" }, error = true)
            } else for (r in rejected) if (!settings.pcNotified(r.profile.id)) {
                settings.setPcNotified(r.profile.id, true)
                Notifications.summary(ctx, "${r.profile.name}: ${r.message()}", error = true)
            }
        }
        if (runnable.isEmpty()) {
            // F2: abgewiesene Profile setzen keinen Timer (Port 445 antwortet, die Abweisung ändert nur der Benutzer); nicht erreichbare schon
            handleUnreachable(unreachable, manual, isRetry)
            // Fortsetzen-Auftrag: nichts mehr fortzusetzen, wenn alle abgewiesen wurden (Anzeige «Pausiert» nicht stehen lassen)
            if (unreachable.isEmpty() && isResumeRun && settings.resumeOnWifi) { settings.resumeOnWifi = false; PauseService.stop(ctx) }
            return Result.success()
        }
        // Ein unterbrochener Sync wird jetzt fortgesetzt -> Merker, Hinweis und Fortsetzen-Auftrag aufräumen
        if (settings.resumeOnWifi) {
            settings.resumeOnWifi = false
            if (!isResumeRun) WorkManager.getInstance(ctx).cancelUniqueWork(SyncScheduler.WORK_RESUME)
        }
        PauseService.stop(ctx) // "Pausiert" weg, ab jetzt normale Fortschrittsanzeige
        Notifications.cancelInterrupted(ctx)
        if (unreachable.isEmpty() && rejected.isEmpty()) settings.noteSkip("")

        val cancel = WorkManager.getInstance(ctx).createCancelPendingIntent(id)
        try {
            setForeground(Notifications.foregroundInfo(ctx, null, cancel))
        } catch (e: Exception) {
            // Android 12+: Start aus dem Hintergrund evtl. nicht erlaubt -> normale Benachrichtigung
            Notifications.updateProgress(ctx, null, cancel)
        }

        var uploaded = 0; var failed = 0; var waiting = 0; var bytes = 0L
        var hardFailure: String? = null
        // Mitten im Lauf verlorene Verbindung (DEC-048): nur diese setzen nach einem Lauf einen Wiederholungs-Timer
        val lostMidRun = ArrayList<Profile>()
        var youngAt = 0L
        var conflicts = 0
        var spaceShortage = false
        var interruptedMidRun = false
        var deferredFiles = 0
        var folderChanged = false // SYNC-20261111-213: Handy-Ordner während des Laufs verändert
        // WLAN während der Übertragung wach halten (sonst schaltet Android es im Standby in den Stromsparmodus)
        val locks = TransferLocks.acquire(ctx)
        try {
            for (p in runnable) {
                val shouldContinue: () -> Boolean = {
                    manual || NetworkRules.mayContinue(
                        p.pcMode(), WifiHelper.currentSsid(ctx), p.ssids(), WifiHelper.onAllowedNetwork(ctx)
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
                uploaded += r.uploaded; failed += r.failed; bytes += r.bytes; deferredFiles += r.deferred
                if (engine.changedDuringRun) folderChanged = true
                if (p.type() == ProfileType.PHOTOS) waiting += r.waiting
                conflicts += engine.newConflicts
                if (engine.spaceShortage) spaceShortage = true
                if (engine.wifiLost && !manual) markInterrupted()
                if (engine.connectionFailed) { unreachable += p; lostMidRun += p; interruptedMidRun = true }
                else if (engine.encryptionUnavailable) {
                    // «Verschlüsselung nicht verfügbar»: ändert sich nur durch den Benutzer. Automatische Läufe melden es je Profil
                    // einmal, bis ein Lauf des Profils gelingt; ein manueller Start meldet immer. Der Verlauf bleibt FAILED.
                    if (manual || !settings.encNotified(p.id)) hardFailure = "${p.name}: ${r.messageText()}"
                    settings.setEncNotified(p.id, true)
                }
                else if (r.status == RunStatus.FAILED) hardFailure = "${p.name}: ${r.messageText()}"
                if (!engine.connectionFailed && !engine.encryptionUnavailable && r.status != RunStatus.FAILED)
                    settings.setEncNotified(p.id, false)
                if (engine.nextYoungFileAt > 0 && (youngAt == 0L || engine.nextYoungFileAt < youngAt)) youngAt = engine.nextYoungFileAt
            }
        } finally {
            locks.release()
            Notifications.cancelProgress(ctx)
        }

        // --- Abschluss-Benachrichtigungen ----------------------------------------
        // Verbindungsabbrüche sind erwartbar: keine Meldung, der Rest folgt automatisch
        when (SyncDecisions.summaryKind(hardFailure != null, interruptedMidRun, uploaded, failed)) {
            SyncDecisions.SummaryKind.FAILED ->
                Notifications.summary(ctx, str(R.string.summary_failed, hardFailure!!), error = true)
            // NONE bei Unterbruch mitten im Lauf: still – Anzeige "Pausiert" bzw. erneuter Versuch übernimmt
            SyncDecisions.SummaryKind.NONE -> {}
            SyncDecisions.SummaryKind.TRANSFERRED -> Notifications.summary(
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
        // SYNC-20261111-173: Dateien, die sich während des Laufs änderten, folgen per automatischem Folgelauf (höchstens einer
        // pro Stunde). Pause/«nur manuell» gelten wie bei jedem automatischen Lauf (nicht planen, kein Stundenkontingent verbrauchen).
        val now = System.currentTimeMillis()
        if (!interruptedMidRun && !settings.isPaused() && SkipFollowUp.mayPlan(deferredFiles, now, settings.lastSkipFollowUpAt, folderChanged)) {
            SyncScheduler.enqueueSkipFollowUp(ctx)
            settings.lastSkipFollowUpAt = now // erst nach dem Einreihen: scheitert es, bleibt das Stundenkontingent erhalten
        }
        // Erfolgreich gesichert -> eine offene Erinnerung "lange nicht gesichert" ist erledigt
        if (SyncDecisions.staleResolved(hardFailure != null, interruptedMidRun, unreachable.isEmpty(), rejected.isEmpty())) {
            Notifications.cancelStale(ctx)
            GlobalSettings(ctx).lastStaleWarnAt = 0
        }

        // F2: Lief mindestens ein Profil, setzt ein nicht erreichbarer PC keinen Timer (ein erreichbares Profil darf die anderen
        // nicht blockieren und der Akku wird geschont); die übrigen werden beim nächsten ohnehin stattfindenden Lauf neu bewertet.
        // Ein Abbruch mitten im Lauf behält die kurze Wiederholung (DEC-048/DEC-126). Abgewiesene Profile setzen nie einen Timer.
        handleUnreachable(lostMidRun, manual, isRetry, midRun = interruptedMidRun)
        return Result.success()
    }

    /**
     * Ein vom Kennungs-Vorabtest abgewiesenes Profil (Zugangsdaten wurden nicht gesendet). [decision] null = Fehler der Aushandlung
     * mit eigenem Text [text] (F3).
     */
    private class PcRejection(val profile: Profile, val decision: PcDecision?, val host: String, val text: String? = null) {
        fun message(): String = text ?: PcIdentityException(decision ?: PcDecision.UNKNOWN, host).message
    }

    private sealed class PcPre {
        object Run : PcPre()
        object Unreachable : PcPre()
        class Rejected(val decision: PcDecision) : PcPre()
        class Failed(val text: String) : PcPre()
    }

    /**
     * Vorab-Prüfung der PC-Kennung für GUID/BOTH (nur Aushandeln, keine Zugangsdaten, je Host einmal über [seen]). Hier wird nie
     * gelernt (V1): eine unbekannte Kennung ergibt UNKNOWN, auch beim manuellen Start; gelernt wird nur beim Speichern und in
     * «Verbindung testen». Alles ausser ALLOW wird abgewiesen (sicherer Ausfall).
     */
    private fun precheckPc(p: Profile, manual: Boolean, seen: PcSeen): PcPre {
        val access = if (manual) PcAccess.USER else PcAccess.AUTO
        return try {
            when (val d = SmbSession.checkPc(p, access, seen)) {
                PcDecision.ALLOW -> PcPre.Run
                else -> PcPre.Rejected(d)
            }
        } catch (e: PcIdentityException) {
            PcPre.Rejected(e.decision)
        } catch (e: Exception) {
            if (isNetworkError(e)) PcPre.Unreachable else PcPre.Failed(userError(e))
        }
    }

    /**
     * Nicht erreichbare Shares: automatisch -> in 30 Minuten erneut prüfen (solange im WLAN),
     * manuell -> Hinweis. Alles erreichbar -> Wartezustand aufheben.
     */
    private fun handleUnreachable(
        unreachable: List<Profile>, manual: Boolean, isRetry: Boolean, midRun: Boolean = false,
    ) {
        val ctx = applicationContext
        val s = GlobalSettings(ctx)
        // Abgewiesene Profile (PC nicht sicher erkannt, SYNC-20261109-047/-055) kommen nicht hierher: ohne Timer, sie werden beim
        // nächsten ohnehin stattfindenden Lauf neu bewertet (Port 445 antwortet, die Abweisung ändert nur der Benutzer).
        val waiting = unreachable
        val names = unreachable.joinToString(", ") { it.name }
        // Noch im Ziel-WLAN? Dann ist der PC weg -> in 30 Minuten erneut prüfen.
        // WLAN selbst verloren -> kein Timer, sondern sofort beim nächsten Verbinden (WifiNetworkReceiver).
        // Die Entscheidung liegt in SyncDecisions.unreachableAction (SYNC-20261111-207, B4, testbar).
        val action = SyncDecisions.unreachableAction(waiting.isNotEmpty(), manual, midRun) {
            val ssid = WifiHelper.currentSsid(ctx)
            val onAllowed = WifiHelper.onAllowedNetwork(ctx)
            waiting.any { NetworkRules.mayContinue(it.pcMode(), ssid, it.ssids(), onAllowed) }
        }
        when (action) {
            SyncDecisions.UnreachableAction.CLEAR_WAIT -> {
                s.shareRetryAt = 0; s.shareUnreachable = ""
                // Die laufende Wiederholung nicht selbst abbrechen
                if (!isRetry) WorkManager.getInstance(ctx).cancelUniqueWork(SyncScheduler.WORK_RETRY)
            }
            // Abbruch mitten in der Übertragung behandeln wie automatisch (auch bei manuellem Start): still fortsetzen
            SyncDecisions.UnreachableAction.NOTIFY_ONLY ->
                Notifications.summary(ctx, str(R.string.summary_pc_unreachable, names), error = true)
            SyncDecisions.UnreachableAction.RETRY_TIMER -> {
                // Im WLAN: Abbruch mitten in der Übertragung = meist kurzer Aussetzer -> bald erneut (2 min).
                // Nur mobile Daten/VPN (0.11.7): immer 2 Stunden. Ein erfolgreicher Lauf (auch von Hand) hebt die
                // Wartezeit auf (CLEAR_WAIT); der manuelle Start ignoriert sie ohnehin.
                val minutes = NetworkRules.retryMinutes(WifiHelper.offWifiSyncActive(ctx), midRun)
                s.shareRetryAt = System.currentTimeMillis() + minutes * 60_000
                s.shareUnreachable = names
                SyncScheduler.enqueueShareRetry(ctx, minutes)
                if (midRun) Notifications.showPaused(ctx, lastProgress, targetWifi, str(R.string.paused_connection_retry, minutes))
            }
            SyncDecisions.UnreachableAction.RESUME_ON_WIFI -> {
                s.shareRetryAt = 0; s.shareUnreachable = ""
                if (!isRetry) WorkManager.getInstance(ctx).cancelUniqueWork(SyncScheduler.WORK_RETRY)
                markInterrupted()
            }
        }
    }
}
