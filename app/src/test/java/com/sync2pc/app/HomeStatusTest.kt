package com.sync2pc.app

import com.sync2pc.app.sync.PcCheckMode
import com.sync2pc.app.ui.DogState
import com.sync2pc.app.ui.HomeHeadline
import com.sync2pc.app.ui.HomeStatus
import com.sync2pc.app.ui.HomeStatusInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Statuskarte folgt dem Gesamtzustand (SYNC-20261110-113, A1/A2/A9) */
class HomeStatusTest {
    /** Alles in Ordnung: aktives Profil, nichts offen, verbunden, letzter Lauf ohne Probleme */
    private val good = HomeStatusInput(
        isRunning = false, hasProfiles = true, hasActiveProfile = true, attentionCount = 0, noConnection = false,
        userPaused = false, wifiPaused = false, lastRunHadErrors = false, allUpToDate = true,
    )

    @Test
    fun gutZustandZeigtAlleAktuell() {
        assertEquals(HomeHeadline.UP_TO_DATE, HomeStatus.headline(good))
    }

    @Test
    fun offenerKonfliktIstNieAktuell() {
        val h = HomeStatus.headline(good.copy(attentionCount = 1))
        assertEquals(HomeHeadline.ATTENTION, h)
        assertTrue(HomeStatus.showsWarning(h))
        assertEquals(DogState.ATTENTION, HomeStatus.dogFor(h, false))
    }

    @Test
    fun keineVerbindungIstNieAktuell() {
        val h = HomeStatus.headline(good.copy(noConnection = true))
        assertEquals(HomeHeadline.NO_CONNECTION, h)
        assertTrue(HomeStatus.showsWarning(h))
        assertEquals(DogState.WAITING, HomeStatus.dogFor(h, false))
    }

    @Test
    fun ohneProfilNieGesichert() {
        // auch wenn allUpToDate fälschlich gesetzt wäre (altes Profil, alter Lauf)
        val h = HomeStatus.headline(good.copy(hasProfiles = false, hasActiveProfile = false))
        assertEquals(HomeHeadline.NOT_CONFIGURED, h)
        assertFalse(HomeStatus.showsWarning(h))
        assertEquals(DogState.WAITING, HomeStatus.dogFor(h, false))
    }

    @Test
    fun nurDeaktivierteProfileNieGesichert() {
        assertEquals(HomeHeadline.NO_ACTIVE_PROFILE, HomeStatus.headline(good.copy(hasActiveProfile = false)))
    }

    @Test
    fun fehlerImLetztenLaufNieAktuell() {
        val h = HomeStatus.headline(good.copy(lastRunHadErrors = true, allUpToDate = false))
        assertEquals(HomeHeadline.ERRORS, h)
        assertTrue(HomeStatus.showsWarning(h))
    }

    @Test
    fun vorrangLaeuftVorAllem() {
        val all = good.copy(isRunning = true, attentionCount = 2, noConnection = true, hasProfiles = false)
        assertEquals(HomeHeadline.RUNNING, HomeStatus.headline(all))
        // Entscheidung vor Pause, Verbindung und Fehlern
        val rest = good.copy(attentionCount = 1, userPaused = true, noConnection = true, lastRunHadErrors = true)
        assertEquals(HomeHeadline.ATTENTION, HomeStatus.headline(rest))
        // Pause vor Verbindung
        assertEquals(HomeHeadline.PAUSED, HomeStatus.headline(good.copy(userPaused = true, noConnection = true)))
        assertEquals(HomeHeadline.PAUSED, HomeStatus.headline(good.copy(wifiPaused = true)))
        // Verbindung vor Fehlern
        assertEquals(HomeHeadline.NO_CONNECTION, HomeStatus.headline(good.copy(noConnection = true, lastRunHadErrors = true)))
    }

