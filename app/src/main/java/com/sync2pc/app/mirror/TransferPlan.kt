package com.sync2pc.app.mirror

/** Regeln für die Übertragungen des Ordner-Syncs (0.3.0), ohne Android-Abhängigkeit. */
object TransferPlan {

    /** Kleine Dateien zuerst: schneller Fortschritt, bei einem Abbruch sind möglichst viele Dateien fertig */
    fun <T> smallFirst(items: List<T>, size: (T) -> Long): List<T> = items.sortedBy(size)

    /**
     * Ab welcher Stelle ein abgebrochener Download fortgesetzt werden darf (0 = neu beginnen).
     * Fortsetzen nur, wenn die angefangene Datei kleiner als das Original ist und die Datei auf dem PC
     * seit dem letzten Schreiben nicht geändert wurde. Unbekannte Zeitstempel -> sicherheitshalber neu.
     */
    fun resumeOffset(tmpSize: Long, tmpModified: Long, remoteSize: Long, remoteModified: Long): Long {
        if (tmpSize <= 0 || tmpSize >= remoteSize) return 0
        if (tmpModified <= 0 || remoteModified <= 0) return 0
        return if (remoteModified <= tmpModified) tmpSize else 0
    }
}
