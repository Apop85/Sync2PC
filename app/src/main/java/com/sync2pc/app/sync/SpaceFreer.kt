package com.sync2pc.app.sync

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.FolderLayout
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncedFile
import com.sync2pc.app.smb.RemoteEntry
import com.sync2pc.app.smb.RemoteFiles
import com.sync2pc.app.smb.SmbSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * "Speicher freigeben" (0.2.0): alte Fotos/Videos, die nachweislich auf dem PC liegen, in den
 * Android-Papierkorb verschieben.
 *
 * Sicherheit: Die Datenbank allein reicht nicht (der Benutzer kann auf dem PC löschen oder verschieben).
 * Direkt vor dem Löschen wird jede Datei auf dem PC gesucht (Pfad aus der Datenbank, sonst andere Ordner
 * desselben Monats) und die Grösse verglichen. Bild + Video mit gleichem Namen (Live-Fotos) werden nur
 * gemeinsam entfernt.
 */
object SpaceFreer {

    data class Candidate(
        val uri: Uri,
        val sourceKey: String,
        val displayName: String,
        val size: Long,
        val profileId: Long,
        val remotePath: String,
        /** Gruppe für Live-Foto-Paare: Ordner + Name ohne Endung */
        val group: String,
        /** Teil-Hash beim Sichern (null = nicht gespeichert -> wird bei Bedarf neu berechnet) */
        val hash: String? = null,
        /** Änderungsdatum (Sekunden) beim Auflisten: Vergleichswert für die Neuprüfung direkt vor dem Löschen (SYNC-20261111-205) */
        val modifiedSec: Long = 0,
    )

    /** Fotos eines Profils als Auswahl-Einträge (0.7.0) + die nötigen Daten für Prüfung und Papierkorb */
    data class PhotoItems(val items: List<FreeItem>, val candidates: Map<String, Candidate>)

    data class VerifyResult(
        val confirmed: List<Candidate>,
        /** Auf dem PC nicht (mehr) gefunden oder andere Grösse – bleiben auf dem Handy */
        val missing: Int,
        /** Profile, deren PC nicht erreichbar war */
        val unreachable: List<String>,
        /** Profile, bei denen unter der Adresse ein anderer/noch nicht erkannter PC antwortet (keine Anmeldung erfolgt) */
        val rejected: List<String> = emptyList(),
        /** Seit dem Auflisten auf dem Handy geändert (Grösse/Datum) – bleiben auf dem Handy (SYNC-20261111-205) */
        val changed: Int = 0,
    )

    private data class Media(
        val id: Long, val uri: Uri, val name: String, val relPath: String, val size: Long,
        val takenMs: Long, val favorite: Boolean,
        /** MediaStore DATE_MODIFIED in Sekunden (wie [SyncedFile.dateModified]) */
        val modifiedSec: Long = 0,
    ) {
        val sourceKey get() = relPath + name
        val group get() = (relPath + name.substringBeforeLast('.')).lowercase()
    }

