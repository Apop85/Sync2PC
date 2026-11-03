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
import com.sync2pc.app.R
import com.sync2pc.app.l10n.str
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
        title = str(R.string.home_cd_log),
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                scope.launch {
                    try {
                        val text = Diagnostics.logExport(ctx)
                        Diagnostics.shareAsFile(ctx, text, str(R.string.log_export_file, Diagnostics.fileStamp()))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(ctx, str(R.string.log_export_failed, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }) { Icon(Icons.Filled.Share, str(R.string.log_share)) }
            IconButton(onClick = {
                scope.launch { withContext(Dispatchers.IO) { dao.clearLogs(); dao.clearRuns() } }
            }) { Icon(Icons.Filled.Delete, str(R.string.log_clear)) }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // "Alle" = kein Filter (setzt beide zurück); "Fehler"/"Übertragungen" einzeln oder zusammen
                    FilterChip(selected = !onlyErrors && !onlyTransfers, onClick = {
                        onlyErrors = false; onlyTransfers = false
                        settings.logFilterErrors = false; settings.logFilterTransfers = false
                    }, label = { Text(str(R.string.free_all)) })
                    FilterChip(selected = onlyErrors, onClick = { onlyErrors = !onlyErrors; settings.logFilterErrors = onlyErrors },
                        label = { Text(str(R.string.log_filter_errors)) })
                    FilterChip(selected = onlyTransfers, onClick = { onlyTransfers = !onlyTransfers; settings.logFilterTransfers = onlyTransfers },
                        label = { Text(str(R.string.log_filter_transfers)) })
                }
            }
            if (shown.isEmpty()) item {
                Text(str(when {
                    runs.isEmpty() -> R.string.log_empty
                    onlyErrors && onlyTransfers -> R.string.log_empty_both
                    onlyErrors -> R.string.log_empty_errors
                    else -> R.string.log_empty_transfers
                }))
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
    val profiles = runs.map { it.profileName.trim() }.distinct().joinToString(", ")
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Kein "OK" mehr (0.9.0): hervorgehoben wird nur, was nicht OK ist
                Text(str(R.string.log_idle_group, runs.size), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall)
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
        "OK" -> str(R.string.common_ok) to MaterialTheme.colorScheme.primary
        "PARTIAL" -> str(R.string.log_status_partial) to Color(0xFFB26A00)
        "FAILED" -> str(R.string.log_status_failed) to MaterialTheme.colorScheme.error
        "CANCELLED" -> str(R.string.log_status_paused) to MaterialTheme.colorScheme.onSurfaceVariant
        "INTERRUPTED" -> str(R.string.log_status_interrupted) to MaterialTheme.colorScheme.onSurfaceVariant
        "WAITING" -> str(R.string.log_status_waiting) to MaterialTheme.colorScheme.tertiary
        else -> str(R.string.log_status_running) to MaterialTheme.colorScheme.tertiary
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${r.id} · " + formatTime(r.startedAt) + " · " + r.profileName.trim(), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall)
                if (r.status != "OK") Text(label, color = color)
            }
            Hint(str(R.string.log_trigger, r.trigger))
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
