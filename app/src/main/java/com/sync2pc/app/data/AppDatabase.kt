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
    ],
    version = 3,
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }

        // Wichtig: Kein fallbackToDestructiveMigration() – fehlt eine Migration, soll die App
        // lieber abstürzen, als still alle Daten zu löschen.

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
