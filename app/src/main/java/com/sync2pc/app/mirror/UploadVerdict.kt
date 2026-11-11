package com.sync2pc.app.mirror

import java.io.IOException

/** Ergebnis der Prüfung am Ende eines Uploads (vor dem Umbenennen der .s2ptmp) */
enum class UploadVerdict {
    /** Alles gelesen, Quelle unverändert: Temp-Datei darf zur fertigen Datei werden */
    COMMIT,

    /** Quelldatei hat sich während der Übertragung geändert: Temp-Datei verwerfen, Datei zurückstellen («folgt später») */
    DEFER,

    /** Gelesene Bytes passen nicht zur Grösse, die Quelle sieht aber unverändert aus: echter Fehler */
    INCOMPLETE;

    companion object {
        /**
         * SYNC-20261111-202: Wächst oder ändert sich eine Handy-Datei während ihrer Übertragung, ist das kein Fehler, sondern
         * ein Fall für den Folgelauf (wie [TransferStep.SKIP_CHANGED]). Eine halbe oder gemischte Version wird nie zur fertigen
         * Datei. Eine Änderung hat Vorrang vor der Bytezahl; ohne Prüfmöglichkeit ([sourceChanged] = false) gilt wie bisher
         * nur die Bytezahl.
         * @param size erwartete Grösse (beim Einlesen)
         * @param bytesRead tatsächlich gelesene/geschriebene Bytes (inkl. Fortsetzungs-Offset)
         * @param sourceChanged Grösse oder Änderungsdatum der Quelle weichen jetzt vom Einlesen ab (oder sie ist verschwunden)
         */
        fun of(size: Long, bytesRead: Long, sourceChanged: Boolean): UploadVerdict = when {
            sourceChanged -> DEFER
            bytesRead != size -> INCOMPLETE
            else -> COMMIT
        }

        /** Obergrenze der zu lesenden Bytes: eine mehr als erwartet genügt, um das Wachsen zu erkennen (kein unbegrenztes Lesen) */
        fun readLimit(size: Long): Long = size + 1

        /** Wie viele Bytes dürfen noch gelesen werden, wenn [bytesSoFar] schon gelesen sind und der Puffer [room] Platz hat? */
        fun room(size: Long, bytesSoFar: Long, room: Int): Int =
            minOf(room.toLong(), (readLimit(size) - bytesSoFar).coerceAtLeast(0)).toInt()
    }
}

/** Die Quelldatei hat sich während des Uploads geändert; die Temp-Datei ist verworfen, nichts wurde als fertig gespeichert */
class SourceChangedException : IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_changed))
