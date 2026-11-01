package com.sync2pc.app

import com.sync2pc.app.data.LogEntry
import com.sync2pc.app.diag.CrashReporter
import com.sync2pc.app.diag.Diagnostics
import com.sync2pc.app.diag.VendorHints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {
    private fun e(level: String, msg: String) = LogEntry(runId = 1, time = 0L, level = level, message = msg)

    @Test fun compressSumsFilesOfNewFormat() {
        val out = Diagnostics.compress(listOf(
            e("I", "Start"),
            e("I", "= 37 Dateien bereits in 2014\\04 vorhanden"),
            e("I", "= 1 Datei bereits in 2026\\10 - Ägypten vorhanden"),
            e("W", "Lauf abgebrochen"),
        ))
        assertEquals(3, out.size)
        assertTrue(out[1], out[1].contains("= 38 Dateien bereits vorhanden (2 Einträge: 2014\\04, 2026\\10 - Ägypten)"))
        assertTrue(out[2].endsWith("W Lauf abgebrochen"))
    }

    @Test fun compressCountsOldFormatPerLine() {
        val out = Diagnostics.compress(listOf(
            e("I", "= a.jpg war bereits in 2019\\02"),
            e("I", "= b.jpg war bereits in 2019\\02"),
            e("I", "= c.jpg war bereits in 2019\\03"),
        ))
        assertEquals(1, out.size)
        assertTrue(out[0], out[0].contains("= 3 Dateien bereits vorhanden (3 Einträge: 2019\\02, 2019\\03)"))
    }

    @Test fun singleLineStaysUnchanged() {
        val out = Diagnostics.compress(listOf(e("I", "= 5 Dateien bereits in 2020\\01 vorhanden")))
        assertEquals(1, out.size)
        assertTrue(out[0].endsWith("I = 5 Dateien bereits in 2020\\01 vorhanden"))
    }

    @Test fun vendorDetection() {
        assertEquals("OnePlus", VendorHints.forDevice("OnePlus")?.vendor)
        assertEquals("Samsung", VendorHints.forDevice("samsung")?.vendor)
        assertEquals("Xiaomi", VendorHints.forDevice("Xiaomi")?.vendor)
        assertEquals("Honor", VendorHints.forDevice("HONOR")?.vendor)
        assertNull(VendorHints.forDevice("Google"))
        assertEquals("https://dontkillmyapp.com/oneplus", VendorHints.webUrl(VendorHints.forDevice("OnePlus")))
        assertEquals("https://dontkillmyapp.com/", VendorHints.webUrl(null))
    }

    @Test fun crashLogKeepsNewestThree() {
        var content = ""
        for (i in 1..5) content = CrashReporter.merge(CrashReporter.format("t$i", "0.1.3", "main", "Fehler $i"), content)
        val list = CrashReporter.split(content)
        assertEquals(CrashReporter.KEEP, list.size)
        assertTrue(list[0].startsWith("t5"))
        assertTrue(list[2].startsWith("t3"))
    }

    @Test fun crashTraceIsShortened() {
        val entry = CrashReporter.format("t", "0.1.3", "main", "x".repeat(CrashReporter.MAX_TRACE_CHARS + 100))
        assertTrue(entry.endsWith("… gekürzt"))
    }
}
