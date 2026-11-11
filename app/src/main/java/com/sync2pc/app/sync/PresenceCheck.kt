package com.sync2pc.app.sync

import com.sync2pc.app.data.DeleteMode
import kotlinx.coroutines.CancellationException

/**
 * "Bereits vorhanden" im Fotos-Sync (SYNC-20261111-142, R-F1/R-I4): Eine schon auf dem PC liegende Datei mit gleichem
 * Namen, gleicher Grösse und gleichem Teil-Hash (3 x 64 KB) beweist noch nicht, dass das Original gelöscht werden darf.
 *
 * Wird nach dem Sync gelöscht (Löschmodus ungleich KEEP), gilt die vorhandene Datei nur dann als gesichert
 * ([com.sync2pc.app.data.SyncedFile.verified]), wenn sie auch auf der Profilstufe, mindestens "Mittel", übereinstimmt.
 * Beim Löschmodus KEEP bleibt es wie bisher (DEC-021): kein Löschen, also keine zusätzliche Prüfung.
 */
object PresenceCheck {

    /** Prüfstufe für vorhandene Dateien: die Profilstufe, mindestens "Mittel" ([PartialHash.LEVEL_MEDIUM]) */
    fun level(profileLevelCode: Int): Int = maxOf(profileLevelCode, PartialHash.LEVEL_MEDIUM)

    /**
     * @param hashLocal Prüfsumme der Handy-Datei auf der Stufe [level] (nur bei Bedarf aufgerufen)
     * @param hashRemote Prüfsumme der vorhandenen PC-Datei auf derselben Stufe
     * @return true = als gesichert verbuchen (Löschfreigabe möglich), false = nur als vorhanden verbuchen, nie löschen.
     * Lesefehler (kein Netzwerk-/Abbruchfehler) = false; Netzwerkfehler und Abbruch gehen nach aussen.
     */
    fun verifiedWhenPresent(mode: DeleteMode, hashLocal: () -> String, hashRemote: () -> String): Boolean {
        if (mode == DeleteMode.KEEP) return true
        return try {
            hashLocal() == hashRemote()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e)) throw e
            false
        }
    }
}
