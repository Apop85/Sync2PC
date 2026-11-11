package com.sync2pc.app.ui

import com.sync2pc.app.sync.NetworkRules
import com.sync2pc.app.sync.PcCheckMode

/**
 * Überschrift der Statuskarte auf der Startseite (SYNC-20261110-113, A1/A2/A4/A9).
 * Die Überschrift folgt dem Gesamtzustand: «alles gesichert» erscheint nur, wenn nichts offen ist,
 * eine Verbindung besteht und mindestens ein aktives Profil existiert.
 */
internal enum class HomeHeadline {
    /** Synchronisierung läuft */
    RUNNING,
    /** Kein Profil angelegt */
    NOT_CONFIGURED,
    /** Profile vorhanden, aber keines aktiv bzw. vollständig eingerichtet */
    NO_ACTIVE_PROFILE,
    /** Konflikt, Entscheidung, Schutzbremse oder Löschbestätigung offen */
    ATTENTION,
    /** Pausiert (Benutzer, «nur manuell» oder WLAN-Unterbruch) */
    PAUSED,
    /** Soll-WLAN nicht verbunden oder PC nicht erreichbar */
    NO_CONNECTION,
    /** Letzter Lauf mit Fehlern */
    ERRORS,
    /** Alles gesichert und aktuell */
    UP_TO_DATE,
    /** Sonst: neutral, wartet */
    IDLE,
}

/** Eingaben der Statusermittlung (alles bereits gelesen, damit die Logik rein und testbar bleibt) */
internal data class HomeStatusInput(
    val isRunning: Boolean,
    val hasProfiles: Boolean,
    /** Mindestens ein aktives, vollständig eingerichtetes Profil */
    val hasActiveProfile: Boolean,
    /** Anzahl offener Entscheidungen (Konflikte, Ordnerzuordnung, Schutzbremse, Löschbestätigung) */
    val attentionCount: Int,
    val noConnection: Boolean,
    val userPaused: Boolean,
    val wifiPaused: Boolean,
    val lastRunHadErrors: Boolean,
    /** Letzter Lauf ohne Probleme und nichts mehr offen (wie bisher berechnet) */
    val allUpToDate: Boolean,
)

internal object HomeStatus {
    /** Vorrang: läuft > nichts eingerichtet > wartet auf dich > pausiert > keine Verbindung > Fehler > gesichert > neutral */
    fun headline(i: HomeStatusInput): HomeHeadline = when {
        i.isRunning -> HomeHeadline.RUNNING
        !i.hasProfiles -> HomeHeadline.NOT_CONFIGURED
        i.attentionCount > 0 -> HomeHeadline.ATTENTION
        !i.hasActiveProfile -> HomeHeadline.NO_ACTIVE_PROFILE
        i.userPaused || i.wifiPaused -> HomeHeadline.PAUSED
        i.noConnection -> HomeHeadline.NO_CONNECTION
        i.lastRunHadErrors -> HomeHeadline.ERRORS
        i.allUpToDate -> HomeHeadline.UP_TO_DATE
        else -> HomeHeadline.IDLE
    }

    /** Hundebild zur Überschrift */
    fun dogFor(h: HomeHeadline, userPaused: Boolean): DogState = when (h) {
        HomeHeadline.RUNNING -> DogState.RUNNING
        HomeHeadline.ATTENTION, HomeHeadline.ERRORS -> DogState.ATTENTION
        HomeHeadline.PAUSED -> if (userPaused) DogState.PAUSED else DogState.WAITING
        HomeHeadline.UP_TO_DATE -> DogState.SLEEPING
        HomeHeadline.NOT_CONFIGURED, HomeHeadline.NO_ACTIVE_PROFILE,
        HomeHeadline.NO_CONNECTION, HomeHeadline.IDLE -> DogState.WAITING
    }

    /** Symbol (Warnzeichen) vor der Überschrift: nie nur Farbe (A4) */
    fun showsWarning(h: HomeHeadline): Boolean = when (h) {
        HomeHeadline.ATTENTION, HomeHeadline.NO_CONNECTION, HomeHeadline.ERRORS, HomeHeadline.NO_ACTIVE_PROFILE -> true
        else -> false
    }

    /**
     * Kann das Profil im aktuellen Netz laufen? Profile ohne WLAN-Liste («nur manuell») hängen nicht von einer
     * Verbindung ab und zählen als erfüllt, sonst würde die Karte dauerhaft «Keine Verbindung» melden.
     */
    fun profileCanRunNow(
        mode: PcCheckMode, profileSsids: List<String>, ssid: String?, onAllowedNetwork: Boolean, offWifiActive: Boolean,
    ): Boolean =
        (NetworkRules.needsWifiName(mode) && profileSsids.isEmpty()) ||
            NetworkRules.eligible(mode, ssid, profileSsids, onAllowedNetwork, offWifiActive)

    /** Keine Verbindung: PC wurde zuletzt nicht erreicht oder kein aktives Profil kann im aktuellen Netz laufen */
    fun noConnection(activeProfileCount: Int, anyCanRunNow: Boolean, pcUnreachable: Boolean): Boolean =
        activeProfileCount > 0 && (pcUnreachable || !anyCanRunNow)
}
