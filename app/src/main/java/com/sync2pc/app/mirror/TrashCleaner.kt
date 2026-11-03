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

    private fun localTree(context: Context, profile: Profile): LocalTree? = try {
        val uri = Uri.parse(profile.localTreeUri)
        if (LocalTree.hasPermission(context, uri)) LocalTree(context, uri) else null
    } catch (e: Exception) { null }

    /** PC-Verbindung, wenn erreichbar (kurzer Test vorab), sonst null */
    private fun openPc(profile: Profile): SmbSession? = try {
        val host = SmbTarget.parse(profile.smbPath)?.host
        if (host != null && SmbSession.isReachable(host)) SmbSession.open(profile) else null
    } catch (e: Exception) { null }

    /** Papierkorb nach [rule] auf den gewählten Seiten leeren (wartet auf einen laufenden Ordner-Sync) */
    suspend fun purge(context: Context, profile: Profile, rule: PurgeRule, sides: PurgeSides): Result =
        withContext(Dispatchers.IO) {
            MirrorLock.mutex.withLock {
                val pending = AppDatabase.get(context).dao().pendingRestores(profile.id)
                val local = if (sides.local)
                    localTree(context, profile)?.purgeTrash(rule, TrashPurge.reservedPaths(pending, Side.LOCAL)) ?: 0 else 0
                var remote = 0
                var remotePending = false
                if (sides.remote) {
                    val s = openPc(profile)
                    if (s == null) { PurgeStore(context).addRule(profile.id, rule); remotePending = true }
                    else s.use { remote = purgeRemote(it, rule, TrashPurge.reservedPaths(pending, Side.REMOTE)) }
                }
                Result(local, remote, remotePending)
            }
        }

    /** Ausgewählte Einträge endgültig löschen; vorgemerkte Wiederherstellungen bleiben (wartet auf laufenden Sync) */
    suspend fun deleteEntries(context: Context, profile: Profile, entries: List<TrashEntry>): Result =
        withContext(Dispatchers.IO) {
            MirrorLock.mutex.withLock {
                val reserved = TrashPurge.reservedKeys(AppDatabase.get(context).dao().pendingRestores(profile.id))
                val list = entries.filter { TrashPurge.key(it) !in reserved }
                val tree = localTree(context, profile)
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
