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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.Profile
import com.sync2pc.app.sync.PreviewEntry
import com.sync2pc.app.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Simulation (Trockenlauf) für EIN Profil: zeigt, welche Datei in welchen Ordner käme bzw. was der
 * Ordner-Sync tun würde – ohne etwas zu übertragen. Wahlweise nur die ersten X Dateien (schneller).
 */
@Composable
fun PreviewScreen(profileId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val settings = remember { GlobalSettings(ctx) }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<Profile?>(null) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<List<PreviewEntry>?>(null) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    // 0 = alle
    var all by remember { mutableStateOf(settings.previewLimit == 0) }
    var limitText by remember { mutableStateOf((settings.previewLimit.takeIf { it > 0 } ?: 100).toString()) }
    val limit = limitText.toIntOrNull()?.takeIf { it in 1..100_000 }

    LaunchedEffect(profileId) { profile = withContext(Dispatchers.IO) { dao.profile(profileId) } }
    // Zurück-Geste führt ins Profil (nicht zur Startseite)
    androidx.activity.compose.BackHandler { onBack() }

    fun start() {
        val p = profile ?: return
        val max = if (all) Int.MAX_VALUE else (limit ?: return)
        settings.previewLimit = if (all) 0 else max
        running = true; result = null; status = "Starte…"; expanded.clear()
        scope.launch {
            val onProgress: (com.sync2pc.app.sync.SyncProgress) -> Unit = { pr ->
                status = if (pr.totalFiles > 0) "${pr.phase} ${pr.doneFiles} / ${pr.totalFiles}" else pr.phase
            }
            val engine: com.sync2pc.app.sync.SyncRunner =
                if (p.type() == com.sync2pc.app.data.ProfileType.MIRROR)
                    com.sync2pc.app.mirror.MirrorEngine(ctx, p, "Simulation", dryRun = true, onProgress = onProgress, previewLimit = max)
                else SyncEngine(ctx, p, "Simulation", dryRun = true, onProgress = onProgress, previewLimit = max)
            val run = withContext(Dispatchers.IO) { engine.run() }
            result = engine.preview.toList()
            status = run.message
            running = false
        }
    }

    ScreenScaffold(title = "Simulation" + (profile?.let { " – ${it.name}" } ?: ""), onBack = onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard {
                    Hint("Zeigt, was beim Synchronisieren passieren würde (Bildordner: Zielordner; Ordner-Sync: Aktion je Datei). " +
                        "Es wird nichts kopiert, angelegt oder gelöscht.")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { all = false }) {
                        RadioButton(selected = !all, onClick = { all = false })
                        Text("Nur die ersten")
                        OutlinedTextField(
                            limitText, { v -> limitText = v.filter { it.isDigit() }.take(6); all = false },
                            singleLine = true, isError = !all && limit == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.padding(horizontal = 8.dp).width(96.dp),
                        )
                        Text("Dateien")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { all = true }) {
                        RadioButton(selected = all, onClick = { all = true })
                        Text("Alle Dateien")
                    }
                    Hint("Bei vielen Fotos dauert „Alle“ länger, weil jedes Foto auf Datum und Ort ausgewertet wird.")
                    Button(onClick = { start() }, enabled = !running && profile != null && (all || limit != null)) {
                        Text("Simulation starten")
                    }
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
                                if (entries.first().needsDecision) Hint("Entscheidung nötig (Ordnerauswahl bzw. Konflikt)")
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
