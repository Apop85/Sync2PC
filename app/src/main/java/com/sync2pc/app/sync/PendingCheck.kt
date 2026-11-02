package com.sync2pc.app.sync

import android.content.Context
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.media.MediaScanner
import com.sync2pc.app.mirror.FileFilter

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
}
