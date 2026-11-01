package com.sync2pc.app.sync

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.DeleteMode
import com.sync2pc.app.data.DeleteState
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.SyncedFile

/**
 * Löschen/Papierkorb nach verifiziertem Upload.
 * Eine Datei wird erst freigegeben, wenn ALLE aktiven Profile, die ihren Ordner sichern,
 * sie verifiziert übertragen haben.
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

    /** Prüft Dateien mit Status WANTED. Liefert Anzahl neu zur Bestätigung freigegebener Dateien. */
    suspend fun sweep(context: Context, log: suspend (String) -> Unit): Int {
        val dao = AppDatabase.get(context).dao()
        // Nur Bildordner-Profile sichern über den MediaStore (Ordner-Profile löschen nie nach dem Sync)
        val profiles = dao.profiles().filter { it.enabled && it.type() == com.sync2pc.app.data.ProfileType.PHOTOS }
        val wanted = dao.byDeleteState(DeleteState.WANTED).groupBy { it.sourceKey }
        var queued = 0
        for ((key, rows) in wanted) {
            val all = dao.syncedAllProfiles(key)
            val syncedBy = all.filter { it.verified }.map { it.profileId }.toSet()
            val covering = profiles.filter { covers(it, key) }
            if (!covering.all { it.id in syncedBy }) continue
            // Papierkorb gewinnt, wenn irgendein Profil nur Papierkorb wünscht
            val modes = covering.map { it.delete() }.filter { it != DeleteMode.KEEP }
            if (modes.isEmpty()) continue
            val trash = modes.contains(DeleteMode.TRASH)
            val row = rows.first()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // Android 10: direkt löschen (Legacy-Storage)
                if (stillSame(context, row) == false) { dao.setDeleteStateForKey(key, DeleteState.DONE); continue }
                try {
                    context.contentResolver.delete(Uri.parse(row.mediaUri), null, null)
                    dao.setDeleteStateForKey(key, DeleteState.DONE)
                    log("Gelöscht: $key")
                } catch (e: Exception) {
                    log("Löschen fehlgeschlagen: $key – ${e.message}")
                }
            } else {
                dao.setDeleteStateForKey(key, if (trash) DeleteState.QUEUED_TRASH else DeleteState.QUEUED_DELETE)
                queued++
            }
        }
        return queued
    }

    /** null = nicht feststellbar, false = Datei fehlt oder wurde verändert */
    private fun stillSame(context: Context, f: SyncedFile): Boolean? = try {
        context.contentResolver.query(
            Uri.parse(f.mediaUri), arrayOf(MediaStore.MediaColumns.SIZE), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getLong(0) == f.size else false }
    } catch (e: Exception) { null }

    data class Batch(val trash: Boolean, val keys: List<String>, val intent: PendingIntent)

    /** Android 11+: Erzeugt Systemdialog-Anfragen (max. 500 Dateien je Anfrage). */
    suspend fun buildRequests(context: Context): List<Batch> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val dao = AppDatabase.get(context).dao()
        val rows = dao.queuedDeletes().distinctBy { it.sourceKey }
        val valid = ArrayList<SyncedFile>()
        for (r in rows) {
            if (stillSame(context, r) == false) dao.setDeleteStateForKey(r.sourceKey, DeleteState.DONE)
            else valid += r
        }
        val result = ArrayList<Batch>()
        for ((trash, list) in valid.groupBy { it.deleteState == DeleteState.QUEUED_TRASH }) {
            for (chunk in list.chunked(500)) {
                val uris = chunk.map { Uri.parse(it.mediaUri) }
                val pi = if (trash) MediaStore.createTrashRequest(context.contentResolver, uris, true)
                else MediaStore.createDeleteRequest(context.contentResolver, uris)
                result += Batch(trash, chunk.map { it.sourceKey }, pi)
            }
        }
        return result
    }

    suspend fun markDone(context: Context, keys: List<String>) {
        val dao = AppDatabase.get(context).dao()
        for (k in keys) dao.setDeleteStateForKey(k, DeleteState.DONE)
    }
}
