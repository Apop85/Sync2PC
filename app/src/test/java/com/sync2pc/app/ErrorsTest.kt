package com.sync2pc.app

import com.sync2pc.app.sync.PcDecision
import com.sync2pc.app.sync.PcIdentityException
import com.sync2pc.app.sync.LocalItemNotReadyException
import com.sync2pc.app.sync.friendlyErrorId
import com.sync2pc.app.sync.isNetworkError
import com.sync2pc.app.sync.isPendingOrTrashed
import com.sync2pc.app.sync.shortError
import com.sync2pc.app.smb.SmbEncryptionUnavailableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** N1 (SYNC-20261111-163): Fehlerklassifizierung aus Errors.kt */
class ErrorsTest {
    @Test fun networkExceptionsAreNetworkErrors() {
        assertTrue(isNetworkError(SocketException("Connection reset")))
        assertTrue(isNetworkError(SocketTimeoutException()))
        assertTrue(isNetworkError(UnknownHostException("pc")))
        assertTrue(isNetworkError(EOFException()))
    }

    @Test fun causeChainIsSearched() {
        assertTrue(isNetworkError(IOException("wrapper", SocketException("x"))))
        assertTrue(isNetworkError(RuntimeException("a", IllegalStateException("b", SocketTimeoutException()))))
    }

    @Test fun smbjClosedMessagesAreNetworkErrors() {
        assertTrue(isNetworkError(IllegalStateException("Connection has already been closed")))
        assertTrue(isNetworkError(IllegalStateException("Client is not connected")))
    }

    @Test fun ordinaryErrorsAreNotNetworkErrors() {
        assertFalse(isNetworkError(IOException("disk full")))
        assertFalse(isNetworkError(IllegalArgumentException("bad")))
    }

    @Test fun unknownPcIsNeverANetworkErrorEvenWithNetworkCause() {
        val inner = PcIdentityException(PcDecision.UNKNOWN, "pc")
        assertFalse(isNetworkError(inner))
        // umhüllende Netzwerk-Exception überdeckt es nicht
        assertFalse(isNetworkError(SocketException("wrap").apply { initCause(inner) }))
        assertFalse(isNetworkError(IOException("x", inner)))
    }

    @Test fun missingEncryptionIsNotANetworkError() {
        assertFalse(isNetworkError(SmbEncryptionUnavailableException("pc")))
        assertFalse(isNetworkError(SocketException("wrap").apply { initCause(SmbEncryptionUnavailableException("pc")) }))
    }

    @Test fun pendingOrTrashedDetection() {
        assertTrue(isPendingOrTrashed(LocalItemNotReadyException("x", IOException())))
        assertTrue(isPendingOrTrashed(IOException("w", IllegalStateException("Only owner is able to interact with pending/trashed item"))))
        assertFalse(isPendingOrTrashed(IllegalStateException("something else")))
    }

    @Test fun shortErrorUsesInnermostCause() {
        val e = IOException("outer: wrapper", IllegalStateException("middle", FileNotFoundException2("path: ENOENT (No such file)")))
        assertEquals("FileNotFoundException2: ENOENT (No such file)", shortError(e))
    }

    @Test fun shortErrorWithoutMessageIsClassNameOnly() {
        assertEquals("EOFException", shortError(EOFException()))
    }

    @Test fun shortErrorNeverShowsEncryptionRawException() {
        val msg = shortError(SmbEncryptionUnavailableException("pc"))
        assertFalse(msg.contains("SmbEncryptionUnavailableException"))
    }

    @Test fun friendlyIdsForKnownStatuses() {
        assertEquals(R.string.err_friendly_login, friendlyErrorId(IOException("STATUS_LOGON_FAILURE")))
        assertEquals(R.string.err_friendly_login, friendlyErrorId(IOException("x", RuntimeException("STATUS_ACCOUNT_LOCKED_OUT"))))
        assertEquals(R.string.err_friendly_share_missing, friendlyErrorId(IOException("STATUS_BAD_NETWORK_NAME")))
        assertEquals(R.string.err_friendly_folder_missing, friendlyErrorId(IOException("STATUS_OBJECT_PATH_NOT_FOUND")))
        assertEquals(R.string.err_friendly_access, friendlyErrorId(IOException("STATUS_ACCESS_DENIED")))
        // -232 (6): Zugriff verweigert bei Verschlüsselung «Aus» nennt auch die Verschlüsselung als mögliche Ursache
        assertEquals(R.string.err_friendly_access_unencrypted,
            friendlyErrorId(com.sync2pc.app.smb.SmbShareAccessDeniedException(IOException("STATUS_ACCESS_DENIED"))))
        assertEquals(R.string.err_friendly_pc_full, friendlyErrorId(IOException("STATUS_DISK_FULL")))
        assertEquals(R.string.err_friendly_phone_full, friendlyErrorId(IOException("write failed: ENOSPC (No space left on device)")))
    }

    @Test fun friendlyIdsForNetworkKinds() {
        assertEquals(R.string.err_friendly_unknown_host, friendlyErrorId(UnknownHostException("pc")))
        assertEquals(R.string.err_friendly_unreachable, friendlyErrorId(ConnectException("refused")))
        assertEquals(R.string.err_friendly_unreachable, friendlyErrorId(SocketTimeoutException()))
        assertEquals(R.string.err_friendly_unreachable, friendlyErrorId(IOException("EHOSTUNREACH (No route to host)")))
    }

    @Test fun unknownErrorHasNoFriendlyText() {
        assertNull(friendlyErrorId(IllegalStateException("völlig unbekannt")))
    }

    // eigene Klasse, damit der Klassenname im Text geprüft werden kann
    private class FileNotFoundException2(msg: String) : IOException(msg)
}