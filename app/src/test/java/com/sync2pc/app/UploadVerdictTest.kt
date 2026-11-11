package com.sync2pc.app

import com.sync2pc.app.mirror.UploadVerdict
import org.junit.Assert.assertEquals
import org.junit.Test

/** Handy-Datei ändert sich während ihrer Übertragung: zurückstellen statt Fehler (SYNC-20261111-202) */
class UploadVerdictTest {
    private val size = 125_829_120L

    @Test fun unchangedAndCompleteCommits() = assertEquals(UploadVerdict.COMMIT, UploadVerdict.of(size, size, false))

    @Test fun grownFileIsDeferredNotError() =
        assertEquals(UploadVerdict.DEFER, UploadVerdict.of(size, size + 1, true))

    @Test fun shrunkFileIsDeferred() = assertEquals(UploadVerdict.DEFER, UploadVerdict.of(size, size - 10, true))

    /** Gleiche Länge, aber geändert (Änderungsdatum): nie als fertig übernehmen */
    @Test fun sameLengthButChangedIsDeferred() = assertEquals(UploadVerdict.DEFER, UploadVerdict.of(size, size, true))

    /** Ohne Hinweis auf eine Änderung bleibt eine falsche Bytezahl ein echter Fehler */
    @Test fun wrongLengthWithoutChangeIsIncomplete() {
        assertEquals(UploadVerdict.INCOMPLETE, UploadVerdict.of(size, size + 1, false))
        assertEquals(UploadVerdict.INCOMPLETE, UploadVerdict.of(size, size - 1, false))
    }

    @Test fun emptyFile() = assertEquals(UploadVerdict.COMMIT, UploadVerdict.of(0, 0, false))

    @Test fun readLimitIsOneByteOverExpectedSize() = assertEquals(size + 1, UploadVerdict.readLimit(size))

    @Test fun roomNeverExceedsBufferOrLimit() {
        assertEquals(1024, UploadVerdict.room(size, 0, 1024))
        assertEquals(10, UploadVerdict.room(size, size - 9, 1024)) // 9 bis zur Grösse + 1 Byte darüber
        assertEquals(1, UploadVerdict.room(size, size, 1024))
        assertEquals(0, UploadVerdict.room(size, size + 1, 1024))
        assertEquals(0, UploadVerdict.room(size, size + 5, 1024))
        assertEquals(1, UploadVerdict.room(0, 0, 1024)) // leere Datei: ein Byte genügt zum Erkennen
    }
}
