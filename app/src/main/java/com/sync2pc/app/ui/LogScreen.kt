package com.sync2pc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.LogItem
import com.sync2pc.app.data.RunGrouping
import com.sync2pc.app.data.SyncRun
import androidx.compose.material3.FilterChip
import com.sync2pc.app.diag.Diagnostics
import com.sync2pc.app.sync.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LogScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val scope = rememberCoroutineScope()
    // Alle Läufe der letzten 90 Tage (älter löscht pruneRuns), höchstens 2000
    val runs by remember { dao.runsFlow(2000) }.collectAsState(initial = emptyList())
    var open by remember { mutableStateOf<Long?>(null) }
    var openGroup by remember { mutableStateOf<Long?>(null) }
    val settings = remember { com.sync2pc.app.data.GlobalSettings(ctx) }
    var onlyErrors by remember { mutableStateOf(settings.logFilterErrors) }
    var onlyTransfers by remember { mutableStateOf(settings.logFilterTransfers) }
    val grouped = remember(runs) { RunGrouping.group(runs) }
    val shown = RunGrouping.filter(grouped, onlyErrors, onlyTransfers)

    ScreenScaffold(
        title = "Verlauf",
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                scope.launch {
                    try {
                        val text = Diagnostics.logExport(ctx)
                        Diagnostics.shareAsFile(ctx, text, "LeoSync-Verlauf_${Diagnostics.fileStamp()}.txt")
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(ctx, "Export fehlgeschlagen: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }) { Icon(Icons.Filled.Share, "Verlauf teilen") }
            IconButton(onClick = {
                scope.launch { withContext(Dispatchers.IO) { dao.clearLogs(); dao.clearRuns() } }
            }) { Icon(Icons.Filled.Delete, "Verlauf löschen") }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Unabhängig ein-/ausschaltbar; keiner aktiv = alle Läufe
                    FilterChip(selected = onlyErrors, onClick = { onlyErrors = !onlyErrors; settings.logFilterErrors = onlyErrors },
                        label = { Text("Fehler") })
                    FilterChip(selected = onlyTransfers, onClick = { onlyTransfers = !onlyTransfers; settings.logFilterTransfers = onlyTransfers },
                        label = { Text("Übertragungen") })
                }
            }
            if (shown.isEmpty()) item {
                Text(when {
                    runs.isEmpty() -> "Noch keine Einträge."
                    onlyErrors && onlyTransfers -> "Keine Läufe mit Fehlern oder Übertragungen."
                    onlyErrors -> "Keine Läufe mit Fehlern."
                    else -> "Keine Läufe mit Übertragungen."
                })
            }
            items(shown, key = { item ->
                when (item) {
                    is LogItem.Single -> "r" + item.run.id
                    is LogItem.Idle -> "g" + item.runs.first().id
                }
            }) { item ->
                when (item) {
                    is LogItem.Single -> RunCard(item.run, open == item.run.id) {
                        open = if (open == item.run.id) null else item.run.id
                    }
                    is LogItem.Idle -> {
                        val gid = item.runs.first().id
                        IdleCard(item.runs, openGroup == gid) { openGroup = if (openGroup == gid) null else gid }
                    }
                }
            }
        }
    }
}

/** Mehrere aufeinanderfolgende Läufe ohne Übertragung als ein Eintrag; antippen zeigt die einzelnen Läufe */
@Composable
private fun IdleCard(runs: List<SyncRun>, expanded: Boolean, onClick: () -> Unit) {
    val newest = runs.first()
    val oldest = runs.last()
    val profiles = runs.map { it.profileName }.distinct().joinToString(", ")
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${runs.size}× geprüft – nichts zu tun", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall)
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
            val end = formatTime(newest.finishedAt ?: newest.startedAt)
            val start = formatTime(oldest.startedAt)
            // Gleicher Tag -> beim Ende nur die Uhrzeit
            val endShort = if (start.substringBefore(' ') == end.substringBefore(' ')) end.substringAfter(' ') else end
            Hint("$start – $endShort · $profiles")
            if (expanded) runs.forEach { r ->
                Text("#${r.id} · ${formatTime(r.startedAt)} · ${r.profileName} (${r.trigger})",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RunCard(r: SyncRun, expanded: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val (label, color) = when (r.status) {
        "OK" -> "OK" to MaterialTheme.colorScheme.primary
        "PARTIAL" -> "Teilweise" to Color(0xFFB26A00)
        "FAILED" -> "Fehler" to MaterialTheme.colorScheme.error
        "CANCELLED" -> "Pausiert" to MaterialTheme.colorScheme.onSurfaceVariant
        "INTERRUPTED" -> "Unterbrochen" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> "Läuft" to MaterialTheme.colorScheme.tertiary
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${r.id} · " + formatTime(r.startedAt) + " · " + r.profileName, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall)
                Text(label, color = color)
            }
            Hint("Auslöser: ${r.trigger}")
            Text(r.message)
            if (r.bytes > 0) Hint(Notifications.formatBytes(r.bytes))
            if (expanded) {
                val entries by remember(r.id) { dao.logFlow(r.id) }.collectAsState(initial = emptyList())
                entries.takeLast(300).forEach { e ->
                    Text(
                        "${formatTime(e.time).substringAfter(' ')}  ${e.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (e.level) {
                            "E" -> MaterialTheme.colorScheme.error
                            "W" -> Color(0xFFB26A00)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}
