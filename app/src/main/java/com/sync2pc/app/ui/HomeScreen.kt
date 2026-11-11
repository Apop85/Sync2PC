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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
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
import com.sync2pc.app.sync.StartResult
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

/**
 * Erhöht sich, wenn das Standardnetz wechselt oder wegfällt (WLAN an/aus, anderes WLAN, mobile Daten), damit die Zeile
 * «WLAN: …» nicht veraltet stehen bleibt (SYNC-20261111-188 B5). Nur Verfügbarkeit, nicht jede Änderung der Signalstärke.
 */
@Composable
fun rememberNetworkTick(): Int {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(ctx) {
        val cm = ctx.getSystemService(android.net.ConnectivityManager::class.java)
        val cb = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { tick++ }
            override fun onLost(network: android.net.Network) { tick++ }
        }
        try { cm?.registerDefaultNetworkCallback(cb) } catch (e: Exception) { /* Anzeige bleibt wie bisher (nur beim Zurückkehren) */ }
        onDispose { try { cm?.unregisterNetworkCallback(cb) } catch (e: Exception) { /* nicht registriert */ } }
    }
    return tick
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigate: (Screen) -> Unit,
    onConfirmDeletes: () -> Unit,
    /** Snackbar der Startseite; von aussen übergeben, damit der Löschablauf (MainActivity) darin melden kann (R12) */
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val tick = rememberResumeTick()
    // B5: WLAN-Name auch bei Netzwechsel neu lesen (nicht nur beim Zurückkehren in die App)
    val netTick = rememberNetworkTick()
    val perms = remember(tick) { PermStatus.read(ctx) }
    val ssid = remember(tick, netTick) { WifiHelper.currentSsid(ctx) }
    val update = remember(tick) { UpdateChecker.stored(ctx) }

    val allProfiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())
    // 0.11.0: ein Profil, das gerade gelöscht wird (Rückgängig noch möglich), ist ausgeblendet
    val pendingDelete by PendingProfileDelete.pending.collectAsState()
    val profiles = allProfiles.filter { it.id != pendingDelete?.id }
    LaunchedEffect(pendingDelete) {
        val pd = pendingDelete ?: return@LaunchedEffect
        val r = snackbar.showSnackbar(
            str(R.string.home_profile_deleted, pd.name), actionLabel = str(R.string.home_undo),
            duration = SnackbarDuration.Long,
        )
        if (r == SnackbarResult.ActionPerformed) PendingProfileDelete.undo() else PendingProfileDelete.commit(ctx)
    }
    val decisions by remember { dao.decisionsFlow() }.collectAsState(initial = emptyList())
    // 0.11.1: Konflikte eines Profils in der Rückgängig-Frist nicht anzeigen (bleiben gespeichert, kommen bei „Rückgängig“ zurück)
    val allConflicts by remember { dao.openConflictsFlow() }.collectAsState(initial = emptyList())
    val conflicts = allConflicts.filter { it.profileId != pendingDelete?.id }
    val deleteCount by remember { dao.queuedDeleteCount() }.collectAsState(initial = 0)
    val lastRun by remember { dao.lastRunFlow() }.collectAsState(initial = null)
    // -173/-177: Dateien, die beim nächsten Sync folgen (jeweils letzter echter Lauf je aktivem Profil)
    val pendingFiles by remember { dao.pendingDeferredFlow() }.collectAsState(initial = 0)
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
    // Restzeit (0.11.0): Tempo seit Beginn dieses Laufs. Erst bei belastbarer Rate (etaSeconds, 063), sonst «wird berechnet»
    val runStart = remember(running?.id) { System.currentTimeMillis() to (progress?.doneBytes ?: 0L) }
    val etaText = progress?.let { pr ->
        if (pr.totalBytes <= pr.doneBytes) null
        else {
            val elapsed = System.currentTimeMillis() - runStart.first
            etaSeconds(elapsed, pr.doneBytes - runStart.second, pr.totalBytes - runStart.second)
                ?.let { formatEta(it) } ?: str(R.string.home_eta_calculating)
        }
    }
    // Grund, falls der letzte automatische Start nicht synchronisiert hat (max. 24 h alt)
    val skipInfo = remember(tick, workKey) {
        val s = settings
        if (s.lastSkipInfo.isNotBlank() && System.currentTimeMillis() - s.lastSkipAt < 24 * 3600_000L)
            "${formatTime(s.lastSkipAt).substringAfter(' ')}: ${s.lastSkipInfo}"
        else null
    }
    // B1: stammt der Grund von einem manuellen Start, heisst die Zeile nicht «Automatischer Start übersprungen»
    val skipManual = remember(tick, workKey) { settings.lastSkipManual }
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
    // 0.11.8: davon die, die nach 5 Fehlversuchen nicht mehr automatisch wiederholt werden
    val exhaustedRestores = remember(tick, workKey, profiles, pendingRestores, restoreFailVersion) {
        RestoreFailures(ctx).exhaustedCount(profiles.map { it.id })
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
        if (s.shareRetryAt > System.currentTimeMillis()) {
            // R7: Nennt die Zeile «Nächste Prüfung um …» (nextPlanned) den Zeitpunkt schon, steht er nicht doppelt im Fehlertext
            if (nextPlanned != null) str(R.string.home_pc_unreachable_short, s.shareUnreachable)
            else str(R.string.home_pc_unreachable, s.shareUnreachable, formatTime(s.shareRetryAt).substringAfter(' '))
        } else null
    }

    // Gesamtzustand für die Statuskarte (SYNC-20261110-113): Verbindung, Profile, offene Entscheidungen
    val activeProfiles = profiles.filter { it.enabled && it.isConfigured() }
    val onAllowedNet = remember(tick, netTick, workKey) { WifiHelper.onAllowedNetwork(ctx) }
    val offWifiActive = remember(tick, netTick, workKey) { WifiHelper.offWifiSyncActive(ctx) }
    val noConnection = HomeStatus.noConnection(
        activeProfiles.size,
        anyCanRunNow = activeProfiles.any { HomeStatus.profileCanRunNow(it.pcMode(), it.ssids(), ssid, onAllowedNet, offWifiActive) },
        pcUnreachable = shareWait != null,
    )
    // Offene Entscheidungen: Konflikte, Ordnerzuordnung, Schutzbremse, Löschbestätigung
    val attentionCount = conflicts.size + decisions.size + guards.size + (if (deleteCount > 0) 1 else 0)
    val lastRunErrors = lastRun?.takeIf { it.status == RunStatus.FAILED || it.failed > 0 }?.failed ?: -1
    val locationMissing = PermNeeds.of(profiles).wifi && !perms.location
    val headline = HomeStatus.headline(HomeStatusInput(
        isRunning = running != null,
        hasProfiles = profiles.isNotEmpty(),
        hasActiveProfile = activeProfiles.isNotEmpty(),
        attentionCount = attentionCount,
        noConnection = noConnection,
        userPaused = userPausedUntil > 0,
        wifiPaused = paused,
        lastRunHadErrors = lastRunErrors >= 0,
        // "Alles aktuell": letzter Lauf ohne Probleme und nichts mehr offen
        allUpToDate = lastRun?.status == RunStatus.OK && !queued && skipInfo == null &&
            decisions.isEmpty() && conflicts.isEmpty() && deleteCount == 0 && guards.isEmpty() &&
            pendingFiles == 0,
        pendingFiles = pendingFiles,
        // R4: ohne Standort-Berechtigung ist der WLAN-Name nicht lesbar (automatische Synchronisierung still gestört)
        locationMissing = locationMissing,
        // B2: ein aktives Profil, das noch nie erfolgreich gestartet wurde, ist nicht «gesichert»
        awaitingFirstStart = activeProfiles.any { it.id in awaitingStart },
        // B1: der letzte Start wurde nicht ausgeführt (PC nicht erreichbar/erkannt, WLAN) -> nicht «Bereit»
        recentSkip = skipInfo != null,
        queuedWork = queued,
    ))
    // -199: Wartende Dateien nur im Zustand «Keine Verbindung» ermitteln (lokal, ohne Netzwerk), nach Zustandswechseln neu
    val waitingFiles by produceState(0, headline == HomeHeadline.NO_CONNECTION, workKey, activeProfiles, pendingFiles) {
        value = if (headline != HomeHeadline.NO_CONNECTION) 0 else {
            delay(800)
            maxOf(pendingFiles, kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                activeProfiles.sumOf { com.sync2pc.app.sync.PendingCheck.openFiles(ctx, it) }
            })
        }
    }
    val attentionText = if (conflicts.isNotEmpty()) plural(R.plurals.home_status_conflict_detail, conflicts.size)
        else str(R.string.home_status_attention_detail)

    // -146 Teil A: Start während eines laufenden Syncs wird nachgeholt; beim manuellen Start sofort sagen, dass er vorgemerkt ist
    val deferred by remember { SyncScheduler.deferredState(ctx) }.collectAsState()
    val deferredSnack = str(R.string.home_sync_deferred_snack)
    fun enqueueManualWithFeedback() {
        if (SyncScheduler.enqueueManual(ctx) == StartResult.DEFERRED) scope.launch {
            snackbar.currentSnackbarData?.dismiss() // mehrfaches Tippen: dieselbe Meldung erneut, nichts stapeln
            snackbar.showSnackbar(deferredSnack)
        }
    }

    /** Synchronisieren (Knopf und Herunterziehen); erste Synchronisierung ohne Ladegerät -> vorher warnen */
    val startSync: () -> Unit = {
        // A2: ohne Verbindung kurz sagen, warum es evtl. nicht klappt (der manuelle Lauf prüft den PC trotzdem selbst)
        if (noConnection) scope.launch {
            snackbar.showSnackbar(str(if (shareWait != null) R.string.home_sync_feedback_pc else R.string.home_sync_feedback_wifi))
        }
        scope.launch {
            if (FirstSync.warnNeeded(ctx, profiles)) showChargeWarning = true
            else { enqueueManualWithFeedback(); pauseVersion++ }
        }
    }
    // Herunterziehen (0.10.0): Kreis kurz zeigen, danach übernimmt die Statuskachel
    var pullRefreshing by remember { mutableStateOf(false) }
    // Einmaliger Tipp (0.11.0); weg nach dem ersten Herunterziehen oder nach Antippen
    var pullTipDone by remember { mutableStateOf(settings.pullTipDone) }
    fun finishPullTip() { if (!pullTipDone) { settings.pullTipDone = true; pullTipDone = true } }
    LaunchedEffect(pullRefreshing, running != null) {
        if (pullRefreshing) { if (running == null) delay(1_500); pullRefreshing = false }
    }

    ScreenScaffold(
        title = "LeoSync",
        onBack = null,
        snackbarHost = { LeoSnackbarHost(snackbar) },
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
                    // 0.11.0: Hilfe & Produktseite im Browser
                    DropdownMenuItem(text = { Text(str(R.string.settings_help)) }, onClick = {
                        settingsMenu = false
                        try {
                            ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(HELP_URL)))
                        } catch (ignored: Exception) {}
                    })
                }
            }
        },
    ) { pad ->
        // Ganz oben weiter nach unten ziehen = Synchronisieren (0.10.0), wie "Neu laden" im Browser.
        // Ohne Profil oder während einer laufenden Synchronisierung passiert nichts.
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = { if (profiles.isNotEmpty() && running == null) { pullRefreshing = true; finishPullTip(); startSync() } },
            modifier = Modifier.fillMaxSize().padding(pad),
        ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 0.11.0: nur fragen, was vorhandene Profile brauchen (ohne Profil gar nichts)
            val needs = PermNeeds.of(profiles)
            if (perms.missing(needs)) item { PermissionCard(perms, needs) }

            // A8: Karten, die eine Entscheidung verlangen, stehen über der Statuskarte (bei Systemschrift 2,0 ohne Scrollen erreichbar)
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
                    skipManual = skipManual,
                    paused = paused,
                    userPausedUntil = userPausedUntil,
                    headline = headline,
                    attentionCount = attentionCount,
                    attentionText = attentionText,
                    lastRunErrors = lastRunErrors.coerceAtLeast(0),
                    pendingFiles = pendingFiles,
                    waitingFiles = waitingFiles,
                    locationMissing = locationMissing,
                    onOpenPermissions = { navigate(Screen.Settings(SettingsSection.PERMISSIONS)) },
                    onSync = startSync,
                    onCancel = { SyncScheduler.cancelAll(ctx) },
                    onResume = { SyncScheduler.resume(ctx); pauseVersion++ },
                    onOpenLog = { navigate(Screen.Log) },
                    hasProfiles = profiles.isNotEmpty(),
                    etaText = etaText,
                    runHint = if (running != null && deferred.requested)
                        str(if (deferred.manual) R.string.home_next_all else R.string.home_next_auto) else null,
                )
            }
            if (!pullTipDone && profiles.isNotEmpty()) item {
                Text(
                    str(R.string.home_pull_tip), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().clickable { finishPullTip() }.padding(horizontal = 8.dp),
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
                        onClick = { enqueueManualWithFeedback(); pauseVersion++ },
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
                            if (failedRestores > exhaustedRestores)
                                Text(plural(R.plurals.home_restore_failed_text, failedRestores - exhaustedRestores))
                            if (exhaustedRestores > 0)
                                Text(plural(R.plurals.home_restore_gaveup_text, exhaustedRestores))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    RestoreManager.retryFailed(ctx, profiles.map { it.id })
                                    restoreFailVersion++; pauseVersion++
                                }) { Text(str(R.string.home_retry)) }
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
                        onClick = { enqueueManualWithFeedback(); pauseVersion++ },
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
    }

    if (showChargeWarning) ChargeWarningDialog(
        onStart = { showChargeWarning = false; enqueueManualWithFeedback(); pauseVersion++ },
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

