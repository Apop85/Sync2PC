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
        /** Antwortet unter der Adresse ein anderer/noch nicht erkannter PC: verständliche Meldung (keine Anmeldung erfolgt), sonst null */
        val pcMessage: String? = null,
    )

    suspend fun load(context: Context, profile: Profile): Loaded = withContext(Dispatchers.IO) {
        val local = try {
            val uri = Uri.parse(profile.localTreeUri)
            if (LocalTree.hasPermission(context, uri)) LocalTree(context, uri).listTrash() else emptyList()
        } catch (e: Exception) { emptyList() }
        var pcOk = false
        var pcMessage: String? = null
        val remote = try {
            val host = SmbTarget.parse(profile.smbPath)?.host
            if (host != null && SmbSession.isReachable(host)) {
                SmbSession.open(profile).use { s -> remoteTrash(s).also { pcOk = true } }
            } else emptyList()
        } catch (e: com.sync2pc.app.sync.PcIdentityException) { pcMessage = e.message; emptyList()
        } catch (e: Exception) { emptyList() }
        val dao = AppDatabase.get(context).dao()
        val existing = dao.mirrorStates(profile.id).map { it.relPath }.toSet()
        // Grösse für die Kachel "Papierkorb" merken (0.6.0)
        PurgeStore(context).saveSizes(profile.id, local.sumOf { it.size }, if (pcOk) remote.sumOf { it.size } else -1)
        Loaded(
            (local + remote).sortedWith(compareBy<TrashEntry>({ it.origPath.lowercase() }).thenByDescending { it.date }),
            pcOk, existing, TrashPurge.reservedKeys(dao.pendingRestores(profile.id)), pcMessage,
        )
    }

    /** Papierkorb auf dem PC (neuer und alter Name) */
    fun remoteTrash(s: SmbSession): List<TrashEntry> {
        val out = ArrayList<TrashEntry>()
        for (t in MirrorEngine.TRASH_DIRS) {
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

    /**
     * "Erneut versuchen" (0.11.8): Versuchszähler der fehlgeschlagenen Wiederherstellungen zurücksetzen und einen
     * Sync anstossen. Die Vormerkung bleibt unverändert; nach weiteren 5 Fehlschlägen wird wieder pausiert.
     */
    fun retryFailed(context: Context, profileIds: Collection<Long>) {
        val f = RestoreFailures(context)
        f.resetAttempts(f.restoreIds(profileIds))
        SyncScheduler.enqueueManual(context)
    }
}

/** Regel für automatische Wiederholungen fehlgeschlagener Wiederherstellungen (0.11.8, DEC-146) */
object RestoreRetryPolicy {
    /** Nach so vielen Fehlversuchen (ohne Netzwerkfehler) wird nicht mehr automatisch wiederholt */
    const val MAX_ATTEMPTS = 5

    fun exhausted(attempts: Int): Boolean = attempts >= MAX_ATTEMPTS

    /** Zähler "restoreId:n" nach einem weiteren Fehlversuch; liefert neue Menge und neuen Stand */
    fun bump(entries: Set<String>, restoreId: Long): Pair<Set<String>, Int> {
        val id = restoreId.toString()
        val old = entries.firstOrNull { it.substringBefore(':') == id }?.substringAfter(':')?.toIntOrNull() ?: 0
        val now = old + 1
        return (entries.filterNot { it.substringBefore(':') == id } + "$id:$now").toSet() to now
    }

    fun count(entries: Set<String>, restoreId: Long): Int =
        entries.firstOrNull { it.substringBefore(':') == restoreId.toString() }?.substringAfter(':')?.toIntOrNull() ?: 0
}

/**
 * Wiederherstellungen, die zurückgerollt wurden und beim nächsten Sync erneut versucht werden (0.7.0), höchstens
 * [RestoreRetryPolicy.MAX_ATTEMPTS]-mal (0.11.8). Danach bleibt die Vormerkung (und damit die Papierkorb-Version
 * geschützt) bestehen, wird aber nicht mehr automatisch ausgeführt, bis der Benutzer "Erneut versuchen" oder
 * "Verwerfen" wählt. Die Vormerkung selbst bleibt in der Datenbank; hier nur Markierung und Zähler (Prefs).
 */
class RestoreFailures(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("restore", Context.MODE_PRIVATE)

    /** Einträge "profilId:restoreId" */
    private fun all(): Set<String> = prefs.getStringSet("failed", emptySet()) ?: emptySet()

    /** Einträge "restoreId:Anzahl Fehlversuche" */
    private fun attempts(): Set<String> = prefs.getStringSet("attempts", emptySet()) ?: emptySet()

    /** Fehlversuch festhalten (Netzwerkfehler zählen nicht, dort bleibt die Vormerkung einfach liegen). Liefert den Stand. */
    fun mark(profileId: Long, restoreId: Long): Int = synchronized(LOCK) {
        val (next, n) = RestoreRetryPolicy.bump(attempts(), restoreId)
        prefs.edit().putStringSet("failed", all() + "$profileId:$restoreId").putStringSet("attempts", next).apply()
        n
    }

    /** true = die automatischen Versuche sind aufgebraucht, die Vormerkung wartet auf den Benutzer */
    fun isExhausted(restoreId: Long): Boolean = RestoreRetryPolicy.exhausted(RestoreRetryPolicy.count(attempts(), restoreId))

    /** Anzahl der Wiederherstellungen mit aufgebrauchten Versuchen (nur gegebene Profile) */
    fun exhaustedCount(profileIds: Collection<Long>): Int = restoreIds(profileIds).count { isExhausted(it) }

    fun resetAttempts(restoreIds: Collection<Long>) = synchronized(LOCK) {
        val ids = restoreIds.map { it.toString() }.toSet()
        prefs.edit().putStringSet("attempts", attempts().filterNot { it.substringBefore(':') in ids }.toSet()).apply()
    }

    fun clear(restoreId: Long) = synchronized(LOCK) {
        val left = all().filterNot { it.substringAfter(':') == restoreId.toString() }.toSet()
        prefs.edit().putStringSet("failed", left).apply()
        resetAttempts(listOf(restoreId))
    }

    fun restoreIds(profileIds: Collection<Long>): List<Long> = all().mapNotNull { e ->
        val p = e.substringBefore(':').toLongOrNull()
        if (p != null && p in profileIds) e.substringAfter(':').toLongOrNull() else null
    }

    /** Profile mit fehlgeschlagener Wiederherstellung */
    fun profiles(): Set<Long> = all().mapNotNull { it.substringBefore(':').toLongOrNull() }.toSet()

    fun forget(profileId: Long) = synchronized(LOCK) {
        resetAttempts(all().filter { it.substringBefore(':') == profileId.toString() }.mapNotNull { it.substringAfter(':').toLongOrNull() })
        prefs.edit().putStringSet("failed", all().filterNot { it.startsWith("$profileId:") }.toSet()).apply()
    }

    private companion object {
        /** Lesen-ändern-Schreiben auf den Prefs: Sync-Lauf und Oberfläche dürfen sich nicht überschneiden (0.11.9) */
        val LOCK = Any()
    }
}
