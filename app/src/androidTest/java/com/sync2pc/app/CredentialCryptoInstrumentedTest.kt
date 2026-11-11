package com.sync2pc.app

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sync2pc.app.data.CredentialCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `CredentialCrypto` gegen den echten Android-Keystore (SYNC-20261111-163, S5, teilweise): Rundlauf und Verhalten bei
 * unbrauchbarem Chiffretext. Läuft nur auf Emulator/Gerät (`gradlew.bat connectedDebugAndroidTest`, QA).
 *
 * «Schlüssel im Keystore gelöscht» (R-SMB5, SYNC-20261111-207): Der Test nutzt einen eigenen Schlüsselnamen
 * (`CredentialCrypto.alias`) und löscht nur diesen Testschlüssel; der echte Schlüssel `sync2pc_credentials` und damit die
 * gespeicherten Passwörter der App bleiben unberührt.
 */
@RunWith(AndroidJUnit4::class)
class CredentialCryptoInstrumentedTest {

    @Test
    fun roundTripReturnsOriginal() {
        val enc = CredentialCrypto.encrypt("qa-test-1 äöü")
        assertNotEquals("qa-test-1 äöü", enc)
        assertEquals("qa-test-1 äöü", CredentialCrypto.decrypt(enc))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", CredentialCrypto.encrypt(""))
        assertEquals("", CredentialCrypto.decrypt(""))
    }

    @Test
    fun encryptingTwiceGivesDifferentCiphertexts() {
        assertNotEquals(CredentialCrypto.encrypt("x"), CredentialCrypto.encrypt("x"))
    }

    @Test
    fun garbageDecryptsToEmptyInsteadOfCrashing() {
        assertEquals("", CredentialCrypto.decrypt("kein base64 !!"))
        assertEquals("", CredentialCrypto.decrypt(Base64.encodeToString(ByteArray(5), Base64.NO_WRAP))) // kürzer als der IV
        assertEquals("", CredentialCrypto.decrypt(Base64.encodeToString(ByteArray(40), Base64.NO_WRAP))) // Tag stimmt nicht
    }

    @Test
    fun deletedKeyMakesOldCiphertextUnreadableButEncryptingStillWorks() {
        val testAlias = "sync2pc_credentials_test_" + System.nanoTime()
        val original = CredentialCrypto.alias
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            CredentialCrypto.alias = testAlias
            val enc = CredentialCrypto.encrypt("geheim")
            assertEquals("geheim", CredentialCrypto.decrypt(enc))
            assertTrue(ks.containsAlias(testAlias))

            ks.deleteEntry(testAlias) // simuliert: Schlüssel weg (Geräte-Reset, Sperrbildschirm-Wechsel …)
            assertEquals("", CredentialCrypto.decrypt(enc)) // kein Absturz, Passwort nicht mehr lesbar

            // Neu verschlüsseln legt einen neuen Schlüssel an und funktioniert wieder
            val enc2 = CredentialCrypto.encrypt("neu")
            assertEquals("neu", CredentialCrypto.decrypt(enc2))
            assertEquals("", CredentialCrypto.decrypt(enc)) // das alte bleibt unlesbar
        } finally {
            CredentialCrypto.alias = original
            try { ks.deleteEntry(testAlias) } catch (ignored: Exception) {}
        }
    }

    @Test
    fun realKeyIsNotTouchedByTests() {
        assertEquals("sync2pc_credentials", CredentialCrypto.alias)
    }

    @Test
    fun tamperedCiphertextDecryptsToEmpty() {
        val bytes = Base64.decode(CredentialCrypto.encrypt("geheim"), Base64.NO_WRAP)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0x01).toByte()
        assertEquals("", CredentialCrypto.decrypt(Base64.encodeToString(bytes, Base64.NO_WRAP)))
    }
}
