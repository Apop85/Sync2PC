package com.sync2pc.app

import com.sync2pc.app.sync.PcAccess
import com.sync2pc.app.sync.PcCheckMode
import com.sync2pc.app.sync.PcDecision
import com.sync2pc.app.sync.PcEditorRules
import com.sync2pc.app.sync.PcIdentity
import com.sync2pc.app.sync.PcIdentityException
import com.sync2pc.app.sync.friendlyError
import com.sync2pc.app.sync.isNetworkError
import com.sync2pc.app.sync.userErrorMsg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** PC-Erkennung per Kennung (SYNC-20261109-047): reine Regel, Tabelle über Modus x Zugriffsart x Zustand */
class PcIdentityTest {

    private val a = "11111111-2222-3333-4444-555555555555"
    private val b = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val nullUuid = "00000000-0000-0000-0000-000000000000"
    private val host = "192.168.1.20"

    private fun decide(mode: PcCheckMode, access: PcAccess, stored: String, storedHost: String, seen: String?) =
        PcIdentity.decide(mode, stored, storedHost, host, seen, access)

    // ---- Normalisierung --------------------------------------------------------------

    @Test fun normalizeLowercasesAndRejectsInvalid() {
        assertEquals(a, PcIdentity.normalize(a.uppercase()))
        assertEquals(a, PcIdentity.normalize("  {" + a.uppercase() + "} "))
        assertNull(PcIdentity.normalize(null as String?))
        assertNull(PcIdentity.normalize(""))
        assertNull(PcIdentity.normalize("   "))
        assertNull(PcIdentity.normalize(nullUuid))
        assertNull(PcIdentity.normalize(java.util.UUID(0L, 0L)))
        assertEquals(a, PcIdentity.normalize(java.util.UUID.fromString(a.uppercase())))
    }

    @Test fun parseFallsBackToWifi() {
        assertEquals(PcCheckMode.WIFI, PcCheckMode.parse(null))
        assertEquals(PcCheckMode.WIFI, PcCheckMode.parse(""))
        assertEquals(PcCheckMode.WIFI, PcCheckMode.parse("irgendwas"))
        assertEquals(PcCheckMode.GUID, PcCheckMode.parse("GUID"))
        assertEquals(PcCheckMode.BOTH, PcCheckMode.parse("both"))
        assertEquals(PcCheckMode.WIFI, PcCheckMode.parse("WIFI"))
    }

    @Test fun storedOnlyValidForSameHostCaseInsensitive() {
        assertEquals(a, PcIdentity.storedFor(a, "192.168.1.20", host))
        assertEquals(a, PcIdentity.storedFor(a.uppercase(), "MYPC", "mypc"))
        assertNull(PcIdentity.storedFor(a, "192.168.1.21", host))
        assertNull(PcIdentity.storedFor(a, "", host))
        assertNull(PcIdentity.storedFor("", host, host))
        assertNull(PcIdentity.storedFor(nullUuid, host, host))
    }

    // ---- WIFI: nie blockieren ---------------------------------------------------------

    @Test fun wifiNeverBlocks() {
        for (access in PcAccess.values()) {
            // nichts gespeichert, gültige Kennung: still lernen
            assertEquals(PcDecision.ALLOW_AND_LEARN, decide(PcCheckMode.WIFI, access, "", "", a))
            // anderer Host gespeichert: für diesen Host nichts bekannt -> lernen (DAO ändert nur bei leerer Kennung)
            assertEquals(PcDecision.ALLOW_AND_LEARN, decide(PcCheckMode.WIFI, access, a, "other", b))
            // gleich und anders: keine Blockade, nichts zu lernen
            assertEquals(PcDecision.ALLOW, decide(PcCheckMode.WIFI, access, a, host, a))
            assertEquals(PcDecision.ALLOW, decide(PcCheckMode.WIFI, access, a, host, b))
            // keine brauchbare Kennung: einfach anmelden wie bisher
            assertEquals(PcDecision.ALLOW, decide(PcCheckMode.WIFI, access, "", "", null))
            assertEquals(PcDecision.ALLOW, decide(PcCheckMode.WIFI, access, "", "", ""))
            assertEquals(PcDecision.ALLOW, decide(PcCheckMode.WIFI, access, a, host, nullUuid))
        }
    }

