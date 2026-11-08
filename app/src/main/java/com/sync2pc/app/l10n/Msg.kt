package com.sync2pc.app.l10n

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import org.json.JSONArray
import org.json.JSONObject

/**
 * Meldung mit Kennung statt fertigem Text (0.12.0, DEC-159): Ressourcen-**Name** (nie die R-ID, die sich mit jedem
 * Build ändert), Argumente, optional Mehrzahl-Zahl oder Verkettung. Wird als kleiner JSON-Baum gespeichert
 * (`sync_runs.messageCode/triggerCode`, `log_entries.code`) und beim Anzeigen in der aktiven Sprache gerendert.
 * Der fertige Text in der Schreibsprache bleibt als Rückfall in der DB (alte Einträge, unbekannter Schlüssel,
 * Parse- oder Formatfehler → [render] liefert den Rückfalltext).
 *
 * JSON: `{"t":"Text"}` wörtlich · `{"k":"name","a":[…]}` Text · `{"k":"name","n":3,"a":[…]}` Mehrzahl ·
 * `{"j":", ","p":[…]}` Verkettung. Argumente: Text, Zahl oder verschachtelte Meldung.
 * Regel für spätere Änderungen: Felder optional, Leser ignorieren unbekannte Felder.
 */
sealed class Msg {
    /** Wörtlicher Text (Dateinamen, Fehlertexte, Systemtext); nicht übersetzbar */
    class Lit(val text: String) : Msg()

    /** [id] ist nur beim Schreiben bekannt (schneller und sicher); aus der DB gelesen gilt der [name] */
    class Res(val name: String, val args: List<Any>, val id: Int = 0) : Msg()

    class Plural(val name: String, val count: Int, val args: List<Any>, val id: Int = 0) : Msg()

    class Join(val sep: String, val parts: List<Msg>) : Msg()

    /** Text in der aktiven Sprache; null, wenn ein Schlüssel fehlt oder das Format nicht passt */
    fun render(): String? = try { renderInner() } catch (e: Exception) { null }

    /** Wie [render], aber nie null (beim Schreiben aus Ressourcen-IDs kann das nicht scheitern) */
    fun text(): String = render() ?: ""

    /** JSON der Kennung; null bei wörtlichem Text (dort genügt der gespeicherte Text) */
    fun toCode(): String? = if (this is Lit) null else try { toObj().toString() } catch (e: Exception) { null }

    /** Alle Ressourcen-Namen im Baum (für [Msg.keys]) */
    fun keys(): Set<String> = buildSet { collectKeys(this) }

    private fun collectKeys(out: MutableSet<String>) {
        when (this) {
            is Lit -> {}
            is Res -> { out += name; args.forEach { (it as? Msg)?.collectKeys(out) } }
            is Plural -> { out += name; args.forEach { (it as? Msg)?.collectKeys(out) } }
            is Join -> parts.forEach { it.collectKeys(out) }
        }
    }

    private fun renderInner(): String {
        val t = Texts.get()
        return when (this) {
            is Lit -> text
            is Res -> {
                val a = renderArgs(args).toTypedArray()
                if (id != 0) t.string(id, a) else t.stringByName(name, a) ?: throw NoSuchElementException(name)
            }
            is Plural -> {
                val a = (if (args.isEmpty()) listOf<Any>(count) else renderArgs(args)).toTypedArray()
                if (id != 0) t.plural(id, count, a) else t.pluralByName(name, count, a) ?: throw NoSuchElementException(name)
            }
            is Join -> parts.joinToString(sep) { it.renderInner() }
        }
    }

    private fun renderArgs(list: List<Any>): List<Any> = list.map { if (it is Msg) it.renderInner() else it }

    private fun toObj(): JSONObject = when (this) {
        is Lit -> JSONObject().put("t", text)
        is Res -> JSONObject().put("k", name).also { if (args.isNotEmpty()) it.put("a", argsJson(args)) }
        is Plural -> JSONObject().put("k", name).put("n", count).also { if (args.isNotEmpty()) it.put("a", argsJson(args)) }
        is Join -> JSONObject().put("j", sep).put("p", JSONArray().also { a -> parts.forEach { p -> a.put(p.toObj()) } })
    }

    private fun argsJson(list: List<Any>): JSONArray = JSONArray().also { a ->
        for (v in list) a.put(if (v is Msg) v.toObj() else v)
    }

    companion object {
        private const val MAX_DEPTH = 12

        /** Text aus res/values*.xml, Platzhalter wie bei [str]; Argumente: Text, Zahl oder [Msg] */
        fun of(@StringRes id: Int, vararg args: Any): Msg = Res(entry(id), norm(args), id)

        /** Mehrzahl; ohne weitere Argumente wird die Anzahl als %1$s eingesetzt (wie bei [plural]) */
        fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): Msg = Plural(entry(id), count, norm(args), id)

        fun lit(text: String): Msg = Lit(text)

        /** Verkettung; ein einzelner Teil bleibt ohne Hülle */
        fun join(sep: String, parts: List<Msg>): Msg = parts.singleOrNull() ?: Join(sep, parts)

        private fun entry(id: Int): String = Texts.get().entryName(id) ?: ""

        private fun norm(args: Array<out Any>): List<Any> = args.map { if (it is Msg || it is Number) it else it.toString() }

        /** Kennung lesen; null bei leer, kaputt oder unbekannter Form */
        fun fromJson(json: String?): Msg? = try {
            if (json.isNullOrBlank()) null else fromObj(JSONObject(json), 0)
        } catch (e: Exception) { null }

        /** Anzeigetext: Kennung in der aktiven Sprache, sonst der gespeicherte Text */
        fun render(code: String?, fallback: String): String = fromJson(code)?.render() ?: fallback

        /** Ressourcen-Namen einer gespeicherten Kennung; null, wenn keine (lesbare) Kennung vorliegt */
        fun keys(code: String?): Set<String>? = fromJson(code)?.keys()

        private fun fromObj(o: JSONObject, depth: Int): Msg {
            if (depth > MAX_DEPTH) throw IllegalArgumentException("zu tief")
            return when {
                o.has("t") -> Lit(o.getString("t"))
                o.has("j") -> {
                    val a = o.getJSONArray("p")
                    Join(o.getString("j"), List(a.length()) { fromObj(a.getJSONObject(it), depth + 1) })
                }
                o.has("n") -> Plural(o.getString("k"), o.getInt("n"), argsOf(o, depth))
                o.has("k") -> Res(o.getString("k"), argsOf(o, depth))
                else -> throw IllegalArgumentException("unbekannte Form")
            }
        }

        private fun argsOf(o: JSONObject, depth: Int): List<Any> {
            val a = o.optJSONArray("a") ?: return emptyList()
            return List(a.length()) {
                when (val v = a.get(it)) {
                    is JSONObject -> fromObj(v, depth + 1)
                    is Number -> v
                    else -> v.toString()
                }
            }
        }
    }
}
