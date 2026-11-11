package com.sync2pc.app

import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.SmbEncryption
import com.sync2pc.app.smb.SmbEncryptionUnavailableException
import com.sync2pc.app.smb.PcLink
import com.sync2pc.app.smb.PcLogin
import com.sync2pc.app.smb.PcSeen
import com.sync2pc.app.smb.PcStore
import com.sync2pc.app.smb.PcTransport
import com.sync2pc.app.sync.PcAccess
import com.sync2pc.app.sync.PcDecision
import com.sync2pc.app.sync.PcIdentityException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Anmeldelogik der PC-Erkennung ohne Gerät (SYNC-20261109-055 T1): Fake-Verbindung mit Kennung und Aufzeichnung.
 * Kernaussagen: nie anmelden bei UNKNOWN/CHANGED/NO_ID/WRONG_NET; Reihenfolge connect -> Kennung -> Entscheidung -> authenticate;
 * Lernen/Neuvertrauen nur nach erfolgreicher Anmeldung; strengerer Modus aus Kopie und Datenbank; WIFI-Kennung ist kein Vertrauen.
 */
class PcLoginTest {

    private val a = "11111111-2222-3333-4444-555555555555"
    private val b = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val host = "192.168.1.20"
    private val log = ArrayList<String>()

    private inner class FakeLink(
        val guid: String?, val n: Int, val encAvailable: Boolean = true, val sessionEnc: Boolean = true,
        val encThrows: Boolean = false,
    ) : PcLink<String> {
        override fun serverGuid(): String? { log += "guid#$n"; return guid }
        override fun encryptionAvailable(): Boolean { if (encThrows) throw IllegalStateException("kaputt"); return encAvailable }
        override fun authenticate(): String { log += "auth#$n"; return "session#$n" }
        override fun sessionEncrypted(session: String): Boolean { log += "sessenc#$n"; return sessionEnc }
        override fun close() { log += "close#$n" }
    }

    /** Liefert nacheinander die angegebenen Kennungen; ein Eintrag Exception = Verbindungsfehler */
    private inner class FakeTransport(private vararg val guids: Any?) : PcTransport<String> {
        var connects = 0
        /** Verschlüsselung am "PC" verfügbar / Sitzung nach der Anmeldung verschlüsselt / Abfrage wirft */
        var encAvailable = true
        var sessionEnc = true
        var encThrows = false
        override fun connect(host: String): PcLink<String> {
            val g = guids[minOf(connects, guids.size - 1)]
            connects++
            log += "connect#$connects"
            if (g is Exception) throw g
            return FakeLink(g as String?, connects, encAvailable, sessionEnc, encThrows)
        }
    }

    private inner class FakeStore(var db: Profile?) : PcStore {
        val learned = ArrayList<Triple<Long, String, String>>()
        val trusted = ArrayList<Triple<String, String, String>>()
        override fun profile(id: Long): Profile? = db
        override fun learn(id: Long, host: String, guid: String, mode: String): Int { learned += Triple(id, host, guid); return 1 }
        override fun trust(host: String, oldGuid: String, newGuid: String): Int { trusted += Triple(host, oldGuid, newGuid); return 1 }
    }

    private fun profile(mode: String, guid: String = "", guidHost: String = "", id: Long = 1, enc: SmbEncryption = SmbEncryption.PREFERRED) =
        Profile(id = id, smbPath = "\\\\$host\\Fotos", smbUser = "u", pcCheck = mode, pcGuid = guid, pcGuidHost = guidHost,
            smbEncryption = enc.name)

    private fun run(
        copy: Profile, transport: PcTransport<String>, store: PcStore?, access: PcAccess,
        learn: Boolean = false, net: Boolean = true, ask: ((String, String, String) -> Boolean)? = null,
        authenticate: Boolean = true, seen: PcSeen? = null,
    ) = PcLogin.connect(copy, host, transport, store, access, learn, net, ask, authenticate, seen)

    private fun assertRejected(d: PcDecision, block: () -> Unit) {
        try { block(); fail("PcIdentityException erwartet ($d)") } catch (e: PcIdentityException) { assertEquals(d, e.decision) }
        assertFalse("Zugangsdaten gesendet: $log", log.any { it.startsWith("auth") })
        // jede geöffnete Verbindung wurde wieder geschlossen
        val opened = log.filter { it.startsWith("connect#") }.map { it.substringAfter('#') }
        for (n in opened) assertTrue("close#$n fehlt: $log", log.contains("close#$n"))
    }

