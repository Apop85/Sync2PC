package com.sync2pc.app.ui

import com.sync2pc.app.data.RunStatus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sync2pc.app.BuildConfig
import com.sync2pc.app.R
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.Profile
import androidx.compose.foundation.layout.offset
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncRun
import com.sync2pc.app.data.messageText
import com.sync2pc.app.mirror.Guard
import com.sync2pc.app.mirror.GuardKind
import com.sync2pc.app.mirror.GuardStore
import com.sync2pc.app.mirror.MirrorGuard
import com.sync2pc.app.mirror.PurgeStore
import com.sync2pc.app.mirror.RestoreFailures
import com.sync2pc.app.mirror.RestoreManager
import com.sync2pc.app.sync.Notifications
import com.sync2pc.app.sync.SyncProgress
import com.sync2pc.app.sync.SyncScheduler
import com.sync2pc.app.sync.SyncWorker
import com.sync2pc.app.sync.WifiHelper
import com.sync2pc.app.update.UpdateChecker
import kotlinx.coroutines.flow.combine

// Karten und Dialoge der Startseite (0.9.4 aus HomeScreen.kt ausgelagert)

/** Produktseite mit Hilfe (0.11.0, Inhaber) */
internal const val HELP_URL = "https://apop85.github.io/leosync/"

/**
 * Hilfeabschnitt «Verschlüsselung einschalten» (SYNC-20261111-159). Die Website pflegt der Inhaber nach dem Ordner docs/hilfe;
 * die Anker (Überschrift als Kleinbuchstaben-Kennung) sind dort zu bestätigen. Fehlt ein Anker, öffnet die Seite oben.
 */
internal fun encryptionHelpUrl(): String =
    if (Texts.isGerman) HELP_URL + "hilfe.html#verschluesselung-einschalten"
    else HELP_URL + "en/help.html#turning-on-encryption"

// Restzeit nur bei belastbarer Übertragungsrate (SYNC-20261109-063): mindestens so lange gemessen, so viel übertragen
// und höchstens so lang (sonst «wird berechnet»). Verhindert Anzeigen wie «noch ca. 411967 Std.» zu Beginn eines Laufs.
internal const val ETA_MIN_ELAPSED_MS = 60_000L
internal const val ETA_MIN_BYTES = 2L * 1024 * 1024
internal const val ETA_MAX_SECONDS = 72L * 3600

/** Geschätzte Restzeit in Sekunden oder null, wenn die Schätzung (noch) nicht belastbar ist. */
internal fun etaSeconds(elapsedMs: Long, doneBytes: Long, totalBytes: Long): Long? {
    val remaining = totalBytes - doneBytes
    if (remaining <= 0 || elapsedMs < ETA_MIN_ELAPSED_MS || doneBytes < ETA_MIN_BYTES) return null
    val seconds = remaining.toDouble() * elapsedMs / doneBytes / 1000.0
    return if (seconds.isFinite() && seconds <= ETA_MAX_SECONDS) seconds.toLong() else null
}

/** Geschätzte Restzeit: "noch ca. 5 Min." bzw. "noch ca. 2 Std. 10 Min." (0.11.0) */
internal fun formatEta(seconds: Long): String {
    val min = ((seconds + 59) / 60).coerceAtLeast(1)
    val text = if (min < 60) plural(R.plurals.eta_minutes, min.toInt())
        else str(R.string.eta_hours, min / 60, min % 60)
    return str(R.string.home_eta, text)
}

/** "14:30", "morgen 14:30" oder "05.11. 14:30" */
internal fun pauseEndText(until: Long): String {
    val day = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ROOT)
    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ROOT).format(java.util.Date(until))
    val now = System.currentTimeMillis()
    return when (day.format(java.util.Date(until))) {
        day.format(java.util.Date(now)) -> time
        day.format(java.util.Date(now + 24 * 3_600_000L)) -> str(R.string.home_tomorrow, time)
        else -> java.text.SimpleDateFormat(str(R.string.fmt_day_month), Texts.locale).format(java.util.Date(until)) + " $time"
    }
}

