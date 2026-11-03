@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sync2pc.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.mirror.PurgeRule
import com.sync2pc.app.mirror.PurgeSides
import com.sync2pc.app.mirror.RestoreManager
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashCleaner
import com.sync2pc.app.mirror.TrashEntry
import com.sync2pc.app.mirror.TrashIndex
import com.sync2pc.app.mirror.TrashPurge
import com.sync2pc.app.sync.Notifications
import kotlinx.coroutines.launch

/**
 * Profile mit LeoSync-Papierkorb: alle Ordner-Profile (Fotos nutzen den Papierkorb der Galerie).
 * Seit 0.6.3 auch ohne "Alte Versionen behalten" – gelöschte Dateien landen trotzdem im Papierkorb.
 */
fun trashProfiles(all: List<Profile>): List<Profile> = all.filter { it.type() == ProfileType.MIRROR }

/**
 * "Papierkorb" (0.6.3, ersetzt "Wiederherstellen" und "Papierkorb leeren"):
 * Profil wählen (bei einem direkt, oben wechselbar), Übersicht mit "Aufräumen…" (Regeln im Bottom-Sheet),
 * darunter der Inhalt; markierte Einträge über die Leiste unten wiederherstellen oder endgültig löschen.
 */
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = null as List<Profile>?)
    var chosen by remember { mutableStateOf<Long?>(null) }
    val eligible = profiles?.let(::trashProfiles).orEmpty()
    val profile = eligible.firstOrNull { it.id == chosen } ?: eligible.singleOrNull()
    val hasPhotos = profiles.orEmpty().any { it.type() == ProfileType.PHOTOS }
    var menu by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Papierkorb",
        // Bei mehreren Profilen führt "Zurück" zuerst zur Profilauswahl
        onBack = { if (chosen != null && eligible.size > 1) chosen = null else onBack() },
        actions = {
            // Profil wechseln, ohne zurückzugehen
            if (profile != null && eligible.size > 1) Box {
                TextButton(onClick = { menu = true }) {
                    Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
                    Icon(Icons.Filled.ArrowDropDown, "Profil wechseln")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    eligible.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = { chosen = p.id; menu = false })
                    }
                }
            }
        },
    ) { pad ->
        when {
            profiles == null -> Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            eligible.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                SectionCard { Text("Den Papierkorb gibt es nur bei Ordner-Profilen."); PhotosHint(hasPhotos) }
            }
            profile == null -> LazyColumn(
                Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Hint("Welchen Papierkorb möchtest du öffnen?") }
                items(eligible, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth().clickable { chosen = p.id }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Hint("📁 " + p.smbPath + (if (p.remoteSubfolder.isNotBlank()) "\\" + p.remoteSubfolder else ""))
                        }
                    }
                }
                item { PhotosHint(hasPhotos) }
            }
            else -> TrashView(profile, hasPhotos, Modifier.padding(pad), onDone = onBack)
        }
    }
}

@Composable
private fun PhotosHint(show: Boolean) {
    if (show) Hint("Fotos-Profile haben keinen eigenen Papierkorb: Entfernte Fotos liegen im Papierkorb der Galerie " +
        "und werden in der Galerie-App geleert.")
}

