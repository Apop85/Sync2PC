package com.sync2pc.app

import com.sync2pc.app.mirror.PurgeSides
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashEntry
import com.sync2pc.app.mirror.TrashViewRules
import com.sync2pc.app.mirror.TrashViewRules.Empty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Papierkorb-Ansicht (SYNC-20261112-277): Seitenfilter, Aufheben der Auswahl bei Filterwechsel (Löschen und Wiederherstellen
 * wirken nur auf Sichtbares), Zähler je Seite, Leerzustand und Angaben im Wiederherstellen-Dialog.
 * Nicht abgedeckt: die Compose-Oberfläche (TrashScreen); sie ist nur am Gerät prüfbar.
 */
class TrashViewRulesTest {

    private fun e(side: Side, orig: String, date: String = "2026-11-01") =
        TrashEntry(side, "@LeoSync-Bin/$date/$orig#${side.name}", orig, date, 10, 1)

    private val p1 = e(Side.LOCAL, "a.txt")
    private val p2 = e(Side.LOCAL, "b.txt", "2026-11-02")
    private val c1 = e(Side.REMOTE, "a.txt")
    private val c2 = e(Side.REMOTE, "c.txt", "2026-11-02")
    private val all = listOf(p1, p2, c1, c2)

    // ---- Filter ----------------------------------------------------------------------------

    @Test
    fun defaultBothShowsEverything() {
        assertEquals(all, TrashViewRules.visible(all, PurgeSides.BOTH, null))
    }

    @Test
    fun phoneOnlyAndPcOnlyShowOnlyThatSide() {
        assertEquals(listOf(p1, p2), TrashViewRules.visible(all, PurgeSides.LOCAL, null))
        assertEquals(listOf(c1, c2), TrashViewRules.visible(all, PurgeSides.REMOTE, null))
    }

    @Test
    fun dayAndSideCombine() {
        assertEquals(listOf(p2), TrashViewRules.visible(all, PurgeSides.LOCAL, "2026-11-02"))
        assertEquals(listOf(p2, c2), TrashViewRules.visible(all, PurgeSides.BOTH, "2026-11-02"))
        assertTrue(TrashViewRules.visible(listOf(p1, p2), PurgeSides.REMOTE, null).isEmpty())
    }

    // ---- Auswahl bei Filterwechsel ---------------------------------------------------------

    @Test
    fun selectionStaysWhenEverythingIsStillVisible() {
        val r = TrashViewRules.pruneSelection(listOf(p1, p2), TrashViewRules.visible(all, PurgeSides.LOCAL, null))
        assertEquals(listOf(p1, p2), r.selection)
        assertFalse(r.cleared)
    }

    @Test
    fun selectionIsClearedCompletelyWhenAnyEntryGetsHidden() {
        // Phone- und PC-Eintrag gewählt, Filter «Nur Handy»: nichts davon darf unbemerkt gelöscht/wiederhergestellt werden
        val r = TrashViewRules.pruneSelection(listOf(p1, c2), TrashViewRules.visible(all, PurgeSides.LOCAL, null))
        assertTrue(r.selection.isEmpty())
        assertTrue(r.cleared)
    }

    @Test
    fun dayChangeAlsoClearsSelectionWithHiddenEntries() {
        val r = TrashViewRules.pruneSelection(listOf(p1, p2), TrashViewRules.visible(all, PurgeSides.BOTH, "2026-11-02"))
        assertTrue(r.cleared)
        assertTrue(r.selection.isEmpty())
    }

    @Test
    fun emptySelectionIsNotReportedAsCleared() {
        val r = TrashViewRules.pruneSelection(emptyList(), TrashViewRules.visible(all, PurgeSides.LOCAL, null))
        assertFalse(r.cleared)
    }

    @Test
    fun sameFileOnBothSidesIsToldApartBySide() {
        // gleicher Pfad im Papierkorb von Handy und PC: nur der des Handys ist gewählt, Filter «Nur Handy» behält die Auswahl
        val phone = e(Side.LOCAL, "x.txt")
        val pc = phone.copy(side = Side.REMOTE)
        val r = TrashViewRules.pruneSelection(listOf(phone), TrashViewRules.visible(listOf(phone, pc), PurgeSides.LOCAL, null))
        assertFalse(r.cleared)
        val r2 = TrashViewRules.pruneSelection(listOf(pc), TrashViewRules.visible(listOf(phone, pc), PurgeSides.LOCAL, null))
        assertTrue(r2.cleared)
    }

