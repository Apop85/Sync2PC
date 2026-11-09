package com.sync2pc.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Profile::class, SyncedFile::class, FolderMapping::class, PendingDecision::class,
        MediaInfo::class, SyncRun::class, LogEntry::class, MirrorState::class, MirrorConflict::class,
        PendingRestore::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): SyncDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "sync2pc.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build().also { instance = it }
            }

        // Wichtig: Kein fallbackToDestructiveMigration() – fehlt eine Migration, soll die App
        // lieber abstürzen, als still alle Daten zu löschen.

        /** SYNC-20261109-083: Ordnerstruktur je Fotoprofil. Bestehende Profile = V1 (MONTH_SUFFIX), Verhalten unverändert (DEC-084). */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN folderLayout TEXT NOT NULL DEFAULT 'MONTH_SUFFIX'")
            }
        }

        /** SYNC-20261109-047: PC-Erkennung per Kennung. Nur neue Spalten mit Standardwerten; bestehende Profile bleiben bei WLAN-Prüfung. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN pcCheck TEXT NOT NULL DEFAULT 'WIFI'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN pcGuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE profiles ADD COLUMN pcGuidHost TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * 0.12.0 (DEC-159): Verlauf mit Kennungen statt fertiger Texte. Nur neue, nullable Spalten (DEC-084):
         * bestehende Einträge behalten ihren Text (Kennung NULL → Anzeige des gespeicherten Textes).
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sync_runs ADD COLUMN messageCode TEXT")
                db.execSQL("ALTER TABLE sync_runs ADD COLUMN triggerCode TEXT")
                db.execSQL("ALTER TABLE log_entries ADD COLUMN code TEXT")
            }
        }

        /**
         * 0.9.0: Prüfstufe nach dem Hochladen. Bestehende Profile behalten ihr bisheriges Verhalten:
         * Prüfung an = "Wenig" (wie bisher 192 KB), aus = "Aus" (nur wenn nichts gelöscht wird).
         * Zusätzlich Leerzeichen am Anfang/Ende der Profilnamen entfernen.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN verifyLevel INTEGER NOT NULL DEFAULT 2")
                db.execSQL(
                    "UPDATE profiles SET verifyLevel = CASE WHEN verifyUpload = 0 AND deleteMode = 'KEEP' THEN 0 ELSE 1 END"
                )
                db.execSQL("UPDATE profiles SET name = TRIM(name)")
            }
        }

        /** 0.5.0: Versionsschutz pro Profil + vorgemerkte Wiederherstellungen */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN keepVersions INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_restores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, `side` TEXT NOT NULL, `trashPath` TEXT NOT NULL, " +
                        "`origPath` TEXT NOT NULL, `requestedAt` INTEGER NOT NULL)"
                )
            }
        }

        /** 0.4.2: Schutzbremse – Grenze für viele Löschungen pro Profil (Prozent) */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN massDeletePercent INTEGER NOT NULL DEFAULT 20")
            }
        }

        /** 0.1.0: Papierkorb-Frist pro Profil */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN trashRetentionDays INTEGER NOT NULL DEFAULT 30")
            }
        }

        /** 0.0.7: Profiltyp, Richtung, Filter, Unterordner + Tabellen für die Ordner-Spiegelung */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cols = listOf(
                    "profileType TEXT NOT NULL DEFAULT 'PHOTOS'",
                    "direction TEXT NOT NULL DEFAULT 'UPLOAD'",
                    "localTreeUri TEXT NOT NULL DEFAULT ''",
                    "remoteSubfolder TEXT NOT NULL DEFAULT ''",
                    "propagateDeletes INTEGER NOT NULL DEFAULT 0",
                    "conflictMode TEXT NOT NULL DEFAULT 'ASK'",
                    "includePatterns TEXT NOT NULL DEFAULT ''",
                    "excludePatterns TEXT NOT NULL DEFAULT ''",
                    "remotePollMinutes INTEGER NOT NULL DEFAULT 60",
                )
                cols.forEach { db.execSQL("ALTER TABLE profiles ADD COLUMN $it") }
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mirror_state` (`profileId` INTEGER NOT NULL, `relPath` TEXT NOT NULL, " +
                        "`localSize` INTEGER NOT NULL, `localModified` INTEGER NOT NULL, `remoteSize` INTEGER NOT NULL, " +
                        "`remoteModified` INTEGER NOT NULL, `syncedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `relPath`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mirror_conflicts` (`profileId` INTEGER NOT NULL, `relPath` TEXT NOT NULL, " +
                        "`localSize` INTEGER NOT NULL, `localModified` INTEGER NOT NULL, `remoteSize` INTEGER NOT NULL, " +
                        "`remoteModified` INTEGER NOT NULL, `detectedAt` INTEGER NOT NULL, `resolution` TEXT, " +
                        "PRIMARY KEY(`profileId`, `relPath`))"
                )
            }
        }
    }
}
