package com.sync2pc.app.sync

/**
 * Folgelauf nach übersprungenen Dateien (SYNC-20261111-173): Wurden Dateien während des Laufs geändert und deshalb
 * übersprungen ([com.sync2pc.app.data.SyncRun.deferred] > 0), folgt nach [DELAY_MINUTES] Minuten ein automatischer Lauf.
 * Gegen Endlosschleifen (Datei ändert sich laufend) höchstens einer pro Stunde; danach übernehmen die regulären Auslöser.
 */
object SkipFollowUp {
    const val DELAY_MINUTES = 5L
    const val MIN_INTERVAL_MS = 3_600_000L

    /** Darf jetzt ein Folgelauf geplant werden? [lastAt] = Zeitpunkt der letzten Planung (0 = nie) */
    fun mayPlan(deferred: Int, now: Long, lastAt: Long): Boolean =
        deferred > 0 && (lastAt <= 0 || now < lastAt || now - lastAt >= MIN_INTERVAL_MS)
}
