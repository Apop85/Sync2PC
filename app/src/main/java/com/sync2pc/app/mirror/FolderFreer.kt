package com.sync2pc.app.mirror

import android.content.Context
import android.net.Uri
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncDirection
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
            it.equals(MirrorEngine.TRASH_DIR, ignoreCase = true) || it.equals(MirrorEngine.OLD_TRASH_DIR, ignoreCase = true)
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
    )

    /**
     * Jede Datei auf dem PC prüfen (vorhanden, gleiche Grösse, gleicher Inhalts-Hash) und erst dann auf dem Handy
     * löschen. Der Sync-Stand der Datei wird vorher entfernt: Die PC-Kopie bleibt so auch, wenn später
     * "Löschungen übernehmen" eingeschaltet wird. Wartet auf einen laufenden Ordner-Sync ([MirrorLock]).
     * [onProgress](geprüft, gesamt)
     */
    suspend fun verifyAndDelete(
        context: Context, profile: Profile, list: List<FreeItem>, onProgress: (Int, Int) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        MirrorLock.mutex.withLock {
            val dao = AppDatabase.get(context).dao()
            val tree = tree(context, profile)
            val s = try { SmbSession.open(profile) } catch (e: Exception) { null }
                ?: return@withLock Result(0, 0, list.size, unreachable = true)
            var deleted = 0; var bytes = 0L; var kept = 0; var unreachable = false
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
                        if (r == null || r.size != e.size ||
                            tree.contentHash(e.docId, e.size) != s.contentHash(path, r.size)) { kept++; continue }
                        dao.deleteMirrorState(profile.id, key)
                        tree.delete(e.docId)
                        deleted++; bytes += e.size
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
            Result(deleted, bytes, kept, unreachable)
        }
    }

    private fun tree(context: Context, profile: Profile): LocalTree {
        val uri = Uri.parse(profile.localTreeUri)
        if (!LocalTree.hasPermission(context, uri))
            throw IOException("Kein Zugriff mehr auf den Handy-Ordner – bitte im Profil neu auswählen")
        return LocalTree(context, uri)
    }
}