@Composable
private fun TrashView(profile: Profile, hasPhotos: Boolean, modifier: Modifier, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var loaded by remember(profile.id) { mutableStateOf<RestoreManager.Loaded?>(null) }
    var selected by remember(profile.id) { mutableStateOf<List<TrashEntry>>(emptyList()) }
    var date by remember(profile.id) { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showCleanup by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    LaunchedEffect(profile.id, reload) { loaded = null; loaded = RestoreManager.load(ctx, profile) }

    val data = loaded
    if (data == null || working) {
        Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Hint(if (working) "Lösche…" else "Lese den Papierkorb auf Handy und PC…")
        }
        return
    }
    val visible = if (date == null) data.entries else data.entries.filter { it.date == date }
    fun isSel(e: TrashEntry) = selected.any { it.side == e.side && it.trashPath == e.trashPath }
    fun toggle(e: TrashEntry) {
        selected = if (isSel(e)) selected.filterNot { it.side == e.side && it.trashPath == e.trashPath }
            else TrashIndex.select(selected, listOf(e))
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Übersicht: belegter Platz + Aufräumen
            item {
                SectionCard(profile.name) {
                    val local = data.entries.filter { it.side == Side.LOCAL }
                    val remote = data.entries.filter { it.side == Side.REMOTE }
                    Text("Handy: ${local.size} Dateien, ${Notifications.formatBytes(TrashPurge.bytes(local))}")
                    Text(if (data.pcAvailable) "PC: ${remote.size} Dateien, ${Notifications.formatBytes(TrashPurge.bytes(remote))}"
                        else "PC: nicht erreichbar")
                    if (!data.pcAvailable) Text(
                        "Einträge aus dem PC-Papierkorb fehlen in der Liste. Wiederherstellen und Löschen auf dem PC " +
                            "werden vorgemerkt und beim nächsten Sync mit erreichbarem PC ausgeführt.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showCleanup = true },
                            enabled = data.entries.isNotEmpty() || !data.pcAvailable,
                        ) { Text("Aufräumen…") }
                        TextButton(onClick = { reload++ }) { Text("Neu einlesen") }
                    }
                }
            }
            if (data.entries.isEmpty()) {
                item { Hint("Der Papierkorb ist leer.") }
                item { PhotosHint(hasPhotos) }
                return@LazyColumn
            }
            item {
                Hint("Hier liegen gelöschte Dateien und ältere Versionen. Markiere Einträge, um sie wiederherzustellen " +
                    "oder endgültig zu löschen.")
            }
            // Filter nach Datum
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = date == null, onClick = { date = null }, label = { Text("Alle Tage") }) }
                    items(TrashIndex.dates(data.entries)) { d ->
                        FilterChip(selected = date == d, onClick = { date = d }, label = { Text(dayLabel(d)) })
                    }
                }
            }
            date?.let { d ->
                item {
                    OutlinedButton(onClick = { selected = TrashIndex.select(selected, TrashIndex.ofDate(data.entries, d)) },
                        modifier = Modifier.fillMaxWidth()) { Text("Alles vom ${dayLabel(d)} auswählen") }
                }
            }
            // Nach Ordnern gruppiert, darin je Datei alle Versionen (neueste zuerst)
            val byFolder = visible.groupBy { it.folder }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            for ((folder, inFolder) in byFolder) {
                item(key = "folder-$folder") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text("📁 " + folder.ifEmpty { "(Hauptordner)" }, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { selected = TrashIndex.select(selected, TrashIndex.inFolder(visible, folder)) }) {
                            Text("Ganzer Ordner")
                        }
                    }
                }
                for ((_, versions) in inFolder.groupBy { it.origPath.lowercase() }) {
                    val first = versions.first()
                    item(key = "file-${first.origPath.lowercase()}") {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(first.name, style = MaterialTheme.typography.bodyLarge)
                                Hint(if (first.origPath.lowercase() in data.existing) "ältere Version" else "gelöscht")
                                for (v in versions) {
                                    Row(
                                        Modifier.fillMaxWidth().clickable { toggle(v) },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(checked = isSel(v), onCheckedChange = { toggle(v) })
                                        Text(
                                            dayLabel(v.date) + " · " + (if (v.side == Side.LOCAL) "Handy" else "PC") +
                                                " · " + Notifications.formatBytes(v.size),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { PhotosHint(hasPhotos) }
        }

        // Auswahlleiste: erscheint, sobald etwas markiert ist
        if (selected.isNotEmpty()) Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${selected.size} ausgewählt · ${Notifications.formatBytes(TrashPurge.bytes(selected))}",
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { selected = emptyList() }) { Text("Aufheben") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirmRestore = true }, modifier = Modifier.weight(1f)) {
                        Text("Wiederherstellen", maxLines = 1)
                    }
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                        Text("Endgültig löschen", maxLines = 1, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showCleanup) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showCleanup = false }, sheetState = sheetState) {
            CleanupSheet(data) { rule, sides ->
                showCleanup = false; working = true
                scope.launch {
                    val r = TrashCleaner.purge(ctx, profile, rule, sides)
                    working = false
                    Toast.makeText(ctx, if (r.remotePending) "Handy geleert – PC wird beim nächsten Sync mit PC geleert"
                        else "Papierkorb aufgeräumt", Toast.LENGTH_LONG).show()
                    selected = emptyList()
                    reload++
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("${selected.size} Datei(en) endgültig löschen?") },
        text = {
            Text("Die markierten Einträge werden aus dem Papierkorb gelöscht und lassen sich danach nicht mehr " +
                "wiederherstellen." + if (!data.pcAvailable && selected.any { it.side == Side.REMOTE })
                    " Der PC ist nicht erreichbar: Dort wird beim nächsten Sync mit PC gelöscht." else "")
        },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.launch {
                    val r = TrashCleaner.deleteEntries(ctx, profile, selected)
                    Toast.makeText(ctx, if (r.remotePending) "Gelöscht – PC folgt beim nächsten Sync mit PC"
                        else "Endgültig gelöscht", Toast.LENGTH_SHORT).show()
                    selected = emptyList()
                    reload++
                }
            }) { Text("Endgültig löschen", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
    )

    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text("${selected.size} Datei(en) wiederherstellen?") },
        text = {
            Text("Die aktuellen Versionen kommen zuerst in den Papierkorb. Die gewählten Versionen werden auf Handy und PC " +
                "zurückgeschrieben. " + if (data.pcAvailable) "Das passiert gleich mit einem Sync."
                else "Der PC ist nicht erreichbar: Das passiert beim nächsten Sync mit PC.")
        },
        confirmButton = {
            TextButton(onClick = {
                confirmRestore = false
                scope.launch {
                    RestoreManager.request(ctx, profile.id, selected)
                    Toast.makeText(ctx, "Wiederherstellung vorgemerkt", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }) { Text("Wiederherstellen") }
        },
        dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Abbrechen") } },
    )
}

