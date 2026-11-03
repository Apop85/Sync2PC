package com.sync2pc.app.sync

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.smb.RemoteEntry
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
    )

    /** Fotos eines Profils als Auswahl-Einträge (0.7.0) + die nötigen Daten für Prüfung und Papierkorb */
    data class PhotoItems(val items: List<FreeItem>, val candidates: Map<String, Candidate>)

    data class VerifyResult(
        val confirmed: List<Candidate>,
        /** Auf dem PC nicht (mehr) gefunden oder andere Grösse – bleiben auf dem Handy */
        val missing: Int,
        /** Profile, deren PC nicht erreichbar war */
        val unreachable: List<String>,
    )

    private data class Media(
        val id: Long, val uri: Uri, val name: String, val relPath: String, val size: Long,
        val takenMs: Long, val favorite: Boolean,
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
     * Alle Fotos/Videos eines Fotos-Profils (0.7.0). Gesichert = in der Datenbank mit gleicher Grösse (geprüft wird
     * vor dem Löschen auf dem PC). Live-Foto-Paare haben dieselbe Gruppe; FreeRules trennt sie nie.
     */
    suspend fun photoItems(context: Context, profile: Profile): PhotoItems = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(context).dao()
        val media = query(context, listOf(profile))
        val synced = dao.verifiedSynced().groupBy { it.sourceKey }
        val items = ArrayList<FreeItem>()
        val candidates = HashMap<String, Candidate>()
        for (m in media) {
            val s = synced[m.sourceKey]?.firstOrNull { it.size == m.size }
            items += FreeItem(
                key = m.sourceKey, folder = m.relPath.trimEnd('/'), name = m.name, size = m.size, time = m.takenMs,
                backedUp = s != null, favorite = m.favorite, group = m.group,
            )
            if (s != null) candidates[m.sourceKey] =
                Candidate(m.uri, m.sourceKey, m.name, m.size, s.profileId, s.remotePath, m.group, s.partialHash)
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
        var done = 0
        for ((profileId, list) in candidates.groupBy { it.profileId }) {
            val profile = profiles[profileId]
            val session = try { profile?.let { SmbSession.open(it) } } catch (e: Exception) { null }
            if (session == null) {
                unreachable += profile?.name ?: com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.space_profile_id, profileId)
                done += list.size; onProgress(done, candidates.size)
                continue
            }
            session.use { s ->
                val finder = RemoteFinder(s)
                for (c in list) {
                    currentCoroutineContext().ensureActive()
                    if (finder.exists(c.remotePath, c.size) { c.hash ?: localHash(context, c) }) ok += c.sourceKey
                    done++
                    if (done % 10 == 0 || done == candidates.size) onProgress(done, candidates.size)
                }
            }
        }
        // Fehlt ein Partner eines Live-Fotos auf dem PC, bleiben beide auf dem Handy
        val confirmed = wholeGroups(candidates, { it.group }) { it.sourceKey in ok }
        VerifyResult(confirmed, candidates.size - confirmed.size, unreachable)
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
        for (c in list) try { if (context.contentResolver.delete(c.uri, null, null) > 0) n++ } catch (ignored: Exception) {}
        n
    }

    /** Teil-Hash der Datei auf dem Handy (null = nicht lesbar) */
    private fun localHash(context: Context, c: Candidate): String? = try {
        context.contentResolver.openFileDescriptor(c.uri, "r")?.let { pfd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { ins ->
                val ch = ins.channel
                PartialHash.compute(c.size) { off, len ->
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

    /** Sucht eine Datei auf dem PC: zuerst am gespeicherten Pfad, dann in allen Ordnern desselben Monats */
    private class RemoteFinder(private val s: SmbSession) {
        private val yearListings = HashMap<String, List<RemoteEntry>>()
        private val folderListings = HashMap<String, Map<String, RemoteEntry>>()

        /**
         * [hash] liefert den Teil-Hash der Handy-Datei. Er wird nur für die Suche in anderen Monatsordnern
         * gebraucht: Dort reichen Name und Grösse nicht als Beweis (0.4.1).
         */
        fun exists(remotePath: String, size: Long, hash: () -> String?): Boolean {
            if (remotePath.isBlank()) return false
            s.stat(remotePath)?.let { if (it.size == size) return true }
            // Vom Benutzer umsortiert (z. B. "10" -> "10 - Ägypten")?
            val name = remotePath.substringAfterLast('\\')
            val folder = remotePath.substringBeforeLast('\\', "")
            val yearPath = folder.substringBeforeLast('\\', "")
            val month = FolderNames.monthOf(folder.substringAfterLast('\\')) ?: return false
            val siblings = yearListings.getOrPut(yearPath) { s.listDir(yearPath).filter { it.isDirectory } }
            for (d in siblings) {
                if (FolderNames.monthOf(d.name) != month) continue
                val path = if (yearPath.isEmpty()) d.name else "$yearPath\\${d.name}"
                if (path.equals(folder, ignoreCase = true)) continue
                val files = folderListings.getOrPut(path) { s.listDir(path).associateBy { it.name.lowercase() } }
                val e = files[name.lowercase()] ?: continue
                if (e.size != size) continue
                val h = hash() ?: return false // Handy-Datei nicht lesbar -> sicherheitshalber behalten
                if (s.partialHash("$path\\${e.name}", size) == h) return true
            }
            return false
        }
    }

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
                val id = c.getLong(iId)
                val video = c.getInt(iType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val volume = c.getString(iVol) ?: MediaStore.VOLUME_EXTERNAL_PRIMARY
                val base = if (video) MediaStore.Video.Media.getContentUri(volume) else MediaStore.Images.Media.getContentUri(volume)
                val taken = if (!c.isNull(iTaken) && c.getLong(iTaken) > 0) c.getLong(iTaken) else c.getLong(iMod) * 1000
                out[id] = Media(
                    id, ContentUris.withAppendedId(base, id), name, c.getString(iPath) ?: "", size, taken,
                    favorite = iFav >= 0 && c.getInt(iFav) == 1,
                )
            }
        }
        return out.values.toList()
    }
}
