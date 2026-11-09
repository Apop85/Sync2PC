package com.sync2pc.app

import com.sync2pc.app.data.FolderLayout
import com.sync2pc.app.sync.FolderNames
import com.sync2pc.app.sync.MonthFolderSearch
import com.sync2pc.app.sync.RemoteFinder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Speicher freigeben": Suche auf dem PC je Ordnervorlage (SYNC-20261109-084, DEC-136) */
class RemoteFinderTest {
    private val size = 500L

    private fun elsewhere(
        share: FakeShare, stored: String, layout: FolderLayout, hint: Int? = null, hash: String? = "H",
    ) = RemoteFinder(share).existsElsewhere(stored, size, layout, hint) { hash }

    // ---- Pfad zerlegen ------------------------------------------------------------------
    @Test fun locateFindsYearAndDirs() {
        val l = MonthFolderSearch.locate("Fotos\\2026\\Ägypten\\06\\a.jpg")!!
        assertEquals("Fotos\\2026", l.yearPath)
        assertEquals(listOf("Ägypten", "06"), l.dirs)
        assertEquals(6, MonthFolderSearch.monthOfLocation(l, FolderLayout.COUNTRY_FOLDER))
        val v3 = MonthFolderSearch.locate("Fotos\\2026\\06\\Ägypten\\a.jpg")!!
        assertEquals(6, MonthFolderSearch.monthOfLocation(v3, FolderLayout.COUNTRY_SUBFOLDER))
    }

    @Test fun locateUsesLastYearSegmentAndRejectsUnknownShapes() {
        assertEquals("Backup\\2020\\Fotos\\2026", MonthFolderSearch.locate("Backup\\2020\\Fotos\\2026\\06\\a.jpg")!!.yearPath)
        assertNull(MonthFolderSearch.locate("a.jpg"))
        assertNull(MonthFolderSearch.locate("Fotos\\2026\\a.jpg"))              // kein Ordner unter dem Jahr
        assertNull(MonthFolderSearch.locate("Fotos\\Sonstiges\\06\\a.jpg"))     // kein Jahresordner
        assertNull(MonthFolderSearch.locate("2026\\a\\b\\c\\x.jpg"))            // zu tief für alle Vorlagen
    }

    // ---- V1 ---------------------------------------------------------------------------------
    @Test fun v1_movedFromMonthToCountryFolderIsFound() {
        val share = FakeShare("Fotos").add("2026\\06 - Ägypten\\a.jpg", size, "H")
        assertTrue(elsewhere(share, "Fotos\\2026\\06\\a.jpg", FolderLayout.MONTH_SUFFIX))
    }

    @Test fun v1_sameNameAndSizeButOtherContentIsNotFound() {
        val share = FakeShare("Fotos").add("2026\\06 - Ägypten\\a.jpg", size, "ANDERS")
        assertFalse(elsewhere(share, "Fotos\\2026\\06\\a.jpg", FolderLayout.MONTH_SUFFIX))
    }

