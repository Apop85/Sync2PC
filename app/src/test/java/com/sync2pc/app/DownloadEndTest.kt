package com.sync2pc.app

import com.sync2pc.app.smb.DownloadEnd
import com.sync2pc.app.smb.DownloadEnd.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

/** SYNC-20261111-232 (2): vorzeitiges Ende eines Downloads ist eine Unterbrechung, kein fertiger Download */
class DownloadEndTest {
    @Test fun allBytesReadIsComplete() {
        assertEquals(Kind.COMPLETE, DownloadEnd.classify(10_485_760, 10_485_760, 10_485_760, true))
        assertEquals(Kind.COMPLETE, DownloadEnd.classify(0, 0, null, false))
    }

    @Test fun earlyEndWithProgressAndUnchangedRemoteIsInterrupted() {
        // QA-Befund: 1769472 von 10485760 Bytes, Datei auf dem PC unverändert gross
        assertEquals(Kind.INTERRUPTED, DownloadEnd.classify(1_769_472, 10_485_760, 10_485_760, true))
        assertEquals(Kind.INTERRUPTED, DownloadEnd.classify(1_769_472, 10_485_760, null, true))
    }

    @Test fun earlyEndWithShrunkRemoteIsChanged() {
        assertEquals(Kind.CHANGED, DownloadEnd.classify(1_769_472, 10_485_760, 1_769_472, true))
    }

    @Test fun moreBytesThanExpectedIsChanged() {
        assertEquals(Kind.CHANGED, DownloadEnd.classify(11_000_000, 10_485_760, 11_000_000, true))
    }

    /** Review: dauerhaft zu kurz lesbare Datei darf nicht jeden Lauf als «unterbrochen» beenden (Aushungern) */
    @Test fun earlyEndWithoutProgressIsASingleFileError() {
        assertEquals(Kind.CHANGED, DownloadEnd.classify(1_769_472, 10_485_760, 10_485_760, false))
        assertEquals(Kind.CHANGED, DownloadEnd.classify(1_769_472, 10_485_760, null, false))
    }
}
