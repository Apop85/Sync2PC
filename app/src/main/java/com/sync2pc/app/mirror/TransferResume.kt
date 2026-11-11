package com.sync2pc.app.mirror

/** Nächster Schritt einer Übertragung (Upload/Download) im Ordner-Sync */
enum class TransferStep {
    /** Ziel hat sich seit dem Einlesen geändert (oder ist neu entstanden): nicht überschreiben, der nächste Lauf entscheidet */
    SKIP_CHANGED,

    /** Sync-Stand vergessen, die bisherige Version am Ziel in den Papierkorb, dann übertragen */
    TRASH_THEN_TRANSFER,

    /** Direkt übertragen (bzw. an der Abbruchstelle fortsetzen) */
    TRANSFER,
}

/**
 * Entscheidung vor einer Übertragung, auch für die Wiederholung nach Verbindungsabbruch (SYNC-20261111-166).
 *
 * Fehler davor: Der erste Versuch verschob die alte Version am Ziel in den Papierkorb und brach dann ab. Beim zweiten
 * Versuch fehlte das Ziel, die Prüfung «unverändert» schlug an, und die Übertragung wurde ohne Fehler übersprungen
 * (Sync-Stand schon vergessen, Status «alles aktuell»). Hat der Lauf das Ziel selbst schon in den Papierkorb verschoben
 * ([alreadyTrashed]), ist sein Fehlen also erwartet: kein erneutes Verschieben, Übertragung fortsetzen. Ist am Ziel dagegen
 * inzwischen wieder etwas aufgetaucht, wird nie überschrieben.
 */
object TransferResume {
    /**
     * @param trashOther Versionsschutz: überschriebene Version in den Papierkorb ([MirrorPlanner.Decision.trashOther])
     * @param hadTarget beim Einlesen gab es am Ziel eine Datei
     * @param alreadyTrashed dieser Lauf hat die Datei am Ziel schon in den Papierkorb verschoben (Wiederholung)
     * @param targetUnchanged Ziel jetzt noch wie beim Einlesen (nur bei [alreadyTrashed] = false von Bedeutung)
     * @param targetAbsent am Ziel liegt jetzt keine Datei (nur bei [alreadyTrashed] = true von Bedeutung)
     */
    fun step(trashOther: Boolean, hadTarget: Boolean, alreadyTrashed: Boolean, targetUnchanged: Boolean, targetAbsent: Boolean): TransferStep =
        when {
            alreadyTrashed -> if (targetAbsent) TransferStep.TRANSFER else TransferStep.SKIP_CHANGED
            !targetUnchanged -> TransferStep.SKIP_CHANGED
            trashOther && hadTarget -> TransferStep.TRASH_THEN_TRANSFER
            else -> TransferStep.TRANSFER
        }
}