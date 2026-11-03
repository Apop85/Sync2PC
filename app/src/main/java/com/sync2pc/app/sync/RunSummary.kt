package com.sync2pc.app.sync

import com.sync2pc.app.R
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str

/**
 * Kurztext eines Laufs für Verlauf und Benachrichtigung (0.3.4): nur was passiert ist.
 * Die Zahl der bereits gesicherten (unveränderten) Dateien interessiert hier nicht – sie steht im Detail-Log.
 * Seit 0.8.0 in der App-Sprache (gespeichert wird der fertige Text, siehe RunGrouping.isIdle).
 */
object RunSummary {
    val NOTHING: String get() = str(R.string.run_nothing)

    /** Fotos-Sync. [found] = von der Duplikatsuche auf dem PC gefundene, der App noch unbekannte Dateien */
    fun photos(uploaded: Int, found: Int, failed: Int, waiting: Int): String {
        val parts = buildList {
            if (uploaded > 0) add(str(R.string.run_uploaded, uploaded))
            if (found > 0) add(str(R.string.run_found, found))
            if (failed > 0) add(plural(R.plurals.run_failed, failed))
            if (waiting > 0) add(plural(R.plurals.run_waiting, waiting))
        }
        return if (parts.isEmpty()) NOTHING else parts.joinToString(", ")
    }

    /** Ordner-Sync: nur Teile, die nicht 0 sind */
    fun mirror(up: Int, down: Int, trashed: Int, conflicts: Int, failed: Int, spaceShortage: Boolean): String {
        val parts = buildList {
            if (up > 0) add(str(R.string.run_up, up))
            if (down > 0) add(str(R.string.run_down, down))
            if (trashed > 0) add(str(R.string.run_trashed, trashed))
            if (conflicts > 0) add(plural(R.plurals.run_conflicts, conflicts))
            if (failed > 0) add(plural(R.plurals.run_failed, failed))
            if (spaceShortage) add(str(R.string.run_space_full))
        }
        return if (parts.isEmpty()) NOTHING else parts.joinToString(", ")
    }
}
