package com.sync2pc.app.ui

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

/** Erhöht sich bei jedem onResume -> Berechtigungen/WLAN neu einlesen */
@Composable
fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return tick
}

@Composable
fun HomeScreen(navigate: (Screen) -> Unit, onConfirmDeletes: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val tick = rememberResumeTick()
    val perms = remember(tick) { PermStatus.read(ctx) }
    val ssid = remember(tick) { WifiHelper.currentSsid(ctx) }
    val update = remember(tick) { UpdateChecker.stored(ctx) }

    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())
    val decisions by remember { dao.decisionsFlow() }.collectAsState(initial = emptyList())
    val conflicts by remember { dao.openConflictsFlow() }.collectAsState(initial = emptyList())
    val deleteCount by remember { dao.queuedDeleteCount() }.collectAsState(initial = 0)
    val lastRun by remember { dao.lastRunFlow() }.collectAsState(initial = null)
    val pendingRestores by remember { dao.pendingRestoreCount() }.collectAsState(initial = 0)

    val wm = remember { WorkManager.getInstance(ctx) }
    val workFlow = remember {
        combine(SyncScheduler.ALL_SYNC_WORK.map { wm.getWorkInfosForUniqueWorkFlow(it) }) { arr -> arr.flatMap { it } }
    }
    val works by workFlow.collectAsState(initial = emptyList())
    val running = works.firstOrNull { it.state == WorkInfo.State.RUNNING }
    // Eingeplant heisst nicht immer "wartet": Nachhol-Sync und erneute PC-Prüfung laufen absichtlich später.
    // Fällig (Startzeit erreicht, aber Bedingungen fehlen) vs. zeitversetzt (Startzeit in der Zukunft)
    val now = System.currentTimeMillis()
    val enqueued = works.filter { it.state == WorkInfo.State.ENQUEUED }
    val queued = enqueued.any { it.nextScheduleTimeMillis <= now + 5_000 }
    val nextPlanned = enqueued.map { it.nextScheduleTimeMillis }.filter { it in (now + 5_001) until Long.MAX_VALUE }.minOrNull()
    // Warum wartet ein fälliger Auftrag? (WLAN fehlt / nicht am Ladegerät / startet gleich)
    val waitText = remember(tick, works, ssid, profiles) {
        val s = GlobalSettings(ctx)
        val onTarget = ssid != null && profiles.any { it.enabled && it.ssids().contains(ssid) }
        val charging = ctx.getSystemService(android.os.BatteryManager::class.java)?.isCharging == true
        when {
            !onTarget -> str(R.string.home_wait_wifi)
            s.onlyWhileCharging && !charging -> str(R.string.home_wait_charger)
            else -> str(R.string.home_starting)
        }
    }
    val progress = running?.let { SyncWorker.progressFrom(it.progress) }
    // Grund, falls der letzte automatische Start nicht synchronisiert hat (max. 24 h alt)
    val skipInfo = remember(tick, works) {
        val s = GlobalSettings(ctx)
        if (s.lastSkipInfo.isNotBlank() && System.currentTimeMillis() - s.lastSkipAt < 24 * 3600_000L)
            "${formatTime(s.lastSkipAt).substringAfter(' ')}: ${s.lastSkipInfo}"
        else null
    }
    // Durch WLAN-Unterbruch pausiert?
    val paused = remember(tick, works) { GlobalSettings(ctx).resumeOnWifi }
    // Vom Benutzer pausiert (Symbol oben rechts)? pauseVersion erzwingt das Neulesen nach Aktionen
    var pauseVersion by remember { mutableIntStateOf(0) }
    // Long.MAX_VALUE = "für immer" (nur manuelle Syncs)
    val userPausedUntil = remember(tick, works, pauseVersion) {
        val s = GlobalSettings(ctx)
        if (s.manualOnly) Long.MAX_VALUE else s.pausedUntil.takeIf { it > System.currentTimeMillis() } ?: 0L
    }
    LaunchedEffect(userPausedUntil) {
        // Pause läuft ab, während die Startseite offen ist -> Anzeige aktualisieren
        if (userPausedUntil in 1 until Long.MAX_VALUE) { delay(userPausedUntil - System.currentTimeMillis() + 500); pauseVersion++ }
    }
    var showPauseDialog by remember { mutableStateOf(false) }
    var showChargeWarning by remember { mutableStateOf(false) }
    var settingsMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val awaitingStart = remember(tick, works) { GlobalSettings(ctx).awaitingFirstStart() }
    // Schutzbremse (0.4.1): offene Entscheidungen der Ordner-Profile; guardVersion = nach einer Antwort neu lesen
    var guardVersion by remember { mutableIntStateOf(0) }
    val guards = remember(tick, works, guardVersion, profiles) {
        val ids = profiles.map { it.id }.toSet()
        GuardStore(ctx).all().filter { it.profileId in ids }
    }
    LaunchedEffect(guards.isEmpty()) { if (guards.isEmpty()) Notifications.cancelGuard(ctx) }
    // Papierkorb (0.6.0): zuletzt gemessene Grösse und vorgemerktes Leeren auf dem PC
    val trashSizeText = remember(tick, works, profiles) {
        val store = PurgeStore(ctx)
        val sizes = trashProfiles(profiles).map { store.sizes(it.id) }
        val local = sizes.map { it.first }.filter { it >= 0 }
        val remote = sizes.map { it.second }.filter { it >= 0 }
        str(R.string.home_tool_trash_text) +
            if (local.isEmpty() && remote.isEmpty()) ""
            else "\n" + str(R.string.home_tool_trash_used,
                if (local.isEmpty()) "?" else Notifications.formatBytes(local.sum()),
                if (remote.isEmpty()) "?" else Notifications.formatBytes(remote.sum()))
    }
    val purgeProfilesPending = remember(tick, works, profiles) {
        val ids = profiles.map { it.id }.toSet()
        PurgeStore(ctx).pendingProfiles().filter { it in ids }.toSet()
    }
    val pendingPurges = purgeProfilesPending.size
    // 0.7.0: Wiederherstellungen pro Profil, fehlgeschlagene (zurückgerollt) und Wartende hinter der Schutzbremse
    val restoreProfilesPending by remember { dao.pendingRestoreProfiles() }.collectAsState(initial = emptyList())
    var restoreFailVersion by remember { mutableIntStateOf(0) }
    val failedRestores = remember(tick, works, profiles, pendingRestores, restoreFailVersion) {
        RestoreFailures(ctx).restoreIds(profiles.map { it.id }).size
    }
    val guardedIds = guards.map { it.profileId }.toSet()
    /** Zusatz, wenn ein Teil der vorgemerkten Arbeit hinter der Schutzbremse wartet */
    fun guardNote(pendingIds: Collection<Long>): String? = when {
        pendingIds.isEmpty() || pendingIds.none { it in guardedIds } -> null
        pendingIds.all { it in guardedIds } -> str(R.string.home_guard_note_all)
        else -> str(R.string.home_guard_note_some)
    }
    // PC im Ziel-WLAN nicht erreichbar -> Hinweis mit Zeitpunkt der nächsten Prüfung
    val shareWait = remember(tick, works) {
        val s = GlobalSettings(ctx)
        if (s.shareRetryAt > System.currentTimeMillis())
            str(R.string.home_pc_unreachable, s.shareUnreachable, formatTime(s.shareRetryAt).substringAfter(' '))
        else null
    }

    ScreenScaffold(
        title = "LeoSync",
        onBack = null,
        actions = {
            // Symbol = Aktion (0.9.0): Automatik läuft -> ⏸ (tippen = pausieren), pausiert -> ▶ (tippen = fortsetzen)
            if (userPausedUntil > 0) IconButton(onClick = { SyncScheduler.resume(ctx); pauseVersion++ }) {
                Icon(Icons.Filled.PlayArrow, str(R.string.home_cd_paused))
            } else IconButton(onClick = { showPauseDialog = true }) {
                Icon(AppIcons.Pause, str(R.string.home_cd_running))
            }
            IconButton(onClick = { navigate(Screen.Stats) }) { Icon(AppIcons.BarChart, str(R.string.home_cd_stats)) }
            IconButton(onClick = { navigate(Screen.Log) }) { Icon(Icons.AutoMirrored.Filled.List, str(R.string.home_cd_log)) }
            // Zahnrad -> Menü mit den Rubriken der Einstellungen (je eine eigene Seite)
            Box {
                IconButton(onClick = { settingsMenu = true }) { Icon(Icons.Filled.Settings, str(R.string.home_cd_settings)) }
                DropdownMenu(expanded = settingsMenu, onDismissRequest = { settingsMenu = false }) {
                    SettingsSection.entries.forEach { sec ->
                        DropdownMenuItem(text = { Text(str(sec.title)) }, onClick = {
                            settingsMenu = false
                            // "Updates" öffnet direkt die Update-Seite (prüft sofort)
                            navigate(if (sec == SettingsSection.UPDATES) Screen.Update else Screen.Settings(sec))
                        })
                    }
                }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!perms.allOk) item { PermissionCard(perms) }

            item {
                StatusCard(
                    ssid = ssid,
                    isRunning = running != null,
                    isQueued = queued,
                    waitText = waitText,
                    nextPlanned = nextPlanned,
                    progress = progress,
                    lastRun = lastRun,
                    shareWait = shareWait,
                    skipInfo = skipInfo,
                    paused = paused,
                    userPausedUntil = userPausedUntil,
                    // "Alles aktuell": letzter Lauf ohne Probleme und nichts mehr offen
                    allUpToDate = lastRun?.status == "OK" && !queued && skipInfo == null &&
                        decisions.isEmpty() && conflicts.isEmpty() && deleteCount == 0 && guards.isEmpty(),
                    // LeoSync wartet auf eine Entscheidung von dir (Bild "Aufmerksamkeit")
                    needsYou = guards.isNotEmpty() || decisions.isNotEmpty() || conflicts.isNotEmpty() || deleteCount > 0,
                    onSync = {
                        // Erste Synchronisierung ohne Ladegerät -> vorher warnen
                        scope.launch {
                            if (FirstSync.warnNeeded(ctx, profiles)) showChargeWarning = true
                            else { SyncScheduler.enqueueManual(ctx); pauseVersion++ }
                        }
                    },
                    onCancel = { SyncScheduler.cancelAll(ctx) },
                    onResume = { SyncScheduler.resume(ctx); pauseVersion++ },
                    hasProfiles = profiles.isNotEmpty(),
                )
            }

            items(guards, key = { "guard-${it.profileId}" }) { g ->
                val profile = profiles.firstOrNull { it.id == g.profileId }
                GuardCard(
                    g, profile?.name ?: str(R.string.home_profile_fallback),
                    onChooseFolder = { navigate(Screen.ProfileEdit(g.profileId)) },
                    onChoice = { choice ->
                        scope.launch { MirrorGuard.resolve(ctx, g.profileId, choice); guardVersion++ }
                    },
                )
            }

            if (update != null) item {
                ActionCard(
                    title = str(R.string.home_update_title),
                    text = str(R.string.home_update_text, update.version, UpdateChecker.currentVersion()),
                    button = str(R.string.home_update_button),
                    onClick = { navigate(Screen.Update) },
                )
            }
            if (conflicts.isNotEmpty()) item {
                ActionCard(
                    title = str(R.string.home_conflicts_title),
                    text = plural(R.plurals.home_conflicts_text, conflicts.size),
                    button = str(R.string.home_conflicts_button),
                    onClick = { navigate(Screen.Conflicts) },
                )
            }
            if (decisions.isNotEmpty()) item {
                val files = decisions.sumOf { it.fileCount }
                ActionCard(
                    title = str(R.string.home_decisions_title),
                    text = plural(R.plurals.home_decisions_text, files, files, plural(R.plurals.home_groups, decisions.size)),
                    button = str(R.string.home_decisions_button),
                    onClick = { navigate(Screen.Decisions) },
                )
            }
            if (deleteCount > 0) item {
                ActionCard(
                    title = str(R.string.home_delete_title),
                    text = plural(R.plurals.home_delete_text, deleteCount),
                    button = str(R.string.home_delete_button),
                    onClick = onConfirmDeletes,
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(str(R.string.home_profiles), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { navigate(Screen.ProfileEdit(0)) }) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text(str(R.string.home_add_profile))
                    }
                }
            }
            if (profiles.isEmpty()) item {
                SectionCard {
                    Text(str(R.string.home_no_profile))
                    Button(onClick = { navigate(Screen.ProfileEdit(0)) }) { Text(str(R.string.home_first_profile)) }
                }
            }
            items(profiles, key = { it.id }) { p ->
                ProfileCard(p, ssid, awaitingFirstStart = p.id in awaitingStart) { navigate(Screen.ProfileEdit(p.id)) }
            }
            // Tools (0.6.3): immer sichtbar; ohne passendes Profil ausgegraut, Tippen zeigt einen Hinweis
            val canFree = freeProfiles(profiles).isNotEmpty()
            val hasMirror = trashProfiles(profiles).isNotEmpty()
            val tools = listOf(
                ToolTile("free", AppIcons.Smartphone, str(R.string.home_tool_free),
                    if (canFree) str(R.string.home_tool_free_text)
                    else str(R.string.home_tool_free_disabled),
                    enabled = canFree,
                    disabledHint = str(R.string.free_hint),
                ) { navigate(Screen.FreeSpace) },
                ToolTile("trash", Icons.Filled.Delete, str(R.string.home_tool_trash),
                    if (hasMirror) trashSizeText else str(R.string.home_tool_trash_disabled),
                    enabled = hasMirror,
                    disabledHint = str(R.string.home_tool_trash_hint),
                ) { navigate(Screen.Trash) },
            )
            run {
                item { Text(str(R.string.home_tools), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
                if (pendingPurges > 0) item {
                    val note = guardNote(purgeProfilesPending)
                    ActionCard(
                        title = str(R.string.home_purge_pending_title),
                        text = str(R.string.home_purge_pending_text) +
                            (note?.let { "\n$it" } ?: ""),
                        button = str(R.string.common_sync_now),
                        onClick = { SyncScheduler.enqueueManual(ctx); pauseVersion++ },
                    )
                }
                if (failedRestores > 0) item {
                    // 0.7.0: zurückgerollt, Vormerkung bleibt – erneut versuchen oder verwerfen
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(str(R.string.home_restore_failed_title), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(plural(R.plurals.home_restore_failed_text, failedRestores))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { SyncScheduler.enqueueManual(ctx); pauseVersion++ }) { Text(str(R.string.home_retry)) }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        RestoreManager.discardFailed(ctx, profiles.map { it.id })
                                        restoreFailVersion++
                                    }
                                }) { Text(str(R.string.home_discard)) }
                            }
                        }
                    }
                }
                if (pendingRestores > failedRestores) item {
                    val note = guardNote(restoreProfilesPending)
                    ActionCard(
                        title = str(R.string.home_restore_pending_title),
                        text = plural(R.plurals.home_restore_pending_text, pendingRestores - failedRestores) +
                            (note?.let { "\n$it" } ?: ""),
                        button = str(R.string.common_sync_now),
                        onClick = { SyncScheduler.enqueueManual(ctx); pauseVersion++ },
                    )
                }
                // Ein Tool pro Zeile (0.6.1)
                items(tools, key = { "tool-" + it.id }) { t -> ToolCard(t, Modifier.fillMaxWidth()) }
            }

            item {
                Text(
                    str(R.string.home_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (showChargeWarning) ChargeWarningDialog(
        onStart = { showChargeWarning = false; SyncScheduler.enqueueManual(ctx); pauseVersion++ },
        onDismiss = { showChargeWarning = false },
    )

    if (showPauseDialog) PauseDialog(
        onDismiss = { showPauseDialog = false },
        onPause = { hours ->
            SyncScheduler.pause(ctx, hours)
            showPauseDialog = false
            pauseVersion++
        },
        onForever = {
            SyncScheduler.pauseForever(ctx)
            showPauseDialog = false
            pauseVersion++
        },
    )
}

/** "14:30", "morgen 14:30" oder "05.11. 14:30" */
private fun pauseEndText(until: Long): String {
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
private fun PauseDialog(onDismiss: () -> Unit, onPause: (Int) -> Unit, onForever: () -> Unit) {
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
private const val PAUSE_FOREVER = -1

@Composable
private fun StatusCard(
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
        // Textbereich mit Mindesthöhe (0.9.0): so gross wie ein laufender Sync mit Fortschritt, damit die Kachel
        // beim Wechsel des Zustands nicht springt. Nur bei seltenen langen Meldungen wird sie grösser.
        val typo = MaterialTheme.typography
        val minText = with(LocalDensity.current) {
            (typo.bodyLarge.lineHeight * 3).toDp() + typo.bodySmall.lineHeight.toDp() + 28.dp
        }
        Column(Modifier.fillMaxWidth().heightIn(min = minText), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (upToDate) {
            Text(str(R.string.home_all_saved), style = typo.titleSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            // Eine Zeile Abstand, damit sich die Meldung abhebt
            Spacer(Modifier.height(with(LocalDensity.current) { typo.bodySmall.lineHeight.toDp() }))
        }
        Text(str(R.string.home_wifi, ssid ?: str(R.string.home_wifi_none)))
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
                    progress?.currentFile.orEmpty(), minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis,
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
                val nothing = lastRun.status == "OK" && lastRun.uploaded == 0 && lastRun.failed == 0
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
        }
        // Ohne Profil ist "Synchronisieren" ausgegraut – sagen, warum
        if (!hasProfiles) Hint(str(R.string.home_sync_needs_profile))
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
private class ToolTile(
    val id: String, val icon: ImageVector, val title: String, val text: String,
    val enabled: Boolean = true, val disabledHint: String? = null,
    val onClick: () -> Unit,
)

@Composable
private fun ToolCard(t: ToolTile, modifier: Modifier) {
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
private fun GuardCard(g: Guard, profileName: String, onChooseFolder: () -> Unit, onChoice: (MirrorGuard.Choice) -> Unit) {
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
private fun ActionCard(title: String, text: String, button: String, onClick: () -> Unit) {
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
private fun ProfileCard(p: Profile, ssid: String?, awaitingFirstStart: Boolean, onClick: () -> Unit) {
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
                Text("${p.dir().short}  $target")
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
private fun PermissionCard(perms: PermStatus) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null); Spacer(Modifier.width(8.dp))
                Text(str(R.string.perm_incomplete), style = MaterialTheme.typography.titleMedium)
            }
            PermissionRows(perms, showDone = false)
        }
    }
}