/** "Aufräumen…": Regeln wie früher "Papierkorb leeren" (0.6.0); onPurge erst nach der Rückfrage */
@Composable
private fun CleanupSheet(data: RestoreManager.Loaded, onPurge: (PurgeRule, PurgeSides) -> Unit) {
    var mode by remember { mutableStateOf(PurgeRule.Mode.OLDER) }
    var daysText by remember { mutableStateOf("7") }
    var dates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sides by remember { mutableStateOf(PurgeSides.BOTH) }
    var confirm by remember { mutableStateOf(false) }

    val days = daysText.toIntOrNull()?.coerceIn(0, 3650)
    val rule: PurgeRule? = when (mode) {
        PurgeRule.Mode.ALL -> PurgeRule(PurgeRule.Mode.ALL)
        PurgeRule.Mode.OLDER -> days?.let { PurgeRule.older(it) }
        PurgeRule.Mode.DATES -> if (dates.isEmpty()) null else PurgeRule(PurgeRule.Mode.DATES, dates = dates)
    }
    val affected = rule?.let { TrashPurge.affected(data.entries, it, sides) }.orEmpty()
    // PC nicht erreichbar: dort ist unbekannt, was betroffen ist -> trotzdem vormerken können
    val pcUnknown = sides.remote && !data.pcAvailable
    val canRun = rule != null && (affected.isNotEmpty() || pcUnknown)

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Aufräumen", style = MaterialTheme.typography.titleLarge)
        Hint("Endgültig gelöschte Dateien lassen sich nicht mehr wiederherstellen.")

        Text("Was?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ModeRow("Älter als … Tage", mode == PurgeRule.Mode.OLDER) { mode = PurgeRule.Mode.OLDER }
        if (mode == PurgeRule.Mode.OLDER) OutlinedTextField(
            daysText, { v -> daysText = v.filter { it.isDigit() }.take(4) },
            label = { Text("Tage") }, isError = days == null, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        ModeRow("Einzelne Tage", mode == PurgeRule.Mode.DATES) { mode = PurgeRule.Mode.DATES }
        if (mode == PurgeRule.Mode.DATES) {
            val all = TrashIndex.dates(data.entries)
            if (all.isEmpty()) Hint("Keine Tage im Papierkorb.")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(all) { d ->
                    FilterChip(selected = d in dates, onClick = { dates = if (d in dates) dates - d else dates + d },
                        label = { Text(dayLabel(d)) })
                }
            }
        }
        ModeRow("Alles", mode == PurgeRule.Mode.ALL) { mode = PurgeRule.Mode.ALL }

        Text("Wo?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        PurgeSides.entries.forEach { s -> ModeRow(s.label, sides == s) { sides = s } }
        Hint("„Nur Handy“ schafft Platz auf dem Telefon; die Kopien im PC-Papierkorb bleiben erhalten.")

        Text(
            if (rule == null) "Bitte auswählen, was geleert werden soll."
            else "Betrifft ${affected.size} Dateien (${Notifications.formatBytes(TrashPurge.bytes(affected))})" +
                if (pcUnknown) " – dazu der PC-Papierkorb, sobald der PC erreichbar ist" else "",
        )
        if (pcUnknown) Hint("Der PC ist nicht erreichbar. Das Handy wird sofort geleert, der PC beim nächsten Sync mit PC.")
        Button(
            onClick = { confirm = true }, enabled = canRun, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError),
        ) { Text("Endgültig löschen") }
    }

    if (confirm && rule != null) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Endgültig löschen?") },
        text = {
            Text("${affected.size} Dateien (${Notifications.formatBytes(TrashPurge.bytes(affected))}) werden endgültig " +
                "gelöscht (${sides.label})" + (if (pcUnknown) ", auf dem PC beim nächsten Sync mit PC" else "") +
                ". Das lässt sich nicht rückgängig machen.")
        },
        confirmButton = {
            TextButton(onClick = { confirm = false; onPurge(rule, sides) }) {
                Text("Endgültig löschen", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Abbrechen") } },
    )
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

/** "2026-11-03" -> "03.11.2026" */
fun dayLabel(d: String): String {
    val p = d.split('-')
    return if (p.size == 3) "${p[2]}.${p[1]}.${p[0]}" else d
}
