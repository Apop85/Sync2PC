package com.sync2pc.app.sync

/** Regeln für Ordner- und Dateinamen auf dem Share. */
object FolderNames {
    /** Erste alleinstehende zweistellige Monatszahl 01–12 im Ordnernamen */
    private val monthRegex = Regex("""(?<!\d)(0[1-9]|1[0-2])(?!\d)""")
    private val invalidChars = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    /** Monat aus einem (evtl. vom Benutzer umbenannten) Ordnernamen, z. B. "06 - Ägypten Hurghada" -> 6 */
    fun monthOf(folderName: String): Int? =
        monthRegex.find(folderName)?.groupValues?.get(1)?.toInt()

    /** "06" bzw. "06 - Ägypten" */
    fun defaultName(month: Int, countryName: String?): String {
        val m = "%02d".format(month)
        return if (countryName.isNullOrBlank()) m else sanitize("$m - $countryName")
    }

    /** Ist das ein unveränderter, von der App erzeugter Name ("06" oder "06 - <Land>")? */
    fun isGeneratedName(folderName: String, countryNames: Set<String>): Boolean {
        if (folderName.length == 2 && monthOf(folderName) != null) return true
        val m = Regex("""^(0[1-9]|1[0-2]) - (.+)$""").find(folderName) ?: return false
        val country = m.groupValues[2]
        return countryNames.any { sanitize(it).equals(country, ignoreCase = true) }
    }

    /** Für Windows ungültige Zeichen ersetzen */
    fun sanitize(name: String): String =
        name.replace(invalidChars, "_").trim().trimEnd('.', ' ').ifEmpty { "_" }

    /** Konfliktname: n=2 -> "bild_2.jpg", n=3 -> "bild_3.jpg" */
    fun conflictName(fileName: String, n: Int): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot <= 0) "${fileName}_$n"
        else fileName.substring(0, dot) + "_" + n + fileName.substring(dot)
    }

    /**
     * Gleicher Dateiname, auch mit Konflikt-Zusatz: "bild.jpg" passt zu "bild.jpg", "BILD.JPG", "bild_2.jpg", "bild_17.jpg".
     */
    fun sameBaseName(existing: String, original: String): Boolean {
        if (existing.equals(original, ignoreCase = true)) return true
        val dot = original.lastIndexOf('.')
        val base = if (dot > 0) original.substring(0, dot) else original
        val ext = if (dot > 0) original.substring(dot) else ""
        val rx = Regex("^" + Regex.escape(base) + "_\\d+" + Regex.escape(ext) + "$", RegexOption.IGNORE_CASE)
        return rx.matches(existing)
    }

    /** Gruppenschlüssel für Ordner-Zuordnungen: "2026/06/EG" oder "2026/06/-" */
    fun groupKey(year: Int, month: Int, countryKey: String?): String =
        "%04d/%02d/%s".format(year, month, countryKey?.ifBlank { null } ?: "-")
}