    // ---- keine Anmeldung ---------------------------------------------------------------------

    @Test fun unknownIdNeverAuthenticatesForUserAndAuto() {
        for (mode in listOf("GUID", "BOTH")) for (access in listOf(PcAccess.USER, PcAccess.AUTO)) {
            log.clear()
            val store = FakeStore(profile(mode))
            assertRejected(PcDecision.UNKNOWN) { run(profile(mode), FakeTransport(a), store, access, learn = true) }
            assertTrue(store.learned.isEmpty())
        }
    }

    @Test fun wrongNetNeverAuthenticates() {
        for (mode in listOf("GUID", "BOTH")) {
            log.clear()
            assertRejected(PcDecision.WRONG_NET) {
                run(profile(mode), FakeTransport(a), FakeStore(profile(mode)), PcAccess.SETUP, learn = true, net = false)
            }
        }
    }

    @Test fun noIdNeverAuthenticates() {
        for (g in listOf(null, "", "00000000-0000-0000-0000-000000000000")) {
            log.clear()
            assertRejected(PcDecision.NO_ID) {
                run(profile("GUID", a, host), FakeTransport(g), FakeStore(profile("GUID", a, host)), PcAccess.SETUP, learn = true)
            }
        }
    }

    @Test fun changedIdNeverAuthenticatesWithoutConfirmation() {
        val db = profile("GUID", a, host)
        // USER/AUTO und SETUP ohne Rückfrage-Funktion: nie anmelden
        for (access in PcAccess.values()) {
            log.clear()
            assertRejected(PcDecision.CHANGED) { run(profile("GUID"), FakeTransport(b), FakeStore(db), access, learn = true) }
        }
        // Rückfrage verneint: nicht anmelden, die erste Verbindung ist VOR der Frage geschlossen
        log.clear()
        var asked = false
        assertRejected(PcDecision.CHANGED) {
            run(profile("GUID"), FakeTransport(b), FakeStore(db), PcAccess.SETUP, ask = { _, _, _ ->
                asked = true
                assertTrue("Verbindung muss vor der Rückfrage geschlossen sein: $log", log.contains("close#1"))
                false
            })
        }
        assertTrue(asked)
    }

    // ---- Reihenfolge, Lernen -------------------------------------------------------------------

    @Test fun orderIsConnectGuidDecideAuthenticate_andLearnOnlyAfterCommit() {
        val store = FakeStore(profile("BOTH"))
        val c = run(profile("BOTH"), FakeTransport(a), store, PcAccess.SETUP, learn = true)
        assertEquals(listOf("connect#1", "guid#1", "auth#1"), log)
        assertEquals(PcDecision.ALLOW_AND_LEARN, c.decision)
        assertEquals("session#1", c.session)
        // noch nichts gelernt: erst nach connectShare in commit
        assertTrue(store.learned.isEmpty())
        assertEquals(Triple(1L, host, a), c.learn!!.let { Triple(it.id, it.host, it.guid) })
        assertEquals("BOTH", c.learn!!.mode)
        PcLogin.commit(c, store)
        assertEquals(listOf(Triple(1L, host, a)), store.learned)
    }

    @Test fun noLearningWithoutExplicitRequest_orWithoutSavedProfile() {
        // Freigaben/Ordner wählen (learn = false): anmelden, aber nicht lernen (V4)
        val c1 = run(profile("GUID"), FakeTransport(a), FakeStore(profile("GUID")), PcAccess.SETUP, learn = false)
        assertEquals(PcDecision.ALLOW_AND_LEARN, c1.decision)
        assertNull(c1.learn)
        // nicht gespeichertes Profil (id 0): anmelden unter den Regeln, kein Lernen, kein Store nötig
        val c2 = run(profile("GUID", id = 0), FakeTransport(a), null, PcAccess.SETUP, learn = true)
        assertNull(c2.learn)
        // gespeichert, aber nicht (mehr) in der Datenbank
        val c3 = run(profile("GUID"), FakeTransport(a), FakeStore(null), PcAccess.SETUP, learn = true)
        assertNull(c3.learn)
    }

