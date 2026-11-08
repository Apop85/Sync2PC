package com.sync2pc.app.diag

/**
 * Maskiert und pseudonymisiert Diagnosedaten (0.12.4): keine Benutzernamen, IP-Adressen, Rechner-/WLAN-/Profilnamen,
 * Pfade oder genauen Dateinamen im Export. Reine Kotlin-Klasse (ohne Android), damit sie unit-testbar ist.
 *
 * Zwei Stufen:
 *  1. Bekannte Werte ([register]): Namen aus Profilen/Einstellungen werden durch stabile Platzhalter ersetzt
 *     ("Profil-1", "Host-1", "WLAN-1" …). Derselbe Wert ergibt im selben Export immer denselben Platzhalter,
 *     damit Zusammenhänge erkennbar bleiben. Die Zuordnung steht nur im Speicher und wird nie exportiert.
 *  2. Muster ([scrub]): Freitext (Log, Fehlertexte, Stacktraces) wird nach IP-/MAC-Adressen, E-Mail, URLs, Pfaden
 *     und Dateinamen durchsucht. Dateiendungen bleiben erhalten (Fehlersuche), Ordner/Namen nicht.
 *
 * Grundsatz: lieber zu viel als zu wenig maskieren.
 */
class Redactor {
    private val pseudonyms = LinkedHashMap<String, String>()   // "label|wert" -> Platzhalter
    private val counters = HashMap<String, Int>()
    private class Known(val original: String, val regex: Regex, val placeholder: String)
    private val known = ArrayList<Known>()                     // bekannte Werte, längste zuerst

    /** Platzhalter für [value] (stabil je Export). Leere Werte bleiben leer. */
    fun pseudo(label: String, value: String): String {
        val v = value.trim()
        if (v.isEmpty()) return ""
        return pseudonyms.getOrPut("$label|${v.lowercase()}") {
            val n = (counters[label] ?: 0) + 1
            counters[label] = n
            "$label-$n"
        }
    }

    /**
     * Merkt sich [value] als sensibel: jedes Vorkommen in [scrub] wird durch den Platzhalter ersetzt.
     * Zu kurze Werte (< 3 Zeichen) werden nicht als Freitext-Treffer gesucht (zu viele Fehltreffer), aber über [pseudo] bleibt der Platzhalter nutzbar.
     * @return der Platzhalter
     */
    fun register(label: String, value: String): String {
        val p = pseudo(label, value)
        val v = value.trim()
        if (v.length >= 3 && known.none { it.original.equals(v, ignoreCase = true) }) {
            known += Known(v, Regex(Regex.escape(v), RegexOption.IGNORE_CASE), p)
            known.sortByDescending { it.original.length } // längste zuerst ("Foto Backup" vor "Foto")
        }
        return p
    }

    /** Wie [register] für jedes Element einer Liste (z. B. WLAN-Namen, Ordner) */
    fun registerAll(label: String, values: Collection<String>) = values.forEach { register(label, it) }

    /**
     * Ordnerpfad "a/b\c" -> Platzhalter je Segment ("Ordner-1/Ordner-2"). Erhalten bleiben reine Zahlensegmente
     * (Jahr/Monat) und die Standardordner von Android (DCIM, Camera …); sie sind für die Fehlersuche nötig.
     */
    fun folderPath(path: String, separator: String = "/"): String =
        path.split('/', '\\').filter { it.isNotBlank() }.joinToString(separator) { segment(it) }

    /** Wie [register] für die einzelnen Segmente eines Ordnerpfads (Standardordner und Zahlen ausgenommen) */
    fun registerFolders(path: String) {
        path.split('/', '\\').map { it.trim().trimEnd(':') }.filter { it.length >= 3 && !isKeepSegment(it) }
            .forEach { register("Ordner", it) }
    }

    /** Entfernt alles Sensible aus Freitext. */
    fun scrub(text: String): String {
        if (text.isEmpty()) return text
        var s = text
        // 1. Bekannte Werte (Profilnamen, Host, Freigabe, Benutzer, WLAN …)
        for (k in known) s = k.regex.replace(s, Regex.escapeReplacement(k.placeholder))
        // 2. Adressen und Kennungen
        val protectedUrls = ArrayList<String>() // öffentliche Update-Quelle vor den Pfad-Regeln schützen und am Ende zurücksetzen
        s = URL.replace(s) { m ->
            if (isPublicUrl(m.value)) { protectedUrls += m.value; "\u0001${protectedUrls.size - 1}\u0001" }
            else m.groupValues[1] + "://<URL>"
        }
        s = EMAIL.replace(s, "<E-Mail>")
        s = MAC.replace(s, "<MAC>")
        s = IPV4.replace(s) { m -> if (m.groupValues.drop(1).all { it.toInt() <= 255 }) "<IP>" else m.value }
        s = IPV6.replace(s) { m ->
            val v = m.value
            if (v.contains("::") || v.count { it == ':' } >= 7) "<IP>" else v
        }
        s = COORDS.replace(s, "<Koordinaten>")
        s = UNKNOWN_HOST.replace(s) { m -> m.groupValues[1] + "<Host>" }
        s = HOSTNAME.replace(s, "<Host>")
        // 3. Pfade und Dateinamen
        s = UNC.replace(s) { "\\\\<Pfad>" }
        s = WIN_ABS.replace(s, "<Pfad>")
        s = POSIX_ABS.replace(s) { m ->
            // Speicherpfade (Fotos/Ordner) behalten Standardordner und Zahlen, alles andere (App-Verzeichnisse) wird entfernt
            if (STORAGE_ROOT.containsMatchIn(m.value)) "/" + folderPath(m.value) else "<Pfad>"
        }
        // Zielordner der Fotos "2026\10 - Ägypten": Jahr/Monat bleiben, der Zusatz (Land/Ort) nicht
        s = DATE_FOLDER.replace(s) { m -> m.groupValues[1] + " - <Name>" }
        s = REL_PATH.replace(s) { m ->
            val v = m.value
            // "und/oder", "WLAN/Mobil": ein einzelner "/" ohne Dateiendung ist meist Fliesstext, kein Pfad
            if (v.count { it == '/' } == 1 && !v.contains('\\') && !FILE_EXT.containsMatchIn(v)) v else relPath(v)
        }
        s = FILE_NAME.replace(s) { m -> "<Datei>." + m.groupValues[1].lowercase() }
        if (protectedUrls.isNotEmpty()) s = RESTORE.replace(s) { m -> protectedUrls[m.groupValues[1].toInt()] }
        return s
    }

