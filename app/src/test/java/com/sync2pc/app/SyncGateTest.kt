package com.sync2pc.app

import com.sync2pc.app.sync.DeferredKind
import com.sync2pc.app.sync.DeferredState
import com.sync2pc.app.sync.DeferredStore
import com.sync2pc.app.sync.SyncGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sperre und Nachlauf (SYNC-20261111-143): T1 (zweiter Auftrag wird nicht verworfen, Nachlauf genau einmal) und T5
 * (Ausnahme im Aufräumen darf die Sperre nicht festhalten, R-BG4) aus SYNC-20261110-127.
 */
class SyncGateTest {

    private class FakeStore(var kind: DeferredKind? = null) : DeferredStore {
        var writes = 0
        override fun read(): DeferredKind? = kind
        override fun write(kind: DeferredKind?): Boolean { this.kind = kind; writes++; return true }
    }

    // ---- T1: zwei Aufträge gleichzeitig -------------------------------------------------------------------------

    @Test fun secondStartDuringRunIsDeferredNotDiscarded() {
        val store = FakeStore()
        val gate = SyncGate(store)
        assertTrue(gate.tryAcquireOrDefer(manual = false))
        assertFalse(gate.tryAcquireOrDefer(manual = false)) // zweiter Worker: keine Sperre
        assertEquals(DeferredKind.AUTO, store.kind)         // ... aber vorgemerkt, persistent
        assertEquals(DeferredState(requested = true, manual = false), gate.state.value)
        assertEquals(DeferredKind.AUTO, gate.release())     // Inhaber bekommt den Nachlauf
        assertEquals(DeferredState.NONE, gate.state.value)
        assertNull(store.kind)
    }

    @Test fun deferredRunHappensExactlyOnce() {
        val gate = SyncGate(FakeStore())
        gate.tryAcquireOrDefer(false)
        gate.tryAcquireOrDefer(false); gate.tryAcquireOrDefer(false); gate.requestStart(false) // mehrere Anforderungen
        assertEquals(DeferredKind.AUTO, gate.release())
        assertNull(gate.release())                      // kein zweiter Nachlauf
        assertTrue(gate.tryAcquireOrDefer(false))       // der Nachlauf selbst: frei, kein neuer Merker
        assertNull(gate.release())                      // keine Schleife
    }

    @Test fun manualStartIsKeptAsManualAndWinsOverAuto() {
        val store = FakeStore()
        val gate = SyncGate(store)
        gate.tryAcquireOrDefer(false)
        assertTrue(gate.requestStart(manual = false))
        assertTrue(gate.requestStart(manual = true))
        assertTrue(gate.requestStart(manual = false)) // ein späterer automatischer ändert die manuelle Art nicht
        assertEquals(DeferredState(requested = true, manual = true), gate.state.value)
        assertEquals(DeferredKind.MANUAL, store.kind)
        assertEquals(DeferredKind.MANUAL, gate.release())
    }

    @Test fun requestWhileIdleIsNotDeferredAndLeavesNoMarker() {
        val store = FakeStore()
        val gate = SyncGate(store)
        assertFalse(gate.requestStart(manual = true)) // Aufrufer reiht selbst ein
        assertEquals(DeferredState.NONE, gate.state.value)
        assertEquals(0, store.writes)
    }

    @Test fun releaseWithoutDeferredWritesNothing() {
        val store = FakeStore()
        val gate = SyncGate(store)
        gate.tryAcquireOrDefer(true)
        assertNull(gate.release())
        assertEquals(0, store.writes)
    }

    // ---- Prozess-Kill: persistenter Merker ----------------------------------------------------------------------

    @Test fun persistedMarkerIsClaimedOnceAtProcessStartWhenIdle() {
        val store = FakeStore(DeferredKind.MANUAL)
        val gate = SyncGate(store) // neuer Prozess liest den Merker
        assertEquals(DeferredState(requested = true, manual = true), gate.state.value)
        assertEquals(DeferredKind.MANUAL, gate.claimIfIdle())
        assertNull(gate.claimIfIdle())
        assertNull(store.kind)
    }

    @Test fun persistedMarkerStaysWhileRunAndGoesToTheLockOwner() {
        val gate = SyncGate(FakeStore(DeferredKind.AUTO))
        assertTrue(gate.tryAcquireOrDefer(false))
        assertNull(gate.claimIfIdle())                    // läuft: nicht doppelt einreihen
        assertEquals(DeferredKind.AUTO, gate.release())   // der Inhaber übernimmt
    }

