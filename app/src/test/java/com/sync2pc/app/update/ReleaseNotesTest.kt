package com.sync2pc.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseNotesTest {
    @Test fun fullChangelogLineBecomesLink() {
        val r = ReleaseNotes.clean("**Full Changelog**: https://github.com/a/b/compare/v0.14.6...v0.14.7")
        assertEquals("", r.text)
        assertEquals("https://github.com/a/b/compare/v0.14.6...v0.14.7", r.link)
    }

    @Test fun markdownCharactersAreRemoved() {
        val r = ReleaseNotes.clean("## Neu\n- **Papierkorb** verbessert\n- `Verlauf` löschen\n\n**Full Changelog**: https://x.y/z")
        assertEquals("Neu\n• Papierkorb verbessert\n• Verlauf löschen", r.text)
        assertFalse(r.text.contains("*"))
        assertEquals("https://x.y/z", r.link)
    }

    @Test fun markdownLinkKeepsItsText() {
        val r = ReleaseNotes.clean("Siehe [Anleitung](https://x.y/doc) dazu")
        assertEquals("Siehe Anleitung dazu", r.text)
        assertEquals("https://x.y/doc", r.link)
    }

    @Test fun blankNotesGiveNothing() {
        val r = ReleaseNotes.clean("   ")
        assertEquals("", r.text)
        assertNull(r.link)
    }
}