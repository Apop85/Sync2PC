package com.sync2pc.app

import com.sync2pc.app.data.PendingRestore
import com.sync2pc.app.mirror.PurgeRule
import com.sync2pc.app.mirror.PurgeSides
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashEntry
import com.sync2pc.app.mirror.TrashPurge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Papierkorb leeren" (0.6.0) */
class TrashPurgeTest {

    private fun e(date: String, side: Side, size: Long = 10) =
        TrashEntry(side, ".LeoSync-Papierkorb/$date/a.txt", "a.txt", date, size, 0)

    @Test fun rules() {
        assertTrue(PurgeRule(PurgeRule.Mode.ALL).matches("2026-11-03"))
        val older = PurgeRule(PurgeRule.Mode.OLDER, before = "2026-11-01")
        assertTrue(older.matches("2026-10-31"))
        assertFalse(older.matches("2026-11-01"))
        assertFalse(older.matches("kein-datum")) // nie andere Ordner löschen
        val dates = PurgeRule(PurgeRule.Mode.DATES, dates = setOf("2026-11-02"))
        assertTrue(dates.matches("2026-11-02"))
        assertFalse(dates.matches("2026-11-03"))
    }

    @Test fun olderUsesSameCutoffAsRetention() {
        // 3. November 12:00 UTC, 7 Tage -> Stichtag 27. Oktober
        val now = 1793707200000L
        assertEquals(PurgeRule.Mode.OLDER, PurgeRule.older(7, now).mode)
        assertEquals("2026-10-27", PurgeRule.older(7, now).before)
    }

    @Test fun encodeDecodeRoundTrip() {
        for (r in listOf(
            PurgeRule(PurgeRule.Mode.ALL),
            PurgeRule(PurgeRule.Mode.OLDER, before = "2026-10-04"),
            PurgeRule(PurgeRule.Mode.DATES, dates = setOf("2026-11-02", "2026-11-01")),
        )) assertEquals(r, PurgeRule.decode(r.encode()))
        assertNull(PurgeRule.decode("Unsinn"))
        assertNull(PurgeRule.decode("DATES:"))
        assertNull(PurgeRule.decode("OLDER:"))
    }

    @Test fun affectedBySide() {
        val list = listOf(e("2026-10-01", Side.LOCAL, 5), e("2026-10-01", Side.REMOTE, 7), e("2026-11-03", Side.LOCAL))
        val rule = PurgeRule(PurgeRule.Mode.OLDER, before = "2026-11-01")
        assertEquals(2, TrashPurge.affected(list, rule, PurgeSides.BOTH).size)
        assertEquals(5L, TrashPurge.bytes(TrashPurge.affected(list, rule, PurgeSides.LOCAL)))
        assertEquals(7L, TrashPurge.bytes(TrashPurge.affected(list, rule, PurgeSides.REMOTE)))
        assertEquals(3, TrashPurge.affected(list, PurgeRule(PurgeRule.Mode.ALL), PurgeSides.BOTH).size)
    }

    /** 0.7.0: zum Wiederherstellen vorgemerkte Einträge bleiben immer liegen */
    @Test fun reservedEntriesAreKept() {
        val list = listOf(e("2026-10-01", Side.LOCAL, 5), e("2026-10-01", Side.REMOTE, 7))
        val pending = listOf(PendingRestore(profileId = 1, side = "REMOTE",
            trashPath = ".LeoSync-Papierkorb/2026-10-01/A.TXT", origPath = "a.txt", requestedAt = 0))
        val reserved = TrashPurge.reservedKeys(pending)
        val all = PurgeRule(PurgeRule.Mode.ALL)
        val hit = TrashPurge.affected(list, all, PurgeSides.BOTH, reserved)
        assertEquals(listOf(Side.LOCAL), hit.map { it.side })
        assertEquals(1, TrashPurge.protectedHits(list, all, PurgeSides.BOTH, reserved))
        assertEquals(0, TrashPurge.protectedHits(list, all, PurgeSides.LOCAL, reserved))
        assertEquals(setOf(".leosync-papierkorb/2026-10-01/a.txt"), TrashPurge.reservedPaths(pending, Side.REMOTE))
        assertTrue(TrashPurge.reservedPaths(pending, Side.LOCAL).isEmpty())
    }
}
