package com.sync2pc.app.mirror

import android.content.Context
import com.sync2pc.app.R
import com.sync2pc.app.l10n.str
import com.sync2pc.app.sync.Notifications
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InterventionState { WAITING, RUNNING, DONE }

/** [progress] = (geprüft, gesamt) oder null; [result] nur im Zustand DONE */
data class InterventionTask(val state: InterventionState, val progress: Pair<Int, Int>? = null, val result: String? = null)

/**
 * Zustandslogik der Eingriffe von Hand (ohne Android-Klassen, getestet in InterventionRegistryTest).
 * Alle Zustandswechsel laufen unter derselben Monitor-Sperre; so kann "Abbrechen" nicht gleichzeitig mit dem
 * Start des Eingriffs wirken (0.11.10, QA -035).
 */
open class InterventionRegistry(
    private val scope: CoroutineScope,
    /** Läuft gerade ein Sync (oder ein anderer Eingriff)? Nur für den Anfangszustand WAITING */
    private val lockBusy: () -> Boolean,
    private val failedText: (Throwable) -> String,
) {
    /** Kennzeichnet einen Start; ein abgebrochener oder ersetzter Eingriff kann den Zustand des neuen nicht mehr ändern */
    private class Token { @Volatile var cancelled = false }

    private val map = MutableStateFlow<Map<Long, InterventionTask>>(emptyMap())
    val tasks: StateFlow<Map<Long, InterventionTask>> = map

    private val tokens = HashMap<Long, Token>()
    private val jobs = HashMap<Long, Job>()
    private val viewers = HashMap<Long, Int>()

    /** Wartet oder läuft ein Eingriff für dieses Profil? Dann sind weitere Lösch-Eingriffe gesperrt. */
    fun isActive(profileId: Long): Boolean {
        val st = map.value[profileId]?.state
        return st == InterventionState.WAITING || st == InterventionState.RUNNING
    }

    /**
     * Eingriff vormerken bzw. starten. [work] erhält `onStart` (aufrufen, sobald die Sperre erworben ist; wirft, wenn
     * inzwischen abgebrochen wurde) und `progress`, und liefert den Ergebnistext. [notify] stellt das Ergebnis zu,
     * wenn keine Ansicht offen ist (true = zugestellt). false = für dieses Profil steht schon ein Eingriff an.
     */
    @Synchronized
    fun start(
        profileId: Long, notify: (String) -> Boolean,
        work: suspend (onStart: () -> Unit, progress: (Int, Int) -> Unit) -> String,
    ): Boolean {
        if (isActive(profileId)) return false
        val token = Token()
        tokens[profileId] = token
        set(profileId, InterventionTask(if (lockBusy()) InterventionState.WAITING else InterventionState.RUNNING))
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val text = work(
                    {
                        synchronized(this@InterventionRegistry) {
                            if (token.cancelled) throw CancellationException("abgebrochen")
                            set(profileId, InterventionTask(InterventionState.RUNNING))
                        }
                    },
                    { done, total ->
                        synchronized(this@InterventionRegistry) {
                            if (tokens[profileId] === token) set(profileId, InterventionTask(InterventionState.RUNNING, done to total))
                        }
                    },
                )
                finish(profileId, token, text, notify)
            } catch (e: CancellationException) {
                release(profileId, token)
                throw e
            } catch (e: Exception) {
                finish(profileId, token, failedText(e), notify)
            }
        }
        jobs[profileId] = job
        job.start() // erst starten, wenn der Job eingetragen ist (sonst könnte `finish` ihn noch nicht kennen)
        return true
    }

    /** "Abbrechen": nur solange der Eingriff noch wartet; hat er begonnen, wird er zu Ende geführt. */
    @Synchronized
    fun cancel(profileId: Long) {
        if (map.value[profileId]?.state != InterventionState.WAITING) return
        tokens.remove(profileId)?.cancelled = true
        jobs.remove(profileId)?.cancel()
        clear(profileId)
    }

    /** Ergebnis gesehen */
    @Synchronized
    fun dismiss(profileId: Long) {
        if (map.value[profileId]?.state == InterventionState.DONE) clear(profileId)
    }

    /** Eine Ansicht dieses Profils ist offen: Ergebnis dort anzeigen statt als Benachrichtigung */
    @Synchronized
    fun viewerOpened(profileId: Long) { viewers[profileId] = (viewers[profileId] ?: 0) + 1 }

    @Synchronized
    fun viewerClosed(profileId: Long) {
        val n = ((viewers[profileId] ?: 0) - 1).coerceAtLeast(0)
        if (n == 0) { viewers.remove(profileId); dismiss(profileId) } // gesehenes Ergebnis verfällt mit der letzten Ansicht
        else viewers[profileId] = n
    }

    @Synchronized
    private fun finish(profileId: Long, token: Token, text: String, notify: (String) -> Boolean) {
        if (tokens[profileId] !== token) return
        tokens.remove(profileId); jobs.remove(profileId)
        // Ansicht offen: dort anzeigen. Sonst Benachrichtigung; lässt sie sich nicht zustellen (abgeschaltet,
        // keine Berechtigung), bleibt das Ergebnis stehen, bis die Ansicht wieder geöffnet wird.
        if ((viewers[profileId] ?: 0) > 0 || !notify(text)) set(profileId, InterventionTask(InterventionState.DONE, result = text))
        else clear(profileId)
    }

    @Synchronized
    private fun release(profileId: Long, token: Token) {
        if (tokens[profileId] !== token) return // schon durch "Abbrechen" oder einen neuen Eingriff ersetzt
        tokens.remove(profileId); jobs.remove(profileId)
        clear(profileId)
    }

    private fun set(profileId: Long, t: InterventionTask) { map.update { it + (profileId to t) } }
    private fun clear(profileId: Long) { map.update { it - profileId } }
}

/**
 * Eingriff von Hand (Aufräumen, Endgültig löschen, Speicher freigeben), der auf einen laufenden Ordner-Sync wartet
 * (0.11.10, DEC-144, Auftrag SYNC-20261108-012). Der Eingriff läuft in einem **prozessweiten** Bereich und überlebt
 * damit das Verlassen der Ansicht. Er wird nicht gespeichert: Beendet Android den Prozess während des Wartens, entfällt
 * er und es wird nichts gelöscht (sichere Richtung). Ausgeführt wird er von den bestehenden Funktionen, sobald sie die
 * [MirrorLock] erhalten; die Sperre wird nur dort gehalten. Pro Profil steht höchstens ein Eingriff an.
 */
object PendingIntervention : InterventionRegistry(
    CoroutineScope(SupervisorJob() + Dispatchers.Default),
    { MirrorLock.busy },
    { e -> str(R.string.intervention_failed, com.sync2pc.app.sync.shortError(e)) },
) {
    /** Ergebnis ohne offene Ansicht als Benachrichtigung zustellen (je Profil eine, unabhängig vom Hinweis-Schalter) */
    fun start(
        context: Context, profileId: Long,
        work: suspend (onStart: () -> Unit, progress: (Int, Int) -> Unit) -> String,
    ): Boolean {
        val app = context.applicationContext
        return start(profileId, { Notifications.interventionDone(app, profileId, it) }, work)
    }
}
