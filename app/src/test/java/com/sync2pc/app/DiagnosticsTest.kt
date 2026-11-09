package com.sync2pc.app

import com.sync2pc.app.data.LogEntry
import com.sync2pc.app.diag.CrashReporter
import com.sync2pc.app.diag.Diagnostics
import com.sync2pc.app.diag.Redactor
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

    // ---- Maskierung (0.12.4) --------------------------------------------------------
    @Test fun redactsIpUserHostAndFiles() {
        val r = Redactor()
        r.register("Host", "nas-wohnzimmer"); r.register("Benutzer", "hans.muster"); r.register("Profil", "Familienfotos")
        val out = r.scrub("Verbindung zu nas-wohnzimmer (192.168.1.20) als hans.muster fehlgeschlagen, Profil „Familienfotos“")
        assertEquals("Verbindung zu Host-1 (<IP>) als Benutzer-1 fehlgeschlagen, Profil „Profil-1“", out)
    }

    @Test fun samePseudonymWithinExport() {
        val r = Redactor()
        assertEquals(r.pseudo("WLAN", "Heimnetz"), r.pseudo("WLAN", "heimnetz"))
        assertEquals("WLAN-1", r.pseudo("WLAN", "Heimnetz"))
        assertEquals("WLAN-2", r.pseudo("WLAN", "Büro"))
    }

    /** T5 (-055): PC-Kennung und Host der Kennung erscheinen nie im Klartext, gleiche Kennung = gleiches Pseudonym */
    @Test fun pcGuidAndItsHostArePseudonymised() {
        val guid = "11111111-2222-3333-4444-555555555555"
        val r = Redactor()
        r.register("Host", "nas-wohnzimmer"); r.register("PC-Kennung", guid)
        val pseudo = r.pseudo("PC-Kennung", guid)
        assertEquals("PC-Kennung-1", pseudo)
        assertEquals(pseudo, r.pseudo("PC-Kennung", guid.uppercase()))
        assertTrue(!pseudo.contains("1111"))
        val out = r.scrub("PC $guid unter nas-wohnzimmer als anderer PC erkannt")
        assertTrue(out, !out.contains(guid) && !out.contains("nas-wohnzimmer"))
        assertEquals("Host-1", r.pseudo("Host", "NAS-Wohnzimmer"))
    }

    @Test fun fileNamesLoseNameButKeepExtension() {
        val r = Redactor()
        assertEquals("↑ <Datei>.jpg → 2026\\10 - <Name>", r.scrub("↑ IMG_2031 Urlaub Rom.JPG → 2026\\10 - Ägypten"))
        assertEquals("✗ <Datei>.mp4: SocketTimeoutException", r.scrub("✗ clip_privat.mp4: SocketTimeoutException"))
    }

    @Test fun pathsUrlsMailMacCoordinatesAreMasked() {
        val r = Redactor()
        assertEquals("\\\\<Pfad>", r.scrub("""\\server\freigabe\privat"""))
        assertEquals("smb://<URL>", r.scrub("smb://hans@192.168.0.5/foto/x"))
        assertEquals("content://<URL>", r.scrub("content://com.android.externalstorage.documents/tree/primary%3ADCIM"))
        assertEquals("Kontakt <E-Mail>", r.scrub("Kontakt a.b@example.org"))
        assertEquals("BSSID <MAC>", r.scrub("BSSID aa:bb:cc:dd:ee:ff"))
        assertEquals("bei <Koordinaten>", r.scrub("bei 47.376888, 8.541694"))
        assertEquals("<IP> und <IP>", r.scrub("fe80::1a2b:3c4d und 2001:db8::1"))
        assertEquals("UnknownHostException: <Host>", r.scrub("UnknownHostException: meinpc"))
    }

    @Test fun harmlessTextStaysReadable() {
        val r = Redactor()
        assertEquals("12:34:56 Version 0.12.4 und/oder 3 / 5 Dateien", r.scrub("12:34:56 Version 0.12.4 und/oder 3 / 5 Dateien"))
        assertEquals("at com.sync2pc.app.sync.SyncEngine.run(SyncEngine.kt:123)", r.scrub("at com.sync2pc.app.sync.SyncEngine.run(SyncEngine.kt:123)"))
        assertEquals("https://api.github.com/repos/x/y", r.scrub("https://api.github.com/repos/x/y"))
        assertEquals("999.1.1.1", r.scrub("999.1.1.1")) // keine gültige IP
    }

    @Test fun folderPathKeepsStandardFoldersAndDates() {
        val r = Redactor()
        assertEquals("DCIM/Ordner-1", r.folderPath("DCIM/Familie Meier"))
        assertEquals("2019\\02", r.folderPath("2019\\02", "\\"))
        assertEquals("/storage/emulated/0/DCIM/Ordner-2", r.scrub("/storage/emulated/0/DCIM/Reise2024"))
        assertEquals("<Pfad>", r.scrub("/data/app/~~x/ch.leo.sync-y/base.apk"))
    }

    @Test fun maskingIsIdempotent() {
        val r = Redactor()
        val once = r.scrub("Ordner Reise2024/Rom/foto.jpg bei 10.0.0.7")
        assertEquals(once, r.scrub(once))
    }

    @Test fun compressMasksFolderNames() {
        val out = Diagnostics.compress(listOf(
            e("I", "= 1 Datei bereits in 2026\\10 - Ägypten vorhanden"),
            e("W", "Fehler bei 192.168.1.2"),
        ), Redactor())
        assertEquals(2, out.size)
        assertTrue(out[0], out[0].contains("2026\\10 - <Name>") && !out[0].contains("Ägypten"))
        assertTrue(out[1], out[1].endsWith("W Fehler bei <IP>"))
    }

    @Test fun patternShapeHidesContent() {
        assertEquals("2 Regel(n): x*.x x?", Diagnostics.patternShape("Urlaub*.tmp\nab?"))
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
