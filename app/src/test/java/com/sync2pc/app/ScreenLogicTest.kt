package com.sync2pc.app

import com.sync2pc.app.l10n.TimeFormat
import com.sync2pc.app.ui.DecisionChoice
import com.sync2pc.app.ui.PreviewGroupLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SYNC-20261110-135 V4/O1 und -136 K1: reine Logik von Vorschau, Ordnerauswahl und Konflikten */
class ScreenLogicTest {
    private val months = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")

    @Test fun openGroupLabelIsReadable() {
        val g = PreviewGroupLabel.parse("2026\\? (12)")!!
        assertEquals("2026 – Dezember", PreviewGroupLabel.title(g, months))
    }

    @Test fun openGroupWithCountryAndSuffix() {
        val g = PreviewGroupLabel.parse("2026\\? (Spanien\\12 - )")!!
        assertEquals("2026 – Spanien\\Dezember", PreviewGroupLabel.title(g, months))
    }

    @Test fun normalFolderIsNotAnOpenGroup() {
        assertNull(PreviewGroupLabel.parse("2026\\12 Weihnacht"))
    }

    @Test fun noPreselectionWithSeveralCandidates() {
        assertNull(DecisionChoice.effective(listOf("12 A", "12 B"), "12", null, null))
        assertNull(DecisionChoice.effective(listOf("12 Spanien", "12 B"), "12", "Spanien", null))
    }

    @Test fun preselectionKeptForSingleCandidateOrNone() {
        assertEquals("12 A", DecisionChoice.effective(listOf("12 A"), "12", null, null))
        assertEquals("12", DecisionChoice.effective(emptyList(), "12", null, null))
    }

    @Test fun explicitChoiceWins() {
        assertEquals("12 B", DecisionChoice.effective(listOf("12 A", "12 B"), "12", null, "12 B"))
    }

    /** SYNC-20261111-233: Skelett folgt 12 h/24 h und Sekunden; ohne Android gilt das feste Ersatzformat */
    @Test fun timeSkeletonFollowsSystemSettings() {
        assertEquals("yMdHm", TimeFormat.skeleton(withDate = true, is24h = true, seconds = false))
        assertEquals("yMdhms", TimeFormat.skeleton(withDate = true, is24h = false, seconds = true))
        assertEquals("Hm", TimeFormat.skeleton(withDate = false, is24h = true, seconds = false))
        assertEquals("hm", TimeFormat.skeleton(withDate = false, is24h = false, seconds = false))
    }

    @Test fun timeFallbackWithoutAndroid() {
        val ms = java.util.GregorianCalendar(2026, 10, 11, 14, 30, 5).timeInMillis
        assertEquals("2026-11-11 14:30", TimeFormat.dateTime(ms))
        assertEquals("2026-11-11 14:30:05", TimeFormat.dateTime(ms, seconds = true))
        assertEquals("14:30", TimeFormat.clock(ms))
        assertEquals("2026-11-11", TimeFormat.date(ms))
        assertEquals("11-11", TimeFormat.dayMonth(ms))
    }
}
