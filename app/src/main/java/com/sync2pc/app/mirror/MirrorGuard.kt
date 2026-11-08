package com.sync2pc.app.mirror

import android.content.Context
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.sync.SyncScheduler
import kotlin.random.Random

/**
 * Schutzbremse des Ordner-Syncs (0.4.1): Ein Lauf hält an und wartet auf den Benutzer, wenn
 *  - der Basisordner auf dem PC oder dem Handy fehlt bzw. leer ist, obwohl schon synchronisiert wurde,
 *  - mehr als 20 % der synchronisierten Dateien in den Papierkorb kämen,
 *  - nach einem Ordnerwechsel die Stichprobe nicht passt ("Ist das wirklich der richtige Ordner?").
 */
enum class GuardKind { REMOTE_MISSING, LOCAL_MISSING, MASS_DELETE, CHECK_FOLDER }

/**
 * Offene Entscheidung eines Profils. [count]/[total] nur bei MASS_DELETE; [folder] = ganzer Unterordner, der
 * verschwinden würde (0.4.2), leer = viele Löschungen insgesamt.
 */
data class Guard(val profileId: Long, val kind: GuardKind, val count: Int = 0, val total: Int = 0, val folder: String = "")

/** Seite eines Ordner-Profils */
enum class Side { LOCAL, REMOTE }

/** Reine Regeln (ohne Android, getestet in MirrorGuardTest) */
object GuardPolicy {
    /** Ab diesem Anteil (in Prozent) gelöschter Dateien hält der Lauf an */
    const val MASS_DELETE_PERCENT = 20
    /** Anzahl Dateien der Stichprobe */
    const val SAMPLE_FILES = 20
    /** Höchstens so viele Ordner der obersten Ebene prüfen */
    const val SAMPLE_DIRS = 20
    /** So viele Dateien der Stichprobe müssen gleich gross sein (der Rest darf inzwischen geändert sein) */
    const val SAMPLE_SIZE_PERCENT = 80

    /** Kleine Ordner (höchstens so viele synchronisierte Dateien): nur fragen, wenn alles gelöscht würde (0.4.2) */
    const val SMALL_FOLDER_FILES = 5
    /** Ab so vielen Löschungen auf einmal immer nachfragen, egal wie gross das Profil ist (0.4.2) */
    const val MAX_DELETE_FILES = 100

    /**
     * Anhalten wegen vieler Löschungen? Mehr als [percent] % (Standard 20, pro Profil einstellbar) der bisher
     * synchronisierten Dateien kämen in den Papierkorb. Kleine Ordner (≤ 5 Dateien): nur, wenn alle Dateien
     * gelöscht würden – sonst fragte schon jede einzelne Löschung.
     */
    fun massDelete(toTrash: Int, synced: Int, percent: Int = MASS_DELETE_PERCENT): Boolean {
        if (synced <= 0 || toTrash <= 0) return false
        if (toTrash >= MAX_DELETE_FILES) return true
        if (synced <= SMALL_FOLDER_FILES) return toTrash >= synced
        return toTrash * 100 > synced * percent.coerceIn(1, 100)
    }

    /**
     * Würde ein ganzer Unterordner mit mehr als 5 Dateien verschwinden (alle seine synchronisierten Dateien kämen in
     * den Papierkorb)? Liefert den obersten solchen Ordner in Originalschreibweise, sonst null.
     * [trashed] = relative Pfade der Löschungen (Originalschreibweise), [synced] = Schlüssel aus mirror_state.
     */
    fun vanishedFolder(trashed: Collection<String>, synced: Collection<String>): String? {
        if (trashed.isEmpty()) return null
        fun dirsOf(path: String): List<String> {
            val parts = path.split('/').dropLast(1)
            return parts.indices.map { i -> parts.subList(0, i + 1).joinToString("/") }
        }
        val syncedCount = HashMap<String, Int>()
        for (k in synced) for (d in dirsOf(k.lowercase())) syncedCount[d] = (syncedCount[d] ?: 0) + 1
        val trashCount = HashMap<String, Int>()
        val original = HashMap<String, String>()
        for (t in trashed) for (d in dirsOf(t)) {
            val key = d.lowercase()
            trashCount[key] = (trashCount[key] ?: 0) + 1
            original.putIfAbsent(key, d)
        }
        return trashCount.entries
            .filter { (d, n) -> n > SMALL_FOLDER_FILES && n >= (syncedCount[d] ?: Int.MAX_VALUE) }
            .minWithOrNull(compareBy<Map.Entry<String, Int>>({ it.key.count { c -> c == '/' } }, { it.key }))
            ?.let { original[it.key] }
    }

    /** Zufällige Stichprobe der bisher synchronisierten Dateien */
    fun sample(states: List<MirrorState>, n: Int = SAMPLE_FILES, random: Random = Random.Default): List<MirrorState> =
        if (states.size <= n) states else states.shuffled(random).take(n)

    /** Ordner der obersten Ebene (Kleinbuchstaben), die laut Sync-Status existieren müssten */
    fun topDirs(keys: Collection<String>, max: Int = SAMPLE_DIRS): List<String> =
        keys.filter { '/' in it }.map { it.substringBefore('/').lowercase() }.distinct().sorted().take(max)

