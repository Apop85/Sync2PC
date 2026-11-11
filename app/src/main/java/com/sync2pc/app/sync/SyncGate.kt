package com.sync2pc.app.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Art eines vorgemerkten Starts (R-N1): MANUAL setzt sich gegen AUTO durch. */
enum class DeferredKind {
    AUTO, MANUAL;

    /** Persistierte Form: "auto" / "manual" */
    fun toStore(): String = name.lowercase()

    companion object {
        /** "" oder Unbekanntes = kein Merker */
        fun fromStore(s: String?): DeferredKind? = when (s) {
            "auto" -> AUTO
            "manual" -> MANUAL
            else -> null
        }
    }
}

/**
 * Für die Oberfläche beobachtbarer Nachlauf (R-N1): [requested] = ein Start kam während eines laufenden Syncs und wird
 * danach genau einmal nachgeholt; [manual] = darunter war ein manueller Start (dann wird als manueller nachgeholt).
 * Es gibt keinen Start für ein einzelnes Profil: ein Nachlauf gilt immer für alle Profile.
 */
data class DeferredState(val requested: Boolean = false, val manual: Boolean = false) {
    companion object {
        val NONE = DeferredState()
        fun of(kind: DeferredKind?) = if (kind == null) NONE else DeferredState(true, kind == DeferredKind.MANUAL)
    }
}

/** Ergebnis einer Startanforderung: sofort eingereiht oder nach dem laufenden Sync vorgemerkt */
enum class StartResult { STARTED, DEFERRED }

/** Ablage des Merkers über Prozessgrenzen (Produktion: GlobalSettings mit commit()) */
interface DeferredStore {
    fun read(): DeferredKind?
    /** null = Merker löschen. true = dauerhaft gespeichert, false = Speichern fehlgeschlagen (SYNC-20261111-172) */
    fun write(kind: DeferredKind?): Boolean
}

/**
 * Sperre für den Sync-Lauf samt Nachlauf-Merker (SYNC-20261111-143, R-N1, R-BG4/R-BG5). Reine Entscheidungslogik ohne Android:
 *  - höchstens ein Lauf gleichzeitig ([tryAcquireOrDefer]);
 *  - ein Start, der die Sperre nicht bekommt oder sie bei der Anforderung belegt sieht, wird NICHT verworfen, sondern als
 *    Merker festgehalten ([requestStart], [tryAcquireOrDefer]);
 *  - Prüfen/Setzen des Merkers und Sperrzustand liegen unter EINER kurzen Sperre (`lock`): eine Anforderung geht nie zwischen
 *    «Merker prüfen» und «Sperre freigeben» verloren: sie sieht entweder den Lauf (-> Merker, wird von [release] mitgenommen)
 *    oder die freie Sperre (-> Aufrufer reiht selbst ein);
 *  - [release] gibt frei und liefert den Merker genau einmal zurück (kein zweiter Nachlauf ohne neue Anforderung, keine Schleife).
 * Der Merker liegt im Speicher und im [DeferredStore] (nach Prozess-Kill wirksam über [claimIfIdle]).
 */
