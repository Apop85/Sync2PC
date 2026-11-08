package com.sync2pc.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.workDataOf
import com.sync2pc.app.data.SyncRun
import com.sync2pc.app.data.messageText
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.sync.RunSummary
import com.sync2pc.app.sync.SyncScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Kennungen mit den echten Android-Ressourcen (0.12.1, Auftrag -043): `AndroidResolver` löst Namen per
 * `getResourceEntryName`/`getIdentifier` auf. Läuft auf Gerät/Emulator (`gradlew connectedDebugAndroidTest`).
 * Die Unit-Tests (`MsgTest`) nutzen dagegen `TestTexts` (nur Deutsch, aus den XML-Dateien).
 */
@RunWith(AndroidJUnit4::class)
class MsgInstrumentedTest {
    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun prefs() = ctx.getSharedPreferences("lang", Context.MODE_PRIVATE)
    private var savedLang: String? = null

    @Before fun setUp() {
        savedLang = prefs().getString("tag", "")
        Texts.init(ctx)
    }

    @After fun tearDown() {
        prefs().edit().putString("tag", savedLang ?: "").commit()
        Texts.init(ctx)
    }

    /** Erwarteter Text aus den echten Ressourcen in einer bestimmten Sprache */
    private fun res(lang: String, block: Context.() -> String): String {
        val cfg = Configuration(ctx.resources.configuration).apply { setLocale(Locale.forLanguageTag(lang)) }
        return ctx.createConfigurationContext(cfg).block()
    }

    /** Sprache der App setzen (Android 10–12: eigene Einstellung; ab Android 13 steuert das System) */
    private fun useLanguage(lang: String) {
        assumeTrue("Sprachwechsel per Prefs nur bis Android 12", Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        prefs().edit().putString("tag", lang).commit()
        Texts.init(ctx)
    }

    @Test fun nameRoundTripGivesRealTextNotFallback() {
        val code = Msg.of(R.string.run_nothing).toCode()
        assertTrue(code!!.contains("run_nothing"))
        val shown = Msg.render(code, "FALLBACK")
        assertNotEquals("FALLBACK", shown)
        assertEquals(Msg.of(R.string.run_nothing).text(), shown)
    }

    @Test fun pluralAndNestedRoundTrip() {
        val plural = Msg.plural(R.plurals.run_failed, 2)
        assertEquals(plural.text(), Msg.render(plural.toCode(), "FALLBACK"))
        val one = Msg.plural(R.plurals.run_failed, 1)
        assertNotEquals(one.text(), plural.text())
        val nested = Msg.of(R.string.eng_interrupted_photos, Msg.of(R.string.stop_wifi), 7)
        assertEquals(nested.text(), Msg.render(nested.toCode(), "FALLBACK"))
        val summary = RunSummary.mirror(2, 1, 3, 2, 1, true)
        assertEquals(summary.text(), Msg.render(summary.toCode(), "FALLBACK"))
    }

    @Test fun unknownKeyFallsBackToStoredText() {
        assertEquals("alt", Msg.render("""{"k":"gibt_es_nicht"}""", "alt"))
        assertEquals("alt", Msg.render("""{"k":"gibt_es_nicht","n":2}""", "alt"))
        assertEquals("alt", Msg.render("{kaputt", "alt"))
    }

    @Test fun sameCodeFollowsTheActiveLanguage() {
        val code = RunSummary.mirror(1, 0, 0, 0, 0, false).toCode()
        useLanguage("de")
        assertEquals(res("de") { getString(R.string.run_up, 1) }, Msg.render(code, "FALLBACK"))
        useLanguage("en")
        assertEquals(res("en") { getString(R.string.run_up, 1) }, Msg.render(code, "FALLBACK"))
        assertNotEquals(res("de") { getString(R.string.run_up, 1) }, res("en") { getString(R.string.run_up, 1) })
    }

    @Test fun storedRunShowsActiveLanguageNotWritingLanguage() {
        val code = RunSummary.NOTHING.toCode()
        val run = SyncRun(profileId = 1, profileName = "P", trigger = "x", dryRun = false, startedAt = 1,
            message = "geschrieben in Sprache A", messageCode = code)
        useLanguage("en")
        assertEquals(res("en") { getString(R.string.run_nothing) }, run.messageText())
        useLanguage("de")
        assertEquals(res("de") { getString(R.string.run_nothing) }, run.messageText())
    }

    @Test fun triggerDataRoundTripAndFallbackWithoutCode() {
        val t = Msg.of(R.string.trigger_safety, 6)
        val data = SyncScheduler.triggerData(t, SyncScheduler.KEY_MANUAL to false)
        assertEquals(t.text(), SyncScheduler.triggerOf(data, Msg.lit("default")).text())
        assertTrue(data.getString(SyncScheduler.KEY_TRIGGER_CODE)!!.contains("trigger_safety"))
        assertEquals(false, data.getBoolean(SyncScheduler.KEY_MANUAL, true))
        // Bereits eingeplanter Auftrag ohne Kennung: Text; ganz ohne Angaben: Standard
        val old = workDataOf(SyncScheduler.KEY_TRIGGER to "Manuell")
        assertEquals("Manuell", SyncScheduler.triggerOf(old, Msg.lit("default")).text())
        assertEquals("default", SyncScheduler.triggerOf(workDataOf(), Msg.lit("default")).text())
    }
}
