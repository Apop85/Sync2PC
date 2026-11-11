package com.sync2pc.app

import com.sync2pc.app.data.LogEntry
import com.sync2pc.app.data.RunGrouping
import com.sync2pc.app.data.SyncDirection
import com.sync2pc.app.data.SyncRun
import com.sync2pc.app.data.messageText
import com.sync2pc.app.data.text
import com.sync2pc.app.data.triggerText
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.sync.RunSummary
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verlauf mit Kennungen (0.12.0, DEC-159): Kennung → Text, Rückfall, `isIdle` über Schlüssel */
class MsgTest {
    private fun roundTrip(m: Msg): String? = Msg.fromJson(m.toCode())?.render()

    @Test fun codeRendersLikeTheOriginalText() {
        val m = RunSummary.mirror(2, 1, 3, 2, 1, true)
        assertEquals(m.text(), roundTrip(m))
        assertEquals(RunSummary.photos(3, 12, 2, 4).text(), roundTrip(RunSummary.photos(3, 12, 2, 4)))
        assertEquals(RunSummary.NOTHING.text(), roundTrip(RunSummary.NOTHING))
    }

    @Test fun codeStoresResourceNamesNotIds() {
        val code = JSONObject(RunSummary.mirror(2, 0, 0, 0, 0, false).toCode()!!)
        // Einzelner Teil, keine Verkettung: Name + Zahl
        assertEquals("run_up", code.getString("k"))
        assertEquals(2, code.getJSONArray("a").getInt(0))
        assertFalse(code.has("id"))
    }

    @Test fun pluralAndNestedMessagesSurviveStorage() {
        val one = Msg.plural(R.plurals.run_failed, 1)
        val many = Msg.plural(R.plurals.run_failed, 2)
        assertEquals(one.text(), roundTrip(one))
        assertEquals(many.text(), roundTrip(many))
        assertTrue(one.text() != many.text())
        val nested = Msg.of(R.string.eng_interrupted_photos, Msg.of(R.string.stop_wifi), 7)
        assertTrue(nested.text().contains(Msg.of(R.string.stop_wifi).text()))
        assertEquals(nested.text(), roundTrip(nested))
        val joined = Msg.join("\n", listOf(Msg.of(R.string.run_nothing), Msg.of(R.string.run_space_full)))
        assertEquals(joined.text(), roundTrip(joined))
    }

    @Test fun literalTextHasNoCodeAndIsKeptVerbatim() {
        val lit = Msg.lit("SmbException: STATUS_X")
        assertNull(lit.toCode())
        assertEquals("SmbException: STATUS_X", lit.text())
        // Wörtlich als Argument (Dateiname, Fehlertext) bleibt erhalten
        val m = Msg.of(R.string.eng_offline_preview, "x\"y\\z ä")
        assertEquals(m.text(), roundTrip(m))
    }

    @Test fun fallbackTextIsShownWhenNoUsableCode() {
        assertEquals("alt", Msg.render(null, "alt"))
        assertEquals("alt", Msg.render("", "alt"))
        assertEquals("alt", Msg.render("{kaputt", "alt"))
        assertEquals("alt", Msg.render("[1,2]", "alt"))
        assertEquals("alt", Msg.render("""{"zzz":1}""", "alt"))
        // Schlüssel, den es nicht (mehr) gibt
        assertEquals("alt", Msg.render("""{"k":"gibt_es_nicht"}""", "alt"))
        // Mehrzahl-Schlüssel, der nur als Text existiert
        assertEquals("alt", Msg.render("""{"k":"run_up","n":2}""", "alt"))
    }

    @Test fun unknownFieldsAreIgnoredAndDepthIsLimited() {
        assertEquals(RunSummary.NOTHING.text(), Msg.render("""{"k":"run_nothing","neu":42}""", "alt"))
        var deep = """{"k":"run_nothing"}"""
        repeat(30) { deep = """{"k":"run_uploaded","a":[$deep]}""" }
        assertNull(Msg.fromJson(deep))
        assertEquals("alt", Msg.render(deep, "alt"))
    }

