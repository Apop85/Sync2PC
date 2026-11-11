package com.sync2pc.app

import com.sync2pc.app.mirror.ConflictName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Namenswahl «Beide behalten» (SYNC-20261111-171) */
class ConflictNameTest {

    @Test fun firstFreeNameKeepsExtension() {
        assertEquals("a_conflict-phone.txt", ConflictName.free("a.txt") { false })
    }

    @Test fun noExtension() {
        assertEquals("notes_conflict-phone", ConflictName.free("notes") { false })
    }

    @Test fun hiddenFileWithoutBaseKeepsWholeName() {
        // Punkt am Anfang: kein Zusatz vor der "Endung"
        assertEquals(".gitignore_conflict-phone", ConflictName.free(".gitignore") { false })
    }

    @Test fun lastDotSplitsExtension() {
        assertEquals("a.tar_conflict-phone.gz", ConflictName.free("a.tar.gz") { false })
    }

    @Test fun takenNamesGetNumber() {
        val taken = setOf("a_conflict-phone.txt", "a_conflict-phone-2.txt")
        assertEquals("a_conflict-phone-3.txt", ConflictName.free("a.txt") { it in taken })
    }

    @Test fun takenIsAskedForExactCandidates() {
        val asked = ArrayList<String>()
        ConflictName.free("a.txt") { asked += it; asked.size < 3 }
        assertEquals(listOf("a_conflict-phone.txt", "a_conflict-phone-2.txt", "a_conflict-phone-3.txt"), asked)
    }

    @Test fun exhaustedGivesNull() {
        var asked = 0
        assertNull(ConflictName.free("a.txt") { asked++; true })
        assertEquals(ConflictName.MAX_TRIES, asked)
    }

    @Test fun sameNameIgnoresCase() {
        assertTrue(ConflictName.sameName("a_conflict-phone.txt", "A_Conflict-Phone.TXT"))
    }

    @Test fun providerSuffixIsDetected() {
        assertFalse(ConflictName.sameName("a_conflict-phone.txt", "a_conflict-phone (1).txt"))
        assertFalse(ConflictName.sameName("a_conflict-phone.txt", null))
    }
}
