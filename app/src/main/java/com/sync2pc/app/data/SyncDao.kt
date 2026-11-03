package com.sync2pc.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {

    // ---- Profile ------------------------------------------------------------
    @Query("SELECT * FROM profiles ORDER BY id")
    fun profilesFlow(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles ORDER BY id")
    suspend fun profiles(): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun profile(id: Long): Profile?

    @Insert
    suspend fun insertProfile(p: Profile): Long

    @Update
    suspend fun updateProfile(p: Profile)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileRow(id: Long)

    @Query("DELETE FROM synced_files WHERE profileId = :id")
    suspend fun clearSynced(id: Long)

    @Query("DELETE FROM folder_mappings WHERE profileId = :id")
    suspend fun clearMappings(id: Long)

    @Query("DELETE FROM pending_decisions WHERE profileId = :id")
    suspend fun clearDecisions(id: Long)

    // ---- Synchronisierte Dateien -------------------------------------------
    @Query("SELECT sourceKey, size, dateModified FROM synced_files WHERE profileId = :profileId")
    suspend fun syncedLite(profileId: Long): List<SyncedLite>

    @Query("SELECT * FROM synced_files WHERE profileId = :profileId AND sourceKey = :key")
    suspend fun synced(profileId: Long, key: String): SyncedFile?

    @Query("SELECT * FROM synced_files WHERE sourceKey = :key")
    suspend fun syncedAllProfiles(key: String): List<SyncedFile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSynced(f: SyncedFile)

    @Query("SELECT * FROM synced_files WHERE deleteState = :state")
    suspend fun byDeleteState(state: Int): List<SyncedFile>

    @Query("SELECT COUNT(DISTINCT sourceKey) FROM synced_files WHERE deleteState IN (2, 4)")
    fun queuedDeleteCount(): Flow<Int>

    @Query("SELECT * FROM synced_files WHERE deleteState IN (2, 4)")
    suspend fun queuedDeletes(): List<SyncedFile>

    @Query("UPDATE synced_files SET deleteState = :state WHERE sourceKey = :key AND deleteState IN (1, 2, 4)")
    suspend fun setDeleteStateForKey(key: String, state: Int)

    @Query("UPDATE synced_files SET deleteState = :state WHERE profileId = :profileId AND sourceKey = :key")
    suspend fun setDeleteState(profileId: Long, key: String, state: Int)

    @Query("SELECT COUNT(*) FROM synced_files WHERE profileId = :profileId")
    fun syncedCount(profileId: Long): Flow<Int>

    @Query(
        """SELECT s.sourceKey AS sourceKey, s.remotePath AS remotePath, s.size AS size,
                  m.year AS year, m.countryName AS countryName
           FROM synced_files s LEFT JOIN media_info m ON m.sourceKey = s.sourceKey
           WHERE s.profileId = :profileId"""
    )
    suspend fun statRows(profileId: Long): List<StatRow>

    // ---- Ordner-Zuordnungen / Entscheidungen -------------------------------
    @Query("SELECT * FROM folder_mappings WHERE profileId = :profileId AND groupKey = :groupKey")
    suspend fun mapping(profileId: Long, groupKey: String): FolderMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMapping(m: FolderMapping)

    @Query("DELETE FROM folder_mappings WHERE profileId = :profileId AND groupKey = :groupKey")
    suspend fun deleteMapping(profileId: Long, groupKey: String)

    @Query("SELECT * FROM pending_decisions ORDER BY year DESC, month DESC")
    fun decisionsFlow(): Flow<List<PendingDecision>>

    @Query("SELECT * FROM pending_decisions")
    suspend fun decisions(): List<PendingDecision>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDecision(d: PendingDecision)

    @Query("DELETE FROM pending_decisions WHERE profileId = :profileId AND groupKey = :groupKey")
    suspend fun deleteDecision(profileId: Long, groupKey: String)

    @Query("DELETE FROM pending_decisions WHERE profileId = :profileId")
    suspend fun deleteDecisionsOfProfile(profileId: Long)

    // ---- Vorgemerkte Wiederherstellungen (0.5.0) ------------------------------
    @Query("SELECT * FROM pending_restores WHERE profileId = :profileId ORDER BY id")
    suspend fun pendingRestores(profileId: Long): List<PendingRestore>

    @Query("SELECT COUNT(*) FROM pending_restores")
    fun pendingRestoreCount(): Flow<Int>

    /** Profile mit vorgemerkter Wiederherstellung (Hinweis auf der Startseite, 0.7.0) */
    @Query("SELECT DISTINCT profileId FROM pending_restores")
    fun pendingRestoreProfiles(): Flow<List<Long>>

    @Insert
    suspend fun insertRestores(r: List<PendingRestore>)

    @Query("DELETE FROM pending_restores WHERE id = :id")
    suspend fun deleteRestore(id: Long)

    @Query("DELETE FROM pending_restores WHERE profileId = :profileId")
    suspend fun clearRestores(profileId: Long)

    // ---- Ordner-Spiegelung ---------------------------------------------------
    @Query("SELECT * FROM mirror_state WHERE profileId = :profileId")
    suspend fun mirrorStates(profileId: Long): List<MirrorState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMirrorState(s: MirrorState)

    @Query("DELETE FROM mirror_state WHERE profileId = :profileId AND relPath = :relPath")
    suspend fun deleteMirrorState(profileId: Long, relPath: String)

    @Query("DELETE FROM mirror_state WHERE profileId = :profileId")
    suspend fun clearMirrorState(profileId: Long)

    @Query("SELECT * FROM mirror_conflicts WHERE resolution IS NULL ORDER BY detectedAt DESC")
    fun openConflictsFlow(): Flow<List<MirrorConflict>>

    @Query("SELECT COUNT(*) FROM mirror_conflicts WHERE resolution IS NULL")
    suspend fun openConflictCount(): Int

    @Query("SELECT * FROM mirror_conflicts WHERE profileId = :profileId")
    suspend fun conflicts(profileId: Long): List<MirrorConflict>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConflict(c: MirrorConflict)

    @Query("UPDATE mirror_conflicts SET resolution = :resolution WHERE profileId = :profileId AND relPath = :relPath")
    suspend fun resolveConflict(profileId: Long, relPath: String, resolution: String)

    @Query("DELETE FROM mirror_conflicts WHERE profileId = :profileId AND relPath = :relPath")
    suspend fun deleteConflict(profileId: Long, relPath: String)

    @Query("DELETE FROM mirror_conflicts WHERE profileId = :profileId")
    suspend fun clearConflicts(profileId: Long)

    // ---- Analyse-Cache ------------------------------------------------------
    @Query("SELECT * FROM media_info WHERE sourceKey = :key")
    suspend fun mediaInfo(key: String): MediaInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaInfo(i: MediaInfo)

    // ---- Verlauf / Log ------------------------------------------------------
    @Insert
    suspend fun insertRun(r: SyncRun): Long

    @Update
    suspend fun updateRun(r: SyncRun)

    @Query("SELECT * FROM sync_runs WHERE dryRun = 0 ORDER BY id DESC LIMIT :limit")
    fun runsFlow(limit: Int = 100): Flow<List<SyncRun>>

    @Query("SELECT * FROM sync_runs WHERE dryRun = 0 AND finishedAt IS NOT NULL ORDER BY id DESC LIMIT 1")
    fun lastRunFlow(): Flow<SyncRun?>

    /** Offene echte Läufe (Vorschau-Läufe nicht – die können gerade parallel in der Oberfläche laufen) */
    @Query("SELECT * FROM sync_runs WHERE status = 'RUNNING' AND dryRun = 0")
    suspend fun runningRuns(): List<SyncRun>

    @Query("SELECT MAX(time) FROM log_entries WHERE runId = :runId")
    suspend fun lastLogTime(runId: Long): Long?

    /**
     * Läufe, die beim Beenden der App (Update, Hersteller-Stromsparer, Wegwischen) offen blieben, abschliessen.
     * Aufruf nur, wenn sicher kein Lauf aktiv ist (SyncWorker unter Mutex).
     */
    suspend fun closeDanglingRuns() {
        for (r in runningRuns()) {
            val end = lastLogTime(r.id) ?: r.startedAt
            insertLog(LogEntry(runId = r.id, time = end, level = "W",
                message = "Unterbrochen – die App wurde beendet (z. B. durch ein Update). Der Rest folgt beim nächsten Sync."))
            updateRun(r.copy(
                status = "INTERRUPTED", finishedAt = end,
                message = "Unterbrochen – App wurde beendet (z. B. Update), Rest folgt beim nächsten Sync",
            ))
        }
    }

    @Insert
    suspend fun insertLog(e: LogEntry)

    @Query("SELECT * FROM log_entries WHERE runId = :runId ORDER BY id")
    fun logFlow(runId: Long): Flow<List<LogEntry>>

    @Query("DELETE FROM log_entries WHERE time < :before")
    suspend fun pruneLogs(before: Long)

    @Query("DELETE FROM sync_runs WHERE startedAt < :before")
    suspend fun pruneRuns(before: Long)

    @Query("DELETE FROM log_entries")
    suspend fun clearLogs()

    // ---- Sicherung (Export/Import) ------------------------------------------------
    @Query("SELECT * FROM folder_mappings")
    suspend fun allMappings(): List<FolderMapping>

    @Query("SELECT * FROM synced_files")
    suspend fun allSynced(): List<SyncedFile>

    @Query("SELECT * FROM mirror_state")
    suspend fun allMirrorStates(): List<MirrorState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMappings(list: List<FolderMapping>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncedAll(list: List<SyncedFile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMirrorStates(list: List<MirrorState>)

    // ---- Erinnerung / Speicher freigeben -------------------------------------------
    /** Ende des letzten erfolgreichen (auch teilweise erfolgreichen) Laufs eines Profils */
    @Query("SELECT MAX(finishedAt) FROM sync_runs WHERE profileId = :profileId AND dryRun = 0 AND status IN ('OK', 'PARTIAL')")
    suspend fun lastSuccessAt(profileId: Long): Long?

    @Query("SELECT * FROM synced_files WHERE verified = 1")
    suspend fun verifiedSynced(): List<SyncedFile>

    // ---- Diagnose / Export ----------------------------------------------------
    @Query("SELECT * FROM sync_runs WHERE dryRun = 0 ORDER BY id DESC LIMIT :limit")
    suspend fun recentRuns(limit: Int): List<SyncRun>

    @Query("SELECT * FROM log_entries WHERE runId = :runId ORDER BY id")
    suspend fun logs(runId: Long): List<LogEntry>

    @Query("SELECT COUNT(*) FROM synced_files WHERE profileId = :profileId")
    suspend fun syncedCountNow(profileId: Long): Int

    @Query("SELECT COUNT(*) FROM mirror_state WHERE profileId = :profileId")
    suspend fun mirrorStateCount(profileId: Long): Int

    @Query("SELECT COUNT(*) FROM pending_decisions")
    suspend fun decisionCount(): Int

    @Query("SELECT deleteState AS state, COUNT(*) AS count FROM synced_files GROUP BY deleteState")
    suspend fun deleteStateCounts(): List<StateCount>

    @Query("DELETE FROM sync_runs WHERE status != 'RUNNING'")
    suspend fun clearRuns()
}
