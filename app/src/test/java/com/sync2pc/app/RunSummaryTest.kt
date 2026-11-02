package com.sync2pc.app

import com.sync2pc.app.data.LogItem
import com.sync2pc.app.data.RunGrouping
import com.sync2pc.app.data.SyncRun
import com.sync2pc.app.sync.RunSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSummaryTest {
    @Test fun photosSummaryOnlyShowsWhatHappened() {
        assertEquals("3 übertragen", RunSummary.photos(3, 0, 0, 0))
        assertEquals("3 übertragen, 12 bereits vorhanden", RunSummary.photos(3, 12, 0, 0))
        assertEquals("12 bereits vorhanden", RunSummary.photos(0, 12, 0, 0))
        assertEquals("1 übertragen, 2 Fehler, 4 warten auf Ordnerauswahl", RunSummary.photos(1, 0, 2, 4))
        assertEquals(RunSummary.NOTHING, RunSummary.photos(0, 0, 0, 0))
    }

    @Test fun mirrorSummarySkipsZeros() {
        assertEquals("↓ 3 heruntergeladen", RunSummary.mirror(0, 3, 0, 0, 0, false))
        assertEquals("↑ 1 hochgeladen, 2 in Papierkorb", RunSummary.mirror(1, 0, 2, 0, 0, false))
        assertEquals(RunSummary.NOTHING, RunSummary.mirror(0, 0, 0, 0, 0, false))
        assertEquals("Speicher voll", RunSummary.mirror(0, 0, 0, 0, 0, true))
    }

    private fun run(id: Long, status: String = "OK", up: Int = 0, msg: String = RunSummary.NOTHING) =
        SyncRun(id = id, profileId = 1, profileName = "Fotos", trigger = "WLAN", dryRun = false,
            startedAt = id * 1000, finishedAt = id * 1000 + 500, status = status, uploaded = up, message = msg)

    @Test fun consecutiveIdleRunsAreGrouped() {
        // neueste zuerst: 6 idle, 5 idle, 4 Upload, 3 idle, 2 idle, 1 idle, 0 Fehler
        val runs = listOf(run(6), run(5), run(4, up = 3, msg = "3 übertragen"), run(3), run(2), run(1), run(0, status = "FAILED", msg = "x"))
        val items = RunGrouping.group(runs)
        assertEquals(4, items.size)
        assertEquals(listOf(6L, 5L), (items[0] as LogItem.Idle).runs.map { it.id })
        assertEquals(4L, (items[1] as LogItem.Single).run.id)
        assertEquals(3, (items[2] as LogItem.Idle).runs.size)
        assertEquals(0L, (items[3] as LogItem.Single).run.id)
    }

    @Test fun singleIdleRunStaysSingle() {
        val items = RunGrouping.group(listOf(run(2, up = 1, msg = "1 übertragen"), run(1)))
        assertTrue(items[1] is LogItem.Single)
    }

    @Test fun errorAndTransferFilters() {
        // neueste zuerst: Upload, Fehler, Upload mit Datei-Fehler, 2× ruhig, Unterbruch
        val runs = listOf(
            run(6, up = 2, msg = "2 übertragen"),
            run(5, status = "FAILED", msg = "PC nicht erreichbar"),
            run(4, status = "PARTIAL", up = 1, msg = "1 übertragen, 1 Fehler"),
            run(3), run(2),
            run(1, status = "INTERRUPTED", msg = "Unterbrochen"),
        )
        val items = RunGrouping.group(runs)
        fun ids(e: Boolean, t: Boolean) = RunGrouping.filter(items, e, t).map { (it as LogItem.Single).run.id }
        assertEquals(items, RunGrouping.filter(items, false, false)) // kein Filter = alles
        assertEquals(listOf(5L, 4L), ids(e = true, t = false))
        assertEquals(listOf(6L, 4L), ids(e = false, t = true))
        assertEquals(listOf(6L, 5L, 4L), ids(e = true, t = true))
    }

    @Test fun foundOnPcOrTrashIsNotIdle() {
        assertTrue(!RunGrouping.isIdle(run(1, msg = "12 bereits vorhanden")))
        assertTrue(!RunGrouping.isIdle(run(1, msg = "2 in Papierkorb")))
        assertTrue(!RunGrouping.isIdle(run(1, status = "INTERRUPTED")))
        assertTrue(RunGrouping.isIdle(run(1, msg = "0 übertragen, 6988 übersprungen"))) // altes Format bis 0.3.3
    }
}
