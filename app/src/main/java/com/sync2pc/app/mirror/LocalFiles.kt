package com.sync2pc.app.mirror

/**
 * Schmaler Zugriff auf den Handy-Ordner mit genau den Operationen, die Wiederherstellen und Zurückrollen brauchen
 * ([RestoreExecutor], SYNC-20261111-163 Stufe 2). [LocalTree] implementiert dies mit seinen vorhandenen Methoden;
 * in Tests ersetzt es ein Fake. Pfade relativ zum Handy-Ordner, Trenner "/".
 */
interface LocalFiles {
    /** Datei unter einem relativen Pfad suchen (Gross-/Kleinschreibung egal), null = nicht vorhanden */
    fun find(relPath: String): LocalEntry?

    /** Eintrag zu einer Dokument-ID, null = nicht vorhanden */
    fun stat(docId: String): LocalEntry?

    /** Datei von [fromRel] nach [toRel] verschieben (und umbenennen); liefert die Dokument-ID am neuen Ort */
    fun moveFile(fromRel: String, entry: LocalEntry, toRel: String): String

    /** Datei in den Papierkorb ("<Ordner>/@LeoSync-Bin/<Datum>/<relPath>") verschieben; liefert den Pfad im Papierkorb */
    fun moveToTrash(relPath: String, entry: LocalEntry, dateFolder: String): String
}
