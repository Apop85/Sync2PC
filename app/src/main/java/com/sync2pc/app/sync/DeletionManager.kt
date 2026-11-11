package com.sync2pc.app.sync

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sync2pc.app.R
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.DeleteMode
import com.sync2pc.app.data.DeleteState
import com.sync2pc.app.data.LogEntry
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.RunStatus
import com.sync2pc.app.data.SyncDao
import com.sync2pc.app.data.SyncRun
import com.sync2pc.app.data.SyncedFile
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.smb.RemoteFiles
import com.sync2pc.app.smb.SmbSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Aktueller Stand einer Datei auf dem Handy (MediaStore) */
internal sealed class PhoneNow {
    data class Present(val size: Long, val modifiedSec: Long) : PhoneNow()
    /** Die Datei gibt es nicht mehr */
    object Gone : PhoneNow()
    /** Nicht feststellbar (Fehler): nie als Freigabe zu werten */
    object Unknown : PhoneNow()
}

/** Ergebnis der Prüfung der PC-Kopien einer Datei vor dem Löschen (SYNC-20261111-142, R-I1) */
internal sealed class CopyVerdict {
    /** Auf dem PC liegt für jedes nötige Profil eine Kopie mit gleicher Grösse und gleichem Teil-Hash */
    object Ok : CopyVerdict()
    /** Die Kopie fehlt oder weicht ab (nur für diese Profile eindeutig festgestellt) -> Original bleiben, erneut sichern */
    class Missing(val profileIds: List<Long>) : CopyVerdict()
    /** PC nicht erreichbar oder nicht prüfbar -> nichts löschen, vorgemerkt lassen */
    object Unreachable : CopyVerdict()
    /** Unter der Adresse antwortet ein anderer oder noch nicht erkannter PC (keine Anmeldung) -> nichts löschen */
    class Rejected(val message: String) : CopyVerdict()
    /** Die Datei auf dem Handy ist nicht lesbar -> nichts löschen, nichts zurücksetzen */
    object PhoneUnreadable : CopyVerdict()
}

/**
 * Löschen/Papierkorb nach verifiziertem Upload.
 * Eine Datei wird erst freigegeben, wenn ALLE aktiven Profile, die ihren Ordner sichern,
 * sie verifiziert übertragen haben UND die Kopie auf dem PC zum Löschzeitpunkt noch vorhanden und gleich ist
 * (SYNC-20261111-142, R-I1: gleiche Grösse und gleicher Teil-Hash wie die aktuelle Handy-Datei, Prüfung über
 * [RemoteFinder] wie bei "Speicher freigeben"). Ist der PC nicht erreichbar, wird nichts gelöscht.
 * Android 10: direktes Löschen. Android 11+: Sammel-Bestätigung durch den Benutzer (Systemdialog).
 */
object DeletionManager {

    private val videoExt = setOf("mp4", "mov", "3gp", "3g2", "mkv", "webm", "avi", "m4v", "mts", "m2ts")

    private fun isVideo(key: String) = key.substringAfterLast('.', "").lowercase() in videoExt

    private fun covers(p: Profile, sourceKey: String): Boolean {
        val rel = sourceKey.substringBeforeLast('/') + "/"
        val video = isVideo(sourceKey)
        if (video && !p.includeVideos) return false
        if (!video && !p.includeImages) return false
        return p.folders().any { f ->
            val norm = if (f.endsWith("/")) f else "$f/"
            if (p.includeSubfolders) rel.startsWith(norm) else rel == norm
        }
    }

    // ---- Reine Entscheidungslogik (ohne Android, in Tests geprüft) ---------------------------------

    /**
     * Profile, die die AKTUELLE Datei verifiziert gesichert haben: Eine Zeile zählt nur, wenn ihre Grösse und ihr
     * Änderungsdatum zur Datei auf dem Handy passen (R-I1: ein nach der Sicherung geändertes Foto ist nicht gesichert).
     */
    internal fun syncedBy(all: List<SyncedFile>, now: PhoneNow.Present): Set<Long> =
        all.filter { it.verified && SpaceFreer.rowMatches(it, now.size, now.modifiedSec) }.map { it.profileId }.toSet()

