package com.sync2pc.app.mirror

import android.content.Context
import android.net.Uri
import com.sync2pc.app.data.Profile
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.smb.SmbTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "Papierkorb leeren" und "Endgültig löschen" (0.6.0). Das Handy wird sofort geleert. Der PC sofort, wenn er
 * erreichbar ist – sonst wird es vorgemerkt ([PurgeStore]) und vom nächsten Ordner-Sync mit PC erledigt.
 */
object TrashCleaner {

    data class Result(val local: Int, val remote: Int, val remotePending: Boolean)

    /** PC-Papierkorb nach Regel leeren. Liefert die Anzahl gelöschter Ordner. */
    fun purgeRemote(s: SmbSession, rule: PurgeRule): Int {
        var n = 0
        for (t in listOf(MirrorEngine.TRASH_DIR, MirrorEngine.OLD_TRASH_DIR)) {
            val base = s.path(t)
            if (!s.dirExists(base)) continue
            if (rule.mode == PurgeRule.Mode.ALL) { s.deleteDir(base); n++; continue }
            for (e in s.listDir(base)) {
                if (e.isDirectory && rule.matches(e.name)) { s.deleteDir("$base\\${e.name}"); n++ }
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

    /** Papierkorb nach [rule] auf den gewählten Seiten leeren */
    suspend fun purge(context: Context, profile: Profile, rule: PurgeRule, sides: PurgeSides): Result =
        withContext(Dispatchers.IO) {
            val local = if (sides.local) localTree(context, profile)?.purgeTrash(rule) ?: 0 else 0
            var remote = 0
            var pending = false
            if (sides.remote) {
                val s = openPc(profile)
                if (s == null) { PurgeStore(context).addRule(profile.id, rule); pending = true }
                else s.use { remote = purgeRemote(it, rule) }
            }
            Result(local, remote, pending)
        }

    /** Ausgewählte Einträge endgültig löschen (aus "Papierkorb & alte Versionen") */
    suspend fun deleteEntries(context: Context, profile: Profile, entries: List<TrashEntry>): Result =
        withContext(Dispatchers.IO) {
            val tree = localTree(context, profile)
            var local = 0
            for (e in entries.filter { it.side == Side.LOCAL }) {
                if (tree?.deleteTrashFile(e.trashPath) == true) local++
            }
            val pcEntries = entries.filter { it.side == Side.REMOTE }
            var remote = 0
            var pending = false
            if (pcEntries.isNotEmpty()) {
                val s = openPc(profile)
                if (s == null) { PurgeStore(context).addFiles(profile.id, pcEntries.map { it.trashPath }); pending = true }
                else s.use { pc -> for (e in pcEntries) if (deleteRemoteFile(pc, e.trashPath)) remote++ }
            }
            Result(local, remote, pending)
        }
}