    @Test fun v1_storedFolderItselfIsSkipped() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", size + 1, "H")
        assertFalse(elsewhere(share, "Fotos\\2026\\06\\a.jpg", FolderLayout.MONTH_SUFFIX))
        assertEquals(0, share.hashReads)
    }

    // ---- V2 ---------------------------------------------------------------------------------
    @Test fun v2_foundInPlainMonthFolderInYear() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", size, "H")
        assertTrue(elsewhere(share, "Fotos\\2026\\Ägypten\\06\\a.jpg", FolderLayout.COUNTRY_FOLDER))
    }

    @Test fun v2_foundInMonthFolderOfOtherCountry() {
        val share = FakeShare("Fotos").add("2026\\Italien\\06\\a.jpg", size, "H")
        assertTrue(elsewhere(share, "Fotos\\2026\\Ägypten\\06\\a.jpg", FolderLayout.COUNTRY_FOLDER))
    }

    @Test fun v2_otherMonthIsNotSearched() {
        val share = FakeShare("Fotos").add("2026\\Italien\\07\\a.jpg", size, "H").add("2026\\07\\a.jpg", size, "H")
        assertFalse(elsewhere(share, "Fotos\\2026\\Ägypten\\06\\a.jpg", FolderLayout.COUNTRY_FOLDER))
    }

    // ---- V3 ---------------------------------------------------------------------------------
    @Test fun v3_foundDirectlyInMonthFolder() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", size, "H")
        assertTrue(elsewhere(share, "Fotos\\2026\\06\\Ägypten\\a.jpg", FolderLayout.COUNTRY_SUBFOLDER))
    }

    @Test fun v3_foundInOtherCountrySubfolderAndRenamedMonthFolder() {
        val s1 = FakeShare("Fotos").add("2026\\06\\Italien\\a.jpg", size, "H")
        assertTrue(elsewhere(s1, "Fotos\\2026\\06\\Ägypten\\a.jpg", FolderLayout.COUNTRY_SUBFOLDER))
        val s2 = FakeShare("Fotos").add("2026\\06 Reise\\Italien\\a.jpg", size, "H")
        assertTrue(elsewhere(s2, "Fotos\\2026\\06\\Ägypten\\a.jpg", FolderLayout.COUNTRY_SUBFOLDER))
    }

    @Test fun v3_foundWhenFileWasMovedToV1StyleFolder() {
        // Vorlagenunabhängig: Fund auch in einem Ordner einer anderen Struktur
        val share = FakeShare("Fotos").add("2026\\06 - Ägypten\\a.jpg", size, "H")
        assertTrue(elsewhere(share, "Fotos\\2026\\06\\Ägypten\\a.jpg", FolderLayout.COUNTRY_SUBFOLDER))
    }

    // ---- Month hint (media_info) ----------------------------------------------------------------
    @Test fun monthHintAddsSearchWhenPathMonthIsUnreadable() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", size, "H")
        // gespeicherter Ordner ohne Monatszahl ("Urlaub"): ohne media_info-Monat keine Suche, mit Hinweis Fund
        assertFalse(elsewhere(share, "Fotos\\2026\\Urlaub\\a.jpg", FolderLayout.MONTH_SUFFIX, hint = null))
        assertTrue(elsewhere(share, "Fotos\\2026\\Urlaub\\a.jpg", FolderLayout.MONTH_SUFFIX, hint = 6))
    }

    // ---- Sicherheit: DEC-136 ------------------------------------------------------------------------
    @Test fun unreadableLocalFileKeepsPhone() {
        val share = FakeShare("Fotos").add("2026\\06 - Ägypten\\a.jpg", size, "H")
        assertFalse(elsewhere(share, "Fotos\\2026\\06\\a.jpg", FolderLayout.MONTH_SUFFIX, hash = null))
    }

    @Test fun differentSizeOrNameNeverHashes() {
        val share = FakeShare("Fotos").add("2026\\06 - Ägypten\\a.jpg", size + 1, "H").add("2026\\06 - Ägypten\\b.jpg", size, "H")
        assertFalse(elsewhere(share, "Fotos\\2026\\06\\a.jpg", FolderLayout.MONTH_SUFFIX))
        assertEquals(0, share.hashReads)
    }

    @Test fun pathCheckAcceptsOnlyMatchingSize() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", size, "H")
        val f = RemoteFinder(share)
        assertTrue(f.existsAtPath("Fotos\\2026\\06\\a.jpg", size))
        assertFalse(f.existsAtPath("Fotos\\2026\\06\\a.jpg", size + 1))
        assertFalse(f.existsAtPath("Fotos\\2026\\06\\fehlt.jpg", size))
        assertFalse(f.existsAtPath("", size))
    }

    @Test fun unknownPathShapeIsNotFound() {
        val share = FakeShare("Fotos").add("2026\\06\\a.jpg", size, "H")
        assertFalse(elsewhere(share, "irgendwo\\a.jpg", FolderLayout.MONTH_SUFFIX))
    }

    // ---- Eingabeprüfung der Ordnerentscheidung ---------------------------------------------------------
    @Test fun validateDecisionPath() {
        assertEquals("06 Urlaub", FolderNames.validateDecisionPath(" 06 Urlaub ", 6, 1))
        assertEquals("Ägypten\\06 Reise", FolderNames.validateDecisionPath("Ägypten/06 Reise", 6, 2))
        assertNull(FolderNames.validateDecisionPath("Ägypten\\06", 6, 1))      // zu viele Ebenen
        assertNull(FolderNames.validateDecisionPath("06", 6, 2))               // zu wenige Ebenen
        assertNull(FolderNames.validateDecisionPath("Ägypten\\07", 6, 2))      // falsche Monatszahl
        assertNull(FolderNames.validateDecisionPath("06 Urlaub\\Ägypten", 6, 2)) // Monatszahl nur im ersten Segment
        assertNull(FolderNames.validateDecisionPath("", 6, 1))
        assertNull(FolderNames.validateDecisionPath("..\\06", 6, 2))
        assertNull(FolderNames.validateDecisionPath("Ägypten\\\\06", 6, 2))     // leeres Segment
        assertEquals("06 a_b", FolderNames.validateDecisionPath("06 a|b", 6, 1)) // bereinigt
        assertNotNull(FolderNames.sanitizePath("a\\b"))
    }
}
