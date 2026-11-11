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
import com.sync2pc.app.data.messageText
import com.sync2pc.app.data.text
import com.sync2pc.app.data.triggerText
import com.sync2pc.app.mirror.LocalTree
import com.sync2pc.app.smb.SmbTarget
import com.sync2pc.app.sync.PcCheckMode
import com.sync2pc.app.sync.PcIdentity
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
 *
 * Seit 0.12.4 pseudonymisiert: Profil-, Rechner-, Freigabe-, WLAN- und Ordnernamen, Benutzernamen, IP-Adressen,
 * Pfade und genaue Dateinamen kommen nicht mehr vor (siehe [Redactor]). Der Verlauf-Export ([logExport]) ist ein
 * bewusster Export des Benutzers für sich selbst und bleibt unverändert.
 */
object Diagnostics {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY)
    private fun t(ms: Long): String = if (ms <= 0) "-" else fmt.format(Date(ms))

    /** Unique-Work-Namen, deren Zustand interessiert */
    private val WORK_NAMES = listOf(
        SyncScheduler.WORK_AUTO, SyncScheduler.WORK_MANUAL, SyncScheduler.WORK_FOLLOWUP,
        SyncScheduler.WORK_CONTENT, SyncScheduler.WORK_SAFETY, SyncScheduler.WORK_RETRY,
        SyncScheduler.WORK_REMOTE_POLL, SyncScheduler.WORK_RESUME, SyncScheduler.WORK_DEFERRED_AUTO,
        SyncScheduler.WORK_DEFERRED_MANUAL, "update-check", "stale-check", SyncScheduler.WORK_PAUSE_END,
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
        val r = Redactor()
        // Bekannte sensible Werte zuerst erfassen, damit sie auch in Log, Fehlertexten und Abstürzen ersetzt werden
        val profiles = safe { dao.profiles() } ?: emptyList()
        val currentSsid = safe { WifiHelper.currentSsid(ctx) }
        for (p in profiles) {
            r.register("Profil", p.name)
            SmbTarget.parse(p.smbPath)?.let { t ->
                r.register("Host", t.host); r.register("Freigabe", t.share); r.registerFolders(t.basePath)
            } ?: SmbTarget.hostOf(p.smbPath)?.let { r.register("Host", it) }
            p.smbUser.split('\\', '@').forEach { r.register("Benutzer", it) }
            r.registerFolders(p.remoteSubfolder)
            r.registerAll("WLAN", p.ssids())
            if (p.pcGuid.isNotBlank()) r.register("PC-Kennung", p.pcGuid)
            p.folders().forEach { r.registerFolders(it) }
            p.localTreeUri.takeIf { it.isNotBlank() }?.let { r.registerFolders(safe { LocalTree.label(Uri.parse(it)) } ?: "") }
        }
        currentSsid?.let { r.register("WLAN", it) }
        val runs = safe { dao.recentRuns(15) } ?: emptyList()
        runs.forEach { r.register("Profil", it.profileName) } // auch Profile, die es nicht mehr gibt
        buildString {
            appendLine("=== LeoSync Diagnose ===")
            appendLine("Hinweis: Namen, Benutzer, IP-Adressen, Pfade und Dateinamen sind maskiert (Profil-1, Host-1 …).")
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
            kv("WLAN", currentSsid?.let { r.pseudo("WLAN", it) } ?: "(nicht lesbar)")
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
            kv("syncOffWifi", s.syncOffWifi)
            kv("minFileAgeMinutes", s.minFileAgeMinutes)
            kv("Datenbankversion", safe { AppDatabase.get(ctx).openHelper.readableDatabase.version })
            kv("notifySummary", s.notifySummary)
            kv("notifyUpdate", s.notifyUpdate)
            kv("notifyHints", s.notifyHints)
            kv("previewLimit", s.previewLimit)
            kv("homeCountryIso", s.homeCountryIso)
            kv("parallelUploads", s.parallelUploads)
            kv("remotePollMinutes", s.remotePollMinutes)
            kv("updateRepo", s.updateRepo)
            kv("autoUpdateCheck", s.autoUpdateCheck)
            kv("lastUpdateCheck", t(s.lastUpdateCheck))
            kv("updateVersion", s.updateVersion.ifEmpty { "-" })
            kv("updateInstalling", s.updateInstalling.ifEmpty { "-" })
            kv("shareRetryAt", t(s.shareRetryAt))
            kv("shareUnreachable", r.scrub(s.shareUnreachable).ifEmpty { "-" })
            kv("resumeOnWifi", s.resumeOnWifi)
            kv("resumeSince", t(s.resumeSince))
            kv("lastWifiTriggerAt", t(s.lastWifiTriggerAt))
            kv("pausedUntil (Benutzer)", t(s.pausedUntil))
            kv("manualOnly (für immer pausiert)", s.manualOnly)
            kv("Profile warten auf ersten Start", s.awaitingFirstStart().joinToString(", ").ifEmpty { "-" })
            val guards = com.sync2pc.app.mirror.GuardStore(context)
            kv("Schutzbremse (wartet auf dich)", guards.all().joinToString(", ") {
                "#${it.profileId} ${it.kind}" + if (it.kind == com.sync2pc.app.mirror.GuardKind.MASS_DELETE) " ${it.count}/${it.total}" else ""
            }.ifEmpty { "-" })
            kv("staleWarn", if (s.staleWarnEnabled) "an, ${s.staleWarnDays} Tage, zuletzt ${t(s.lastStaleWarnAt)}" else "aus")
            kv("freeSpace", "${s.freeSpaceMonths} Monate, Favoriten behalten ${s.freeSpaceKeepFavorites}")
            kv("lastSkip", if (s.lastSkipAt > 0) "${t(s.lastSkipAt)} – ${r.scrub(s.lastSkipInfo)}" else "-")

            section("Profile")
            if (profiles.isEmpty()) appendLine("(keine)")
            for (p in profiles) {
                appendLine("[${p.id}] ${r.pseudo("Profil", p.name)} – ${p.type().name}, ${if (p.enabled) "aktiv" else "inaktiv"}, ${p.dir().name}")
                val target = SmbTarget.parse(p.smbPath)
                kv("  Freigabe", if (target != null) {
                    "${r.pseudo("Host", target.host)} / ${r.pseudo("Freigabe", target.share)}" +
                        (if (target.basePath.isNotBlank()) " + ${target.basePath.split('\\').count { it.isNotBlank() }} Unterordner" else "")
                } else if (p.smbPath.isBlank()) "-" else "${SmbTarget.hostOf(p.smbPath)?.let { r.pseudo("Host", it) } ?: "?"} (ohne Freigabe)")
                kv("  Unterordner am PC gesetzt", p.remoteSubfolder.isNotBlank())
                kv("  Benutzer gesetzt", p.smbUser.isNotBlank())
                kv("  Passwort gesetzt", p.smbPasswordEnc.isNotBlank())
                kv("  PC-Erkennung", p.pcMode().name)
                kv("  PC-Kennung", if (p.pcGuid.isBlank()) "-" else
                    "${r.pseudo("PC-Kennung", p.pcGuid)} (Host ${if (p.pcGuidHost.isBlank()) "-" else r.pseudo("Host", p.pcGuidHost)})")
                // Wie die Startseite: «PC erkannt / noch nicht erkannt» (gespeicherte Kennung gehört zum jetzigen Host)
                kv("  PC-Status", pcStatus(p.pcMode(), p.pcGuid, p.pcGuidHost, SmbTarget.hostOf(p.smbPath).orEmpty()))
                kv("  WLANs", p.ssids().joinToString(", ") { r.pseudo("WLAN", it) }.ifEmpty { "-" })
                if (p.type() == ProfileType.PHOTOS) {
                    kv("  Quellordner", p.folders().joinToString(", ") { r.folderPath(it) }.ifEmpty { "-" })
                    kv("  Fotos/Videos/Unterordner", "${p.includeImages}/${p.includeVideos}/${p.includeSubfolders}")
                    kv("  Löschen", "${p.delete().name}, Prüfen ${p.verify().name}")
                    kv("  Ordnerstruktur", p.layout().name + if (safe { dao.layoutLocked(p.id) } == true) " (gesperrt)" else "")
                    kv("  Gesicherte Dateien", safe { dao.syncedCountNow(p.id) })
                } else {
                    val uri = p.localTreeUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    kv("  Handy-Ordner", uri?.let { r.folderPath(LocalTree.label(it)) + if (LocalTree.hasPermission(ctx, it)) "" else " (ZUGRIFF FEHLT)" } ?: "-")
                    kv("  Löschungen übernehmen", p.propagateDeletes)
                    kv("  Konflikte", p.conflict().name)
                    kv("  Prüfintervall", "${p.remotePollMinutes} min")
                    kv("  Papierkorb-Frist", if (p.trashRetentionDays > 0) "${p.trashRetentionDays} Tage" else "nie")
                    kv("  Schutzbremse ab", "${p.massDeletePercent} %")
                    kv("  Alte Versionen behalten", p.keepVersions)
                    kv("  Abgeglichene Dateien", safe { dao.mirrorStateCount(p.id) })
                }
                // Muster können Namen enthalten: nur Anzahl und Form ("x*.x"), Buchstaben/Ziffern maskiert
                if (p.includePatterns.isNotBlank()) kv("  Einschluss-Regeln", patternShape(p.includePatterns))
                if (p.excludePatterns.isNotBlank()) kv("  Ausschluss-Regeln", patternShape(p.excludePatterns))
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
                        (it.description?.let { d -> " – ${r.scrub(d)}" } ?: ""))
                }
            }

            section("Abstürze (letzte ${CrashReporter.KEEP})")
            val crashes = safe { CrashReporter.read(ctx) } ?: emptyList()
            if (crashes.isEmpty()) appendLine("(keine)")
            crashes.forEachIndexed { i, c ->
                if (i > 0) appendLine("-----")
                appendLine(r.scrub(c))
            }

            section("Letzte Läufe")
            if (runs.isEmpty()) appendLine("(keine)")
            runs.forEach { appendLine(runLine(it, r2 = r)) }

            section("Warnungen und Fehler (letzte ${runs.size} Läufe)")
            var anyIssue = false
            for (run in runs) {
                val issues = (safe { dao.logs(run.id) } ?: emptyList()).filter { it.level == "W" || it.level == "E" }
                if (issues.isEmpty()) continue
                anyIssue = true
                appendLine("--- Lauf #${run.id} (${t(run.startedAt)}, ${r.pseudo("Profil", run.profileName)})")
                if (issues.size > 30) appendLine("… ${issues.size - 30} ältere ausgelassen")
                issues.takeLast(30).forEach { appendLine(logLine(it, r)) }
            }
            if (!anyIssue) appendLine("(keine)")

            section("Log der letzten 3 Läufe (gekürzt)")
            for (run in runs.take(3)) {
                appendLine("--- Lauf #${run.id} (${t(run.startedAt)}, ${r.pseudo("Profil", run.profileName)})")
                val lines = compress(safe { dao.logs(run.id) } ?: emptyList(), r)
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
                appendLine(runLine(r, localized = true))
                dao.logs(r.id).forEach { appendLine("  ${t(it.time)} ${it.level} ${it.text()}") }
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
        context.startActivity(Intent.createChooser(send, com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.log_share)))
    }

    /** Kontaktadresse für "Problem melden" (DEC-123) */
    const val SUPPORT_EMAIL = "leotools@protonmail.com"

    /**
     * "Problem melden" (0.11.5, DEC-121): öffnet die Mail-App mit Adresse, Betreff, Hinweistext und der Diagnose als
     * Anhang. Gesendet wird nichts automatisch – der Benutzer sieht alles und sendet selbst.
     * @return false, wenn keine Mail-App vorhanden ist (die Adresse wurde dann in die Zwischenablage kopiert)
     */
    suspend fun reportProblem(context: Context): Boolean {
        val text = build(context)
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // alte Exporte aufräumen
            File(dir, "LeoSync-Diagnose_${fileStamp()}.txt").apply { writeText(text, Charsets.UTF_8) }
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.report_subject, BuildConfig.VERSION_NAME))
            putExtra(Intent.EXTRA_TEXT, com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.report_body))
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // 0.11.7: Ein Chooser wirft ohne Mail-App keine Ausnahme (er zeigt nur "Keine Apps") -> vorher prüfen
        // (<queries> im Manifest nötig). Fällt die Prüfung selbst aus, wird der Chooser trotzdem versucht.
        val hasMailApp = try {
            context.packageManager.queryIntentActivities(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")), 0).isNotEmpty()
        } catch (e: Exception) { true }
        return try {
            if (!hasMailApp) throw android.content.ActivityNotFoundException()
            context.startActivity(Intent.createChooser(send, com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.report_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: android.content.ActivityNotFoundException) {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText("LeoSync", SUPPORT_EMAIL))
            false
        }
    }

    fun fileStamp(): String = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.GERMANY).format(Date())

    // ---- Hilfen -------------------------------------------------------------------
    /** Ordner aus "… war bereits in 2019\02" bzw. "… bereits in 2026\10 - Ägypten vorhanden" */
    private val ALREADY_FOLDER = Regex("""bereits in (.+?)(?: vorhanden)?$""")
    private val ALREADY_COUNT = Regex("""^= (\d+) Dateie?n? bereits""")

    private fun logLine(e: LogEntry, r: Redactor? = null): String =
        "${fmt.format(Date(e.time)).substringAfter(' ')} ${e.level} ${r?.scrub(e.message) ?: e.message}"

    /**
     * PC-Erkennungsstatus eines Profils wie auf der Startseite (gleiche Logik, [PcIdentity.storedFor]); nur ein Zustandswort.
     * «anderer PC» / «Kennung weicht ab» entsteht erst beim Anmelden und steht im Lauftext/Log.
     */
    internal fun pcStatus(mode: PcCheckMode, pcGuid: String, pcGuidHost: String, host: String): String = when {
        !mode.checksGuid -> "nicht relevant (nur WLAN)"
        PcIdentity.storedFor(pcGuid, pcGuidHost, host) != null -> "erkannt"
        else -> "noch nicht erkannt"
    }

    /** Form eines Filtermusters ohne Inhalt: Buchstaben/Ziffern -> "x", Platzhalter (*, ?) und Punkte bleiben */
    internal fun patternShape(patterns: String): String {
        val lines = patterns.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return "${lines.size} Regel(n): " + lines.joinToString(" ") { it.replace(Regex("[\\p{L}\\p{N}_-]+"), "x") }
    }

    /**
     * Fasst aufeinanderfolgende "= …"-Zeilen (Datei bereits vorhanden) zu einer Zeile zusammen.
     * Gilt auch für das alte Format bis 0.1.0 ("= bild.jpg war bereits in 2019\02").
     * Mit [r] werden Ordnernamen und der übrige Text maskiert (Jahr/Monat bleiben).
     */
    internal fun compress(entries: List<LogEntry>, r: Redactor? = null): List<String> {
        val out = ArrayList<String>()
        var count = 0
        var files = 0
        val folders = LinkedHashSet<String>()
        var first: LogEntry? = null
        fun flush() {
            val f = first ?: return
            // Mit Maskierung immer die zusammengefasste Form: so steht der Ordnername nie im Klartext
            out += if (count == 1 && r == null) logLine(f)
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
                ALREADY_FOLDER.find(e.message)?.groupValues?.get(1)?.let { folders += r?.folderPath(it, "\\") ?: it }
            } else {
                flush()
                out += logLine(e, r)
            }
        }
        flush()
        return out
    }

    /** [localized] = Kennungen in der aktiven Sprache (Verlauf-Export); sonst der gespeicherte Text (Diagnose, DEC-160) */
    private fun runLine(r: SyncRun, localized: Boolean = false, r2: Redactor? = null): String {
        val profile = r2?.pseudo("Profil", r.profileName) ?: r.profileName
        val message = if (localized) r.messageText() else r.message
        return "#${r.id} ${t(r.startedAt)} → ${t(r.finishedAt ?: 0L)} [${r.status}] $profile " +
            "(${(if (localized) r.triggerText() else r.trigger).let { r2?.scrub(it) ?: it }})" +
            " ↑${r.uploaded} ⤼${r.skipped} ✗${r.failed} ⏸${r.waiting}: ${r2?.scrub(message) ?: message}"
    }

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
