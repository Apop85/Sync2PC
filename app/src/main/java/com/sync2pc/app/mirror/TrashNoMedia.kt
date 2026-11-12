package com.sync2pc.app.mirror

/**
 * Regeln für die `.nomedia`-Marker-Datei im Handy-Papierkorb (SYNC-20261112-249): Sie versteckt `@LeoSync-Bin` samt Unterordnern
 * vor der Galerie (MediaProvider). Rein, getestet in TrashNoMediaTest; das Anlegen selbst steht in [LocalTree.ensureTrashNoMedia].
 */
object TrashNoMedia {
    /**
     * Soll der Papierkorb eines Handy-Ordners mit der Wurzel-Dokument-ID [rootDocId] eine `.nomedia` bekommen? Nicht bei Ordnern
     * des Download-Anbieters mit Nummern-Kennung («msd:», «msf:»): Sie fielen sonst aus dem MediaStore, über den dieser Anbieter
     * auflistet (Papierkorb nicht auflistbar, Zersplitterung «(1)», DEC-254).
     */
    fun applies(rootDocId: String): Boolean = !rootDocId.startsWith("msd:") && !rootDocId.startsWith("msf:")

    /** Ist [name] die Marker-Datei? (Gross-/Kleinschreibung egal) */
    fun isMarker(name: String): Boolean = name.equals(MirrorEngine.NOMEDIA, ignoreCase = true)
}