    // ---- GUID / BOTH --------------------------------------------------------------------

    @Test fun guidAndBothNoIdNeverAuthenticates() {
        for (mode in listOf(PcCheckMode.GUID, PcCheckMode.BOTH)) for (access in PcAccess.values()) {
            for (seen in listOf(null, "", "  ", nullUuid)) {
                // unabhängig davon, was gespeichert ist
                assertEquals(PcDecision.NO_ID, decide(mode, access, "", "", seen))
                assertEquals(PcDecision.NO_ID, decide(mode, access, a, host, seen))
                assertEquals(PcDecision.NO_ID, decide(mode, access, a, "other", seen))
            }
        }
    }

    /** Ohne gespeicherte Kennung für den Host (V1/V2): nur SETUP mit Netzbedingung lernt; USER/AUTO nie */
    @Test fun guidAndBothWithoutStoredId() {
        val unknown = listOf(
            Triple("", "", "leer"), Triple(a, "", "ohne Host"), Triple(a, "192.168.1.99", "anderer Host"),
            Triple(nullUuid, host, "Null-UUID"),
        )
        for (mode in listOf(PcCheckMode.GUID, PcCheckMode.BOTH)) for ((g, h, what) in unknown) {
            // USER und AUTO: nie anmelden, unabhängig von der Netzbedingung
            for (access in listOf(PcAccess.USER, PcAccess.AUTO)) for (net in listOf(true, false))
                assertEquals("$mode $access $what net=$net", PcDecision.UNKNOWN,
                    PcIdentity.decide(mode, g, h, host, a, access, net))
            // SETUP: nur mit Netzbedingung, sonst WRONG_NET (nicht anmelden)
            assertEquals("$mode SETUP $what", PcDecision.ALLOW_AND_LEARN, PcIdentity.decide(mode, g, h, host, a, PcAccess.SETUP, true))
            assertEquals("$mode SETUP kein Netz $what", PcDecision.WRONG_NET, PcIdentity.decide(mode, g, h, host, a, PcAccess.SETUP, false))
        }
    }

    @Test fun learnNetConditionDefaultsToTrueAndWrongNetNeverAuthenticates() {
        assertEquals(PcDecision.ALLOW_AND_LEARN, decide(PcCheckMode.GUID, PcAccess.SETUP, "", "", a))
        assertFalse(PcDecision.WRONG_NET.mayAuthenticate)
        // mit bekannter Kennung spielt die Netzbedingung keine Rolle
        for (net in listOf(true, false)) {
            assertEquals(PcDecision.ALLOW, PcIdentity.decide(PcCheckMode.BOTH, a, host, host, a, PcAccess.SETUP, net))
            assertEquals(PcDecision.CHANGED, PcIdentity.decide(PcCheckMode.BOTH, a, host, host, b, PcAccess.SETUP, net))
        }
    }

    // ---- V7: WIFI-Kennung erzeugt nie Vertrauen ---------------------------------------------

    @Test fun wifiLearnedIdNeverCountsAsTrustForGuidModes() {
        for (eff in listOf(PcCheckMode.GUID, PcCheckMode.BOTH)) {
            assertEquals("" to "", PcIdentity.trustedStored(PcCheckMode.WIFI, eff, a, host))
            // Datenbank-Modus GUID/BOTH: gelernte Kennung bleibt
            for (db in listOf(PcCheckMode.GUID, PcCheckMode.BOTH)) assertEquals(a to host, PcIdentity.trustedStored(db, eff, a, host))
        }
        assertEquals(a to host, PcIdentity.trustedStored(PcCheckMode.WIFI, PcCheckMode.WIFI, a, host))
        // wirksamer Modus: der strengere
        assertEquals(PcCheckMode.GUID, PcIdentity.effectiveMode(PcCheckMode.WIFI, PcCheckMode.GUID))
        assertEquals(PcCheckMode.BOTH, PcIdentity.effectiveMode(PcCheckMode.BOTH, PcCheckMode.WIFI))
        assertEquals(PcCheckMode.WIFI, PcIdentity.effectiveMode(PcCheckMode.WIFI, PcCheckMode.WIFI))
        // zusammen mit decide: WIFI-Kennung + Moduswechsel auf GUID heisst unbekannt
        val (g, h) = PcIdentity.trustedStored(PcCheckMode.WIFI, PcCheckMode.GUID, a, host)
        assertEquals(PcDecision.UNKNOWN, PcIdentity.decide(PcCheckMode.GUID, g, h, host, a, PcAccess.AUTO))
    }

