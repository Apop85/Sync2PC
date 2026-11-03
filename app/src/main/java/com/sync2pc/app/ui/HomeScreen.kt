package com.sync2pc.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sync2pc.app.BuildConfig
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
            !onTarget -> "Wartet auf das Heim-WLAN."
            s.onlyWhileCharging && !charging -> "Wartet auf das Ladegerät („Nur beim Laden“ ist eingeschaltet)."
            else -> "Sync startet gleich…"
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
        "Dateien zurückholen oder endgültig löschen" +
            if (local.isEmpty() && remote.isEmpty()) ""
            else "\nBelegt: Handy " + (if (local.isEmpty()) "?" else Notifications.formatBytes(local.sum())) +
                " · PC " + (if (remote.isEmpty()) "?" else Notifications.formatBytes(remote.sum()))
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
        pendingIds.all { it in guardedIds } -> "Wartet auf deine Entscheidung (Schutzbremse) – erst danach geht es weiter."
        else -> "Bei einem Profil wartet es auf deine Entscheidung (Schutzbremse)."
    }
    // PC im Ziel-WLAN nicht erreichbar -> Hinweis mit Zeitpunkt der nächsten Prüfung
    val shareWait = remember(tick, works) {
        val s = GlobalSettings(ctx)
        if (s.shareRetryAt > System.currentTimeMillis())
            "PC nicht erreichbar (${s.shareUnreachable}) – nächste Prüfung um ${formatTime(s.shareRetryAt).substringAfter(' ')}"
        else null
    }

    ScreenScaffold(
        title = "LeoSync",
        onBack = null,
        actions = {
            // Zustand des Dienstes: ▶ läuft (tippen = pausieren), ⏸ pausiert (tippen = fortsetzen)
            if (userPausedUntil > 0) IconButton(onClick = { SyncScheduler.resume(ctx); pauseVersion++ }) {
                Icon(AppIcons.Pause, "Pausiert – tippen zum Fortsetzen")
            } else IconButton(onClick = { showPauseDialog = true }) {
                Icon(Icons.Filled.PlayArrow, "Läuft – tippen zum Pausieren")
            }
            IconButton(onClick = { navigate(Screen.Stats) }) { Icon(Icons.Filled.DateRange, "Statistik") }
            IconButton(onClick = { navigate(Screen.Log) }) { Icon(Icons.AutoMirrored.Filled.List, "Verlauf") }
            // Zahnrad -> Menü mit den Rubriken der Einstellungen (je eine eigene Seite)
            Box {
                IconButton(onClick = { settingsMenu = true }) { Icon(Icons.Filled.Settings, "Einstellungen") }
                DropdownMenu(expanded = settingsMenu, onDismissRequest = { settingsMenu = false }) {
                    SettingsSection.entries.forEach { sec ->
                        DropdownMenuItem(text = { Text(sec.title) }, onClick = {
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
                    g, profile?.name ?: "Profil",
                    onChooseFolder = { navigate(Screen.ProfileEdit(g.profileId)) },
                    onChoice = { choice ->
                        scope.launch { MirrorGuard.resolve(ctx, g.profileId, choice); guardVersion++ }
                    },
                )
            }

            if (update != null) item {
                ActionCard(
                    title = "Update verfügbar",
                    text = "Version ${update.version} ist verfügbar (installiert: ${UpdateChecker.currentVersion()}).",
                    button = "Ansehen",
                    onClick = { navigate(Screen.Update) },
                )
            }
            if (conflicts.isNotEmpty()) item {
                ActionCard(
                    title = "Konflikte beim Ordner-Sync",
                    text = "${conflicts.size} Datei(en) wurden auf Handy und PC geändert. Wähle, welche Version gilt.",
                    button = "Entscheiden",
                    onClick = { navigate(Screen.Conflicts) },
                )
            }
            if (decisions.isNotEmpty()) item {
                val files = decisions.sumOf { it.fileCount }
                ActionCard(
                    title = "Ordnerauswahl nötig",
                    text = "$files Dateien in ${decisions.size} Gruppe(n) warten, weil es umbenannte Monatsordner gibt.",
                    button = "Zuordnen",
                    onClick = { navigate(Screen.Decisions) },
                )
            }
            if (deleteCount > 0) item {
                ActionCard(
                    title = "Gesicherte Dateien entfernen",
                    text = "$deleteCount Dateien sind verifiziert auf dem PC. Android fragt einmal zur Bestätigung.",
                    button = "Entfernen bestätigen",
                    onClick = onConfirmDeletes,
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Profile", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { navigate(Screen.ProfileEdit(0)) }) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Profil hinzufügen")
                    }
                }
            }
            if (profiles.isEmpty()) item {
                SectionCard {
                    Text("Noch kein Profil. Lege ein Profil mit Ziel-Freigabe, WLAN und Quellordnern an.")
                    Button(onClick = { navigate(Screen.ProfileEdit(0)) }) { Text("Erstes Profil anlegen") }
                }
            }
            items(profiles, key = { it.id }) { p ->
                ProfileCard(p, ssid, awaitingFirstStart = p.id in awaitingStart) { navigate(Screen.ProfileEdit(p.id)) }
            }
            // Tools (0.6.3): immer sichtbar; ohne passendes Profil ausgegraut, Tippen zeigt einen Hinweis
            val canFree = freeProfiles(profiles).isNotEmpty()
            val hasMirror = trashProfiles(profiles).isNotEmpty()
            val tools = listOf(
                ToolTile(AppIcons.Smartphone, "Speicher freigeben",
                    if (canFree) "Alte, auf dem PC gesicherte Dateien vom Handy entfernen"
                    else "Nur bei Fotos-Profilen und Ordner-Profilen Handy → PC",
                    enabled = canFree,
                    disabledHint = FREE_HINT,
                ) { navigate(Screen.FreeSpace) },
                ToolTile(Icons.Filled.Delete, "Papierkorb",
                    if (hasMirror) trashSizeText else "Nur bei Ordner-Profilen",
                    enabled = hasMirror,
                    disabledHint = "Den Papierkorb gibt es nur bei Ordner-Profilen.",
                ) { navigate(Screen.Trash) },
            )
            run {
                item { Text("Tools", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
                if (pendingPurges > 0) item {
                    val note = guardNote(purgeProfilesPending)
                    ActionCard(
                        title = "Papierkorb leeren vorgemerkt",
                        text = "Der PC-Papierkorb wird beim nächsten Sync mit erreichbarem PC geleert." +
                            (note?.let { "\n$it" } ?: ""),
                        button = "Jetzt synchronisieren",
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
                            Text("Wiederherstellen fehlgeschlagen", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text("$failedRestores Datei(en) konnten nicht wiederhergestellt werden. Es wurde alles " +
                                "zurückgerollt; der nächste Sync versucht es erneut. Details stehen im Verlauf.")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { SyncScheduler.enqueueManual(ctx); pauseVersion++ }) { Text("Erneut versuchen") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        RestoreManager.discardFailed(ctx, profiles.map { it.id })
                                        restoreFailVersion++
                                    }
                                }) { Text("Verwerfen") }
                            }
                        }
                    }
                }
                if (pendingRestores > failedRestores) item {
                    val note = guardNote(restoreProfilesPending)
                    ActionCard(
                        title = "Wiederherstellung vorgemerkt",
                        text = "${pendingRestores - failedRestores} Datei(en) werden beim nächsten Sync mit erreichbarem PC " +
                            "wiederhergestellt." + (note?.let { "\n$it" } ?: ""),
                        button = "Jetzt synchronisieren",
                        onClick = { SyncScheduler.enqueueManual(ctx); pauseVersion++ },
                    )
                }
                // Ein Tool pro Zeile (0.6.1)
                items(tools, key = { "tool-" + it.title }) { t -> ToolCard(t, Modifier.fillMaxWidth()) }
            }

            item {
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
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
    val day = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.GERMANY)
    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).format(java.util.Date(until))
    val now = System.currentTimeMillis()
    return when (day.format(java.util.Date(until))) {
        day.format(java.util.Date(now)) -> time
        day.format(java.util.Date(now + 24 * 3_600_000L)) -> "morgen $time"
        else -> java.text.SimpleDateFormat("dd.MM.", java.util.Locale.GERMANY).format(java.util.Date(until)) + " $time"
    }
}

