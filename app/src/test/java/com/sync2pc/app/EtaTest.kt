package com.sync2pc.app

import com.sync2pc.app.ui.ETA_MAX_SECONDS
import com.sync2pc.app.ui.ETA_MIN_BYTES
import com.sync2pc.app.ui.ETA_MIN_ELAPSED_MS
import com.sync2pc.app.ui.etaSeconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Restzeit-Schätzung nur bei belastbarer Rate (SYNC-20261109-063) */
class EtaTest {
    private val mb = 1024L * 1024

    @Test
    fun keineSchaetzungVorMindestdauer() {
        assertNull(etaSeconds(ETA_MIN_ELAPSED_MS - 1, 50 * mb, 340 * mb))
    }

    @Test
    fun keineSchaetzungOhneMindestmenge() {
        // Fall aus dem Emulatortest: 0 B (bzw. wenige Bytes) übertragen, 340 MB offen -> früher «noch ca. 411967 Std.»
        assertNull(etaSeconds(ETA_MIN_ELAPSED_MS, 0, 340 * mb))
        assertNull(etaSeconds(5 * ETA_MIN_ELAPSED_MS, 1, 340 * mb))
        assertNull(etaSeconds(5 * ETA_MIN_ELAPSED_MS, ETA_MIN_BYTES - 1, 340 * mb))
    }

    @Test
    fun keineSchaetzungWennFertigOderUeberschritten() {
        assertNull(etaSeconds(2 * ETA_MIN_ELAPSED_MS, 340 * mb, 340 * mb))
        assertNull(etaSeconds(2 * ETA_MIN_ELAPSED_MS, 400 * mb, 340 * mb))
    }

    @Test
    fun keineSchaetzungUeberObergrenze() {
        // 2 MB in 60 s bei 100 GB offen -> weit über 72 Stunden
        assertNull(etaSeconds(ETA_MIN_ELAPSED_MS, ETA_MIN_BYTES, 100L * 1024 * mb))
    }

    @Test
    fun schaetzungBeiBelastbarerRate() {
        // 50 MB in 60 s, 300 MB offen -> 360 s
        assertEquals(360L, etaSeconds(ETA_MIN_ELAPSED_MS, 50 * mb, 350 * mb))
        assertNotNull(etaSeconds(ETA_MIN_ELAPSED_MS, ETA_MIN_BYTES, 20 * mb))
    }

    @Test
    fun obergrenzeGenau() {
        // Rate 1 MB/s, genau 72 h offen -> noch erlaubt; eine Sekunde mehr nicht
        val perSec = mb
        val done = 100 * mb
        val elapsed = 100_000L // 100 s
        val rest = perSec * ETA_MAX_SECONDS
        assertEquals(ETA_MAX_SECONDS, etaSeconds(elapsed, done, done + rest))
        assertNull(etaSeconds(elapsed, done, done + rest + 2 * perSec))
    }
}
