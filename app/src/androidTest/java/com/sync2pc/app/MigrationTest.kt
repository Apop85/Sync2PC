package com.sync2pc.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sync2pc.app.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Datenbank-Migrationen (0.9.4). Läuft auf einem Gerät bzw. Emulator:
 * `gradlew connectedDebugAndroidTest` (der Builder führt ihn nicht aus).
 * Prüft, dass jede Migration zum exportierten Schema passt und bestehende Daten richtig übernommen werden.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val db = "migration-test"
    private val all = arrayOf(
        AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
        AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
        AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9,
    )

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    /** Profil aus Version 1 (0.0.x) übersteht alle Migrationen bis zur aktuellen Version */
    @Test fun fromVersion1ToLatest() {
        helper.createDatabase(db, 1).apply {
            execSQL(
                "INSERT INTO profiles (id, name, enabled, smbPath, smbUser, smbDomain, smbPasswordEnc, sourceFolders, " +
                    "wifiSsids, includeImages, includeVideos, includeSubfolders, deleteMode, verifyUpload) " +
                    "VALUES (1, ' Fotos ', 1, '\\\\pc\\Fotos', 'leo', '', '', 'DCIM/Camera/', 'heim', 1, 1, 1, 'KEEP', 1)"
            )
            close()
        }
        val d = helper.runMigrationsAndValidate(db, 9, true, *all)
        d.query("SELECT name, profileType, trashRetentionDays, massDeletePercent, keepVersions, verifyLevel, folderLayout FROM profiles").use { c ->
            c.moveToFirst()
            assertEquals("Fotos", c.getString(0))
            assertEquals("PHOTOS", c.getString(1))
            assertEquals(30, c.getInt(2))
            assertEquals(20, c.getInt(3))
            assertEquals(1, c.getInt(4))
            assertEquals(1, c.getInt(5)) // Prüfung an -> "Wenig" wie bisher
            assertEquals("MONTH_SUFFIX", c.getString(6)) // Ordnerstruktur V1 wie bisher
        }
        d.close()
    }

    /** 5 -> 6: Prüfstufe aus verifyUpload und deleteMode */
    @Test fun verifyLevelFromVersion5() {
        helper.createDatabase(db, 5).apply {
            fun insert(id: Int, deleteMode: String, verify: Int) = execSQL(
                "INSERT INTO profiles (id, name, enabled, smbPath, smbUser, smbDomain, smbPasswordEnc, sourceFolders, " +
                    "wifiSsids, includeImages, includeVideos, includeSubfolders, deleteMode, verifyUpload, profileType, " +
                    "direction, localTreeUri, remoteSubfolder, propagateDeletes, conflictMode, includePatterns, " +
                    "excludePatterns, remotePollMinutes, trashRetentionDays, massDeletePercent, keepVersions) VALUES " +
                    "($id, 'P$id', 1, '', '', '', '', '', '', 1, 1, 1, '$deleteMode', $verify, 'PHOTOS', 'UPLOAD', '', '', " +
                    "0, 'ASK', '', '', 60, 30, 20, 1)"
            )
            insert(1, "KEEP", 0)   // aus und nichts entfernt -> Aus
            insert(2, "TRASH", 0)  // aus, aber Fotos werden entfernt -> Wenig (Prüfung war Pflicht)
            insert(3, "KEEP", 1)   // an -> Wenig
            close()
        }
        val d = helper.runMigrationsAndValidate(db, 6, true, AppDatabase.MIGRATION_5_6)
        d.query("SELECT id, verifyLevel FROM profiles ORDER BY id").use { c ->
            val levels = buildMap { while (c.moveToNext()) put(c.getInt(0), c.getInt(1)) }
            assertEquals(mapOf(1 to 0, 2 to 1, 3 to 1), levels)
        }
        d.close()
    }

    /** 6 -> 7 (0.12.0, DEC-159): Kennungs-Spalten; bestehende Läufe und Log-Zeilen behalten ihren Text, Kennung NULL */
    @Test fun runCodesFromVersion6() {
        helper.createDatabase(db, 6).apply {
            execSQL(
                "INSERT INTO sync_runs (id, profileId, profileName, `trigger`, dryRun, startedAt, finishedAt, status, " +
                    "uploaded, skipped, failed, waiting, bytes, message) VALUES " +
                    "(1, 1, 'Fotos', 'WLAN verbunden', 0, 1000, 2000, 'OK', 3, 0, 0, 0, 0, '3 übertragen')"
            )
            execSQL("INSERT INTO log_entries (id, runId, time, level, message) VALUES (1, 1, 1500, 'I', '↑ a.jpg → 2026\\06')")
            close()
        }
        val d = helper.runMigrationsAndValidate(db, 7, true, AppDatabase.MIGRATION_6_7)
        d.query("SELECT `trigger`, message, messageCode, triggerCode FROM sync_runs WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("WLAN verbunden", c.getString(0))
            assertEquals("3 übertragen", c.getString(1))
            assertEquals(true, c.isNull(2))
            assertEquals(true, c.isNull(3))
        }
        d.query("SELECT message, code FROM log_entries WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("↑ a.jpg → 2026\\06", c.getString(0))
            assertEquals(true, c.isNull(1))
        }
        // Neue Zeilen mit Kennung lassen sich schreiben
        d.execSQL("UPDATE sync_runs SET messageCode = '{\"k\":\"run_nothing\"}' WHERE id = 1")
        d.close()
    }

    /** 7 -> 8 (SYNC-20261109-047): PC-Erkennung; bestehende Profile behalten ihre Daten, Standard WLAN, keine Kennung */
    @Test fun pcCheckFromVersion7() {
        helper.createDatabase(db, 7).apply {
            execSQL(
                "INSERT INTO profiles (id, name, enabled, smbPath, smbUser, smbDomain, smbPasswordEnc, sourceFolders, " +
                    "wifiSsids, includeImages, includeVideos, includeSubfolders, deleteMode, verifyUpload, profileType, " +
                    "direction, localTreeUri, remoteSubfolder, propagateDeletes, conflictMode, includePatterns, " +
                    "excludePatterns, remotePollMinutes, trashRetentionDays, massDeletePercent, keepVersions, verifyLevel) VALUES " +
                    "(1, 'Fotos', 1, '\\\\pc\\Fotos', 'leo', '', 'enc', 'DCIM/Camera/', 'heim', 1, 1, 1, 'KEEP', 1, 'PHOTOS', " +
                    "'UPLOAD', '', '', 0, 'ASK', '', '', 60, 30, 20, 1, 2)"
            )
            close()
        }
        val d = helper.runMigrationsAndValidate(db, 8, true, AppDatabase.MIGRATION_7_8)
        d.query("SELECT name, smbPath, smbPasswordEnc, wifiSsids, pcCheck, pcGuid, pcGuidHost FROM profiles WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("Fotos", c.getString(0))
            assertEquals("\\\\pc\\Fotos", c.getString(1))
            assertEquals("enc", c.getString(2))
            assertEquals("heim", c.getString(3))
            assertEquals("WIFI", c.getString(4))
            assertEquals("", c.getString(5))
            assertEquals("", c.getString(6))
        }
        d.close()
    }

    /** 8 -> 9 (SYNC-20261109-083): Ordnerstruktur; bestehende Profile behalten alle Daten und bekommen V1, Sync-Stand bleibt */
    @Test fun folderLayoutFromVersion8() {
        helper.createDatabase(db, 8).apply {
            execSQL(
                "INSERT INTO profiles (id, name, enabled, smbPath, smbUser, smbDomain, smbPasswordEnc, sourceFolders, " +
                    "wifiSsids, includeImages, includeVideos, includeSubfolders, deleteMode, verifyUpload, profileType, " +
                    "direction, localTreeUri, remoteSubfolder, propagateDeletes, conflictMode, includePatterns, " +
                    "excludePatterns, remotePollMinutes, trashRetentionDays, massDeletePercent, keepVersions, verifyLevel, " +
                    "pcCheck, pcGuid, pcGuidHost) VALUES " +
                    "(1, 'Fotos', 1, '\\\\pc\\Fotos', 'leo', '', 'enc', 'DCIM/Camera/', 'heim', 1, 1, 1, 'KEEP', 1, 'PHOTOS', " +
                    "'UPLOAD', '', '', 0, 'ASK', '', '', 60, 30, 20, 1, 2, 'WIFI', '', '')"
            )
            execSQL("INSERT INTO folder_mappings (profileId, groupKey, folderName, seen) VALUES (1, '2026/10/EG', '10 - Ägypten', 1)")
            close()
        }
        val d = helper.runMigrationsAndValidate(db, 9, true, AppDatabase.MIGRATION_8_9)
        d.query("SELECT name, smbPath, smbPasswordEnc, wifiSsids, pcCheck, folderLayout FROM profiles WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("Fotos", c.getString(0))
            assertEquals("\\\\pc\\Fotos", c.getString(1))
            assertEquals("enc", c.getString(2))
            assertEquals("heim", c.getString(3))
            assertEquals("WIFI", c.getString(4))
            assertEquals("MONTH_SUFFIX", c.getString(5))
        }
        d.query("SELECT folderName FROM folder_mappings WHERE profileId = 1").use { c ->
            c.moveToFirst()
            assertEquals("10 - Ägypten", c.getString(0))
        }
        d.close()
    }
}
