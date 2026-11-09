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

    // ---- PC-Erkennung je Modus (SYNC-20261109-047, Stufe 2): GUID = jedes WLAN bzw. jede erlaubte Netzart ----

    /** Braucht das Profil den WLAN-Namen? (WIFI und BOTH; GUID nie) */
    fun needsWifiName(mode: PcCheckMode): Boolean = mode != PcCheckMode.GUID

    /**
     * Darf ein automatischer Lauf das Profil berücksichtigen? WIFI/BOTH: wie bisher (mobile Daten/VPN mit Schalter: ja,
     * sonst muss der lesbare Name in der Liste stehen; unlesbar = nicht erfüllt). GUID: jede erlaubte Netzart.
     * Bei BOTH/GUID über mobile Daten/VPN entscheidet danach die Kennung (Pflicht, Vorab-Prüfung im Worker).
     */
    fun eligible(
        mode: PcCheckMode, ssid: String?, profileSsids: List<String>, onAllowedNetwork: Boolean, offWifiActive: Boolean,
    ): Boolean = when {
        offWifiActive -> true
        mode == PcCheckMode.GUID -> onAllowedNetwork
        else -> ssid != null && profileSsids.contains(ssid)
    }

    /** Wie [mayContinue], aber je Modus: GUID prüft nur die erlaubte Netzart */
    fun mayContinue(mode: PcCheckMode, ssid: String?, profileSsids: List<String>, onAllowedNetwork: Boolean): Boolean =
        if (mode == PcCheckMode.GUID) onAllowedNetwork else mayContinue(ssid, profileSsids, onAllowedNetwork)

    /**
     * Passt ein neu verbundenes WLAN zum Profil (Auslöser/Fortsetzen)? GUID: jedes WLAN. WIFI/BOTH: Name in der Liste;
     * [unreadableMatches] = unlesbarer Name zählt als Treffer (der Worker prüft erneut), beim Fortsetzen nicht.
     */
    fun wifiTriggerMatches(
        mode: PcCheckMode, ssid: String?, profileSsids: List<String>, unreadableMatches: Boolean,
    ): Boolean = when {
        mode == PcCheckMode.GUID -> true
        ssid == null -> unreadableMatches
        else -> profileSsids.contains(ssid)
    }

    /**
     * Netzbedingung fürs erste Kennenlernen des PCs (V3, SYNC-20261109-058). BOTH: aktueller WLAN-Name lesbar und in der Liste
     * des Profils (unlesbar = nicht erfüllt). GUID: immer erfüllt (Lernen nur in der bewussten Einrichtung). WIFI: nicht relevant.
     */
    fun learnNetOk(mode: PcCheckMode, ssid: String?, profileSsids: List<String>): Boolean = when (mode) {
        PcCheckMode.BOTH -> ssid != null && profileSsids.contains(ssid)
        else -> true
    }

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
