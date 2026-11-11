package com.sync2pc.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.workDataOf
import androidx.work.testing.TestListenableWorkerBuilder
import com.sync2pc.app.sync.DeferredState
import com.sync2pc.app.sync.SyncScheduler
import com.sync2pc.app.sync.SyncWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verdrahtung der Sync-Sperre im Worker (SYNC-20261111-163, B1, teilweise): Bekommt `SyncWorker.doWork` die Sperre nicht,
 * macht er keine Arbeit und verhält sich je nach Auftragsart wie festgelegt (R-N1/R-BG4). Läuft mit
 * `gradlew.bat connectedDebugAndroidTest` auf Emulator/Gerät (QA); nicht auf der JVM ausgeführt.
 *
 * Der Fall «Ausnahme in `work()` gibt die Sperre frei, Ursache bleibt erhalten» ist hier NICHT abgedeckt: er braucht eine
 * Testnaht in `SyncWorker.work()` (sonst liefe die echte Engine). Die Sperre selbst (`SyncGate`) ist auf der JVM getestet.
 * Die Tests nutzen die echte Prozess-Sperre; läuft gerade ein echter Sync, werden sie übersprungen.
 */
@RunWith(AndroidJUnit4::class)
class SyncWorkerGateTest {

    private lateinit var ctx: Context
    private var acquired = false
    private val gate get() = SyncScheduler.gate(ctx)

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        // Läuft schon ein echter Sync oder ist ein echter Nachlauf vorgemerkt: nichts anfassen, Test überspringen
        // (Review -163: weder fremde Sperre freigeben noch fremden Merker verwerfen)
        assumeTrue("Es läuft bereits ein Sync oder ein Nachlauf ist vorgemerkt: Test übersprungen",
            !gate.isRunning && !gate.state.value.requested)
        assumeTrue("Sperre nicht erhalten: Test übersprungen", gate.tryAcquire())
        acquired = true
    }

    @After
    fun tearDown() {
        // Nur die selbst belegte Sperre freigeben und den von den Tests gesetzten Merker nicht stehen lassen
        // (release() liefert ihn, reiht nichts ein)
        if (acquired) gate.release()
    }

    private fun run(vararg data: Pair<String, Any?>): Result = runBlocking {
        TestListenableWorkerBuilder<SyncWorker>(ctx).setInputData(workDataOf(*data)).build().doWork()
    }

    @Test
    fun busyGateAutoRunReturnsSuccessWithDeferredMarkerAndDoesNoWork() {
        val r = run()
        assertTrue("Ergebnis muss success sein, war $r", r is Result.Success)
        assertTrue((r as Result.Success).outputData.getBoolean(SyncScheduler.KEY_DEFERRED, false))
        assertEquals(DeferredState(requested = true, manual = false), gate.state.value)
        assertTrue("die Sperre des (simulierten) Laufs bleibt belegt", gate.isRunning)
    }

    @Test
    fun busyGateManualRunIsRecordedAsManual() {
        val r = run(SyncScheduler.KEY_MANUAL to true)
        assertTrue(r is Result.Success)
        assertEquals(DeferredState(requested = true, manual = true), gate.state.value)
    }

    @Test
    fun busyGateSpecialRunsAskForRetryAndRecordNoMarker() {
        for (key in listOf(SyncScheduler.KEY_RESUME, SyncScheduler.KEY_RETRY, SyncScheduler.KEY_SKIP_FOLLOWUP)) {
            val r = run(key to true)
            assertTrue("$key: Ergebnis muss retry sein, war $r", r is Result.Retry)
            assertFalse("$key: kein Merker", gate.state.value.requested)
        }
    }
}
