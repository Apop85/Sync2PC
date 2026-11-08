package com.sync2pc.app.mirror

import android.content.Context
import android.net.Uri
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.smb.SmbTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * "Papierkorb leeren" und "Endgültig löschen" (0.6.0). Das Handy wird sofort geleert. Der PC sofort, wenn er
 * erreichbar ist – sonst wird es vorgemerkt ([PurgeStore]) und vom nächsten Ordner-Sync mit PC erledigt.
 * Seit 0.7.0: wartet auf einen laufenden Ordner-Sync ([MirrorLock]) und lässt vorgemerkte Wiederherstellungen liegen.
 */
object TrashCleaner {

    data class Result(val local: Int, val remote: Int, val remotePending: Boolean)

    /**
     * PC-Papierkorb nach Regel leeren. [keep]: Papierkorb-Pfade (Kleinbuchstaben, "/"), die bleiben müssen.
     * Liefert die Anzahl gelöschter Ordner bzw. einzelner Dateien.
     */
    fun purgeRemote(s: SmbSession, rule: PurgeRule, keep: Set<String> = emptySet()): Int {
        var n = 0
        for (t in MirrorEngine.TRASH_DIRS) {
            val base = s.path(t)
            if (!s.dirExists(base)) continue
            val keepHere = keep.filter { it.startsWith(t.lowercase() + "/") }
            if (rule.mode == PurgeRule.Mode.ALL && keepHere.isEmpty()) { s.deleteDir(base); n++; continue }
            for (e in s.listDir(base)) {
                if (!e.isDirectory || !rule.matches(e.name)) continue
                val prefix = "$t/${e.name}/".lowercase()
                if (keepHere.none { it.startsWith(prefix) }) { s.deleteDir("$base\\${e.name}"); n++ }
                else {
                    // Nur die nicht vorgemerkten Dateien dieses Tages löschen
                    for ((sub, _) in s.listRecursive("$base\\${e.name}")) {
                        val rel = "$t/${e.name}/$sub"
                        if (rel.lowercase() !in keep) { s.delete(s.path(rel.replace('/', '\\'))); n++ }
                    }
                }
            }
        }
        return n
    }

    fun deleteRemoteFile(s: SmbSession, trashPath: String): Boolean {
        val p = s.path(trashPath.replace('/', '\\'))
        if (s.stat(p) == null) return false
        s.delete(p)
        return true
    }

