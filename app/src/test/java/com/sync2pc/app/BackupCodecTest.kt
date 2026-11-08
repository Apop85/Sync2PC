package com.sync2pc.app

import com.sync2pc.app.backup.BackupCodec
import com.sync2pc.app.backup.BackupContent
import com.sync2pc.app.backup.BackupException
import com.sync2pc.app.data.FolderMapping
import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.SyncedFile
import com.sync2pc.app.sync.SpaceFreer
import com.sync2pc.app.sync.StaleCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private val fast = 1_000 // wenige PBKDF2-Runden, damit der Test schnell ist

    private fun content() = BackupContent(
        appVersion = "0.2.0", createdAt = 1_700_000_000_000,
        settings = mapOf("homeCountryIso" to "CH", "parallelUploads" to 3, "staleWarnEnabled" to true),
        profiles = listOf(
            Profile(id = 7, name = "Fotos", smbPath = "\\\\192.168.3.3\\Fotos", smbUser = "leo",
                smbPasswordEnc = "wird-nie-exportiert", sourceFolders = "DCIM/Camera/", trashRetentionDays = 14),
        ),
        passwords = mapOf(7L to "geheim123"),
        mappings = listOf(FolderMapping(7, "2026/10/EG", "10 - Ägypten", seen = true)),
        synced = listOf(SyncedFile(7, "DCIM/Camera/a.jpg", "content://x/1", 123, 456, "abc", "Fotos\\2026\\10\\a.jpg", 789, true)),
        mirrorStates = listOf(MirrorState(7, "doc.pdf", 1, 2, 3, 4, 5)),
    )

    @Test fun roundTripWithoutPassword() {
        val json = BackupCodec.encode(content(), exportPassword = null, iterations = fast)
        assertFalse(json.contains("geheim123"))
        assertFalse(json.contains("wird-nie-exportiert"))
        val p = BackupCodec.parse(json, null)
        assertFalse(p.encrypted)
        assertNull(p.passwordOk)
        assertTrue(p.content.passwords.isEmpty())
        val prof = p.content.profiles.single()
        assertEquals("Fotos", prof.name)
        assertEquals(14, prof.trashRetentionDays)
        assertEquals("", prof.smbPasswordEnc)
        assertEquals(content().mappings, p.content.mappings)
        assertEquals(content().synced, p.content.synced)
        assertEquals(content().mirrorStates, p.content.mirrorStates)
        assertEquals("CH", p.content.settings["homeCountryIso"])
        assertEquals(3, (p.content.settings["parallelUploads"] as Number).toInt())
    }

    @Test fun passwordsAreEncryptedAndRestored() {
        val json = BackupCodec.encode(content(), exportPassword = "Export!", iterations = fast)
        assertFalse(json.contains("geheim123"))
        val ok = BackupCodec.parse(json, "Export!")
        assertTrue(ok.encrypted)
        assertEquals(true, ok.passwordOk)
        assertEquals("geheim123", ok.content.passwords[7L])
    }

    @Test fun wrongOrMissingPasswordStillImportsProfiles() {
        val json = BackupCodec.encode(content(), exportPassword = "Export!", iterations = fast)
        for (pw in listOf("falsch", null)) {
            val p = BackupCodec.parse(json, pw)
            assertEquals(false, p.passwordOk)
            assertTrue(p.content.passwords.isEmpty())
            assertEquals(1, p.content.profiles.size)
        }
    }

    @Test fun offWifiSwitchRoundTripsAsBoolean() {
        val c = content().copy(settings = mapOf("syncOffWifi" to true))
        val p = BackupCodec.parse(BackupCodec.encode(c, exportPassword = null, iterations = fast), null)
        assertEquals(true, p.content.settings["syncOffWifi"])
        val off = content().copy(settings = mapOf("syncOffWifi" to false))
        assertEquals(false, BackupCodec.parse(BackupCodec.encode(off, null, fast), null).content.settings["syncOffWifi"])
    }

    @Test fun backupWithoutOffWifiKeyLeavesItAbsent() {
        // Ältere Sicherung ohne den Schlüssel: kein Wert -> GlobalSettings.importFrom ändert die Einstellung nicht
        val p = BackupCodec.parse(BackupCodec.encode(content(), exportPassword = null, iterations = fast), null)
        assertFalse(p.content.settings.containsKey("syncOffWifi"))
    }

    @Test fun olderBackupGetsDefaults() {
        val json = """{"format":"leosync-backup","version":1,"profiles":[{"id":1,"name":"Alt","smbPath":"\\\\pc\\x"}]}"""
        val p = BackupCodec.parse(json, null).content.profiles.single()
        assertEquals("Alt", p.name)
        assertEquals(Profile().trashRetentionDays, p.trashRetentionDays)
        assertEquals(Profile().profileType, p.profileType)
    }

    @Test fun rejectsForeignAndFutureFiles() {
        for (json in listOf("kein json", """{"format":"etwas"}""", """{"format":"leosync-backup","version":99}""")) {
            try { BackupCodec.parse(json, null); fail("erwartet: Fehler für $json") } catch (e: BackupException) { /* ok */ }
        }
    }

    @Test fun staleReminderRepeatsEveryThreeDays() {
        val day = 24L * 3600 * 1000
        val now = 100 * day
        assertTrue(StaleCheck.shouldNotify(now, 0))
        assertFalse(StaleCheck.shouldNotify(now, now - 2 * day))
        assertTrue(StaleCheck.shouldNotify(now, now - 3 * day))
    }

    @Test fun livePhotoPairsStayTogether() {
        data class F(val name: String, val group: String, val ok: Boolean)
        val files = listOf(F("a.jpg", "a", true), F("a.mp4", "a", false), F("b.jpg", "b", true), F("c.jpg", "c", true), F("c.mp4", "c", true))
        val kept = SpaceFreer.wholeGroups(files, { it.group }) { it.ok }.map { it.name }.toSet()
        assertEquals(setOf("b.jpg", "c.jpg", "c.mp4"), kept)
    }
}