    // ---- «Pausieren»/«Abbrechen» ----------------------------------------------------------------------------------

    @Test fun discardClearsMarkerAndStateAndStore() {
        val store = FakeStore()
        val gate = SyncGate(store)
        gate.tryAcquireOrDefer(false)
        gate.requestStart(true)
        gate.discardDeferred()
        assertEquals(DeferredState.NONE, gate.state.value)
        assertNull(store.kind)
        assertNull(gate.release()) // abgebrochener Lauf reiht nichts mehr ein
    }

    // ---- T5 / R-BG4: Ausnahme im Aufräumen -------------------------------------------------------------------------

    @Test fun exceptionInCleanupStillReleasesLockAndEnqueuesDeferred() {
        val gate = SyncGate(FakeStore())
        gate.tryAcquireOrDefer(false)
        gate.requestStart(manual = true)
        val followed = ArrayList<DeferredKind>()
        try {
            gate.finishRun(cleanup = { throw IllegalStateException("markInterrupted") }, followUp = { followed += it })
            fail("Ausnahme muss weitergereicht werden")
        } catch (e: IllegalStateException) {
            assertEquals("markInterrupted", e.message)
        }
        assertEquals(listOf(DeferredKind.MANUAL), followed)  // Nachlauf trotzdem eingereiht
        assertFalse(gate.isRunning)
        assertTrue(gate.tryAcquireOrDefer(false))            // nächster Sync wird nicht stumm verworfen
    }

    @Test fun lockIsReleasedBeforeFollowUpSoImmediateStartIsNotDiscarded() {
        val gate = SyncGate(FakeStore())
        gate.tryAcquireOrDefer(false)
        gate.requestStart(false)
        var startedInsideFollowUp = false
        gate.finishRun(cleanup = {}, followUp = { startedInsideFollowUp = gate.tryAcquireOrDefer(false) })
        assertTrue(startedInsideFollowUp) // sofort startender Folgeauftrag bekommt die Sperre (R-BG5)
    }

    @Test fun failingFollowUpRestoresMarkerForNextProcessStart() {
        val store = FakeStore()
        val gate = SyncGate(store)
        gate.tryAcquireOrDefer(false)
        gate.requestStart(manual = true)
        try {
            gate.finishRun(cleanup = {}, followUp = { throw RuntimeException("enqueue") })
            fail()
        } catch (e: RuntimeException) {
            assertEquals("enqueue", e.message)
        }
        assertFalse(gate.isRunning)
        assertEquals(DeferredKind.MANUAL, store.kind) // nicht verloren
        assertEquals(DeferredKind.MANUAL, gate.claimIfIdle())
    }

    @Test fun finishRunWithoutMarkerDoesNotEnqueue() {
        val gate = SyncGate(FakeStore())
        gate.tryAcquireOrDefer(false)
        var called = false
        gate.finishRun(cleanup = {}, followUp = { called = true })
        assertFalse(called)
    }

    @Test fun errorInStoreNeverBlocksTheGate() {
        val broken = object : DeferredStore {
            override fun read(): DeferredKind? = throw RuntimeException("read")
            override fun write(kind: DeferredKind?): Boolean = throw RuntimeException("write")
        }
        val gate = SyncGate(broken)
        assertTrue(gate.tryAcquireOrDefer(false))
        assertFalse(gate.tryAcquireOrDefer(true))
        assertEquals(DeferredKind.MANUAL, gate.release()) // Merker im Speicher bleibt gültig
        assertTrue(gate.tryAcquireOrDefer(false))
    }

    // ---- SYNC-20261111-172: Sonderaufträge und Merker-Schreibfehler ----------------------------------------------------

    @Test fun tryAcquireTakesFreeLockWithoutMarker() {
        val store = FakeStore()
        val gate = SyncGate(store)
        assertTrue(gate.tryAcquire())
        assertTrue(gate.isRunning)
        assertNull(gate.release())
        assertEquals(0, store.writes)
    }