    /** M1: Host im Editor ungespeichert geändert: anmelden unter SETUP-Regeln erlaubt, aber nichts lernen/neu vertrauen */
    @Test fun unsavedHostChangeNeverLearnsNorTrusts() {
        val db = profile("GUID", a, "192.168.1.99").copy(smbPath = "\\\\192.168.1.99\\Fotos")
        val c = run(profile("GUID"), FakeTransport(b), FakeStore(db), PcAccess.SETUP, learn = true)
        assertEquals(PcDecision.ALLOW_AND_LEARN, c.decision)
        assertNull(c.learn)
        log.clear()
        val db2 = profile("GUID", a, host).copy(smbPath = "\\\\192.168.1.99\\Fotos")
        assertRejected(PcDecision.CHANGED) { run(profile("GUID"), FakeTransport(b), FakeStore(db2), PcAccess.SETUP, ask = { _, _, _ -> true }) }
    }

    @Test fun knownIdAuthenticatesWithoutLearning_forAllAccessTypes() {
        for (access in PcAccess.values()) {
            log.clear()
            val c = run(profile("GUID"), FakeTransport(a), FakeStore(profile("GUID", a, host)), access, learn = true)
            assertEquals(PcDecision.ALLOW, c.decision)
            assertNull(c.learn); assertNull(c.trust)
            assertEquals(listOf("connect#1", "guid#1", "auth#1"), log)
        }
    }

    @Test fun wifiLearnsSilentlyButAlwaysAuthenticates() {
        val c = run(profile("WIFI"), FakeTransport(a), FakeStore(profile("WIFI")), PcAccess.AUTO)
        assertEquals(PcDecision.ALLOW_AND_LEARN, c.decision)
        assertNotNull(c.learn)
        val c2 = run(profile("WIFI"), FakeTransport(null), FakeStore(profile("WIFI")), PcAccess.AUTO)
        assertEquals(PcDecision.ALLOW, c2.decision)
    }

    // ---- Kennungswechsel (Punkt 3 / V8) --------------------------------------------------------

    @Test fun trustChange_reconnectsAndRequiresExactConfirmedId() {
        val db = profile("GUID", a, host)
        val store = FakeStore(db)
        log.clear()
        val c = run(profile("GUID"), FakeTransport(b, b), store, PcAccess.SETUP, ask = { h, old, new ->
            assertEquals(listOf(host, a, b), listOf(h, old, new))
            true
        })
        // aushandeln, Kennung lesen, schliessen, (fragen), neu verbinden, Kennung prüfen, erst dann anmelden
        assertEquals(listOf("connect#1", "guid#1", "close#1", "connect#2", "guid#2", "auth#2"), log)
        assertEquals(Triple(host, a, b), c.trust)
        assertTrue("Übernahme erst nach der Anmeldung", store.trusted.isEmpty())
        PcLogin.commit(c, store)
        assertEquals(listOf(Triple(host, a, b)), store.trusted)
    }

    @Test fun trustChange_aborts_ifReconnectSeesDifferentId() {
        log.clear()
        val store = FakeStore(profile("GUID", a, host))
        assertRejected(PcDecision.CHANGED) {
            run(profile("GUID"), FakeTransport(b, "cccccccc-dddd-eeee-ffff-000000000001"), store, PcAccess.SETUP, ask = { _, _, _ -> true })
        }
        assertTrue(store.trusted.isEmpty())
    }

    @Test fun trustChange_aborts_ifReconnectFails() {
        log.clear()
        try {
            run(profile("GUID"), FakeTransport(b, java.net.SocketException("weg")), FakeStore(profile("GUID", a, host)),
                PcAccess.SETUP, ask = { _, _, _ -> true })
            fail("Verbindungsfehler erwartet")
        } catch (e: java.net.SocketException) {
        }
        assertFalse(log.any { it.startsWith("auth") })
    }

    // ---- Modus aus Kopie und Datenbank, V7 ------------------------------------------------------

    @Test fun stricterModeOfCopyAndDatabase() {
        // Datenbank GUID, veraltete Kopie WIFI: die Prüfung bleibt (Entscheidung: CHANGED bei anderer Kennung)
        log.clear()
        assertRejected(PcDecision.CHANGED) {
            run(profile("WIFI"), FakeTransport(b), FakeStore(profile("GUID", a, host)), PcAccess.AUTO)
        }
        // Datenbank WIFI, Kopie GUID (noch ungespeicherter Wechsel): strenger gilt
        assertEquals(com.sync2pc.app.sync.PcCheckMode.GUID, PcLogin.currentIdentity(profile("GUID"), FakeStore(profile("WIFI"))).mode)
    }

