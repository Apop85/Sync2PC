package com.sync2pc.app.mirror

import android.content.Context
import android.net.Uri
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.PendingRestore
import com.sync2pc.app.data.Profile
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.smb.SmbTarget
import com.sync2pc.app.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "Papierkorb & alte Versionen" (0.5.0): Papierkorb beider Seiten einlesen und Wiederherstellungen vormerken.
 * Ausgeführt werden sie vom nächsten Ordner-Sync mit erreichbarem PC (MirrorEngine.runRestores) – so blockiert
 * nichts, wenn der PC gerade aus ist.
 */
object RestoreManager {

    data class Loaded(
        val entries: List<TrashEntry>,
        /** PC erreichbar? Sonst fehlen die Versionen aus dem PC-Papierkorb */
        val pcAvailable: Boolean,
        /** Schlüssel (Kleinbuchstaben) der Dateien, die es laut Sync-Stand gerade gibt -> "ältere Version" */
        val existing: Set<String>,
        /** Zum Wiederherstellen vorgemerkte Einträge ([TrashPurge.key]) – werden nie gelöscht (0.7.0) */
        val reserved: Set<String> = emptySet(),
    )

    suspend fun load(context: Context, profile: Profile): Loaded = withContext(Dispatchers.IO) {
        val local = try {
            val uri = Uri.parse(profile.localTreeUri)
            if (LocalTree.hasPermission(context, uri)) LocalTree(context, uri).listTrash() else emptyList()
        } catch (e: Exception) { emptyList() }
        var pcOk = false
        val remote = try {
            val host = SmbTarget.parse(profile.smbPath)?.host
            if (host != null && SmbSession.isReachable(host)) {
                SmbSession.open(profile).use { s -> remoteTrash(s).also { pcOk = true } }
            } else emptyList()
        } catch (e: Exception) { emptyList() }
        val dao = AppDatabase.get(context).dao()
        val existing = dao.mirrorStates(profile.id).map { it.relPath }.toSet()
        // Grösse für die Kachel "Papierkorb" merken (0.6.0)
        PurgeStore(context).saveSizes(profile.id, local.sumOf { it.size }, if (pcOk) remote.sumOf { it.size } else -1)
        Loaded(
            (local + remote).sortedWith(compareBy<TrashEntry>({ it.origPath.lowercase() }).thenByDescending { it.date }),
            pcOk, existing, TrashPurge.reservedKeys(dao.pendingRestores(profile.id)),
        )
    }

    /** Papierkorb auf dem PC (neuer und alter Name) */
    private fun remoteTrash(s: SmbSession): List<TrashEntry> {
        val out = ArrayList<TrashEntry>()
        for (t in listOf(MirrorEngine.TRASH_DIR, MirrorEngine.OLD_TRASH_DIR)) {
            val base = s.path(t)
            if (!s.dirExists(base)) continue
            val files = s.listRecursive(base).filterKeys { !it.endsWith(SmbSession.TEMP_SUFFIX, true) }
            val siblings = files.keys.groupBy({ it.substringBeforeLast('/', "") }, { it.substringAfterLast('/').lowercase() })
            for ((rel, e) in files) {
                val names = siblings[rel.substringBeforeLast('/', "")].orEmpty().toSet()
                TrashIndex.entry(Side.REMOTE, "$t/$rel", e.size, e.modified, names)?.let { out += it }
            }
        }
        return out
    }

    /** Vormerken und einen Sync anstossen. Ist der PC aus, läuft es beim nächsten Sync mit PC. */
    suspend fun request(context: Context, profileId: Long, entries: List<TrashEntry>) {
        if (entries.isEmpty()) return
        val now = System.currentTimeMillis()
        AppDatabase.get(context).dao().insertRestores(
            TrashIndex.select(emptyList(), entries).map {
                PendingRestore(profileId = profileId, side = it.side.name, trashPath = it.trashPath,
                    origPath = it.origPath, requestedAt = now)
            }
        )
        SyncScheduler.enqueueManual(context)
    }

    /** Fehlgeschlagene Wiederherstellungen verwerfen (Knopf auf der Startseite) */
    suspend fun discardFailed(context: Context, profileIds: Collection<Long>) {
        val dao = AppDatabase.get(context).dao()
        val f = RestoreFailures(context)
        for (id in f.restoreIds(profileIds)) { dao.deleteRestore(id); f.clear(id) }
    }
}

/**
 * Wiederherstellungen, die zurückgerollt wurden und beim nächsten Sync erneut versucht werden (0.7.0).
 * Die Vormerkung selbst bleibt in der Datenbank; hier nur die Markierung für den Hinweis auf der Startseite.
 */
class RestoreFailures(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("restore", Context.MODE_PRIVATE)

    /** Einträge "profilId:restoreId" */
    private fun all(): Set<String> = prefs.getStringSet("failed", emptySet()) ?: emptySet()

    fun mark(profileId: Long, restoreId: Long) {
        prefs.edit().putStringSet("failed", all() + "$profileId:$restoreId").apply()
    }

    fun clear(restoreId: Long) {
        val left = all().filterNot { it.substringAfter(':') == restoreId.toString() }.toSet()
        prefs.edit().putStringSet("failed", left).apply()
    }

    fun restoreIds(profileIds: Collection<Long>): List<Long> = all().mapNotNull { e ->
        val p = e.substringBefore(':').toLongOrNull()
        if (p != null && p in profileIds) e.substringAfter(':').toLongOrNull() else null
    }

    /** Profile mit fehlgeschlagener Wiederherstellung */
    fun profiles(): Set<Long> = all().mapNotNull { it.substringBefore(':').toLongOrNull() }.toSet()

    fun forget(profileId: Long) {
        prefs.edit().putStringSet("failed", all().filterNot { it.startsWith("$profileId:") }.toSet()).apply()
    }
}
