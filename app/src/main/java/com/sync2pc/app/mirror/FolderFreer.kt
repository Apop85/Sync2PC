package com.sync2pc.app.mirror

import android.content.Context
import android.net.Uri
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncDirection
import com.sync2pc.app.smb.RemoteStore
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.sync.FreeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * "Speicher freigeben" für Ordner-Profile (0.7.0): Dateien, die nachweislich unverändert auf dem PC liegen, auf dem
 * Handy löschen. Nur Profile Handy → PC ohne "Löschungen übernehmen" – dort übernimmt der Sync das Fehlen auf dem
 * Handy nie (MirrorEngine.plan: DROP, PC-Kopie bleibt; nach "Sync-Status zurücksetzen": kein Download).
 */
object FolderFreer {

    fun eligible(p: Profile): Boolean =
        p.type() == ProfileType.MIRROR && p.dir() == SyncDirection.UPLOAD && !p.propagateDeletes && p.localTreeUri.isNotBlank()

    /** Dateien des Handy-Ordners; gesichert = Sync-Stand passt zur Datei (Grösse + Änderungszeit) */
    suspend fun items(context: Context, profile: Profile): List<FreeItem> = withContext(Dispatchers.IO) {
        val tree = tree(context, profile)
        val filter = FileFilter(profile.includePatterns, profile.excludePatterns)
        val skipTrash: (String) -> Boolean = {
            MirrorEngine.isTrashDir(it)
        }
        val states = AppDatabase.get(context).dao().mirrorStates(profile.id).associateBy { it.relPath }
        tree.listRecursive(skipTrash).filterKeys { filter.accepts(it) }.map { (rel, e) ->
            val st = states[rel.lowercase()]
            FreeItem(
                key = rel, folder = rel.substringBeforeLast('/', ""), name = e.name, size = e.size, time = e.modified,
                backedUp = st != null && st.localSize == e.size && st.localModified == e.modified,
            )
        }
    }

    data class Result(
        val deleted: Int,
        val bytes: Long,
        /** Auf dem PC nicht gefunden, anders oder inzwischen auf dem Handy geändert – bleiben */
        val kept: Int,
        /** PC nicht erreichbar bzw. Verbindung abgebrochen */
        val unreachable: Boolean,
        /** Leer gewordene Ordner auf dem Handy, die entfernt wurden (0.11.8) */
        val foldersRemoved: Int = 0,
        /** Unter der Adresse antwortet ein anderer/noch nicht erkannter PC (keine Anmeldung, nichts gelöscht): verständliche Meldung */
        val rejectedMessage: String? = null,
    )

    /**
     * Ordner, die nie entfernt werden (0.11.8, DEC-147): das Profil-Wurzel, Papierkorb-Ordner und "DCIM/Camera".
     * [rootLabel] = Anzeigename des gewählten Ordners (z. B. "DCIM/Camera" oder "DCIM"), [rel] = relativer Ordnerpfad.
     */
    fun isProtectedDir(rootLabel: String, rel: String): Boolean {
        if (rel.isEmpty()) return true
        if (rel.split('/').any { MirrorEngine.isTrashDir(it) }) return true
        val full = (rootLabel + "/" + rel).replace('\\', '/').lowercase().trimEnd('/')
        return Regex("""(^|[/: ])dcim/camera$""").containsMatchIn(full)
    }

    /**
     * Ordner, die durch das Löschen der Dateien [deletedRels] leer geworden sein können: alle Elternordner, tiefste
     * zuerst (so wird ein leerer Elternordner erst nach seinem leeren Unterordner geprüft), ohne geschützte.
     * Ob sie wirklich leer sind, prüft erst [LocalTree.deleteDirIfEmpty].
     */
    fun emptyDirCandidates(deletedRels: Collection<String>, isProtected: (String) -> Boolean): List<String> {
        val dirs = LinkedHashSet<String>()
        for (f in deletedRels) {
            var d = f.substringBeforeLast('/', "")
            while (d.isNotEmpty()) { dirs += d; d = d.substringBeforeLast('/', "") }
        }
        return dirs.filterNot(isProtected).sortedByDescending { it.count { c -> c == '/' } }
    }

