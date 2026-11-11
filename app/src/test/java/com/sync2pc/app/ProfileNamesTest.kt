package com.sync2pc.app

import com.sync2pc.app.data.ProfileNames
import com.sync2pc.app.data.ProfileNames.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SYNC-20261111-235: Profilnamen einmalig, ohne Beachtung der Gross-/Kleinschreibung, Rand bereinigt */
class ProfileNamesTest {
    private val others = listOf("Foto", "Dokumente")

    @Test fun trimsWhitespaceControlAndInvisibleCharsAtTheEdges() {
        assertEquals("Foto", ProfileNames.normalize("  Foto \t"))
        assertEquals("Foto", ProfileNames.normalize("\u0007Foto\n"))
        assertEquals("Foto", ProfileNames.normalize("​Foto​"))
        assertEquals("a  b", ProfileNames.normalize(" a  b "))
    }

    @Test fun emptyNames() {
        assertEquals(Verdict.EMPTY, ProfileNames.verdict("", null, others))
        assertEquals(Verdict.EMPTY, ProfileNames.verdict("  \t\u0001 ", null, others))
    }

    @Test fun caseAndEdgesAreIgnored() {
        assertTrue(ProfileNames.isTaken("foto", others))
        assertTrue(ProfileNames.isTaken("  FOTO ", others))
        assertEquals(Verdict.TAKEN, ProfileNames.verdict("fOtO", null, others))
    }

    @Test fun differentNameIsFree() {
        assertFalse(ProfileNames.isTaken("Fotos", others))
        assertEquals(Verdict.OK, ProfileNames.verdict("Fotos", null, others))
    }

    @Test fun editingOwnProfileIsNotADuplicate() {
        // das bearbeitete Profil ist in [others] nicht enthalten
        assertEquals(Verdict.OK, ProfileNames.verdict("Foto", "Foto", listOf("Dokumente")))
        assertEquals(Verdict.OK, ProfileNames.verdict("FOTO", "Foto", listOf("Dokumente")))
    }

    @Test fun existingDuplicateIsOnlyReportedWhileUnchanged() {
        assertEquals(Verdict.TAKEN_UNCHANGED, ProfileNames.verdict("Foto", "Foto", others))
        assertEquals(Verdict.TAKEN_UNCHANGED, ProfileNames.verdict(" foto ", "Foto", others))
    }

    @Test fun renamingToADuplicateIsBlocked() {
        assertEquals(Verdict.TAKEN, ProfileNames.verdict("Dokumente", "Foto", others))
    }
}
