package com.sync2pc.app

import com.sync2pc.app.sync.NetworkRules
import com.sync2pc.app.sync.SyncScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Netzwerk-Entscheidungen (DEC-126, 0.11.7): Schalter AUS/EIN × WLAN/Mobil/kein Netz × SSID */
class NetworkRulesTest {

    @Test fun onAllowedNetworkTable() {
        // (isWifi, isMobileOrVpn, schalter) -> erwartet
        val cases = listOf(
            Triple(true, false, false) to true,   // WLAN, Schalter aus
            Triple(true, false, true) to true,    // WLAN, Schalter an
            Triple(false, true, false) to false,  // Mobil, Schalter aus (Verhalten wie vor 0.11.5)
            Triple(false, true, true) to true,    // Mobil, Schalter an
            Triple(false, false, false) to false, // kein Netz
            Triple(false, false, true) to false,  // kein Netz, Schalter an
        )
        for ((c, expected) in cases) {
            assertEquals("$c", expected, NetworkRules.onAllowedNetwork(c.first, c.second, c.third))
        }
    }

    @Test fun offWifiNeedsSwitchAndMobile() {
        assertTrue(NetworkRules.offWifiSyncActive(isMobileOrVpn = true, switchOn = true))
        assertFalse(NetworkRules.offWifiSyncActive(isMobileOrVpn = true, switchOn = false))
        assertFalse(NetworkRules.offWifiSyncActive(isMobileOrVpn = false, switchOn = true))
        assertFalse(NetworkRules.offWifiSyncActive(isMobileOrVpn = false, switchOn = false))
    }

    @Test fun mayContinueBySsidOrNetwork() {
        val ssids = listOf("Heim", "Heim 5G")
        assertTrue(NetworkRules.mayContinue("Heim", ssids, onAllowedNetwork = false)) // SSID passt, Netz egal
        assertFalse(NetworkRules.mayContinue("Fremd", ssids, onAllowedNetwork = true)) // fremdes WLAN gewinnt immer
        assertTrue(NetworkRules.mayContinue(null, ssids, onAllowedNetwork = true))     // Mobil mit Schalter / unlesbar im WLAN
        assertFalse(NetworkRules.mayContinue(null, ssids, onAllowedNetwork = false))   // kein erlaubtes Netz
        assertFalse(NetworkRules.mayContinue("Heim", emptyList(), onAllowedNetwork = true))
    }

    @Test fun retryMinutes() {
        assertEquals(120L, NetworkRules.retryMinutes(offWifi = true, midRun = false))
        assertEquals(120L, NetworkRules.retryMinutes(offWifi = true, midRun = true)) // auch nach Abbruch mitten im Lauf
        assertEquals(2L, NetworkRules.retryMinutes(offWifi = false, midRun = true))
        assertEquals(SyncScheduler.SHARE_RETRY_MINUTES, NetworkRules.retryMinutes(offWifi = false, midRun = false))
        assertEquals(30L, SyncScheduler.SHARE_RETRY_MINUTES)
    }

    @Test fun reachabilityWaits() {
        assertEquals(listOf(0L, 3_000L), NetworkRules.reachabilityWaits(manual = true, offWifi = false))
        assertEquals(listOf(0L, 3_000L), NetworkRules.reachabilityWaits(manual = true, offWifi = true)) // manuell unverändert
        assertEquals(listOf(0L, 5_000L), NetworkRules.reachabilityWaits(manual = false, offWifi = true))
        assertEquals(listOf(0L, 5_000L, 10_000L, 15_000L, 30_000L), NetworkRules.reachabilityWaits(manual = false, offWifi = false))
    }
}
