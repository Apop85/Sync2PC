package com.sync2pc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Jeder Text muss in Englisch (values) und Deutsch (values-de) vorhanden sein, mit denselben Platzhaltern (0.8.0). */
class StringsParityTest {

    private val en = TestTexts.load("values")
    private val de = TestTexts.load("values-de")

    // app_name steht nur in values (gleich in allen Sprachen)
    private val shared = setOf("app_name")

    @Test
    fun sameKeys() {
        val enKeys = en.strings.keys - shared
        val deKeys = de.strings.keys - shared
        assertEquals("Nur Englisch: ${enKeys - deKeys}", emptySet<String>(), enKeys - deKeys)
        assertEquals("Nur Deutsch: ${deKeys - enKeys}", emptySet<String>(), deKeys - enKeys)
        assertEquals("Mehrzahl nur Englisch", emptySet<String>(), en.plurals.keys - de.plurals.keys)
        assertEquals("Mehrzahl nur Deutsch", emptySet<String>(), de.plurals.keys - en.plurals.keys)
    }

    @Test
    fun pluralsHaveOneAndOther() {
        for ((name, items) in en.plurals + de.plurals.mapKeys { it.key + " (de)" }) {
            assertTrue("$name: one fehlt", "one" in items)
            assertTrue("$name: other fehlt", "other" in items)
        }
    }

    @Test
    fun samePlaceholders() {
        for ((name, v) in en.strings) {
            val d = de.strings[name] ?: continue
            assertEquals("Platzhalter in $name", placeholders(v), placeholders(d))
        }
        for ((name, items) in en.plurals) {
            val d = de.plurals[name] ?: continue
            assertEquals("Platzhalter in $name", placeholders(items["other"]!!), placeholders(d["other"]!!))
        }
    }

    /** Texte mit Platzhaltern müssen sich formatieren lassen (z. B. "20 %" statt "20 %%" bricht ab). */
    @Test
    fun formatsWork() {
        val all = en.strings.map { "values/" + it.key to it.value } + de.strings.map { "values-de/" + it.key to it.value } +
            (en.plurals.flatMap { p -> p.value.map { "values/" + p.key + "#" + it.key to it.value } }) +
            (de.plurals.flatMap { p -> p.value.map { "values-de/" + p.key + "#" + it.key to it.value } })
        for ((name, v) in all) {
            val isPlural = name.contains('#')
            val ph = placeholders(v)
            if (ph.isEmpty() && !isPlural) {
                assertTrue("$name: \"%%\" ohne Platzhalter wird nicht ersetzt", !v.contains("%%"))
                continue
            }
            val max = ph.maxOfOrNull { it.substringAfter('%').substringBefore('$').toInt() } ?: 0
            val args = Array<Any>(maxOf(max, 1)) { i ->
                if (ph.any { it.startsWith("%${i + 1}$") && it.endsWith("d") }) 1 else "x"
            }
            try {
                String.format(Locale.ROOT, v, *args)
            } catch (e: Exception) {
                throw AssertionError("$name lässt sich nicht formatieren: $v (${e.message})")
            }
        }
    }

    /** aapt2 verlangt \' für Apostrophe */
    @Test
    fun apostrophesEscaped() {
        for ((name, v) in en.raw + de.raw) {
            val bad = Regex("(?<!\\\\)'").containsMatchIn(v) && !(v.startsWith("\"") && v.endsWith("\""))
            assertTrue("$name: Apostroph ohne \\", !bad)
        }
    }

    private fun placeholders(s: String): Set<String> =
        Regex("%(\\d+)\\$([sd])").findAll(s).map { it.value }.toSet()
}
