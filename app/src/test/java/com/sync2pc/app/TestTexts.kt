package com.sync2pc.app

import com.sync2pc.app.l10n.Texts
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Texte für Unit-Tests (ohne Android): liest alle XML-Dateien in res/values-de und löst R-IDs per Reflection auf.
 * Wird von [Texts.get] automatisch geladen, wenn nichts initialisiert ist – bestehende Tests prüfen weiter Deutsch.
 */
object TestTexts {

    class Res(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>, val raw: Map<String, String>)

    private val resDir: File by lazy {
        listOf(File("src/main/res"), File("app/src/main/res")).firstOrNull { it.isDirectory }
            ?: error("res-Ordner nicht gefunden (Arbeitsverzeichnis ${File(".").absolutePath})")
    }

    fun load(folder: String): Res {
        val strings = LinkedHashMap<String, String>()
        val raw = LinkedHashMap<String, String>()
        val plurals = LinkedHashMap<String, Map<String, String>>()
        val dir = File(resDir, folder)
        dir.listFiles { f -> f.name.endsWith(".xml") }?.sortedBy { it.name }?.forEach { f ->
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f)
            val ss = doc.getElementsByTagName("string")
            for (i in 0 until ss.length) {
                val e = ss.item(i) as Element
                if (e.getAttribute("translatable") == "false") continue
                val name = e.getAttribute("name")
                raw[name] = e.textContent
                strings[name] = unescape(e.textContent)
            }
            val ps = doc.getElementsByTagName("plurals")
            for (i in 0 until ps.length) {
                val e = ps.item(i) as Element
                val items = LinkedHashMap<String, String>()
                val its = e.getElementsByTagName("item")
                for (j in 0 until its.length) {
                    val it = its.item(j) as Element
                    items[it.getAttribute("quantity")] = unescape(it.textContent)
                    raw[e.getAttribute("name") + "#" + it.getAttribute("quantity")] = it.textContent
                }
                plurals[e.getAttribute("name")] = items
            }
        }
        return Res(strings, plurals, raw)
    }

    /** Wie aapt2: ohne Anführungszeichen Leerraum zusammenfassen, dann \-Escapes auflösen */
    fun unescape(s: String): String {
        val t = if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) s.substring(1, s.length - 1)
        else s.replace(Regex("\\s+"), " ").trim()
        val sb = StringBuilder()
        var i = 0
        while (i < t.length) {
            val c = t[i]
            if (c == '\\' && i + 1 < t.length) {
                when (val n = t[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    else -> sb.append(n)
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

    private fun names(cls: String): Map<Int, String> = try {
        Class.forName("com.sync2pc.app.R\$$cls").fields.associate { it.getInt(null) to it.name }
    } catch (e: ClassNotFoundException) {
        emptyMap()
    }

    private val de by lazy { load("values-de") }
    private val stringNames by lazy { names("string") }
    private val pluralNames by lazy { names("plurals") }

    private object GermanResolver : Texts.Resolver {
        override fun string(id: Int, args: Array<out Any>): String {
            val name = stringNames[id] ?: error("Unbekannte String-ID $id")
            val v = de.strings[name] ?: error("Kein deutscher Text für $name")
            return if (args.isEmpty()) v else String.format(Locale.GERMAN, v, *args)
        }

        override fun plural(id: Int, count: Int, args: Array<out Any>): String {
            val name = pluralNames[id] ?: error("Unbekannte Plurals-ID $id")
            val items = de.plurals[name] ?: error("Keine deutsche Mehrzahl für $name")
            val v = (if (count == 1) items["one"] else items["other"]) ?: items["other"]!!
            return String.format(Locale.GERMAN, v, *args)
        }

        override val locale: Locale = Locale.GERMAN

        override fun entryName(id: Int): String? = stringNames[id] ?: pluralNames[id]

        override fun stringByName(name: String, args: Array<out Any>): String? {
            val v = de.strings[name] ?: return null
            return if (args.isEmpty()) v else String.format(Locale.GERMAN, v, *args)
        }

        override fun pluralByName(name: String, count: Int, args: Array<out Any>): String? {
            val items = de.plurals[name] ?: return null
            val v = (if (count == 1) items["one"] else items["other"]) ?: items["other"] ?: return null
            return String.format(Locale.GERMAN, v, *args)
        }
    }

    @JvmStatic
    fun install() {
        Texts.resolver = GermanResolver
    }
}