    @Test fun wifiLearnedIdIsNoTrustForGuidModes() {
        val store = FakeStore(profile("WIFI", a, host)) // still bei WIFI gelernt
        // Wechsel auf GUID noch ungespeichert: gespeicherte Kennung zählt als leer -> AUTO/USER melden nicht an
        for (access in listOf(PcAccess.AUTO, PcAccess.USER)) {
            log.clear()
            assertRejected(PcDecision.UNKNOWN) { run(profile("GUID"), FakeTransport(a), store, access) }
        }
        // SETUP darf anmelden (Einrichtung), lernt aber nicht für einen ungespeicherten Moduswechsel
        val c = run(profile("GUID"), FakeTransport(a), store, PcAccess.SETUP, learn = true)
        assertEquals(PcDecision.ALLOW_AND_LEARN, c.decision)
        assertNull(c.learn)
        // auch eine abweichende Kennung ist bei WIFI-Datenbankstand kein Vertrauensanker
        log.clear()
        assertRejected(PcDecision.UNKNOWN) { run(profile("BOTH"), FakeTransport(b), store, PcAccess.AUTO) }
    }

    @Test fun missingStoreForSavedProfileFailsSafe() {
        log.clear()
        try {
            run(profile("GUID"), FakeTransport(a), null, PcAccess.SETUP, learn = true)
            fail("IllegalStateException erwartet")
        } catch (e: IllegalStateException) {
        }
        assertFalse(log.any { it.startsWith("auth") })
        assertTrue(log.contains("close#1"))
    }

    // ---- Vorab-Prüfung ohne Anmeldung, Host-Zwischenspeicher (F5) ---------------------------------

    @Test fun precheckNeverAuthenticatesAndClosesTheConnection() {
        log.clear()
        val c = run(profile("GUID"), FakeTransport(a), FakeStore(profile("GUID", a, host)), PcAccess.AUTO, authenticate = false)
        assertEquals(PcDecision.ALLOW, c.decision)
        assertEquals(listOf("connect#1", "guid#1", "close#1"), log)
    }

    @Test fun hostCacheNegotiatesOncePerHost_butDecidesPerProfile() {
        val t = FakeTransport(a)
        val seen = PcSeen()
        val p1 = profile("GUID", a, host, id = 1)
        val p2 = profile("GUID", b, host, id = 2)          // anderes Profil, eigene (abweichende) Kennung
        val p3 = profile("WIFI", a, host, id = 3)           // WIFI-Kennung darf p4 nichts freischalten
        val p4 = profile("GUID", "", "", id = 4)
        val store = object : PcStore {
            val all = listOf(p1, p2, p3, p4).associateBy { it.id }
            override fun profile(id: Long) = all[id]
            override fun learn(id: Long, host: String, guid: String, mode: String) = 0
            override fun trust(host: String, oldGuid: String, newGuid: String) = 0
        }
        val d1 = run(p1, t, store, PcAccess.AUTO, authenticate = false, seen = seen).decision
        val d2 = run(p2, t, store, PcAccess.AUTO, authenticate = false, seen = seen).decision
        val d4 = run(p4, t, store, PcAccess.AUTO, authenticate = false, seen = seen).decision
        assertEquals(1, t.connects)
        assertEquals(PcDecision.ALLOW, d1)
        assertEquals(PcDecision.CHANGED, d2)
        assertEquals(PcDecision.UNKNOWN, d4)
    }

    @Test fun hostCacheRemembersFailuresPerRun() {
        val t = FakeTransport(java.net.SocketTimeoutException("aus"))
        val seen = PcSeen()
        val store = FakeStore(profile("GUID", a, host))
        repeat(2) {
            try {
                run(profile("GUID"), t, store, PcAccess.AUTO, authenticate = false, seen = seen)
                fail("Verbindungsfehler erwartet")
            } catch (e: java.net.SocketTimeoutException) {
            }
        }
        assertEquals("zweiter Versuch im selben Lauf fragt den Host nicht erneut", 1, t.connects)
    }

