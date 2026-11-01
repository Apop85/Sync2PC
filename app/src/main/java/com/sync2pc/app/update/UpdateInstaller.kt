package com.sync2pc.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.sync2pc.app.BuildConfig
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.ui.MainActivity
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Lädt die APK, prüft sie und übergibt sie dem Android-Paketinstaller. */
object UpdateInstaller {
    const val ACTION_INSTALL_STATUS = "com.sync2pc.app.INSTALL_STATUS"

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Lädt die APK in den Cache. onProgress(bytes, total) */
    fun download(context: Context, info: UpdateInfo, onProgress: (Long, Long) -> Unit): File {
        val dir = File(context.cacheDir, "update").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // alte Downloads entfernen
        val target = File(dir, "LeoSync-${info.version}.apk")
        val conn = URL(info.apkUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "LeoSync/${BuildConfig.VERSION_NAME}")
        try {
            if (conn.responseCode != 200) throw IOException("Download fehlgeschlagen (HTTP ${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.size
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        verify(context, target)
        return target
    }

    /** Sicherheitsprüfung: richtige App und wirklich neuer. Die Signatur prüft Android beim Installieren. */
    private fun verify(context: Context, apk: File) {
        val pi = context.packageManager.getPackageArchiveInfo(apk.path, 0)
            ?: throw IOException("Die heruntergeladene Datei ist keine gültige APK")
        if (pi.packageName != context.packageName)
            throw IOException("Die APK gehört zu '${pi.packageName}', nicht zu dieser App" +
                if (BuildConfig.DEBUG) " (Debug-Builds lassen sich nicht per Update aktualisieren)" else "")
        if (pi.longVersionCode <= BuildConfig.VERSION_CODE.toLong())
            throw IOException("Die APK ist nicht neuer als die installierte Version")
    }

    /**
     * Startet die Installation. Das Ergebnis kommt als Intent an die MainActivity
     * (ACTION_INSTALL_STATUS), die dann den Bestätigungsdialog von Android öffnet.
     */
    fun install(context: Context, apk: File, version: String) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out, 64 * 1024) }
                session.fsync(out)
            }
            GlobalSettings(context).updateInstalling = version
            val intent = Intent(context, MainActivity::class.java)
                .setAction(ACTION_INSTALL_STATUS)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pi = PendingIntent.getActivity(
                context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pi.intentSender)
        }
    }
}
