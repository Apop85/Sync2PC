package com.sync2pc.app

import com.sync2pc.app.mirror.TransferPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferPlanTest {
    @Test fun smallFilesFirst() {
        val sizes = listOf(500L, 3L, 70L, 3L)
        assertEquals(listOf(3L, 3L, 70L, 500L), TransferPlan.smallFirst(sizes) { it })
    }

    @Test fun resumeWhenPcFileUnchanged() {
        // 400 von 1000 Bytes geladen, PC-Datei älter als die angefangene Datei -> ab 400 weiter
        assertEquals(400L, TransferPlan.resumeOffset(tmpSize = 400, tmpModified = 2_000, remoteSize = 1_000, remoteModified = 1_000))
    }

    @Test fun restartWhenPcFileChangedOrUnclear() {
        // PC-Datei nach dem Abbruch geändert
        assertEquals(0L, TransferPlan.resumeOffset(400, 2_000, 1_000, 3_000))
        // angefangene Datei leer oder schon so gross wie das Original
        assertEquals(0L, TransferPlan.resumeOffset(0, 2_000, 1_000, 1_000))
        assertEquals(0L, TransferPlan.resumeOffset(1_000, 2_000, 1_000, 1_000))
        assertEquals(0L, TransferPlan.resumeOffset(1_200, 2_000, 1_000, 1_000))
        // Zeitstempel unbekannt -> sicherheitshalber neu
        assertEquals(0L, TransferPlan.resumeOffset(400, 0, 1_000, 1_000))
        assertEquals(0L, TransferPlan.resumeOffset(400, 2_000, 1_000, 0))
    }
}