    @Test fun cacheIsNotUsedForRealSignIn() {
        val t = FakeTransport(a)
        val seen = PcSeen()
        val store = FakeStore(profile("GUID", a, host))
        run(profile("GUID"), t, store, PcAccess.AUTO, authenticate = false, seen = seen)
        // Anmeldung verbindet immer frisch und prüft die Kennung dieser Verbindung
        run(profile("GUID"), t, store, PcAccess.AUTO, authenticate = true, seen = seen)
        assertEquals(2, t.connects)
        assertEquals(1, log.count { it == "auth#2" })
    }

    // ---- SMB3-Verschlüsselung (SYNC-20261111-144, Testfälle S2/S3 aus SYNC-20261110-129) ------------

    private fun encProfile(enc: SmbEncryption) = profile("GUID", a, host, enc = enc)

    private fun assertEncRejected(afterLogin: Boolean, block: () -> Unit) {
        try { block(); fail("SmbEncryptionUnavailableException erwartet") } catch (e: SmbEncryptionUnavailableException) {
            assertEquals(afterLogin, e.afterLogin)
            assertEquals(host, e.host)
        }
        // jede geöffnete Verbindung wurde wieder geschlossen
        val opened = log.filter { it.startsWith("connect#") }.map { it.substringAfter('#') }
        for (n in opened) assertTrue("close#$n fehlt: $log", log.contains("close#$n"))
    }

    @Test fun requiredWithoutServerEncryptionNeverAuthenticates() {
        val t = FakeTransport(a).also { it.encAvailable = false }
        val p = encProfile(SmbEncryption.REQUIRED)
        assertEncRejected(afterLogin = false) { run(p, t, FakeStore(p), PcAccess.AUTO) }
        assertFalse("Zugangsdaten gesendet: $log", log.any { it.startsWith("auth") })
    }

    @Test fun requiredWhenEncryptionQueryFailsCountsAsUnavailable() {
        val t = FakeTransport(a).also { it.encThrows = true }
        val p = encProfile(SmbEncryption.REQUIRED)
        assertEncRejected(afterLogin = false) { run(p, t, FakeStore(p), PcAccess.AUTO) }
        assertFalse("Zugangsdaten gesendet: $log", log.any { it.startsWith("auth") })
    }

    @Test fun requiredWithServerEncryptionAuthenticatesAndChecksSession() {
        val p = encProfile(SmbEncryption.REQUIRED)
        val c = run(p, FakeTransport(a), FakeStore(p), PcAccess.AUTO)
        assertEquals("session#1", c.session)
        // Reihenfolge: aushandeln, Kennung, Anmeldung, danach Sitzungsprüfung
        assertEquals(listOf("connect#1", "guid#1", "auth#1", "sessenc#1"), log)
    }

    @Test fun requiredWithUnencryptedSessionAfterLoginAbortsAndClosesTheConnection() {
        val t = FakeTransport(a).also { it.sessionEnc = false }
        val p = encProfile(SmbEncryption.REQUIRED)
        assertEncRejected(afterLogin = true) { run(p, t, FakeStore(p), PcAccess.AUTO) }
        assertTrue(log.contains("auth#1"))
        assertTrue(log.indexOf("sessenc#1") > log.indexOf("auth#1"))
        // nichts gelernt/übernommen: die Sitzung wird nie an den Aufrufer gegeben
        val store = FakeStore(encProfile(SmbEncryption.REQUIRED))
        log.clear()
        assertEncRejected(afterLogin = true) { run(encProfile(SmbEncryption.REQUIRED), FakeTransport(a).also { it.sessionEnc = false }, store, PcAccess.AUTO) }
        assertTrue(store.learned.isEmpty() && store.trusted.isEmpty())
    }

    @Test fun preferredAndOffAuthenticateWithoutServerEncryptionAndSkipTheCheck() {
        for (enc in listOf(SmbEncryption.PREFERRED, SmbEncryption.OFF)) {
            log.clear()
            val t = FakeTransport(a).also { it.encAvailable = false; it.sessionEnc = false }
            val p = encProfile(enc)
            val c = run(p, t, FakeStore(p), PcAccess.AUTO)
            assertEquals("session#1", c.session)
            assertEquals("$enc: keine Sitzungsprüfung", listOf("connect#1", "guid#1", "auth#1"), log)
        }
    }

