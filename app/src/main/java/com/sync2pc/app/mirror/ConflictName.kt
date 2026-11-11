package com.sync2pc.app.mirror

/**
 * Namenswahl für «Beide behalten» (SYNC-20261111-171). Die Konfliktkopie darf nie eine vorhandene Datei überschreiben
 * (weder auf dem Handy noch auf dem PC): Es wird der erste freie Name gewählt.
 *  `name_conflict-phone.ext`, sonst `name_conflict-phone-2.ext`, `-3` …
 */
object ConflictName {
    /** Zusatz beim Behalten beider Versionen (bis 0.7.x "_Konflikt-Handy") */
    const val SUFFIX = "_conflict-phone"

    /** Höchstens so viele Kandidaten probieren, danach gibt es keinen Namen (Aufrufer bricht ohne Änderung ab) */
    const val MAX_TRIES = 100

    /**
     * Erster freier Name für [fileName] (nur der Name ohne Ordner).
     * @param taken liefert true, wenn der Name (Gross-/Kleinschreibung beliebig) schon vergeben ist
     * @return null, wenn die ersten [MAX_TRIES] Namen alle vergeben sind
     */
    fun free(fileName: String, taken: (String) -> Boolean): String? {
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        for (n in 1..MAX_TRIES) {
            val candidate = if (n == 1) base + SUFFIX + ext else "$base$SUFFIX-$n$ext"
            if (!taken(candidate)) return candidate
        }
        return null
    }

    /** Hat der Speicher-Anbieter das Dokument genau so benannt wie geplant? (sonst hängt er z. B. « (1)» an) */
    fun sameName(planned: String, actual: String?): Boolean = actual != null && planned.equals(actual, ignoreCase = true)
}
