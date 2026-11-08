package com.sync2pc.app

import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashEntry
import com.sync2pc.app.mirror.TrashPurge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Aufräumen führt nur den bestätigten Stand aus (0.11.10, DEC-144) */
class TrashPurgeConfirmedTest {

    private fun e(date: String, name: String, size: Long = 10, side: Side = Side.REMOTE) =
        TrashEntry(side, ".LeoSync-Papierkorb/$date/$name", name, date, size, 0)

    @Test fun wholeDayWhenEverythingConfirmed() {
        val cur = listOf(e("2026-11-08", "a.txt"), e("2026-11-08", "b.txt"))
        val p = TrashPurge.planConfirmed(cur, cur, emptySet())
        assertEquals(listOf(".LeoSync-Papierkorb/2026-11-08"), p.wholeDirs)
        assertEquals(0, p.files.size)
    }

    @Test fun newerVersionFromSyncStaysEvenWhenDayWasConfirmed() {
        // Beim Bestätigen gab es nur a.txt; inzwischen hat ein Sync b.txt in denselben Tag gelegt
        val seen = listOf(e("2026-11-08", "a.txt"))
        val now = seen + e("2026-11-08", "b.txt")
        val p = TrashPurge.planConfirmed(now, seen, emptySet())
        assertEquals(0, p.wholeDirs.size)
        assertEquals(listOf("a.txt"), p.files.map { it.name })
    }

    @Test fun changedSizeIsSkipped() {
        val seen = listOf(e("2026-11-08", "a.txt", size = 10))
        val now = listOf(e("2026-11-08", "a.txt", size = 99))
        val p = TrashPurge.planConfirmed(now, seen, emptySet())
        assertEquals(0, p.wholeDirs.size)
        assertEquals(0, p.files.size)
    }

    @Test fun reservedRestoreIsNeverDeletedAndKeepsDayAlive() {
        val a = e("2026-11-08", "a.txt"); val b = e("2026-11-08", "b.txt")
        val p = TrashPurge.planConfirmed(listOf(a, b), listOf(a, b), setOf(TrashPurge.key(b)))
        assertEquals(0, p.wholeDirs.size)
        assertEquals(listOf("a.txt"), p.files.map { it.name })
    }

    @Test fun vanishedEntriesAreIgnoredAndDaysAreIndependent() {
        val a = e("2026-11-01", "a.txt"); val gone = e("2026-11-02", "x.txt"); val c = e("2026-11-03", "c.txt")
        val p = TrashPurge.planConfirmed(listOf(a, c), listOf(a, gone), emptySet())
        assertEquals(listOf(".LeoSync-Papierkorb/2026-11-01"), p.wholeDirs)
        assertEquals(0, p.files.size) // 2026-11-03 war nicht bestätigt
    }

    @Test fun dateDirNeedsThreeSegments() {
        assertEquals(".LeoSync-Papierkorb/2026-11-08", TrashPurge.dateDir(".LeoSync-Papierkorb/2026-11-08/sub/a.txt"))
        assertNull(TrashPurge.dateDir(".LeoSync-Papierkorb/a.txt"))
    }
}
