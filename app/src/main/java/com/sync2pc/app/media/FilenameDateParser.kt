package com.sync2pc.app.media

import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId

/**
 * Erkennt ein Datum im Dateinamen, z. B.
 *  IMG_20260614_101530.jpg, PXL_20260614_101530123.jpg, VID-20260614-WA0001.mp4,
 *  Screenshot_2026-06-14-10-15-30.png, WhatsApp Image 2026-06-14 at 10.15.30.jpeg,
 *  signal-2026-06-14-101530.jpg, 1718364000000.jpg (Unix-Zeit in ms)
 */
object FilenameDateParser {
    private val ymd = Regex("""(?<!\d)((?:19|20)\d{2})[-_.]?(0[1-9]|1[0-2])[-_.]?(0[1-9]|[12]\d|3[01])""")
    private val epochMs = Regex("""(?<!\d)(1[3-9]\d{11})(?!\d)""")

    fun parse(fileName: String, zone: ZoneId = ZoneId.systemDefault()): YearMonth? {
        val maxYear = Year.now(zone).value + 1
        for (m in ymd.findAll(fileName)) {
            val y = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            if (y in 1990..maxYear) return YearMonth.of(y, mo)
        }
        epochMs.find(fileName)?.let {
            val ms = it.groupValues[1].toLong()
            val dt = Instant.ofEpochMilli(ms).atZone(zone)
            if (dt.year in 2011..maxYear) return YearMonth.of(dt.year, dt.monthValue)
        }
        return null
    }
}
