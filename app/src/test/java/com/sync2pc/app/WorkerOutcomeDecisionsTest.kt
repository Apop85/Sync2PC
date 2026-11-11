package com.sync2pc.app

import com.sync2pc.app.mirror.OldFileRecovery
import com.sync2pc.app.sync.SyncDecisions
import com.sync2pc.app.sync.SyncDecisions.SummaryKind
import com.sync2pc.app.sync.SyncDecisions.UnreachableAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** B1/B4/P5 (SYNC-20261111-207): Abschluss-Entscheidungen des SyncWorker und Wiederanlauf «.s2pold» */
class WorkerOutcomeDecisionsTest {
    // ---- B1: Ausnahme in work() UND im Abschluss ----
    @Test fun onlyFinishFailsRethrowsFinishFailure() {
        val finish = IllegalStateException("finish")
        assertSame(finish, SyncDecisions.mergeFailures(null, finish))
    }

    @Test fun bothFailKeepsWorkCauseAndAttachesFinish() {
        val work = RuntimeException("work")
        val finish = IllegalStateException("finish")
        val r = SyncDecisions.mergeFailures(work, finish)
        assertSame(work, r)
        assertEquals(listOf<Throwable>(finish), r.suppressed.toList())
    }

    // ---- B4: Abschluss-Meldung ----
    @Test fun hardFailureBeatsEverything() {
        assertEquals(SummaryKind.FAILED, SyncDecisions.summaryKind(true, true, 5, 1))
        assertEquals(SummaryKind.FAILED, SyncDecisions.summaryKind(true, false, 0, 0))
    }

    @Test fun midRunInterruptionIsSilent() {
        assertEquals(SummaryKind.NONE, SyncDecisions.summaryKind(false, true, 5, 1))
    }

    @Test fun transferredOrFailedFilesAreReported() {
        assertEquals(SummaryKind.TRANSFERRED, SyncDecisions.summaryKind(false, false, 3, 0))
        assertEquals(SummaryKind.TRANSFERRED, SyncDecisions.summaryKind(false, false, 0, 2))
    }

    @Test fun nothingDoneIsSilent() {
        assertEquals(SummaryKind.NONE, SyncDecisions.summaryKind(false, false, 0, 0))
    }

    @Test fun staleReminderOnlyResolvedAfterCleanRun() {
        assertTrue(SyncDecisions.staleResolved(false, false, true, true))
        assertFalse(SyncDecisions.staleResolved(true, false, true, true))
        assertFalse(SyncDecisions.staleResolved(false, true, true, true))
        assertFalse(SyncDecisions.staleResolved(false, false, false, true))
        assertFalse(SyncDecisions.staleResolved(false, false, true, false))
    }

    // ---- B4: nicht erreichbare Profile ----
    @Test fun nothingWaitingClearsWaitWithoutNetworkQuery() {
        var asked = false
        val a = SyncDecisions.unreachableAction(false, manual = true, midRun = true) { asked = true; true }
        assertEquals(UnreachableAction.CLEAR_WAIT, a)
        assertFalse(asked)
    }

    @Test fun manualStartOnlyNotifies() {
        var asked = false
        val a = SyncDecisions.unreachableAction(true, manual = true, midRun = false) { asked = true; true }
        assertEquals(UnreachableAction.NOTIFY_ONLY, a)
        assertFalse(asked)
    }

    @Test fun manualStartLostMidRunIsTreatedLikeAutomatic() {
        assertEquals(UnreachableAction.RETRY_TIMER, SyncDecisions.unreachableAction(true, true, true) { true })
        assertEquals(UnreachableAction.RESUME_ON_WIFI, SyncDecisions.unreachableAction(true, true, true) { false })
    }

    @Test fun automaticInTargetWifiSetsTimer() {
        assertEquals(UnreachableAction.RETRY_TIMER, SyncDecisions.unreachableAction(true, false, false) { true })
    }

    @Test fun automaticWifiLostResumesOnConnect() {
        assertEquals(UnreachableAction.RESUME_ON_WIFI, SyncDecisions.unreachableAction(true, false, false) { false })
    }

    // ---- P5: liegengebliebene «.s2pold» ----
    @Test fun oldFileWithBaseIsDeletedOnlyWhenRecovering() {
        assertEquals(OldFileRecovery.Action.DELETE_OLD, OldFileRecovery.decide(baseExists = true, recover = true))
        assertEquals(OldFileRecovery.Action.IGNORE, OldFileRecovery.decide(baseExists = true, recover = false))
    }

    @Test fun orphanOldFileIsTheValidFile() {
        assertEquals(OldFileRecovery.Action.RENAME_BACK, OldFileRecovery.decide(baseExists = false, recover = true))
        // Simulation: nichts verändern, aber unter dem Namen der Datei zählen
        assertEquals(OldFileRecovery.Action.LIST_AS_BASE, OldFileRecovery.decide(baseExists = false, recover = false))
    }

    @Test fun simulationNeverDeletesOrRenames() {
        for (base in listOf(true, false)) {
            val a = OldFileRecovery.decide(base, recover = false)
            assertTrue(a == OldFileRecovery.Action.IGNORE || a == OldFileRecovery.Action.LIST_AS_BASE)
        }
    }
}
