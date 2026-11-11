package com.sync2pc.app.sync

import android.content.Context
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.media.MediaScanner
import android.net.Uri
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncDirection
import com.sync2pc.app.mirror.FileFilter
import com.sync2pc.app.mirror.LocalTree
import com.sync2pc.app.mirror.MirrorEngine

/**
 * Rein lokale Prüfung ohne Netzwerk: Warten bei einem Bildordner-Profil Fotos/Videos auf die Übertragung?
 * Gleicher Abgleich wie in SyncEngine (Filter, Mindestalter, bekannte Dateien nach Grösse + Änderungsdatum).
 */
object PendingCheck {
    /** Anzahl übertragungsbereiter Dateien (zu junge zählen nicht – für sie plant SyncEngine einen Nachhol-Sync) */
    suspend fun readyFiles(context: Context, p: Profile, minAgeMinutes: Int): Int {
        val dao = AppDatabase.get(context).dao()
        val filter = FileFilter(p.includePatterns, p.excludePatterns)
        val known = dao.syncedLite(p.id).associateBy { it.sourceKey }
        val readyBefore = System.currentTimeMillis() / 1000 - minAgeMinutes * 60L
        return MediaScanner.scan(context, p.folders(), p.includeSubfolders, p.includeImages, p.includeVideos)
            .count { m ->
                filter.accepts(m.relativePath + m.displayName) &&
                    maxOf(m.dateAddedSec, m.dateModifiedSec) < readyBefore &&
                    known[m.sourceKey].let { k -> k == null || k.size != m.size || k.dateModified != m.dateModifiedSec }
            }
    }

    /**
     * Ordner-Profil (SYNC-20261111-199), rein lokal ohne Netzwerk: Dateien im Handy-Ordner, die neu oder seit dem letzten Abgleich
     * geändert sind (gleicher Vergleich wie MirrorPlanner: Grösse + Änderungszeit gegen `mirror_state`). Nur Richtungen, die vom
     * Handy hochladen; bei «PC → Handy» ist offen nicht ohne PC-Liste bekannt (0). Nicht lesbar -> 0.
     */
    suspend fun mirrorLocalOpen(context: Context, p: Profile): Int {
        if (p.dir() == SyncDirection.DOWNLOAD || p.localTreeUri.isBlank()) return 0
        val uri = Uri.parse(p.localTreeUri)
        if (!LocalTree.hasPermission(context, uri)) return 0
        val states = AppDatabase.get(context).dao().mirrorStates(p.id).associateBy { it.relPath }
        val filter = FileFilter(p.includePatterns, p.excludePatterns)
        return LocalTree(context, uri).listRecursive({ MirrorEngine.isTrashDir(it) }, recover = false)
            .filterKeys { filter.accepts(it) }
            .count { (rel, e) ->
                states[rel.lowercase()].let { st -> st == null || e.size != st.localSize || e.modified != st.localModified }
            }
    }

    /** Wartende Dateien eines Profils je Typ (Fehler, z. B. fehlender Zugriff: 0) */
    suspend fun openFiles(context: Context, p: Profile): Int = try {
        if (p.type() == ProfileType.MIRROR) mirrorLocalOpen(context, p) else readyFiles(context, p, 0)
    } catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e }
    catch (e: Exception) { 0 }
}