    /**
     * Passt der Ordner zum Sync-Status? Alle Dateien der Stichprobe und alle Ordner müssen vorhanden sein,
     * mindestens 80 % der Dateien gleich gross. [size] liefert die Grösse einer Datei (null = fehlt),
     * [dirExists] prüft einen Ordner. Pfade sind die Schlüssel aus mirror_state (Kleinbuchstaben).
     */
    fun matches(
        sample: List<MirrorState>, dirs: List<String>, side: Side,
        size: (String) -> Long?, dirExists: (String) -> Boolean,
    ): Boolean {
        if (dirs.any { !dirExists(it) }) return false
        var sameSize = 0
        for (st in sample) {
            val s = size(st.relPath) ?: return false
            if (s == (if (side == Side.LOCAL) st.localSize else st.remoteSize)) sameSize++
        }
        return sample.isEmpty() || sameSize * 100 >= sample.size * SAMPLE_SIZE_PERCENT
    }
}

/** Offene Entscheidungen und einmalige Freigaben je Profil (SharedPreferences, keine DB-Migration nötig) */
class GuardStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("guard", Context.MODE_PRIVATE)

    fun get(profileId: Long): Guard? {
        val v = prefs.getString("block_$profileId", null) ?: return null
        // Ordnername zuletzt: darf selbst ";" enthalten
        val parts = v.split(';', limit = 4)
        val kind = try { GuardKind.valueOf(parts[0]) } catch (e: Exception) { return null }
        return Guard(
            profileId, kind, parts.getOrNull(1)?.toIntOrNull() ?: 0, parts.getOrNull(2)?.toIntOrNull() ?: 0,
            parts.getOrNull(3) ?: "",
        )
    }

    fun all(): List<Guard> = prefs.all.keys.filter { it.startsWith("block_") }
        .mapNotNull { it.removePrefix("block_").toLongOrNull()?.let(::get) }

    fun blockedIds(): Set<Long> = all().map { it.profileId }.toSet()

    fun set(g: Guard) {
        prefs.edit().putString("block_${g.profileId}", "${g.kind.name};${g.count};${g.total};${g.folder}").apply()
    }

    fun clear(profileId: Long) { prefs.edit().remove("block_$profileId").apply() }

    /** Alles zu einem Profil vergessen (Profil gelöscht, Sync-Status zurückgesetzt) */
    fun forget(profileId: Long) {
        prefs.edit().remove("block_$profileId").remove("check_$profileId")
            .remove("allowDelete_$profileId").remove("keepFiles_$profileId").apply()
    }

    /** Nach einem Ordnerwechsel: Seiten, deren Stichprobe beim nächsten Lauf noch aussteht */
    fun pendingCheck(profileId: Long): Set<Side> =
        (prefs.getString("check_$profileId", "") ?: "").split(',').mapNotNull {
            try { Side.valueOf(it) } catch (e: Exception) { null }
        }.toSet()

    fun setPendingCheck(profileId: Long, sides: Set<Side>) {
        if (sides.isEmpty()) prefs.edit().remove("check_$profileId").apply()
        else prefs.edit().putString("check_$profileId", sides.joinToString(",") { it.name }).apply()
    }

    /** "Trotzdem löschen": Die Schutzbremse für viele Löschungen greift beim nächsten Lauf einmal nicht */
    fun allowDeleteOnce(profileId: Long) { prefs.edit().putBoolean("allowDelete_$profileId", true).apply() }
    fun takeAllowDelete(profileId: Long): Boolean = takeFlag("allowDelete_$profileId")

    /** "Nicht löschen": Der nächste Lauf übernimmt keine Löschungen, sondern stellt wieder her bzw. behält */
    fun keepFilesOnce(profileId: Long) { prefs.edit().putBoolean("keepFiles_$profileId", true).apply() }
    fun takeKeepFiles(profileId: Long): Boolean = takeFlag("keepFiles_$profileId")

    private fun takeFlag(key: String): Boolean {
        val v = prefs.getBoolean(key, false)
        if (v) prefs.edit().remove(key).apply()
        return v
    }
}

/**
 * Stichprobe direkt beim Speichern eines Profils mit gewechseltem Ordner (0.4.1): prüft etwa 20 Dateien und
 * die obersten Ordner aus dem Sync-Stand, ohne die ganzen Ordner einzulesen.
 */
object FolderCheck {
    enum class Result { OK, MISMATCH, UNAVAILABLE }