    /**
     * Prüft die PC-Kopien für die Zeilen [rows] (je gesicherndes Profil eine). [phoneHash] liefert den Teil-Hash der
     * aktuellen Handy-Datei (null = nicht lesbar), [remoteOf] die Verbindung zum PC des Profils (wirft bei Fehlern).
     * Sicherer Ausfall: Nur eine eindeutige Antwort "fehlt/weicht ab" ergibt [CopyVerdict.Missing]; jede Unsicherheit
     * (Netz, Zugriff, Erkennung des PCs) bedeutet "nichts löschen" ohne die Zeilen zurückzusetzen.
     */
    internal fun verifyCopies(
        rows: List<SyncedFile>,
        phoneHash: () -> String?,
        remoteOf: (Long) -> RemoteFiles,
    ): CopyVerdict {
        if (rows.isEmpty()) return CopyVerdict.Unreachable // nichts prüfbar -> nie löschen
        val hash = phoneHash() ?: return CopyVerdict.PhoneUnreadable
        val missing = ArrayList<Long>()
        for (row in rows) {
            try {
                val remote = remoteOf(row.profileId)
                if (!RemoteFinder(remote).existsAtPath(row.remotePath, row.size) { hash }) missing += row.profileId
            } catch (e: CancellationException) {
                throw e
            } catch (e: PcIdentityException) {
                return CopyVerdict.Rejected(e.message ?: "")
            } catch (e: Exception) {
                return CopyVerdict.Unreachable
            }
        }
        return if (missing.isEmpty()) CopyVerdict.Ok else CopyVerdict.Missing(missing)
    }

    // ---- Android: Zustand der Datei und Verbindungen -------------------------------------------------

