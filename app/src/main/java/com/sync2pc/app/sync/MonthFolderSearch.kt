package com.sync2pc.app.sync

import com.sync2pc.app.data.FolderLayout

/**
 * Vorlagenunabhängige Suche "alle Ordner des Monats M im Jahr" (SYNC-20261109-084, DEC-022/DEC-136).
 * Gilt für alle Ordnervorlagen inkl. V1. Sie liefert nur Ordner, in denen eine Datei schon liegen könnte; ob eine
 * Datei wirklich ein Duplikat ist, entscheiden Name, Grösse und Prüfsumme beim Aufrufer.
 */
object MonthFolderSearch {

    /**
     * Alle Ordner des Monats [month] unterhalb des Jahresordners als relative Pfade (`\`):
     *  (a) Ordner im Jahr mit der Monatszahl,
     *  (b) deren Unterordner (V3: Länderordner im Monatsordner),
     *  (c) Ordner mit der Monatszahl in Unterordnern ohne Monatszahl (V2: Monatsordner im Länderordner).
     * [listDirs] liefert die Unterordner-Namen eines relativen Pfads ("" = Jahresordner); der Aufrufer cached.
     */
    fun related(month: Int, listDirs: (String) -> List<String>): List<String> {
        val out = LinkedHashMap<String, String>()
        fun add(p: String) { out.putIfAbsent(p.lowercase(), p) }
        for (top in listDirs("")) {
            when (FolderNames.monthOf(top)) {
                month -> {
                    add(top)
                    for (sub in listDirs(top)) add("$top\\$sub")
                }
                null -> for (sub in listDirs(top)) if (FolderNames.monthOf(sub) == month) add("$top\\$sub")
                else -> {}
            }
        }
        return out.values.toList()
    }

    /** Lage einer Datei auf dem PC: Jahresordner (voller Pfad) und die Ordner darunter bis zur Datei */
    data class Location(val yearPath: String, val dirs: List<String>) {
        /** Relativer Pfad des Ordners unterhalb des Jahres */
        val relDir: String get() = dirs.joinToString("\\")
    }

    private val yearRegex = Regex("""^\d{4}$""")

    /**
     * Zerlegt einen gespeicherten Dateipfad ("Basis\2026\Ägypten\06\bild.jpg"): Jahr = letztes vierstelliges Segment,
     * darunter 1 oder 2 Ordner. Unbekannte Form -> null (dann wird nicht gesucht, die Datei bleibt "nicht gefunden").
     */
    fun locate(remotePath: String): Location? {
        val segs = remotePath.split('\\')
        if (segs.size < 3) return null
        for (i in segs.size - 2 downTo 0) {
            if (!yearRegex.matches(segs[i])) continue
            val dirs = segs.subList(i + 1, segs.size - 1)
            if (dirs.size !in 1..2) return null
            return Location(segs.subList(0, i + 1).joinToString("\\"), dirs)
        }
        return null
    }

    /** Monat aus den Ordnern einer [Location] gemäss Vorlage (V2: letzter Ordner, sonst erster), sonst erster Ordner mit Monatszahl */
    fun monthOfLocation(loc: Location, layout: FolderLayout): Int? {
        val preferred = if (layout == FolderLayout.COUNTRY_FOLDER) loc.dirs.last() else loc.dirs.first()
        return FolderNames.monthOf(preferred) ?: loc.dirs.firstNotNullOfOrNull { FolderNames.monthOf(it) }
    }
}