    private fun localAccessLost() =
        java.io.IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.err_local_access_lost))

    private fun localTree(context: Context, profile: Profile): LocalTree? = try {
        val uri = Uri.parse(profile.localTreeUri)
        if (LocalTree.hasPermission(context, uri)) LocalTree(context, uri) else null
    } catch (e: Exception) { null }

    /** PC-Verbindung, wenn erreichbar (kurzer Test vorab), sonst null */
    private fun openPc(profile: Profile): SmbSession? = try {
        val host = SmbTarget.parse(profile.smbPath)?.host
        if (host != null && SmbSession.isReachable(host)) SmbSession.open(profile) else null
    } catch (e: Exception) { null }

    /**
     * Papierkorb aufräumen (wartet auf einen laufenden Ordner-Sync). Seit 0.11.10 (DEC-144) wird **nur der bestätigte
     * Stand** ausgeführt: [confirmed] sind die Einträge, die der Benutzer vor dem Bestätigen gesehen hat; unter der
     * Sperre wird neu eingelesen und nur gelöscht, was davon unverändert noch da und nicht zum Wiederherstellen
     * vorgemerkt ist ([TrashPurge.planConfirmed]). Was ein Sync inzwischen in den Papierkorb gelegt hat, bleibt liegen.
     * [pcKnown] = der PC-Papierkorb war beim Einlesen bekannt; sonst gilt wie bisher die Regel [rule] (vorgemerkt bzw.
     * sofort, wenn der PC jetzt erreichbar ist). [onStart] wird aufgerufen, sobald die Sperre erworben ist.
     */
    suspend fun purgeConfirmed(
        context: Context, profile: Profile, rule: PurgeRule, sides: PurgeSides,
        confirmed: List<TrashEntry>, pcKnown: Boolean, onStart: () -> Unit = {},
    ): Result = withContext(Dispatchers.IO) {
        MirrorLock.mutex.withLock {
            onStart()
            val pending = AppDatabase.get(context).dao().pendingRestores(profile.id)
            val reserved = TrashPurge.reservedKeys(pending)
            var local = 0
            if (sides.local) {
                val tree = localTree(context, profile)
                // Handy-Berechtigung weg: Fehler melden, bevor irgendetwas (auch auf dem PC) gelöscht wird (QA -035)
                if (tree == null && confirmed.any { it.side == Side.LOCAL }) throw localAccessLost()
                if (tree != null) {
                    val plan = TrashPurge.planConfirmed(tree.listTrash(), confirmed.filter { it.side == Side.LOCAL }, reserved)
                    for (d in plan.wholeDirs) if (tree.deleteTrashDir(d)) local++
                    for (f in plan.files) if (tree.deleteTrashFile(f.trashPath)) local++
                    // Leer gewordene Ordner entfernen (nur wirklich leere; Fehler hier sind unwichtig)
                    val cleanup = FolderFreer.emptyDirCandidates(plan.files.map { it.trashPath }) { false } +
                        plan.wholeDirs.map { it.substringBefore('/') }.distinct()
                    for (dir in cleanup) try { tree.deleteDirIfEmpty(dir) } catch (e: Exception) { /* bleibt bestehen */ }
                }
            }
            var remote = 0
            var remotePending = false
            if (sides.remote) {
                val pcConfirmed = confirmed.filter { it.side == Side.REMOTE }
                val s = openPc(profile)
                if (!pcKnown) {
                    // PC war beim Einlesen unbekannt: wie bisher die Regel (vorgemerkt oder jetzt ausgeführt)
                    if (s == null) { PurgeStore(context).addRule(profile.id, rule); remotePending = true }
                    else s.use { remote = purgeRemote(it, rule, TrashPurge.reservedPaths(pending, Side.REMOTE)) }
                } else if (pcConfirmed.isNotEmpty()) {
                    if (s == null) { PurgeStore(context).addFiles(profile.id, pcConfirmed.map { it.trashPath }); remotePending = true }
                    else try {
                        s.use { pc ->
                            val plan = TrashPurge.planConfirmed(RestoreManager.remoteTrash(pc), pcConfirmed, reserved)
                            for (d in plan.wholeDirs) { pc.deleteDir(pc.path(d.replace('/', '\\'))); remote++ }
                            for (f in plan.files) if (deleteRemoteFile(pc, f.trashPath)) remote++
                            // Leer gewordene Datumsordner entfernen
                            for (d in plan.files.mapNotNull { TrashPurge.dateDir(it.trashPath) }.distinct()) {
                                val p = pc.path(d.replace('/', '\\'))
                                if (pc.dirExists(p) && pc.listRecursive(p).isEmpty()) pc.deleteDir(p)
                            }
                        }
                    } catch (e: Exception) {
                        if (!com.sync2pc.app.sync.isNetworkError(e)) throw e
                        // Verbindung weg: genau die bestätigten Dateien vormerken (nie mehr als gesehen)
                        PurgeStore(context).addFiles(profile.id, pcConfirmed.map { it.trashPath }); remotePending = true
                    }
                }
            }
            Result(local, remote, remotePending)
        }
    }

    /**
     * Ausgewählte Einträge endgültig löschen; vorgemerkte Wiederherstellungen bleiben (wartet auf laufenden Sync).
     * [onStart] wird aufgerufen, sobald die Sperre erworben ist.
     */
    suspend fun deleteEntries(
        context: Context, profile: Profile, entries: List<TrashEntry>, onStart: () -> Unit = {},
    ): Result =
        withContext(Dispatchers.IO) {
            MirrorLock.mutex.withLock {
                onStart()
                val reserved = TrashPurge.reservedKeys(AppDatabase.get(context).dao().pendingRestores(profile.id))
                val list = entries.filter { TrashPurge.key(it) !in reserved }
                val tree = localTree(context, profile)
                if (tree == null && list.any { it.side == Side.LOCAL }) throw localAccessLost() // nichts löschen, Fehler melden
                var local = 0
                for (e in list.filter { it.side == Side.LOCAL }) {
                    if (tree?.deleteTrashFile(e.trashPath) == true) local++
                }
                val pcEntries = list.filter { it.side == Side.REMOTE }
                var remote = 0
                var remotePending = false
                if (pcEntries.isNotEmpty()) {
                    val s = openPc(profile)
                    if (s == null) { PurgeStore(context).addFiles(profile.id, pcEntries.map { it.trashPath }); remotePending = true }
                    else s.use { pc -> for (e in pcEntries) if (deleteRemoteFile(pc, e.trashPath)) remote++ }
                }
                Result(local, remote, remotePending)
            }
        }
}
