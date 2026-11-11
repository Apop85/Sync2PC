package com.sync2pc.app

import com.sync2pc.app.ui.ConflictUi
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

    @Test fun secondsAreAddedOnce() {
        assertEquals("dd.MM.yyyy HH:mm:ss", ConflictUi.patternWithSeconds("dd.MM.yyyy HH:mm"))
        assertEquals("yyyy-MM-dd HH:mm:ss", ConflictUi.patternWithSeconds("yyyy-MM-dd HH:mm:ss"))
    }
}
