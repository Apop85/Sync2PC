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
import androidx.compose.material3.TextButton
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
import com.sync2pc.app.R
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.l10n.str
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
    var progressText by remember { mutableStateOf("") }
    var offline by remember { mutableStateOf(false) }
    var cancelRequested by remember { mutableStateOf(false) }
    val canInstall = remember(tick) { UpdateInstaller.canInstall(ctx) }
    var autoUpdate by remember { mutableStateOf(s.autoUpdateCheck) }

    fun check() {
        busy = true; offline = false; status = str(R.string.update_checking)
        scope.launch {
            status = try {
                val r = withContext(Dispatchers.IO) { UpdateChecker.checkAndStore(ctx) }
                info = r
                if (r == null) str(R.string.update_latest, UpdateChecker.currentVersion()) else null
            } catch (e: Exception) {
                // Kein Netz: verständlicher Text statt Ausnahmetext (Rohtext klein darunter)
                if (e is java.net.UnknownHostException || e is java.net.ConnectException ||
                    e is java.net.SocketTimeoutException || e is java.net.NoRouteToHostException) {
                    offline = true
                    e.message ?: e.javaClass.simpleName
                } else e.message ?: e.javaClass.simpleName
            }
            busy = false
        }
    }

    fun install(u: UpdateInfo) {
        busy = true; progress = 0f; progressText = ""; cancelRequested = false
        status = str(R.string.update_downloading, u.version)
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    UpdateInstaller.download(ctx, u) { done, total ->
                        if (cancelRequested) throw kotlinx.coroutines.CancellationException("cancelled")
                        if (total > 0) {
                            progress = done.toFloat() / total
                            progressText = "${(done * 100 / total).toInt()} % · " +
                                Notifications.formatBytes(done) + " / " + Notifications.formatBytes(total)
                        } else progressText = Notifications.formatBytes(done)
                    }
                }
                status = str(R.string.update_installing)
                withContext(Dispatchers.IO) { UpdateInstaller.install(ctx, file, u.version) }
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(Notifications.ID_UPDATE)
            } catch (e: kotlinx.coroutines.CancellationException) {
                status = str(R.string.update_download_cancelled)
            } catch (e: Exception) {
                status = str(R.string.update_error, e.message ?: e.javaClass.simpleName)
            }
            progress = null; busy = false
        }
    }

    // Beim Öffnen sofort prüfen (kein zusätzlicher Knopf nötig)
    LaunchedEffect(Unit) { check() }

    ScreenScaffold(title = str(R.string.settings_updates), onBack = onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(str(R.string.update_installed)) {
                    Text(str(R.string.home_version, UpdateChecker.currentVersion()))
                    // Quelle ist seit 0.4.0 fest eingebaut (nicht mehr änderbar)
                    Hint(str(R.string.update_source, s.updateRepo))
                    if (s.lastUpdateCheck > 0) Hint(str(R.string.update_last_check, formatTime(s.lastUpdateCheck)))
                    if (busy && progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    // Die Seite prüft beim Öffnen; erneut prüfen ohne die Seite zu verlassen
                    if (!busy && !offline) TextButton(onClick = { check() }) { Text(str(R.string.update_check_now)) }
                    SwitchRow(str(R.string.update_daily), autoUpdate, subtitle = str(R.string.update_daily_sub)) {
                        autoUpdate = it; s.autoUpdateCheck = it; com.sync2pc.app.sync.SyncScheduler.setupAll(ctx)
                    }
                }
            }
            val u = info
            if (u != null) item {
                SectionCard(str(R.string.update_available, u.version)) {
                    if (u.size > 0) Hint(str(R.string.update_size, Notifications.formatBytes(u.size)))
                    val notes = remember(u.notes) { ReleaseNotes.clean(u.notes) }
                    if (notes.text.isNotBlank()) Text(notes.text, style = MaterialTheme.typography.bodySmall)
                    notes.link?.let { link ->
                        TextButton(onClick = {
                            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) } catch (ignored: Exception) {}
                        }) { Text(str(R.string.update_changes_link)) }
                    }
                    if (!canInstall) {
                        Hint(str(R.string.update_permission_hint))
                        OutlinedButton(onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                            )
                        }) { Text(str(R.string.update_permission)) }
                    }
                    Button(onClick = { install(u) }, enabled = !busy && canInstall) { Text(str(R.string.update_install)) }
                    Hint(str(R.string.update_install_hint))
                }
            }
            if (progress != null) item {
                SectionCard {
                    LinearProgressIndicator(progress = { progress ?: 0f }, modifier = Modifier.fillMaxWidth())
                    if (progressText.isNotBlank()) Hint(progressText)
                    TextButton(onClick = { cancelRequested = true }) { Text(str(R.string.common_cancel)) }
                }
            }
            if (offline) item {
                SectionCard {
                    Text(str(R.string.update_offline), color = MaterialTheme.colorScheme.error)
                    status?.let { Hint(it) }
                    TextButton(onClick = { check() }, enabled = !busy) { Text(str(R.string.update_check_again)) }
                }
            } else status?.let { item { Text(it) } }
        }
    }
}
