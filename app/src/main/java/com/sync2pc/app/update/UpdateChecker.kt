package com.sync2pc.app.update

import android.content.Context
import com.sync2pc.app.BuildConfig
import com.sync2pc.app.data.GlobalSettings
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val size: Long,
    val notes: String,
)

/** Prüft GitHub Releases (neuestes Release, nicht Vorabversion) auf eine neuere APK. */
object UpdateChecker {
    private val repoRegex = Regex("""^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$""")

    /** Akzeptiert "user/repo" oder "https://github.com/user/repo(.git)" */
    fun normalizeRepo(input: String): String? {
        val s = input.trim()
            .removePrefix("https://").removePrefix("http://").removePrefix("github.com/")
            .removeSuffix("/").removeSuffix(".git")
        return if (repoRegex.matches(s)) s else null
    }

    fun currentVersion(): String = BuildConfig.VERSION_NAME.substringBefore('-')

    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V").substringBefore('-')
            .split('.').map { it.toIntOrNull() ?: 0 }.let { it + List((3 - it.size).coerceAtLeast(0)) { 0 } }

    fun isNewer(remote: String, local: String = currentVersion()): Boolean {
        val r = parts(remote); val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** Liest das neueste Release. Wirft IOException mit lesbarer Meldung. */
    fun fetchLatest(repo: String): UpdateInfo {
        val conn = URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "Sync2PC/${BuildConfig.VERSION_NAME}")
        try {
            when (conn.responseCode) {
                200 -> {}
                404 -> throw IOException("Kein Release gefunden (Repository '$repo' privat, falsch geschrieben oder noch ohne Release)")
                403 -> throw IOException("GitHub-Abfragelimit erreicht – später erneut versuchen")
                else -> throw IOException("GitHub antwortet mit HTTP ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name").ifBlank { throw IOException("Release ohne Versions-Tag") }
            val assets = json.optJSONArray("assets")
            var url = ""; var size = 0L
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name").lowercase()
                if (name.endsWith(".apk") && !name.contains("debug")) {
                    url = a.optString("browser_download_url"); size = a.optLong("size"); break
                }
            }
            if (url.isBlank()) throw IOException("Release $tag enthält keine APK-Datei")
            return UpdateInfo(tag.removePrefix("v").removePrefix("V"), url, size, json.optString("body"))
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Prüft und speichert das Ergebnis in den Einstellungen.
     * @return neuere Version oder null (aktuell)
     */
    fun checkAndStore(context: Context): UpdateInfo? {
        val s = GlobalSettings(context)
        val repo = normalizeRepo(s.updateRepo) ?: throw IOException("Kein gültiges GitHub-Repository eingestellt (Format: benutzer/repo)")
        val info = fetchLatest(repo)
        s.lastUpdateCheck = System.currentTimeMillis()
        return if (isNewer(info.version)) {
            s.updateVersion = info.version; s.updateApkUrl = info.apkUrl
            s.updateNotes = info.notes; s.updateSize = info.size
            info
        } else {
            s.updateVersion = ""; s.updateApkUrl = ""; s.updateNotes = ""; s.updateSize = 0
            null
        }
    }

    /** Gespeichertes, noch nicht installiertes Update */
    fun stored(context: Context): UpdateInfo? {
        val s = GlobalSettings(context)
        if (s.updateVersion.isBlank() || s.updateApkUrl.isBlank() || !isNewer(s.updateVersion)) return null
        return UpdateInfo(s.updateVersion, s.updateApkUrl, s.updateSize, s.updateNotes)
    }
}
