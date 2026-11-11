package com.sync2pc.app

import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.data.SyncDirection
import com.sync2pc.app.data.SyncDirection.BOTH
import com.sync2pc.app.data.SyncDirection.DOWNLOAD
import com.sync2pc.app.data.SyncDirection.UPLOAD
import com.sync2pc.app.mirror.LocalEntry
import com.sync2pc.app.mirror.MirrorPlanner
import com.sync2pc.app.mirror.MirrorPlanner.Decision
import com.sync2pc.app.mirror.PlanKind
import com.sync2pc.app.smb.RemoteEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * P1/P2 (SYNC-20261111-163): Entscheidungstabelle des 3-Wege-Abgleichs aus MirrorEngine.plan().
 * Die Tests beschreiben das heutige Verhalten; sie ändern es nicht.
 */
class MirrorPlannerTest {
    private fun local(size: Long = 10, mod: Long = 100) = LocalEntry("id", "a.txt", size, mod, false)
    private fun remote(size: Long = 10, mod: Long = 100) = RemoteEntry("a.txt", size, false, mod)
    private fun state(ls: Long = 10, lm: Long = 100, rs: Long = 10, rm: Long = 100) = MirrorState(1, "a.txt", ls, lm, rs, rm, 0)

    private var hashCalls = 0
    private fun decide(
        l: LocalEntry?, r: RemoteEntry?, st: MirrorState?, dir: SyncDirection,
        propagate: Boolean = false, newer: Boolean = false, keep: Boolean = false, same: Boolean = false,
    ): Decision? = runBlocking {
        hashCalls = 0
        MirrorPlanner.decide(l, r, st, dir, propagate, newer, keep) { hashCalls++; same }
    }

    private fun kind(d: Decision?) = d?.kind

    // ---- nichts vorhanden ----
    @Test fun bothMissingWithStateDropsTheState() {
        for (dir in SyncDirection.entries) assertEquals(Decision(PlanKind.DROP), decide(null, null, state(), dir))
    }

    @Test fun bothMissingWithoutStateDoesNothing() {
        for (dir in SyncDirection.entries) assertNull(decide(null, null, null, dir))
    }

    // ---- nur auf dem Handy ----
    @Test fun newOnPhoneIsUploadedWhenDirectionAllowsIt() {
        assertEquals(PlanKind.UPLOAD, kind(decide(local(), null, null, UPLOAD)))
        assertEquals(PlanKind.UPLOAD, kind(decide(local(), null, null, BOTH)))
        assertNull(decide(local(), null, null, DOWNLOAD)) // PC -> Handy: Handy-Datei wird ignoriert, nicht gelöscht
    }

    @Test fun deletedOnPcRestoredInUploadMode() {
        assertEquals(PlanKind.UPLOAD, kind(decide(local(), null, state(), UPLOAD, propagate = true)))
    }

    @Test fun deletedOnPcPropagatesOnlyWhenEnabled() {
        assertEquals(PlanKind.TRASH_LOCAL, kind(decide(local(), null, state(), BOTH, propagate = true)))
        assertEquals(PlanKind.TRASH_LOCAL, kind(decide(local(), null, state(), DOWNLOAD, propagate = true)))
    }

    @Test fun deletedOnPcWithoutPropagationLosesNothing() {
        assertEquals(PlanKind.UPLOAD, kind(decide(local(), null, state(), BOTH))) // nichts verlieren
        assertEquals(PlanKind.DROP, kind(decide(local(), null, state(), DOWNLOAD))) // Handy-Kopie bleibt
    }

    // ---- nur auf dem PC ----
    @Test fun newOnPcIsDownloadedWhenDirectionAllowsIt() {
        assertEquals(PlanKind.DOWNLOAD, kind(decide(null, remote(), null, DOWNLOAD)))
        assertEquals(PlanKind.DOWNLOAD, kind(decide(null, remote(), null, BOTH)))
        assertNull(decide(null, remote(), null, UPLOAD))
    }

    @Test fun deletedOnPhoneRestoredInDownloadMode() {
        assertEquals(PlanKind.DOWNLOAD, kind(decide(null, remote(), state(), DOWNLOAD, propagate = true)))
    }

    @Test fun deletedOnPhonePropagatesOnlyWhenEnabled() {
        assertEquals(PlanKind.TRASH_REMOTE, kind(decide(null, remote(), state(), BOTH, propagate = true)))
        assertEquals(PlanKind.TRASH_REMOTE, kind(decide(null, remote(), state(), UPLOAD, propagate = true)))
    }

    @Test fun deletedOnPhoneWithoutPropagationLosesNothing() {
        assertEquals(PlanKind.DOWNLOAD, kind(decide(null, remote(), state(), BOTH)))
        assertEquals(PlanKind.DROP, kind(decide(null, remote(), state(), UPLOAD)))
    }

    // ---- beide vorhanden ----
    @Test fun unchangedBothSidesNeedNothingAndNoHashing() {
        for (dir in SyncDirection.entries) {
            assertNull(decide(local(), remote(), state(), dir))
            assertEquals(0, hashCalls)
        }
    }

    @Test fun firstComparisonWithEqualContentOnlyRecords() {
        for (dir in SyncDirection.entries) assertEquals(Decision(PlanKind.RECORD), decide(local(), remote(), null, dir, same = true))
    }

    @Test fun firstComparisonWithDifferentContent() {
        assertEquals(Decision(PlanKind.UPLOAD, trashOther = true), decide(local(), remote(), null, UPLOAD))
        assertEquals(Decision(PlanKind.DOWNLOAD, trashOther = true), decide(local(), remote(), null, DOWNLOAD))
        assertEquals(Decision(PlanKind.CONFLICT), decide(local(), remote(), null, BOTH))
    }

