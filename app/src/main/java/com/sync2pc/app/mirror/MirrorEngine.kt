package com.sync2pc.app.mirror

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import com.sync2pc.app.data.AppDatabase
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
import com.sync2pc.app.sync.FolderNames
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
        const val TRASH_DIR = ".LeoSync-Papierkorb"
        /** Papierkorb-Name bis 0.0.11 – wird weiterhin nie synchronisiert */
        const val OLD_TRASH_DIR = ".Sync2PC-Papierkorb"
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

    override suspend fun run(): SyncRun {
        run = run.copy(id = dao.insertRun(run))
        var status = "OK"
        var message = ""
        var massWarn: String? = null
        try {
            if (!dryRun) {
                val day = 24L * 3600 * 1000
                val now = System.currentTimeMillis()
                dao.pruneLogs(now - 30 * day); dao.pruneRuns(now - 90 * day)
            }
            log("I", "Start ($trigger) – Ordner-Sync '${profile.name}' (${dir.label})")
            // Offene Entscheidung der Schutzbremse -> nichts tun, bis der Benutzer entschieden hat
            if (!dryRun) guards.get(profile.id)?.let { return block(it) }
            emit("Lese Ordner…", 0, 0, 0, 0, null)

            // --- 1. Beide Seiten einlesen ---------------------------------------
            val treeUri = Uri.parse(profile.localTreeUri)
            if (!LocalTree.hasPermission(context, treeUri))
                throw IOException("Kein Zugriff mehr auf den Handy-Ordner – bitte im Profil neu auswählen")
            tree = LocalTree(context, treeUri)
            var states = dao.mirrorStates(profile.id).associateBy { it.relPath }
            // Handy-Ordner gelöscht oder verschoben? (Schutzbremse statt "alles gelöscht")
            val localRootOk = try { tree.stat(tree.rootDocId) != null } catch (e: Exception) { false }
            if (!localRootOk) {
                if (states.isNotEmpty()) return block(Guard(profile.id, GuardKind.LOCAL_MISSING))
                throw IOException("Handy-Ordner nicht gefunden – bitte im Profil neu auswählen")
            }
            val filter = FileFilter(profile.includePatterns, profile.excludePatterns)
            val skipTrash: (String) -> Boolean = { it.equals(TRASH_DIR, ignoreCase = true) || it.equals(OLD_TRASH_DIR, ignoreCase = true) }
            smb = try {
                SmbSession.open(profile)
            } catch (e: Exception) {
                if (dryRun) { message = "Offline-Vorschau (Share nicht erreichbar): ${e.message}"; null }
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
            if (!dryRun && s != null && guards.pendingCheck(profile.id).isEmpty() && runRestores(s))
                states = dao.mirrorStates(profile.id).associateBy { it.relPath }

            // recover: liegengebliebene ".s2pold" aufräumen bzw. zurückholen (nicht in der Simulation)
            local = tree.listRecursive(skipTrash, recover = !dryRun).filterKeys { filter.accepts(it) }
                .entries.associate { it.key.lowercase() to (it.key to it.value) }
            if (s != null) {
                remote = if (baseExists) s.listRecursive(s.path(), skipTrash).filterKeys { filter.accepts(it) }
                    .entries.associate { it.key.lowercase() to (it.key to it.value) } else emptyMap()
            }
            val conflicts = dao.conflicts(profile.id).associateBy { it.relPath.lowercase() }
            log("I", "Handy: ${local.size} Dateien, PC: ${remote.size} Dateien")

            // Nach einem Ordnerwechsel: Stichprobe, ob der neue Ordner zum Sync-Stand passt
            val pending = guards.pendingCheck(profile.id)
            if (pending.isNotEmpty() && s != null) {
                if (states.isNotEmpty() && !folderMatches(pending, states.values.toList()))
                    return block(Guard(profile.id, GuardKind.CHECK_FOLDER))
                if (!dryRun) guards.setPendingCheck(profile.id, emptySet())
            }
            // "Nicht löschen" bei der Schutzbremse: dieser Lauf übernimmt keine Löschungen
            if (!dryRun && guards.takeKeepFiles(profile.id)) {
                propagate = false
                log("I", "Löschungen werden in diesem Lauf nicht übernommen (deine Entscheidung)")
            }
            if (!dryRun) purgeTrash()

            // --- 2. Planen -------------------------------------------------------
            emit("Vergleiche…", 0, 0, 0, 0, null)
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
                massWarn = "⚠ Der Lauf würde anhalten und nachfragen: " + (
                    if (g.folder.isNotEmpty()) "Der Ordner „${g.folder}“ käme ganz in den Papierkorb"
                    else "$toTrash von ${states.size} Dateien kämen in den Papierkorb")
            }
            if (!dryRun) for (p in doneConflicts) dao.deleteConflict(profile.id, p)

            // --- 3. Ausführen -----------------------------------------------------
            // Phase A (nacheinander): Verwaltung – Papierkorb, Stand merken, Konflikte, Aufräumen
            // Phase B (parallel):     Übertragungen, kleine Dateien zuerst
            val transfers = actions.filter { it.kind == Kind.UPLOAD || it.kind == Kind.DOWNLOAD || it.kind == Kind.KEEP_BOTH }
            if (dryRun) {
                for (a in actions) { if (preview.size >= previewLimit) break; previewOf(a) }
                val visible = actions.count { it.kind != Kind.RECORD && it.kind != Kind.DROP }
                if (message.isEmpty()) message = "$visible Aktionen" +
                    (if (preview.size < visible) " – die ersten ${preview.size} simuliert" else " simuliert")
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
                    status = "PARTIAL"; message = "Abgebrochen: WLAN nicht mehr verbunden"
                    log("W", message)
                }
            }

            if (status == "OK" && (failed.get() > 0 || spaceShortage)) status = "PARTIAL"
            if (message.isEmpty()) message = com.sync2pc.app.sync.RunSummary.mirror(
                up.get(), down.get(), trashed.get(), newConflicts, failed.get(), spaceShortage
            )
            return finish(status, message)
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val reason = interruptReason()
                if (reason != null) {
                    log("W", "Unterbrochen ($reason) – wird automatisch fortgesetzt")
                    finish("INTERRUPTED", "Unterbrochen ($reason) – ↑ ${up.get()} / ↓ ${down.get()} übertragen, Rest folgt automatisch")
                } else {
                    log("W", "Pausiert (von dir) – läuft beim nächsten automatischen Start weiter")
                    finish("CANCELLED", "Pausiert (von dir) – läuft beim nächsten automatischen Start weiter")
                }
            }
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e)) {
                connectionFailed = true
                log("W", "Verbindung unterbrochen (${com.sync2pc.app.sync.shortError(e)}) – wird beim nächsten Sync fortgesetzt")
                return finish("INTERRUPTED", "Verbindung unterbrochen – ↑ ${up.get()} / ↓ ${down.get()} übertragen, Rest folgt beim nächsten Sync")
            }
            log("E", "Lauf fehlgeschlagen: ${com.sync2pc.app.sync.shortError(e)}")
            return finish("FAILED", com.sync2pc.app.sync.shortError(e))
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
        val msg = if (dryRun) "⚠ Der Lauf würde anhalten und nachfragen: ${MirrorGuard.title(g)}"
        else "Wartet auf deine Entscheidung: ${MirrorGuard.title(g)}"
        if (!dryRun) { guards.set(g); blocked = true }
        log("W", msg)
        return finish("WAITING", msg)
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
                log("I", "🗑 Handy: ${a.rel} (auf dem PC gelöscht)")
            }
            Kind.TRASH_REMOTE -> {
                trashRemote(s, a.rel)
                dao.deleteMirrorState(profile.id, a.key)
                log("I", "🗑 PC: ${a.rel} (auf dem Handy gelöscht)")
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
                log("W", "⚠ Konflikt: ${a.rel} wurde auf Handy und PC geändert")
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
        log("I", "Übertrage ${ordered.size} Dateien mit $parallel parallelen Verbindungen")
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
                    log("W", "Zu wenig Speicher auf dem Handy – übersprungen: ${a.rel}")
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
                if (!unchanged(s.stat(remotePath(s, a.rel)), r)) return skipChanged(a.rel, "auf dem PC")
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
                    if (!unchanged(nowLocal, l)) return skipChanged(a.rel, "auf dem Handy")
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
                val newName = if (dot > 0) name.substring(0, dot) + "_Konflikt-Handy" + name.substring(dot) else name + "_Konflikt-Handy"
                val newRel = if (parent.isEmpty()) newName else "$parent/$newName"
                val newDoc = tree.rename(l.docId, newName)
                val ls = tree.stat(newDoc) ?: throw IOException("Umbenannte Datei nicht gefunden")
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
        val rs = s.stat(path) ?: throw IOException("Datei nach Upload nicht gefunden")
        if (rs.size != l.size) throw IOException("Grösse nach Upload stimmt nicht")
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
            return skipChanged(rel, "auf dem Handy")
        }
        // Stand so lesen wie beim nächsten Einlesen (Ordnerliste statt Einzelabfrage): Manche Speicherorte (z. B.
        // "Downloads") liefern sonst andere Zeitstempel -> der nächste Lauf hielte die Datei für geändert (0.5.1)
        val ls = (try { tree.find(rel) } catch (e: Exception) { null }) ?: tree.stat(docId)
            ?: throw IOException("Datei nach Download nicht gefunden")
        if (ls.size != r.size) throw IOException("Grösse nach Download stimmt nicht")
        record(key, ls, r)
        down.incrementAndGet(); bytes.addAndGet(r.size)
        log("I", "↓ $rel" + if (resumedAt > 0) " (fortgesetzt ab ${com.sync2pc.app.sync.Notifications.formatBytes(resumedAt)})" else "")
    }

    /**
     * Vorgemerkte Wiederherstellungen ausführen (0.5.0): aktuelle Version auf beiden Seiten in den Papierkorb,
     * alte Version an den ursprünglichen Ort zurück und auf die andere Seite kopieren, Sync-Stand setzen.
     * Netzwerkfehler -> Vormerkung bleibt, der Lauf wird wie üblich unterbrochen. Liefert true, wenn etwas geschah.
     */
    private suspend fun runRestores(s: SmbSession): Boolean {
        val list = dao.pendingRestores(profile.id)
        if (list.isEmpty()) return false
        log("I", "Stelle ${list.size} Datei(en) aus dem Papierkorb wieder her")
        for (r in list) {
            currentCoroutineContext().ensureActive()
            val key = r.origPath.lowercase()
            try {
                val fromPc = r.side == Side.REMOTE.name
                val trashFull = s.path(r.trashPath.replace('/', '\\'))
                val localSource = if (fromPc) null else tree.find(r.trashPath)
                val remoteSource = if (fromPc) s.stat(trashFull) else null
                if (localSource == null && remoteSource == null) {
                    log("W", "↺ ${r.origPath}: Version im Papierkorb nicht mehr vorhanden – übersprungen")
                    dao.deleteRestore(r.id)
                    continue
                }
                // 1. Aktuelle Versionen (falls vorhanden) in den Papierkorb – so ist auch das Wiederherstellen umkehrbar
                dao.deleteMirrorState(profile.id, key)
                tree.find(r.origPath)?.let { tree.moveToTrash(r.origPath, it, today); trashed.incrementAndGet() }
                if (s.stat(remotePath(s, r.origPath)) != null) trashRemote(s, r.origPath)
                // 2. Alte Version zurück an ihren Ort, 3. auf die andere Seite kopieren (setzt den Sync-Stand)
                if (localSource != null) {
                    val id = tree.moveFile(r.trashPath, localSource, r.origPath)
                    val l = tree.stat(id) ?: throw IOException("Wiederhergestellte Datei nicht gefunden")
                    upload(s, key, r.origPath, l) {}
                } else {
                    val dest = remotePath(s, r.origPath)
                    s.ensureDir(dest.substringBeforeLast('\\', ""))
                    s.move(trashFull, dest)
                    val re = s.stat(dest) ?: throw IOException("Wiederhergestellte Datei nicht gefunden")
                    download(s, key, r.origPath, re) {}
                }
                dao.deleteConflict(profile.id, r.origPath)
                dao.deleteRestore(r.id)
                log("I", "↺ ${r.origPath} wiederhergestellt (Version vom ${r.trashPath.split('/').getOrNull(1) ?: "?"})")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isNetworkError(e)) { connectionFailed = true; throw e } // bleibt vorgemerkt
                failed.incrementAndGet()
                log("E", "✗ Wiederherstellen ${r.origPath}: ${com.sync2pc.app.sync.shortError(e)}")
                dao.deleteRestore(r.id)
            }
        }
        return true
    }

    /** Datei wurde seit dem Einlesen geändert -> nicht überschreiben; der nächste Lauf sieht beide Stände */
    private suspend fun skipChanged(rel: String, where: String) {
        log("W", "⏭ $rel: wurde während des Laufs $where geändert – folgt beim nächsten Sync")
    }

    /** PC-Datei nach "<Basis>\.LeoSync-Papierkorb\<Datum>\<Pfad>" verschieben */
    private fun trashRemote(s: SmbSession, rel: String) {
        val parent = rel.substringBeforeLast('/', "").replace('/', '\\')
        val trashDir = s.path(listOf(TRASH_DIR, today, parent).filter { it.isNotEmpty() }.joinToString("\\"))
        s.ensureDir(trashDir)
        val orig = rel.substringAfterLast('/')
        var name = orig
        var n = 2
        while (s.stat("$trashDir\\$name") != null) name = FolderNames.conflictName(orig, n++)
        s.move(remotePath(s, rel), "$trashDir\\$name")
        trashed.incrementAndGet()
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
        emit("Synchronisiere…", sh.done.get(), sh.totalFiles, b, sh.totalBytes, file)
    }

    /** Abgelaufene Datumsordner im Papierkorb auf beiden Seiten endgültig löschen */
    private suspend fun purgeTrash() {
        val days = profile.trashRetentionDays
        if (days <= 0) return
        val cutoff = TrashPolicy.cutoff(days)
        var n = 0
        try {
            n += tree.purgeTrash(cutoff)
        } catch (e: Exception) {
            log("W", "Papierkorb auf dem Handy nicht geleert: ${com.sync2pc.app.sync.shortError(e)}")
        }
        val s = smb
        if (s != null) {
            try {
                for (t in listOf(TRASH_DIR, OLD_TRASH_DIR)) {
                    val base = s.path(t)
                    for (e in s.listDir(base)) {
                        if (e.isDirectory && TrashPolicy.isExpired(e.name, cutoff)) { s.deleteDir("$base\\${e.name}"); n++ }
                    }
                }
            } catch (e: Exception) {
                if (isNetworkError(e)) throw e
                log("W", "Papierkorb auf dem PC nicht geleert: ${com.sync2pc.app.sync.shortError(e)}")
            }
        }
        if (n > 0) log("I", "🗑 Papierkorb: $n Ordner älter als $days Tage endgültig gelöscht")
    }

    private suspend fun record(key: String, l: LocalEntry, r: RemoteEntry) {
        dao.upsertMirrorState(
            MirrorState(profile.id, key, l.size, l.modified, r.size, r.modified, System.currentTimeMillis())
        )
    }

    private fun previewOf(a: Action) {
        val label = when (a.kind) {
            Kind.UPLOAD -> if (a.trashOther) "↑ Handy → PC (PC-Version in Papierkorb)" else "↑ Handy → PC"
            Kind.DOWNLOAD -> if (a.trashOther) "↓ PC → Handy (Handy-Version in Papierkorb)" else "↓ PC → Handy"
            Kind.TRASH_LOCAL -> "🗑 Auf dem Handy in den Papierkorb"
            Kind.TRASH_REMOTE -> "🗑 Auf dem PC in den Papierkorb"
            Kind.CONFLICT -> "⚠ Konflikt – Entscheidung nötig"
            Kind.KEEP_BOTH -> "⇄ Beide Versionen behalten"
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
        if (!dryRun) log("I", "Ende: $message")
        return run
    }

    private fun emit(phase: String, done: Int, total: Int, doneBytes: Long, totalBytes: Long, file: String?) =
        onProgress(SyncProgress(profile.name, phase, done, total, doneBytes, totalBytes, file))

    private fun isNetworkError(e: Throwable): Boolean = com.sync2pc.app.sync.isNetworkError(e)
}
