package com.sync2pc.app.mirror

// White-/Blacklist mit einfachen Mustern (Gross-/Kleinschreibung egal):
//   *.pdf        alle PDFs (in jedem Ordner)
//   Thumbs.db    genau dieser Dateiname
//   Temp/**      alles im Ordner "Temp" (relativ zum Sync-Ordner)
//   ?            genau ein Zeichen
// Muster ohne "/" gelten für den Dateinamen, Muster mit "/" für den relativen Pfad.
// Whitelist leer = alles erlaubt. Blacklist hat Vorrang.
// (Bewusst Zeilenkommentare: "/**" in einem Blockkommentar öffnet in Kotlin einen verschachtelten Kommentar.)
class FileFilter(include: String, exclude: String) {
    private val inc = parse(include)
    private val exc = parse(exclude) + ALWAYS_EXCLUDED

    fun accepts(relPath: String): Boolean {
        val p = relPath.lowercase()
        val name = p.substringAfterLast('/')
        fun matches(rules: List<Pair<Boolean, Regex>>) =
            rules.any { (onPath, rx) -> rx.matches(if (onPath) p else name) }
        if (matches(exc)) return false
        return inc.isEmpty() || matches(inc)
    }

    val isEmpty: Boolean get() = inc.isEmpty() && exc.size == ALWAYS_EXCLUDED.size

    companion object {
        /** Temp-Dateien und Papierkorb werden nie synchronisiert */
        // inkl. Papierkorb-Name bis 0.0.11 (".Sync2PC-Papierkorb") und alte Version beim Ersetzen (".s2pold", 0.4.1)
        private val ALWAYS_EXCLUDED = parse("*.s2ptmp\n*.s2pold\n.LeoSync-Bin/**\n.LeoSync-Papierkorb/**\n.Sync2PC-Papierkorb/**")

        fun split(s: String): List<String> =
            s.split('\n', ',', ';').map { it.trim() }.filter { it.isNotEmpty() }

        private fun parse(s: String): List<Pair<Boolean, Regex>> =
            split(s).map { pat ->
                val p = pat.lowercase().replace('\\', '/').trimStart('/')
                (p.contains('/')) to globToRegex(p)
            }

        fun globToRegex(glob: String): Regex {
            val sb = StringBuilder("^")
            var i = 0
            while (i < glob.length) {
                val c = glob[i]
                when {
                    c == '*' && i + 1 < glob.length && glob[i + 1] == '*' -> { sb.append(".*"); i++ }
                    c == '*' -> sb.append("[^/]*")
                    c == '?' -> sb.append("[^/]")
                    else -> sb.append(Regex.escape(c.toString()))
                }
                i++
            }
            return Regex(sb.append("$").toString())
        }
    }
}
