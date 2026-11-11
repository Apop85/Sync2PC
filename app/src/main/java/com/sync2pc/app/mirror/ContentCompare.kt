package com.sync2pc.app.mirror

import com.sync2pc.app.sync.PartialHash
import com.sync2pc.app.sync.isNetworkError
import kotlinx.coroutines.CancellationException

/** Der Vergleich wurde abgebrochen, weil der Lauf nicht weiterlaufen darf (z. B. WLAN weg): Ergebnis offen */
class HashAbortedException : Exception("Vergleich abgebrochen")

/**
 * Gleichheitsprüfung Handy-Datei gegen PC-Datei (Ordner-Sync und "Speicher freigeben" für Ordner-Profile,
 * SYNC-20261111-142, R-I3). Gleich ist nur, was bei gleicher Grösse über die GANZE Datei denselben Hash ergibt
 * ([PartialHash.comparison]); Stichproben genügen als Beweis nie.
 *
 * Sicherer Ausfall: Jeder Lesefehler, der kein Netzwerk-/Abbruchfehler ist, ergibt "nicht gleich". Das führt im Sync zu
 * einer Übertragung bzw. Konfliktabfrage (die ersetzte Version kommt in den Papierkorb) und beim Freigeben zu "nicht
 * freigeben". Abbruch (Coroutine, WLAN weg) und Netzwerkfehler werden weitergereicht: Der Lauf wird unterbrochen,
 * es wird nichts aus einem unvollständigen Vergleich geschlossen.
 */
object ContentCompare {

    /**
     * @param hashLocal Hash der Handy-Datei (über [PartialHash.comparison]), wird nur bei gleicher Grösse aufgerufen
     * @param hashRemote Hash der PC-Datei
     */
    fun same(localSize: Long, remoteSize: Long, hashLocal: () -> String, hashRemote: () -> String): Boolean {
        if (localSize != remoteSize) return false
        return try {
            hashLocal() == hashRemote()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HashAbortedException) {
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e)) throw e
            false
        }
    }
}