    @Test fun bothChangedNewerWinsInBothDirectionsMode() {
        val st = state()
        assertEquals(Decision(PlanKind.UPLOAD, true), decide(local(11, 300), remote(12, 200), st, BOTH, newer = true))
        assertEquals(Decision(PlanKind.UPLOAD, true), decide(local(11, 200), remote(12, 200), st, BOTH, newer = true)) // gleich: Handy
        assertEquals(Decision(PlanKind.DOWNLOAD, true), decide(local(11, 200), remote(12, 300), st, BOTH, newer = true))
    }

    @Test fun bothChangedAskingCreatesConflictOnlyInBothMode() {
        val st = state()
        assertEquals(Decision(PlanKind.CONFLICT), decide(local(11, 300), remote(12, 200), st, BOTH))
        assertEquals(Decision(PlanKind.UPLOAD, true), decide(local(11, 300), remote(12, 200), st, UPLOAD))
        assertEquals(Decision(PlanKind.DOWNLOAD, true), decide(local(11, 300), remote(12, 200), st, DOWNLOAD))
    }

    @Test fun oneSidedChangeFollowsDirection() {
        val st = state()
        val changedPhone = local(20, 300)
        assertEquals(PlanKind.UPLOAD, kind(decide(changedPhone, remote(), st, UPLOAD)))
        assertEquals(PlanKind.UPLOAD, kind(decide(changedPhone, remote(), st, BOTH)))
        assertNull(decide(changedPhone, remote(), st, DOWNLOAD))
        val changedPc = remote(20, 300)
        assertEquals(PlanKind.DOWNLOAD, kind(decide(local(), changedPc, st, DOWNLOAD)))
        assertEquals(PlanKind.DOWNLOAD, kind(decide(local(), changedPc, st, BOTH)))
        assertNull(decide(local(), changedPc, st, UPLOAD))
    }

    @Test fun keepVersionsMovesTheOverwrittenVersionToTrash() {
        val st = state()
        assertEquals(Decision(PlanKind.UPLOAD, true), decide(local(20, 300), remote(), st, BOTH, keep = true))
        assertEquals(Decision(PlanKind.DOWNLOAD, true), decide(local(), remote(20, 300), st, BOTH, keep = true))
        assertEquals(Decision(PlanKind.UPLOAD, false), decide(local(20, 300), remote(), st, BOTH, keep = false))
    }

    @Test fun timestampOnlyShiftWithSameSizeAndContentIsOnlyRecorded() {
        // Nur der Zeitstempel hat sich verschoben, gleiche Grösse und gleicher Inhalt: Stand merken statt übertragen
        assertEquals(PlanKind.RECORD, kind(decide(local(10, 999), remote(), state(), BOTH, same = true)))
        assertEquals(1, hashCalls)
    }

    @Test fun differentSizeNeverHashesInOneSidedCase() {
        decide(local(20, 300), remote(), state(), BOTH)
        assertEquals(0, hashCalls)
    }

    @Test fun oneWayProfileDoesNotHashWhenNothingWouldBeTransferred() {
        // Handy geändert, aber PC -> Handy: es würde nichts übertragen, also kein Hashen bei jedem Lauf
        assertNull(decide(local(10, 999), remote(), state(), DOWNLOAD, same = true))
        assertEquals(0, hashCalls)
    }

    // ---- Invarianten ----
    /** Ohne gespeicherten Stand wird nie gelöscht, sondern übertragen, gemerkt oder gefragt */
    @Test fun withoutStateNothingIsEverDeleted() {
        val sides = listOf(
            Triple(local(), null as RemoteEntry?, "nur Handy"),
            Triple(null as LocalEntry?, remote(), "nur PC"),
            Triple(local(11, 300), remote(12, 200), "beide"),
        )
        for ((l, r, label) in sides) for (dir in SyncDirection.entries)
            for (propagate in listOf(false, true)) for (newer in listOf(false, true))
                for (keep in listOf(false, true)) for (same in listOf(false, true)) {
                    val k = kind(decide(l, r, null, dir, propagate, newer, keep, same))
                    val msg = "$label dir=$dir propagate=$propagate newer=$newer keep=$keep same=$same -> $k"
                    assertFalse(msg, k == PlanKind.TRASH_LOCAL || k == PlanKind.TRASH_REMOTE)
                    assertFalse(msg, k == PlanKind.DROP)
                }
    }

    @Test fun trashActionsOnlyWithStateAndPropagation() {
        for (dir in SyncDirection.entries) for (propagate in listOf(false, true)) {
            for ((l, r) in listOf(local() to null, null to remote())) {
                val k = kind(decide(l, r as RemoteEntry?, state(), dir, propagate))
                if (k == PlanKind.TRASH_LOCAL || k == PlanKind.TRASH_REMOTE) assertEquals(true, propagate)
            }
        }
    }

    @Test fun deletionsNeverOnTheSideThatIsTheSourceOfTruth() {
        // Handy -> PC: gelöscht werden darf nur auf dem PC (TRASH_REMOTE), nie auf dem Handy
        assertNotNull(decide(null, remote(), state(), UPLOAD, propagate = true))
        assertEquals(PlanKind.TRASH_REMOTE, kind(decide(null, remote(), state(), UPLOAD, propagate = true)))
        assertEquals(PlanKind.UPLOAD, kind(decide(local(), null, state(), UPLOAD, propagate = true)))
        // PC -> Handy: gelöscht werden darf nur auf dem Handy
        assertEquals(PlanKind.TRASH_LOCAL, kind(decide(local(), null, state(), DOWNLOAD, propagate = true)))
        assertEquals(PlanKind.DOWNLOAD, kind(decide(null, remote(), state(), DOWNLOAD, propagate = true)))
    }
}