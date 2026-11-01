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
        set(v) = prefs.edit().putBoolean("resumeOnWifi", v).apply()

    /** Kürzestes Prüfintervall aller aktiven Ordner-Profile (Minuten), 0 = keine Ordner-Profile */
    var remotePollMinutes: Int
        get() = prefs.getInt("remotePollMinutes", 0)
        set(v) = prefs.edit().putInt("remotePollMinutes", v).apply()

    var lastWifiTriggerAt: Long
        get() = prefs.getLong("lastWifiTriggerAt", 0)
        set(v) = prefs.edit().putLong("lastWifiTriggerAt", v).apply()
}