    suspend fun check(context: Context, profile: com.sync2pc.app.data.Profile, sides: Set<Side>): Result =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val states = AppDatabase.get(context).dao().mirrorStates(profile.id)
            if (states.isEmpty() || sides.isEmpty()) return@withContext Result.OK
            val sample = GuardPolicy.sample(states)
            val dirs = GuardPolicy.topDirs(states.map { it.relPath })
            try {
                if (Side.LOCAL in sides) {
                    val uri = android.net.Uri.parse(profile.localTreeUri)
                    if (!LocalTree.hasPermission(context, uri)) return@withContext Result.UNAVAILABLE
                    val tree = LocalTree(context, uri)
                    if (tree.stat(tree.rootDocId) == null) return@withContext Result.MISMATCH
                    val ok = GuardPolicy.matches(sample, dirs, Side.LOCAL, { tree.find(it)?.size }, { tree.findDir(it) != null })
                    if (!ok) return@withContext Result.MISMATCH
                }
                if (Side.REMOTE in sides) {
                    val host = com.sync2pc.app.smb.SmbTarget.parse(profile.smbPath)?.host
                        ?: return@withContext Result.UNAVAILABLE
                    if (!com.sync2pc.app.smb.SmbSession.isReachable(host)) return@withContext Result.UNAVAILABLE
                    val ok = com.sync2pc.app.smb.SmbSession.open(profile).use { s ->
                        val base = s.path()
                        s.dirExists(base) && GuardPolicy.matches(
                            sample, dirs, Side.REMOTE,
                            { s.stat(s.path(it.replace('/', '\\')))?.size },
                            { s.dirExists(s.path(it.replace('/', '\\'))) },
                        )
                    }
                    if (!ok) return@withContext Result.MISMATCH
                }
                Result.OK
            } catch (e: Exception) {
                // PC nicht erreichbar, Anmeldung fehlgeschlagen … -> der nächste Lauf prüft
                Result.UNAVAILABLE
            }
        }
}

/** Antworten des Benutzers auf eine offene Entscheidung */
object MirrorGuard {
    enum class Choice {
        /** Viele Löschungen: wie geplant ausführen */
        DELETE_ANYWAY,
        /** Viele Löschungen: nicht übernehmen, Dateien behalten bzw. wiederherstellen */
        KEEP_FILES,
        /** Ordner-Warnung: Ordner trotzdem übernehmen (Sync-Status bleibt) */
        ACCEPT_FOLDER,
        /** Als neuen Ordner behandeln bzw. neu anlegen: Sync-Status zurücksetzen, Erstabgleich ohne Löschen */
        RESET_AS_NEW,
    }

    suspend fun resolve(context: Context, profileId: Long, choice: Choice) {
        val store = GuardStore(context)
        when (choice) {
            Choice.DELETE_ANYWAY -> store.allowDeleteOnce(profileId)
            Choice.KEEP_FILES -> store.keepFilesOnce(profileId)
            Choice.ACCEPT_FOLDER -> store.setPendingCheck(profileId, emptySet())
            Choice.RESET_AS_NEW -> {
                val dao = AppDatabase.get(context).dao()
                dao.clearMirrorState(profileId); dao.clearConflicts(profileId)
                store.setPendingCheck(profileId, emptySet())
            }
        }
        store.clear(profileId)
        SyncScheduler.enqueueManual(context)
    }

    /** Text der Warnung (Startseite und Benachrichtigung) */
    fun title(g: Guard): String = titleMsg(g).text()

    /** Wie [title], als Kennung für den Verlauf (0.12.0) */
    fun titleMsg(g: Guard): com.sync2pc.app.l10n.Msg = com.sync2pc.app.l10n.Msg.of(
        when (g.kind) {
            GuardKind.REMOTE_MISSING -> com.sync2pc.app.R.string.guard_title_remote
            GuardKind.LOCAL_MISSING -> com.sync2pc.app.R.string.guard_title_local
            GuardKind.MASS_DELETE -> com.sync2pc.app.R.string.guard_title_mass
            GuardKind.CHECK_FOLDER -> com.sync2pc.app.R.string.profile_folder_warning_title
        }
    )

    /**
     * Speichern eines Profils mit gewechseltem Ordner abschliessen: [result] der Stichprobe bzw. die
     * Entscheidung des Benutzers. Eine offene Schutzbremse des Profils ist damit erledigt.
     */
    suspend fun afterFolderChange(context: Context, profileId: Long, sides: Set<Side>, resetState: Boolean, checked: Boolean) {
        val store = GuardStore(context)
        if (resetState) {
            val dao = AppDatabase.get(context).dao()
            dao.clearMirrorState(profileId); dao.clearConflicts(profileId)
        }
        // Nicht geprüft (PC nicht erreichbar) -> Stichprobe beim nächsten Lauf
        store.setPendingCheck(profileId, if (resetState || checked) emptySet() else sides)
        store.clear(profileId)
    }

    fun text(g: Guard, profileName: String): String = when (g.kind) {
        GuardKind.REMOTE_MISSING -> com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.guard_text_remote, profileName)
        GuardKind.LOCAL_MISSING -> com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.guard_text_local, profileName)
        GuardKind.MASS_DELETE -> if (g.folder.isNotEmpty())
            com.sync2pc.app.l10n.plural(com.sync2pc.app.R.plurals.guard_text_folder, g.count, profileName, g.folder, g.count)
        else com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.guard_text_mass, profileName, g.count, g.total)
        GuardKind.CHECK_FOLDER -> com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.guard_text_check, profileName)
    }
}
