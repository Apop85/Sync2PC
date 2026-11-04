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
    // 0.9.5: Während einer Synchronisierung kommen laufend Fortschritts-Updates. Einstellungen werden nur neu
    // gelesen, wenn sich der Zustand eines Auftrags ändert (workKey), nicht bei jedem Fortschritt.
    val workKey = remember(works) { works.map { Triple(it.id, it.state, it.runAttemptCount) } }
    val settings = remember { GlobalSettings(ctx) }
    val running = works.firstOrNull { it.state == WorkInfo.State.RUNNING }
    // Eingeplant heisst nicht immer "wartet": Nachhol-Sync und erneute PC-Prüfung laufen absichtlich später.
    // Fällig (Startzeit erreicht, aber Bedingungen fehlen) vs. zeitversetzt (Startzeit in der Zukunft)
    val now = System.currentTimeMillis()
    val enqueued = works.filter { it.state == WorkInfo.State.ENQUEUED }
    val queued = enqueued.any { it.nextScheduleTimeMillis <= now + 5_000 }
    val nextPlanned = enqueued.map { it.nextScheduleTimeMillis }.filter { it in (now + 5_001) until Long.MAX_VALUE }.minOrNull()
    // Warum wartet ein fälliger Auftrag? (WLAN fehlt / nicht am Ladegerät / startet gleich)
    val waitText = remember(tick, workKey, ssid, profiles) {
        val s = settings
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
    val skipInfo = remember(tick, workKey) {
        val s = settings
        if (s.lastSkipInfo.isNotBlank() && System.currentTimeMillis() - s.lastSkipAt < 24 * 3600_000L)
            "${formatTime(s.lastSkipAt).substringAfter(' ')}: ${s.lastSkipInfo}"
        else null
    }
    // Durch WLAN-Unterbruch pausiert?
    val paused = remember(tick, workKey) { settings.resumeOnWifi }
    // Vom Benutzer pausiert (Symbol oben rechts)? pauseVersion erzwingt das Neulesen nach Aktionen
    var pauseVersion by remember { mutableIntStateOf(0) }
    // Long.MAX_VALUE = "für immer" (nur manuelle Syncs)
    val userPausedUntil = remember(tick, workKey, pauseVersion) {
        val s = settings
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
    val awaitingStart = remember(tick, workKey) { settings.awaitingFirstStart() }
    // Schutzbremse (0.4.1): offene Entscheidungen der Ordner-Profile; guardVersion = nach einer Antwort neu lesen
    var guardVersion by remember { mutableIntStateOf(0) }
    val guards = remember(tick, workKey, guardVersion, profiles) {
        val ids = profiles.map { it.id }.toSet()
        GuardStore(ctx).all().filter { it.profileId in ids }
    }
    LaunchedEffect(guards.isEmpty()) { if (guards.isEmpty()) Notifications.cancelGuard(ctx) }
    // Papierkorb (0.6.0): zuletzt gemessene Grösse und vorgemerktes Leeren auf dem PC
    val trashSizeText = remember(tick, workKey, profiles) {
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
    val purgeProfilesPending = remember(tick, workKey, profiles) {
        val ids = profiles.map { it.id }.toSet()
        PurgeStore(ctx).pendingProfiles().filter { it in ids }.toSet()
    }
    val pendingPurges = purgeProfilesPending.size
    // 0.7.0: Wiederherstellungen pro Profil, fehlgeschlagene (zurückgerollt) und Wartende hinter der Schutzbremse
    val restoreProfilesPending by remember { dao.pendingRestoreProfiles() }.collectAsState(initial = emptyList())
    var restoreFailVersion by remember { mutableIntStateOf(0) }
    val failedRestores = remember(tick, workKey, profiles, pendingRestores, restoreFailVersion) {
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
    val shareWait = remember(tick, workKey) {
        val s = settings
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
                    allUpToDate = lastRun?.status == RunStatus.OK && !queued && skipInfo == null &&
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

