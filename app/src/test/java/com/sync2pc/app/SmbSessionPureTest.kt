package com.sync2pc.app

import com.hierynomus.mserref.NtStatus
import com.hierynomus.mssmb2.SMB2MessageCommandCode
import com.hierynomus.mssmb2.SMBApiException
import com.sync2pc.app.smb.SmbSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket

/** N2/N3/N4 (SYNC-20261111-163): reine Teile der SMB-Schicht */
class SmbSessionPureTest {
    private fun api(status: NtStatus) = SMBApiException(status.value, SMB2MessageCommandCode.SMB2_NEGOTIATE, "test", null)

    // ---- N2: Dialekt abgelehnt ----
    @Test fun notSupportedStatusIsDialectRejected() {
        assertTrue(SmbSession.isDialectRejected(api(NtStatus.STATUS_NOT_SUPPORTED)))
    }

    @Test fun dialectRejectionIsFoundInCauseChain() {
        assertTrue(SmbSession.isDialectRejected(IOException("outer", RuntimeException("mid", api(NtStatus.STATUS_NOT_SUPPORTED)))))
    }

    @Test fun otherStatusIsNotDialectRejection() {
        assertFalse(SmbSession.isDialectRejected(api(NtStatus.STATUS_LOGON_FAILURE)))
        assertFalse(SmbSession.isDialectRejected(IOException("plain")))
    }

    // ---- N3: Freigaben-Filter ----
    @Test fun onlyDiskSharesWithoutHiddenOnesAreListedSorted() {
        val raw = listOf(
            "Zeta" to 0, "alpha" to 0, "C$" to 0x80000000.toInt(), "ADMIN$" to 0x80000000.toInt(),
            "IPC$" to 0x80000003.toInt(), "Printer" to 1, "Hidden$" to 0, "Beta" to 0,
        )
        assertEquals(listOf("alpha", "Beta", "Zeta"), SmbSession.filterShares(raw))
    }

    @Test fun specialFlagBitsAreMaskedOut() {
        // Typ 0 mit gesetztem Spezial-Bit (0x80000000) bleibt eine Disk-Freigabe, solange der Name nicht auf $ endet
        assertEquals(listOf("Daten"), SmbSession.filterShares(listOf("Daten" to 0x80000000.toInt())))
    }

    @Test fun noSharesGivesEmptyList() {
        assertEquals(emptyList<String>(), SmbSession.filterShares(emptyList()))
    }

    // ---- N4: Erreichbarkeit mit Port ----
    @Test fun reachableWhenPortListens() {
        ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress()).use { s ->
            assertTrue(SmbSession.isReachable("127.0.0.1", 2000, s.localPort))
        }
    }

    @Test fun notReachableWhenPortClosed() {
        val port = ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress()).use { it.localPort }
        assertFalse(SmbSession.isReachable("127.0.0.1", 500, port))
    }
}