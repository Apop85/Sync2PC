package com.sync2pc.app

import com.hierynomus.mssmb2.SMB2Dialect
import com.sync2pc.app.data.SmbEncryption
import com.sync2pc.app.smb.smbConfigFor
import com.sync2pc.app.smb.smbDialectText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Abbildung der Verschlüsselungseinstellung auf die smbj-Konfiguration (SYNC-20261111-144, Testfall S1 aus SYNC-20261110-129):
 * reine Funktion, ohne Gerät und ohne Verbindung.
 */
class SmbConfigTest {

    private val smb3 = setOf(SMB2Dialect.SMB_3_1_1, SMB2Dialect.SMB_3_0_2, SMB2Dialect.SMB_3_0)

    @Test fun offKeepsTheDefaultsNoEncryption() {
        val c = smbConfigFor(SmbEncryption.OFF, 30)
        assertFalse(c.isEncryptData)
        assertFalse(c.isSigningRequired)
        // alle Dialekte wie bisher (smbj-Standard)
        assertTrue(c.supportedDialects.containsAll(smb3))
        assertTrue(c.supportedDialects.any { !it.isSmb3x })
    }

    @Test fun preferredEnablesEncryptionButKeepsAllDialects() {
        val c = smbConfigFor(SmbEncryption.PREFERRED, 30)
        assertTrue(c.isEncryptData)
        assertFalse(c.isSigningRequired)
        assertTrue(c.supportedDialects.containsAll(smb3))
        assertTrue("ohne Pflicht bleiben SMB-2-Server erreichbar", c.supportedDialects.any { !it.isSmb3x })
    }

    @Test fun requiredEnablesEncryptionAndRestrictsToSmb3() {
        val c = smbConfigFor(SmbEncryption.REQUIRED, 30)
        assertTrue(c.isEncryptData)
        assertFalse("Signieren bleibt unverändert", c.isSigningRequired)
        assertEquals(smb3, c.supportedDialects.toSet())
    }

    @Test fun timeoutsAndMultiProtocolAsBefore() {
        // open: 60 s / 90 s, Multi-Protocol-Aushandlung
        val open = smbConfigFor(SmbEncryption.PREFERRED, 60, 90, multiProtocol = true)
        assertEquals(60_000L, open.transactTimeout)
        assertEquals(60_000L, open.readTimeout)
        assertEquals(90_000, open.soTimeout)
        assertTrue(open.isUseMultiProtocolNegotiate)
        // open quick: 30 s / 30 s
        val quick = smbConfigFor(SmbEncryption.OFF, 30, 30, multiProtocol = true)
        assertEquals(30_000, quick.soTimeout)
        // checkPc / Freigabenliste: nur withTimeout(30 s), Standard-Socket-Timeout, keine Multi-Protocol-Aushandlung
        val plain = smbConfigFor(SmbEncryption.OFF, 30)
        val dflt = com.hierynomus.smbj.SmbConfig.createDefaultConfig()
        assertEquals(30_000L, plain.transactTimeout)
        assertEquals(dflt.soTimeout, plain.soTimeout)
        assertEquals(dflt.isUseMultiProtocolNegotiate, plain.isUseMultiProtocolNegotiate)
    }

    @Test fun requiredKeepsTimeoutsAndMultiProtocol() {
        val c = smbConfigFor(SmbEncryption.REQUIRED, 60, 90, multiProtocol = true)
        assertEquals(60_000L, c.transactTimeout)
        assertEquals(90_000, c.soTimeout)
        assertTrue(c.isUseMultiProtocolNegotiate)
    }

    @Test fun dialectText() {
        assertEquals("3.1.1", smbDialectText(SMB2Dialect.SMB_3_1_1))
        assertEquals("3.0.2", smbDialectText(SMB2Dialect.SMB_3_0_2))
        assertEquals("3.0", smbDialectText(SMB2Dialect.SMB_3_0))
        assertEquals("2.1", smbDialectText(SMB2Dialect.SMB_2_1))
        assertEquals("2.0.2", smbDialectText(SMB2Dialect.SMB_2_0_2))
        assertEquals("?", smbDialectText(null))
    }
}
