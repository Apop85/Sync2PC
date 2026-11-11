package com.sync2pc.app.mirror

import com.sync2pc.app.data.RunStatus

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import com.sync2pc.app.R
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.data.ConflictMode
import com.sync2pc.app.data.ConflictResolution
import com.sync2pc.app.data.LogEntry
import com.sync2pc.app.data.MirrorConflict
import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.SyncDirection
import com.sync2pc.app.data.SyncRun
import com.sync2pc.app.smb.RemoteEntry
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.sync.PartialHash
import com.sync2pc.app.sync.PreviewEntry
import com.sync2pc.app.sync.SyncProgress
import com.sync2pc.app.sync.SyncRunner
import com.sync2pc.app.data.GlobalSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Art der Aktion je Datei; die Entscheidungstabelle steht in [MirrorPlanner] (SYNC-20261111-163) */
private typealias Kind = PlanKind

/**
 * Ordner-Spiegelung Handy-Ordner ⇄ PC-Ordner (1:1 mit Unterordnern).
 *
 * 3-Wege-Abgleich je Datei: Handy-Stand (L), PC-Stand (R) und Stand beim letzten Abgleich (S).
 *  - nur auf einer Seite geändert/neu   -> in Sync-Richtung kopieren
 *  - auf beiden Seiten geändert         -> gleich? merken : Konflikt (fragen bzw. neuere gewinnt)
 *  - auf einer Seite gelöscht           -> optional übernehmen (Papierkorb der Gegenseite), sonst wiederherstellen
 * Ersetzte oder gelöschte Dateien landen immer in ".LeoSync-Papierkorb/<Datum>/…". Endgültig gelöscht
 * werden sie erst nach Ablauf der Papierkorb-Frist des Profils (trashRetentionDays, 0 = nie).
 */
