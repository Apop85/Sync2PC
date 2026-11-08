package com.sync2pc.app.sync

/**
 * Reine Entscheidungslogik zum Netzwerk (ohne Android-Klassen, daher als JVM-Test prüfbar).
 * [WifiHelper] liest die Netzeigenschaften und ruft diese Regeln auf (0.11.7, DEC-126).
 */
object NetworkRules {
    /** Wartezeit nach "PC nicht erreichbar" bzw. Verbindungsabbruch, solange nur mobile Daten/VPN aktiv sind */
    const val OFF_WIFI_RETRY_MINUTES = 120L

    /** Wartezeit nach einem Verbindungsabbruch mitten im Lauf im WLAN (DEC-048) */
    const val MID_RUN_RETRY_MINUTES = 2L

    /** WLAN immer; mobile Daten/VPN nur mit dem Schalter "syncOffWifi" */
    fun onAllowedNetwork(isWifi: Boolean, isMobileOrVpn: Boolean, switchOn: Boolean): Boolean =
        isWifi || (switchOn && isMobileOrVpn)

    /** Mobile Daten/VPN ohne WLAN bei eingeschaltetem Schalter: die SSID-Prüfung entfällt */
    fun offWifiSyncActive(isMobileOrVpn: Boolean, switchOn: Boolean): Boolean = switchOn && isMobileOrVpn

    /**
     * Darf ein automatischer Lauf für ein Profil weiterlaufen? Mit lesbarer SSID entscheidet die SSID-Liste
     * des Profils, sonst (kein WLAN oder Name unlesbar) die erlaubte Netzart.
     */
    fun mayContinue(ssid: String?, profileSsids: List<String>, onAllowedNetwork: Boolean): Boolean =
        if (ssid != null) profileSsids.contains(ssid) else onAllowedNetwork

    /**
     * Minuten bis zum nächsten Versuch, wenn der PC nicht erreichbar ist.
     * Nur mobile Daten/VPN: lange Wartezeit (schont Akku und Datenvolumen), auch nach Abbruch mitten im Lauf.
     */
    fun retryMinutes(offWifi: Boolean, midRun: Boolean): Long = when {
        offWifi -> OFF_WIFI_RETRY_MINUTES
        midRun -> MID_RUN_RETRY_MINUTES
        else -> SyncScheduler.SHARE_RETRY_MINUTES
    }

    /** Wartezeiten (ms) vor jedem Erreichbarkeitsversuch; der erste Versuch läuft sofort */
    fun reachabilityWaits(manual: Boolean, offWifi: Boolean): List<Long> = when {
        manual -> listOf(0L, 3_000L)
        offWifi -> listOf(0L, 5_000L)
        else -> listOf(0L, 5_000L, 10_000L, 15_000L, 30_000L)
    }
}
