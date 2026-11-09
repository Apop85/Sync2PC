package com.sync2pc.app

import com.sync2pc.app.sync.FolderNames
import com.sync2pc.app.sync.FolderNames.DecisionPathProblem
import com.sync2pc.app.sync.PartialHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderNamesTest {
    private val countries = setOf("Ägypten", "Italien", "Schweiz")

    @Test fun monthFromRenamedFolders() {
        assertEquals(6, FolderNames.monthOf("06"))
        assertEquals(6, FolderNames.monthOf("06 - Ägypten"))
        assertEquals(6, FolderNames.monthOf("Urlaub 06 Hurghada"))
        assertEquals(6, FolderNames.monthOf("2026-06 Ferien"))
        assertEquals(12, FolderNames.monthOf("12 - Weihnachten"))
        assertNull(FolderNames.monthOf("Sommer"))
        assertNull(FolderNames.monthOf("13 - Test"))
    }

    @Test fun defaultNames() {
        assertEquals("06", FolderNames.defaultName(6, null))
        assertEquals("06 - Ägypten", FolderNames.defaultName(6, "Ägypten"))
    }

    @Test fun generatedNamesAreNotCandidates() {
        assertTrue(FolderNames.isGeneratedName("06", countries))
        assertTrue(FolderNames.isGeneratedName("06 - Italien", countries))
        assertFalse(FolderNames.isGeneratedName("06 - Hurghada", countries))
        assertFalse(FolderNames.isGeneratedName("06 - Ägypten mit Familie", countries))
    }

    @Test fun conflictNamesStartAtTwo() {
        assertEquals("bild_2.jpg", FolderNames.conflictName("bild.jpg", 2))
        assertEquals("bild_3.jpg", FolderNames.conflictName("bild.jpg", 3))
        assertEquals("archiv.tar_2.gz", FolderNames.conflictName("archiv.tar.gz", 2))
        assertEquals("README_2", FolderNames.conflictName("README", 2))
    }

    @Test fun sameBaseName() {
        assertTrue(FolderNames.sameBaseName("bild.jpg", "bild.jpg"))
        assertTrue(FolderNames.sameBaseName("BILD.JPG", "bild.jpg"))
        assertTrue(FolderNames.sameBaseName("bild_2.jpg", "bild.jpg"))
        assertTrue(FolderNames.sameBaseName("bild_17.jpg", "bild.jpg"))
        assertFalse(FolderNames.sameBaseName("bild_x.jpg", "bild.jpg"))
        assertFalse(FolderNames.sameBaseName("bild2.jpg", "bild.jpg"))
        assertFalse(FolderNames.sameBaseName("bild.png", "bild.jpg"))
    }

    @Test fun sanitize() {
        assertEquals("a_b_c", FolderNames.sanitize("a:b?c"))
        assertEquals("Ordner", FolderNames.sanitize("Ordner. "))
    }

    @Test fun partialHashRegions() {
        assertEquals(listOf(0L to 1000), PartialHash.regions(1000))
        val big = 10L * 1024 * 1024
        val r = PartialHash.regions(big)
        assertEquals(3, r.size)
        assertEquals(big - PartialHash.CHUNK, r[2].first)
    }

    @Test fun partialHashDependsOnSize() {
        val data = ByteArray(300_000) { (it % 251).toByte() }
        val read = { off: Long, len: Int -> data.copyOfRange(off.toInt(), off.toInt() + len) }
        val h1 = PartialHash.compute(data.size.toLong(), read)
        val h2 = PartialHash.compute(data.size.toLong(), read)
        assertEquals(h1, h2)
    }

    @Test fun decisionPathProblemPerTemplate() {
        // V1 (eine Ebene)
        assertNull(FolderNames.decisionPathProblem("06 Urlaub", 6, 1))
        assertEquals(DecisionPathProblem.SEGMENTS, FolderNames.decisionPathProblem("Italien\\06 Urlaub", 6, 1))
        assertEquals(DecisionPathProblem.MONTH, FolderNames.decisionPathProblem("07 Urlaub", 6, 1))
        // V2 mit Land (zwei Ebenen)
        assertNull(FolderNames.decisionPathProblem("Italien\\06 Urlaub", 6, 2))
        assertEquals(DecisionPathProblem.SEGMENTS, FolderNames.decisionPathProblem("06 Urlaub", 6, 2))
        assertEquals(DecisionPathProblem.EMPTY_OR_DOTS, FolderNames.decisionPathProblem("Italien\\", 6, 2))
        assertEquals(DecisionPathProblem.EMPTY_OR_DOTS, FolderNames.decisionPathProblem("..\\06", 6, 2))
        assertEquals(DecisionPathProblem.MONTH, FolderNames.decisionPathProblem("Italien\\05", 6, 2))
        // Konsistenz mit validateDecisionPath
        assertEquals("Italien\\06 Urlaub", FolderNames.validateDecisionPath(" Italien / 06 Urlaub ", 6, 2))
        assertNull(FolderNames.validateDecisionPath("06 Urlaub", 6, 2))
    }
}
