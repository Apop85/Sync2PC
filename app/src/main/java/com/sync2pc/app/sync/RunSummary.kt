package com.sync2pc.app.sync

/**
 * Kurztext eines Laufs für Verlauf und Benachrichtigung (0.3.4): nur was passiert ist.
 * Die Zahl der bereits gesicherten (unveränderten) Dateien interessiert hier nicht – sie steht im Detail-Log.
 */
object RunSummary {
    const val NOTHING = "Nichts zu tun"

    /** Fotos-Sync. [found] = von der Duplikatsuche auf dem PC gefundene, der App noch unbekannte Dateien */
    fun photos(uploaded: Int, found: Int, failed: Int, waiting: Int): String {
        val parts = buildList {
            if (uploaded > 0) add("$uploaded übertragen")
            if (found > 0) add("$found bereits vorhanden")
            if (failed > 0) add("$failed Fehler")
            if (waiting > 0) add("$waiting warten auf Ordnerauswahl")
        }
        return if (parts.isEmpty()) NOTHING else parts.joinToString(", ")
    }

    /** Ordner-Sync: nur Teile, die nicht 0 sind */
    fun mirror(up: Int, down: Int, trashed: Int, conflicts: Int, failed: Int, spaceShortage: Boolean): String {
        val parts = buildList {
            if (up > 0) add("↑ $up hochgeladen")
            if (down > 0) add("↓ $down heruntergeladen")
            if (trashed > 0) add("$trashed in Papierkorb")
            if (conflicts > 0) add("$conflicts Konflikt(e)")
            if (failed > 0) add("$failed Fehler")
            if (spaceShortage) add("Speicher voll")
        }
        return if (parts.isEmpty()) NOTHING else parts.joinToString(", ")
    }
}