    // ---- Profil-Editor: reine Entscheidungen (T3, V5-V7) -----------------------------------

    @Test fun keepStoredOnSave() {
        val g = PcCheckMode.GUID
        assertTrue(PcEditorRules.keepStoredOnSave(g, g, host, host))
        assertTrue(PcEditorRules.keepStoredOnSave(PcCheckMode.BOTH, g, host.uppercase(), host))
        assertFalse("Hostwechsel verwirft", PcEditorRules.keepStoredOnSave(g, g, "192.168.1.99", host))
        assertFalse("kein Host", PcEditorRules.keepStoredOnSave(g, g, host, ""))
        assertFalse("nichts gespeichert", PcEditorRules.keepStoredOnSave(g, g, "", host))
        assertFalse("neues Profil", PcEditorRules.keepStoredOnSave(null, g, null, host))
        // V7: WIFI in der Datenbank, neu GUID/BOTH: verwerfen
        assertFalse(PcEditorRules.keepStoredOnSave(PcCheckMode.WIFI, g, host, host))
        assertFalse(PcEditorRules.keepStoredOnSave(PcCheckMode.WIFI, PcCheckMode.BOTH, host, host))
        // WIFI bleibt WIFI: behalten (heutiges Verhalten)
        assertTrue(PcEditorRules.keepStoredOnSave(PcCheckMode.WIFI, PcCheckMode.WIFI, host, host))
        // GUID -> WIFI -> behalten (verringert nichts)
        assertTrue(PcEditorRules.keepStoredOnSave(g, PcCheckMode.WIFI, host, host))
    }

    @Test fun clearOnModeChangeOnlyFromWifiToGuidModes() {
        assertTrue(PcEditorRules.clearOnModeChange(PcCheckMode.WIFI, PcCheckMode.GUID))
        assertTrue(PcEditorRules.clearOnModeChange(PcCheckMode.WIFI, PcCheckMode.BOTH))
        assertFalse(PcEditorRules.clearOnModeChange(PcCheckMode.GUID, PcCheckMode.BOTH))
        assertFalse(PcEditorRules.clearOnModeChange(PcCheckMode.BOTH, PcCheckMode.GUID))
        assertFalse(PcEditorRules.clearOnModeChange(PcCheckMode.GUID, PcCheckMode.WIFI))
        assertFalse(PcEditorRules.clearOnModeChange(PcCheckMode.WIFI, PcCheckMode.WIFI))
    }

    @Test fun learnAfterSaveDecision() {
        for (m in listOf(PcCheckMode.GUID, PcCheckMode.BOTH)) {
            assertTrue(PcEditorRules.learnAfterSave(m, host, true, "", ""))
            assertTrue("andere Host-Kennung", PcEditorRules.learnAfterSave(m, host, true, a, "192.168.1.99"))
            assertTrue("Null-UUID zählt als leer", PcEditorRules.learnAfterSave(m, host, true, nullUuid, host))
            assertFalse("bereits bekannt", PcEditorRules.learnAfterSave(m, host, true, a, host))
            assertFalse("kein Host", PcEditorRules.learnAfterSave(m, "", true, "", ""))
            assertFalse("kein Benutzer", PcEditorRules.learnAfterSave(m, host, false, "", ""))
        }
        assertFalse("WIFI lernt nicht nach dem Speichern", PcEditorRules.learnAfterSave(PcCheckMode.WIFI, host, true, "", ""))
    }

    @Test fun guidAndBothWithStoredId() {
        for (mode in listOf(PcCheckMode.GUID, PcCheckMode.BOTH)) for (access in PcAccess.values()) {
            assertEquals(PcDecision.ALLOW, decide(mode, access, a, host, a))
            assertEquals(PcDecision.ALLOW, decide(mode, access, a, host.uppercase(), a.uppercase()))
            assertEquals(PcDecision.CHANGED, decide(mode, access, a, host, b))
        }
    }

