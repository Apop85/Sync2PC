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
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
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
    const val ID_PAUSED = 1008
    const val ID_STALE = 1009
    const val ID_GUARD = 1010
    /** Basis; je Profil eine eigene Kennung (Basis + Profil-ID mod 1000), damit sich Ergebnisse nicht überschreiben */
    const val ID_INTERVENTION = 20000

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val progress = NotificationChannel(CH_PROGRESS, str(R.string.notif_channel_progress), NotificationManager.IMPORTANCE_LOW).apply {
            description = str(R.string.notif_channel_progress_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        val alerts = NotificationChannel(CH_ALERTS, str(R.string.settings_notify_hints), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = str(R.string.notif_channel_alerts_desc)
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

    /** Wirft das System eine Meldung auf [channel] nicht weg? (Hinweise der App und der Kanal sind eingeschaltet) */
    private fun deliverable(context: Context, channel: String): Boolean = try {
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (context.getSystemService(NotificationManager::class.java)?.getNotificationChannel(channel)?.importance
                ?: NotificationManager.IMPORTANCE_DEFAULT) != NotificationManager.IMPORTANCE_NONE
    } catch (e: Exception) { true }

    fun formatBytes(b: Long): String = when {
        b >= 1L shl 30 -> String.format(Texts.locale, "%.1f GB", b / (1024.0 * 1024 * 1024))
        b >= 1L shl 20 -> String.format(Texts.locale, "%.1f MB", b / (1024.0 * 1024))
        b >= 1L shl 10 -> String.format(Texts.locale, "%.0f KB", b / 1024.0)
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
            title = p?.profileName ?: "LeoSync"
            text = p?.phase ?: str(R.string.preview_starting)
            pct = 0; indeterminate = true
        } else {
            pct = if (p.totalBytes > 0) ((p.doneBytes * 100) / p.totalBytes).toInt().coerceIn(0, 100) else 0
            title = str(R.string.notif_progress_title, p.profileName, p.doneFiles, p.totalFiles)
            text = str(R.string.notif_progress_text, formatBytes(p.doneBytes), formatBytes(p.totalBytes), pct)
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
        // Hält nur den laufenden Sync an; beim nächsten automatischen Start geht es weiter
        if (cancel != null) b.addAction(0, str(R.string.common_pause), cancel)
        return b.build()
    }

    fun foregroundInfo(context: Context, p: SyncProgress?, cancel: PendingIntent?): ForegroundInfo =
        ForegroundInfo(ID_PROGRESS, progress(context, p, cancel), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

    fun updateProgress(context: Context, p: SyncProgress?, cancel: PendingIntent?) {
        if (!canPost(context)) return
        try { NotificationManagerCompat.from(context).notify(ID_PROGRESS, progress(context, p, cancel)) } catch (ignored: SecurityException) {}
    }

    fun cancelProgress(context: Context) = NotificationManagerCompat.from(context).cancel(ID_PROGRESS)

    private fun post(
        context: Context, id: Int, channel: String, title: String, text: String, open: String?,
        largeImage: Int? = null,
    ) {
        if (!canPost(context)) return
        val b = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context, open, id))
        if (largeImage != null) {
            try { b.setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, largeImage)) } catch (ignored: Exception) {}
        }
        try { NotificationManagerCompat.from(context).notify(id, b.build()) } catch (ignored: SecurityException) {}
    }

    // Schalter in Einstellungen → Benachrichtigungen (0.4.0). Fortschritt und "Pausiert" sind nicht abschaltbar
    // (Android verlangt die Fortschrittsanzeige; "Pausiert" sichert das Fortsetzen ab).
    private fun s(context: Context) = com.sync2pc.app.data.GlobalSettings(context)

    /** Abschlussmeldung; bei Erfolg mit dem schlafenden Hund ("alles aktuell"). Erfolg: Schalter "Meldung nach jedem Sync", Fehler: "Hinweise" */
    fun summary(context: Context, text: String, error: Boolean) {
        if (if (error) !s(context).notifyHints else !s(context).notifySummary) return
        post(context, ID_SUMMARY, if (error) CH_ALERTS else CH_PROGRESS,
            str(if (error) R.string.notif_summary_problem else R.string.notif_summary_done), text, OPEN_LOG,
            largeImage = if (error) null else R.drawable.dog_idle)
    }

    fun decisionNeeded(context: Context, groups: Int, files: Int) {
        if (!s(context).notifyHints) return // steht auch auf der Startseite ("Ordnerauswahl nötig")
        post(context, ID_DECISION, CH_ALERTS, str(R.string.home_decisions_title),
            plural(R.plurals.notif_decision_text, files, files, plural(R.plurals.home_groups, groups)),
            OPEN_DECISIONS)
    }

    /** Ergebnis eines vorgemerkten Eingriffs (Aufräumen, Endgültig löschen, Speicher freigeben), wenn die Ansicht zu ist (0.11.10) */
    fun interventionDone(context: Context, profileId: Long, text: String): Boolean {
        // Antwort auf eine Aktion des Benutzers, daher unabhängig vom Schalter "Hinweise". false = nicht zustellbar
        // (keine Berechtigung): das Ergebnis bleibt dann in der App stehen (InterventionRegistry).
        // 0.12.3 (-039): auch zustellbar? Hinweise für LeoSync im System aus oder Kanal ausgeschaltet -> false.
        if (!canPost(context) || !deliverable(context, CH_ALERTS)) return false
        post(context, ID_INTERVENTION + (profileId % 1000).toInt(), CH_ALERTS, str(R.string.notif_summary_done), text, null)
        return true
    }

    fun deleteReady(context: Context, files: Int) {
        if (!s(context).notifyHints) return // steht auch auf der Startseite ("Gesicherte Dateien entfernen")
        post(context, ID_DELETE, CH_ALERTS, str(R.string.home_delete_title),
            plural(R.plurals.notif_delete_text, files),
            OPEN_DELETE)
    }

    /**
     * Fortschrittsanzeige im Zustand "Pausiert" (WLAN weg). Bleibt stehen, bis der Sync weiterläuft.
     * Reine Anzeige – Wegwischen hat keinen Einfluss auf das automatische Fortsetzen.
     */
    fun pausedNotification(context: Context, last: SyncProgress?, wifiNames: String, reason: String? = null): Notification {
        val pct = if (last != null && last.totalBytes > 0) ((last.doneBytes * 100) / last.totalBytes).toInt().coerceIn(0, 100) else 0
        val title = str(R.string.log_status_paused) + (last?.let { " – ${it.profileName}" } ?: "")
        val text = (reason ?: (if (wifiNames.isNotBlank()) str(R.string.notif_wait_wifi_named, wifiNames) else str(R.string.notif_wait_wifi))) +
            (if (last != null && last.totalFiles > 0) " · " + str(R.string.notif_paused_files, last.doneFiles, last.totalFiles) else "")
        val view = RemoteViews(context.packageName, R.layout.notification_paused).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_text, text)
            setProgressBar(R.id.notif_progress, 100, pct, false)
        }
        return NotificationCompat.Builder(context, CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openApp(context, null, 1))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(view)
            .build()
    }

    /** Fallback ohne Vordergrunddienst (falls Android den Start gerade nicht erlaubt) */
    fun showPaused(context: Context, last: SyncProgress?, wifiNames: String, reason: String? = null) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(ID_PAUSED, pausedNotification(context, last, wifiNames, reason))
        } catch (ignored: SecurityException) {}
    }

    fun cancelPaused(context: Context) = NotificationManagerCompat.from(context).cancel(ID_PAUSED)

    /** Alte Hinweis-Benachrichtigung aus 0.0.9 entfernen */
    fun cancelInterrupted(context: Context) = NotificationManagerCompat.from(context).cancel(ID_INTERRUPTED)

    fun conflictsNeeded(context: Context, count: Int) {
        if (!s(context).notifyHints) return // steht auch auf der Startseite ("Konflikte beim Ordner-Sync")
        post(context, ID_CONFLICT, CH_ALERTS, str(R.string.home_conflicts_title),
            plural(R.plurals.notif_conflict_text, count),
            OPEN_CONFLICTS)
    }

    /** Schutzbremse (0.4.1): Ordner-Profil ruht, bis der Benutzer auf der Startseite entscheidet */
    fun guardNeeded(context: Context, profileName: String) {
        if (!s(context).notifyHints) return // steht auch auf der Startseite
        post(context, ID_GUARD, CH_ALERTS, str(R.string.notif_guard_title),
            str(R.string.notif_guard_text, profileName),
            null, largeImage = R.drawable.dog_attention)
    }

    fun cancelGuard(context: Context) = NotificationManagerCompat.from(context).cancel(ID_GUARD)

    /** Erinnerung: seit [days] Tagen keine erfolgreiche Sicherung (0.2.0) */
    fun staleWarning(context: Context, days: Int, lines: List<String>) =
        post(context, ID_STALE, CH_ALERTS, plural(R.plurals.notif_stale_title, days),
            lines.joinToString("\n") + "\n" + str(R.string.notif_stale_text),
            null, largeImage = R.drawable.dog_wait)

    fun cancelStale(context: Context) = NotificationManagerCompat.from(context).cancel(ID_STALE)

    fun updateAvailable(context: Context, version: String) {
        if (!s(context).notifyUpdate) return // steht auch auf der Startseite ("Update verfügbar")
        post(context, ID_UPDATE, CH_ALERTS, str(R.string.home_update_title),
            str(R.string.notif_update_text, version), OPEN_UPDATE)
    }

    /** Nach einem Selbst-Update, falls Android den automatischen Neustart der App nicht erlaubt */
    fun updateInstalled(context: Context, version: String) {
        if (!s(context).notifyUpdate) return
        post(context, ID_UPDATE, CH_ALERTS, str(R.string.notif_installed_title),
            str(R.string.notif_installed_text, version), null)
    }
}
