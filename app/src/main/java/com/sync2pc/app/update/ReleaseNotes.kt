package com.sync2pc.app.update

/**
 * Release-Text der Update-Seite ohne Markdown-Zeichen. Reine Links (z. B. «**Full Changelog**: https://…») werden
 * aus dem Text genommen; der erste Link wird als antippbarer Knopf angeboten. Nur Anzeige.
 */
object ReleaseNotes {
    data class Cleaned(val text: String, val link: String?)

    private val URL = Regex("""https?://[^\s)>\]]+""")
    private val MD_LINK = Regex("""\[([^\]]+)]\((https?://[^)\s]+)\)""")

    fun clean(raw: String, maxChars: Int = 3000): Cleaned {
        if (raw.isBlank()) return Cleaned("", null)
        var link: String? = null
        val lines = raw.take(maxChars).lines().mapNotNull { l0 ->
            var l = l0
            // [Text](url) -> Text, der erste Link wird gemerkt
            MD_LINK.find(l)?.let { if (link == null) link = it.groupValues[2] }
            l = MD_LINK.replace(l) { it.groupValues[1] }
            val m = URL.find(l)
            if (m != null) {
                if (link == null) link = m.value
                l = URL.replace(l, "")
            }
            l = l.replace("**", "").replace("__", "").replace("`", "")
                .replace(Regex("""^\s{0,3}#{1,6}\s*"""), "")
                .replace(Regex("""^\s*[*-]\s+"""), "• ")
                .trimEnd()
            // Zeile, die nur noch eine Beschriftung für den entfernten Link war («Full Changelog:») -> weglassen
            if (m != null && (l.isBlank() || l.trim().endsWith(":"))) null else l
        }
        val text = lines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
        return Cleaned(text, link)
    }
}