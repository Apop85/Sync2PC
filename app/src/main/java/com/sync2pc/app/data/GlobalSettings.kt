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

    // ---- Selbst-Update (GitHub Releases) -------------------------------------
    /** "besitzer/repo" auf GitHub; Vorgabe aus version.properties (UPDATE_REPO) */
    var updateRepo: String
        get() = prefs.getString("updateRepo", null) ?: com.sync2pc.app.BuildConfig.UPDATE_REPO
        set(v) = prefs.edit().putString("updateRepo", v.trim()).apply()

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

    /** Kürzestes Prüfintervall aller aktiven Ordner-Profile (Minuten), 0 = keine Ordner-Profile */
    var remotePollMinutes: Int
        get() = prefs.getInt("remotePollMinutes", 0)
        set(v) = prefs.edit().putInt("remotePollMinutes", v).apply()

    /** Warum der letzte automatische Start nicht synchronisiert hat (leer = alles ok) */
    val lastSkipInfo: String get() = prefs.getString("lastSkipInfo", "") ?: ""
    val lastSkipAt: Long get() = prefs.getLong("lastSkipAt", 0)

    fun noteSkip(info: String) {
        prefs.edit().putString("lastSkipInfo", info).putLong("lastSkipAt", System.currentTimeMillis()).apply()
    }

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
     * Ordner-Prüfung, Erinnerung). Nur noch "Jetzt synchronisieren" – das hebt diesen Modus NICHT auf.
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
        "homeCountryIso" to homeCountryIso, "parallelUploads" to parallelUploads,
        "updateRepo" to updateRepo, "autoUpdateCheck" to autoUpdateCheck,
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
        s("homeCountryIso") { homeCountryIso = it }; i("parallelUploads") { parallelUploads = it }
        s("updateRepo") { updateRepo = it }; b("autoUpdateCheck") { autoUpdateCheck = it }
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
