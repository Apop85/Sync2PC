package com.sync2pc.app.diag

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.WorkManager
import com.sync2pc.app.BuildConfig
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.LogEntry
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncRun
import com.sync2pc.app.mirror.LocalTree
import com.sync2pc.app.sync.SyncScheduler
import com.sync2pc.app.sync.WifiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnosedaten für die Fehlersuche (Beta): Gerät, Hintergrund-Einschränkungen, Berechtigungen,
 * Einstellungen, Profile, geplante Aufträge und die letzten Läufe. Ohne Passwörter.
 */
object Diagnostics {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY)
    private fun t(ms: Long): String = if (ms <= 0) "-" else fmt.format(Date(ms))

    /** Unique-Work-Namen, deren Zustand interessiert */
    private val WORK_NAMES = listOf(
        SyncScheduler.WORK_AUTO, SyncScheduler.WORK_MANUAL, SyncScheduler.WORK_FOLLOWUP,
        SyncScheduler.WORK_CONTENT, SyncScheduler.WORK_PERIODIC, SyncScheduler.WORK_RETRY,
        SyncScheduler.WORK_REMOTE_POLL, SyncScheduler.WORK_RESUME, "update-check", "stale-check",
    )

    /** Kopfzeilen, die jeder Export trägt (Zuordnung zum Gerät) */
    fun header(context: Context): String = buildString {
        appendLine("Geräte-ID: ${GlobalSettings(context).deviceId}")
        appendLine("App: LeoSync ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE})")
        appendLine("Gerät: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE}), Android ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
        appendLine("Erstellt: ${t(System.currentTimeMillis())}")
    }

    suspend fun build(context: Context): String = withContext(Dispatchers.IO) {
        val ctx = context.applicationContext
        val s = GlobalSettings(ctx)
        val dao = AppDatabase.get(ctx).dao()
        buildString {
            appendLine("=== LeoSync Diagnose ===")
            append(header(ctx))
            appendLine("Hersteller-OS: ${Build.DISPLAY}, Sicherheitspatch ${Build.VERSION.SECURITY_PATCH}")

            section("Hintergrund und Akku")
            val pm = ctx.getSystemService(PowerManager::class.java)
            val am = ctx.getSystemService(ActivityManager::class.java)
            val usm = ctx.getSystemService(UsageStatsManager::class.java)
            val bm = ctx.getSystemService(BatteryManager::class.java)
            kv("Akku-Optimierung aus", safe { pm?.isIgnoringBatteryOptimizations(ctx.packageName) })
            kv("Energiesparmodus", safe { pm?.isPowerSaveMode })
            kv("Doze aktiv", safe { pm?.isDeviceIdleMode })
            kv("Hintergrund eingeschränkt", safe { am?.isBackgroundRestricted })
            kv("Standby-Bucket", safe { usm?.appStandbyBucket?.let { bucketName(it) } })
            kv("Hersteller-Hinweis", VendorHints.forDevice()?.vendor ?: "keiner")
            kv("Lädt", safe { bm?.isCharging })
            kv("Akkustand", safe { bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.let { "$it %" } })

            section("Berechtigungen")
            kv("Standort (genau)", perm(ctx, Manifest.permission.ACCESS_FINE_LOCATION))
            kv("Standort immer", perm(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            kv("Medien-Standort", perm(ctx, Manifest.permission.ACCESS_MEDIA_LOCATION))
            if (Build.VERSION.SDK_INT >= 33) {
                kv("Fotos", perm(ctx, Manifest.permission.READ_MEDIA_IMAGES))
                kv("Videos", perm(ctx, Manifest.permission.READ_MEDIA_VIDEO))
            } else {
                kv("Speicher lesen", perm(ctx, Manifest.permission.READ_EXTERNAL_STORAGE))
            }
            kv("Benachrichtigungen", safe { NotificationManagerCompat.from(ctx).areNotificationsEnabled() })
            kv("Apps installieren", safe { ctx.packageManager.canRequestPackageInstalls() })
            kv("Ordnerfreigaben (SAF)", safe { ctx.contentResolver.persistedUriPermissions.size })

            section("Netzwerk")
            kv("Standortdienst an", safe { WifiHelper.isLocationEnabled(ctx) })
            kv("Im WLAN", safe { WifiHelper.isOnWifi(ctx) })
            kv("SSID", safe { WifiHelper.currentSsid(ctx) ?: "(nicht lesbar)" })
            kv("Netz", safe {
                val cm = ctx.getSystemService(ConnectivityManager::class.java)
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                if (caps == null) "keins" else listOfNotNull(
                    "WLAN".takeIf { caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) },
                    "Mobil".takeIf { caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) },
                    "VPN".takeIf { caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) },
                    "ungemessen".takeIf { caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) },
                    "validiert".takeIf { caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) },
                ).joinToString(", ")
            })

            section("Einstellungen")
            kv("autoSyncOnWifi", s.autoSyncOnWifi)
            kv("syncOnNewMedia", s.syncOnNewMedia)
            kv("onlyWhileCharging", s.onlyWhileCharging)
            kv("minFileAgeMinutes", s.minFileAgeMinutes)
            kv("homeCountryIso", s.homeCountryIso)
            kv("parallelUploads", s.parallelUploads)
            kv("remotePollMinutes", s.remotePollMinutes)
            kv("updateRepo", s.updateRepo)
            kv("autoUpdateCheck", s.autoUpdateCheck)
            kv("lastUpdateCheck", t(s.lastUpdateCheck))
            kv("updateVersion", s.updateVersion.ifEmpty { "-" })
            kv("updateInstalling", s.updateInstalling.ifEmpty { "-" })
            kv("shareRetryAt", t(s.shareRetryAt))
            kv("shareUnreachable", s.shareUnreachable.ifEmpty { "-" })
            kv("resumeOnWifi", s.resumeOnWifi)
            kv("resumeSince", t(s.resumeSince))
            kv("lastWifiTriggerAt", t(s.lastWifiTriggerAt))
            kv("staleWarn", if (s.staleWarnEnabled) "an, ${s.staleWarnDays} Tage, zuletzt ${t(s.lastStaleWarnAt)}" else "aus")
            kv("freeSpace", "${s.freeSpaceMonths} Monate, Favoriten behalten ${s.freeSpaceKeepFavorites}")
            kv("lastSkip", if (s.lastSkipAt > 0) "${t(s.lastSkipAt)} – ${s.lastSkipInfo}" else "-")

            section("Profile")
            val profiles = safe { dao.profiles() } ?: emptyList()
            if (profiles.isEmpty()) appendLine("(keine)")
            for (p in profiles) {
                appendLine("[${p.id}] ${p.name} – ${p.type().name}, ${if (p.enabled) "aktiv" else "inaktiv"}, ${p.dir().name}")
                kv("  Share", p.smbPath + (if (p.remoteSubfolder.isNotBlank()) " + ${p.remoteSubfolder}" else ""))
                kv("  Benutzer gesetzt", p.smbUser.isNotBlank())
                kv("  Passwort gesetzt", p.smbPasswordEnc.isNotBlank())
                kv("  WLANs", p.ssids().joinToString(", ").ifEmpty { "-" })
                if (p.type() == ProfileType.PHOTOS) {
                    kv("  Quellordner", p.folders().joinToString(", ").ifEmpty { "-" })
                    kv("  Fotos/Videos/Unterordner", "${p.includeImages}/${p.includeVideos}/${p.includeSubfolders}")
                    kv("  Löschen", "${p.delete().name}, Prüfen ${p.mustVerify()}")
                    kv("  Gesicherte Dateien", safe { dao.syncedCountNow(p.id) })
                } else {
                    val uri = p.localTreeUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    kv("  Handy-Ordner", uri?.let { LocalTree.label(it) + if (LocalTree.hasPermission(ctx, it)) "" else " (ZUGRIFF FEHLT)" } ?: "-")
                    kv("  Löschungen übernehmen", p.propagateDeletes)
                    kv("  Konflikte", p.conflict().name)
                    kv("  Prüfintervall", "${p.remotePollMinutes} min")
                    kv("  Papierkorb-Frist", if (p.trashRetentionDays > 0) "${p.trashRetentionDays} Tage" else "nie")
                    kv("  Abgeglichene Dateien", safe { dao.mirrorStateCount(p.id) })
                }
                if (p.includePatterns.isNotBlank()) kv("  Whitelist", p.includePatterns.replace('\n', ' '))
                if (p.excludePatterns.isNotBlank()) kv("  Blacklist", p.excludePatterns.replace('\n', ' '))
            }

            section("Offen")
            kv("Ordner-Entscheidungen", safe { dao.decisionCount() })
            kv("Konflikte", safe { dao.openConflictCount() })
            kv("Lösch-Zustände", safe { dao.deleteStateCounts().joinToString(", ") { "${it.state}=${it.count}" } })

            section("Geplante Aufträge (WorkManager)")
            val wm = WorkManager.getInstance(ctx)
            for (name in WORK_NAMES) {
                val infos = safe { wm.getWorkInfosForUniqueWork(name).get() } ?: emptyList()
                if (infos.isEmpty()) { appendLine("$name: -"); continue }
                for (w in infos) {
                    val next = safe { w.nextScheduleTimeMillis }?.takeIf { it in 1 until Long.MAX_VALUE }
                    appendLine("$name: ${w.state}, Versuche ${w.runAttemptCount}" +
                        (next?.let { ", nächster Start ${t(it)}" } ?: "") +
                        (safe { w.stopReason }?.takeIf { it > 0 }?.let { ", Stopp-Grund $it" } ?: ""))
                }
            }

            section("Aktive Benachrichtigungen")
            val active = safe { ctx.getSystemService(NotificationManager::class.java)?.activeNotifications?.toList() } ?: emptyList()
            if (active.isEmpty()) appendLine("(keine)")
            active.forEach { appendLine("ID ${it.id}, Kanal ${it.notification.channelId}, seit ${t(it.postTime)}") }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                section("Letzte Prozess-Enden (Android)")
                val exits = safe { am?.getHistoricalProcessExitReasons(ctx.packageName, 0, 12) } ?: emptyList()
                if (exits.isEmpty()) appendLine("(keine)")
                // Hersteller-Killer (z. B. OnePlus/Oppo "o-kill"): Grund OTHER oder Kennung in der Beschreibung
                val vendorKills = exits.filter { it.reason == 13 || it.description?.contains("kill", ignoreCase = true) == true }
                if (vendorKills.isNotEmpty()) {
                    val active = vendorKills.count { it.importance <= 125 } // 100 Vordergrund, 125 Vordergrunddienst
                    val last = vendorKills.maxOf { it.timestamp }
                    appendLine("⚠ Hersteller-Killer: ${vendorKills.size}×, zuletzt ${t(last)} " +
                        "($active× während die App aktiv war, z. B. Sync)")
                }
                exits.forEach {
                    appendLine("${t(it.timestamp)}: ${exitReason(it.reason)}, Wichtigkeit ${it.importance}" +
                        (it.description?.let { d -> " – $d" } ?: ""))
                }
            }

            section("Abstürze (letzte ${CrashReporter.KEEP})")
            val crashes = safe { CrashReporter.read(ctx) } ?: emptyList()
            if (crashes.isEmpty()) appendLine("(keine)")
            crashes.forEachIndexed { i, c ->
                if (i > 0) appendLine("-----")
                appendLine(c)
            }

            section("Letzte Läufe")
            val runs = safe { dao.recentRuns(15) } ?: emptyList()
            if (runs.isEmpty()) appendLine("(keine)")
            runs.forEach { appendLine(runLine(it)) }

            section("Warnungen und Fehler (letzte ${runs.size} Läufe)")
            var anyIssue = false
            for (r in runs) {
                val issues = (safe { dao.logs(r.id) } ?: emptyList()).filter { it.level == "W" || it.level == "E" }
                if (issues.isEmpty()) continue
                anyIssue = true
                appendLine("--- Lauf #${r.id} (${t(r.startedAt)}, ${r.profileName})")
                if (issues.size > 30) appendLine("… ${issues.size - 30} ältere ausgelassen")
                issues.takeLast(30).forEach { appendLine(logLine(it)) }
            }
            if (!anyIssue) appendLine("(keine)")

            section("Log der letzten 3 Läufe (gekürzt)")
            for (r in runs.take(3)) {
                appendLine("--- Lauf #${r.id} (${t(r.startedAt)}, ${r.profileName})")
                val lines = compress(safe { dao.logs(r.id) } ?: emptyList())
                if (lines.size > 80) appendLine("… ${lines.size - 80} ältere Zeilen ausgelassen")
                lines.takeLast(80).forEach { appendLine(it) }
            }
            appendLine()
            appendLine("=== Ende ===")
        }
    }

    /** Vollständiger Verlauf (alle gespeicherten Läufe mit Log) als Text */
    suspend fun logExport(context: Context): String = withContext(Dispatchers.IO) {
        val ctx = context.applicationContext
        val dao = AppDatabase.get(ctx).dao()
        buildString {
            appendLine("=== LeoSync Verlauf ===")
            append(header(ctx))
            for (r in dao.recentRuns(100)) {
                appendLine()
                appendLine(runLine(r))
                dao.logs(r.id).forEach { appendLine("  ${t(it.time)} ${it.level} ${it.message}") }
            }
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText("LeoSync Diagnose", text))
        // Ab Android 13 zeigt das System selbst eine Bestätigung
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Diagnosedaten kopiert", Toast.LENGTH_SHORT).show()
    }

    /** Text als Datei über das Android-Teilen-Menü weitergeben */
    suspend fun shareAsFile(context: Context, text: String, fileName: String) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // alte Exporte aufräumen
            File(dir, fileName).apply { writeText(text, Charsets.UTF_8) }
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Verlauf teilen"))
    }

    fun fileStamp(): String = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.GERMANY).format(Date())

    // ---- Hilfen -------------------------------------------------------------------
    /** Ordner aus "… war bereits in 2019\02" bzw. "… bereits in 2026\10 - Ägypten vorhanden" */
    private val ALREADY_FOLDER = Regex("""bereits in (.+?)(?: vorhanden)?$""")
    private val ALREADY_COUNT = Regex("""^= (\d+) Dateie?n? bereits""")

    private fun logLine(e: LogEntry): String =
        "${fmt.format(Date(e.time)).substringAfter(' ')} ${e.level} ${e.message}"

    /**
     * Fasst aufeinanderfolgende "= …"-Zeilen (Datei bereits vorhanden) zu einer Zeile zusammen.
     * Gilt auch für das alte Format bis 0.1.0 ("= bild.jpg war bereits in 2019\02").
     */
    internal fun compress(entries: List<LogEntry>): List<String> {
        val out = ArrayList<String>()
        var count = 0
        var files = 0
        val folders = LinkedHashSet<String>()
        var first: LogEntry? = null
        fun flush() {
            val f = first ?: return
            out += if (count == 1) logLine(f)
            else "${fmt.format(Date(f.time)).substringAfter(' ')} I = $files Dateien bereits vorhanden ($count Einträge" +
                (if (folders.isNotEmpty()) ": ${folders.take(6).joinToString(", ")}${if (folders.size > 6) ", …" else ""}" else "") + ")"
            count = 0; files = 0; folders.clear(); first = null
        }
        for (e in entries) {
            if (e.level == "I" && e.message.startsWith("= ")) {
                if (first == null) first = e
                count++
                // Ab 0.1.1 "= 37 Dateien bereits in …", davor eine Zeile pro Datei
                files += ALREADY_COUNT.find(e.message)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                ALREADY_FOLDER.find(e.message)?.groupValues?.get(1)?.let { folders += it }
            } else {
                flush()
                out += logLine(e)
            }
        }
        flush()
        return out
    }

    private fun runLine(r: SyncRun): String =
        "#${r.id} ${t(r.startedAt)} → ${t(r.finishedAt ?: 0L)} [${r.status}] ${r.profileName} (${r.trigger})" +
            " ↑${r.uploaded} ⤼${r.skipped} ✗${r.failed} ⏸${r.waiting}: ${r.message}"

    private fun StringBuilder.section(title: String) {
        appendLine()
        appendLine("## $title")
    }

    private fun StringBuilder.kv(key: String, value: Any?) {
        appendLine("$key: ${value ?: "?"}")
    }

    private inline fun <T> safe(block: () -> T): T? = try { block() } catch (e: Exception) { null }

    private fun perm(ctx: Context, p: String): String =
        if (ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED) "ja" else "nein"

    private fun bucketName(b: Int): String = when (b) {
        5 -> "ACTIVE"
        10 -> "WORKING_SET"
        20 -> "FREQUENT"
        30 -> "RARE"
        40 -> "RESTRICTED"
        50 -> "NEVER"
        else -> "?"
    } + " ($b)"

    private fun exitReason(r: Int): String = when (r) {
        1 -> "EXIT_SELF"
        2 -> "SIGNALED"
        3 -> "LOW_MEMORY"
        4 -> "CRASH"
        5 -> "CRASH_NATIVE"
        6 -> "ANR"
        7 -> "INITIALIZATION_FAILURE"
        8 -> "PERMISSION_CHANGE"
        9 -> "EXCESSIVE_RESOURCE_USAGE"
        10 -> "USER_REQUESTED"
        11 -> "USER_STOPPED"
        12 -> "DEPENDENCY_DIED"
        13 -> "OTHER"
        14 -> "FREEZER"
        15 -> "PACKAGE_STATE_CHANGE"
        16 -> "PACKAGE_UPDATED"
        else -> "UNKNOWN"
    } + " ($r)"
}