    @Test fun storedCodeUsesTheActiveLanguageNotTheWritingLanguage() {
        val code = RunSummary.mirror(1, 0, 0, 0, 0, false).toCode()
        // Text beim Schreiben (angenommen Englisch) ist nur Rückfall; angezeigt wird die Kennung (hier Deutsch)
        val run = SyncRun(profileId = 1, profileName = "P", trigger = "Manual", triggerCode = Msg.of(R.string.trigger_manual).toCode(),
            dryRun = false, startedAt = 1, message = "↑ 1 uploaded", messageCode = code)
        assertEquals("↑ 1 hochgeladen", run.messageText())
        assertEquals(Msg.of(R.string.trigger_manual).text(), run.triggerText())
        // Alter Eintrag ohne Kennung: gespeicherter Text
        assertEquals("↑ 1 uploaded", run.copy(messageCode = null).messageText())
        assertEquals("Manual", run.copy(triggerCode = null).triggerText())
    }

    private fun run(msg: String, code: String?, status: String = "OK") =
        SyncRun(profileId = 1, profileName = "P", trigger = "t", dryRun = false, startedAt = 1, finishedAt = 2,
            status = status, message = msg, messageCode = code)

    @Test fun isIdleUsesKeysWhenCodePresent() {
        // Text in beliebiger Sprache: nur der Schlüssel zählt (Risiko D35)
        assertFalse(RunGrouping.isIdle(run("5 already present", RunSummary.photos(0, 5, 0, 0).toCode())))
        assertFalse(RunGrouping.isIdle(run("x", RunSummary.mirror(0, 0, 2, 0, 0, false).toCode())))
        assertFalse(RunGrouping.isIdle(run("x", RunSummary.mirror(0, 0, 0, 1, 0, false).toCode())))
        assertFalse(RunGrouping.isIdle(run("x", RunSummary.mirror(0, 0, 0, 0, 0, true).toCode())))
        assertFalse(RunGrouping.isIdle(run("x", RunSummary.mirror(0, 0, 3, 0, 0, false).toCode())))
        // Merkmal-Wort im Text, aber Kennung sagt „nichts zu tun“ -> ruhig
        assertTrue(RunGrouping.isIdle(run("Konflikt Speicher Papierkorb", RunSummary.NOTHING.toCode())))
    }

    @Test fun isIdleFallsBackToWordsWithoutCode() {
        assertFalse(RunGrouping.isIdle(run("2 in Papierkorb", null)))
        assertFalse(RunGrouping.isIdle(run("3 already present", null)))
        assertTrue(RunGrouping.isIdle(run(RunSummary.NOTHING.text(), null)))
        // Kaputte Kennung zählt wie keine
        assertFalse(RunGrouping.isIdle(run("2 in Papierkorb", "{kaputt")))
    }

    @Test fun logLinesRenderFromCodeOrFallBackToStoredText() {
        val already = Msg.plural(R.plurals.eng_already_in, 3, 3, "2026\\06")
        val e = LogEntry(runId = 1, time = 0, level = "I", message = "3 already in 2026\\06", code = already.toCode())
        assertEquals(already.text(), e.text())
        assertEquals("3 already in 2026\\06", e.copy(code = null).text())
        assertEquals("3 already in 2026\\06", e.copy(code = "{kaputt").text())
        // Auslöser und Richtung stecken als Kennung in der Startzeile (nicht als Text der Schreibsprache)
        val start = Msg.of(R.string.eng_start_mirror, Msg.of(R.string.trigger_manual), "Profil", SyncDirection.BOTH.labelMsg)
        val line = LogEntry(runId = 1, time = 0, level = "I", message = "Start", code = start.toCode())
        assertEquals(start.text(), line.text())
        assertTrue(line.text().contains(Msg.of(R.string.trigger_manual).text()))
        assertTrue(line.text().contains(SyncDirection.BOTH.label))
        // Wörtliche Zeilen (Dateinamen, Symbole) haben keine Kennung
        assertNull(Msg.lit("↑ a.jpg → 2026\\06").toCode())
    }

    @Test fun notIdleKeysAreRealResources() {
        val t = Texts.get()
        for (k in RunSummary.NOT_IDLE_KEYS) {
            val s = try { t.stringByName(k, arrayOf(1)) } catch (e: Exception) { null }
            val p = try { t.pluralByName(k, 1, arrayOf(1)) } catch (e: Exception) { null }
            assertNotNull("Schlüssel $k fehlt in den Texten", s ?: p)
        }
        // und sie kommen in den Kurztexten wirklich vor
        val used = RunSummary.mirror(1, 1, 1, 1, 1, true, 1).keys() + RunSummary.photos(1, 1, 1, 1).keys()
        assertTrue(used.containsAll(RunSummary.NOT_IDLE_KEYS))
    }
}
