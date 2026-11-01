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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
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
 * Ersetzte oder gelöschte Dateien landen immer in ".LeoSync-Papierkorb/<Datum>/…" – nie endgültig weg.
 */
class MirrorEngine(
    private val context: Context,
    private val profile: Profile,
    private val trigger: String,
    private val dryRun: Boolean = false,
    private val shouldContinue: () -> Boolean = { true },
    private val onProgress: (SyncProgress) -> Unit = {},
) : SyncRunner {

    companion object {
        const val TRASH_DIR = ".LeoSync-Papierkorb"
        /** Papierkorb-Name bis 0.0.11 – wird weiterhin nie synchronisiert */
        const val OLD_TRASH_DIR = ".Sync2PC-Papierkorb"
        /** Mindestens so viel Speicher frei lassen */
        private const val RESERVE_BYTES = 300L * 1024 * 1024
    }

    override val preview = ArrayList<PreviewEntry>()
    override var connectionFailed = false
        private set
    override var wifiLost = false
        private set
    override var newConflicts = 0
        private set
    override var spaceShortage = false
        private set

    private val dao = AppDatabase.get(context).dao()
    private val dir: SyncDirection = profile.dir()
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

    private var up = 0; private var down = 0; private var failed = 0; private var bytes = 0L; private var trashed = 0

    override suspend fun run(): SyncRun {
        run = run.copy(id = dao.insertRun(run))
        var status = "OK"
        var message = ""
        try {
            if (!dryRun) {
                val day = 24L * 3600 * 1000
                val now = System.currentTimeMillis()
                dao.pruneLogs(now - 30 * day); dao.pruneRuns(now - 90 * day)
            }
            log("I", "Start ($trigger) – Ordner-Sync '${profile.name}' (${dir.label})")
            emit("Lese Ordner…", 0, 0, 0, 0, null)

            // --- 1. Beide Seiten einlesen ---------------------------------------
            val treeUri = Uri.parse(profile.localTreeUri)
            if (!LocalTree.hasPermission(context, treeUri))
                throw IOException("Kein Zugriff mehr auf den Handy-Ordner – bitte im Profil neu auswählen")
            tree = LocalTree(context, treeUri)
            val filter = FileFilter(profile.includePatterns, profile.excludePatterns)
            val skipTrash: (String) -> Boolean = { it.equals(TRASH_DIR, ignoreCase = true) || it.equals(OLD_TRASH_DIR, ignoreCase = true) }
            local = tree.listRecursive(skipTrash).filterKeys { filter.accepts(it) }
                .entries.associate { it.key.lowercase() to (it.key to it.value) }

            smb = try {
                SmbSession.open(profile)
            } catch (e: Exception) {
                if (dryRun) { message = "Offline-Vorschau (Share nicht erreichbar): ${e.message}"; null }
                else { if (isNetworkError(e)) connectionFailed = true; throw e }
            }
            val s = smb
            if (s != null) {
                if (!dryRun) s.ensureDir(s.path())
                remote = s.listRecursive(s.path(), skipTrash).filterKeys { filter.accepts(it) }
                    .entries.associate { it.key.lowercase() to (it.key to it.value) }
            }
            val states = dao.mirrorStates(profile.id).associateBy { it.relPath }
            val conflicts = dao.conflicts(profile.id).associateBy { it.relPath.lowercase() }
            log("I", "Handy: ${local.size} Dateien, PC: ${remote.size} Dateien")

            // --- 2. Planen -------------------------------------------------------
            emit("Vergleiche…", 0, 0, 0, 0, null)
            val actions = ArrayList<Action>()
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
                        if (!dryRun) dao.deleteConflict(profile.id, c.relPath)
                        continue
                    }
                    // Eine Seite ist inzwischen weg -> Konflikt erledigt, normal weiter
                    if (!dryRun) dao.deleteConflict(profile.id, c.relPath)
                }
                plan(k, rel, l?.second, r?.second, st)?.let { actions += it }
            }

            // --- 3. Ausführen -----------------------------------------------------
            val transfers = actions.filter { it.kind == Kind.UPLOAD || it.kind == Kind.DOWNLOAD || it.kind == Kind.KEEP_BOTH }
            val totalBytes = transfers.sumOf { a ->
                when (a.kind) {
                    Kind.UPLOAD -> local[a.key]?.second?.size ?: 0
                    Kind.DOWNLOAD -> remote[a.key]?.second?.size ?: 0
                    else -> (local[a.key]?.second?.size ?: 0) + (remote[a.key]?.second?.size ?: 0)
                }
            }
            var budget = freeSpace() - RESERVE_BYTES
            var done = 0
            for ((index, a) in actions.withIndex()) {
                currentCoroutineContext().ensureActive()
                if (dryRun) { previewOf(a); continue }
                if (index % 10 == 0 && !shouldContinue()) {
                    wifiLost = true; status = "PARTIAL"; message = "Abgebrochen: WLAN nicht mehr verbunden"
                    log("W", message); break
                }
                try {
                    val needs = if (a.kind == Kind.DOWNLOAD || a.kind == Kind.KEEP_BOTH) remote[a.key]?.second?.size ?: 0 else 0
                    if (needs > 0 && needs > budget) {
                        spaceShortage = true
                        log("W", "Zu wenig Speicher auf dem Handy – übersprungen: ${a.rel}")
                        continue
                    }
                    budget -= needs
                    val base = bytes
                    execute(a) { b -> emit("Synchronisiere…", done, transfers.size, base + b, totalBytes, a.rel) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Verbindungsabbruch ist erwartbar -> Lauf unterbrechen, kein Datei-Fehler
                    if (isNetworkError(e)) { connectionFailed = true; throw e }
                    failed++
                    log("E", "✗ ${a.rel}: ${com.sync2pc.app.sync.shortError(e)}")
                }
                if (a in transfers) { done++; emit("Synchronisiere…", done, transfers.size, bytes, totalBytes, null) }
            }

            if (status == "OK" && (failed > 0 || spaceShortage)) status = "PARTIAL"
            if (message.isEmpty()) message = buildString {
                append("↑ $up hochgeladen, ↓ $down heruntergeladen")
                if (trashed > 0) append(", $trashed in Papierkorb")
                if (newConflicts > 0) append(", $newConflicts Konflikt(e)")
                if (failed > 0) append(", $failed Fehler")
                if (spaceShortage) append(", Speicher voll")
            }
            return finish(status, message)
        } catch (e: CancellationException) {
            withContext(NonCancellable) { log("W", "Lauf abgebrochen"); finish("CANCELLED", "Abgebrochen") }
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e)) {
                connectionFailed = true
                log("W", "Verbindung unterbrochen (${com.sync2pc.app.sync.shortError(e)}) – wird beim nächsten Sync fortgesetzt")
                return finish("INTERRUPTED", "Verbindung unterbrochen – ↑ $up / ↓ $down übertragen, Rest folgt beim nächsten Sync")
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
                profile.propagateDeletes -> Action(k, rel, Kind.TRASH_LOCAL)
                dir == SyncDirection.BOTH -> Action(k, rel, Kind.UPLOAD)              // nichts verlieren
                else -> Action(k, rel, Kind.DROP)                                     // PC → Handy: Handy-Kopie behalten
            }
        }
        if (l == null && r != null) {
            if (st == null) return if (dir.down) Action(k, rel, Kind.DOWNLOAD) else null // neu auf dem PC
            return when {
                dir == SyncDirection.DOWNLOAD -> Action(k, rel, Kind.DOWNLOAD)
                profile.propagateDeletes -> Action(k, rel, Kind.TRASH_REMOTE)
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
        if (changedL) return if (dir.up) Action(k, rel, Kind.UPLOAD) else null
        return if (dir.down) Action(k, rel, Kind.DOWNLOAD) else null
    }

    private fun sameContent(l: LocalEntry, rel: String, r: RemoteEntry): Boolean {
        val s = smb ?: return false
        if (l.size != r.size) return false
        val lh = PartialHash.compute(l.size) { off, len -> tree.readAt(l.docId, off, len) }
        val rh = s.partialHash(remotePath(rel), r.size)
        return lh == rh
    }

    // ---- Ausführung ------------------------------------------------------------------
    private fun remotePath(rel: String): String = smb!!.path(rel.replace('/', '\\'))

    private suspend fun execute(a: Action, onBytes: (Long) -> Unit) {
        val l = local[a.key]?.second
        val r = remote[a.key]?.second
        when (a.kind) {
            Kind.UPLOAD -> {
                l ?: return
                if (a.trashOther && r != null) trashRemote(a.rel)
                upload(a.key, a.rel, l, onBytes)
            }
            Kind.DOWNLOAD -> {
                r ?: return
                if (a.trashOther && l != null) { tree.moveToTrash(a.rel, l, today); trashed++ }
                download(a.key, a.rel, r, onBytes)
            }
            Kind.TRASH_LOCAL -> {
                l ?: return
                tree.moveToTrash(a.rel, l, today); trashed++
                dao.deleteMirrorState(profile.id, a.key)
                log("I", "🗑 Handy: ${a.rel} (auf dem PC gelöscht)")
            }
            Kind.TRASH_REMOTE -> {
                trashRemote(a.rel)
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
                upload(newRel.lowercase(), newRel, ls, onBytes)
                download(a.key, a.rel, r, onBytes)
            }
        }
    }

    private suspend fun upload(key: String, rel: String, l: LocalEntry, onBytes: (Long) -> Unit) {
        val s = smb!!
        val path = remotePath(rel)
        s.ensureDir(path.substringBeforeLast('\\', ""))
        val ctx = currentCoroutineContext()
        s.upload(
            finalPath = path, size = l.size,
            openInput = { off -> tree.openInput(l.docId, off) },
            onProgress = onBytes, checkCancel = { ctx.ensureActive() },
            modifiedMillis = l.modified, replaceExisting = true,
        )
        val rs = s.stat(path) ?: throw IOException("Datei nach Upload nicht gefunden")
        if (rs.size != l.size) throw IOException("Grösse nach Upload stimmt nicht")
        record(key, l, rs)
        up++; bytes += l.size
        log("I", "↑ $rel")
    }

    private suspend fun download(key: String, rel: String, r: RemoteEntry, onBytes: (Long) -> Unit) {
        val s = smb!!
        val ctx = currentCoroutineContext()
        val docId = tree.writeFile(rel) { out ->
            s.download(remotePath(rel), out, onBytes) { ctx.ensureActive() }
        }
        val ls = tree.stat(docId) ?: throw IOException("Datei nach Download nicht gefunden")
        if (ls.size != r.size) throw IOException("Grösse nach Download stimmt nicht")
        record(key, ls, r)
        down++; bytes += r.size
        log("I", "↓ $rel")
    }

    /** PC-Datei nach "<Basis>\.LeoSync-Papierkorb\<Datum>\<Pfad>" verschieben */
    private fun trashRemote(rel: String) {
        val s = smb!!
        val parent = rel.substringBeforeLast('/', "").replace('/', '\\')
        val trashDir = s.path(listOf(TRASH_DIR, today, parent).filter { it.isNotEmpty() }.joinToString("\\"))
        s.ensureDir(trashDir)
        val orig = rel.substringAfterLast('/')
        var name = orig
        var n = 2
        while (s.stat("$trashDir\\$name") != null) name = FolderNames.conflictName(orig, n++)
        s.move(remotePath(rel), "$trashDir\\$name")
        trashed++
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
            uploaded = up + down, skipped = 0, failed = failed, waiting = newConflicts, bytes = bytes
        )
        dao.updateRun(run)
        if (!dryRun) log("I", "Ende: $message")
        return run
    }

    private fun emit(phase: String, done: Int, total: Int, doneBytes: Long, totalBytes: Long, file: String?) =
        onProgress(SyncProgress(profile.name, phase, done, total, doneBytes, totalBytes, file))

    private fun isNetworkError(e: Throwable): Boolean = com.sync2pc.app.sync.isNetworkError(e)
}
