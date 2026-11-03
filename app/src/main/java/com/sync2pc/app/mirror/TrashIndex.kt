package com.sync2pc.app.mirror

/**
 * Eine Datei im Papierkorb eines Ordner-Profils (0.5.0).
 * [trashPath]: Pfad relativ zum Profilordner, z. B. ".LeoSync-Papierkorb/2026-11-03/Docs/a_2.txt".
 * [origPath]: ursprünglicher Pfad, z. B. "Docs/a.txt". [date]: Datumsordner "yyyy-MM-dd".
 */
data class TrashEntry(
    val side: Side,
    val trashPath: String,
    val origPath: String,
    val date: String,
    val size: Long,
    val modified: Long,
) {
    val name: String get() = origPath.substringAfterLast('/')
    val folder: String get() = origPath.substringBeforeLast('/', "")
}

/** Papierkorb-Einträge zuordnen und auswählen (rein, getestet in TrashIndexTest) */
object TrashIndex {
    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    private val NUMBERED = Regex("""^(.+)_(\d+)(\.[^.]*)?$""")
    /** Seit 0.7.0: "Foto (14-32-05).jpg", bei gleicher Sekunde "Foto (14-32-05-2).jpg" */
    private val VERSIONED = Regex("""^(.+) \(\d{2}-\d{2}-\d{2}(?:-\d+)?\)(\.[^.]*)?$""")

    /** Name einer weiteren Version im Papierkorb: Uhrzeit [time] ("HH-mm-ss") vor der Endung, [n] >= 2 bei gleicher Sekunde */
    fun versionName(fileName: String, time: String, n: Int = 1): String {
        val tag = if (n <= 1) time else "$time-$n"
        val dot = fileName.lastIndexOf('.')
        return if (dot <= 0) "$fileName ($tag)" else fileName.substring(0, dot) + " ($tag)" + fileName.substring(dot)
    }

    /** Aktuelle Uhrzeit für [versionName] */
    fun timeNow(now: Long = System.currentTimeMillis()): String =
        java.text.SimpleDateFormat("HH-mm-ss", java.util.Locale.GERMANY).format(java.util.Date(now))

    /** Freier Name im Papierkorb-Ordner: Originalname, sonst mit Uhrzeit. [taken] prüft (ohne Gross-/Kleinschreibung) */
    fun uniqueTrashName(fileName: String, time: String, taken: (String) -> Boolean): String {
        if (!taken(fileName)) return fileName
        var n = 1
        while (true) {
            val c = versionName(fileName, time, n++)
            if (!taken(c)) return c
        }
    }

    /**
     * Ursprünglicher Name einer Datei im Papierkorb.
     * Seit 0.7.0 heissen weitere Versionen am selben Tag "a (14-32-05).txt" – eindeutig, wird immer entfernt.
     * Bis 0.6.x hiessen sie "a_2.txt" (FolderNames.conflictName). "_N" wird nur entfernt, wenn es den Grundnamen
     * im selben Papierkorb-Ordner auch gibt – sonst hiess die Datei wirklich so. [siblings] in Kleinbuchstaben.
     */
    fun originalName(name: String, siblings: Set<String>): String {
        VERSIONED.matchEntire(name)?.let { v -> return v.groupValues[1] + v.groupValues[2] }
        val m = NUMBERED.matchEntire(name) ?: return name
        val n = m.groupValues[2].toIntOrNull() ?: return name
        if (n < 2) return name
        val base = m.groupValues[1] + m.groupValues[3]
        return if (base.lowercase() in siblings) base else name
    }

    /**
     * Pfad im Papierkorb -> Eintrag. [trashPath] beginnt mit dem Papierkorb-Ordner, danach folgt der Datumsordner.
     * null = kein gültiger Eintrag (z. B. Datei direkt im Papierkorb ohne Datumsordner).
     */
    fun entry(side: Side, trashPath: String, size: Long, modified: Long, siblings: Set<String>): TrashEntry? {
        val parts = trashPath.split('/')
        if (parts.size < 3) return null
        val date = parts[1]
        if (!DATE.matches(date)) return null
        val dirs = parts.subList(2, parts.size - 1)
        val name = originalName(parts.last(), siblings)
        val orig = (dirs + name).joinToString("/")
        return TrashEntry(side, trashPath, orig, date, size, modified)
    }

    /** Neueste Version je Datei (neuester Tag, dann neueste Änderungszeit) */
    fun latestPerFile(entries: List<TrashEntry>): List<TrashEntry> =
        entries.groupBy { it.origPath.lowercase() }.values
            .map { list -> list.maxWith(compareBy<TrashEntry>({ it.date }, { it.modified })) }
            .sortedBy { it.origPath.lowercase() }

    /** "Alles vom Datum X": je Datei die neueste Version dieses Tages */
    fun ofDate(entries: List<TrashEntry>, date: String): List<TrashEntry> =
        latestPerFile(entries.filter { it.date == date })

    /** Ganzer Ordner (inkl. Unterordner): je Datei die neueste Version */
    fun inFolder(entries: List<TrashEntry>, folder: String): List<TrashEntry> {
        val f = folder.trim('/').lowercase()
        return latestPerFile(entries.filter { f.isEmpty() || it.origPath.lowercase().startsWith("$f/") })
    }

    /** Pro Datei höchstens eine Version auswählen: die zuletzt gewählte ersetzt eine frühere derselben Datei */
    fun select(current: List<TrashEntry>, add: List<TrashEntry>): List<TrashEntry> {
        val byFile = LinkedHashMap<String, TrashEntry>()
        for (e in current + add) byFile[e.origPath.lowercase()] = e
        return byFile.values.toList()
    }

    /** Datumsordner, neueste zuerst */
    fun dates(entries: List<TrashEntry>): List<String> = entries.map { it.date }.distinct().sortedDescending()
}