class MirrorEngine(
    private val context: Context,
    private val profile: Profile,
    private val trigger: Msg,
    private val dryRun: Boolean = false,
    private val shouldContinue: () -> Boolean = { true },
    private val onProgress: (SyncProgress) -> Unit = {},
    /** Bei einem Stopp: Grund, wenn Android unterbrochen hat (z. B. "WLAN getrennt"); null = vom Benutzer abgebrochen */
    private val interruptReason: () -> Msg? = { null },
    /** Simulation (dryRun): höchstens so viele Einträge anzeigen */
    private val previewLimit: Int = Int.MAX_VALUE,
) : SyncRunner {

    companion object {
        const val TRASH_DIR = ".LeoSync-Bin"
        /** Name des Papierkorbs von 0.0.12 bis 0.7.x – wird weiter erkannt (Liste, Wiederherstellen, Leeren) */
        const val LEGACY_TRASH_DIR = ".LeoSync-Papierkorb"
        /** Papierkorb-Name bis 0.0.11 – wird weiterhin nie synchronisiert */
        const val OLD_TRASH_DIR = ".Sync2PC-Papierkorb"
        /** Alle Papierkorb-Namen: der aktuelle zuerst (dorthin wird neu verschoben) */
        val TRASH_DIRS = listOf(TRASH_DIR, LEGACY_TRASH_DIR, OLD_TRASH_DIR)
        /**
         * Handy-Papierkorb in Ordnern des Download-Anbieters mit Nummern-Kennung (msd:, Android 10): MediaStore indexiert Ordner
         * mit Punkt am Namensanfang nicht, sie fehlen in der Ordnerliste (SYNC-20261111-229). Dort heisst der Papierkorb ohne
         * Punkt. Nur auf dem Handy; der PC-Papierkorb heisst weiter [TRASH_DIR] (darum nicht in [TRASH_DIRS]).
         */
        const val MEDIASTORE_TRASH_DIR = "@LeoSync-Bin"
        fun isTrashDir(name: String): Boolean =
            TRASH_DIRS.any { it.equals(name, ignoreCase = true) } || MEDIASTORE_TRASH_DIR.equals(name, ignoreCase = true)
        /** Zusatz beim Behalten beider Versionen (bis 0.7.x "_Konflikt-Handy") */
        const val CONFLICT_SUFFIX = ConflictName.SUFFIX
        /** Mindestens so viel Speicher frei lassen */
        private const val RESERVE_BYTES = 300L * 1024 * 1024
    }

    override val preview = ArrayList<PreviewEntry>()
    // @Volatile: werden seit 0.3.0 von parallelen Übertragungen gesetzt
    @Volatile override var connectionFailed = false
        private set
    @Volatile override var wifiLost = false
        private set
    override var newConflicts = 0
        private set
    @Volatile override var spaceShortage = false
        private set
    override var blocked = false
        private set
    @Volatile override var encryptionUnavailable = false
        private set

    private val dao = AppDatabase.get(context).dao()
    private val guards = GuardStore(context)
    private val dir: SyncDirection = profile.dir()
    /** Löschungen übernehmen? (einmal aus nach "Nicht löschen" bei der Schutzbremse) */
    private var propagate = profile.propagateDeletes
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())


    /** key = relativer Pfad in Kleinbuchstaben, rel = Originalschreibweise */
    private data class Action(val key: String, val rel: String, val kind: Kind, val trashOther: Boolean = false)

    /** Erledigter Konflikt; [expected] = die Entscheidung, die der Plan ausführt (null = Seite fehlt, unbedingt löschen) */
    private data class DoneConflict(val key: String, val relPath: String, val expected: String?)

    private lateinit var tree: LocalTree
    private var smb: SmbSession? = null
    private var local: Map<String, Pair<String, LocalEntry>> = emptyMap()
    private var remote: Map<String, Pair<String, RemoteEntry>> = emptyMap()
    /** Ein Inhaltsvergleich wurde abgebrochen (WLAN weg): Der Lauf endet ohne etwas auszuführen (R-I3) */
    private var hashAborted = false
    private var lastCompareEmit = 0L

    private var run = SyncRun(
        profileId = profile.id, profileName = profile.name, trigger = trigger.text(), triggerCode = trigger.toCode(),
        dryRun = dryRun, startedAt = System.currentTimeMillis()
    )

    /** Log-Zeile als Kennung (DEC-159): Text in der Schreibsprache als Rückfall, Kennung für die Anzeige */
    private suspend fun log(level: String, msg: Msg) {
        if (dryRun) return
        dao.insertLog(LogEntry(runId = run.id, time = System.currentTimeMillis(), level = level,
            message = msg.text(), code = msg.toCode()))
    }

    /** Wörtliche Zeile (Symbole, Dateinamen, Fehlertexte): bleibt wie sie ist, ohne Kennung */
    private suspend fun log(level: String, text: String) = log(level, Msg.lit(text))

    // Zähler (threadsicher, parallele Übertragungen)
    private val up = AtomicInteger(); private val down = AtomicInteger(); private val failed = AtomicInteger()
    private val bytes = AtomicLong(); private val trashed = AtomicInteger()
    /** Dateien, die während des Laufs geändert wurden und deshalb erst beim nächsten Sync folgen (SYNC-20261111-173) */
    private val deferred = AtomicInteger()
    /** Der Lauf hat selbst auf dem Handy geschrieben (Download, Papierkorb, Umbenennen, Aufräumen): kein Folgelauf wegen Ordneränderung (SYNC-20261111-213) */
    @Volatile private var localTouched = false
    @Volatile override var changedDuringRun = false
        private set

    /** Zwischen den parallelen Übertragungen geteilter Zustand */
    private class Shared(val totalFiles: Int, val totalBytes: Long, budget: Long) {
        val done = AtomicInteger()
        /** Bytes vollständig übertragener Dateien */
        val bytesDone = AtomicLong()
        /** Bytes der gerade laufenden Übertragungen (Schlüssel -> Bytes) */
        val inflight = ConcurrentHashMap<String, Long>()
        val stop = AtomicBoolean(false)
        /** Noch verfügbarer Speicher auf dem Handy für Downloads */
        val budget = AtomicLong(budget)
        @Volatile var lastEmit = 0L
    }

    /** Echte Läufe sperren Eingriffe von Hand in den Papierkorb (0.7.0, [MirrorLock]); die Simulation nicht */
    override suspend fun run(): SyncRun = if (dryRun) runInner() else MirrorLock.mutex.withLock { runInner() }

    private suspend fun runInner(): SyncRun {
        run = run.copy(id = dao.insertRun(run))
        var status = RunStatus.OK
        var message: Msg? = null
        var massWarn: Msg? = null
        try {
            if (!dryRun) {
                val day = 24L * 3600 * 1000
                val now = System.currentTimeMillis()
                dao.pruneLogs(now - 30 * day); dao.pruneRuns(now - 90 * day)
            }
            log("I", Msg.of(R.string.eng_start_mirror, trigger, profile.name, dir.labelMsg))
            // Offene Entscheidung der Schutzbremse -> nichts tun, bis der Benutzer entschieden hat
            if (!dryRun) guards.get(profile.id)?.let { return block(it) }
            emit(str(R.string.eng_reading_folders), 0, 0, 0, 0, null)

            // --- 1. Beide Seiten einlesen ---------------------------------------
            val treeUri = Uri.parse(profile.localTreeUri)
            if (!LocalTree.hasPermission(context, treeUri))
                throw IOException(str(R.string.err_local_access_lost))
            tree = LocalTree(context, treeUri)
            var states = dao.mirrorStates(profile.id).associateBy { it.relPath }
            // Handy-Ordner gelöscht oder verschoben? (Schutzbremse statt "alles gelöscht")
            val localRootOk = try { tree.stat(tree.rootDocId) != null } catch (e: Exception) { false }
            if (!localRootOk) {
                if (states.isNotEmpty()) return block(Guard(profile.id, GuardKind.LOCAL_MISSING))
                throw IOException(str(R.string.eng_local_missing))
            }
            val filter = FileFilter(profile.includePatterns, profile.excludePatterns)
            val skipTrash: (String) -> Boolean = { isTrashDir(it) }
            smb = try {
                SmbSession.open(profile)
            } catch (e: Exception) {
                if (dryRun) { message = Msg.of(R.string.eng_offline_preview, com.sync2pc.app.sync.userErrorMsg(e)); null }
                else { if (isNetworkError(e)) connectionFailed = true; throw e }
            }
            val s = smb
            var baseExists = false
            if (s != null) {
                val base = s.path()
                baseExists = s.dirExists(base)
                if (!baseExists && states.isNotEmpty()) {
                    // Schon synchronisiert, aber der PC-Ordner fehlt: nicht leer neu anlegen (sonst gälte alles als gelöscht)
                    return block(Guard(profile.id, GuardKind.REMOTE_MISSING))
                }
                if (!baseExists && !dryRun) { s.ensureDir(base); baseExists = true } // Erstabgleich: Ordner anlegen
            }
            // Vorgemerkte Wiederherstellungen (0.5.0) vor dem Einlesen: Der Abgleich sieht danach den neuen Stand
            // (nicht, solange nach einem Ordnerwechsel die Stichprobe aussteht – erst muss der Ordner bestätigt sein)
            var purgesRan = false
            if (!dryRun && s != null && guards.pendingCheck(profile.id).isEmpty()) {
                if (runRestores(s)) states = dao.mirrorStates(profile.id).associateBy { it.relPath }
                // Danach vorgemerktes Leeren des PC-Papierkorbs (0.6.0) – nach dem Wiederherstellen, damit nichts
                // gelöscht wird, was gerade zurückgeholt werden soll
                runPendingPurges(s)
                purgesRan = true
            }

            // recover: liegengebliebene ".s2pold" aufräumen bzw. zurückholen (nicht in der Simulation)
            local = tree.listRecursive(skipTrash, recover = !dryRun).filterKeys { filter.accepts(it) }
                .entries.associate { it.key.lowercase() to (it.key to it.value) }
            if (s != null) {
                remote = if (baseExists) s.listRecursive(s.path(), skipTrash).filterKeys { filter.accepts(it) }
                    .entries.associate { it.key.lowercase() to (it.key to it.value) } else emptyMap()
            }
            val conflicts = dao.conflicts(profile.id).associateBy { it.relPath.lowercase() }
            log("I", Msg.of(R.string.eng_counts, local.size, remote.size))

            // Nach einem Ordnerwechsel: Stichprobe, ob der neue Ordner zum Sync-Stand passt
            val pending = guards.pendingCheck(profile.id)
            if (pending.isNotEmpty() && s != null) {
                if (states.isNotEmpty() && !folderMatches(pending, states.values.toList()))
                    return block(Guard(profile.id, GuardKind.CHECK_FOLDER))
                if (!dryRun) {
                    guards.setPendingCheck(profile.id, emptySet())
                    // 0.11.10 (-034): Vorgemerktes PC-Leeren jetzt ausführen, noch vor Planung und Übertragung. Sonst
                    // legt dieser Lauf ersetzte Versionen in den PC-Papierkorb, und der nächste Lauf würde sie bei
                    // einer Regel "Alles"/"Tage" mit heutigem Datum ungesehen mitlöschen. Bewusst nur das Leeren,
                    // keine Wiederherstellungen (local/remote sind schon eingelesen; Papierkorb-Ordner sind davon
                    // ausgeschlossen, das Leeren ändert die Listen also nicht). Netzfehler: Vormerkung bleibt, Abbruch.
                    if (!purgesRan) { runPendingPurges(s); purgesRan = true }
                }
            }
            // "Nicht löschen" bei der Schutzbremse: dieser Lauf übernimmt keine Löschungen
            if (!dryRun && guards.takeKeepFiles(profile.id)) {
                propagate = false
                log("I", Msg.of(R.string.eng_keep_files))
            }
            if (!dryRun) purgeTrash()

            // --- 2. Planen -------------------------------------------------------
            emit(str(R.string.eng_comparing), 0, 0, 0, 0, null)
            val actions = ArrayList<Action>()
            // Erledigte Konflikte erst nach der Schutzbremse entfernen (sonst ginge die Entscheidung verloren)
            val doneConflicts = ArrayList<DoneConflict>()
            val keys = (local.keys + remote.keys + states.keys).toSortedSet()
            for (k in keys) {
                currentCoroutineContext().ensureActive()
                val l = local[k]; val r = remote[k]; val st = states[k]
                val rel = l?.first ?: r?.first ?: k
                val c = conflicts[k]
                if (c != null) {
                    if (l != null && r != null) {
                        when (c.resolution) {
                            null -> continue // wartet weiterhin auf Entscheidung
                            ConflictResolution.KEEP_LOCAL -> actions += Action(k, rel, Kind.UPLOAD, trashOther = true)
                            ConflictResolution.KEEP_REMOTE -> actions += Action(k, rel, Kind.DOWNLOAD, trashOther = true)
                            else -> actions += Action(k, rel, Kind.KEEP_BOTH)
                        }
                        doneConflicts += DoneConflict(k, c.relPath, c.resolution)
                        continue
                    }
                    // Eine Seite ist inzwischen weg -> Konflikt erledigt, normal weiter (unbedingt löschen)
                    doneConflicts += DoneConflict(k, c.relPath, null)
                }
                val planned = plan(k, rel, l?.second, r?.second, st)
                // Vergleich wegen WLAN-Verlust abgebrochen: Ergebnis wäre "nicht gleich" ohne Beweis -> nichts ausführen
                if (hashAborted) break
                planned?.let { actions += it }
            }
            if (hashAborted) {
                wifiLost = true
                log("W", Msg.of(R.string.eng_wifi_lost))
                return finish(RunStatus.PARTIAL, Msg.of(R.string.eng_wifi_lost))
            }

            // Schutzbremse: zu viele Löschungen (Anteil pro Profil, ab 100 Dateien immer) oder ein ganzer Unterordner
            val trashRels = actions.filter { it.kind == Kind.TRASH_LOCAL || it.kind == Kind.TRASH_REMOTE }.map { it.rel }
            val toTrash = trashRels.size
            val allowed = !dryRun && guards.takeAllowDelete(profile.id) // einmalige Freigabe immer verbrauchen
            val vanished = GuardPolicy.vanishedFolder(trashRels, states.keys)
            if ((GuardPolicy.massDelete(toTrash, states.size, profile.massDeletePercent) || vanished != null) && !allowed) {
                val g = Guard(profile.id, GuardKind.MASS_DELETE, toTrash, states.size,
                    folder = if (GuardPolicy.massDelete(toTrash, states.size, profile.massDeletePercent)) "" else vanished.orEmpty())
                if (!dryRun) return block(g)
                massWarn = Msg.of(R.string.eng_would_stop,
                    if (g.folder.isNotEmpty()) Msg.of(R.string.eng_would_trash_folder, g.folder)
                    else Msg.of(R.string.eng_would_trash_files, toTrash, states.size))
            }
            if (!dryRun) for (d in doneConflicts) {
                if (d.expected == null) { dao.deleteConflict(profile.id, d.relPath); continue }
                // Entscheidung des Benutzers: nur löschen, wenn sie noch genau so gespeichert ist. Hat er inzwischen
                // «Rückgängig» gedrückt, wird die Aktion nicht ausgeführt (SYNC-20261111-172)
                if (dao.deleteConflictIf(profile.id, d.relPath, d.expected) == 0) {
                    actions.removeAll { it.key == d.key }
                    log("I", Msg.of(R.string.eng_conflict_undone, d.relPath))
                }
            }

            // --- 3. Ausführen -----------------------------------------------------
            // Phase A (nacheinander): Verwaltung – Papierkorb, Stand merken, Konflikte, Aufräumen
            // Phase B (parallel):     Übertragungen, kleine Dateien zuerst
            val transfers = actions.filter { it.kind == Kind.UPLOAD || it.kind == Kind.DOWNLOAD || it.kind == Kind.KEEP_BOTH }
            if (dryRun) {
                for (a in actions) { if (preview.size >= previewLimit) break; previewOf(a) }
                val visible = actions.count { it.kind != Kind.RECORD && it.kind != Kind.DROP }
                if (message == null) message = Msg.of(if (preview.size < visible) R.string.eng_actions_first else R.string.eng_actions_all,
                    visible, preview.size)
                massWarn?.let { w -> message = Msg.join("\n", listOfNotNull(w, message)) }
            } else {
                val admin = actions.filter { it !in transfers }
                for ((index, a) in admin.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    if (index % 10 == 0 && !shouldContinue()) { wifiLost = true; break }
                    try {
                        executeAdmin(a, smb!!)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Verbindungsabbruch ist erwartbar -> Lauf unterbrechen, kein Datei-Fehler
                        if (isNetworkError(e)) { connectionFailed = true; throw e }
                        failed.incrementAndGet()
                        log("E", "✗ ${a.rel}: ${com.sync2pc.app.sync.shortError(e)}")
                    }
                }
                if (!wifiLost && transfers.isNotEmpty()) runTransfers(transfers)
                if (wifiLost) {
                    status = RunStatus.PARTIAL; message = Msg.of(R.string.eng_wifi_lost)
                    log("W", Msg.of(R.string.eng_wifi_lost))
                }
            }

            if (status == RunStatus.OK && (failed.get() > 0 || spaceShortage)) status = RunStatus.PARTIAL
            val summary = message ?: com.sync2pc.app.sync.RunSummary.mirror(
                up.get(), down.get(), trashed.get(), newConflicts, failed.get(), spaceShortage, deferred.get()
            )
            return finish(status, summary)
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val reason = interruptReason()
                if (reason != null) {
                    log("W", Msg.of(R.string.eng_interrupted_log, reason))
                    finish(RunStatus.INTERRUPTED, Msg.of(R.string.eng_interrupted_mirror, reason, up.get(), down.get()))
                } else {
                    log("W", Msg.of(R.string.eng_paused_log))
                    finish(RunStatus.CANCELLED, Msg.of(R.string.eng_paused_log))
                }
            }
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e)) {
                connectionFailed = true
                log("W", Msg.of(R.string.eng_connection_log_mirror, com.sync2pc.app.sync.shortError(e)))
                return finish(RunStatus.INTERRUPTED, Msg.of(R.string.eng_connection_mirror, up.get(), down.get()))
            }
            if (com.sync2pc.app.sync.isEncryptionUnavailable(e)) encryptionUnavailable = true
            log("E", Msg.of(R.string.eng_run_failed, com.sync2pc.app.sync.shortError(e)))
            return finish(RunStatus.FAILED, com.sync2pc.app.sync.userErrorMsg(e))
        } finally {
            smb?.close()
        }
    }

    // ---- Planung einer Datei ---------------------------------------------------------
    private suspend fun plan(k: String, rel: String, l: LocalEntry?, r: RemoteEntry?, st: MirrorState?): Action? =
        MirrorPlanner.decide(
            l, r, st, dir, propagate,
            conflictNewer = profile.conflict() == ConflictMode.NEWER, keepVersions = profile.keepVersions,
            sameContent = { sameContent(l!!, rel, r!!) },
        )?.let { Action(k, rel, it.kind, it.trashOther) }
    /**
     * Gleicher Inhalt? Nur beim Erstabgleich, bei auf beiden Seiten geänderten Dateien und bei einseitig geänderten
     * Dateien gleicher Grösse nötig, darum gründlich: immer die GANZE Datei beider Seiten (R-I3, SYNC-20261111-142,
     * vorher über 32 MB nur 32 Stichproben: eine Änderung dazwischen wurde nie übertragen). Das Lesen ist abbrechbar
     * (Coroutine, WLAN weg) und meldet bei grossen Dateien den Fortschritt. Lesefehler oder Abbruch ergeben
     * "nicht gleich" (siehe [ContentCompare]); bei WLAN-Verlust endet der Lauf, bevor etwas ausgeführt wird.
     */
    private suspend fun sameContent(l: LocalEntry, rel: String, r: RemoteEntry): Boolean {
        val s = smb ?: return false
        if (l.size != r.size) return false
        val ctx = currentCoroutineContext()
        var lastNetCheck = 0L
        val check: () -> Unit = {
            ctx.ensureActive()
            val now = System.currentTimeMillis()
            if (now - lastNetCheck > 2_000) {
                lastNetCheck = now
                if (!shouldContinue()) throw HashAbortedException()
            }
        }
        val big = l.size > PartialHash.FULL_LIMIT
        var doneLocal = 0L
        var doneRemote = 0L
        fun report() {
            val now = System.currentTimeMillis()
            if (now - lastCompareEmit < 700) return
            lastCompareEmit = now
            emit(str(R.string.eng_comparing), 0, 0, doneLocal + doneRemote, l.size * 2, rel)
        }
        return try {
            ContentCompare.same(l.size, r.size,
                { tree.contentHash(l.docId, l.size, check) { b -> if (big) { doneLocal = b; report() } } },
                { s.contentHash(remotePath(rel), r.size, check) { b -> if (big) { doneRemote = b; report() } } })
        } catch (e: HashAbortedException) {
            hashAborted = true
            false
        }
    }

    /** Stichprobe nach einem Ordnerwechsel: passt der Ordner auf den gewechselten Seiten zum Sync-Stand? */
    private fun folderMatches(sides: Set<Side>, states: List<MirrorState>): Boolean {
        val sample = GuardPolicy.sample(states)
        val dirs = GuardPolicy.topDirs(states.map { it.relPath })
        return sides.all { side ->
            val sizes: Map<String, Long> =
                if (side == Side.LOCAL) local.mapValues { it.value.second.size } else remote.mapValues { it.value.second.size }
            val existingDirs = sizes.keys.filter { '/' in it }.map { it.substringBefore('/') }.toSet()
            GuardPolicy.matches(sample, dirs, side, { sizes[it] }, { it in existingDirs })
        }
    }

    /**
     * Schutzbremse: Lauf anhalten und auf den Benutzer warten (Startseite + Hinweis).
     * In der Simulation wird nur gemeldet, dass der Lauf anhalten würde.
     */
    private suspend fun block(g: Guard): SyncRun {
        val msg = if (dryRun) Msg.of(R.string.eng_would_stop, MirrorGuard.titleMsg(g))
        else Msg.of(R.string.eng_waiting_decision, MirrorGuard.titleMsg(g))
        if (!dryRun) { guards.set(g); blocked = true }
        log("W", msg)
        return finish(RunStatus.WAITING, msg)
    }

    /** Ist die PC-Datei noch so wie beim Einlesen? (beide null = weiterhin nicht vorhanden) */
    private fun unchanged(now: RemoteEntry?, listed: RemoteEntry?): Boolean =
        if (now == null || listed == null) now == null && listed == null
        else now.size == listed.size && now.modified == listed.modified

    private fun unchanged(now: LocalEntry?, listed: LocalEntry?): Boolean =
        if (now == null || listed == null) now == null && listed == null
        else now.size == listed.size && now.modified == listed.modified

    /**
     * Handy-Datei jetzt, per Ordnerliste wie beim Einlesen; null = verschwunden. Lässt sich der Ordner nicht lesen, gilt die Datei
     * als unverändert ([fallback]): dann entscheidet wie bisher die Bytezahl (SYNC-20261111-202).
     */
    private fun findNow(rel: String, fallback: LocalEntry): LocalEntry? =
        try { tree.find(rel) } catch (e: CancellationException) { throw e } catch (e: Exception) { fallback }

    // ---- Ausführung ------------------------------------------------------------------
    private fun remotePath(s: SmbSession, rel: String): String = s.path(rel.replace('/', '\\'))
    private fun remotePath(rel: String): String = remotePath(smb!!, rel)

    /** Grösse einer Übertragung (für Reihenfolge, Fortschritt und Speicherbudget) */
    private fun sizeOf(a: Action): Long = when (a.kind) {
        Kind.UPLOAD -> local[a.key]?.second?.size ?: 0
        Kind.DOWNLOAD -> remote[a.key]?.second?.size ?: 0
        else -> (local[a.key]?.second?.size ?: 0) + (remote[a.key]?.second?.size ?: 0)
    }

    /** Phase A: Verwaltungsschritte (nacheinander, Haupt-Verbindung) */
    private suspend fun executeAdmin(a: Action, s: SmbSession) {
        val l = local[a.key]?.second
        val r = remote[a.key]?.second
        when (a.kind) {
            Kind.TRASH_LOCAL -> {
                l ?: return
                localTouched = true; tree.moveToTrash(a.rel, l, today); trashed.incrementAndGet()
                dao.deleteMirrorState(profile.id, a.key)
                log("I", Msg.of(R.string.eng_trash_local, a.rel))
            }
            Kind.TRASH_REMOTE -> {
                trashRemote(s, a.rel)
                dao.deleteMirrorState(profile.id, a.key)
                log("I", Msg.of(R.string.eng_trash_remote, a.rel))
            }
            Kind.RECORD -> if (l != null && r != null) record(a.key, l, r)
            Kind.DROP -> dao.deleteMirrorState(profile.id, a.key)
            Kind.CONFLICT -> {
                if (l == null || r == null) return
                newConflicts++
                dao.upsertConflict(
                    MirrorConflict(
                        profile.id, a.rel, l.size, l.modified, r.size, r.modified, System.currentTimeMillis()
                    )
                )
                log("W", Msg.of(R.string.eng_conflict, a.rel))
            }
            Kind.UPLOAD, Kind.DOWNLOAD, Kind.KEEP_BOTH -> {} // Phase B
        }
    }

    /**
     * Phase B: Übertragungen parallel (Einstellung "Parallele Übertragungen"), kleine Dateien zuerst.
     * Jede Übertragung hat ihre eigene SMB-Verbindung. Bei Verbindungsabbruch einmal neu verbinden und
     * wiederholen; ist das Netz weg, wird der ganze Lauf unterbrochen (Rest folgt beim nächsten Sync).
     */
    private suspend fun runTransfers(transfers: List<Action>) {
        val ordered = TransferPlan.smallFirst(transfers) { sizeOf(it) }
        val sh = Shared(ordered.size, ordered.sumOf { sizeOf(it) }, freeSpace() - RESERVE_BYTES)
        val queue = ConcurrentLinkedQueue(ordered)
        val parallel = GlobalSettings(context).parallelUploads.coerceIn(1, 4).coerceAtMost(ordered.size)
        log("I", Msg.plural(R.plurals.eng_transfer_mirror, ordered.size, ordered.size, parallel))
        emitShared(sh, null, force = true)
        coroutineScope {
            repeat(parallel) { launch { transferWorker(queue, sh) } }
        }
    }

    /** Schlüssel der Dateien, die dieser Lauf selbst schon in den Papierkorb verschoben hat (für die Wiederholung, -166) */
    private val trashedByRun: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /** «Beide behalten», von diesem Lauf schon umbenannt: Schlüssel -> (neuer relativer Pfad, neue Dokument-ID) (-171) */
    private val renamedByRun = ConcurrentHashMap<String, Pair<String, String>>()

    /** «Beide behalten», Konfliktkopie schon hochgeladen: bei einer Wiederholung nur noch der Download (-171) */
    private val uploadedByRun: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private suspend fun transferWorker(queue: ConcurrentLinkedQueue<Action>, sh: Shared) {
        var session: SmbSession? = null
        try {
            while (!sh.stop.get()) {
                val a = queue.poll() ?: break
                currentCoroutineContext().ensureActive()
                if (sh.done.get() % 10 == 0 && !shouldContinue()) {
                    if (sh.stop.compareAndSet(false, true)) wifiLost = true
                    break
                }
                val size = sizeOf(a)
                // Speicher auf dem Handy für Downloads gemeinsam verrechnen
                val needs = if (a.kind == Kind.DOWNLOAD || a.kind == Kind.KEEP_BOTH) remote[a.key]?.second?.size ?: 0 else 0
                if (needs > 0 && sh.budget.addAndGet(-needs) < 0) {
                    sh.budget.addAndGet(needs)
                    spaceShortage = true
                    log("W", Msg.of(R.string.eng_no_space, a.rel))
                    countDone(sh, size)
                    continue
                }
                var attempt = 0
                while (true) {
                    try {
                        // Tote Verbindung (Leerlauf-Trennung, kurzer WLAN-Aussetzer) -> neu aufbauen
                        val s = session?.takeIf { it.isAlive() }
                            ?: SmbSession.open(profile).also { try { session?.close() } catch (ignored: Exception) {}; session = it }
                        executeTransfer(a, s) { b ->
                            sh.inflight[a.key] = b
                            emitShared(sh, a.rel)
                        }
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (isNetworkError(e)) {
                            try { session?.close() } catch (ignored: Exception) {}
                            session = null
                            if (attempt++ == 0) { delay(2_000); continue } // Upload/Download setzt an der Abbruchstelle fort
                            sh.stop.set(true)
                            connectionFailed = true
                            throw e // Netz wirklich weg -> Lauf unterbrechen
                        }
                        failed.incrementAndGet()
                        log("E", "✗ ${a.rel}: ${com.sync2pc.app.sync.shortError(e)}")
                        break
                    }
                }
                sh.inflight.remove(a.key)
                countDone(sh, size)
            }
        } finally {
            session?.close()
        }
    }

    private suspend fun executeTransfer(a: Action, s: SmbSession, onBytes: (Long) -> Unit) {
        val l = local[a.key]?.second
        val r = remote[a.key]?.second
        when (a.kind) {
            Kind.UPLOAD -> {
                l ?: return
                // Handy-Datei seit dem Einlesen schon vor dem Start geändert (oder verschwunden)? Dann zurückstellen, und zwar
                // vor jedem Eingriff am PC (Papierkorb) (SYNC-20261111-202). Per Ordnerliste lesen wie beim Einlesen.
                if (!unchanged(findNow(a.rel, l), l)) return skipChanged(a.rel, Msg.of(R.string.eng_on_phone))
                // Während des Laufs auf dem PC geändert? Dann nicht überschreiben – der nächste Lauf entscheidet.
                // Wiederholung nach Verbindungsabbruch: Hat dieser Lauf die PC-Datei schon in den Papierkorb verschoben,
                // fehlt sie erwartungsgemäss (SYNC-20261111-166, siehe TransferResume)
                val before = a.key in trashedByRun
                val nowRemote = s.stat(remotePath(s, a.rel))
                when (TransferResume.step(a.trashOther, r != null, before, unchanged(nowRemote, r), nowRemote == null)) {
                    TransferStep.SKIP_CHANGED -> return skipChanged(a.rel, Msg.of(R.string.eng_on_pc))
                    TransferStep.TRASH_THEN_TRANSFER -> {
                        // Zuerst den Sync-Stand vergessen: Scheitert der Upload, gilt die Datei beim nächsten Lauf
                        // als neu (wird übertragen) statt als "auf dem PC gelöscht" (würde sonst auch das Handy löschen)
                        dao.deleteMirrorState(profile.id, a.key)
                        trashRemote(s, a.rel)
                        trashedByRun.add(a.key)
                    }
                    TransferStep.TRANSFER -> {}
                }
                try {
                    upload(s, a.key, a.rel, l, onBytes = onBytes)
                } catch (e: SourceChangedException) {
                    // Handy-Datei während der Übertragung geändert: Temp-Datei ist gelöscht, kein Sync-Stand für diese Version
                    // geschrieben -> der nächste Lauf überträgt die neue Version (SYNC-20261111-202)
                    return skipChanged(a.rel, Msg.of(R.string.eng_on_phone))
                }
            }
            Kind.DOWNLOAD -> {
                r ?: return
                // Vorhandene Handy-Datei seit dem Einlesen geändert? (Neu entstandene prüft writeFileResumable)
                // Wiederholung nach Verbindungsabbruch: Hat dieser Lauf die Handy-Datei schon in den Papierkorb verschoben,
                // fehlt sie erwartungsgemäss (SYNC-20261111-166, siehe TransferResume)
                val before = a.key in trashedByRun
                // Per Ordnerliste lesen wie beim Einlesen (stat() liefert bei manchen Speicherorten andere Werte)
                val nowLocal = if (l != null || before) try { tree.find(a.rel) } catch (e: Exception) { null } else null
                when (TransferResume.step(a.trashOther, l != null, before, l == null || unchanged(nowLocal, l), nowLocal == null)) {
                    TransferStep.SKIP_CHANGED -> return skipChanged(a.rel, Msg.of(R.string.eng_on_phone))
                    TransferStep.TRASH_THEN_TRANSFER -> {
                        dao.deleteMirrorState(profile.id, a.key)
                        localTouched = true; tree.moveToTrash(a.rel, l!!, today); trashed.incrementAndGet()
                        trashedByRun.add(a.key)
                    }
                    TransferStep.TRANSFER -> {}
                }
                download(s, a.key, a.rel, r, expectNewAfterOwnTrash = before, onBytes = onBytes)
            }
            Kind.KEEP_BOTH -> {
                if (l == null || r == null) return
                // Handy-Version umbenennen und hochladen, PC-Version unter Originalnamen holen (SYNC-20261111-171):
                // freier Name (nie eine vorhandene Datei), Upload ohne Überschreiben, Wiederholung ohne zweites Umbenennen
                val parent = a.rel.substringBeforeLast('/', "")
                val name = a.rel.substringAfterLast('/')
                fun relOf(n: String) = if (parent.isEmpty()) n else "$parent/$n"
                val prior = renamedByRun[a.key]
                val newRel: String
                val newDoc: String
                if (prior != null) {
                    // Wiederholung nach Verbindungsabbruch: Die alte Dokument-ID gilt nicht mehr, nur der Merker
                    newRel = prior.first; newDoc = prior.second
                } else {
                    val newName = ConflictName.free(name) { c ->
                        val rel = relOf(c)
                        local.containsKey(rel.lowercase()) || remote.containsKey(rel.lowercase()) ||
                            (try { tree.find(rel) } catch (e: Exception) { null }) != null ||
                            s.stat(remotePath(s, rel)) != null
                    } ?: throw IOException(str(R.string.eng_err_conflict_name_exhausted, a.rel))
                    newRel = relOf(newName)
                    localTouched = true; newDoc = tree.rename(l.docId, newName)
                    // Sofort vergessen (Review -171 H1): Scheitert eine der folgenden Prüfungen oder stirbt der Prozess, gilt die
                    // Datei im nächsten Lauf als neu. Mit altem Stand sähe der Planer «auf dem Handy gelöscht» und
                    // würde die PC-Version in den Papierkorb verschieben.
                    dao.deleteMirrorState(profile.id, a.key)
                    // Der Anbieter darf den Namen nicht eigenmächtig ändern (z. B. « (1)» anhängen): sonst nicht hochladen.
                    // Die Datei bleibt umbenannt erhalten; der nächste Lauf überträgt sie als neue Datei.
                    val actual = try { tree.stat(newDoc)?.name } catch (e: Exception) { null }
                    if (!ConflictName.sameName(newName, actual))
                        throw IOException(str(R.string.eng_err_conflict_name_differs, newName, actual ?: "?"))
                    renamedByRun[a.key] = newRel to newDoc
                }
                // Hochgeladen wird nur die umbenannte Datei selbst, nie eine zufällig gleichnamige andere
                val listed = try { tree.find(newRel) } catch (e: Exception) { null }
                val ls = listed?.takeIf { it.docId == newDoc } ?: tree.stat(newDoc)
                    ?: throw IOException(str(R.string.eng_err_renamed_missing))
                dao.deleteMirrorState(profile.id, a.key)
                // Wiederholung nach einem Abbruch im Download: Der Upload ist schon fertig und darf nicht erneut laufen
                // (die Zieldatei existiert, ohne Überschreiben gäbe es einen Fehler; Review -171 M1)
                if (a.key !in uploadedByRun) {
                    try {
                        // Vergleichsbasis nur, wenn ls aus der Ordnerliste stammt (stat() liefert z. B. andere Zeitstempel)
                        upload(s, newRel.lowercase(), newRel, ls, replaceExisting = false, watchSource = listed?.docId == newDoc, onBytes = onBytes)
                    } catch (e: SourceChangedException) {
                        // Konfliktkopie während der Übertragung geändert: nichts hochgeladen; der Sync-Stand ist schon vergessen,
                        // der nächste Lauf überträgt sie als neue Datei und holt die PC-Version (SYNC-20261111-202)
                        return skipChanged(a.rel, Msg.of(R.string.eng_on_phone))
                    }
                    uploadedByRun.add(a.key)
                }
                download(s, a.key, a.rel, r) { b -> onBytes(ls.size + b) }
            }
            else -> {} // Phase A
        }
    }

    private suspend fun upload(
        s: SmbSession, key: String, rel: String, l: LocalEntry, replaceExisting: Boolean = true, watchSource: Boolean = true,
        onBytes: (Long) -> Unit,
    ) {
        val path = remotePath(s, rel)
        s.ensureDir(path.substringBeforeLast('\\', ""))
        val ctx = currentCoroutineContext()
        s.upload(
            finalPath = path, size = l.size,
            openInput = { off -> tree.openInput(l.docId, off) },
            onProgress = onBytes, checkCancel = { ctx.ensureActive() },
            modifiedMillis = l.modified, replaceExisting = replaceExisting,
            // Fortsetzen einer angefangenen .s2ptmp nur mit Prüfung gegen die Handy-Datei (0.4.1)
            expectedHash = { PartialHash.compute(l.size) { off, len -> tree.readAt(l.docId, off, len) } },
            // Handy-Datei während der Übertragung geändert/gewachsen? Dann Temp-Datei verwerfen, zurückstellen (-202)
            sourceChanged = if (watchSource) ({ !unchanged(findNow(rel, l), l) }) else null,
        )
        val rs = s.stat(path) ?: throw IOException(str(R.string.eng_err_after_upload_missing))
        if (rs.size != l.size) throw IOException(str(R.string.eng_err_after_upload_size))
        record(key, l, rs)
        up.incrementAndGet(); bytes.addAndGet(l.size)
        log("I", "↑ $rel")
    }

    private suspend fun download(
        s: SmbSession, key: String, rel: String, r: RemoteEntry, expectNewAfterOwnTrash: Boolean = false, onBytes: (Long) -> Unit,
    ) {
        val ctx = currentCoroutineContext()
        // Abgebrochene Downloads setzen an der Abbruchstelle fort (angefangene .s2ptmp auf dem Handy)
        // Gab es die Datei beim Einlesen nicht, ist sie inzwischen aber auf dem Handy entstanden -> nicht überschreiben
        // (Wiederholung nach eigenem Verschieben in den Papierkorb: die Datei fehlt jetzt, es darf nichts Neues dort liegen)
        val expectNew = local[key] == null || expectNewAfterOwnTrash
        val (docId, resumedAt) = try {
            localTouched = true; tree.writeFileResumable(rel, r.size, r.modified, expectNew = expectNew) { out, offset ->
                s.download(remotePath(s, rel), out, onBytes, { ctx.ensureActive() }, startOffset = offset)
            }
        } catch (e: ChangedDuringRunException) {
            return skipChanged(rel, Msg.of(R.string.eng_on_phone))
        }
        // Stand so lesen wie beim nächsten Einlesen (Ordnerliste statt Einzelabfrage): Manche Speicherorte (z. B.
        // "Downloads") liefern sonst andere Zeitstempel -> der nächste Lauf hielte die Datei für geändert (0.5.1)
        val ls = (try { tree.find(rel) } catch (e: Exception) { null }) ?: tree.stat(docId)
            ?: throw IOException(str(R.string.eng_err_after_download_missing))
        if (ls.size != r.size) throw IOException(str(R.string.eng_err_after_download_size))
        record(key, ls, r)
        down.incrementAndGet(); bytes.addAndGet(r.size)
        log("I", if (resumedAt > 0) Msg.join("", listOf(Msg.lit("↓ $rel "),
            Msg.of(R.string.eng_resumed_at, com.sync2pc.app.sync.Notifications.formatBytes(resumedAt)))) else Msg.lit("↓ $rel"))
    }

    /**
     * Vorgemerkte Wiederherstellungen ausführen (0.5.0): aktuelle Version auf beiden Seiten in den Papierkorb,
     * alte Version an den ursprünglichen Ort zurück und auf die andere Seite kopieren, Sync-Stand setzen.
     * Netzwerkfehler -> Vormerkung bleibt, der Lauf wird wie üblich unterbrochen. Liefert true, wenn etwas geschah.
     */
    private suspend fun runRestores(s: SmbSession): Boolean {
        val failures = RestoreFailures(context)
        // Ablauf, Zurückrollen und Fehlerbehandlung: RestoreExecutor (SYNC-20261111-163 Stufe 2)
        return RestoreExecutor(tree, s, object : RestoreHost {
            override val today: String get() = this@MirrorEngine.today
            override suspend fun pendingRestores() = dao.pendingRestores(profile.id)
            override suspend fun mirrorStates() = dao.mirrorStates(profile.id)
            override suspend fun deleteMirrorState(key: String) = dao.deleteMirrorState(profile.id, key)
            override suspend fun upsertMirrorState(state: MirrorState) = dao.upsertMirrorState(state)
            override suspend fun deleteConflict(relPath: String) = dao.deleteConflict(profile.id, relPath)
            override suspend fun deleteRestore(id: Long) = dao.deleteRestore(id)
            override fun isExhausted(restoreId: Long) = failures.isExhausted(restoreId)
            override fun markFailure(restoreId: Long) = failures.mark(profile.id, restoreId)
            override fun clearFailure(restoreId: Long) = failures.clear(restoreId)
            override suspend fun log(level: String, msg: Msg) = this@MirrorEngine.log(level, msg)
            override fun warn(what: String, e: Exception) { android.util.Log.w("MirrorEngine", what, e) }
            override fun countTrashed() { trashed.incrementAndGet() }
            override fun countFailed() { failed.incrementAndGet() }
            override fun markConnectionFailed() { connectionFailed = true }
            override fun trashRemote(rel: String) = this@MirrorEngine.trashRemote(s, rel)
            override suspend fun upload(key: String, rel: String, local: LocalEntry) = this@MirrorEngine.upload(s, key, rel, local, watchSource = false) {}
            override suspend fun download(key: String, rel: String, remote: RemoteEntry) = this@MirrorEngine.download(s, key, rel, remote) {}
        }).run()
    }

    /** Vorgemerktes Leeren des PC-Papierkorbs (PC war beim Auslösen nicht erreichbar) */
    private suspend fun runPendingPurges(s: SmbSession) {
        val store = PurgeStore(context)
        val rules = store.rules(profile.id)
        val files = store.files(profile.id)
        if (rules.isEmpty() && files.isEmpty()) return
        // Noch vorgemerkte Wiederherstellungen (z. B. fehlgeschlagene) nie mitlöschen (0.7.0)
        val keep = TrashPurge.reservedPaths(dao.pendingRestores(profile.id), Side.REMOTE)
        try {
            var dirs = 0
            for (r in rules) dirs += TrashCleaner.purgeRemote(s, r, keep)
            var n = 0
            for (f in files) if (f.lowercase() !in keep && TrashCleaner.deleteRemoteFile(s, f)) n++
            store.clear(profile.id)
            log("I", Msg.of(R.string.eng_purged_pc, dirs, n))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e)) { connectionFailed = true; throw e } // bleibt vorgemerkt
            store.clear(profile.id)
            log("E", Msg.of(R.string.eng_purge_pc_failed, com.sync2pc.app.sync.shortError(e)))
        }
    }

    /** Datei wurde seit dem Einlesen geändert -> nicht überschreiben; der nächste Lauf sieht beide Stände */
    private suspend fun skipChanged(rel: String, where: Msg) {
        deferred.incrementAndGet()
        log("W", Msg.of(R.string.eng_changed_during_run, rel, where))
    }

    /**
     * PC-Datei nach "<Basis>\.LeoSync-Papierkorb\<Datum>\<Pfad>" verschieben; gleicher Name dort -> Uhrzeit anhängen
     * ("a (14-32-05).txt", 0.7.0). Liefert den vollen Pfad im Papierkorb.
     */
    private fun trashRemote(s: SmbSession, rel: String): String {
        val parent = rel.substringBeforeLast('/', "").replace('/', '\\')
        val trashDir = s.path(listOf(TRASH_DIR, today, parent).filter { it.isNotEmpty() }.joinToString("\\"))
        s.ensureDir(trashDir)
        val name = TrashIndex.uniqueTrashName(rel.substringAfterLast('/'), TrashIndex.timeNow()) { s.stat("$trashDir\\$it") != null }
        s.move(remotePath(s, rel), "$trashDir\\$name")
        trashed.incrementAndGet()
        return "$trashDir\\$name"
    }

    private fun countDone(sh: Shared, size: Long) {
        sh.bytesDone.addAndGet(size)
        val done = sh.done.incrementAndGet()
        emitShared(sh, null, force = done == sh.totalFiles)
    }

    private fun emitShared(sh: Shared, file: String?, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - sh.lastEmit < 700) return
        sh.lastEmit = now
        val b = (sh.bytesDone.get() + sh.inflight.values.sum()).coerceAtMost(sh.totalBytes)
        emit(str(R.string.eng_syncing), sh.done.get(), sh.totalFiles, b, sh.totalBytes, file)
    }

    /** Abgelaufene Datumsordner im Papierkorb auf beiden Seiten endgültig löschen */
    private suspend fun purgeTrash() {
        val days = profile.trashRetentionDays
        if (days <= 0) return
        val rule = PurgeRule(PurgeRule.Mode.OLDER, before = TrashPolicy.cutoff(days))
        // Vorgemerkte Wiederherstellungen bleiben auch nach Ablauf der Frist liegen (0.7.0)
        val pending = dao.pendingRestores(profile.id)
        var n = 0
        try {
            localTouched = true; n += tree.purgeTrash(rule, TrashPurge.reservedPaths(pending, Side.LOCAL))
        } catch (e: Exception) {
            log("W", Msg.of(R.string.eng_purge_local_failed, com.sync2pc.app.sync.shortError(e)))
        }
        val s = smb
        if (s != null) {
            try {
                n += TrashCleaner.purgeRemote(s, rule, TrashPurge.reservedPaths(pending, Side.REMOTE))
            } catch (e: Exception) {
                if (isNetworkError(e)) throw e
                log("W", Msg.of(R.string.eng_purge_remote_failed, com.sync2pc.app.sync.shortError(e)))
            }
        }
        if (n > 0) log("I", Msg.of(R.string.eng_purged_expired, n, days))
    }

    private suspend fun record(key: String, l: LocalEntry, r: RemoteEntry) {
        dao.upsertMirrorState(
            MirrorState(profile.id, key, l.size, l.modified, r.size, r.modified, System.currentTimeMillis())
        )
    }

    private fun previewOf(a: Action) {
        val label = when (a.kind) {
            Kind.UPLOAD -> str(if (a.trashOther) R.string.eng_pv_upload_trash else R.string.eng_pv_upload)
            Kind.DOWNLOAD -> str(if (a.trashOther) R.string.eng_pv_download_trash else R.string.eng_pv_download)
            Kind.TRASH_LOCAL -> str(R.string.eng_pv_trash_local)
            Kind.TRASH_REMOTE -> str(R.string.eng_pv_trash_remote)
            Kind.CONFLICT -> str(R.string.eng_pv_conflict)
            Kind.KEEP_BOTH -> str(R.string.eng_pv_keep_both)
            Kind.RECORD, Kind.DROP -> return
        }
        preview += PreviewEntry(label, a.rel, a.kind == Kind.CONFLICT)
    }

    private fun freeSpace(): Long = try {
        @Suppress("DEPRECATION")
        StatFs(Environment.getExternalStorageDirectory().path).availableBytes
    } catch (e: Exception) { Long.MAX_VALUE / 2 }

    private suspend fun finish(status: String, message: Msg): SyncRun {
        val text = message.text()
        run = run.copy(
            finishedAt = System.currentTimeMillis(), status = status, message = text, messageCode = message.toCode(),
            uploaded = up.get() + down.get(), skipped = 0, failed = failed.get(), waiting = newConflicts, bytes = bytes.get(), deferred = deferred.get()
        )
        dao.updateRun(run)
        // SYNC-20261111-213: Datei während des Laufs neu entstanden/gelöscht -> Folgelauf (billiger Ordner-Stempelvergleich)
        if (!dryRun && status != RunStatus.FAILED && ::tree.isInitialized) {
            changedDuringRun = withContext(Dispatchers.IO) { tree.changedSinceListing(localTouched) }
        }
        if (!dryRun) log("I", Msg.of(R.string.eng_end, message))
        return run
    }

    private fun emit(phase: String, done: Int, total: Int, doneBytes: Long, totalBytes: Long, file: String?) =
        onProgress(SyncProgress(profile.name, phase, done, total, doneBytes, totalBytes, file))

    private fun isNetworkError(e: Throwable): Boolean = com.sync2pc.app.sync.isNetworkError(e)
}