    @Test fun onlyAllowDecisionsMayAuthenticate() {
        assertTrue(PcDecision.ALLOW.mayAuthenticate)
        assertTrue(PcDecision.ALLOW_AND_LEARN.mayAuthenticate)
        assertFalse(PcDecision.UNKNOWN.mayAuthenticate)
        assertFalse(PcDecision.CHANGED.mayAuthenticate)
        assertFalse(PcDecision.NO_ID.mayAuthenticate)
        assertFalse(PcDecision.WRONG_NET.mayAuthenticate)
    }

    // ---- PcIdentityException / Fehlerbehandlung --------------------------------------------

    @Test fun exceptionIsNotNetworkError() {
        for (d in listOf(PcDecision.CHANGED, PcDecision.UNKNOWN, PcDecision.NO_ID)) {
            assertFalse(isNetworkError(PcIdentityException(d, host)))
        }
    }

    @Test fun exceptionIsNotNetworkErrorWhenNested() {
        val pc = PcIdentityException(PcDecision.CHANGED, host)
        assertFalse(isNetworkError(RuntimeException("outer", pc)))
        assertFalse(isNetworkError(RuntimeException("a", RuntimeException("b", pc))))
        // auch unter einer umhüllenden Netzwerk-Exception
        assertFalse(isNetworkError(java.net.SocketException("wrapper").initCause(pc)))
        assertFalse(isNetworkError(java.io.IOException("x", java.net.SocketException("y").also { it.initCause(pc) })))
    }

    @Test fun networkErrorsStillRecognized() {
        assertTrue(isNetworkError(java.net.SocketException("reset")))
        assertTrue(isNetworkError(RuntimeException("x", java.io.EOFException())))
    }

    @Test fun exceptionIsNotAnIoException() {
        assertFalse((PcIdentityException(PcDecision.CHANGED, host) as Exception) is java.io.IOException)
    }

    @Test fun messagesAreReadableAndContainHost() {
        val all = listOf(PcDecision.CHANGED, PcDecision.UNKNOWN, PcDecision.NO_ID, PcDecision.WRONG_NET)
        for (d in all) {
            val e = PcIdentityException(d, host)
            assertTrue(e.message, e.message.contains(host))
            assertEquals(e.message, friendlyError(e))
            assertEquals(e.message, friendlyError(RuntimeException("outer", e)))
            assertEquals(e.message, userErrorMsg(e).text())
        }
        // die Meldungen unterscheiden sich
        val texts = all.map { PcIdentityException(it, host).message }
        assertEquals(4, texts.toSet().size)
    }

    // --- SYNC-20261109-079: Rückfrage beim Hostwechsel eines bestehenden Profils ---
    @Test fun hostChangeAsksOnlyForExistingGuidProfilesWithNewUnconfirmedHost() {
        val g = PcCheckMode.GUID
        // bestehendes Profil, neuer Host, noch nicht bestätigt -> Rückfrage
        assertTrue(PcEditorRules.needsHostConfirm(true, g, "192.0.2.10", "192.0.2.20", null))
        assertTrue(PcEditorRules.needsHostConfirm(true, PcCheckMode.BOTH, "pc-alt", "pc-neu", null))
        // Ersteinrichtung (neues Profil): nie
        assertFalse(PcEditorRules.needsHostConfirm(false, g, "", "192.0.2.20", null))
        assertFalse(PcEditorRules.needsHostConfirm(false, g, "192.0.2.10", "192.0.2.20", null))
        // gleicher Host (Gross-/Kleinschreibung egal): nie
        assertFalse(PcEditorRules.needsHostConfirm(true, g, "PC-Alt", "pc-alt", null))
        // WIFI-Modus: keine Kennungsprüfung, keine Rückfrage
        assertFalse(PcEditorRules.needsHostConfirm(true, PcCheckMode.WIFI, "pc-alt", "pc-neu", null))
        // in dieser Sitzung bestätigter Host: nicht erneut fragen; anderer Host: wieder fragen
        assertFalse(PcEditorRules.needsHostConfirm(true, g, "pc-alt", "PC-Neu", "pc-neu"))
        assertTrue(PcEditorRules.needsHostConfirm(true, g, "pc-alt", "pc-dritter", "pc-neu"))
        // leere Angaben: keine Rückfrage (Host noch nicht eingegeben)
        assertFalse(PcEditorRules.needsHostConfirm(true, g, "pc-alt", "", null))
        assertFalse(PcEditorRules.needsHostConfirm(true, g, null, "pc-neu", null))
    }
}