package com.sync2pc.app.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.sync.Notifications
import com.sync2pc.app.ui.Hint
import com.sync2pc.app.ui.ScreenScaffold
import com.sync2pc.app.ui.SectionCard
import com.sync2pc.app.ui.SwitchRow
import com.sync2pc.app.ui.formatTime
import com.sync2pc.app.ui.rememberResumeTick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Update-Seite: prüfen, herunterladen, installieren. */
@Composable
fun UpdateScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = remember { GlobalSettings(ctx) }
    val tick = rememberResumeTick()
    var info by remember { mutableStateOf(UpdateChecker.stored(ctx)) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) }
    val canInstall = remember(tick) { UpdateInstaller.canInstall(ctx) }
    var autoUpdate by remember { mutableStateOf(s.autoUpdateCheck) }

    fun check() {
        busy = true; status = "Suche nach Updates…"
        scope.launch {
            status = try {
                val r = withContext(Dispatchers.IO) { UpdateChecker.checkAndStore(ctx) }
                info = r
                if (r == null) "Du hast die neueste Version (${UpdateChecker.currentVersion()})." else null
            } catch (e: Exception) {
                e.message ?: e.javaClass.simpleName
            }
            busy = false
        }
    }

    fun install(u: UpdateInfo) {
        busy = true; progress = 0f; status = "Lade Version ${u.version}…"
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    UpdateInstaller.download(ctx, u) { done, total ->
                        if (total > 0) progress = done.toFloat() / total
                    }
                }
                status = "Installation wird gestartet…"
                withContext(Dispatchers.IO) { UpdateInstaller.install(ctx, file, u.version) }
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(Notifications.ID_UPDATE)
            } catch (e: Exception) {
                status = "Fehler: ${e.message ?: e.javaClass.simpleName}"
            }
            progress = null; busy = false
        }
    }

    // Beim Öffnen sofort prüfen (kein zusätzlicher Knopf nötig)
    LaunchedEffect(Unit) { check() }

    ScreenScaffold(title = "Updates", onBack = onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard("Installiert") {
                    Text("Version ${UpdateChecker.currentVersion()}")
                    // Quelle ist seit 0.4.0 fest eingebaut (nicht mehr änderbar)
                    Hint("Quelle: github.com/${s.updateRepo} (Releases)")
                    if (s.lastUpdateCheck > 0) Hint("Zuletzt geprüft: ${formatTime(s.lastUpdateCheck)}")
                    if (busy && progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    SwitchRow("Täglich nach Updates suchen", autoUpdate, subtitle = "Nur im WLAN, Meldung per Benachrichtigung") {
                        autoUpdate = it; s.autoUpdateCheck = it; com.sync2pc.app.sync.SyncScheduler.setupAll(ctx)
                    }
                }
            }
            val u = info
            if (u != null) item {
                SectionCard("Version ${u.version} verfügbar") {
                    if (u.size > 0) Hint("Grösse: ${Notifications.formatBytes(u.size)}")
                    if (u.notes.isNotBlank()) Text(u.notes.take(3000), style = MaterialTheme.typography.bodySmall)
                    if (!canInstall) {
                        Hint("Android muss LeoSync einmalig erlauben, Apps zu installieren.")
                        OutlinedButton(onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                            )
                        }) { Text("Erlaubnis erteilen") }
                    }
                    Button(onClick = { install(u) }, enabled = !busy && canInstall) { Text("Herunterladen und installieren") }
                    Hint("Android fragt zur Bestätigung. Nach dem Update startet LeoSync neu.")
                }
            }
            if (progress != null) item { LinearProgressIndicator(progress = { progress ?: 0f }, modifier = Modifier.fillMaxWidth()) }
            status?.let { item { Text(it) } }
        }
    }
}
