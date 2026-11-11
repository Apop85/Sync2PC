package com.sync2pc.app.sync

import com.sync2pc.app.data.SyncRun

/** Gemeinsame Schnittstelle von Bildordner-Sync (SyncEngine) und Ordner-Spiegelung (MirrorEngine). */
interface SyncRunner {
    /** Vorschau-Einträge (nur Trockenlauf) */
    val preview: List<PreviewEntry>
    /** Lauf scheiterte an der Netzwerkverbindung zum Share */
    val connectionFailed: Boolean
    /** Lauf wurde abgebrochen, weil das Ziel-WLAN weg war */
    val wifiLost: Boolean
    /** Frühester Zeitpunkt für zu junge Dateien (0 = keine) */
    val nextYoungFileAt: Long get() = 0L
    /** Neu entdeckte Konflikte, die eine Entscheidung brauchen */
    val newConflicts: Int get() = 0
    /** Downloads wegen zu wenig Speicher übersprungen */
    val spaceShortage: Boolean get() = false
    /** Schutzbremse (0.4.1): Lauf hat angehalten und wartet auf eine Entscheidung des Benutzers */
    val blocked: Boolean get() = false
    /** Lauf scheiterte, weil «Verschlüsselung erforderlich» am PC nicht verfügbar ist (SYNC-20261111-172, Punkt 7) */
    val encryptionUnavailable: Boolean get() = false

    suspend fun run(): SyncRun
}
