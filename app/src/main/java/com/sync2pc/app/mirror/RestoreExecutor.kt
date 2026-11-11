package com.sync2pc.app.mirror

import com.sync2pc.app.R
import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.data.PendingRestore
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.l10n.str
import com.sync2pc.app.smb.RemoteEntry
import com.sync2pc.app.smb.RemoteStore
import com.sync2pc.app.sync.isNetworkError
import com.sync2pc.app.sync.shortError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException

/**
 * Alles, was [RestoreExecutor] von der [MirrorEngine] braucht und nicht Handy-Ordner oder Freigabe ist: Datenbank
 * (Sync-Stand, Konflikte, Vormerkungen), Fehlerzähler, Protokoll, Zähler und die Übertragungen, die in der Engine bleiben.
 * SYNC-20261111-163 Stufe 2: in Tests durch ein Fake ersetzbar.
 */
interface RestoreHost {
    /** Datum für den Papierkorb-Ordner des Handys */
    val today: String

    /** Vorgemerkte Wiederherstellungen des Profils (ohne Filter nach aufgebrauchten Versuchen) */
    suspend fun pendingRestores(): List<PendingRestore>
    /** Sync-Stand des Profils vor dem Wiederherstellen (für das Zurückrollen) */
    suspend fun mirrorStates(): List<MirrorState>
    suspend fun deleteMirrorState(key: String)
    suspend fun upsertMirrorState(state: MirrorState)
    suspend fun deleteConflict(relPath: String)
    suspend fun deleteRestore(id: Long)

    /** [com.sync2pc.app.mirror.RestoreFailures]: Fehlerzähler je Vormerkung */
    fun isExhausted(restoreId: Long): Boolean
    /** Fehlversuch festhalten; liefert den neuen Stand */
    fun markFailure(restoreId: Long): Int
    fun clearFailure(restoreId: Long)

    suspend fun log(level: String, msg: Msg)
    /** Logcat-Hinweis, wenn ein Schritt des Zurückrollens fehlschlägt */
    fun warn(what: String, e: Exception)
    fun countTrashed()
    fun countFailed()
    /** Netzwerkfehler: Der Lauf wird wie üblich unterbrochen */
    fun markConnectionFailed()

    /** PC-Datei in den PC-Papierkorb verschieben (zählt selbst mit); liefert den vollen Pfad im Papierkorb */
    fun trashRemote(rel: String): String
    /** Handy-Datei auf den PC kopieren und den Sync-Stand setzen */
    suspend fun upload(key: String, rel: String, local: LocalEntry)
    /** PC-Datei aufs Handy kopieren und den Sync-Stand setzen */
    suspend fun download(key: String, rel: String, remote: RemoteEntry)
}

/**
 * Vorgemerkte Wiederherstellungen ausführen (0.5.0), aus der [MirrorEngine] ausgelagert (SYNC-20261111-163 Stufe 2,
 * Verhalten unverändert): aktuelle Version auf beiden Seiten in den Papierkorb, alte Version an den ursprünglichen Ort
 * zurück und auf die andere Seite kopieren, Sync-Stand setzen. Bei einem Fehler wird zurückgerollt (0.7.0).
 */
