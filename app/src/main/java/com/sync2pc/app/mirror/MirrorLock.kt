package com.sync2pc.app.mirror

import kotlinx.coroutines.sync.Mutex

/**
 * Sperre zwischen Ordner-Sync und Eingriffen von Hand in den Papierkorb bzw. den Handy-Ordner (0.7.0):
 * Aufräumen, Endgültig löschen und Speicher freigeben warten, bis ein laufender Ordner-Sync fertig ist –
 * sonst könnte z. B. "Alles leeren" eine gerade ersetzte Version sofort mitlöschen.
 */
object MirrorLock {
    val mutex = Mutex()

    /** Läuft gerade ein Ordner-Sync (oder ein anderer Eingriff)? Für den Hinweis "Wartet auf laufenden Sync…" */
    val busy: Boolean get() = mutex.isLocked
}