    /** Stichtag: Aufnahmen vor diesem Zeitpunkt sind "älter als [months] Monate" */
    fun cutoffMillis(months: Int, now: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, -months) }.timeInMillis

    /** Gruppen behalten, in denen ALLE Mitglieder geeignet sind (Live-Foto-Paare nie trennen) */
    fun <T> wholeGroups(all: List<T>, group: (T) -> String, ok: (T) -> Boolean): List<T> =
        all.groupBy(group).values.filter { members -> members.all(ok) }.flatten()

    /**
     * Passt die Datenbankzeile zur aktuellen Datei auf dem Handy? Grösse UND Änderungsdatum müssen gleich sein
     * (R-I2, SYNC-20261111-142): Ein bearbeitetes Foto gleicher Grösse gilt nicht als gesichert.
     */
    internal fun rowMatches(row: SyncedFile, size: Long, modifiedSec: Long): Boolean =
        row.size == size && row.dateModified == modifiedSec

    /** Ist die Handy-Datei noch so wie beim Auflisten? (Grösse UND Änderungsdatum; SYNC-20261111-205) */
    internal fun unchanged(listedSize: Long, listedModifiedSec: Long, size: Long?, modifiedSec: Long?): Boolean =
        size != null && modifiedSec != null && listedSize == size && listedModifiedSec == modifiedSec

    /** Aktuelle (Grösse, Änderungsdatum) hinter der URI aus dem MediaStore; null = nicht lesbar/nicht mehr vorhanden */
    private fun currentState(context: Context, uri: Uri): Pair<Long, Long>? = try {
        context.contentResolver.query(
            uri, arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getLong(0) to c.getLong(1) else null }
    } catch (e: Exception) { null }

    /**
     * Neuprüfung unmittelbar vor dem Löschen: Dateien, deren Grösse oder Änderungsdatum vom Stand beim Auflisten
     * abweicht (z. B. nach dem Öffnen der Seite bearbeitet), bleiben auf dem Handy. Nicht lesbar = geändert.
     * Live-Foto-Paare bleiben ganz, sobald ein Partner abweicht. Liefert (unverändert, Anzahl bleibt).
     */
    fun recheckUnchanged(context: Context, list: List<Candidate>): Pair<List<Candidate>, Int> {
        val ok = HashSet<String>()
        for (c in list) {
            val st = currentState(context, c.uri)
            if (unchanged(c.size, c.modifiedSec, st?.first, st?.second)) ok += c.sourceKey
        }
        val keep = wholeGroups(list, { it.group }) { it.sourceKey in ok }
        return keep to (list.size - keep.size)
    }

    /**
     * Alle Fotos/Videos eines Fotos-Profils (0.7.0). Gesichert = in der Datenbank mit gleicher Grösse und gleichem
     * Änderungsdatum (geprüft wird vor dem Löschen auf dem PC). Live-Foto-Paare haben dieselbe Gruppe; FreeRules
     * trennt sie nie.
     */
    suspend fun photoItems(context: Context, profile: Profile): PhotoItems = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(context).dao()
        val media = query(context, listOf(profile))
        val synced = dao.verifiedSynced().groupBy { it.sourceKey }
        val items = ArrayList<FreeItem>()
        val candidates = HashMap<String, Candidate>()
        for (m in media) {
            val s = synced[m.sourceKey]?.firstOrNull { rowMatches(it, m.size, m.modifiedSec) }
            items += FreeItem(
                key = m.sourceKey, folder = m.relPath.trimEnd('/'), name = m.name, size = m.size, time = m.takenMs,
                backedUp = s != null, favorite = m.favorite, group = m.group,
            )
            if (s != null) candidates[m.sourceKey] =
                Candidate(m.uri, m.sourceKey, m.name, m.size, s.profileId, s.remotePath, m.group, s.partialHash, m.modifiedSec)
        }
        PhotoItems(items, candidates)
    }

    /** Prüft jede Datei auf dem PC. [onProgress](geprüft, gesamt) */
    suspend fun verify(
        context: Context,
        candidates: List<Candidate>,
        onProgress: (Int, Int) -> Unit,
    ): VerifyResult = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(context).dao()
        val profiles = dao.profiles().associateBy { it.id }
        val ok = HashSet<String>()
        val unreachable = ArrayList<String>()
        val rejected = ArrayList<String>()
        var done = 0
        for ((profileId, list) in candidates.groupBy { it.profileId }) {
            val profile = profiles[profileId]
            var notRecognised = false
            val session = try { profile?.let { SmbSession.open(it) } }
                catch (e: PcIdentityException) { notRecognised = true; null }   // anderer PC: keine Anmeldung, nichts geprüft
                catch (e: Exception) { null }
            if (session == null) {
                val label = profile?.name ?: com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.space_profile_id, profileId)
                if (notRecognised) rejected += label else unreachable += label
                done += list.size; onProgress(done, candidates.size)
                continue
            }
            session.use { s ->
                val finder = RemoteFinder(s)
                val layout = profile?.layout() ?: FolderLayout.MONTH_SUFFIX
                for (c in list) {
                    currentCoroutineContext().ensureActive()
                    // Zuerst am gespeicherten Pfad; nur wenn dort nichts liegt: Monat aus media_info als Zusatz zum Pfad
                    // Am Pfad zählt nur gleiche Grösse UND gleicher Teil-Hash (R-I2): Name und Grösse allein beweisen nichts
                    if (finder.existsAtPath(c.remotePath, c.size) { c.hash ?: localHash(context, c) } ||
                        finder.existsElsewhere(c.remotePath, c.size, layout, dao.mediaInfo(c.sourceKey)?.month) {
                            c.hash ?: localHash(context, c)
                        }
                    ) ok += c.sourceKey
                    done++
                    if (done % 10 == 0 || done == candidates.size) onProgress(done, candidates.size)
                }
            }
        }
        // Fehlt ein Partner eines Live-Fotos auf dem PC, bleiben beide auf dem Handy
        val onPc = wholeGroups(candidates, { it.group }) { it.sourceKey in ok }
        // Direkt vor dem Papierkorb-Antrag: seit dem Auflisten bearbeitete Fotos bleiben (SYNC-20261111-205)
        val (confirmed, changed) = recheckUnchanged(context, onPc)
        VerifyResult(confirmed, candidates.size - onPc.size, unreachable, rejected, changed)
    }

    /** Systemdialog-Anfragen "In den Papierkorb" (Android 11+), höchstens 500 Dateien je Anfrage */
    fun trashRequests(context: Context, list: List<Candidate>): List<Pair<PendingIntent, Int>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return list.chunked(500).map { chunk ->
            MediaStore.createTrashRequest(context.contentResolver, chunk.map { it.uri }, true) to chunk.size
        }
    }

    /** Android 10 kennt keinen Papierkorb: direkt löschen (nach ausdrücklicher Bestätigung in der App) */
    suspend fun deleteDirectly(context: Context, list: List<Candidate>): Int = withContext(Dispatchers.IO) {
        var n = 0
        // Zwischen Prüfung und Bestätigung kann das Foto bearbeitet worden sein: erneut prüfen (SYNC-20261111-205)
        val (safe, _) = recheckUnchanged(context, list)
        for (c in safe) try { if (context.contentResolver.delete(c.uri, null, null) > 0) n++ } catch (ignored: Exception) {}
        n
    }

    /** Teil-Hash der Datei auf dem Handy (null = nicht lesbar) */
    private fun localHash(context: Context, c: Candidate): String? = localHash(context, c.uri, c.size)

    /** Teil-Hash (LOW) der aktuellen Datei hinter [uri] (null = nicht lesbar); auch für [DeletionManager] */
    internal fun localHash(context: Context, uri: Uri, size: Long): String? = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.let { pfd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { ins ->
                val ch = ins.channel
                PartialHash.compute(size) { off, len ->
                    val bb = java.nio.ByteBuffer.allocate(len)
                    var pos = off
                    while (bb.hasRemaining()) {
                        val n = ch.read(bb, pos)
                        if (n < 0) break
                        pos += n
                    }
                    if (bb.position() == len) bb.array() else bb.array().copyOf(bb.position())
                }
            }
        }
    } catch (e: Exception) { null }


    // ---- MediaStore --------------------------------------------------------------------
    private fun query(context: Context, profiles: List<Profile>): List<Media> {
        if (profiles.isEmpty()) return emptyList()
        val args = ArrayList<String>()
        val parts = ArrayList<String>()
        for (p in profiles) {
            val types = buildList {
                if (p.includeImages) add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                if (p.includeVideos) add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            }
            if (types.isEmpty()) continue
            for (f in p.folders()) {
                val norm = if (f.endsWith("/")) f else "$f/"
                val pathSel = if (p.includeSubfolders) {
                    args += norm.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? ESCAPE '\\'"
                } else {
                    args += norm
                    "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                }
                parts += "(${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${types.joinToString(",")}) AND $pathSel)"
            }
        }
        if (parts.isEmpty()) return emptyList()
        val hasFavorite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID); add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.MediaColumns.DISPLAY_NAME); add(MediaStore.MediaColumns.RELATIVE_PATH)
            add(MediaStore.MediaColumns.SIZE); add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_MODIFIED); add(MediaStore.MediaColumns.VOLUME_NAME)
            if (hasFavorite) add(MediaStore.MediaColumns.IS_FAVORITE)
        }.toTypedArray()
        val selection = "(${parts.joinToString(" OR ")}) AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
        val out = LinkedHashMap<Long, Media>()
        context.contentResolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), projection, selection, args.toTypedArray(), null
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val iType = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val iPath = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val iTaken = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val iMod = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val iVol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)
            val iFav = if (hasFavorite) c.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE) else -1
            while (c.moveToNext()) {
                val name = c.getString(iName) ?: continue
                val size = c.getLong(iSize)
                if (size <= 0) continue
                // Papierkorb-Dateien nie anbieten (SYNC-20261112-249)
                if (com.sync2pc.app.mirror.MirrorEngine.isInTrash(c.getString(iPath) ?: "")) continue
                val id = c.getLong(iId)
                val video = c.getInt(iType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val volume = c.getString(iVol) ?: MediaStore.VOLUME_EXTERNAL_PRIMARY
                val base = if (video) MediaStore.Video.Media.getContentUri(volume) else MediaStore.Images.Media.getContentUri(volume)
                val taken = if (!c.isNull(iTaken) && c.getLong(iTaken) > 0) c.getLong(iTaken) else c.getLong(iMod) * 1000
                out[id] = Media(
                    id, ContentUris.withAppendedId(base, id), name, c.getString(iPath) ?: "", size, taken,
                    favorite = iFav >= 0 && c.getInt(iFav) == 1,
                    modifiedSec = c.getLong(iMod),
                )
            }
        }
        return out.values.toList()
    }
}

