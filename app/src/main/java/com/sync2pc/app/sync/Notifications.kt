package com.sync2pc.app.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.sync2pc.app.R
import com.sync2pc.app.ui.MainActivity

object Notifications {
    const val CH_PROGRESS = "sync_progress"
    const val CH_ALERTS = "sync_alerts"

    const val ID_PROGRESS = 1001
    const val ID_SUMMARY = 1002
    const val ID_DECISION = 1003
    const val ID_DELETE = 1004
    const val ID_UPDATE = 1005

    const val EXTRA_OPEN = "open"
    const val OPEN_DECISIONS = "decisions"
    const val OPEN_DELETE = "delete"
    const val OPEN_LOG = "log"
    const val OPEN_UPDATE = "update"
    const val OPEN_CONFLICTS = "conflicts"
    const val ID_CONFLICT = 1006
    const val ID_INTERRUPTED = 1007

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val progress = NotificationChannel(CH_PROGRESS, "Synchronisierung", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Fortschritt der Übertragung"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        val alerts = NotificationChannel(CH_ALERTS, "Hinweise", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Ordnerauswahl, Löschbestätigung, Fehler"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannels(listOf(progress, alerts))
    }

    private fun openApp(context: Context, open: String?, req: Int): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (open != null) putExtra(EXTRA_OPEN, open)
        }
        return PendingIntent.getActivity(context, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun canPost(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33

    fun formatBytes(b: Long): String = when {
        b >= 1L shl 30 -> "%.1f GB".format(b / (1024.0 * 1024 * 1024))
        b >= 1L shl 20 -> "%.1f MB".format(b / (1024.0 * 1024))
        b >= 1L shl 10 -> "%.0f KB".format(b / 1024.0)
        else -> "$b B"
    }

    fun progress(context: Context, p: SyncProgress?, cancel: PendingIntent?): Notification {
        val b = NotificationCompat.Builder(context, CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openApp(context, null, 1))
        val title: String
        val text: String
        val pct: Int
        val indeterminate: Boolean
        if (p == null || p.totalFiles == 0) {
            title = p?.profileName ?: "Sync2PC"
            text = p?.phase ?: "Starte…"
            pct = 0; indeterminate = true
        } else {
            pct = if (p.totalBytes > 0) ((p.doneBytes * 100) / p.totalBytes).toInt().coerceIn(0, 100) else 0
            title = "${p.profileName}: ${p.doneFiles} / ${p.totalFiles} Dateien"
            text = "${formatBytes(p.doneBytes)} von ${formatBytes(p.totalBytes)} ($pct %)"
            indeterminate = false
        }
        // Standardtexte (Barrierefreiheit, Fallback) + eigenes Layout mit laufendem Hund
        b.setContentTitle(title).setContentText(text)
        val small = RemoteViews(context.packageName, R.layout.notification_progress_small).apply {
            setTextViewText(R.id.notif_title, if (indeterminate) "$title – $text" else "$title · $pct %")
            setProgressBar(R.id.notif_progress, 100, pct, indeterminate)
        }
        val big = RemoteViews(context.packageName, R.layout.notification_progress_big).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_text, text)
            setTextViewText(R.id.notif_file, p?.currentFile ?: "")
            setProgressBar(R.id.notif_progress, 100, pct, indeterminate)
        }
        b.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
        if (cancel != null) b.addAction(0, "Abbrechen", cancel)
        return b.build()
    }

    fun foregroundInfo(context: Context, p: SyncProgress?, cancel: PendingIntent?): ForegroundInfo =
        ForegroundInfo(ID_PROGRESS, progress(context, p, cancel), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

    fun updateProgress(context: Context, p: SyncProgress?, cancel: PendingIntent?) {
        if (!canPost(context)) return
        try { NotificationManagerCompat.from(context).notify(ID_PROGRESS, progress(context, p, cancel)) } catch (ignored: SecurityException) {}
    }

    fun cancelProgress(context: Context) = NotificationManagerCompat.from(context).cancel(ID_PROGRESS)

    private fun post(context: Context, id: Int, channel: String, title: String, text: String, open: String?) {
        if (!canPost(context)) return
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context, open, id))
            .build()
        try { NotificationManagerCompat.from(context).notify(id, n) } catch (ignored: SecurityException) {}
    }

    fun summary(context: Context, text: String, error: Boolean) =
        post(context, ID_SUMMARY, if (error) CH_ALERTS else CH_PROGRESS,
            if (error) "Sync2PC – Problem" else "Sync2PC – fertig", text, OPEN_LOG)

    fun decisionNeeded(context: Context, groups: Int, files: Int) =
        post(context, ID_DECISION, CH_ALERTS, "Ordnerauswahl nötig",
            "$files Dateien in $groups Gruppe(n) warten: Es gibt umbenannte Monatsordner. Tippen zum Zuordnen.",
            OPEN_DECISIONS)

    fun deleteReady(context: Context, files: Int) =
        post(context, ID_DELETE, CH_ALERTS, "Gesicherte Dateien entfernen",
            "$files Dateien sind verifiziert gesichert. Tippen, um sie vom Telefon zu entfernen.",
            OPEN_DELETE)

    /** Reiner Hinweis – Wegwischen hat keinen Einfluss auf das automatische Fortsetzen */
    fun interrupted(context: Context) =
        post(context, ID_INTERRUPTED, CH_PROGRESS, "Sync unterbrochen",
            "Das WLAN wurde getrennt. Der Sync wird automatisch fortgesetzt, sobald das Heim-WLAN wieder verbunden ist.",
            null)

    fun cancelInterrupted(context: Context) = NotificationManagerCompat.from(context).cancel(ID_INTERRUPTED)

    fun conflictsNeeded(context: Context, count: Int) =
        post(context, ID_CONFLICT, CH_ALERTS, "Konflikt beim Ordner-Sync",
            "$count Datei(en) wurden auf Handy und PC geändert. Tippen, um zu entscheiden, welche Version gilt.",
            OPEN_CONFLICTS)

    fun updateAvailable(context: Context, version: String) =
        post(context, ID_UPDATE, CH_ALERTS, "Update verfügbar",
            "Sync2PC $version ist verfügbar. Tippen zum Installieren.", OPEN_UPDATE)

    /** Nach einem Selbst-Update, falls Android den automatischen Neustart der App nicht erlaubt */
    fun updateInstalled(context: Context, version: String) =
        post(context, ID_UPDATE, CH_ALERTS, "Update installiert",
            "Sync2PC wurde auf Version $version aktualisiert. Tippen zum Öffnen.", null)
}
