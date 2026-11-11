package com.sync2pc.app

import androidx.work.NetworkType
import androidx.work.WorkInfo
import com.sync2pc.app.sync.SyncDecisions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** B2/B3/B6 (SYNC-20261111-163): reine Entscheidungen aus SyncWorker, WifiNetworkReceiver, SyncScheduler */
class SyncDecisionsTest {
    // ---- B3: nach Stopp fortsetzen? ----
    @Test fun resumeAfterSystemStop() {
        assertTrue(SyncDecisions.resumeAfterStop(true, WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY, manual = false))
        assertTrue(SyncDecisions.resumeAfterStop(true, WorkInfo.STOP_REASON_UNKNOWN, manual = false)) // API < 31
    }

    @Test fun userCancelIsNoInterruption() {
        assertFalse(SyncDecisions.resumeAfterStop(true, WorkInfo.STOP_REASON_CANCELLED_BY_APP, manual = false))
    }

    @Test fun manualRunIsNeverResumed() {
        assertFalse(SyncDecisions.resumeAfterStop(true, WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY, manual = true))
    }

    @Test fun notStoppedNeverResumes() {
        assertFalse(SyncDecisions.resumeAfterStop(false, WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY, manual = false))
    }

    @Test fun interruptTextPerReason() {
        assertNull(SyncDecisions.interruptText(false, WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY))
        assertNull(SyncDecisions.interruptText(true, WorkInfo.STOP_REASON_CANCELLED_BY_APP))
        assertEquals(SyncDecisions.InterruptText(R.string.stop_wifi), SyncDecisions.interruptText(true, WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY))
        assertEquals(SyncDecisions.InterruptText(R.string.stop_charger), SyncDecisions.interruptText(true, WorkInfo.STOP_REASON_CONSTRAINT_CHARGING))
        assertEquals(SyncDecisions.InterruptText(R.string.stop_timeout), SyncDecisions.interruptText(true, WorkInfo.STOP_REASON_TIMEOUT))
    }

    @Test fun systemReasonsCarryTheCode() {
        for (r in listOf(WorkInfo.STOP_REASON_QUOTA, WorkInfo.STOP_REASON_APP_STANDBY, WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION,
            WorkInfo.STOP_REASON_DEVICE_STATE, WorkInfo.STOP_REASON_SYSTEM_PROCESSING)) {
            assertEquals(SyncDecisions.InterruptText(R.string.stop_system, r), SyncDecisions.interruptText(true, r))
        }
    }

    @Test fun otherReasonsFallBackToAndroidText() {
        assertEquals(SyncDecisions.InterruptText(R.string.stop_android, 9999), SyncDecisions.interruptText(true, 9999))
    }

    // ---- B6: Drosselung WifiNetworkReceiver ----
    @Test fun wifiTriggerIsThrottledWithin60Seconds() {
        assertTrue(SyncDecisions.wifiTriggerThrottled(false, now = 100_000, lastTriggerAt = 100_000 - 59_999))
    }

    @Test fun wifiTriggerPassesAfter60Seconds() {
        assertFalse(SyncDecisions.wifiTriggerThrottled(false, now = 100_000, lastTriggerAt = 100_000 - 60_000))
        assertFalse(SyncDecisions.wifiTriggerThrottled(false, now = 100_000, lastTriggerAt = 0))
    }

    @Test fun wifiTriggerNeverThrottledWhenResuming() {
        assertFalse(SyncDecisions.wifiTriggerThrottled(true, now = 100_000, lastTriggerAt = 99_999))
    }

    // ---- B2: Auftragsbeschreibung automatischer Start ----
    @Test fun defaultAutoStartIsUnmeteredAndExpedited() {
        assertEquals(SyncDecisions.AutoSpec(NetworkType.UNMETERED, requiresCharging = false, expedited = true),
            SyncDecisions.autoSpec(syncOffWifi = false, onlyWhileCharging = false))
    }

    @Test fun offWifiSwitchAllowsAnyConnection() {
        assertEquals(NetworkType.CONNECTED, SyncDecisions.autoSpec(true, false).network)
    }

    @Test fun chargingRequirementDisablesExpedited() {
        // Expedited erlaubt nur Netzwerk-Bedingungen
        val spec = SyncDecisions.autoSpec(false, true)
        assertTrue(spec.requiresCharging)
        assertFalse(spec.expedited)
    }
}