/**
 * Sucht eine Datei auf dem PC: zuerst am gespeicherten Pfad ([existsAtPath]), dann in allen Ordnern desselben
 * Monats ([existsElsewhere], vorlagenunabhängig über [MonthFolderSearch.related], SYNC-20261109-084).
 */
internal class RemoteFinder(private val s: RemoteFiles) {
    private val dirListings = HashMap<String, List<String>>()
    private val fileListings = HashMap<String, Map<String, RemoteEntry>>()

    /**
     * Liegt die Datei am gespeicherten Pfad? Bestätigt nur bei gleicher Grösse UND gleichem Teil-Hash (R-I2,
     * SYNC-20261111-142; zuvor genügte die Grösse): Eine auf dem PC ersetzte oder beschädigte Datei gleicher Grösse
     * darf das Original nie freigeben. [hash] liefert den Teil-Hash der Handy-Datei (null = nicht lesbar -> nicht
     * bestätigt). Lesefehler am PC (kein Netzwerkfehler) = nicht bestätigt; Netzwerkfehler gehen nach aussen.
     */
    fun existsAtPath(remotePath: String, size: Long, hash: () -> String?): Boolean {
        if (remotePath.isBlank()) return false
        val e = s.stat(remotePath) ?: return false
        if (e.size != size) return false
        val local = hash() ?: return false
        return try {
            s.partialHash(remotePath, size) == local
        } catch (ex: kotlinx.coroutines.CancellationException) {
            throw ex
        } catch (ex: Exception) {
            if (isNetworkError(ex)) throw ex
            false
        }
    }

