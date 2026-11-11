package com.sync2pc.app

import com.sync2pc.app.mirror.TransferResume
import com.sync2pc.app.mirror.TransferStep
import org.junit.Assert.assertEquals
import org.junit.Test

/** SYNC-20261111-166: Wiederholung nach Verbindungsabbruch darf die Übertragung nie überspringen, wenn der Lauf selbst verschoben hat */
class TransferResumeTest {
    private fun step(trashOther: Boolean = true, hadTarget: Boolean = true, before: Boolean = false, unchanged: Boolean = true, absent: Boolean = false) =
        TransferResume.step(trashOther, hadTarget, before, unchanged, absent)

    // ---- erster Versuch (Verhalten wie bisher) ----
    @Test fun firstAttemptWithVersionProtectionTrashesThenTransfers() {
        assertEquals(TransferStep.TRASH_THEN_TRANSFER, step())
    }

    @Test fun firstAttemptWithoutVersionProtectionJustTransfers() {
        assertEquals(TransferStep.TRANSFER, step(trashOther = false))
    }

    @Test fun firstAttemptWithoutExistingTargetJustTransfers() {
        assertEquals(TransferStep.TRANSFER, step(hadTarget = false))
    }

    @Test fun firstAttemptWithChangedTargetIsSkipped() {
        assertEquals(TransferStep.SKIP_CHANGED, step(unchanged = false))
        assertEquals(TransferStep.SKIP_CHANGED, step(trashOther = false, unchanged = false))
    }

    // ---- Wiederholung (der Fehler): Ziel fehlt, weil der Lauf es selbst in den Papierkorb verschoben hat ----
    @Test fun retryAfterOwnTrashContinuesInsteadOfSkipping() {
        // «unchanged» ist hier falsch (Ziel fehlt), das war der Auslöser für das stille Überspringen
        assertEquals(TransferStep.TRANSFER, step(before = true, unchanged = false, absent = true))
    }

    @Test fun retryNeverTrashesAgain() {
        for (trashOther in listOf(false, true)) for (had in listOf(false, true)) for (u in listOf(false, true)) {
            val s = step(trashOther, had, before = true, unchanged = u, absent = true)
            assertEquals(TransferStep.TRANSFER, s)
        }
    }

    @Test fun retryWithNewFileAtTargetIsNeverOverwritten() {
        // inzwischen ist am Ziel wieder etwas entstanden (anderes Programm, Benutzer): nicht überschreiben
        assertEquals(TransferStep.SKIP_CHANGED, step(before = true, unchanged = true, absent = false))
        assertEquals(TransferStep.SKIP_CHANGED, step(before = true, unchanged = false, absent = false))
    }

    // ---- Invariante ----
    /** Eine Übertragung wird nie stillschweigend übersprungen, wenn das Ziel fehlt und der Lauf selbst verschoben hat */
    @Test fun skipNeverHappensForAbsentTargetAfterOwnTrash() {
        for (trashOther in listOf(false, true)) for (had in listOf(false, true)) for (u in listOf(false, true)) {
            assertEquals(false, step(trashOther, had, before = true, unchanged = u, absent = true) == TransferStep.SKIP_CHANGED)
        }
    }
}