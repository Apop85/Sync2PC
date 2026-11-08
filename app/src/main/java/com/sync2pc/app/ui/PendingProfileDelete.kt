package com.sync2pc.app.ui

import android.content.Context
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.mirror.GuardStore
import com.sync2pc.app.mirror.PurgeStore
import com.sync2pc.app.mirror.RestoreFailures
import com.sync2pc.app.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Profil löschen mit "Rückgängig" (0.11.0). Das Profil wird zuerst nur vorgemerkt (ausgeblendet, keine
 * Synchronisierung) und erst endgültig gelöscht, wenn der Hinweis auf der Startseite abläuft – spätestens nach
 * [FALLBACK_MS]. Wird die App vorher beendet, bleibt das Profil erhalten (sichere Richtung).
 */
object PendingProfileDelete {
    data class Pending(val id: Long, val name: String)

    const val FALLBACK_MS = 15_000L
    val pending = MutableStateFlow<Pending?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    fun request(ctx: Context, id: Long, name: String) {
        val app = ctx.applicationContext
        // Ein anderes vorgemerktes Profil zuerst endgültig löschen
        pending.value?.takeIf { it.id != id }?.let { old -> job?.cancel(); scope.launch { delete(app, old.id) } }
        pending.value = Pending(id, name)
        job?.cancel()
        job = scope.launch { delay(FALLBACK_MS); commit(app) }
    }

    fun undo() {
        job?.cancel(); job = null
        pending.value = null
    }

    /** Hinweis abgelaufen bzw. weggewischt: jetzt endgültig löschen */
    fun commit(ctx: Context) {
        val p = pending.value ?: return
        job?.cancel(); job = null
        pending.value = null
        scope.launch { delete(ctx.applicationContext, p.id) }
    }

    /** Profil samt Sync-Stand und Vormerkungen löschen (bis 0.10.x direkt in ProfileScreen) */
    suspend fun delete(ctx: Context, profileId: Long) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(ctx).dao()
        dao.clearSynced(profileId); dao.clearMappings(profileId)
        dao.clearDecisions(profileId); dao.clearMirrorState(profileId)
        dao.clearConflicts(profileId); dao.deleteProfileRow(profileId); dao.clearRestores(profileId)
        GuardStore(ctx).forget(profileId)
        PurgeStore(ctx).forget(profileId)
        RestoreFailures(ctx).forget(profileId)
        GlobalSettings(ctx).setAwaitingFirstStart(profileId, false)
        SyncScheduler.updateRemotePoll(ctx, dao.profiles())
    }
}
