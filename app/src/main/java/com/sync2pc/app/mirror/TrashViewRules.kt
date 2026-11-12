package com.sync2pc.app.mirror

/**
 * Reine Logik der Papierkorb-Ansicht (SYNC-20261112-277, Entwurf `.claude/ux/papierkorb-filter.md`): Seitenfilter «Anzeigen»,
 * Aufheben der Auswahl bei Filterwechsel, Zähler je Seite und Angaben im Wiederherstellen-Dialog. Getestet in TrashViewRulesTest.
 * Der Seitenfilter nutzt [PurgeSides] (Handy und PC / Nur Handy / Nur PC), dieselbe Auswahl wie «Wo?» beim Aufräumen.
 */
object TrashViewRules {
    /** Sichtbare Einträge: Seitenfilter [sides] und Tag [day] (null = alle Tage) */
    fun visible(entries: List<TrashEntry>, sides: PurgeSides, day: String?): List<TrashEntry> =
        entries.filter { (if (it.side == Side.LOCAL) sides.local else sides.remote) && (day == null || it.date == day) }

    /** Auswahl nach einem Filterwechsel: [selection] bleibt nur, wenn **alle** Einträge sichtbar sind; sonst leer und [cleared] */
    data class Pruned(val selection: List<TrashEntry>, val cleared: Boolean)

    fun pruneSelection(selection: List<TrashEntry>, visible: List<TrashEntry>): Pruned {
        if (selection.isEmpty()) return Pruned(selection, false)
        val keys = visible.mapTo(HashSet()) { it.side to it.trashPath }
        return if (selection.all { (it.side to it.trashPath) in keys }) Pruned(selection, false) else Pruned(emptyList(), true)
    }

    /** Anzahl Einträge je Seite */
    data class SideCounts(val phone: Int, val pc: Int)

    fun sideCounts(entries: List<TrashEntry>): SideCounts =
        SideCounts(entries.count { it.side == Side.LOCAL }, entries.count { it.side == Side.REMOTE })

    /** «Handy 2, PC 1»; Seiten ohne Einträge entfallen. [phoneLabel]/[pcLabel] sind die Namen der Seiten in der App-Sprache */
    fun sidesText(c: SideCounts, phoneLabel: String, pcLabel: String): String =
        listOfNotNull(
            if (c.phone > 0) "$phoneLabel ${c.phone}" else null,
            if (c.pc > 0) "$pcLabel ${c.pc}" else null,
        ).joinToString(", ")

    /** Was die Liste statt der Einträge zeigt, wenn der Filter nichts übrig lässt */
    enum class Empty { NONE, PHONE, PC, PC_UNREACHABLE }

    /**
     * Leerzustand durch den Filter ([visible] leer, aber der Papierkorb nicht): «Nur PC» bei nicht erreichbarem PC erklärt den
     * Grund und behauptet nie, der PC-Papierkorb sei leer.
     */
    fun emptyKind(visible: List<TrashEntry>, sides: PurgeSides, pcAvailable: Boolean): Empty = when {
        visible.isNotEmpty() -> Empty.NONE
        sides == PurgeSides.REMOTE && !pcAvailable -> Empty.PC_UNREACHABLE
        sides == PurgeSides.REMOTE -> Empty.PC
        sides == PurgeSides.LOCAL -> Empty.PHONE
        else -> Empty.NONE
    }

    /**
     * Angaben für den Wiederherstellen-Dialog (Variante 4A). Wiederherstellen schreibt immer auf Handy **und** PC, deshalb gelten
     * [files] für beide Seiten. [replaces]: Dateien, die laut Sync-Stand ([existing], Kleinbuchstaben) aktuell vorhanden sind und
     * ersetzt werden (die aktuelle Fassung kommt vorher in den Papierkorb); [missing]: neu angelegt. Der Sync-Stand kennt nur
     * «vorhanden», nicht die Seite, deshalb gelten die Zähler für Handy und PC gleich. [names]: Dateinamen bei höchstens drei Dateien.
     */
    data class RestorePlan(val files: Int, val replaces: Int, val missing: Int, val names: List<String>)

    fun restorePlan(selection: List<TrashEntry>, existing: Set<String>): RestorePlan {
        val files = selection.distinctBy { it.origPath.lowercase() }
        val replaces = files.count { it.origPath.lowercase() in existing }
        return RestorePlan(
            files = files.size, replaces = replaces, missing = files.size - replaces,
            names = if (files.size <= 3) files.map { it.origPath } else emptyList(),
        )
    }
}