    @Test
    fun ausstehendeDateienSindWederAktuellNochFehler() {
        // SYNC-20261111-177: Rangfolge ... Fehler > ausstehend > gesichert
        val h = HomeStatus.headline(good.copy(pendingFiles = 2, allUpToDate = false))
        assertEquals(HomeHeadline.PENDING, h)
        assertFalse(HomeStatus.showsWarning(h))
        assertEquals(DogState.WAITING, HomeStatus.dogFor(h, false))
        // auch wenn allUpToDate fälschlich gesetzt wäre
        assertEquals(HomeHeadline.PENDING, HomeStatus.headline(good.copy(pendingFiles = 1)))
    }

    @Test
    fun ausstehendeDateienHabenNiedrigereRangAlsFehlerUndVerbindung() {
        assertEquals(HomeHeadline.ERRORS, HomeStatus.headline(good.copy(pendingFiles = 1, lastRunHadErrors = true)))
        assertEquals(HomeHeadline.NO_CONNECTION, HomeStatus.headline(good.copy(pendingFiles = 1, noConnection = true)))
        assertEquals(HomeHeadline.ATTENTION, HomeStatus.headline(good.copy(pendingFiles = 1, attentionCount = 1)))
        assertEquals(HomeHeadline.PAUSED, HomeStatus.headline(good.copy(pendingFiles = 1, userPaused = true)))
    }

    @Test
    fun fehlendeStandortBerechtigungIstNieGesichert() {
        // SYNC-20261111-176 R4: neutral statt «Alles gesichert und aktuell»
        assertEquals(HomeHeadline.IDLE, HomeStatus.headline(good.copy(locationMissing = true)))
        // Fehler und Verbindung behalten ihren Vorrang
        assertEquals(HomeHeadline.ERRORS, HomeStatus.headline(good.copy(locationMissing = true, lastRunHadErrors = true)))
    }

    @Test
    fun neutralWennNichtAktuellUndNichtsOffen() {
        val h = HomeStatus.headline(good.copy(allUpToDate = false))
        assertEquals(HomeHeadline.IDLE, h)
        assertFalse(HomeStatus.showsWarning(h))
    }

    @Test
    fun pauseZeigtPausenHund() {
        assertEquals(DogState.PAUSED, HomeStatus.dogFor(HomeHeadline.PAUSED, true))
        assertEquals(DogState.WAITING, HomeStatus.dogFor(HomeHeadline.PAUSED, false))
        assertEquals(DogState.SLEEPING, HomeStatus.dogFor(HomeHeadline.UP_TO_DATE, false))
    }

    @Test
    fun verbindungsErmittlung() {
        val wifi = listOf("Heim")
        // WIFI-Modus: Name muss in der Liste stehen
        assertTrue(HomeStatus.profileCanRunNow(PcCheckMode.WIFI, wifi, "Heim", true, false))
        assertFalse(HomeStatus.profileCanRunNow(PcCheckMode.WIFI, wifi, "Fremd", true, false))
        assertFalse(HomeStatus.profileCanRunNow(PcCheckMode.WIFI, wifi, null, false, false))
        // ohne WLAN-Liste (nur manuell) hängt nichts an der Verbindung
        assertTrue(HomeStatus.profileCanRunNow(PcCheckMode.WIFI, emptyList(), null, false, false))
        // GUID: jede erlaubte Netzart
        assertTrue(HomeStatus.profileCanRunNow(PcCheckMode.GUID, emptyList(), null, true, false))
        assertFalse(HomeStatus.profileCanRunNow(PcCheckMode.GUID, emptyList(), null, false, false))
        // mobile Daten/VPN mit Schalter
        assertTrue(HomeStatus.profileCanRunNow(PcCheckMode.WIFI, wifi, null, true, true))
    }

    @Test
    fun keineVerbindungNurMitAktivemProfil() {
        assertFalse(HomeStatus.noConnection(0, anyCanRunNow = false, pcUnreachable = true))
        assertTrue(HomeStatus.noConnection(1, anyCanRunNow = false, pcUnreachable = false))
        assertTrue(HomeStatus.noConnection(2, anyCanRunNow = true, pcUnreachable = true))
        assertFalse(HomeStatus.noConnection(2, anyCanRunNow = true, pcUnreachable = false))
    }
}
