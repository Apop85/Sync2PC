package com.sync2pc.app

import com.sync2pc.app.ui.invalidFolderName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Namensprüfung für "Ordner anlegen" in der PC-Ordnerauswahl (0.11.3, DEC-192) */
class RemoteFolderNameTest {
    @Test fun validNames() {
        assertFalse(invalidFolderName("Fotos"))
        assertFalse(invalidFolderName("2026-06 Ägypten"))
        assertFalse(invalidFolderName("a.b"))
        assertFalse(invalidFolderName(".hidden"))
        assertFalse(invalidFolderName("a".repeat(255)))
    }

    @Test fun emptyAndDots() {
        assertTrue(invalidFolderName(""))
        assertTrue(invalidFolderName("."))
        assertTrue(invalidFolderName(".."))
    }

    @Test fun forbiddenCharacters() {
        for (c in "\\/:*?\"<>|") assertTrue("Zeichen $c", invalidFolderName("a${c}b"))
        assertTrue(invalidFolderName("a\u0001b"))
    }

    @Test fun trailingDotOrSpaceAndLength() {
        assertTrue(invalidFolderName("Ordner."))
        assertTrue(invalidFolderName("Ordner "))
        assertTrue(invalidFolderName("a".repeat(256)))
    }
}
