package com.sync2pc.app.mirror

/**
 * Hat sich der Handy-Ordner seit dem Einlesen verändert (SYNC-20261111-213)? Eine Datei, die nach dem Einlesen entsteht oder
 * verschwindet, steht nicht in der Liste des Laufs; sie wird nie «zurückgestellt», also auch nicht von [com.sync2pc.app.sync.SkipFollowUp]
 * erfasst. Billiger Indikator ohne zweites Auflisten: der Änderungsstempel jedes Ordners (ändert sich beim Anlegen, Löschen und
 * Umbenennen von Einträgen im Ordner). Reine Entscheidung, damit sie ohne Android testbar ist.
 */
object FolderChange {
    /** Höchstens so viele Ordner werden am Laufende nachgeprüft (Kosten: eine Abfrage je Ordner) */
    const val MAX_CHECKS = 300

    /**
     * [before] = Ordner-ID -> Stempel beim Einlesen (0 = Anbieter liefert keinen), [now] liefert den aktuellen Stempel
     * (null = Ordner nicht mehr lesbar bzw. verschwunden). [ownWrites] = der Lauf hat selbst auf dem Handy geschrieben
     * (Download, Papierkorb, Umbenennen, Aufräumen): seine eigenen Änderungen dürfen keinen Folgelauf auslösen, dann keine Aussage.
     */
    fun changed(before: Map<String, Long>, ownWrites: Boolean, now: (String) -> Long?): Boolean {
        if (ownWrites || before.isEmpty()) return false
        var checks = 0
        for ((id, stamp) in before) {
            if (checks++ >= MAX_CHECKS) break
            val current = now(id) ?: return true // verschwunden = verändert
            if (stamp != 0L && current != stamp) return true
        }
        return false
    }
}
