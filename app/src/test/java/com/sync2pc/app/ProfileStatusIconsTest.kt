package com.sync2pc.app

import com.sync2pc.app.ui.ProfileIcon
import com.sync2pc.app.ui.ProfileStatusIcons
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Statussymbole der Profilkachel (SYNC-20261112-285): alle 8 Kombinationen WLAN/PC/Verschlüsselt, feste Reihenfolge
 * (WLAN, PC, Schild) und keine Lücke. Nicht abgedeckt: Grösse, Ausrichtung und Farben (nur am Gerät prüfbar).
 */
class ProfileStatusIconsTest {
    private val W = ProfileIcon.WIFI
    private val P = ProfileIcon.PC
    private val E = ProfileIcon.ENCRYPTED

    @Test fun none() = assertEquals(emptyList<ProfileIcon>(), ProfileStatusIcons.visible(false, false, false))
    @Test fun wifiOnly() = assertEquals(listOf(W), ProfileStatusIcons.visible(true, false, false))
    @Test fun pcOnly() = assertEquals(listOf(P), ProfileStatusIcons.visible(false, true, false))
    @Test fun encryptedOnly() = assertEquals(listOf(E), ProfileStatusIcons.visible(false, false, true))
    @Test fun wifiAndPc() = assertEquals(listOf(W, P), ProfileStatusIcons.visible(true, true, false))
    @Test fun wifiAndEncrypted() = assertEquals(listOf(W, E), ProfileStatusIcons.visible(true, false, true))
    @Test fun pcAndEncrypted() = assertEquals(listOf(P, E), ProfileStatusIcons.visible(false, true, true))
    @Test fun all() = assertEquals(listOf(W, P, E), ProfileStatusIcons.visible(true, true, true))

    @Test
    fun shieldIsAlwaysLastAndNoGapEntries() {
        for (w in listOf(false, true)) for (p in listOf(false, true)) for (e in listOf(false, true)) {
            val l = ProfileStatusIcons.visible(w, p, e)
            assertEquals(listOf(w, p, e).count { it }, l.size)
            if (e) assertEquals(E, l.last())
            assertEquals(l.sortedBy { it.ordinal }, l)
        }
    }
}
