package com.sync2pc.app.smb

/** Grösse nach dem Download passt nicht (Datei geändert oder kein Fortschritt): Temp-Datei verwerfen, Einzelfehler der Datei */
class DownloadSizeException(message: String) : java.io.IOException(message)

/**
 * Bewertung des Endes eines Downloads (SYNC-20261111-232 Punkt 2). Reine Logik, getestet in DownloadEndTest.
 *
 * Endet das Lesen vor der erwarteten Grösse, gibt es zwei Ursachen: die Verbindung wurde unterbrochen (der Server
 * meldet «Dateiende», obwohl die Datei grösser ist) oder die Datei auf dem PC ist inzwischen kleiner geworden.
 * Eine Unterbrechung gilt nur, wenn in diesem Aufruf Bytes gelesen wurden: sonst würde eine dauerhaft zu kurz lesbare
 * Datei (z. B. Cloud-Platzhalter) jeden Lauf als «unterbrochen» beenden (Review SYNC-20261111-232).
 */
object DownloadEnd {
    enum class Kind {
        /** Alle erwarteten Bytes gelesen */
        COMPLETE,
        /** Zu früh beendet nach Fortschritt, die Datei ist auf dem PC (laut aktueller Abfrage) nicht kleiner: Verbindung unterbrochen, Temp-Datei behalten, später fortsetzen */
        INTERRUPTED,
        /** Grösse stimmt nicht (Datei geändert, mehr Bytes als erwartet oder kein Fortschritt): Einzelfehler, Temp-Datei verwerfen, nächster Lauf beginnt neu */
        CHANGED,
    }

    /**
     * [read] = gelesene Bytes inkl. Startposition, [expected] = Grösse beim Einlesen, [current] = jetzt auf dem PC gemeldete
     * Grösse (null = nicht abfragbar, z. B. weil die Verbindung weg ist), [progressed] = in diesem Aufruf wurden Bytes gelesen.
     */
    fun classify(read: Long, expected: Long, current: Long?, progressed: Boolean): Kind = when {
        read == expected -> Kind.COMPLETE
        read > expected -> Kind.CHANGED
        current != null && current < expected -> Kind.CHANGED
        !progressed -> Kind.CHANGED
        else -> Kind.INTERRUPTED
    }
}
