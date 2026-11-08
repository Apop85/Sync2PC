package com.sync2pc.app.mirror

import com.sync2pc.app.data.RunStatus

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import com.sync2pc.app.R
import com.sync2pc.app.data.AppDatabase
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
    private val trigger: String,
    private val dryRun: Boolean = false,
    private val shouldContinue: () -> Boolean = { true },
    private val onProgress: (SyncProgress) -> Unit = {},
    /** Bei einem Stopp: Grund, wenn Android unterbrochen hat (z. B. "WLAN getrennt"); null = vom Benutzer abgebrochen */
    private val interruptReason: () -> String? = { null },
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
        fun isTrashDir(name: String): Boolean = TRASH_DIRS.any { it.equals(name, ignoreCase = true) }
        /** Zusatz beim Behalten beider Versionen (bis 0.7.x "_Konflikt-Handy") */
        const val CONFLICT_SUFFIX = "_conflict-phone"
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

    private val dao = AppDatabase.get(context).dao()
    private val guards = GuardStore(context)
    private val dir: SyncDirection = profile.dir()
    /** Löschungen übernehmen? (einmal aus nach "Nicht löschen" bei der Schutzbremse) */
    private var propagate = profile.propagateDeletes
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())

    private enum class Kind { UPLOAD, DOWNLOAD, TRASH_LOCAL, TRASH_REMOTE, RECORD, DROP, CONFLICT, KEEP_BOTH }

    /** key = relativer Pfad in Kleinbuchstaben, rel = Originalschreibweise */
    private data class Action(val key: String, val rel: String, val kind: Kind, val trashOther: Boolean = false)

    private lateinit var tree: LocalTree
    private var smb: SmbSession? = null
    private var local: Map<String, Pair<String, LocalEntry>> = emptyMap()
    private var remote: Map<String, Pair<String, RemoteEntry>> = emptyMap()

    private var run = SyncRun(
        profileId = profile.id, profileName = profile.name, trigger = trigger,
        dryRun = dryRun, startedAt = System.currentTimeMillis()
    )

    private suspend fun log(level: String, msg: String) {
        if (dryRun) return
        dao.insertLog(LogEntry(runId = run.id, time = System.currentTimeMillis(), level = level, message = msg))
    }

    // Zähler (threadsicher, parallele Übertragungen)
    private val up = AtomicInteger(); private val down = AtomicInteger(); private val failed = AtomicInteger()
    private val bytes = AtomicLong(); private val trashed = AtomicInteger()

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
        var message = ""
        var massWarn: String? = null
        try {
            if (!dryRun) {
                val day = 24L * 3600 * 1000
                val now = System.currentTimeMillis()
                dao.pruneLogs(now - 30 * day); dao.pruneRuns(now - 90 * day)
            }
            log("I", str(R.string.eng_start_mirror, trigger, profile.name, dir.label))
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
                if (dryRun) { message = str(R.string.eng_offline_preview, e.message ?: ""); null }
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
            log("I", str(R.string.eng_counts, local.size, remote.size))

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
                log("I", str(R.string.eng_keep_files))
            }
            if (!dryRun) purgeTrash()

            // --- 2. Planen -------------------------------------------------------
            emit(str(R.string.eng_comparing), 0, 0, 0, 0, null)
            val actions = ArrayList<Action>()
            // Erledigte Konflikte erst nach der Schutzbremse entfernen (sonst ginge die Entscheidung verloren)
            val doneConflicts = ArrayList<String>()
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
                        doneConflicts += c.relPath
                        continue
                    }
                    // Eine Seite ist inzwischen weg -> Konflikt erledigt, normal weiter
                    doneConflicts += c.relPath
                }
                plan(k, rel, l?.second, r?.second, st)?.let { actions += it }
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
                massWarn = str(R.string.eng_would_stop,
                    if (g.folder.isNotEmpty()) str(R.string.eng_would_trash_folder, g.folder)
                    else str(R.string.eng_would_trash_files, toTrash, states.size))
            }
            if (!dryRun) for (p in doneConflicts) dao.deleteConflict(profile.id, p)

            // --- 3. Ausführen -----------------------------------------------------
            // Phase A (nacheinander): Verwaltung – Papierkorb, Stand merken, Konflikte, Aufräumen
            // Phase B (parallel):     Übertragungen, kleine Dateien zuerst
            val transfers = actions.filter { it.kind == Kind.UPLOAD || it.kind == Kind.DOWNLOAD || it.kind == Kind.KEEP_BOTH }
            if (dryRun) {
                for (a in actions) { if (preview.size >= previewLimit) break; previewOf(a) }
                val visible = actions.count { it.kind != Kind.RECORD && it.kind != Kind.DROP }
                if (message.isEmpty()) message = str(if (preview.size < visible) R.string.eng_actions_first else R.string.eng_actions_all,
                    visible, preview.size)
                massWarn?.let { message = "$it\n$message" }
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
                    status = RunStatus.PARTIAL; message = str(R.string.eng_wifi_lost)
                    log("W", message)
                }
            }

            if (status == RunStatus.OK && (failed.get() > 0 || spaceShortage)) status = RunStatus.PARTIAL
            if (message.isEmpty()) message = com.sync2pc.app.sync.RunSummary.mirror(
                up.get(), down.get(), trashed.get(), newConflicts, failed.get(), spaceShortage
            )
            return finish(status, message)
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val reason = interruptReason()
                if (reason != null) {
                    log("W", str(R.string.eng_interrupted_log, reason))
                    finish(RunStatus.INTERRUPTED, str(R.string.eng_interrupted_mirror, reason, up.get(), down.get()))
                } else {
                    log("W", str(R.string.eng_paused_log))
                    finish(RunStatus.CANCELLED, str(R.string.eng_paused_log))
                }
            }
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e)) {
                connectionFailed = true
                log("W", str(R.string.eng_connection_log_mirror, com.sync2pc.app.sync.shortError(e)))
                return finish(RunStatus.INTERRUPTED, str(R.string.eng_connection_mirror, up.get(), down.get()))
            }
            log("E", str(R.string.eng_run_failed, com.sync2pc.app.sync.shortError(e)))
            return finish(RunStatus.FAILED, com.sync2pc.app.sync.userError(e))
        } finally {
            smb?.close()
        }
    }

    // ---- Planung einer Datei ---------------------------------------------------------
    private suspend fun plan(k: String, rel: String, l: LocalEntry?, r: RemoteEntry?, st: MirrorState?): Action? {
        if (l == null && r == null) return if (st != null) Action(k, rel, Kind.DROP) else null

        if (l != null && r == null) {
            if (st == null) return if (dir.up) Action(k, rel, Kind.UPLOAD) else null // neu auf dem Handy
            // war synchron, auf dem PC gelöscht
            return when {
                dir == SyncDirection.UPLOAD -> Action(k, rel, Kind.UPLOAD)            // Ziel wiederherstellen
                propagate -> Action(k, rel, Kind.TRASH_LOCAL)
                dir == SyncDirection.BOTH -> Action(k, rel, Kind.UPLOAD)              // nichts verlieren
                else -> Action(k, rel, Kind.DROP)                                     // PC → Handy: Handy-Kopie behalten
            }
        }
        if (l == null && r != null) {
            if (st == null) return if (dir.down) Action(k, rel, Kind.DOWNLOAD) else null // neu auf dem PC
            return when {
                dir == SyncDirection.DOWNLOAD -> Action(k, rel, Kind.DOWNLOAD)
                propagate -> Action(k, rel, Kind.TRASH_REMOTE)
                dir == SyncDirection.BOTH -> Action(k, rel, Kind.DOWNLOAD)
                else -> Action(k, rel, Kind.DROP)
            }
        }
        // beide vorhanden
        l!!; r!!
        val changedL = st == null || l.size != st.localSize || l.modified != st.localModified
        val changedR = st == null || r.size != st.remoteSize || r.modified != st.remoteModified
        if (!changedL && !changedR) return null
        if (st == null || (changedL && changedR)) {
            if (sameContent(l, rel, r)) return Action(k, rel, Kind.RECORD)
            return when (dir) {
                SyncDirection.UPLOAD -> Action(k, rel, Kind.UPLOAD, trashOther = true)
                SyncDirection.DOWNLOAD -> Action(k, rel, Kind.DOWNLOAD, trashOther = true)
                SyncDirection.BOTH ->
                    if (profile.conflict() == ConflictMode.NEWER) {
                        if (l.modified >= r.modified) Action(k, rel, Kind.UPLOAD, trashOther = true)
                        else Action(k, rel, Kind.DOWNLOAD, trashOther = true)
                    } else Action(k, rel, Kind.CONFLICT)
            }
        }
        // Nur eine Seite "geändert", Grösse aber gleich wie auf der anderen Seite: Ist der Inhalt gleich, hat sich nur
        // der Zeitstempel verschoben (z. B. vom Speicherort nachträglich gesetzt) -> Stand merken statt übertragen (0.5.1)
        // (nur prüfen, wenn sonst wirklich übertragen würde – sonst würde bei Einweg-Profilen jeder Lauf neu hashen)
        val wouldTransfer = if (changedL) dir.up else dir.down
        if (wouldTransfer && l.size == r.size && sameContent(l, rel, r)) return Action(k, rel, Kind.RECORD)
        // Nur eine Seite geändert: Versionsschutz (0.5.0) -> die überschriebene Version kommt in den Papierkorb
        if (changedL) return if (dir.up) Action(k, rel, Kind.UPLOAD, trashOther = profile.keepVersions) else null
        return if (dir.down) Action(k, rel, Kind.DOWNLOAD, trashOther = profile.keepVersions) else null
    }

    /**
     * Gleicher Inhalt? Nur beim Erstabgleich und bei auf beiden Seiten geänderten Dateien nötig, darum
     * gründlich (0.4.1): bis 32 MB die ganze Datei, darüber 32 Stichproben (PartialHash.thorough).
     */
    private fun sameContent(l: LocalEntry, rel: String, r: RemoteEntry): Boolean {
        val s = smb ?: return false
        if (l.size != r.size) return false
        return tree.contentHash(l.docId, l.size) == s.contentHash(remotePath(rel), r.size)
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
        val msg = if (dryRun) str(R.string.eng_would_stop, MirrorGuard.title(g))
        else str(R.string.eng_waiting_decision, MirrorGuard.title(g))
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
                tree.moveToTrash(a.rel, l, today); trashed.incrementAndGet()
                dao.deleteMirrorState(profile.id, a.key)
                log("I", str(R.string.eng_trash_local, a.rel))
            }
            Kind.TRASH_REMOTE -> {
                trashRemote(s, a.rel)
                dao.deleteMirrorState(profile.id, a.key)
                log("I", str(R.string.eng_trash_remote, a.rel))
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
                log("W", str(R.string.eng_conflict, a.rel))
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
        log("I", plural(R.plurals.eng_transfer_mirror, ordered.size, ordered.size, parallel))
        emitShared(sh, null, force = true)
        coroutineScope {
            repeat(parallel) { launch { transferWorker(queue, sh) } }
        }
    }

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
                    log("W", str(R.string.eng_no_space, a.rel))
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
                // Während des Laufs auf dem PC geändert? Dann nicht überschreiben – der nächste Lauf entscheidet
                if (!unchanged(s.stat(remotePath(s, a.rel)), r)) return skipChanged(a.rel, str(R.string.eng_on_pc))
                if (a.trashOther && r != null) {
                    // Zuerst den Sync-Stand vergessen: Scheitert der Upload, gilt die Datei beim nächsten Lauf
                    // als neu (wird übertragen) statt als "auf dem PC gelöscht" (würde sonst auch das Handy löschen)
                    dao.deleteMirrorState(profile.id, a.key)
                    trashRemote(s, a.rel)
                }
                upload(s, a.key, a.rel, l, onBytes)
            }
            Kind.DOWNLOAD -> {
                r ?: return
                // Vorhandene Handy-Datei seit dem Einlesen geändert? (Neu entstandene prüft writeFileResumable)
                if (l != null) {
                    // Per Ordnerliste lesen wie beim Einlesen (stat() liefert bei manchen Speicherorten andere Werte)
                    val nowLocal = try { tree.find(a.rel) } catch (e: Exception) { null }
                    if (!unchanged(nowLocal, l)) return skipChanged(a.rel, str(R.string.eng_on_phone))
                }
                if (a.trashOther && l != null) {
                    dao.deleteMirrorState(profile.id, a.key)
                    tree.moveToTrash(a.rel, l, today); trashed.incrementAndGet()
                }
                download(s, a.key, a.rel, r, onBytes)
            }
            Kind.KEEP_BOTH -> {
                if (l == null || r == null) return
                // Handy-Version umbenennen und hochladen, PC-Version unter Originalnamen holen
                val parent = a.rel.substringBeforeLast('/', "")
                val name = a.rel.substringAfterLast('/')
                val dot = name.lastIndexOf('.')
                val newName = if (dot > 0) name.substring(0, dot) + CONFLICT_SUFFIX + name.substring(dot) else name + CONFLICT_SUFFIX
                val newRel = if (parent.isEmpty()) newName else "$parent/$newName"
                val newDoc = tree.rename(l.docId, newName)
                val ls = (try { tree.find(newRel) } catch (e: Exception) { null }) ?: tree.stat(newDoc)
                    ?: throw IOException(str(R.string.eng_err_renamed_missing))
                dao.deleteMirrorState(profile.id, a.key)
                upload(s, newRel.lowercase(), newRel, ls, onBytes)
                download(s, a.key, a.rel, r) { b -> onBytes(ls.size + b) }
            }
            else -> {} // Phase A
        }
    }

    private suspend fun upload(s: SmbSession, key: String, rel: String, l: LocalEntry, onBytes: (Long) -> Unit) {
        val path = remotePath(s, rel)
        s.ensureDir(path.substringBeforeLast('\\', ""))
        val ctx = currentCoroutineContext()
        s.upload(
            finalPath = path, size = l.size,
            openInput = { off -> tree.openInput(l.docId, off) },
            onProgress = onBytes, checkCancel = { ctx.ensureActive() },
            modifiedMillis = l.modified, replaceExisting = true,
            // Fortsetzen einer angefangenen .s2ptmp nur mit Prüfung gegen die Handy-Datei (0.4.1)
            expectedHash = { PartialHash.compute(l.size) { off, len -> tree.readAt(l.docId, off, len) } },
        )
        val rs = s.stat(path) ?: throw IOException(str(R.string.eng_err_after_upload_missing))
        if (rs.size != l.size) throw IOException(str(R.string.eng_err_after_upload_size))
        record(key, l, rs)
        up.incrementAndGet(); bytes.addAndGet(l.size)
        log("I", "↑ $rel")
    }

    private suspend fun download(s: SmbSession, key: String, rel: String, r: RemoteEntry, onBytes: (Long) -> Unit) {
        val ctx = currentCoroutineContext()
        // Abgebrochene Downloads setzen an der Abbruchstelle fort (angefangene .s2ptmp auf dem Handy)
        // Gab es die Datei beim Einlesen nicht, ist sie inzwischen aber auf dem Handy entstanden -> nicht überschreiben
        val expectNew = local[key] == null
        val (docId, resumedAt) = try {
            tree.writeFileResumable(rel, r.size, r.modified, expectNew = expectNew) { out, offset ->
                s.download(remotePath(s, rel), out, onBytes, { ctx.ensureActive() }, startOffset = offset)
            }
        } catch (e: ChangedDuringRunException) {
            return skipChanged(rel, str(R.string.eng_on_phone))
        }
        // Stand so lesen wie beim nächsten Einlesen (Ordnerliste statt Einzelabfrage): Manche Speicherorte (z. B.
        // "Downloads") liefern sonst andere Zeitstempel -> der nächste Lauf hielte die Datei für geändert (0.5.1)
        val ls = (try { tree.find(rel) } catch (e: Exception) { null }) ?: tree.stat(docId)
            ?: throw IOException(str(R.string.eng_err_after_download_missing))
        if (ls.size != r.size) throw IOException(str(R.string.eng_err_after_download_size))
        record(key, ls, r)
        down.incrementAndGet(); bytes.addAndGet(r.size)
        log("I", "↓ $rel" + if (resumedAt > 0) " " + str(R.string.eng_resumed_at, com.sync2pc.app.sync.Notifications.formatBytes(resumedAt)) else "")
    }

    /**
     * Vorgemerkte Wiederherstellungen ausführen (0.5.0): aktuelle Version auf beiden Seiten in den Papierkorb,
     * alte Version an den ursprünglichen Ort zurück und auf die andere Seite kopieren, Sync-Stand setzen.
     * Netzwerkfehler -> Vormerkung bleibt, der Lauf wird wie üblich unterbrochen. Liefert true, wenn etwas geschah.
     */
    private suspend fun runRestores(s: SmbSession): Boolean {
        val failures = RestoreFailures(context)
        // Nach 5 Fehlversuchen nicht mehr automatisch wiederholen (0.11.8, DEC-146): Vormerkung bleibt bestehen
        // (schützt die Papierkorb-Version), der Benutzer wählt auf der Startseite "Erneut versuchen" oder "Verwerfen"
        val list = dao.pendingRestores(profile.id).filterNot { failures.isExhausted(it.id) }
        if (list.isEmpty()) return false
        log("I", plural(R.plurals.eng_restoring, list.size))
        val oldStates = dao.mirrorStates(profile.id).associateBy { it.relPath }
        for (r in list) {
            currentCoroutineContext().ensureActive()
            val key = r.origPath.lowercase()
            val fromPc = r.side == Side.REMOTE.name
            val trashFull = s.path(r.trashPath.replace('/', '\\'))
            val dest = remotePath(s, r.origPath)
            // Für das Zurückrollen (0.7.0): wohin die aktuellen Versionen gingen, ob die alte schon am Ziel liegt
            var localTrashed: String? = null
            var remoteTrashed: String? = null
            var sourceMoved = false
            try {
                val localSource = if (fromPc) null else tree.find(r.trashPath)
                val remoteSource = if (fromPc) s.stat(trashFull) else null
                if (localSource == null && remoteSource == null) {
                    log("W", str(R.string.eng_restore_gone, r.origPath))
                    dao.deleteRestore(r.id); failures.clear(r.id)
                    continue
                }
                // 1. Aktuelle Versionen (falls vorhanden) in den Papierkorb – so ist auch das Wiederherstellen umkehrbar
                dao.deleteMirrorState(profile.id, key)
                tree.find(r.origPath)?.let { localTrashed = tree.moveToTrash(r.origPath, it, today); trashed.incrementAndGet() }
                if (s.stat(dest) != null) remoteTrashed = trashRemote(s, r.origPath)
                // 2. Alte Version zurück an ihren Ort, 3. auf die andere Seite kopieren (setzt den Sync-Stand)
                if (localSource != null) {
                    val id = tree.moveFile(r.trashPath, localSource, r.origPath)
                    sourceMoved = true
                    // Per Ordnerliste lesen wie beim Einlesen (stat() liefert bei manchen Speicherorten andere Werte, 0.7.0)
                    val l = (try { tree.find(r.origPath) } catch (e: Exception) { null }) ?: tree.stat(id)
                        ?: throw IOException(str(R.string.eng_err_restored_missing))
                    upload(s, key, r.origPath, l) {}
                } else {
                    s.ensureDir(dest.substringBeforeLast('\\', ""))
                    s.move(trashFull, dest)
                    sourceMoved = true
                    val re = s.stat(dest) ?: throw IOException(str(R.string.eng_err_restored_missing))
                    download(s, key, r.origPath, re) {}
                }
                dao.deleteConflict(profile.id, r.origPath)
                dao.deleteRestore(r.id); failures.clear(r.id)
                log("I", str(R.string.eng_restored, r.origPath, r.trashPath.split('/').getOrNull(1) ?: "?"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Zurückrollen: alte Version zurück in den Papierkorb, aktuelle Versionen an ihren Platz (0.7.0)
                val rolledBack = rollbackRestore(s, r.origPath, r.trashPath, trashFull, dest, fromPc, sourceMoved, localTrashed, remoteTrashed)
                if (rolledBack) oldStates[key]?.let { dao.upsertMirrorState(it) }
                if (isNetworkError(e)) { connectionFailed = true; throw e } // bleibt vorgemerkt
                failed.incrementAndGet()
                // Vormerkung bleiben lassen: nächster Sync versucht es erneut, die Startseite zeigt einen Hinweis
                val attempts = failures.mark(profile.id, r.id)
                val err = com.sync2pc.app.sync.shortError(e)
                log("E", when {
                    !rolledBack -> str(R.string.eng_restore_failed_partial, r.origPath, err)
                    RestoreRetryPolicy.exhausted(attempts) ->
                        str(R.string.eng_restore_gave_up, r.origPath, err, RestoreRetryPolicy.MAX_ATTEMPTS)
                    else -> str(R.string.eng_restore_failed_rolled_back, r.origPath, err)
                })
            }
        }
        return true
    }

    /** Ein fehlgeschlagenes Wiederherstellen rückgängig machen. true = alles wieder wie vorher */
    private fun rollbackRestore(
        s: SmbSession, origPath: String, trashPath: String, trashFull: String, dest: String, fromPc: Boolean,
        sourceMoved: Boolean, localTrashed: String?, remoteTrashed: String?,
    ): Boolean {
        var ok = true
        fun step(what: String, block: () -> Unit) {
            try { block() } catch (e: Exception) { ok = false; android.util.Log.w("MirrorEngine", "Zurückrollen $what", e) }
        }
        if (sourceMoved) {
            if (fromPc) step("PC-Quelle") { if (s.stat(dest) != null && s.stat(trashFull) == null) s.move(dest, trashFull) }
            else step("Handy-Quelle") {
                val back = tree.find(origPath)
                if (back != null && tree.find(trashPath) == null) tree.moveFile(origPath, back, trashPath)
            }
        }
        remoteTrashed?.let { t -> step("PC-Version") { if (s.stat(dest) == null) s.move(t, dest) else ok = false } }
        localTrashed?.let { t ->
            step("Handy-Version") {
                val e = tree.find(t)
                if (e != null && tree.find(origPath) == null) tree.moveFile(t, e, origPath) else ok = false
            }
        }
        return ok
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
            log("I", str(R.string.eng_purged_pc, dirs, n))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e)) { connectionFailed = true; throw e } // bleibt vorgemerkt
            store.clear(profile.id)
            log("E", str(R.string.eng_purge_pc_failed, com.sync2pc.app.sync.shortError(e)))
        }
    }

    /** Datei wurde seit dem Einlesen geändert -> nicht überschreiben; der nächste Lauf sieht beide Stände */
    private suspend fun skipChanged(rel: String, where: String) {
        log("W", str(R.string.eng_changed_during_run, rel, where))
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
            n += tree.purgeTrash(rule, TrashPurge.reservedPaths(pending, Side.LOCAL))
        } catch (e: Exception) {
            log("W", str(R.string.eng_purge_local_failed, com.sync2pc.app.sync.shortError(e)))
        }
        val s = smb
        if (s != null) {
            try {
                n += TrashCleaner.purgeRemote(s, rule, TrashPurge.reservedPaths(pending, Side.REMOTE))
            } catch (e: Exception) {
                if (isNetworkError(e)) throw e
                log("W", str(R.string.eng_purge_remote_failed, com.sync2pc.app.sync.shortError(e)))
            }
        }
        if (n > 0) log("I", str(R.string.eng_purged_expired, n, days))
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

    private suspend fun finish(status: String, message: String): SyncRun {
        run = run.copy(
            finishedAt = System.currentTimeMillis(), status = status, message = message,
            uploaded = up.get() + down.get(), skipped = 0, failed = failed.get(), waiting = newConflicts, bytes = bytes.get()
        )
        dao.updateRun(run)
        if (!dryRun) log("I", str(R.string.eng_end, message))
        return run
    }

    private fun emit(phase: String, done: Int, total: Int, doneBytes: Long, totalBytes: Long, file: String?) =
        onProgress(SyncProgress(profile.name, phase, done, total, doneBytes, totalBytes, file))

    private fun isNetworkError(e: Throwable): Boolean = com.sync2pc.app.sync.isNetworkError(e)
}