class SyncGate(
    private val store: DeferredStore,
    /** Meldung ins Diagnose-Log (Logcat), wenn der Merker nicht gespeichert werden konnte; der Merker im Speicher bleibt */
    private val onStoreFailure: (String) -> Unit = {},
) {
    private val lock = Any()
    private var running = false
    private var pending: DeferredKind? = runCatching { store.read() }.getOrNull()
    private val _state = MutableStateFlow(DeferredState.of(pending))

    /** Nachlauf angefordert? (Oberfläche) */
    val state: StateFlow<DeferredState> = _state.asStateFlow()

    val isRunning: Boolean get() = synchronized(lock) { running }

    /**
     * Worker-Start: true = Sperre erhalten (Aufrufer muss mit [release]/[finishRun] freigeben).
     * false = ein Lauf ist aktiv: dieser Start wird als Merker vorgemerkt (nicht verworfen).
     */
    fun tryAcquireOrDefer(manual: Boolean): Boolean = synchronized(lock) {
        if (!running) { running = true; return true }
        record(if (manual) DeferredKind.MANUAL else DeferredKind.AUTO)
        false
    }

    /**
     * Sperre für Sonderaufträge (Fortsetzen, Wiederholung, Folgelauf): true = erhalten (mit [release]/[finishRun] freigeben).
     * false = ein Lauf ist aktiv: KEIN Merker, der Aufrufer bleibt eingereiht und versucht es später erneut
     * (`Result.retry()`), damit Eingabedaten und Auftragsart (KEY_RESUME/KEY_RETRY) erhalten bleiben.
     */
    fun tryAcquire(): Boolean = synchronized(lock) {
        if (running) return false
        running = true
        true
    }

    /**
     * Startanforderung der Oberfläche/eines Auslösers: true = ein Lauf ist aktiv, der Start ist vorgemerkt (der Aufrufer reiht
     * NICHT selbst ein). false = frei, der Aufrufer reiht den Auftrag ein.
     */
    fun requestStart(manual: Boolean): Boolean = synchronized(lock) {
        if (!running) return false
        record(if (manual) DeferredKind.MANUAL else DeferredKind.AUTO)
        true
    }

    /** Gibt die Sperre frei und liefert den Merker (genau einmal), null = nichts vorgemerkt. Wirft nie. */
    fun release(): DeferredKind? = synchronized(lock) {
        running = false
        take()
    }

    /**
     * Prozessstart: ein persistenter Merker ohne laufenden Sync wird übernommen (Prozess-Kill nach der Anforderung).
     * Läuft ein Sync, bleibt der Merker für dessen [release].
     */
    fun claimIfIdle(): DeferredKind? = synchronized(lock) { if (running) null else take() }

    /** Merker verwerfen («Pausieren»/«Abbrechen» durch den Benutzer) */
    fun discardDeferred() { synchronized(lock) { take() } }

    /** Merker wieder setzen (Einreihen des Nachlaufs ist fehlgeschlagen: nicht verlieren) */
    fun restore(kind: DeferredKind) { synchronized(lock) { record(kind) } }

    /**
     * Abschluss eines Laufs (R-BG4/R-BG5): [cleanup] (Fortschritt weg, Unterbruch vormerken) darf scheitern, die Sperre wird in
     * jedem Fall freigegeben und ein vorgemerkter Nachlauf trotzdem eingereiht ([followUp]); scheitert [followUp], wird der
     * Merker wiederhergestellt (der nächste Prozessstart holt ihn nach). Die erste Ausnahme wird danach weitergereicht.
     */
    fun finishRun(cleanup: () -> Unit, followUp: (DeferredKind) -> Unit) {
        var failure: Throwable? = null
        try { cleanup() } catch (t: Throwable) { failure = t }
        val kind = release()
        if (kind != null) {
            try { followUp(kind) } catch (t: Throwable) { restore(kind); if (failure == null) failure = t }
        }
        failure?.let { throw it }
    }

    // --- nur unter `lock` ---
    private fun record(kind: DeferredKind) {
        val merged = if (pending == DeferredKind.MANUAL || kind == DeferredKind.MANUAL) DeferredKind.MANUAL else DeferredKind.AUTO
        if (merged == pending) return
        persist(merged)
        pending = merged
        _state.value = DeferredState.of(merged)
    }

    private fun take(): DeferredKind? {
        val kind = pending ?: return null
        pending = null
        persist(null)
        _state.value = DeferredState.NONE
        return kind
    }

    /** Speichern darf den Ablauf nie stören; ein Fehlschlag (false oder Ausnahme) wird nur gemeldet */
    private fun persist(kind: DeferredKind?) {
        val ok = try { store.write(kind) } catch (t: Throwable) { false }
        if (!ok) runCatching { onStoreFailure("Merker «${kind?.toStore() ?: "gelöscht"}» nicht gespeichert") }
    }
}
