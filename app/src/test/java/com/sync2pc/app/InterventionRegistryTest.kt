package com.sync2pc.app

import com.sync2pc.app.mirror.InterventionRegistry
import com.sync2pc.app.mirror.InterventionState
import com.sync2pc.app.mirror.InterventionTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zustandslogik der Eingriffe beim laufenden Sync (0.11.10, DEC-144, QA -035) */
class InterventionRegistryTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var busy = true
    private val reg = InterventionRegistry(scope, { busy }) { "Fehler: ${it.message}" }

    @After fun tearDown() { scope.cancel() }

    private fun task(id: Long = 1) = reg.tasks.value[id]

    private fun await(id: Long = 1, cond: (InterventionTask?) -> Boolean) = runBlocking {
        withTimeout(5_000) { reg.tasks.first { cond(it[id]) } }
    }

    @Test fun waitingWhileSyncRunsThenRunningAndDone() {
        val gate = CompletableDeferred<Unit>()
        val notified = ArrayList<String>()
        reg.viewerOpened(1)
        assertTrue(reg.start(1, { notified += it; true }) { onStart, _ -> gate.await(); onStart(); "fertig" })
        assertEquals(InterventionState.WAITING, task()?.state)
        gate.complete(Unit)
        await { it?.state == InterventionState.DONE }
        assertEquals("fertig", task()?.result)
        assertTrue(notified.isEmpty()) // Ansicht offen: keine Benachrichtigung
        reg.dismiss(1)
        assertNull(task())
    }

    @Test fun onlyOneInterventionPerProfile() {
        val gate = CompletableDeferred<Unit>()
        assertTrue(reg.start(1, { true }) { onStart, _ -> gate.await(); onStart(); "a" })
        assertFalse(reg.start(1, { true }) { _, _ -> "b" })
        assertTrue(reg.start(2, { true }) { onStart, _ -> gate.await(); onStart(); "c" }) // anderes Profil: erlaubt
        gate.complete(Unit)
    }

    @Test fun cancelWhileWaitingNeverRunsTheWork() {
        val gate = CompletableDeferred<Unit>()
        var ran = false
        reg.start(1, { true }) { onStart, _ -> gate.await(); onStart(); ran = true; "x" }
        reg.cancel(1)
        assertNull(task())
        gate.complete(Unit)
        Thread.sleep(200)
        assertFalse(ran)
        assertNull(task())
        // Nach "Abbrechen" ist sofort ein neuer Eingriff möglich, und der alte kann ihn nicht mehr stören
        val gate2 = CompletableDeferred<Unit>()
        assertTrue(reg.start(1, { true }) { onStart, _ -> gate2.await(); onStart(); "neu" })
        assertEquals(InterventionState.WAITING, task()?.state)
        gate2.complete(Unit)
        await { it == null } // ohne Ansicht: zugestellt, Zustand entfernt
    }

    @Test fun cancelIsIgnoredOnceRunning() {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        reg.viewerOpened(1)
        reg.start(1, { true }) { onStart, _ -> onStart(); started.complete(Unit); finish.await(); "ok" }
        runBlocking { withTimeout(5_000) { started.await() } }
        assertEquals(InterventionState.RUNNING, task()?.state)
        reg.cancel(1)
        assertEquals(InterventionState.RUNNING, task()?.state)
        finish.complete(Unit)
        await { it?.state == InterventionState.DONE }
        assertEquals("ok", task()?.result)
    }

    @Test fun resultIsNotifiedWhenNoViewerIsOpen() {
        busy = false
        val notified = ArrayList<String>()
        reg.start(1, { notified += it; true }) { onStart, _ -> onStart(); "Ergebnis" }
        await { it == null }
        assertEquals(listOf("Ergebnis"), notified)
    }

    @Test fun undeliverableResultStaysUntilViewIsOpened() {
        busy = false
        reg.start(1, { false }) { onStart, _ -> onStart(); "Ergebnis" }
        await { it?.state == InterventionState.DONE }
        assertEquals("Ergebnis", task()?.result)
        // Neuer Eingriff ist trotzdem möglich (DONE ist nicht aktiv)
        assertFalse(reg.isActive(1))
    }

    @Test fun failureProducesText() {
        busy = false
        reg.viewerOpened(1)
        reg.start(1, { true }) { onStart, _ -> onStart(); error("kaputt") }
        await { it?.state == InterventionState.DONE }
        assertEquals("Fehler: kaputt", task()?.result)
    }

    @Test fun closingLastViewDismissesSeenResultAndCounterStaysNonNegative() {
        busy = false
        reg.viewerOpened(1)
        reg.start(1, { true }) { onStart, _ -> onStart(); "x" }
        await { it?.state == InterventionState.DONE }
        reg.viewerClosed(1)
        assertNull(task())
        reg.viewerClosed(1) // zu oft geschlossen: darf nichts verfälschen
        reg.viewerOpened(1)
        reg.start(1, { true }) { onStart, _ -> onStart(); "y" }
        await { it?.state == InterventionState.DONE } // Ansicht gilt als offen
    }
}
