package com.sync2pc.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.sync.SyncScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Content-Trigger für neue Fotos (SYNC-20261111-207, B5; Bezug 0.16.9/-204): Ein Lauf stellt den Auslöser nur scharf, wenn
 * keiner wartet (KEEP); ein wartender behält seine Frist. Änderungen der Einstellungen ersetzen ihn (REPLACE).
 *
 * Läuft mit `gradlew.bat connectedDebugAndroidTest` auf Emulator/Testgerät (QA), nicht auf der JVM. Die Tests benutzen den
 * Test-WorkManager (eigene, leere Datenbank, berührt keine echten Aufträge) und stellen die geänderten Einstellungen
 * (`syncOnNewMedia`, `manualOnly`) im Anschluss wieder her.
 */
@RunWith(AndroidJUnit4::class)
class ContentTriggerArmTest {

    private lateinit var ctx: Context
    private lateinit var wm: WorkManager
    private lateinit var settings: GlobalSettings
    private var oldOnNewMedia = true
    private var oldManualOnly = false

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
        wm = WorkManager.getInstance(ctx)
        settings = GlobalSettings(ctx)
        oldOnNewMedia = settings.syncOnNewMedia
        oldManualOnly = settings.manualOnly
        settings.syncOnNewMedia = true
        settings.manualOnly = false
    }

    @After
    fun tearDown() {
        settings.syncOnNewMedia = oldOnNewMedia
        settings.manualOnly = oldManualOnly
    }

    private fun contentWork(): List<WorkInfo> = wm.getWorkInfosForUniqueWork(SyncScheduler.WORK_CONTENT).get()
    private fun waiting(): List<WorkInfo> = contentWork().filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun firstArmEnqueuesOneTrigger() {
        SyncScheduler.armContentTrigger(ctx, replace = false)
        assertEquals(1, waiting().size)
    }

    @Test
    fun keepLeavesWaitingTriggerUntouched() {
        SyncScheduler.armContentTrigger(ctx, replace = false)
        val id = waiting().single().id
        // Ein Lauf ohne neue Dateien (SyncWorker.doWork, finally) darf die Frist nicht neu beginnen lassen
        SyncScheduler.armContentTrigger(ctx, replace = false)
        SyncScheduler.armContentTrigger(ctx, replace = false)
        assertEquals(id, waiting().single().id)
    }

    @Test
    fun replaceStartsFreshTrigger() {
        SyncScheduler.armContentTrigger(ctx, replace = false)
        val id = waiting().single().id
        SyncScheduler.armContentTrigger(ctx, replace = true) // Einstellungsänderung
        val now = waiting().single()
        assertNotEquals(id, now.id)
        assertTrue(contentWork().any { it.id == id && it.state == WorkInfo.State.CANCELLED })
    }

    @Test
    fun disabledOptionCancelsTrigger() {
        SyncScheduler.armContentTrigger(ctx, replace = false)
        settings.syncOnNewMedia = false
        SyncScheduler.armContentTrigger(ctx, replace = false)
        assertTrue(waiting().isEmpty())
    }

    @Test
    fun manualOnlyCancelsTrigger() {
        SyncScheduler.armContentTrigger(ctx, replace = false)
        settings.manualOnly = true
        SyncScheduler.armContentTrigger(ctx, replace = false)
        assertTrue(waiting().isEmpty())
    }
}