    /**
     * [hash] liefert den Teil-Hash der Handy-Datei (null = nicht lesbar -> Datei bleibt auf dem Handy). Er ist hier
     * Pflicht: Name und Grösse allein reichen in anderen Ordnern nicht als Beweis (DEC-136).
     * Monat: aus dem gespeicherten Pfad gemäss Vorlage und aus [monthHint] (media_info); beide werden durchsucht.
     * Kann der Pfad nicht zerlegt werden (kein Jahresordner), wird nicht gesucht (= nicht gefunden).
     */
    fun existsElsewhere(remotePath: String, size: Long, layout: FolderLayout, monthHint: Int?, hash: () -> String?): Boolean {
        if (remotePath.isBlank()) return false
        val loc = MonthFolderSearch.locate(remotePath) ?: return false
        val name = remotePath.substringAfterLast('\\')
        val months = listOfNotNull(MonthFolderSearch.monthOfLocation(loc, layout), monthHint).distinct()
        var h: String? = null
        var hashed = false
        for (month in months) {
            val folders = MonthFolderSearch.related(month) { rel ->
                dirListings.getOrPut("${loc.yearPath}\\$rel".lowercase()) {
                    val p = if (rel.isEmpty()) loc.yearPath else "${loc.yearPath}\\$rel"
                    s.listDir(p).filter { it.isDirectory }.map { it.name }
                }
            }
            for (rel in folders) {
                if (rel.equals(loc.relDir, ignoreCase = true)) continue
                val path = "${loc.yearPath}\\$rel"
                val files = fileListings.getOrPut(path.lowercase()) {
                    s.listDir(path).filter { !it.isDirectory }.associateBy { it.name.lowercase() }
                }
                val e = files[name.lowercase()] ?: continue
                if (e.size != size) continue
                if (!hashed) { h = hash(); hashed = true }
                val local = h ?: return false // Handy-Datei nicht lesbar -> sicherheitshalber behalten
                if (s.partialHash("$path\\${e.name}", size) == local) return true
            }
        }
        return false
    }
}
