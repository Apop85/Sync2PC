package com.sync2pc.app

import com.sync2pc.app.backup.BackupCodec
import com.sync2pc.app.backup.BackupContent
import com.sync2pc.app.data.DeleteMode
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.VerifyLevel
import com.sync2pc.app.sync.PartialHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prüfstufen nach dem Hochladen (0.9.0): Bereiche je Stufe, wirksame Stufe im Profil, Sicherung */
class VerifyLevelTest {
    private val mb = 1024L * 1024

    @Test fun lowAndMediumMatchExistingHashes() {
        for (size in listOf(100L, 3L * PartialHash.CHUNK, 50 * mb, 5_000 * mb)) {
            assertEquals(PartialHash.regions(size), PartialHash.levelRegions(PartialHash.LEVEL_LOW, size))
            assertEquals(PartialHash.thoroughRegions(size), PartialHash.levelRegions(PartialHash.LEVEL_MEDIUM, size))
        }
        // "Wenig" ergibt dieselbe Prüfsumme wie bisher (bestehende Sync-Stände bleiben gültig)
        val data = ByteArray(500_000) { (it % 251).toByte() }
        val read: (Long, Int) -> ByteArray = { off, len -> data.copyOfRange(off.toInt(), off.toInt() + len) }
        assertEquals(PartialHash.compute(data.size.toLong(), read),
            PartialHash.forLevel(PartialHash.LEVEL_LOW, data.size.toLong(), read))
    }

    @Test fun offReadsNothing() {
        assertTrue(PartialHash.levelRegions(PartialHash.LEVEL_OFF, 10 * mb).isEmpty())
    }

    @Test fun highReadsWholeFileUpTo256MbThenSamples() {
        val small = 200 * mb
        assertEquals(small, PartialHash.levelRegions(PartialHash.LEVEL_HIGH, small).sumOf { it.second.toLong() })
        val big = 2_000 * mb
        val r = PartialHash.levelRegions(PartialHash.LEVEL_HIGH, big)
        assertEquals(PartialHash.HIGH_SAMPLES, r.size)
        assertEquals(0L, r.first().first)
        assertEquals(big - PartialHash.CHUNK, r.last().first)
    }

    @Test fun fullCoversEveryByteWithoutGaps() {
        val size = 3_000 * mb + 12_345
        val r = PartialHash.levelRegions(PartialHash.LEVEL_FULL, size)
        assertEquals(size, r.sumOf { it.second.toLong() })
        var expected = 0L
        for ((off, len) in r) { assertEquals(expected, off); expected += len }
    }

    @Test fun offIsRaisedWhenFilesAreRemoved() {
        val keep = Profile(verifyLevel = VerifyLevel.OFF.code, deleteMode = DeleteMode.KEEP.name)
        assertEquals(VerifyLevel.OFF, keep.verify())
        assertFalse(keep.mustVerify())
        val trash = keep.copy(deleteMode = DeleteMode.TRASH.name)
        assertEquals(VerifyLevel.MEDIUM, trash.verify())
        assertTrue(trash.mustVerify())
        // Unbekannter Code -> Standard
        assertEquals(VerifyLevel.MEDIUM, Profile(verifyLevel = 99).verify())
    }

    private fun json(p: Profile) = BackupCodec.encode(
        BackupContent(appVersion = "0.9.0", createdAt = 1L, settings = emptyMap(), profiles = listOf(p),
            passwords = emptyMap(), mappings = emptyList(), synced = emptyList(), mirrorStates = emptyList()),
        exportPassword = null, iterations = 1_000,
    )

    private fun parse(json: String) = BackupCodec.parse(json, null).content.profiles.single()

    @Test fun backupKeepsLevelAndTrimsName() {
        val p = parse(json(Profile(id = 3, name = "  Fotos ", verifyLevel = VerifyLevel.FULL.code)))
        assertEquals(VerifyLevel.FULL.code, p.verifyLevel)
        assertEquals("Fotos", p.name)
    }

    @Test fun oldBackupWithoutLevelUsesVerifyUpload() {
        val modern = json(Profile(id = 3, name = "Fotos", verifyLevel = VerifyLevel.HIGH.code))
        // Sicherung wie vor 0.9.0: kein verifyLevel
        val old = modern.replace(Regex("\"verifyLevel\":\\d+,"), "").replace(Regex(",\"verifyLevel\":\\d+"), "")
        assertFalse(old.contains("verifyLevel"))
        assertEquals(VerifyLevel.LOW.code, parse(old).verifyLevel)
        val oldOff = old.replace("\"verifyUpload\":true", "\"verifyUpload\":false")
        assertEquals(VerifyLevel.OFF.code, parse(oldOff).verifyLevel)
    }
}
