package com.sync2pc.app.ui


/**
 * Reine Hilfslogik für Vorschau, Ordnerauswahl und Konflikte (SYNC-20261110-135/-136), ohne Android-Abhängigkeit,
 * damit sie ohne Gerät testbar ist.
 */

/** V4: Gruppenbezeichnung der Vorschau für noch offene Ordnerwahl ("2026\? (12)" bzw. "2026\? (Spanien\12 - )") */
object PreviewGroupLabel {
    /** @property prefix Ordner vor dem Monatsordner (z. B. Länderordner), leer wenn keiner */
    data class OpenGroup(val year: String, val prefix: String, val month: Int?, val rawName: String)

    private val OPEN = Regex("^(\\d{4})\\\\\\? \\((.*)\\)$")
    private val MONTH_START = Regex("^(\\d{1,2})(?!\\d)")

    /** null = keine Gruppe mit offener Wahl (Bezeichnung dann unverändert anzeigen) */
    fun parse(folder: String): OpenGroup? {
        val m = OPEN.matchEntire(folder) ?: return null
        val name = m.groupValues[2]
        val prefix = name.substringBeforeLast('\\', "")
        val last = name.substringAfterLast('\\')
        val month = MONTH_START.find(last)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..12 }
        return OpenGroup(m.groupValues[1], prefix, month, name)
    }

    /** Anzeigename "<Jahr> – [<Prefix>\]<Monat>"; ohne erkennbaren Monat der rohe Vorschlagsname */
    fun title(g: OpenGroup, monthNames: List<String>): String {
        val monthText = g.month?.let { monthNames.getOrNull(it - 1) } ?: g.rawName.substringAfterLast('\\').trim()
        val pre = if (g.prefix.isEmpty()) "" else g.prefix + "\\"
        return "${g.year} – $pre$monthText"
    }
}

/** O1: Vorauswahl und "Alle übernehmen" in der Ordnerauswahl */
object DecisionChoice {
    /**
     * Wirksame Auswahl einer Gruppe: eine getroffene Wahl, sonst bei genau einem vorhandenen Ordner dieser
     * (bzw. der zum Land passende), ohne vorhandene Ordner der Vorschlag. Bei mehreren vorhandenen Ordnern
     * gibt es keine Vorauswahl (null): der Benutzer muss bewusst wählen.
     */
    fun effective(candidates: List<String>, defaultName: String, countryName: String?, chosen: String?): String? {
        if (chosen != null) return chosen
        if (candidates.size > 1) return null
        return countryName?.let { n -> candidates.firstOrNull { it.contains(n, ignoreCase = true) } }
            ?: candidates.firstOrNull() ?: defaultName
    }
}

/** K1: Zeitformat mit Sekunden und Rückgängig-Prüfung */
object ConflictUi {
    /** Fügt Sekunden hinzu ("dd.MM.yyyy HH:mm" -> "dd.MM.yyyy HH:mm:ss"); Muster mit Sekunden bleiben unverändert */
    fun patternWithSeconds(pattern: String): String =
        if (pattern.contains("ss")) pattern else pattern.replace("HH:mm", "HH:mm:ss")

    // Rückgängig: atomar über SyncDao.undoResolution (SYNC-20261111-172), keine Prüfung mehr in der Oberfläche
}
