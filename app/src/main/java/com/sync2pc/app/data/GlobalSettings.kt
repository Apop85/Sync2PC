package com.sync2pc.app.data

import android.content.Context
import android.content.SharedPreferences

/** Profilübergreifende Einstellungen. */
class GlobalSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("global", Context.MODE_PRIVATE)

    /** Automatisch synchronisieren, sobald ein Heim-WLAN verbunden wird */
    var autoSyncOnWifi: Boolean
        get() = prefs.getBoolean("autoSyncOnWifi", true)
        set(v) = prefs.edit().putBoolean("autoSyncOnWifi", v).apply()

    /** Neue Fotos/Videos sofort hochladen (wenn im Heim-WLAN) */
    var syncOnNewMedia: Boolean
        get() = prefs.getBoolean("syncOnNewMedia", true)
        set(v) = prefs.edit().putBoolean("syncOnNewMedia", v).apply()

    /** Automatische Syncs nur beim Laden (manueller Sync ignoriert das) */
    /** 0.11.5 (DEC-126): automatisch auch über mobile Daten bzw. VPN synchronisieren (Standard: aus) */
    var syncOffWifi: Boolean
        get() = prefs.getBoolean("syncOffWifi", false)
        set(v) = prefs.edit().putBoolean("syncOffWifi", v).apply()

    var onlyWhileCharging: Boolean
        get() = prefs.getBoolean("onlyWhileCharging", false)
        set(v) = prefs.edit().putBoolean("onlyWhileCharging", v).apply()

    /** Dateien erst übertragen, wenn sie mindestens so alt sind (Minuten) */
    var minFileAgeMinutes: Int
        get() = prefs.getInt("minFileAgeMinutes", 5)
        set(v) = prefs.edit().putInt("minFileAgeMinutes", v.coerceIn(0, 24 * 60)).apply()

    /** ISO-Code des Heimatlandes: Fotos von dort bekommen keinen Länderzusatz */
    var homeCountryIso: String
        get() = prefs.getString("homeCountryIso", "CH") ?: "CH"
        set(v) = prefs.edit().putString("homeCountryIso", v.trim().uppercase()).apply()

    /** Anzahl gleichzeitiger Übertragungen (je eine eigene SMB-Verbindung) */
    var parallelUploads: Int
        get() = prefs.getInt("parallelUploads", 3).coerceIn(1, 4)
        set(v) = prefs.edit().putInt("parallelUploads", v.coerceIn(1, 4)).apply()

    /**
     * Standards für Neuinstallationen (0.11.0), einmal beim App-Start: "Meldung nach jeder Synchronisierung"
     * ist bei neuen Installationen aus. Wer die App schon nutzt (Einstellungen vorhanden), behält "an".
     */
    fun applyInstallDefaults() {
        if (prefs.contains("notifySummary")) return
        prefs.edit().putBoolean("notifySummary", prefs.all.isNotEmpty()).apply()
    }

    /** Tipp "Herunterziehen startet eine Synchronisierung" schon gezeigt/benutzt? (0.11.0) */
    var pullTipDone: Boolean
        get() = prefs.getBoolean("pullTipDone", false)
        set(v) = prefs.edit().putBoolean("pullTipDone", v).apply()

    // ---- Benachrichtigungen (0.4.0) ---------------------------------------------
    /** Meldung nach jedem erfolgreichen Sync ("3 Dateien übertragen") */
    var notifySummary: Boolean
        get() = prefs.getBoolean("notifySummary", true)
        set(v) = prefs.edit().putBoolean("notifySummary", v).apply()

    /** "Update verfügbar" / "Update installiert" */
    var notifyUpdate: Boolean
        get() = prefs.getBoolean("notifyUpdate", true)
        set(v) = prefs.edit().putBoolean("notifyUpdate", v).apply()

    /** Hinweise: Ordnerauswahl, Konflikte, Dateien entfernen, Fehler (alles auch auf der Startseite sichtbar) */
    var notifyHints: Boolean
        get() = prefs.getBoolean("notifyHints", true)
        set(v) = prefs.edit().putBoolean("notifyHints", v).apply()

    // ---- Selbst-Update (GitHub Releases) -------------------------------------
    /**
     * "besitzer/repo" auf GitHub – seit 0.4.0 fest aus version.properties (UPDATE_REPO), nicht mehr änderbar.
     * Ein früher in den Einstellungen gespeicherter Wert wird ignoriert.
     */
    val updateRepo: String
        get() = com.sync2pc.app.BuildConfig.UPDATE_REPO

    var autoUpdateCheck: Boolean
        get() = prefs.getBoolean("autoUpdateCheck", true)
        set(v) = prefs.edit().putBoolean("autoUpdateCheck", v).apply()

    var lastUpdateCheck: Long
        get() = prefs.getLong("lastUpdateCheck", 0)
        set(v) = prefs.edit().putLong("lastUpdateCheck", v).apply()

    /** Zuletzt gefundene neuere Version (leer = keine) */
    var updateVersion: String
        get() = prefs.getString("updateVersion", "") ?: ""
        set(v) = prefs.edit().putString("updateVersion", v).apply()
    var updateApkUrl: String
        get() = prefs.getString("updateApkUrl", "") ?: ""
        set(v) = prefs.edit().putString("updateApkUrl", v).apply()
    var updateNotes: String
        get() = prefs.getString("updateNotes", "") ?: ""
        set(v) = prefs.edit().putString("updateNotes", v).apply()
    var updateSize: Long
        get() = prefs.getLong("updateSize", 0)
        set(v) = prefs.edit().putLong("updateSize", v).apply()
    /** Für diese Version wurde bereits benachrichtigt */
    var updateNotifiedVersion: String
        get() = prefs.getString("updateNotifiedVersion", "") ?: ""
        set(v) = prefs.edit().putString("updateNotifiedVersion", v).apply()
    /** Version, die gerade installiert wird -> nach dem Update App neu öffnen */
    var updateInstalling: String
        get() = prefs.getString("updateInstalling", "") ?: ""
        set(v) = prefs.edit().putString("updateInstalling", v).commit().let { }

    /** Share war nicht erreichbar: vor diesem Zeitpunkt (ms) keine automatische Prüfung; 0 = kein Warten */
    var shareRetryAt: Long
        get() = prefs.getLong("shareRetryAt", 0)
        set(v) = prefs.edit().putLong("shareRetryAt", v).apply()

    /** Namen der Profile, deren Share zuletzt nicht erreichbar war (für die Anzeige) */
    var shareUnreachable: String
        get() = prefs.getString("shareUnreachable", "") ?: ""
        set(v) = prefs.edit().putString("shareUnreachable", v).apply()

    /** Sync wurde durch WLAN-Verlust unterbrochen -> beim nächsten Verbinden sofort fortsetzen (ohne Drosselung) */
    var resumeOnWifi: Boolean
        get() = prefs.getBoolean("resumeOnWifi", false)
        set(v) = prefs.edit().putBoolean("resumeOnWifi", v)
            .putLong("resumeSince", if (v) (prefs.getLong("resumeSince", 0).takeIf { it > 0 } ?: System.currentTimeMillis()) else 0)
            .apply()

    /** Seit wann ein unterbrochener Sync auf das WLAN wartet (0 = nicht) */
    val resumeSince: Long get() = prefs.getLong("resumeSince", 0)

    /**
     * Vorgemerkter Nachlauf (R-N1, SYNC-20261111-143): "" = keiner, "auto" oder "manual". Ein Start, der während eines
     * laufenden Syncs kam, wird nach dessen Ende nachgeholt. Mit `commit()` (nicht `apply()`): muss einen Prozess-Kill
     * kurz nach der Anforderung überleben (vgl. R-S1). Nicht Teil der exportierbaren Einstellungen.
     */
    val deferredStart: String
        get() = prefs.getString("deferredStart", "") ?: ""

    /** Speichert den Nachlauf-Merker synchron; false = nicht gespeichert (SYNC-20261111-172) */
    fun writeDeferredStart(v: String): Boolean = prefs.edit().putString("deferredStart", v).commit()

    /** Kürzestes Prüfintervall aller aktiven Ordner-Profile (Minuten), 0 = keine Ordner-Profile */
    var remotePollMinutes: Int
        get() = prefs.getInt("remotePollMinutes", 0)
        set(v) = prefs.edit().putInt("remotePollMinutes", v).apply()

    /** Warum der letzte automatische Start nicht synchronisiert hat (leer = alles ok) */
    val lastSkipInfo: String get() = prefs.getString("lastSkipInfo", "") ?: ""
    val lastSkipAt: Long get() = prefs.getLong("lastSkipAt", 0)

    /** Der letzte Grund stammt von einem manuellen Start (SYNC-20261111-188 B1: «Automatischer Start übersprungen» wäre falsch) */
    val lastSkipManual: Boolean get() = prefs.getBoolean("lastSkipManual", false)

    fun noteSkip(info: String, manual: Boolean = false) {
        prefs.edit().putString("lastSkipInfo", info).putLong("lastSkipAt", System.currentTimeMillis())
            .putBoolean("lastSkipManual", manual).apply()
    }

    // ---- PC nicht erkannt: Benachrichtigung einmal je Profil, bis geklärt (SYNC-20261109-047) ----
    fun pcNotified(profileId: Long): Boolean =
        (prefs.getStringSet("pcNotified", emptySet()) ?: emptySet()).contains(profileId.toString())

    fun setPcNotified(profileId: Long, notified: Boolean) {
        val cur = (prefs.getStringSet("pcNotified", emptySet()) ?: emptySet()).toMutableSet()
        if (!(if (notified) cur.add(profileId.toString()) else cur.remove(profileId.toString()))) return
        prefs.edit().putStringSet("pcNotified", cur).apply()
    }

    /** Zeitpunkt der letzten Planung eines Folgelaufs nach übersprungenen Dateien (SYNC-20261111-173), 0 = nie */
    var lastSkipFollowUpAt: Long
        get() = prefs.getLong("lastSkipFollowUpAt", 0)
        set(v) = prefs.edit().putLong("lastSkipFollowUpAt", v).apply()

    // ---- «Verschlüsselung nicht verfügbar»: Fehlermeldung je Profil nur einmal, bis ein Lauf gelingt (SYNC-20261111-172) ----
    fun encNotified(profileId: Long): Boolean =
        (prefs.getStringSet("encNotified", emptySet()) ?: emptySet()).contains(profileId.toString())

    fun setEncNotified(profileId: Long, notified: Boolean) {
        val cur = (prefs.getStringSet("encNotified", emptySet()) ?: emptySet()).toMutableSet()
        if (!(if (notified) cur.add(profileId.toString()) else cur.remove(profileId.toString()))) return
        prefs.edit().putStringSet("encNotified", cur).apply()
    }

    // ---- Neue Profile warten auf den ersten manuellen Start (0.3.6) ------------------
    /** IDs neu angelegter Profile, für die noch kein manueller Sync lief -> keine automatischen Syncs */
    fun awaitingFirstStart(): Set<Long> =
        (prefs.getStringSet("awaitingFirstStart", emptySet()) ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()

    fun setAwaitingFirstStart(profileId: Long, waiting: Boolean) {
        val cur = awaitingFirstStart().toMutableSet()
        if (waiting) cur += profileId else cur -= profileId
        prefs.edit().putStringSet("awaitingFirstStart", cur.map { it.toString() }.toSet()).apply()
    }

    /** Simulation: Standard-Anzahl Dateien (0 = alle) */
    var previewLimit: Int
        get() = prefs.getInt("previewLimit", 100).coerceIn(0, 100_000)
        set(v) = prefs.edit().putInt("previewLimit", v.coerceIn(0, 100_000)).apply()

    /** Verlauf-Filter (0.3.5): nur Läufe mit Fehler / nur Läufe mit Übertragungen; beide aus = alles */
    var logFilterErrors: Boolean
        get() = prefs.getBoolean("logFilterErrors", false)
        set(v) = prefs.edit().putBoolean("logFilterErrors", v).apply()
    var logFilterTransfers: Boolean
        get() = prefs.getBoolean("logFilterTransfers", false)
        set(v) = prefs.edit().putBoolean("logFilterTransfers", v).apply()

    // ---- Pause durch den Benutzer (seit 0.3.1) ---------------------------------------
    /** Bis zu diesem Zeitpunkt (ms) keine automatischen Syncs; 0 = nicht pausiert */
    var pausedUntil: Long
        get() = prefs.getLong("pausedUntil", 0)
        set(v) = prefs.edit().putLong("pausedUntil", v).apply()

    fun isPaused(now: Long = System.currentTimeMillis()): Boolean = pausedUntil > now || manualOnly

    /**
     * "Für immer pausiert" (0.3.5): keine Hintergrund-Syncs mehr (WLAN, neue Fotos, Sicherheitsnetz,
     * Ordner-Prüfung, Erinnerung). Nur noch "Synchronisieren" – das hebt diesen Modus NICHT auf.
     */
    var manualOnly: Boolean
        get() = prefs.getBoolean("manualOnly", false)
        set(v) = prefs.edit().putBoolean("manualOnly", v).apply()

    // ---- Erinnerung "lange nicht gesichert" (seit 0.2.0) -------------------------
    var staleWarnEnabled: Boolean
        get() = prefs.getBoolean("staleWarnEnabled", true)
        set(v) = prefs.edit().putBoolean("staleWarnEnabled", v).apply()

    /** Nach so vielen Tagen ohne erfolgreiche Sicherung erinnern */
    var staleWarnDays: Int
        get() = prefs.getInt("staleWarnDays", 7).coerceIn(1, 365)
        set(v) = prefs.edit().putInt("staleWarnDays", v.coerceIn(1, 365)).apply()

    /** Zeitpunkt der letzten Erinnerung (0 = keine offen) */
    var lastStaleWarnAt: Long
        get() = prefs.getLong("lastStaleWarnAt", 0)
        set(v) = prefs.edit().putLong("lastStaleWarnAt", v).apply()

    /** Erste Prüfung überhaupt – Ersatz für "letzter Sync", wenn es noch keinen Lauf gibt */
    fun staleBaseline(): Long {
        val v = prefs.getLong("staleBaseline", 0)
        if (v > 0) return v
        val now = System.currentTimeMillis()
        prefs.edit().putLong("staleBaseline", now).apply()
        return now
    }

    // ---- Speicher freigeben (seit 0.2.0) -----------------------------------------
    var freeSpaceMonths: Int
        get() = prefs.getInt("freeSpaceMonths", 12).coerceIn(1, 240)
        set(v) = prefs.edit().putInt("freeSpaceMonths", v.coerceIn(1, 240)).apply()

    var freeSpaceKeepFavorites: Boolean
        get() = prefs.getBoolean("freeSpaceKeepFavorites", true)
        set(v) = prefs.edit().putBoolean("freeSpaceKeepFavorites", v).apply()

    // ---- Sicherung: welche Einstellungen exportiert werden --------------------------
    /** Exportierbare Einstellungen (Geräte-ID, Zeitstempel und Zwischenstände bewusst nicht) */
    fun exportable(): Map<String, Any> = linkedMapOf(
        "autoSyncOnWifi" to autoSyncOnWifi, "syncOnNewMedia" to syncOnNewMedia,
        "onlyWhileCharging" to onlyWhileCharging, "minFileAgeMinutes" to minFileAgeMinutes,
        "syncOffWifi" to syncOffWifi,
        "homeCountryIso" to homeCountryIso, "parallelUploads" to parallelUploads,
        "autoUpdateCheck" to autoUpdateCheck,
        "notifySummary" to notifySummary, "notifyUpdate" to notifyUpdate, "notifyHints" to notifyHints,
        "staleWarnEnabled" to staleWarnEnabled, "staleWarnDays" to staleWarnDays,
        "freeSpaceMonths" to freeSpaceMonths, "freeSpaceKeepFavorites" to freeSpaceKeepFavorites,
    )

    /** Übernimmt Einstellungen aus einer Sicherung; unbekannte oder falsch typisierte Werte werden ignoriert */
    fun importFrom(map: Map<String, Any>) {
        fun b(k: String, set: (Boolean) -> Unit) { (map[k] as? Boolean)?.let(set) }
        fun i(k: String, set: (Int) -> Unit) { (map[k] as? Number)?.toInt()?.let(set) }
        fun s(k: String, set: (String) -> Unit) { (map[k] as? String)?.let(set) }
        b("autoSyncOnWifi") { autoSyncOnWifi = it }; b("syncOnNewMedia") { syncOnNewMedia = it }
        b("onlyWhileCharging") { onlyWhileCharging = it }; i("minFileAgeMinutes") { minFileAgeMinutes = it }
        b("syncOffWifi") { syncOffWifi = it }
        s("homeCountryIso") { homeCountryIso = it }; i("parallelUploads") { parallelUploads = it }
        // "updateRepo" wird seit 0.4.0 nicht mehr übernommen (fest eingebaut)
        b("autoUpdateCheck") { autoUpdateCheck = it }
        b("notifySummary") { notifySummary = it }; b("notifyUpdate") { notifyUpdate = it }; b("notifyHints") { notifyHints = it }
        b("staleWarnEnabled") { staleWarnEnabled = it }; i("staleWarnDays") { staleWarnDays = it }
        i("freeSpaceMonths") { freeSpaceMonths = it }; b("freeSpaceKeepFavorites") { freeSpaceKeepFavorites = it }
    }

    /** Zufällige, dauerhafte Geräte-ID für Diagnose und Logs (keine Hardware-Kennung) */
    val deviceId: String
        get() = prefs.getString("deviceId", null) ?: java.util.UUID.randomUUID().toString()
            .replace("-", "").take(12).uppercase()
            .also { prefs.edit().putString("deviceId", it).apply() }

    var lastWifiTriggerAt: Long
        get() = prefs.getLong("lastWifiTriggerAt", 0)
        set(v) = prefs.edit().putLong("lastWifiTriggerAt", v).apply()
}
