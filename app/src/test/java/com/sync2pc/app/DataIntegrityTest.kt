package com.sync2pc.app

import com.sync2pc.app.data.DeleteMode
import com.sync2pc.app.data.SyncedFile
import com.sync2pc.app.mirror.ContentCompare
import com.sync2pc.app.mirror.HashAbortedException
import com.sync2pc.app.smb.RemoteFiles
import com.sync2pc.app.sync.CopyVerdict
import com.sync2pc.app.sync.DeletionManager
import com.sync2pc.app.sync.PartialHash
import com.sync2pc.app.sync.PcDecision
import com.sync2pc.app.sync.PcIdentityException
import com.sync2pc.app.sync.PhoneNow
import com.sync2pc.app.sync.PresenceCheck
import com.sync2pc.app.sync.RemoteFinder
import com.sync2pc.app.sync.SpaceFreer
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketException

/**
 * Datenintegrität (SYNC-20261111-142): Gleichheitsprüfung vor Löschen, Freigeben und "synchron".
 * R-I3 (Ordner-Sync > 32 MB), R-I2 (Speicher freigeben Fotos), R-I1 (Löschen nach Sicherung), R-F1/R-I4 ("bereits vorhanden").
 */
class DataIntegrityTest {
    private val mb = 1024 * 1024

    private fun reader(x: ByteArray): (Long, Int) -> ByteArray = { off, len -> x.copyOfRange(off.toInt(), off.toInt() + len) }

    // ===== R-I3: vollständiger Vergleich auch über 32 MB ==================================================

    @Test fun r3_stichprobeSiehtAenderungZwischenDenStichprobenNicht_vollvergleichSchon() {
        val size = 33 * mb // über FULL_LIMIT
        assertTrue(size > PartialHash.FULL_LIMIT)
        val a = ByteArray(size) { (it % 251).toByte() }
        val b = a.copyOf().also { it[500_000] = (it[500_000] + 1).toByte() } // zwischen den 32 Stichproben
        // Altes Verhalten: beide Hashes gleich -> würde als "gleich" verbucht (Datenverlust)
        assertEquals(PartialHash.thorough(size.toLong(), reader(a)), PartialHash.thorough(size.toLong(), reader(b)))
        // Neu: ganze Datei -> verschieden
        assertNotEquals(PartialHash.comparison(size.toLong(), reader(a)), PartialHash.comparison(size.toLong(), reader(b)))
        assertFalse(ContentCompare.same(size.toLong(), size.toLong(),
            { PartialHash.comparison(size.toLong(), reader(a)) }, { PartialHash.comparison(size.toLong(), reader(b)) }))
        // Identische Dateien sind weiterhin gleich
        assertTrue(ContentCompare.same(size.toLong(), size.toLong(),
            { PartialHash.comparison(size.toLong(), reader(a)) }, { PartialHash.comparison(size.toLong(), reader(a.copyOf())) }))
    }

    @Test fun r3_bisZuGrenzeIdentischMitBisherigemHash() {
        for (size in listOf(0L, 1L, 100L, 3L * PartialHash.CHUNK, 5L * mb + 17, PartialHash.FULL_LIMIT)) {
            val data = ByteArray(size.toInt()) { (it % 241).toByte() }
            assertEquals("Grösse $size", PartialHash.thorough(size, reader(data)), PartialHash.comparison(size, reader(data)))
        }
    }

    @Test fun r3_kurzeLeseblockWirdAlsFehlerGewertet() {
        val size = 3L * mb
        val data = ByteArray(size.toInt())
        // Datei ist beim Lesen kürzer geworden: Block unvollständig
        val shortRead: (Long, Int) -> ByteArray = { off, len -> data.copyOfRange(off.toInt(), minOf(off.toInt() + len, 2 * mb)) }
        try { PartialHash.comparison(size, shortRead); fail("IOException erwartet") } catch (e: IOException) { /* ok */ }
        // ... und damit "nicht gleich" (sicherer Ausfall)
        assertFalse(ContentCompare.same(size, size, { PartialHash.comparison(size, shortRead) }, { PartialHash.comparison(size, reader(data)) }))
    }

    @Test fun r3_ungleicheGroesseWirdNieGehasht() {
        var hashed = false
        assertFalse(ContentCompare.same(10, 11, { hashed = true; "a" }, { hashed = true; "a" }))
        assertFalse(hashed)
    }