/**
 * Wie lange pausieren? Schnellwahl 1/4/24 Stunden, eigene Anzahl (1–168) oder "für immer" (nur manuell).
 * Seit 0.9.0 wird zuerst gewählt (sichtbar markiert) und dann mit "Pausieren" bestätigt.
 */
@Composable
internal fun PauseDialog(onDismiss: () -> Unit, onPause: (Int) -> Unit, onForever: () -> Unit) {
    /** Gewählte Schnellwahl in Stunden, FOREVER = für immer, null = nichts bzw. eigene Anzahl */
    var preset by remember { mutableStateOf<Int?>(null) }
    var hoursText by remember { mutableStateOf("") }
    val custom = hoursText.toIntOrNull()?.takeIf { it in 1..168 }
    val forever = preset == PAUSE_FOREVER
    val hours = if (hoursText.isNotEmpty()) custom else preset?.takeIf { it != PAUSE_FOREVER }
    val valid = forever || hours != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(R.string.home_pause_title)) },
        text = {
            // SYNC-20261111-218 B1: scrollbar und geclippt, damit Hinweis und eingebettetes Icon bei Schrift 2,0 erreichbar bleiben
            DialogScroll(Arrangement.spacedBy(8.dp)) {
                Hint(str(R.string.home_pause_hint))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 4, 24).forEach { h ->
                        FilterChip(
                            selected = preset == h && hoursText.isEmpty(),
                            onClick = { preset = h; hoursText = "" },
                            label = { Text("$h h") },
                        )
                    }
                }
                OutlinedTextField(
                    hoursText, { v -> hoursText = v.filter { it.isDigit() }.take(3); if (hoursText.isNotEmpty()) preset = null },
                    label = { Text(str(R.string.home_pause_hours)) },
                    isError = hoursText.isNotEmpty() && custom == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                FilterChip(
                    selected = forever,
                    onClick = { preset = PAUSE_FOREVER; hoursText = "" },
                    label = { Text(str(R.string.home_pause_forever)) },
                )
                Hint(str(R.string.home_pause_forever_hint))
                if (!valid) Hint(str(R.string.home_pause_choose))
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { if (forever) onForever() else hours?.let(onPause) }) {
                Text(str(R.string.common_pause))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str(R.string.common_cancel)) } },
    )
}

/** Kennung für "für immer" im Pause-Dialog */
internal const val PAUSE_FOREVER = -1

