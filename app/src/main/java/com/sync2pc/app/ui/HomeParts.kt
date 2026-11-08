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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncRun
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    paused: Boolean,
    userPausedUntil: Long,
    allUpToDate: Boolean,
    needsYou: Boolean,
    onSync: () -> Unit,
    onCancel: () -> Unit,
    onResume: () -> Unit,
    hasProfiles: Boolean,
    etaText: String? = null,
) {
    val userPaused = userPausedUntil > 0
    SectionCard(title = str(R.string.home_status)) {
        // Zustandsbild: laufender Hund = Sync läuft, stehender Hund = wartet auf dich, schlafender Hund =
        // alles gesichert, sonst sitzender Hund. Vorrang: läuft > wartet auf dich > pausiert > alles gesichert > wartet
        val upToDate = !isRunning && !paused && !userPaused && shareWait == null && allUpToDate
        val dog = when {
            isRunning -> DogState.RUNNING
            needsYou -> DogState.ATTENTION
            userPaused -> DogState.PAUSED
            upToDate -> DogState.SLEEPING
            else -> DogState.WAITING
        }
        DogStatus(dog, Modifier.align(Alignment.CenterHorizontally))
        // Feste Plätze für alle Zustände (0.9.3), damit WLAN-Zeile und Knopf nie verrutschen:
        // 1. Überschrift (nur "✓ Alles gesichert" sichtbar, sonst leere Zeile) + eine Zeile Abstand,
        // 2. WLAN (einzeilig), 3. Zustandsbereich mit Mindesthöhe für die üblichen Fälle
        // (2 Zeilen Haupttext + Balken bzw. Hinweis + 2 kleine Zeilen). Nur lange Fehlermeldungen machen ihn grösser.
        val typo = MaterialTheme.typography
        val density = LocalDensity.current
        val statusMin = with(density) {
            (typo.bodyLarge.lineHeight * 2).toDp() + (typo.bodySmall.lineHeight * 2).toDp() + 24.dp
        }
        // Screenreader (0.11.0): Zustandswechsel werden angesagt (Live-Bereich), auch wenn die Zeile sichtbar leer ist
        val stateLabel = when {
            isRunning -> str(R.string.home_running)
            upToDate -> str(R.string.home_all_saved)
            else -> ""
        }
        Text(if (upToDate) str(R.string.home_all_saved) else "", style = typo.titleSmall, minLines = 1, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.CenterHorizontally).semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = stateLabel
            })
        Spacer(Modifier.height(with(density) { typo.bodySmall.lineHeight.toDp() }))
        Text(str(R.string.home_wifi, ssid ?: str(R.string.home_wifi_none)), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Column(Modifier.fillMaxWidth().heightIn(min = statusMin), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            isRunning -> {
                // Feste Zeilenzahl während des Laufs (0.9.2), damit die Kachel nicht springt: Fortschritt bzw. Phase
                // immer 2 Zeilen, Dateiname immer genau 1 Zeile (auch wenn noch keiner bekannt ist)
                val withFiles = progress?.takeIf { it.totalFiles > 0 }
                val mainText = if (withFiles != null) str(R.string.home_progress, withFiles.profileName.trim(),
                    withFiles.doneFiles, withFiles.totalFiles, Notifications.formatBytes(withFiles.doneBytes),
                    Notifications.formatBytes(withFiles.totalBytes))
                    else progress?.phase ?: str(R.string.home_running)
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
            }
            userPaused && userPausedUntil == Long.MAX_VALUE -> {
                Text(str(R.string.home_manual_only), style = MaterialTheme.typography.titleSmall)
                Hint(str(R.string.home_manual_only_hint))
                lastRun?.let { Hint(str(R.string.home_last_sync, formatTime(it.finishedAt ?: it.startedAt), it.profileName.trim())) }
            }
            userPaused -> {
                Text(str(R.string.home_paused_until, pauseEndText(userPausedUntil)), style = MaterialTheme.typography.titleSmall)
                Hint(str(R.string.home_paused_until_hint))
            }
            paused -> {
                Text(str(R.string.home_paused_wifi), style = MaterialTheme.typography.titleSmall)
                Hint(str(R.string.home_paused_wifi_hint))
            }
            shareWait != null -> {
                Text(shareWait, color = MaterialTheme.colorScheme.error)
                Hint(str(R.string.home_pc_unreachable_hint))
            }
            isQueued -> Text(waitText)
            lastRun != null -> {
                Text(str(R.string.home_last_sync, formatTime(lastRun.finishedAt ?: lastRun.startedAt), lastRun.profileName.trim()))
                val nothing = lastRun.status == RunStatus.OK && lastRun.uploaded == 0 && lastRun.failed == 0
                Hint(if (nothing) str(R.string.home_nothing_to_do) else lastRun.message)
            }
            else -> Text(str(R.string.home_no_sync_yet))
        }
        // Zeitversetzt geplante Prüfung (z. B. Nachhol-Sync für gerade erst aufgenommene Fotos)
        if (!isRunning && !userPaused && !isQueued && nextPlanned != null)
            Hint(str(R.string.home_next_check, pauseEndText(nextPlanned)))
        if (!isRunning && !userPaused && shareWait == null && skipInfo != null) {
            Text(str(R.string.home_skipped, skipInfo), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        // Ohne Profil ist "Synchronisieren" ausgegraut – sagen, warum (im Zustandsbereich, damit nichts verrutscht)
        if (!hasProfiles) Hint(str(R.string.home_sync_needs_profile))
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

/** Eintrag in der Sektion "Tools" (0.5.0, seit 0.6.1 eine Zeile pro Tool, seit 0.6.3 ausgegraut möglich) */
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
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name.trim(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                // Zustand als neutrales Etikett (nicht in Akzentfarbe, sonst wirkt es wie ein Link)
                if (!p.enabled) Text(str(R.string.home_profile_disabled), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge)
                else if (ssid != null && p.ssids().contains(ssid)) Text(str(R.string.home_profile_on_wifi),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            }
            val target = p.smbPath.ifBlank { str(R.string.home_profile_no_target) } +
                (if (p.remoteSubfolder.isNotBlank()) "\\" + p.remoteSubfolder.trim('\\', '/') else "")
            if (p.type() == ProfileType.MIRROR) {
                val localLabel = p.localTreeUri.takeIf { it.isNotBlank() }
                    ?.let { com.sync2pc.app.mirror.LocalTree.label(android.net.Uri.parse(it)) } ?: str(R.string.home_profile_no_local)
                // Handy- und PC-Ordner je auf einer eigenen Zeile (0.9.0), sonst bricht der Pfad mitten im "\\" um
                Text("📁 $localLabel")
                // Pfeil für den Screenreader als Wort (0.11.0): "Beidseitig: \\PC\Freigabe"
                Text("${p.dir().short}  $target", modifier = Modifier.semantics { contentDescription = "${p.dir().label}: $target" })
                Hint(p.dir().label + if (p.propagateDeletes) " · " + str(R.string.home_profile_propagate) else "")
            } else {
                Text("📷 $target")
                Hint(str(R.string.home_profile_sources, p.folders().joinToString(", ").ifBlank { str(R.string.home_profile_none) }))
            }
            Hint(str(R.string.home_wifi, p.ssids().joinToString(", ").ifBlank { str(R.string.home_profile_no_wifi) }))
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
