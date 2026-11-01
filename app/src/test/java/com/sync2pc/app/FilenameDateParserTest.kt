package com.sync2pc.app

import com.sync2pc.app.media.FilenameDateParser
import com.sync2pc.app.smb.SmbTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId

class FilenameDateParserTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private fun p(n: String) = FilenameDateParser.parse(n, zone)

    @Test fun commonPatterns() {
        assertEquals(YearMonth.of(2026, 6), p("IMG_20260614_101530.jpg"))
        assertEquals(YearMonth.of(2026, 6), p("PXL_20260614_101530123.jpg"))
        assertEquals(YearMonth.of(2026, 6), p("VID-20260614-WA0001.mp4"))
        assertEquals(YearMonth.of(2026, 6), p("Screenshot_2026-06-14-10-15-30.png"))
        assertEquals(YearMonth.of(2026, 6), p("WhatsApp Image 2026-06-14 at 10.15.30.jpeg"))
        assertEquals(YearMonth.of(2026, 6), p("signal-2026-06-14-101530.jpg"))
    }

    @Test fun epochMillis() {
        // 2024-06-14 11:20 UTC
        assertEquals(YearMonth.of(2024, 6), p("1718364000000.jpg"))
    }

    @Test fun noDate() {
        assertNull(p("DSC_0001.JPG"))
        assertNull(p("foto.jpg"))
    }

    @Test fun smbPaths() {
        assertEquals(SmbTarget("fs01.local", "Fotos", ""), SmbTarget.parse("\\\\fs01.local\\Fotos"))
        assertEquals(SmbTarget("fs01.local", "Fotos", "Handy\\Max"), SmbTarget.parse("\\\\fs01.local\\Fotos\\Handy\\Max\\"))
        assertEquals(SmbTarget("fs01", "Fotos", ""), SmbTarget.parse("smb://fs01/Fotos"))
        assertEquals(SmbTarget("fs01", "Fotos", ""), SmbTarget.parse("SMB:\\\\fs01\\Fotos"))
        assertNull(SmbTarget.parse("\\\\fs01"))
    }
}
