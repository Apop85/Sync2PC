package com.sync2pc.app

import com.sync2pc.app.sync.LocalItemNotReadyException
import com.sync2pc.app.sync.isNetworkError
import com.sync2pc.app.sync.isPendingOrTrashed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Foto-Scan: pending/trashed-Einträge sind kein Dateifehler (SYNC-20261109-050) */
class PendingTrashedErrorTest {

    private val msg = "Only owner is able to interact with pending/trashed item"

    @Test fun providerMessageIsRecognized() {
        assertTrue(isPendingOrTrashed(IllegalStateException(msg)))
    }

    @Test fun wrappedExceptionIsRecognized() {
        val e = LocalItemNotReadyException("x.jpg", IllegalStateException(msg))
        assertTrue(isPendingOrTrashed(e))
        assertTrue(isPendingOrTrashed(RuntimeException("outer", e)))
    }

    @Test fun otherErrorsAreNotAffected() {
        assertFalse(isPendingOrTrashed(IllegalStateException("anderer Fehler")))
        assertFalse(isPendingOrTrashed(java.io.IOException("Datei nicht lesbar")))
        assertFalse(isPendingOrTrashed(java.net.SocketException("reset")))
    }

    @Test fun notTreatedAsNetworkError() {
        assertFalse(isNetworkError(LocalItemNotReadyException("x.jpg", IllegalStateException(msg))))
    }
}