    /** Aktuelle Grösse und Änderungsdatum (Sekunden) der Datei; [PhoneNow.Unknown] bei Fehlern */
    private fun phoneNow(context: Context, mediaUri: String): PhoneNow = try {
        context.contentResolver.query(
            Uri.parse(mediaUri), arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED), null, null, null
        )?.use { c ->
            if (c.moveToFirst()) PhoneNow.Present(c.getLong(0), c.getLong(1)) else PhoneNow.Gone
        } ?: PhoneNow.Unknown
    } catch (e: Exception) { PhoneNow.Unknown }

    /** Verbindungen zu den PCs der Profile, je Profil höchstens einmal geöffnet; ein Fehler gilt für den ganzen Lauf */
    private class PcSessions(private val profiles: Map<Long, Profile>) : AutoCloseable {
        private val open = HashMap<Long, SmbSession>()
        private val failed = HashMap<Long, Exception>()

        fun remoteOf(profileId: Long): RemoteFiles {
            failed[profileId]?.let { throw it }
            open[profileId]?.let { s -> if (s.isAlive()) return s else { try { s.close() } catch (ignored: Exception) {}; open.remove(profileId) } }
            val profile = profiles[profileId] ?: throw IllegalStateException("Profil $profileId fehlt")
            return try {
                SmbSession.open(profile).also { open[profileId] = it }
            } catch (e: Exception) {
                failed[profileId] = e
                throw e
            }
        }

        override fun close() { for (s in open.values) try { s.close() } catch (ignored: Exception) {} }
    }

    /** Sammelt die Ergebnisse der PC-Prüfung eines Durchlaufs */
    private class Tally {
        var unreachable = false
        var rejected: String? = null
        var reset = 0
    }

    // ---- Android 10: direkt löschen; Android 11+: für den Systemdialog vormerken ------------------------

    /** Prüft Dateien mit Status WANTED. Liefert Anzahl neu zur Bestätigung freigegebener Dateien. */
    suspend fun sweep(context: Context, log: suspend (Msg) -> Unit): Int {
        val dao = AppDatabase.get(context).dao()
        // Nur Bildordner-Profile sichern über den MediaStore (Ordner-Profile löschen nie nach dem Sync)
        val profiles = dao.profiles().filter { it.enabled && it.type() == com.sync2pc.app.data.ProfileType.PHOTOS }
        val wanted = dao.byDeleteState(DeleteState.WANTED).groupBy { it.sourceKey }
        var queued = 0
        val tally = Tally()
        PcSessions(profiles.associateBy { it.id }).use { pc ->
            for ((key, rows) in wanted) {
                val row = rows.first()
                // Aktueller Stand der Datei: nicht feststellbar -> nicht anfassen; weg -> nichts zu löschen
                val now = when (val n = phoneNow(context, row.mediaUri)) {
                    is PhoneNow.Present -> n
                    PhoneNow.Gone -> { dao.setDeleteStateForKey(key, DeleteState.DONE); continue }
                    PhoneNow.Unknown -> continue
                }
                val all = dao.syncedAllProfiles(key)
                val syncedBy = syncedBy(all, now)
                val covering = profiles.filter { covers(it, key) }
                if (!covering.all { it.id in syncedBy }) continue
                // Papierkorb gewinnt, wenn irgendein Profil nur Papierkorb wünscht
                val modes = covering.map { it.delete() }.filter { it != DeleteMode.KEEP }
                if (modes.isEmpty()) continue
                val trash = modes.contains(DeleteMode.TRASH)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    // Android 10: direkt löschen (Legacy-Storage), aber erst nach der Prüfung der PC-Kopien
                    val coveringIds = covering.map { it.id }.toSet()
                    val toCheck = all.filter { it.profileId in coveringIds && it.profileId in syncedBy }
                    val phone = Uri.parse(row.mediaUri)
                    val verdict = verifyCopies(toCheck, { SpaceFreer.localHash(context, phone, now.size) }) { pc.remoteOf(it) }
                    if (!applyVerdict(dao, key, verdict, tally, log)) continue
                    try {
                        context.contentResolver.delete(phone, null, null)
                        dao.setDeleteStateForKey(key, DeleteState.DONE)
                        log(Msg.of(R.string.delete_log_done, key))
                    } catch (e: Exception) {
                        log(Msg.of(R.string.delete_log_failed, key, e.message ?: ""))
                    }
                } else {
                    dao.setDeleteStateForKey(key, if (trash) DeleteState.QUEUED_TRASH else DeleteState.QUEUED_DELETE)
                    queued++
                }
            }
        }
        summarize(tally, log)
        return queued
    }

    /**
     * Wertet das Ergebnis der PC-Prüfung aus. true = Original darf gelöscht werden. Sonst bleibt es auf dem Handy:
     * bei [CopyVerdict.Missing] werden die betroffenen Zeilen entfernt (die Datei gilt beim nächsten Lauf wieder als
     * neu und wird erneut gesichert) und der Löschstatus zurückgesetzt, mit Eintrag im Verlauf.
     */
    private suspend fun applyVerdict(
        dao: SyncDao, key: String, verdict: CopyVerdict, tally: Tally, log: suspend (Msg) -> Unit,
    ): Boolean {
        when (verdict) {
            CopyVerdict.Ok -> return true
            is CopyVerdict.Missing -> {
                for (id in verdict.profileIds) dao.deleteSynced(id, key)
                dao.setDeleteStateForKey(key, DeleteState.NONE)
                tally.reset++
                log(Msg.of(R.string.delete_log_pc_missing, key))
            }
            CopyVerdict.Unreachable -> tally.unreachable = true
            is CopyVerdict.Rejected -> tally.rejected = verdict.message
            CopyVerdict.PhoneUnreadable -> log(Msg.of(R.string.delete_log_phone_unreadable, key))
        }
        return false
    }

    /** Eine Zeile je Durchlauf, wenn der PC nicht erreichbar oder nicht erkannt war */
    private suspend fun summarize(tally: Tally, log: suspend (Msg) -> Unit) {
        if (tally.unreachable) log(Msg.of(R.string.delete_pc_unreachable))
        tally.rejected?.let { log(Msg.of(R.string.delete_pc_rejected, it)) }
    }

    /**
     * Ergebnis von [buildRequests]: [batches] für den Systemdialog; [unreachable]/[rejectedMessage] = die PC-Prüfung
     * war für mindestens eine Datei nicht möglich (diese bleibt vorgemerkt, nichts gelöscht); [reset] = Dateien, deren
     * PC-Kopie fehlte oder abwich (bleiben auf dem Handy, werden erneut gesichert).
     */
    data class Outcome(
        val batches: List<Batch>,
        val unreachable: Boolean = false,
        val rejectedMessage: String? = null,
        val reset: Int = 0,
    )

    data class Batch(val trash: Boolean, val keys: List<String>, val intent: PendingIntent)

    /**
     * Android 11+: Erzeugt Systemdialog-Anfragen (max. 500 Dateien je Anfrage). Vor dem Dialog wird jede Datei
     * geprüft (R-I1, SYNC-20261111-142): Handy-Datei seit der Sicherung unverändert, PC-Kopie vorhanden und gleich.
     * Netzwerkzugriffe: nur aufrufen aus einem Hintergrund-Thread (MainActivity: Dispatchers.IO).
     */
    suspend fun buildRequests(context: Context): Outcome = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext Outcome(emptyList())
        val dao = AppDatabase.get(context).dao()
        val profiles = dao.profiles().filter { it.enabled && it.type() == com.sync2pc.app.data.ProfileType.PHOTOS }
        val rows = dao.queuedDeletes().distinctBy { it.sourceKey }
        val valid = ArrayList<SyncedFile>()
        val tally = Tally()
        val history = HistoryNotes(dao, profiles.associateBy { it.id })
        PcSessions(profiles.associateBy { it.id }).use { pc ->
            for (r in rows) {
                val key = r.sourceKey
                val now = when (val n = phoneNow(context, r.mediaUri)) {
                    is PhoneNow.Present -> n
                    PhoneNow.Gone -> { dao.setDeleteStateForKey(key, DeleteState.DONE); continue }
                    PhoneNow.Unknown -> continue // nicht feststellbar: vorgemerkt lassen, nichts löschen
                }
                val all = dao.syncedAllProfiles(key)
                val syncedBy = syncedBy(all, now)
                val covering = profiles.filter { covers(it, key) }
                // Seit der Sicherung geändert oder nicht mehr von allen sichernden Profilen gesichert -> nicht löschen
                if (covering.isEmpty() || !covering.all { it.id in syncedBy }) {
                    dao.setDeleteStateForKey(key, DeleteState.DONE); continue
                }
                val coveringIds = covering.map { it.id }.toSet()
                val toCheck = all.filter { it.profileId in coveringIds && it.profileId in syncedBy }
                val phone = Uri.parse(r.mediaUri)
                val verdict = verifyCopies(toCheck, { SpaceFreer.localHash(context, phone, now.size) }) { pc.remoteOf(it) }
                val ok = applyVerdict(dao, key, verdict, tally) { history.note(r.profileId, it) }
                if (ok) valid += r
            }
        }
        history.finish(tally)
        val result = ArrayList<Batch>()
        for ((trash, list) in valid.groupBy { it.deleteState == DeleteState.QUEUED_TRASH }) {
            for (chunk in list.chunked(500)) {
                val uris = chunk.map { Uri.parse(it.mediaUri) }
                val pi = if (trash) MediaStore.createTrashRequest(context.contentResolver, uris, true)
                else MediaStore.createDeleteRequest(context.contentResolver, uris)
                result += Batch(trash, chunk.map { it.sourceKey }, pi)
            }
        }
        Outcome(result, tally.unreachable, tally.rejected, tally.reset)
    }

    /**
     * Verlaufseintrag für die Prüfung ausserhalb eines Sync-Laufs (Systemdialog-Weg): Ein Lauf "manuell" mit den Zeilen
     * zu zurückgesetzten Dateien und zur Meldung "PC nicht erreichbar". Wird nur angelegt, wenn es etwas zu melden gibt.
     */
    private class HistoryNotes(private val dao: SyncDao, private val profiles: Map<Long, Profile>) {
        private val lines = ArrayList<Pair<String, Msg>>()
        private var firstProfile: Long? = null

        fun note(profileId: Long, msg: Msg) {
            if (firstProfile == null) firstProfile = profileId
            lines += "W" to msg
        }

        suspend fun finish(tally: Tally) {
            if (tally.unreachable) lines += "W" to Msg.of(R.string.delete_pc_unreachable)
            tally.rejected?.let { lines += "W" to Msg.of(R.string.delete_pc_rejected, it) }
            if (lines.isEmpty()) return
            val pid = firstProfile ?: profiles.keys.firstOrNull() ?: 0L
            val trigger = Msg.of(R.string.trigger_manual)
            val summary = lines.first().second
            val now = System.currentTimeMillis()
            val runId = dao.insertRun(
                SyncRun(
                    profileId = pid, profileName = profiles[pid]?.name ?: "", trigger = trigger.text(),
                    triggerCode = trigger.toCode(), dryRun = false, startedAt = now, finishedAt = now,
                    status = RunStatus.PARTIAL, message = summary.text(), messageCode = summary.toCode(),
                )
            )
            for ((level, m) in lines) {
                dao.insertLog(LogEntry(runId = runId, time = now, level = level, message = m.text(), code = m.toCode()))
            }
        }
    }

    suspend fun markDone(context: Context, keys: List<String>) {
        val dao = AppDatabase.get(context).dao()
        for (k in keys) dao.setDeleteStateForKey(k, DeleteState.DONE)
    }
}