/** Wie lange pausieren? Schnellwahl 1/4/24 Stunden, eigene Anzahl (1–168) oder "für immer" (nur manuell) */
@Composable
private fun PauseDialog(onDismiss: () -> Unit, onPause: (Int) -> Unit, onForever: () -> Unit) {
    var hoursText by remember { mutableStateOf("") }
    val hours = hoursText.toIntOrNull()?.takeIf { it in 1..168 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wie lange pausieren?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Hint("Ein laufender Sync wird angehalten. Bis zum Ende der Pause startet nichts automatisch.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 4, 24).forEach { h -> OutlinedButton(onClick = { onPause(h) }) { Text("$h h") } }
                }
                OutlinedTextField(
                    hoursText, { v -> hoursText = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Eigene Anzahl Stunden (1–168)") },
                    isError = hoursText.isNotEmpty() && hours == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = onForever, modifier = Modifier.fillMaxWidth()) { Text("Für immer (nur manuell)") }
                Hint("Schaltet die Automatik ganz ab: Synchronisiert wird nur noch mit „Synchronisieren“, bis du über ⏸ wieder einschaltest.")
            }
        },
        confirmButton = { TextButton(enabled = hours != null, onClick = { hours?.let(onPause) }) { Text("Pausieren") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

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
    SectionCard(title = "Status") {
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
        if (upToDate) Text("✓ Alles gesichert und aktuell", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("WLAN: " + (ssid ?: "nicht verbunden / nicht lesbar"))
        when {
            isRunning -> {
                if (progress != null && progress.totalFiles > 0) {
                    val f = if (progress.totalBytes > 0) progress.doneBytes.toFloat() / progress.totalBytes else 0f
                    Text("${progress.profileName}: ${progress.doneFiles} / ${progress.totalFiles} Dateien – " +
                        "${Notifications.formatBytes(progress.doneBytes)} von ${Notifications.formatBytes(progress.totalBytes)}")
                    LinearProgressIndicator(progress = { f.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    progress.currentFile?.let { Hint(it) }
                } else {
                    Text(progress?.phase ?: "Synchronisierung läuft…")
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            userPaused && userPausedUntil == Long.MAX_VALUE -> {
                Text("⏸ Automatik aus – nur manuelle Syncs", style = MaterialTheme.typography.titleSmall)
                Hint("Es wird nur synchronisiert, wenn du „Synchronisieren“ tippst. ⏸ oben rechts schaltet die Automatik wieder ein.")
                lastRun?.let { Hint("Letzter Sync: ${formatTime(it.finishedAt ?: it.startedAt)} (${it.profileName})") }
            }
            userPaused -> {
                Text("⏸ Pausiert bis ${pauseEndText(userPausedUntil)}", style = MaterialTheme.typography.titleSmall)
                Hint("Automatische Syncs ruhen bis dahin. „Fortsetzen“ oder „Synchronisieren“ beendet die Pause sofort.")
            }
            paused -> {
                Text("⏸ Pausiert – wartet auf das Heim-WLAN", style = MaterialTheme.typography.titleSmall)
                Hint("Der Sync wurde durch einen WLAN-Unterbruch angehalten und läuft automatisch weiter, sobald das WLAN wieder verbunden ist.")
            }
            shareWait != null -> {
                Text(shareWait, color = MaterialTheme.colorScheme.error)
                Hint("Ist der PC eingeschaltet und die Freigabe aktiv? „Synchronisieren“ prüft sofort.")
            }
            isQueued -> Text(waitText)
            lastRun != null -> {
                Text("Letzter Sync: ${formatTime(lastRun.finishedAt ?: lastRun.startedAt)} (${lastRun.profileName})")
                val nothing = lastRun.status == "OK" && lastRun.uploaded == 0 && lastRun.failed == 0
                Hint(if (nothing) "Nichts zu tun – alles aktuell" else lastRun.message)
            }
            else -> Text("Noch kein Sync durchgeführt.")
        }
        // Zeitversetzt geplante Prüfung (z. B. Nachhol-Sync für gerade erst aufgenommene Fotos)
        if (!isRunning && !userPaused && !isQueued && nextPlanned != null)
            Hint("Nächste Prüfung um ${pauseEndText(nextPlanned)}")
        if (!isRunning && !userPaused && shareWait == null && skipInfo != null) {
            Text("⚠ Automatischer Start übersprungen – $skipInfo", color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        // Zwei gleich breite Knöpfe, Text einzeilig (kein Umbruch wie "Vorsc-hau")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val w = Modifier.weight(1f)
            // "Pausieren" hält nur den laufenden Sync an; beim nächsten automatischen Start geht es weiter
            if (isRunning) OutlinedButton(onClick = onCancel, modifier = w) { Text("Pausieren", maxLines = 1) }
            // Befristete Pause -> "Fortsetzen"; "für immer" -> weiterhin manuell synchronisieren können
            else if (userPaused && userPausedUntil != Long.MAX_VALUE) Button(onClick = onResume, modifier = w) { Text("Fortsetzen", maxLines = 1) }
            else Button(onClick = onSync, enabled = hasProfiles, modifier = w) { Text("Synchronisieren", maxLines = 1) }
            // "Simulieren" ist seit 0.3.6 pro Profil im Profil-Bildschirm
        }
    }
}

/** Eintrag in der Sektion "Tools" (0.5.0, seit 0.6.1 eine Zeile pro Tool, seit 0.6.3 ausgegraut möglich) */
private class ToolTile(
    val icon: ImageVector, val title: String, val text: String,
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
                    Button(onClick = onChooseFolder, modifier = full) { Text("Neuen Ordner wählen") }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.ACCEPT_FOLDER) }, modifier = full) {
                        Text("Erneut prüfen (Ordner wiederhergestellt)")
                    }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.RESET_AS_NEW) }, modifier = full) {
                        Text("Neu anlegen und abgleichen")
                    }
                    Hint("„Neu anlegen“ gleicht alles neu ab, ohne etwas zu löschen. Bis du entscheidest, ruht nur dieses Profil.")
                }
                GuardKind.MASS_DELETE -> {
                    Button(onClick = { onChoice(MirrorGuard.Choice.KEEP_FILES) }, modifier = full) { Text("Nicht löschen") }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.DELETE_ANYWAY) }, modifier = full) {
                        Text("Trotzdem in den Papierkorb")
                    }
                    Hint("„Nicht löschen“ behält die Dateien bzw. stellt sie auf der anderen Seite wieder her.")
                }
                GuardKind.CHECK_FOLDER -> {
                    Button(onClick = onChooseFolder, modifier = full) { Text("Anderen Ordner wählen") }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.ACCEPT_FOLDER) }, modifier = full) {
                        Text("Trotzdem übernehmen")
                    }
                    OutlinedButton(onClick = { onChoice(MirrorGuard.Choice.RESET_AS_NEW) }, modifier = full) {
                        Text("Als neuen Ordner behandeln")
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
                Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (!p.enabled) Text("deaktiviert", color = MaterialTheme.colorScheme.error)
                else if (ssid != null && p.ssids().contains(ssid)) Text("im WLAN ✓", color = MaterialTheme.colorScheme.primary)
            }
            val target = p.smbPath.ifBlank { "Kein Ziel festgelegt" } +
                (if (p.remoteSubfolder.isNotBlank()) "\\" + p.remoteSubfolder.trim('\\', '/') else "")
            if (p.type() == ProfileType.MIRROR) {
                val localLabel = p.localTreeUri.takeIf { it.isNotBlank() }
                    ?.let { com.sync2pc.app.mirror.LocalTree.label(android.net.Uri.parse(it)) } ?: "kein Handy-Ordner"
                Text("📁 $localLabel  ${p.dir().short}  $target")
                Hint(p.dir().label + if (p.propagateDeletes) " · Löschungen übernehmen" else "")
            } else {
                Text("📷 $target")
                Hint("Quellen: " + (p.folders().joinToString(", ").ifBlank { "keine" }))
            }
            Hint("WLAN: " + (p.ssids().joinToString(", ").ifBlank { "keines – nur manueller Sync" }))
            if (awaitingFirstStart && p.enabled) Text(
                "Wartet auf den ersten Start – zuerst im Profil simulieren oder „Synchronisieren“ tippen",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun PermissionCard(perms: PermStatus) {
    val ctx = LocalContext.current
    val multi = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null); Spacer(Modifier.width(8.dp))
                Text("Einrichtung unvollständig", style = MaterialTheme.typography.titleMedium)
            }
            if (!perms.media) PermRow("Zugriff auf Fotos & Videos", "Erteilen") {
                multi.launch(PermStatus.mediaPermissions)
            }
            if (perms.media && !perms.mediaLocation) PermRow("Standort in Fotos lesen (für Länderordner)", "Erteilen") {
                single.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
            if (!perms.notifications && Build.VERSION.SDK_INT >= 33) PermRow("Benachrichtigungen (Fortschritt)", "Erteilen") {
                single.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!perms.location) PermRow("Standort (Android braucht ihn, um den WLAN-Namen zu lesen)", "Erteilen") {
                multi.launch(PermStatus.locationPermissions)
            }
            if (perms.location && !perms.backgroundLocation) PermRow(
                "Standort „Immer zulassen“ (WLAN-Erkennung im Hintergrund)", "Öffnen"
            ) { single.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
            if (!perms.locationServiceOn) PermRow("Standortdienst ist ausgeschaltet", "Einschalten") {
                ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            if (!perms.batteryUnrestricted) PermRow("Akku-Optimierung deaktivieren (zuverlässiger Hintergrund-Sync)", "Öffnen") {
                ctx.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                )
            }
        }
    }
}

@Composable
private fun PermRow(text: String, button: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(button) }
    }
}
