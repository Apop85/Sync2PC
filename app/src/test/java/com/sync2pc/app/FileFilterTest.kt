package com.sync2pc.app

import com.sync2pc.app.mirror.FileFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileFilterTest {
    @Test fun emptyAcceptsAllButTemp() {
        val f = FileFilter("", "")
        assertTrue(f.accepts("a/b/c.pdf"))
        assertFalse(f.accepts("a/b/c.pdf.s2ptmp"))
        assertFalse(f.accepts("a/b/c.pdf.s2pold")) // alte Version beim Ersetzen (0.4.1)
        assertFalse(f.accepts(".LeoSync-Bin/2026-11-01/x.txt")) // seit 0.8.0
        assertFalse(f.accepts(".LeoSync-Papierkorb/2026-11-01/x.txt")) // 0.0.12 bis 0.7.x
        assertFalse(f.accepts(".Sync2PC-Papierkorb/2026-11-01/x.txt")) // alter Name bis 0.0.11
    }

    @Test fun whitelist() {
        val f = FileFilter("*.pdf, *.DOCX", "")
        assertTrue(f.accepts("Ordner/Bericht.PDF"))
        assertTrue(f.accepts("brief.docx"))
        assertFalse(f.accepts("bild.jpg"))
    }

    @Test fun blacklistWins() {
        val f = FileFilter("*.pdf", "Temp/**\nentwurf*.pdf")
        assertTrue(f.accepts("Arbeit/bericht.pdf"))
        assertFalse(f.accepts("Temp/bericht.pdf"))
        assertFalse(f.accepts("Arbeit/Entwurf-1.pdf"))
    }

    @Test fun nameOnlyPatternMatchesInSubfolders() {
        val f = FileFilter("", "Thumbs.db")
        assertFalse(f.accepts("a/b/Thumbs.db"))
        assertTrue(f.accepts("a/b/thumbs.dbx"))
    }
}