    @Test fun tryAcquireOnBusyLockSetsNoMarker() {
        val store = FakeStore()
        val gate = SyncGate(store)
        assertTrue(gate.tryAcquireOrDefer(false))
        assertFalse(gate.tryAcquire())                 // Sonderauftrag: wartet per Result.retry(), kein Merker
        assertEquals(DeferredState.NONE, gate.state.value)
        assertEquals(0, store.writes)
        assertNull(gate.release())                     // kein Nachlauf entstanden
        assertTrue(gate.tryAcquire())                  // beim nächsten Versuch frei
    }

    @Test fun storeFailureIsReportedAndMarkerStaysInMemory() {
        val failing = object : DeferredStore {
            override fun read(): DeferredKind? = null
            override fun write(kind: DeferredKind?): Boolean = false
        }
        val reports = ArrayList<String>()
        val gate = SyncGate(failing, onStoreFailure = { reports += it })
        gate.tryAcquireOrDefer(false)
        assertFalse(gate.tryAcquireOrDefer(true))
        assertEquals(1, reports.size)
        assertEquals(DeferredState(requested = true, manual = true), gate.state.value) // Merker im Speicher bleibt
        assertEquals(DeferredKind.MANUAL, gate.release())
        assertEquals(2, reports.size)                  // auch das Löschen wird gemeldet
    }

    @Test fun storeExceptionIsReportedToo() {
        val throwing = object : DeferredStore {
            override fun read(): DeferredKind? = null
            override fun write(kind: DeferredKind?): Boolean = throw RuntimeException("commit")
        }
        val reports = ArrayList<String>()
        val gate = SyncGate(throwing, onStoreFailure = { reports += it })
        gate.tryAcquireOrDefer(false)
        gate.tryAcquireOrDefer(false)
        assertEquals(1, reports.size)
        assertEquals(DeferredKind.AUTO, gate.release())
    }

    // ---- Nebenläufigkeit ------------------------------------------------------------------------------------------

    @Test fun onlyOneOfManyConcurrentStartsGetsTheLockAndTheRestIsDeferred() {
        val n = 16
        val gate = SyncGate(FakeStore())
        val pool = Executors.newFixedThreadPool(n)
        val ready = CountDownLatch(n)
        val go = CountDownLatch(1)
        val winners = AtomicInteger()
        try {
            repeat(n) { i ->
                pool.execute {
                    ready.countDown(); go.await()
                    if (gate.tryAcquireOrDefer(manual = i == 0)) winners.incrementAndGet()
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            go.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))
        } finally { pool.shutdownNow() }
        assertEquals(1, winners.get())
        val kind = gate.release()
        // Merker ist gesetzt; MANUAL nur, wenn Thread 0 verloren hat (sonst war er der Inhaber)
        assertTrue(kind == DeferredKind.AUTO || kind == DeferredKind.MANUAL)
        assertNull(gate.release())
    }

    @Test fun noRequestIsLostBetweenCheckAndRelease() {
        // Anforderer und Freigeber wechseln sich ab: jede Anforderung wird entweder als Merker mitgenommen
        // (requestStart = true) oder der Aufrufer darf selbst einreihen (false). Nie beides, nie keines.
        val gate = SyncGate(FakeStore())
        val rounds = 2_000
        val requested = AtomicInteger()   // als Merker angenommen
        val enqueuedBySelf = AtomicInteger()
        val taken = AtomicInteger()       // vom Inhaber mitgenommene Merker (Summe der release()-Ergebnisse)
        val pool = Executors.newFixedThreadPool(2)
        val done = CountDownLatch(2)
        pool.execute {
            repeat(rounds) {
                gate.tryAcquireOrDefer(false)
                if (gate.release() != null) taken.incrementAndGet()
            }
            done.countDown()
        }
        pool.execute {
            repeat(rounds) {
                if (gate.requestStart(false)) requested.incrementAndGet() else enqueuedBySelf.incrementAndGet()
            }
            done.countDown()
        }
        assertTrue(done.await(20, TimeUnit.SECONDS))
        pool.shutdownNow()
        assertEquals(rounds, requested.get() + enqueuedBySelf.get())
        // Rest-Merker (nach der letzten Freigabe angelegt) einsammeln: Der Inhaber-Thread endet frei
        val rest = if (gate.claimIfIdle() != null) 1 else 0
        // Mehrere Anforderungen verschmelzen zu einem Merker: mindestens ein Merker, wenn überhaupt vorgemerkt wurde
        if (requested.get() > 0) assertTrue(taken.get() + rest >= 1)
        assertFalse(gate.isRunning)
    }
}