    @Test fun r3_lesefehlerErgibtNichtGleich_abbruchUndNetzwerkgehenNachAussen() {
        assertFalse(ContentCompare.same(5, 5, { throw IOException("Lesefehler") }, { "x" }))
        assertFalse(ContentCompare.same(5, 5, { "x" }, { throw IllegalStateException("kaputt") }))
        try { ContentCompare.same(5, 5, { "x" }, { throw CancellationException("abgebrochen") }); fail() } catch (e: CancellationException) { /* ok */ }
        try { ContentCompare.same(5, 5, { throw HashAbortedException() }, { "x" }); fail() } catch (e: HashAbortedException) { /* ok */ }
        try { ContentCompare.same(5, 5, { "x" }, { throw SocketException("weg") }); fail() } catch (e: SocketException) { /* ok */ }
    }

    @Test fun r3_abbruchImVergleichStopptVorDemNaechstenBlock_undFortschrittWirdGemeldet() {
        val size = 40L * mb
        var blocks = 0
        var last = 0L
        val read: (Long, Int) -> ByteArray = { _, len -> ByteArray(len).also { blocks++ } }
        try {
            PartialHash.comparison(size, read, check = { if (blocks >= 3) throw HashAbortedException() }, onBytes = { last = it })
            fail("Abbruch erwartet")
        } catch (e: HashAbortedException) { /* ok */ }
        assertEquals(3, blocks)
        assertEquals(3L * mb, last)
        // Vollständiger Lauf meldet am Ende die ganze Grösse
        PartialHash.comparison(size, { _, len -> ByteArray(len) }, onBytes = { last = it })
        assertEquals(size, last)
    }

    // ===== R-I2: Speicher freigeben (Fotos) ===============================================================

    private val size = 500L

