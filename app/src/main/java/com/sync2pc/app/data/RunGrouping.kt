package com.sync2pc.app.data

/** Eintrag im Verlauf: ein einzelner Lauf oder mehrere aufeinanderfolgende "Nichts zu tun"-Läufe (0.3.4) */
sealed class LogItem {
    data class Single(val run: SyncRun) : LogItem()
    /** Neueste zuerst (wie im Verlauf) */
    data class Idle(val runs: List<SyncRun>) : LogItem()
}

object RunGrouping {
    /** Lauf ohne Übertragung, ohne Fehler und ohne offene Punkte? */
    fun isIdle(r: SyncRun): Boolean =
        r.status == "OK" && r.uploaded == 0 && r.failed == 0 && r.waiting == 0 &&
            listOf("Papierkorb", "bereits vorhanden", "Konflikt", "Speicher").none { r.message.contains(it) }

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

    /** Filter "Nur Übertragungen und Probleme": ruhige Läufe ausblenden */
    fun important(items: List<LogItem>): List<LogItem> =
        items.filter { it is LogItem.Single && !isIdle(it.run) }
}