    @Test fun existingProfilesWithoutFieldBehaveAsPreferred() {
        // Standardwert des Profils und unbekannte/leere Werte -> PREFERRED (kein Abbruch ohne Serververschlüsselung)
        assertEquals(SmbEncryption.PREFERRED, Profile().encryption())
        assertEquals(SmbEncryption.PREFERRED, SmbEncryption.parse(null))
        assertEquals(SmbEncryption.PREFERRED, SmbEncryption.parse(""))
        assertEquals(SmbEncryption.PREFERRED, SmbEncryption.parse("  "))
        assertEquals(SmbEncryption.REQUIRED, SmbEncryption.parse("quatsch")) // vorhanden, aber unbekannt: sicherer Ausfall
        assertEquals(SmbEncryption.REQUIRED, SmbEncryption.parse("required"))
    }

    @Test fun precheckRequiredWithoutServerEncryptionAborts() {
        val t = FakeTransport(a).also { it.encAvailable = false }
        val p = encProfile(SmbEncryption.REQUIRED)
        assertEncRejected(afterLogin = false) { run(p, t, FakeStore(p), PcAccess.AUTO, authenticate = false) }
        assertFalse(log.any { it.startsWith("auth") })
    }

    @Test fun precheckRequiredWithServerEncryptionAllows() {
        val p = encProfile(SmbEncryption.REQUIRED)
        assertEquals(PcDecision.ALLOW, run(p, FakeTransport(a), FakeStore(p), PcAccess.AUTO, authenticate = false).decision)
    }

    @Test fun precheckPreferredWithoutServerEncryptionAllows() {
        val t = FakeTransport(a).also { it.encAvailable = false }
        val p = encProfile(SmbEncryption.PREFERRED)
        assertEquals(PcDecision.ALLOW, run(p, t, FakeStore(p), PcAccess.AUTO, authenticate = false).decision)
    }

    @Test fun precheckIdentityDecisionHasPrecedenceOverEncryption() {
        // Unbekannte Kennung bei REQUIRED ohne Verschlüsselung: die Kennungs-Entscheidung wird geliefert (kein Verschlüsselungsabbruch)
        val t = FakeTransport(b).also { it.encAvailable = false }
        val p = encProfile(SmbEncryption.REQUIRED)
        assertEquals(PcDecision.CHANGED, run(p, t, FakeStore(p), PcAccess.AUTO, authenticate = false).decision)
    }

    @Test fun hostCacheKeepsEncryptionResultPerModeAndRepeatsTheAbort() {
        val t = FakeTransport(a).also { it.encAvailable = false }
        val seen = PcSeen()
        val req = encProfile(SmbEncryption.REQUIRED)
        repeat(2) { assertEncRejected(afterLogin = false) { run(req, t, FakeStore(req), PcAccess.AUTO, authenticate = false, seen = seen) } }
        assertEquals("gleicher Modus: nur eine Aushandlung je Host und Lauf", 1, t.connects)
        // anderer Modus am selben Host: eigene Aushandlung (die Verfügbarkeit hängt von der Konfiguration ab)
        val pref = encProfile(SmbEncryption.PREFERRED)
        assertEquals(PcDecision.ALLOW, run(pref, t, FakeStore(pref), PcAccess.AUTO, authenticate = false, seen = seen).decision)
        assertEquals(2, t.connects)
    }

    @Test fun encryptionExceptionIsNotNetworkErrorAndHasReadableTexts() {
        val e = SmbEncryptionUnavailableException(host)
        val e2 = SmbEncryptionUnavailableException(host, afterLogin = true)
        for (x in listOf(e, e2)) {
            assertFalse(com.sync2pc.app.sync.isNetworkError(x))
            assertFalse(com.sync2pc.app.sync.isNetworkError(java.net.SocketException("w").initCause(x)))
            assertFalse((x as Exception) is java.io.IOException)
            assertTrue(x.message, x.message.contains(host))
            assertEquals(x.message, com.sync2pc.app.sync.userError(x))
            assertEquals(x.message, com.sync2pc.app.sync.userError(RuntimeException("outer", x)))
            assertEquals(x.message, com.sync2pc.app.sync.shortError(x))
            assertEquals(x.message, com.sync2pc.app.sync.userErrorMsg(x).text())
        }
        assertTrue(e.message != e2.message)
    }
}