    // ---- intern -------------------------------------------------------------------
    private fun isKeepSegment(t: String): Boolean =
        t.all { it.isDigit() } || t.startsWith("<") || t.lowercase() in STANDARD_FOLDERS || PLACEHOLDER.matches(t)

    private fun segment(seg: String): String {
        val t = seg.trim()
        if (isKeepSegment(t)) return t                            // 2019, 04, DCIM, bereits maskiertes
        val m = MONTH_NAME.matchEntire(t)                         // "10 - Ägypten" -> "10 - <Name>"
        if (m != null) return m.groupValues[1] + " - <Name>"
        val ext = FILE_EXT.find(t)?.groupValues?.get(1)
        return if (ext != null) "<Datei>.${ext.lowercase()}" else pseudo("Ordner", t)
    }

    /** Relativer Pfad im Freitext: Segmente maskieren, Pfadtrenner erhalten */
    private fun relPath(token: String): String {
        val sep = if (token.contains('\\')) "\\" else "/"
        return token.split('/', '\\').joinToString(sep) { segment(it) }
    }

    private fun isPublicUrl(url: String): Boolean {
        val host = url.substringAfter("://").substringBefore('/').substringBefore(':').lowercase()
        return host in PUBLIC_HOSTS
    }

    companion object {
        /** Öffentliche Update-Quelle: enthält nichts Persönliches und hilft bei Update-Fehlern */
        private val PUBLIC_HOSTS = setOf("api.github.com", "github.com", "objects.githubusercontent.com", "raw.githubusercontent.com")

        private const val EXT = "jpe?g|png|gif|webp|heic|heif|bmp|tiff?|dng|cr2|nef|arw|raw|mp4|mov|avi|mkv|3gp|webm|m4v|mp3|m4a|wav|aac|ogg|flac|opus|" +
            "pdf|docx?|xlsx?|pptx?|odt|ods|txt|csv|zip|rar|7z|apk|sqlite|db|bak|tmp|lnk|vcf|ics|rtf|epub|svg"

        /** Ordnernamen, die jedes Android-Handy hat: kein Personenbezug, aber wichtig für die Fehlersuche */
        private val STANDARD_FOLDERS = setOf(
            "dcim", "camera", "pictures", "screenshots", "download", "downloads", "movies", "documents", "music",
            "android", "recordings", "primary", "storage", "emulated", "sdcard",
        )
        private val STORAGE_ROOT = Regex("""^/(?:storage|sdcard|mnt)(?:/|$)""")
        private val PLACEHOLDER =Regex("""(?:Ordner|Profil|Host|Freigabe|WLAN|Benutzer)-\d+""")

        private val FILE_EXT =Regex("""\.($EXT)$""", RegexOption.IGNORE_CASE)
        private val MONTH_NAME = Regex("""(\d{1,2})\s*-\s*.+""")
        private val DATE_FOLDER = Regex("""\b(\d{4}[\\/]\d{1,2}) - [^\r\n,;()"“→↑↓<]+""")
        private val RESTORE = Regex("\u0001(\\d+)\u0001")
        private val URL = Regex("""\b([a-zA-Z][a-zA-Z0-9+.-]*)://[^\s"'<>)]+""")
        private val EMAIL = Regex("""[\w.+-]+@[\w-]+(?:\.[\w-]+)+""")
        private val MAC = Regex("""\b(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\b""")
        private val IPV4 = Regex("""\b(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\b""")
        private val IPV6 = Regex("""(?<![\w:])(?:[0-9A-Fa-f]{0,4}:){2,7}[0-9A-Fa-f]{0,4}(?![\w:])""")
        private val COORDS = Regex("""-?\d{1,3}\.\d{4,}\s*[,;]\s*-?\d{1,3}\.\d{4,}""")
        private val UNKNOWN_HOST = Regex("""(?i)(UnknownHostException:\s*|resolve host\s*"?)(?:[A-Za-z0-9._-]+)""")
        private val HOSTNAME = Regex("""(?i)\b[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:local|lan|home|internal|intranet|localdomain|fritz\.box)(?!\.?\w)""")
        private val UNC = Regex("""\\\\[^\s\\"]+(?:\\[^\s"]*)*""")
        private val WIN_ABS = Regex("""\b[A-Za-z]:\\[^\s"]*""")
        private val POSIX_ABS = Regex("""(?<![\w:/.])/(?:[\w.@~=+-]+/)+[\w.@~=+-]*""")
        private val REL_PATH = Regex("""(?<![\w<])[^\s"„“'\\/:*?<>|]+(?:[\\/][^\s"„“'\\/:*?<>|]+)+""")
        private val FILE_NAME = Regex(
            """(?:[^\s\\/:*?"„“<>|↑↓✗⤼⏸=→,;()]+ ){0,4}[^\s\\/:*?"„“<>|↑↓✗⤼⏸=→,;()]+\.($EXT)(?![\w])""",
            RegexOption.IGNORE_CASE,
        )
    }
}
