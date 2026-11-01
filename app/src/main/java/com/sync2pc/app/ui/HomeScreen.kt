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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
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

    val wm = remember { WorkManager.getInstance(ctx) }
    val workFlow = remember {
        combine(SyncScheduler.ALL_SYNC_WORK.map { wm.getWorkInfosForUniqueWorkFlow(it) }) { arr -> arr.flatMap { it } }
    }
    val works by workFlow.collectAsState(initial = emptyList())
    val running = works.firstOrNull { it.state == WorkInfo.State.RUNNING }
    val queued = works.any { it.state == WorkInfo.State.ENQUEUED }
    val progress = running?.let { SyncWorker.progressFrom(it.progress) }
    // Grund, falls der letzte automatische Start nicht synchronisiert hat (max. 24 h alt)
    val skipInfo = remember(tick, works) {
        val s = GlobalSettings(ctx)
        if (s.lastSkipInfo.isNotBlank() && System.currentTimeMillis() - s.lastSkipAt < 24 * 3600_000L)
            "${formatTime(s.lastSkipAt).substringAfter(' ')}: ${s.lastSkipInfo}"
        else null
    }
    // PC im Ziel-WLAN nicht erreichbar -> Hinweis mit Zeitpunkt der nächsten Prüfung
    val shareWait = remember(tick, works) {
        val s = GlobalSettings(ctx)
        if (s.shareRetryAt > System.currentTimeMillis())
            "PC nicht erreichbar (${s.shareUnreachable}) – nächste Prüfung um ${formatTime(s.shareRetryAt).substringAfter(' ')}"
        else null
    }

    ScreenScaffold(
        title = "Sync2PC",
        onBack = null,
        actions = {
            IconButton(onClick = { navigate(Screen.Stats) }) { Icon(Icons.Filled.DateRange, "Statistik") }
            IconButton(onClick = { navigate(Screen.Log) }) { Icon(Icons.AutoMirrored.Filled.List, "Verlauf") }
            IconButton(onClick = { navigate(Screen.Settings) }) { Icon(Icons.Filled.Settings, "Einstellungen") }
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
                    progress = progress,
                    lastRun = lastRun,
                    shareWait = shareWait,
                    skipInfo = skipInfo,
                    onSync = { SyncScheduler.enqueueManual(ctx) },
                    onCancel = { SyncScheduler.cancelAll(ctx) },
                    onPreview = { navigate(Screen.Preview) },
                    hasProfiles = profiles.isNotEmpty(),
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
            items(profiles, key = { it.id }) { p -> ProfileCard(p, ssid) { navigate(Screen.ProfileEdit(p.id)) } }

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
}

@Composable
private fun StatusCard(
    ssid: String?,
    isRunning: Boolean,
    isQueued: Boolean,
    progress: SyncProgress?,
    lastRun: SyncRun?,
    shareWait: String?,
    skipInfo: String?,
    onSync: () -> Unit,
    onCancel: () -> Unit,
    onPreview: () -> Unit,
    hasProfiles: Boolean,
) {
    SectionCard(title = "Status") {
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
            shareWait != null -> {
                Text(shareWait, color = MaterialTheme.colorScheme.error)
                Hint("Ist der PC eingeschaltet und die Freigabe aktiv? „Jetzt synchronisieren“ prüft sofort.")
            }
            isQueued -> Text("Sync eingeplant – wartet auf passendes WLAN/Bedingungen.")
            lastRun != null -> {
                Text("Letzter Sync: ${formatTime(lastRun.finishedAt ?: lastRun.startedAt)} (${lastRun.profileName})")
                Hint(lastRun.message)
            }
            else -> Text("Noch kein Sync durchgeführt.")
        }
        if (!isRunning && shareWait == null && skipInfo != null) {
            Text("⚠ Automatischer Start übersprungen – $skipInfo", color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isRunning) OutlinedButton(onClick = onCancel) { Text("Abbrechen") }
            else Button(onClick = onSync, enabled = hasProfiles) { Text("Jetzt synchronisieren") }
            OutlinedButton(onClick = onPreview, enabled = hasProfiles) { Text("Vorschau") }
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
private fun ProfileCard(p: Profile, ssid: String?, onClick: () -> Unit) {
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
