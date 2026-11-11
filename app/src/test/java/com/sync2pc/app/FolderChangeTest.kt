package com.sync2pc.app

import com.sync2pc.app.mirror.FolderChange
import com.sync2pc.app.sync.SkipFollowUp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SYNC-20261111-213: Folgelauf für Dateien, die nach dem Einlesen entstehen (Ordner-Stempelvergleich) */
class FolderChangeTest {
    private val before = mapOf("root" to 100L, "a" to 200L, "b" to 300L)

    @Test fun unchangedFoldersGiveNoFollowUp() =
        assertFalse(FolderChange.changed(before, ownWrites = false) { before[it] })

    @Test fun newFileInRootChangesRootStamp() =
        assertTrue(FolderChange.changed(before, ownWrites = false) { if (it == "root") 150L else before[it] })

    @Test fun changeInSubfolderIsFound() =
        assertTrue(FolderChange.changed(before, ownWrites = false) { if (it == "b") 301L else before[it] })

    @Test fun vanishedFolderCountsAsChanged() =
        assertTrue(FolderChange.changed(before, ownWrites = false) { if (it == "a") null else before[it] })

    @Test fun ownWritesNeverTriggerFollowUp() =
        assertFalse(FolderChange.changed(before, ownWrites = true) { 999L })

    @Test fun providerWithoutStampGivesNoStatement() =
        assertFalse(FolderChange.changed(mapOf("root" to 0L), ownWrites = false) { 12345L })

    @Test fun emptySnapshotGivesNoStatement() =
        assertFalse(FolderChange.changed(emptyMap(), ownWrites = false) { 1L })

    @Test fun checkCountIsCapped() {
        val many = (1..1000).associate { "d$it" to 5L }
        var asked = 0
        FolderChange.changed(many, ownWrites = false) { asked++; 5L }
        assertTrue("höchstens ${FolderChange.MAX_CHECKS} Abfragen, waren $asked", asked <= FolderChange.MAX_CHECKS)
    }

    // ---- gleiche Begrenzung wie beim Folgelauf nach zurückgestellten Dateien ----
    private val hour = SkipFollowUp.MIN_INTERVAL_MS

    @Test fun changedFolderPlansFollowUpWithoutDeferredFiles() =
        assertTrue(SkipFollowUp.mayPlan(0, 10 * hour, 0, changedDuringRun = true))

    @Test fun changedFolderStillLimitedToOnePerHour() {
        val last = 10 * hour
        assertFalse(SkipFollowUp.mayPlan(0, last + 5 * 60_000, last, changedDuringRun = true))
        assertTrue(SkipFollowUp.mayPlan(0, last + hour, last, changedDuringRun = true))
    }

    @Test fun nothingDeferredAndNothingChangedPlansNothing() =
        assertFalse(SkipFollowUp.mayPlan(0, 10 * hour, 0, changedDuringRun = false))
}