    // ---- Zähler ----------------------------------------------------------------------------

    @Test
    fun countsAndSidesText() {
        val c = TrashViewRules.sideCounts(listOf(p1, p2, c1))
        assertEquals(2, c.phone); assertEquals(1, c.pc)
        assertEquals("Handy 2, PC 1", TrashViewRules.sidesText(c, "Handy", "PC"))
        assertEquals("Handy 2", TrashViewRules.sidesText(TrashViewRules.sideCounts(listOf(p1, p2)), "Handy", "PC"))
        assertEquals("PC 1", TrashViewRules.sidesText(TrashViewRules.sideCounts(listOf(c1)), "Handy", "PC"))
        assertEquals("", TrashViewRules.sidesText(TrashViewRules.sideCounts(emptyList()), "Handy", "PC"))
    }

    // ---- Leerzustand -----------------------------------------------------------------------

    @Test
    fun emptyKindPerFilter() {
        assertEquals(Empty.NONE, TrashViewRules.emptyKind(all, PurgeSides.BOTH, true))
        assertEquals(Empty.PHONE, TrashViewRules.emptyKind(emptyList(), PurgeSides.LOCAL, true))
        assertEquals(Empty.PC, TrashViewRules.emptyKind(emptyList(), PurgeSides.REMOTE, true))
    }

    @Test
    fun emptyKindWithEntriesOrBothIsNone() {
        assertEquals(Empty.NONE, TrashViewRules.emptyKind(listOf(c1), PurgeSides.REMOTE, false))
        assertEquals(Empty.NONE, TrashViewRules.emptyKind(emptyList(), PurgeSides.BOTH, false))
        assertTrue(TrashViewRules.visible(emptyList(), PurgeSides.LOCAL, null).isEmpty())
    }

    @Test
    fun selectionOfEntriesGoneAfterReloadIsCleared() {
        // «Neu einlesen»: PC nicht mehr erreichbar, PC-Eintrag nicht mehr in der Liste -> Auswahl wird aufgehoben
        val r = TrashViewRules.pruneSelection(listOf(p1, c1), listOf(p1, p2))
        assertTrue(r.cleared)
        assertTrue(r.selection.isEmpty())
    }

    @Test
    fun pcOnlyWithUnreachablePcNeverClaimsEmpty() {
        assertEquals(Empty.PC_UNREACHABLE, TrashViewRules.emptyKind(emptyList(), PurgeSides.REMOTE, false))
        // Handy-Filter ist von der Erreichbarkeit des PCs unabhängig
        assertEquals(Empty.PHONE, TrashViewRules.emptyKind(emptyList(), PurgeSides.LOCAL, false))
    }

    // ---- Wiederherstellen-Dialog -----------------------------------------------------------

    @Test
    fun restorePlanCountsReplacedAndMissingFiles() {
        val plan = TrashViewRules.restorePlan(listOf(p1, p2), setOf("a.txt"))
        assertEquals(2, plan.files)
        assertEquals(1, plan.replaces)
        assertEquals(1, plan.missing)
    }

    @Test
    fun restorePlanNamesOnlyUpToThreeFiles() {
        val three = listOf(e(Side.LOCAL, "1.txt"), e(Side.LOCAL, "2.txt"), e(Side.LOCAL, "3.txt"))
        assertEquals(listOf("1.txt", "2.txt", "3.txt"), TrashViewRules.restorePlan(three, emptySet()).names)
        val four = three + e(Side.LOCAL, "4.txt")
        assertTrue(TrashViewRules.restorePlan(four, emptySet()).names.isEmpty())
    }

    @Test
    fun restorePlanCompareIgnoresCaseOfExistingKeys() {
        val plan = TrashViewRules.restorePlan(listOf(e(Side.LOCAL, "Docs/Report.TXT")), setOf("docs/report.txt"))
        assertEquals(1, plan.replaces)
        assertEquals(0, plan.missing)
    }

    @Test
    fun restorePlanCountsAFileOnce() {
        val plan = TrashViewRules.restorePlan(listOf(p1, c1), emptySet())
        assertEquals(1, plan.files)
    }
}