    /**
     * Jede Datei auf dem PC prüfen (vorhanden, gleiche Grösse, gleicher Inhalts-Hash) und erst dann auf dem Handy
     * löschen. Der Sync-Stand der Datei wird vorher entfernt: Die PC-Kopie bleibt so auch, wenn später
     * "Löschungen übernehmen" eingeschaltet wird. Wartet auf einen laufenden Ordner-Sync ([MirrorLock]).
     * [onProgress](geprüft, gesamt)
     */
    suspend fun verifyAndDelete(
        context: Context, profile: Profile, list: List<FreeItem>, onProgress: (Int, Int) -> Unit,
        onStart: () -> Unit = {},
        /** Fortschritt beim Lesen einer grossen Datei: (relativer Pfad, gelesene Bytes beider Seiten, Bytes gesamt) */
        onBytes: (String, Long, Long) -> Unit = { _, _, _ -> },
        /** Öffnet die PC-Sitzung (Standard: echte SMB-Verbindung; in Tests ersetzbar, SYNC-20261111-163) */
        openSession: (Profile) -> RemoteStore = { SmbSession.open(it) },
    ): Result = withContext(Dispatchers.IO) {
        MirrorLock.mutex.withLock {
            onStart() // Sperre erworben: ab jetzt läuft der Eingriff wirklich (0.11.10)
            val dao = AppDatabase.get(context).dao()
            val tree = tree(context, profile)
            var rejected: String? = null
            val s = try { openSession(profile) }
                catch (e: com.sync2pc.app.sync.PcIdentityException) { rejected = e.message; null }
                catch (e: Exception) { null }
                ?: return@withLock Result(0, 0, list.size, unreachable = rejected == null, rejectedMessage = rejected)
            var deleted = 0; var bytes = 0L; var kept = 0; var unreachable = false
            val deletedRels = ArrayList<String>()
            s.use {
                val states = dao.mirrorStates(profile.id).associateBy { it.relPath }
                for ((done, item) in list.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    try {
                        val key = item.rel.lowercase()
                        val e = tree.find(item.rel)
                        val st = states[key]
                        // Seit dem Einlesen geändert oder nicht mehr gesichert -> bleibt
                        if (e == null || e.size != item.size || e.modified != item.time ||
                            st == null || st.localSize != e.size || st.localModified != e.modified) { kept++; continue }
                        val path = s.path(item.rel.replace('/', '\\'))
                        val r = s.stat(path)
                        if (r == null) { kept++; continue }
                        // Nachweislich unverändert = ganze Datei beider Seiten gleich (R-I3, SYNC-20261111-142): auch über
                        // 32 MB kein Stichproben-Hash mehr. Lesefehler -> "nicht gleich" -> Datei bleibt auf dem Handy.
                        // Abbruch (Coroutine) und Netzwerkfehler gehen nach aussen (unten: Abbruch bzw. "nicht erreichbar").
                        val ctx = currentCoroutineContext()
                        val check: () -> Unit = { ctx.ensureActive() }
                        var doneLocal = 0L; var doneRemote = 0L
                        val same = ContentCompare.same(e.size, r.size,
                            { tree.contentHash(e.docId, e.size, check) { b -> doneLocal = b; onBytes(item.rel, doneLocal + doneRemote, e.size * 2) } },
                            { s.contentHash(path, r.size, check) { b -> doneRemote = b; onBytes(item.rel, doneLocal + doneRemote, e.size * 2) } })
                        if (!same) { kept++; continue }
                        dao.deleteMirrorState(profile.id, key)
                        tree.delete(e.docId)
                        deleted++; bytes += e.size; deletedRels += item.rel
                    } catch (ex: kotlinx.coroutines.CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        if (com.sync2pc.app.sync.isNetworkError(ex)) {
                            unreachable = true
                            kept += list.size - done
                            break
                        }
                        kept++
                    }
                    if (done % 5 == 4 || done == list.size - 1) onProgress(done + 1, list.size)
                }
            }
            // Leer gewordene Ordner aufräumen (0.11.8, DEC-147). Nebensache: Fehler hier ändern am Ergebnis nichts,
            // gelöscht wird nur, was beim Prüfen wirklich leer ist (kein Inhalt, nie Wurzel/Papierkorb/DCIM/Camera).
            var folders = 0
            if (deletedRels.isNotEmpty()) {
                val label = LocalTree.label(Uri.parse(profile.localTreeUri))
                for (dir in emptyDirCandidates(deletedRels) { isProtectedDir(label, it) }) {
                    try { if (tree.deleteDirIfEmpty(dir)) folders++ } catch (ex: Exception) { /* bleibt bestehen */ }
                }
            }
            Result(deleted, bytes, kept, unreachable, folders)
        }
    }

    private fun tree(context: Context, profile: Profile): LocalTree {
        val uri = Uri.parse(profile.localTreeUri)
        if (!LocalTree.hasPermission(context, uri))
            throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.err_local_access_lost))
        return LocalTree(context, uri)
    }
}
