package com.sync2pc.app.data

/** Eintrag im Verlauf: ein einzelner Lauf oder mehrere aufeinanderfolgende "Nichts zu tun"-Läufe (0.3.4) */
sealed class LogItem {
    data class Single(val run: SyncRun) : LogItem()
    /** Neueste zuerst (wie im Verlauf) */
    data class Idle(val runs: List<SyncRun>) : LogItem()
}

object RunGrouping {
    /**
     * Merkmale im gespeicherten Kurztext (RunSummary), die einen Lauf "nicht ruhig" machen. Beide Sprachen, weil der
     * Text in der Sprache gespeichert wird, die beim Lauf aktiv war (0.8.0).
     */
    private val notIdleMarkers = listOf(
        "Papierkorb", "bereits vorhanden", "Konflikt", "Speicher",
        "trash", "already present", "conflict", "storage",
    )

    /** Lauf ohne Übertragung, ohne Fehler und ohne offene Punkte? */
    fun isIdle(r: SyncRun): Boolean =
        r.status == RunStatus.OK && r.uploaded == 0 && r.failed == 0 && r.waiting == 0 &&
            notIdleMarkers.none { r.message.contains(it, ignoreCase = true) }

    /** [runs] neueste zuerst. Aufeinanderfolgende ruhige Läufe (mind. 2) werden zu einem Eintrag. */
    fun group(runs: List<SyncRun>): List<LogItem> {
        val out = ArrayList<LogItem>()
        val idle = ArrayList<SyncRun>()
        fun flush() {
            when (idle.size) {
                0 -> {}
                1 -> out += LogItem.Single(idle[0])
                else -> out += LogItem.Idle(idle.toList())
            }
            idle.clear()
        }
        for (r in runs) {
            if (isIdle(r)) idle += r else { flush(); out += LogItem.Single(r) }
        }
        flush()
        return out
    }

    /** Lauf mit Fehler (fehlgeschlagen, teilweise oder einzelne Dateien mit Fehler) */
    fun hasError(r: SyncRun): Boolean =
        r.status == RunStatus.FAILED || r.status == RunStatus.PARTIAL || r.status == RunStatus.WAITING || r.failed > 0

    /** Lauf, der mindestens eine Datei übertragen hat (Ordner-Sync: hoch- oder heruntergeladen) */
    fun hasTransfer(r: SyncRun): Boolean = r.uploaded > 0

    /**
     * Filter im Verlauf (0.3.5): keiner aktiv = alles; "Fehler" und/oder "Übertragungen" = nur passende Läufe
     * (beide aktiv = Läufe mit Fehler ODER Übertragung). Zusammengefasste ruhige Läufe fallen dann weg.
     */
    fun filter(items: List<LogItem>, errors: Boolean, transfers: Boolean): List<LogItem> {
        if (!errors && !transfers) return items
        return items.filter { item ->
            item is LogItem.Single && ((errors && hasError(item.run)) || (transfers && hasTransfer(item.run)))
        }
    }
}