class RestoreExecutor(
    private val tree: LocalFiles,
    private val s: RemoteStore,
    private val host: RestoreHost,
) {
    private fun remotePath(rel: String): String = s.path(rel.replace('/', '\\'))

    /**
     * Netzwerkfehler -> Vormerkung bleibt, der Lauf wird wie üblich unterbrochen (Ausnahme geht nach aussen).
     * Liefert true, wenn etwas geschah.
     */
    suspend fun run(): Boolean {
        // Nach 5 Fehlversuchen nicht mehr automatisch wiederholen (0.11.8, DEC-146): Vormerkung bleibt bestehen
        // (schützt die Papierkorb-Version), der Benutzer wählt auf der Startseite "Erneut versuchen" oder "Verwerfen"
        val list = host.pendingRestores().filterNot { host.isExhausted(it.id) }
        if (list.isEmpty()) return false
        host.log("I", Msg.plural(R.plurals.eng_restoring, list.size))
        val oldStates = host.mirrorStates().associateBy { it.relPath }
        for (r in list) {
            currentCoroutineContext().ensureActive()
            val key = r.origPath.lowercase()
            val fromPc = r.side == Side.REMOTE.name
            val trashFull = s.path(r.trashPath.replace('/', '\\'))
            val dest = remotePath(r.origPath)
            // Für das Zurückrollen (0.7.0): wohin die aktuellen Versionen gingen, ob die alte schon am Ziel liegt
            var localTrashed: String? = null
            var remoteTrashed: String? = null
            var sourceMoved = false
            try {
                val localSource = if (fromPc) null else tree.find(r.trashPath)
                val remoteSource = if (fromPc) s.stat(trashFull) else null
                if (localSource == null && remoteSource == null) {
                    host.log("W", Msg.of(R.string.eng_restore_gone, r.origPath))
                    host.deleteRestore(r.id); host.clearFailure(r.id)
                    continue
                }
                // 1. Aktuelle Versionen (falls vorhanden) in den Papierkorb – so ist auch das Wiederherstellen umkehrbar
                host.deleteMirrorState(key)
                tree.find(r.origPath)?.let { localTrashed = tree.moveToTrash(r.origPath, it, host.today); host.countTrashed() }
                if (s.stat(dest) != null) remoteTrashed = host.trashRemote(r.origPath)
                // 2. Alte Version zurück an ihren Ort, 3. auf die andere Seite kopieren (setzt den Sync-Stand)
                if (localSource != null) {
                    val id = tree.moveFile(r.trashPath, localSource, r.origPath)
                    sourceMoved = true
                    // Per Ordnerliste lesen wie beim Einlesen (stat() liefert bei manchen Speicherorten andere Werte, 0.7.0)
                    val l = (try { tree.find(r.origPath) } catch (e: Exception) { null }) ?: tree.stat(id)
                        ?: throw IOException(str(R.string.eng_err_restored_missing))
                    host.upload(key, r.origPath, l)
                } else {
                    s.ensureDir(dest.substringBeforeLast('\\', ""))
                    s.move(trashFull, dest)
                    sourceMoved = true
                    val re = s.stat(dest) ?: throw IOException(str(R.string.eng_err_restored_missing))
                    host.download(key, r.origPath, re)
                }
                host.deleteConflict(r.origPath)
                host.deleteRestore(r.id); host.clearFailure(r.id)
                host.log("I", Msg.of(R.string.eng_restored, r.origPath, r.trashPath.split('/').getOrNull(1) ?: "?"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Zurückrollen: alte Version zurück in den Papierkorb, aktuelle Versionen an ihren Platz (0.7.0)
                val rolledBack = rollback(r.origPath, r.trashPath, trashFull, dest, fromPc, sourceMoved, localTrashed, remoteTrashed)
                if (rolledBack) oldStates[key]?.let { host.upsertMirrorState(it) }
                if (isNetworkError(e)) { host.markConnectionFailed(); throw e } // bleibt vorgemerkt
                host.countFailed()
                // Vormerkung bleiben lassen: nächster Sync versucht es erneut, die Startseite zeigt einen Hinweis
                val attempts = host.markFailure(r.id)
                val err = shortError(e)
                host.log("E", when {
                    !rolledBack -> Msg.of(R.string.eng_restore_failed_partial, r.origPath, err)
                    RestoreRetryPolicy.exhausted(attempts) ->
                        Msg.of(R.string.eng_restore_gave_up, r.origPath, err, RestoreRetryPolicy.MAX_ATTEMPTS)
                    else -> Msg.of(R.string.eng_restore_failed_rolled_back, r.origPath, err)
                })
            }
        }
        return true
    }

    /** Ein fehlgeschlagenes Wiederherstellen rückgängig machen. true = alles wieder wie vorher */
    private fun rollback(
        origPath: String, trashPath: String, trashFull: String, dest: String, fromPc: Boolean,
        sourceMoved: Boolean, localTrashed: String?, remoteTrashed: String?,
    ): Boolean {
        var ok = true
        fun step(what: String, block: () -> Unit) {
            try { block() } catch (e: Exception) { ok = false; host.warn("Zurückrollen $what", e) }
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
}
