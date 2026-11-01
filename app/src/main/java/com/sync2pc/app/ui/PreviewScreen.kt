@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.sync.PreviewEntry
import com.sync2pc.app.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Trockenlauf: zeigt, welche Datei in welchen Ordner käme – ohne etwas zu übertragen. */
@Composable
fun PreviewScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val scope = rememberCoroutineScope()
    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Long?>(null) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<List<PreviewEntry>?>(null) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(profiles) { if (selected == null) selected = profiles.firstOrNull()?.id }

    fun start() {
        val p = profiles.firstOrNull { it.id == selected } ?: return
        running = true; result = null; status = "Starte…"
        scope.launch {
            val engine = SyncEngine(ctx, p, "Vorschau", dryRun = true, onProgress = { pr ->
                status = if (pr.totalFiles > 0) "${pr.phase} ${pr.doneFiles} / ${pr.totalFiles}" else pr.phase
            })
            val run = withContext(Dispatchers.IO) { engine.run() }
            result = engine.preview.toList()
            status = run.message
            running = false
        }
    }

    ScreenScaffold(title = "Vorschau", onBack = onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard {
                    Hint("Zeigt, wohin neue Dateien übertragen würden. Es wird nichts kopiert oder angelegt.")
                    if (profiles.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(profiles, key = { it.id }) { p ->
                            FilterChip(selected = p.id == selected, onClick = { selected = p.id }, label = { Text(p.name) })
                        }
                    }
                    Button(onClick = { start() }, enabled = !running && selected != null) { Text("Vorschau starten") }
                    if (running) LinearProgressIndicator(Modifier.fillMaxWidth())
                    status?.let { Text(it) }
                }
            }
            val r = result
            if (r != null) {
                val groups = r.groupBy { it.targetFolder }.toSortedMap()
                item { Text("${r.size} Dateien in ${groups.size} Ordnern", style = MaterialTheme.typography.titleMedium) }
                groups.forEach { (folder, entries) ->
                    item(key = folder) {
                        val open = expanded[folder] == true
                        Card(Modifier.fillMaxWidth().clickable { expanded[folder] = !open }) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        folder, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                                        color = if (entries.first().needsDecision) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text("${entries.size}")
                                }
                                if (entries.first().needsDecision) Hint("Ordnerauswahl nötig (umbenannter Monatsordner vorhanden)")
                                if (open) {
                                    entries.take(200).forEach { Text(it.fileName, style = MaterialTheme.typography.bodySmall) }
                                    if (entries.size > 200) Hint("… und ${entries.size - 200} weitere")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