@Composable
internal fun StatusCard(
    ssid: String?,
    isRunning: Boolean,
    isQueued: Boolean,
    waitText: String,
    nextPlanned: Long?,
    progress: SyncProgress?,
    lastRun: SyncRun?,
    shareWait: String?,
    skipInfo: String?,
    /** Der Grund stammt von einem manuellen Start (SYNC-20261111-188 B1) */
    skipManual: Boolean = false,
    paused: Boolean,
    userPausedUntil: Long,
    /** Überschrift nach dem Gesamtzustand (siehe [HomeStatus.headline]); Konflikt/Verbindung/Profil fliessen dort ein */
    headline: HomeHeadline,
    /** Anzahl offener Entscheidungen (für die Überschrift «n Entscheidungen erforderlich») */
    attentionCount: Int,
    /** Zeile unter der Überschrift bei offenen Entscheidungen, z. B. «1 Konflikt wartet auf deine Entscheidung.» */
    attentionText: String,
    /** Letzter Lauf mit Fehlern: Anzahl (0 = Lauf fehlgeschlagen ohne Einzelfehler) */
    lastRunErrors: Int,
    /** Dateien, die beim nächsten Sync folgen (SYNC-20261111-177), nur bei [HomeHeadline.PENDING] von Bedeutung */
    pendingFiles: Int,
    /** Dateien, die lokal auf die Übertragung warten (SYNC-20261111-199); nur bei [HomeHeadline.NO_CONNECTION] angezeigt */
    waitingFiles: Int = 0,
    /** Standort-Berechtigung fehlt, obwohl der WLAN-Name gebraucht wird (SYNC-20261111-176 R4) */
    locationMissing: Boolean,
    onOpenPermissions: () -> Unit,
    onSync: () -> Unit,
    onCancel: () -> Unit,
    onResume: () -> Unit,
    onOpenLog: () -> Unit,
    hasProfiles: Boolean,
    etaText: String? = null,
    /** Platz für eine zusätzliche Hinweiszeile in der Laufanzeige (z. B. «Danach: …»); null = nichts */
    runHint: String? = null,
) {
    val userPaused = userPausedUntil > 0
    SectionCard(title = str(R.string.home_status)) {
        // Zustandsbild folgt der Überschrift (HomeStatus.dogFor); Vorrang steht in HomeStatus.headline
        DogStatus(HomeStatus.dogFor(headline, userPaused), Modifier.align(Alignment.CenterHorizontally))
        // Aufbau (A5, SYNC-20261110-113): 1. Überschrift mit Symbol (immer 1–2 Zeilen, linksbündig wie alle Zeilen),
        // 2. WLAN (einzeilig), 3. Zustandsbereich mit Mindesthöhe für die üblichen Fälle
        // (2 Zeilen Haupttext + Balken bzw. Hinweis + 2 kleine Zeilen, davon eine für runHint). Nur lange Meldungen machen ihn grösser.
        val typo = MaterialTheme.typography
        val density = LocalDensity.current
        val statusMin = with(density) {
            (typo.bodyLarge.lineHeight * 2).toDp() + (typo.bodySmall.lineHeight * 2).toDp() + 24.dp
        }
        StatusHeadline(
            headline, attentionCount, lastRunErrors,
            onClick = if (headline == HomeHeadline.ERRORS) onOpenLog else null,
        )
        // Zeile bleibt immer stehen (gleiche Höhe, nichts verschiebt sich); ohne lesbaren WLAN-Namen bleibt sie leer
        Text(if (ssid != null) str(R.string.home_wifi, ssid) else " ", maxLines = 1, overflow = TextOverflow.Ellipsis)
        Column(Modifier.fillMaxWidth().heightIn(min = statusMin), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            isRunning -> {
                // Feste Zeilenzahl während des Laufs (0.9.2), damit die Kachel nicht springt: Fortschritt bzw. Phase
                // immer 2 Zeilen, Dateiname immer genau 1 Zeile (auch wenn noch keiner bekannt ist)
                val withFiles = progress?.takeIf { it.totalFiles > 0 }
                val mainText = if (withFiles != null) str(R.string.home_progress, withFiles.profileName.trim(),
                    withFiles.doneFiles, withFiles.totalFiles, Notifications.formatBytes(withFiles.doneBytes),
                    Notifications.formatBytes(withFiles.totalBytes))
                    else progress?.phase ?: str(R.string.home_starting) // die Überschrift sagt schon «Synchronisierung läuft»
                Text(mainText, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (withFiles != null) {
                    val f = if (withFiles.totalBytes > 0) withFiles.doneBytes.toFloat() / withFiles.totalBytes else 0f
                    LinearProgressIndicator(progress = { f.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    // Restzeit (0.11.0) vor dem Dateinamen, sobald sie geschätzt werden kann
                    listOfNotNull(etaText, progress?.currentFile).joinToString(" · "), minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Platz für eine zusätzliche Hinweiszeile («Danach: …»), die der Mindesthöhe entspricht (zweite kleine Zeile)
                // -146: einzeilig, vom Screenreader höflich angesagt (polite), Höhe liegt in der Mindesthöhe der Karte
                if (runHint != null) Text(
                    runHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
                )
            }
            // Ohne Profil: «Lege zuerst ein Profil an» als Hauptaussage, keine Zeitangaben gelöschter Profile (A9)
            !hasProfiles -> Text(str(R.string.home_sync_needs_profile))
            // Offene Entscheidung: nie «alles aktuell» (A1); Zeitpunkt des letzten Laufs bleibt als kleine Zeile
            headline == HomeHeadline.ATTENTION -> {
                Text(attentionText)
                lastRun?.let { Hint(str(R.string.home_last_sync, formatTime(it.finishedAt ?: it.startedAt), it.profileName.trim())) }
            }
            headline == HomeHeadline.NO_ACTIVE_PROFILE -> Text(str(R.string.home_no_active_detail))
            // R6: Die Überschrift «⏸ Pausiert» steht in der Überschriftszeile, hier nur die Detailzeile
            userPaused && userPausedUntil == Long.MAX_VALUE -> {
                Text(str(R.string.home_manual_only))
                Hint(str(R.string.home_manual_only_hint))
                lastRun?.let { Hint(str(R.string.home_last_sync, formatTime(it.finishedAt ?: it.startedAt), it.profileName.trim())) }
            }
            userPaused -> {
                Text(str(R.string.home_paused_until, pauseEndText(userPausedUntil)))
                Hint(str(R.string.home_paused_until_hint))
            }
            paused -> {
                Text(str(R.string.home_paused_wifi))
                Hint(str(R.string.home_paused_wifi_hint))
            }
            shareWait != null -> {
                Text(shareWait, color = MaterialTheme.colorScheme.error)
                Hint(str(R.string.home_pc_unreachable_hint))
            }
            // Soll-WLAN nicht verbunden: Ursache und Handlung statt grünem Gut-Zustand (A2)
            // B1: Gibt es einen konkreten Grund (skipInfo, rote Zeile unten), erklärt er den Zustand besser als der allgemeine Hinweis
            headline == HomeHeadline.NO_CONNECTION && skipInfo == null -> {
                Text(str(R.string.home_no_connection_wifi))
                Hint(str(R.string.home_no_connection_hint))
            }
            isQueued -> Text(waitText)
            lastRun != null -> {
                Text(str(R.string.home_last_sync, formatTime(lastRun.finishedAt ?: lastRun.startedAt), lastRun.profileName.trim()))
                val nothing = lastRun.status == RunStatus.OK && lastRun.uploaded == 0 && lastRun.failed == 0
                // «Nichts zu tun – alles aktuell» nur, wenn die Überschrift ebenfalls «aktuell» sagt
                if (headline == HomeHeadline.ERRORS) Hint(str(R.string.home_status_errors_hint))
                // -177: Ist ein Folgelauf geplant, nennt ihn die Zeile «Nächste Prüfung um …» (keine Zeit im Hinweis);
                // sonst «… folgen mit der nächsten Prüfung» (kein Zeitversprechen)
                else if (headline == HomeHeadline.PENDING) Hint(plural(
                    if (nextPlanned != null) R.plurals.home_pending_hint else R.plurals.home_pending_hint_check,
                    pendingFiles, pendingFiles))
                else if (nothing && headline == HomeHeadline.UP_TO_DATE) Hint(str(R.string.home_nothing_to_do))
                else if (!nothing) Hint(lastRun.messageText())
            }
            else -> Text(str(R.string.home_no_sync_yet))
        }
        // -199: Keine Verbindung, aber Dateien warten: die Zahl nennen (der Hund sitzt, die Synchronisierung steht noch aus)
        if (headline == HomeHeadline.NO_CONNECTION && !isRunning && waitingFiles > 0)
            Text(plural(R.plurals.home_waiting_files, waitingFiles, waitingFiles))
        // Zeitversetzt geplante Prüfung (z. B. Nachhol-Sync für gerade erst aufgenommene Fotos); ohne Profil nie (A9)
        if (hasProfiles && !isRunning && !userPaused && !isQueued && nextPlanned != null)
            Hint(str(R.string.home_next_check, pauseEndText(nextPlanned)))
        if (hasProfiles && !isRunning && !userPaused && shareWait == null && skipInfo != null) {
            Text(str(if (skipManual) R.string.home_skipped_manual else R.string.home_skipped, skipInfo), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        // R4: Fehlende Standort-Berechtigung nie still lassen: Zeile mit Symbol und Weg zur Lösung
        if (locationMissing && !isRunning) {
            Text(str(R.string.home_location_missing), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpenPermissions) { Text(str(R.string.settings_perm_title), maxLines = 1) }
        }
        }
        // Zwei gleich breite Knöpfe, Text einzeilig (kein Umbruch wie "Vorsc-hau")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val w = Modifier.weight(1f)
            // "Pausieren" hält nur den laufenden Sync an; beim nächsten automatischen Start geht es weiter
            if (isRunning) OutlinedButton(onClick = onCancel, modifier = w) { Text(str(R.string.common_pause), maxLines = 1) }
            // Befristete Pause -> "Fortsetzen"; "für immer" -> weiterhin manuell synchronisieren können
            else if (userPaused && userPausedUntil != Long.MAX_VALUE) Button(onClick = onResume, modifier = w) { Text(str(R.string.home_resume), maxLines = 1) }
            else Button(onClick = onSync, enabled = hasProfiles, modifier = w) { Text(str(R.string.home_sync), maxLines = 1) }
            // "Simulieren" ist seit 0.3.6 pro Profil im Profil-Bildschirm
        }
    }
}

/**
 * Überschrift der Statuskarte: Symbol plus Text, nie nur Farbe (A4). Warnzustände mit Warnzeichen, «nichts eingerichtet»
 * mit neutralem Info-Symbol. Linksbündig wie die übrigen Zeilen; 1–2 Zeilen, damit lange Schrift den Text nicht abschneidet.
 * Zustandswechsel werden dem Screenreader angesagt (Live-Bereich).
 */
@Composable
private fun StatusHeadline(headline: HomeHeadline, attentionCount: Int, errors: Int, onClick: (() -> Unit)?) {
    val text = when (headline) {
        HomeHeadline.RUNNING -> str(R.string.home_running)
        HomeHeadline.NOT_CONFIGURED -> str(R.string.home_status_not_configured)
        HomeHeadline.NO_ACTIVE_PROFILE -> str(R.string.home_status_no_active)
        HomeHeadline.ATTENTION -> plural(R.plurals.home_status_attention, attentionCount.coerceAtLeast(1))
        HomeHeadline.PAUSED -> str(R.string.home_status_paused) // R6: Überschrift «⏸ Pausiert», Detail im Zustandsbereich
        HomeHeadline.PENDING -> str(R.string.home_status_pending)
        HomeHeadline.NO_CONNECTION -> str(R.string.home_status_no_connection)
        HomeHeadline.ERRORS ->
            if (errors > 0) plural(R.plurals.home_status_errors, errors) else str(R.string.home_status_failed)
        HomeHeadline.UP_TO_DATE -> str(R.string.home_all_saved)
        HomeHeadline.IDLE -> str(R.string.home_status_idle)
    }
    if (text.isEmpty()) return
    val warn = HomeStatus.showsWarning(headline)
    val color = if (headline == HomeHeadline.ERRORS || headline == HomeHeadline.ATTENTION)
        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (warn) { Icon(Icons.Filled.Warning, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        else if (headline == HomeHeadline.NOT_CONFIGURED) {
            Icon(Icons.Filled.Info, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = color, minLines = 1, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
    }
}

/** Eintrag in der Sektion "Tools" (0.5.0,seit 0.6.1 eine Zeile pro Tool, seit 0.6.3 ausgegraut möglich) */
internal class ToolTile(
    val id: String, val icon: ImageVector, val title: String, val text: String,
    val enabled: Boolean = true, val disabledHint: String? = null,
    val onClick: () -> Unit,
)

@Composable
internal fun ToolCard(t: ToolTile, modifier: Modifier) {
    val ctx = LocalContext.current
    Card(
        modifier = modifier.alpha(if (t.enabled) 1f else 0.45f).clickable {
            if (t.enabled) t.onClick()
            else t.disabledHint?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show() }
        },
        colors = CardDefaults.cardColors(containerColor =
            if (t.enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(t.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(t.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

/** Schutzbremse (0.4.1): Ordner-Profil ruht, bis du entscheidest */
@Composable
internal fun GuardCard(g: Guard, profileName: String, onChooseFolder: () -> Unit, onChoice: (MirrorGuard.Choice) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null); Spacer(Modifier.width(8.dp))
                Text(MirrorGuard.title(g), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(MirrorGuard.text(g, profileName))
            val full = Modifier.fillMaxWidth()
            when (g.kind) {
                GuardKind.REMOTE_MISSING, GuardKind.LOCAL_MISSING -> {
                    Button(onClick = onChooseFolder, modifier = full) { Text(str(R.string.guard_choose_new)) }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.ACCEPT_FOLDER) }, modifier = full) {
                        Text(str(R.string.guard_recheck))
                    }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.RESET_AS_NEW) }, modifier = full) {
                        Text(str(R.string.guard_reset))
                    }
                    Hint(str(R.string.guard_reset_hint))
                }
                GuardKind.MASS_DELETE -> {
                    Button(onClick = { onChoice(MirrorGuard.Choice.KEEP_FILES) }, modifier = full) { Text(str(R.string.guard_keep)) }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.DELETE_ANYWAY) }, modifier = full) {
                        Text(str(R.string.guard_delete_anyway))
                    }
                    Hint(str(R.string.guard_keep_hint))
                }
                GuardKind.CHECK_FOLDER -> {
                    Button(onClick = onChooseFolder, modifier = full) { Text(str(R.string.guard_choose_other)) }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.ACCEPT_FOLDER) }, modifier = full) {
                        Text(str(R.string.guard_accept))
                    }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.RESET_AS_NEW) }, modifier = full) {
                        Text(str(R.string.guard_as_new))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActionCard(title: String, text: String, button: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text)
            Button(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
internal fun ProfileCard(p: Profile, ssid: String?, awaitingFirstStart: Boolean, onClick: () -> Unit) {
    // 0.11.1 (DEC-072): bei Fotos-Profilen Anzahl gesicherter und noch offener Dateien (rein lokal, ohne Netzwerk).
    // Ordner-Profile (0.11.2): "offen" wäre nur mit PC-Listing bekannt -> stattdessen "synchronisiert" (Stand letzter
    // Abgleich, `mirror_state`) und die lokal bekannten offenen Konflikte.
    val ctx = LocalContext.current
    val isPhotos = p.type() != ProfileType.MIRROR
    val synced: Int? by remember(p.id) {
        val dao = AppDatabase.get(ctx).dao()
        if (isPhotos) dao.syncedCount(p.id) else dao.mirrorStateCountFlow(p.id)
    }.collectAsState<Int, Int?>(initial = null)
    val openConflicts by remember(p.id) { AppDatabase.get(ctx).dao().openConflictCountFlow(p.id) }.collectAsState(initial = 0)
    var open by remember(p.id) { mutableStateOf<Int?>(null) }
    // SYNC-20261111-199: auch bei Ordner-Profilen die lokal wartenden Dateien (neu/geändert seit dem letzten Abgleich)
    // SYNC-20261111-218 B3: auch beim Zurückkehren in die App neu zählen (Dateien ändern sich ausserhalb der App)
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) resumeTick++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(p, synced, resumeTick) {
        delay(800) // während eines Laufs ändert sich der Zähler laufend: nicht bei jeder Datei neu scannen
        open = try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (isPhotos) com.sync2pc.app.sync.PendingCheck.readyFiles(ctx, p, 0)
                else com.sync2pc.app.sync.PendingCheck.mirrorLocalOpen(ctx, p)
            }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e }
        catch (e: Exception) { null } // z. B. Medienzugriff fehlt -> Zeile ausblenden
    }
    // SYNC-20261111-225: tatsächlicher Zustand der letzten Anmeldung, nicht die Einstellung (siehe EncryptionStatus)
    val encTick by com.sync2pc.app.smb.EncryptionStatus.changes.collectAsState()
    val encrypted = remember(p, encTick, resumeTick) { com.sync2pc.app.smb.EncryptionStatus.lastKnown(p) == true }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name.trim(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                // Zustand als neutrales Etikett (nicht in Akzentfarbe, sonst wirkt es wie ein Link)
                if (!p.enabled) Text(str(R.string.home_profile_disabled), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge)
                else if (ssid != null && p.ssids().contains(ssid)) Text(str(R.string.home_profile_on_wifi),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                // SYNC-20261111-225: Icon «Verschlüsselt» (22 dp fest, optisch 12 dp vom Rand) als letztes Element der Kopfzeile:
                // Der Text endet davor (weight(1f)), so ragt er nie über das Icon.
                if (encrypted) {
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_encrypted),
                        contentDescription = str(R.string.icon_encrypted),
                        modifier = Modifier.size(22.dp).offset(x = 4.dp, y = (-4).dp).align(Alignment.Top),
                    )
                }
            }
            val target = p.smbPath.ifBlank { str(R.string.home_profile_no_target) } +
                (if (p.remoteSubfolder.isNotBlank()) "\\" + p.remoteSubfolder.trim('\\', '/') else "")
            if (p.type() == ProfileType.MIRROR) {
                val localLabel = p.localTreeUri.takeIf { it.isNotBlank() }
                    ?.let { com.sync2pc.app.mirror.LocalTree.label(ctx, android.net.Uri.parse(it)) } ?: str(R.string.home_profile_no_local)
                // Handy- und PC-Ordner je auf einer eigenen Zeile (0.9.0), sonst bricht der Pfad mitten im "\\" um
                Text("📁 $localLabel")
                // Pfeil für den Screenreader als Wort (0.11.0): "Beidseitig: \\PC\Freigabe"
                // SYNC-20261111-222 B2b: Icon (NBSP) und erster Pfadteil bleiben zusammen, Umbruch nur nach "\" (ZWSP)
                Text("${p.dir().short}\u00A0\u00A0${pathBreakAfterBackslash(uncNoBreak(target))}", modifier = Modifier.semantics { contentDescription = "${p.dir().label}: $target" })
                Hint(p.dir().label + if (p.propagateDeletes) " · " + str(R.string.home_profile_propagate) else "")
            } else {
                Text("📷 ${uncNoBreak(target)}")
                Hint(str(R.string.home_profile_sources, p.folders().joinToString(", ").ifBlank { str(R.string.home_profile_none) }))
            }
            // R5: Fehlender Handy-Ordner-Zugriff (z. B. nach Import) sofort auf der Karte sichtbar, tippen öffnet das Profil
            if (p.type() == ProfileType.MIRROR && p.localTreeUri.isNotBlank() &&
                !com.sync2pc.app.mirror.LocalTree.hasPermission(ctx, android.net.Uri.parse(p.localTreeUri)))
                Text(str(R.string.home_profile_access_lost), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            val pcMode = p.pcMode()
            if (com.sync2pc.app.sync.NetworkRules.needsWifiName(pcMode))
                Hint(str(R.string.home_wifi, p.ssids().joinToString(", ").ifBlank { str(R.string.home_profile_no_wifi) }))
            if (pcMode.checksGuid) {
                val known = com.sync2pc.app.sync.PcIdentity.storedFor(
                    p.pcGuid, p.pcGuidHost, com.sync2pc.app.smb.SmbTarget.hostOf(p.smbPath).orEmpty()) != null
                Hint(str(if (known) R.string.home_profile_pc_known else R.string.home_profile_pc_unknown))
            }
            val sc = synced; val oc = open
            if (isPhotos && sc != null && oc != null) Hint(str(R.string.home_profile_counts, sc, oc))
            if (!isPhotos && sc != null) Hint(
                plural(R.plurals.home_profile_mirror_synced, sc) +
                    (if (oc != null && oc > 0) " · " + str(R.string.home_profile_open_files, oc) else "") +
                    if (openConflicts > 0) " · " + plural(R.plurals.home_profile_open_conflicts, openConflicts) else ""
            )
            // P9: aktiver Filter ist sonst nicht erkennbar (0 Dateien synchronisiert ohne Erklärung)
            if (p.includePatterns.isNotBlank() || p.excludePatterns.isNotBlank()) Hint(str(R.string.home_profile_filter_active))
            if (awaitingFirstStart && p.enabled) Text(
                str(R.string.home_profile_awaiting),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
internal fun PermissionCard(perms: PermStatus, needs: PermNeeds) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null); Spacer(Modifier.width(8.dp))
                Text(str(R.string.perm_incomplete), style = MaterialTheme.typography.titleMedium)
            }
            PermissionRows(perms, showDone = false, needs = needs)
        }
    }
}