    @Test fun r2_pfadBestaetigtNurBeiGleicherGroesseUndGleichemHash() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", size, "H")
        val f = RemoteFinder(share)
        val p = "Fotos\\2026\\06\\a.jpg"
        assertTrue(f.existsAtPath(p, size) { "H" })
        assertFalse(f.existsAtPath(p, size) { "ANDERS" })          // gleiche Grösse, anderer Inhalt
        assertFalse(f.existsAtPath(p, size + 1) { "H" })           // andere Grösse
        assertFalse(f.existsAtPath(p, size) { null })              // Handy-Datei nicht lesbar
        assertFalse(f.existsAtPath("Fotos\\2026\\06\\fehlt.jpg", size) { "H" })
        assertFalse(f.existsAtPath("", size) { "H" })
    }

    @Test fun r2_ohneGroessenGleichheitWirdNichtGehasht() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", size + 1, "H")
        assertFalse(RemoteFinder(share).existsAtPath("Fotos\\2026\\06\\a.jpg", size) { "H" })
        assertEquals(0, share.hashReads)
    }

    @Test fun r2_lesefehlerAmPcIstNichtBestaetigt_netzwerkfehlerGehtNachAussen() {
        val broken = object : RemoteFiles by FakeShare("Fotos").add("a.jpg", size, "H") {
            override fun partialHash(path: String, size: Long): String = throw IOException("gesperrt")
        }
        assertFalse(RemoteFinder(broken).existsAtPath("Fotos\\a.jpg", size) { "H" })
        val offline = object : RemoteFiles by FakeShare("Fotos").add("a.jpg", size, "H") {
            override fun partialHash(path: String, size: Long): String = throw SocketException("weg")
        }
        try { RemoteFinder(offline).existsAtPath("Fotos\\a.jpg", size) { "H" }; fail() } catch (e: SocketException) { /* ok */ }
    }

    private fun row(profile: Long, size: Long = 100, modified: Long = 1000, verified: Boolean = true, path: String = "Fotos\\2026\\06\\a.jpg") =
        SyncedFile(
            profileId = profile, sourceKey = "DCIM/Camera/a.jpg", mediaUri = "content://media/external/images/media/1",
            size = size, dateModified = modified, partialHash = "H", remotePath = path, syncedAt = 1L, verified = verified,
        )

    @Test fun r2_gesichertNurWennGroesseUndAenderungsdatumPassen() {
        val r = row(1, size = 100, modified = 1000)
        assertTrue(SpaceFreer.rowMatches(r, 100, 1000))
        assertFalse(SpaceFreer.rowMatches(r, 100, 1001))   // bearbeitet, gleiche Grösse
        assertFalse(SpaceFreer.rowMatches(r, 101, 1000))
    }

    @Test fun speicherFreigeben_neupruefungVorLoeschenErkenntBearbeitung() {   // SYNC-20261111-205
        assertTrue(SpaceFreer.unchanged(100, 1000, 100, 1000))
        assertFalse(SpaceFreer.unchanged(100, 1000, 101, 1000))    // andere Grösse
        assertFalse(SpaceFreer.unchanged(100, 1000, 100, 1001))    // bearbeitet (neues Datum)
        assertFalse(SpaceFreer.unchanged(100, 1000, null, null))   // nicht lesbar = geändert
    }

    // ===== R-I1: Löschen nach Sicherung ===================================================================

    @Test fun r1_syncedByZaehltNurZeilenZurAktuellenDatei() {
        val now = PhoneNow.Present(100, 1000)
        val all = listOf(
            row(1, 100, 1000),                       // passt
            row(2, 100, 999),                        // Foto seit der Sicherung bearbeitet
            row(3, 99, 1000),                        // andere Grösse
            row(4, 100, 1000, verified = false),     // nicht verifiziert
        )
        assertEquals(setOf(1L), DeletionManager.syncedBy(all, now))
    }

    private fun shares(vararg pairs: Pair<Long, RemoteFiles>): (Long) -> RemoteFiles {
        val m = pairs.toMap()
        return { id -> m[id] ?: throw IOException("kein PC") }
    }

    @Test fun r1_kopieVorhandenUndGleich_darfGeloeschtWerden() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", 100, "H")
        val v = DeletionManager.verifyCopies(listOf(row(1)), { "H" }, shares(1L to share))
        assertTrue(v is CopyVerdict.Ok)
    }

    @Test fun r1_kopieFehlt_nichtLoeschen_zeilenZuruecksetzen() {
        val share = FakeShare("Fotos")  // leer
        val v = DeletionManager.verifyCopies(listOf(row(1)), { "H" }, shares(1L to share))
        assertTrue(v is CopyVerdict.Missing)
        assertEquals(listOf(1L), (v as CopyVerdict.Missing).profileIds)
    }

    @Test fun r1_kopieWeichtAb_nichtLoeschen() {
        val gleicheGroesseAndererInhalt = FakeShare("Fotos").add("2026\\06\\a.jpg", 100, "ANDERS")
        assertTrue(DeletionManager.verifyCopies(listOf(row(1)), { "H" }, shares(1L to gleicheGroesseAndererInhalt)) is CopyVerdict.Missing)
        val andereGroesse = FakeShare("Fotos").add("2026\\06\\a.jpg", 101, "H")
        assertTrue(DeletionManager.verifyCopies(listOf(row(1)), { "H" }, shares(1L to andereGroesse)) is CopyVerdict.Missing)
    }

    @Test fun r1_mehrereProfile_jedeKopieMussStimmen() {
        val ok = FakeShare("Fotos").add("2026\\06\\a.jpg", 100, "H")
        val leer = FakeShare("Fotos")
        val v = DeletionManager.verifyCopies(listOf(row(1), row(2)), { "H" }, shares(1L to ok, 2L to leer))
        assertEquals(listOf(2L), (v as CopyVerdict.Missing).profileIds) // nur das Profil ohne Kopie wird zurückgesetzt
    }

    @Test fun r1_pcNichtErreichbar_nichtsLoeschenNichtsZuruecksetzen() {
        val v = DeletionManager.verifyCopies(listOf(row(1)), { "H" }) { throw SocketException("PC aus") }
        assertTrue(v is CopyVerdict.Unreachable)
        // auch ein beliebiger anderer Fehler beim Verbinden/Lesen bedeutet: nicht geprüft = nicht löschen
        assertTrue(DeletionManager.verifyCopies(listOf(row(1)), { "H" }) { throw IllegalStateException("x") } is CopyVerdict.Unreachable)
    }

    @Test fun r1_pcErkennungLehntAb_nichtsLoeschen() {
        val v = DeletionManager.verifyCopies(listOf(row(1)), { "H" }) { throw PcIdentityException(PcDecision.CHANGED, "pc") }
        assertTrue(v is CopyVerdict.Rejected)
    }

    @Test fun r1_handyDateiNichtLesbar_nichtsLoeschenNichtsZuruecksetzen() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", 100, "H")
        assertTrue(DeletionManager.verifyCopies(listOf(row(1)), { null }, shares(1L to share)) is CopyVerdict.PhoneUnreadable)
    }

    @Test fun r1_ohnePruefbareZeilenWirdNieGeloescht() {
        assertTrue(DeletionManager.verifyCopies(emptyList(), { "H" }, shares()) is CopyVerdict.Unreachable)
    }

    @Test fun r1_vergleichtGegenAktuelleHandyDatei() {
        // Der Hash kommt aus der aktuellen Handy-Datei (Lambda), nicht aus der Zeile: bearbeitet -> Hash weicht ab
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", 100, "H")
        assertTrue(DeletionManager.verifyCopies(listOf(row(1)), { "H-neu" }, shares(1L to share)) is CopyVerdict.Missing)
    }

    // ===== R-F1/R-I4: "bereits vorhanden" =================================================================

    @Test fun f1_stufeIstMindestensMittel() {
        assertEquals(PartialHash.LEVEL_MEDIUM, PresenceCheck.level(PartialHash.LEVEL_OFF))
        assertEquals(PartialHash.LEVEL_MEDIUM, PresenceCheck.level(PartialHash.LEVEL_LOW))
        assertEquals(PartialHash.LEVEL_MEDIUM, PresenceCheck.level(PartialHash.LEVEL_MEDIUM))
        assertEquals(PartialHash.LEVEL_HIGH, PresenceCheck.level(PartialHash.LEVEL_HIGH))
        assertEquals(PartialHash.LEVEL_FULL, PresenceCheck.level(PartialHash.LEVEL_FULL))
    }

    @Test fun f1_keepBleibtWieBisher_keinePruefung() {
        var hashed = false
        assertTrue(PresenceCheck.verifiedWhenPresent(DeleteMode.KEEP, { hashed = true; "a" }, { hashed = true; "b" }))
        assertFalse(hashed)
    }

    @Test fun f1_loeschmodusMitGleichenDaten_istGesichert() {
        val data = ByteArray(mb) { (it % 251).toByte() }
        val level = PresenceCheck.level(PartialHash.LEVEL_LOW)
        for (mode in listOf(DeleteMode.TRASH, DeleteMode.DELETE)) {
            assertTrue(PresenceCheck.verifiedWhenPresent(mode,
                { PartialHash.forLevel(level, data.size.toLong(), reader(data)) },
                { PartialHash.forLevel(level, data.size.toLong(), reader(data.copyOf())) }))
        }
    }

    @Test fun f1_gleicherTeilhashAberAndererInhalt_keineLoeschfreigabe() {
        val a = ByteArray(mb) { (it % 251).toByte() }
        val b = a.copyOf().also { it[300_000] = (it[300_000] + 1).toByte() } // ausserhalb der 3 x 64 KB
        val n = a.size.toLong()
        // Der bisherige "bereits vorhanden"-Test (Teil-Hash) hält beide für gleich ...
        assertEquals(PartialHash.compute(n, reader(a)), PartialHash.compute(n, reader(b)))
        // ... die Prüfung auf Profilstufe (mindestens Mittel) nicht
        val level = PresenceCheck.level(PartialHash.LEVEL_LOW)
        assertFalse(PresenceCheck.verifiedWhenPresent(DeleteMode.TRASH,
            { PartialHash.forLevel(level, n, reader(a)) }, { PartialHash.forLevel(level, n, reader(b)) }))
    }

    @Test fun f1_lesefehlerKeineFreigabe_netzwerkUndAbbruchGehenNachAussen() {
        assertFalse(PresenceCheck.verifiedWhenPresent(DeleteMode.TRASH, { "a" }, { throw IOException("x") }))
        try { PresenceCheck.verifiedWhenPresent(DeleteMode.TRASH, { "a" }, { throw SocketException("weg") }); fail() } catch (e: SocketException) { /* ok */ }
        try { PresenceCheck.verifiedWhenPresent(DeleteMode.TRASH, { throw CancellationException("x") }, { "a" }); fail() } catch (e: CancellationException) { /* ok */ }
    }
}
