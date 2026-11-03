package com.sync2pc.app.ui

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.mirror.FolderFreer
import com.sync2pc.app.mirror.MirrorLock
import com.sync2pc.app.sync.Age
import com.sync2pc.app.sync.AgeUnit
import com.sync2pc.app.sync.FileKind
import com.sync2pc.app.sync.FreeCriteria
import com.sync2pc.app.sync.FreeItem
import com.sync2pc.app.sync.FreeRules
import com.sync2pc.app.sync.Notifications
import com.sync2pc.app.sync.SpaceFreer
import kotlinx.coroutines.launch

/** Profile, bei denen "Speicher freigeben" möglich ist (0.7.0) */
fun freeProfiles(all: List<Profile>): List<Profile> =
    all.filter { (it.type() == ProfileType.PHOTOS && it.folders().isNotEmpty()) || FolderFreer.eligible(it) }

/**
 * "Speicher freigeben" (0.2.0, seit 0.7.0 auch für Ordner-Profile Handy → PC ohne "Löschungen übernehmen"):
 * Profil wählen, Kriterien in Zeilen (Tippen öffnet die Auswahl), Vorschau mit Abwählen, dann jede Datei auf dem
 * PC prüfen. Fotos kommen in den Papierkorb der Galerie, Ordner-Dateien werden direkt gelöscht.
 */
@Composable
fun FreeSpaceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = null as List<Profile>?)
    var chosen by remember { mutableStateOf<Long?>(null) }
    val eligible = profiles?.let(::freeProfiles).orEmpty()
    val profile = eligible.firstOrNull { it.id == chosen } ?: eligible.singleOrNull()

    ScreenScaffold(
        title = "Speicher freigeben",
        onBack = { if (chosen != null && eligible.size > 1) chosen = null else onBack() },
    ) { pad ->
        when {
            profiles == null -> Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            eligible.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                SectionCard {
                    Text("Kein passendes Profil.")
                    Hint(FREE_HINT)
                }
            }
            profile == null -> LazyColumn(
                Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Hint("Für welches Profil möchtest du Speicher freigeben?") }
                items(eligible, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth().clickable { chosen = p.id }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Hint(if (p.type() == ProfileType.PHOTOS) "Fotos & Videos" else "Ordner Handy → PC")
                        }
                    }
                }
            }
            else -> FreeView(profile, eligible, onSwitch = { chosen = it }, modifier = Modifier.padding(pad))
        }
    }
}

const val FREE_HINT = "Speicher freigeben gibt es nur bei Fotos-Profilen und bei Ordner-Profilen Handy → PC " +
    "ohne „Löschungen übernehmen“."

private enum class Picker { PROFILE, AGE, FOLDERS, TYPES, SIZE, EXCLUDE }

@Composable
private fun FreeView(profile: Profile, eligible: List<Profile>, onSwitch: (Long) -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val settings = remember { GlobalSettings(ctx) }
    val scope = rememberCoroutineScope()
    val photos = profile.type() == ProfileType.PHOTOS
    val hasTrash = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    var reload by remember { mutableIntStateOf(0) }
    var items by remember(profile.id) { mutableStateOf<List<FreeItem>?>(null) }
    var photoData by remember(profile.id) { mutableStateOf<SpaceFreer.PhotoItems?>(null) }
    var loadError by remember(profile.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(profile.id, reload) {
        items = null; loadError = null
        try {
            if (photos) { val d = SpaceFreer.photoItems(ctx, profile); photoData = d; items = d.items }
            else items = FolderFreer.items(ctx, profile)
        } catch (e: Exception) { loadError = e.message ?: "Fehler beim Einlesen" }
    }

    var criteria by remember(profile.id) {
        mutableStateOf(FreeCriteria(
            age = Age(settings.freeSpaceMonths, AgeUnit.MONTHS),
            keepFavorites = settings.freeSpaceKeepFavorites,
        ))
    }
    var more by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    var showPreview by remember(profile.id) { mutableStateOf(false) }
    var deselected by remember(profile.id, criteria) { mutableStateOf<Set<String>>(emptySet()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var resultText by remember(profile.id) { mutableStateOf<String?>(null) }
    var confirmFolder by remember { mutableStateOf(false) }
    var confirmDirect by remember { mutableStateOf<List<SpaceFreer.Candidate>?>(null) }

    // Systemdialoge "In den Papierkorb" nacheinander (je max. 500 Dateien, nur Fotos)
    var requests by remember { mutableStateOf<List<Pair<android.app.PendingIntent, Int>>>(emptyList()) }
    var moved by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val current = requests.firstOrNull()
        if (res.resultCode == Activity.RESULT_OK && current != null) {
            moved += current.second
            requests = requests.drop(1)
            if (requests.isEmpty()) { resultText = "$moved Dateien in den Papierkorb der Galerie verschoben."; reload++ }
        } else {
            requests = emptyList()
            if (moved > 0) { resultText = "$moved Dateien in den Papierkorb der Galerie verschoben."; reload++ }
        }
    }
    LaunchedEffect(requests) {
        requests.firstOrNull()?.let { launcher.launch(IntentSenderRequest.Builder(it.first.intentSender).build()) }
    }

    val all = items
    if (all == null) {
        Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loadError != null) {
                Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { reload++ }) { Text("Erneut versuchen") }
            } else {
                CircularProgressIndicator()
                Hint("Lese Dateien…")
            }
        }
        return
    }

    val now = remember(all, criteria) { System.currentTimeMillis() }
    val selection = remember(all, criteria) { FreeRules.select(all, criteria, now) }
    val chosenKeys = selection.candidates.map { it.key }.toSet() - deselected
    val final = remember(all, selection, deselected) { FreeRules.wholeGroups(selection.candidates, chosenKeys) }
    val finalBytes = final.sumOf { it.size }
    val folders = remember(all) { FreeRules.folders(all) }
    val kindsOffered = if (photos) listOf(FileKind.IMAGE, FileKind.VIDEO) else FileKind.entries.toList()
    val defaults = FreeCriteria()

    fun runPhotos() {
        val data = photoData ?: return
        val list = final.mapNotNull { data.candidates[it.key] }
        busy = "Prüfe auf dem PC…"; progress = 0 to list.size; resultText = null
        scope.launch {
            try {
                val v = SpaceFreer.verify(ctx, list) { d, t -> progress = d to t }
                resultText = buildString {
                    append("${v.confirmed.size} Dateien auf dem PC bestätigt.")
                    if (v.missing > 0) append(" ${v.missing} wurden auf dem PC nicht gefunden oder sind anders – sie bleiben.")
                    if (v.unreachable.isNotEmpty()) append(" PC nicht erreichbar: ${v.unreachable.joinToString(", ")}.")
                }
                if (v.confirmed.isNotEmpty()) {
                    if (hasTrash) { moved = 0; requests = SpaceFreer.trashRequests(ctx, v.confirmed) }
                    else confirmDirect = v.confirmed
                }
            } catch (e: Exception) {
                resultText = "Prüfung fehlgeschlagen: ${e.message}"
            } finally { busy = null; progress = null }
        }
    }

    fun runFolder() {
        busy = if (MirrorLock.busy) "Wartet, bis der laufende Sync fertig ist… Diese Ansicht bitte offen lassen."
            else "Prüfe auf dem PC und lösche…"
        progress = 0 to final.size; resultText = null
        scope.launch {
            try {
                val r = FolderFreer.verifyAndDelete(ctx, profile, final) { d, t ->
                    busy = "Prüfe auf dem PC und lösche…"; progress = d to t
                }
                resultText = buildString {
                    append("${r.deleted} Dateien gelöscht, ${Notifications.formatBytes(r.bytes)} frei.")
                    if (r.kept > 0) append(" ${r.kept} Dateien bleiben (auf dem PC nicht gefunden, anders oder inzwischen geändert).")
                    if (r.unreachable) append(" Der PC war nicht (mehr) erreichbar.")
                }
                reload++
            } catch (e: Exception) {
                resultText = "Fehlgeschlagen: ${e.message}"
            } finally { busy = null; progress = null }
        }
    }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionCard(profile.name) {
                Hint(if (photos) "Verschiebt Fotos und Videos, die nachweislich auf dem PC gesichert sind, in den Papierkorb " +
                    "der Galerie. Vorher prüft LeoSync jede Datei auf dem PC. Was dort fehlt oder anders ist, bleibt."
                else "Löscht Dateien auf dem Handy, die nachweislich unverändert auf dem PC liegen. Auf dem PC bleiben sie " +
                    "erhalten. Ordner-Dateien kommen nicht in einen Papierkorb, sie werden nach der Prüfung direkt gelöscht.")
            }
        }
        // Kriterien
        item {
            SectionCard("Auswahl") {
                if (eligible.size > 1) ValueField("Profil", profile.name) { picker = Picker.PROFILE }
                ValueField(if (photos) "Älter als (Aufnahmedatum)" else "Älter als (Änderungsdatum)",
                    criteria.age?.label ?: "egal") { picker = Picker.AGE }
                ValueField("Ordner", when (criteria.folders.size) {
                    0 -> "Alle"
                    1 -> criteria.folders.first()
                    else -> "${criteria.folders.size} Ordner"
                }) { picker = Picker.FOLDERS }
                TextButton(onClick = { more = !more }) { Text(if (more) "Weitere Kriterien ausblenden" else "Weitere Kriterien…") }
                // Geänderte Zeilen bleiben auch zugeklappt sichtbar
                if (more || criteria.kinds != defaults.kinds || criteria.extensions.isNotEmpty())
                    ValueField("Dateitypen", typesLabel(criteria)) { picker = Picker.TYPES }
                if (more || criteria.minSize > 0)
                    ValueField("Grösse ab", if (criteria.minSize <= 0) "Kein Minimum" else Notifications.formatBytes(criteria.minSize)) {
                        picker = Picker.SIZE
                    }
                if (more || criteria.exclude.isNotEmpty())
                    ValueField("Ausnehmen", when (criteria.exclude.size) {
                        0 -> "Nichts"
                        1 -> criteria.exclude.first()
                        else -> "${criteria.exclude.size} Muster"
                    }) { picker = Picker.EXCLUDE }
                if (photos && (more || !criteria.keepFavorites)) {
                    if (hasTrash) SwitchRow("Favoriten behalten", criteria.keepFavorites,
                        subtitle = "In der Galerie als Favorit markierte Fotos bleiben auf dem Handy.") {
                        criteria = criteria.copy(keepFavorites = it); settings.freeSpaceKeepFavorites = it
                    }
                    else Hint("Favoriten kann Android erst ab Version 11 melden.")
                }
            }
        }
        // Ergebnis
        item {
            SectionCard {
                Text("Passend: ${final.size} Dateien · ${Notifications.formatBytes(finalBytes)}",
                    style = MaterialTheme.typography.titleMedium)
                if (deselected.isNotEmpty()) Hint("${selection.candidates.size - final.size} in der Vorschau abgewählt.")
                if (selection.notBackedUp > 0) Hint("${selection.notBackedUp} passende Dateien sind (noch) nicht gesichert und bleiben.")
                if (selection.favoritesKept > 0) Hint("${selection.favoritesKept} Favoriten bleiben.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = selection.candidates.isNotEmpty(), onClick = { showPreview = !showPreview }) {
                        Text(if (showPreview) "Vorschau ausblenden" else "Vorschau")
                    }
                }
                Button(
                    enabled = busy == null && final.isNotEmpty(),
                    onClick = { if (photos) runPhotos() else confirmFolder = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (photos) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                ) {
                    Text(when {
                        !photos -> "Auf dem PC prüfen und löschen"
                        hasTrash -> "Auf dem PC prüfen und in den Papierkorb"
                        else -> "Auf dem PC prüfen und löschen"
                    })
                }
                if (photos && hasTrash) Hint("Im Papierkorb der Galerie bleiben die Dateien 30 Tage wiederherstellbar.")
            }
        }
        busy?.let { b ->
            item {
                SectionCard {
                    Text(b)
                    val p = progress
                    if (p != null && p.second > 0) {
                        LinearProgressIndicator(progress = { p.first.toFloat() / p.second }, modifier = Modifier.fillMaxWidth())
                        Hint("${p.first} / ${p.second}")
                    } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        resultText?.let { t -> item { SectionCard("Ergebnis") { Text(t) } } }

        // Vorschau: nach Ordnern gruppiert, einzelne Dateien und ganze Ordner abwählbar
        if (showPreview) {
            val byFolder = selection.candidates.groupBy { it.folder }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            for ((folder, inFolder) in byFolder) {
                item(key = "pf-$folder") {
                    val keys = inFolder.map { it.key }.toSet()
                    val allOn = keys.none { it in deselected }
                    Row(Modifier.fillMaxWidth().clickable {
                        deselected = if (allOn) deselected + keys else deselected - keys
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = allOn, onCheckedChange = { on -> deselected = if (on) deselected - keys else deselected + keys })
                        Text("📁 " + folder.ifEmpty { "(Hauptordner)" } + " · ${inFolder.size}",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                items(inFolder, key = { "pi-" + it.key }) { f ->
                    val on = f.key !in deselected
                    Row(Modifier.fillMaxWidth().clickable { deselected = if (on) deselected + f.key else deselected - f.key }
                        .padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = on, onCheckedChange = { deselected = if (it) deselected - f.key else deselected + f.key })
                        Column {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium)
                            Hint(formatTime(f.time).substringBefore(' ') + " · " + Notifications.formatBytes(f.size))
                        }
                    }
                }
            }
            if (photos) item { Hint("Live-Fotos (Bild + Video) werden nur gemeinsam entfernt.") }
        }
    }

    // ---- Auswahlfenster ---------------------------------------------------------------
    when (picker) {
        Picker.PROFILE -> PickDialog("Profil", onDismiss = { picker = null }) {
            eligible.forEach { p ->
                RadioLine(p.name, p.id == profile.id) { picker = null; onSwitch(p.id) }
            }
        }
        Picker.AGE -> AgeDialog(criteria.age, onDismiss = { picker = null }) { a ->
            criteria = criteria.copy(age = a); picker = null
            if (a != null && a.unit == AgeUnit.MONTHS && a.amount in 1..240) settings.freeSpaceMonths = a.amount
        }
        Picker.FOLDERS -> FoldersDialog(folders, criteria.folders, onDismiss = { picker = null }) {
            criteria = criteria.copy(folders = it); picker = null
        }
        Picker.TYPES -> TypesDialog(kindsOffered, criteria, allowExtensions = !photos, onDismiss = { picker = null }) { k, e ->
            criteria = criteria.copy(kinds = k, extensions = e); picker = null
        }
        Picker.SIZE -> SizeDialog(criteria.minSize, onDismiss = { picker = null }) {
            criteria = criteria.copy(minSize = it); picker = null
        }
        Picker.EXCLUDE -> ExcludeDialog(criteria.exclude, onDismiss = { picker = null }) {
            criteria = criteria.copy(exclude = it); picker = null
        }
        null -> {}
    }

    if (confirmFolder) AlertDialog(
        onDismissRequest = { confirmFolder = false },
        title = { Text("${final.size} Dateien löschen?") },
        text = {
            Text("LeoSync prüft jede Datei auf dem PC (vorhanden, gleiche Grösse, gleicher Inhalt) und löscht sie danach " +
                "endgültig vom Handy (${Notifications.formatBytes(finalBytes)}). Auf dem PC bleiben sie erhalten. " +
                "Was dort fehlt oder anders ist, bleibt auf dem Handy.")
        },
        confirmButton = {
            TextButton(onClick = { confirmFolder = false; runFolder() }) {
                Text("Prüfen und löschen", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmFolder = false }) { Text("Abbrechen") } },
    )

    confirmDirect?.let { list ->
        AlertDialog(
            onDismissRequest = { confirmDirect = null },
            title = { Text("Endgültig löschen?") },
            text = { Text("Android 10 hat keinen Papierkorb. ${list.size} Dateien werden endgültig vom Handy gelöscht. Sie sind auf dem PC vorhanden.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDirect = null
                    scope.launch {
                        val n = SpaceFreer.deleteDirectly(ctx, list)
                        Toast.makeText(ctx, "$n Dateien gelöscht", Toast.LENGTH_LONG).show()
                        reload++
                    }
                }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { confirmDirect = null }) { Text("Abbrechen") } },
        )
    }
}

private fun typesLabel(c: FreeCriteria): String {
    val parts = c.kinds.sortedBy { it.ordinal }.map { it.label } + c.extensions.sorted().map { "*.$it" }
    return if (parts.isEmpty()) "Alle" else parts.joinToString(", ")
}

// ---- Dialoge ------------------------------------------------------------------------------

@Composable
private fun PickDialog(
    title: String, onDismiss: () -> Unit, confirm: (@Composable () -> Unit)? = null, content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) { content() } },
        confirmButton = { confirm?.invoke() },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun RadioLine(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun CheckLine(label: String, checked: Boolean, indent: Int = 0, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(start = (indent * 16).dp),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
private fun AgeDialog(current: Age?, onDismiss: () -> Unit, onDone: (Age?) -> Unit) {
    var any by remember { mutableStateOf(current == null) }
    var amount by remember { mutableStateOf((current?.amount ?: 12).toString()) }
    var unit by remember { mutableStateOf(current?.unit ?: AgeUnit.MONTHS) }
    val n = amount.toIntOrNull()?.takeIf { it in 1..9999 }
    PickDialog("Älter als", onDismiss, confirm = {
        TextButton(enabled = any || n != null, onClick = { onDone(if (any) null else Age(n!!, unit)) }) { Text("Übernehmen") }
    }) {
        RadioLine("Alter egal", any) { any = true }
        RadioLine("Älter als …", !any) { any = false }
        if (!any) {
            OutlinedTextField(
                amount, { v -> amount = v.filter { it.isDigit() }.take(4) },
                label = { Text("Anzahl") }, isError = n == null, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
            AgeUnit.entries.forEach { u -> RadioLine(u.many, unit == u) { unit = u } }
        }
    }
}

@Composable
private fun FoldersDialog(folders: List<String>, current: Set<String>, onDismiss: () -> Unit, onDone: (Set<String>) -> Unit) {
    var chosen by remember { mutableStateOf(current) }
    PickDialog("Ordner", onDismiss, confirm = { TextButton(onClick = { onDone(chosen) }) { Text("Übernehmen") } }) {
        CheckLine("Alle Ordner", chosen.isEmpty()) { if (it) chosen = emptySet() }
        if (folders.isEmpty()) Hint("Keine Unterordner.")
        Hint("Ein Ordner schliesst seine Unterordner ein.")
        for (f in folders) {
            val depth = f.count { it == '/' }
            // Liegt in einem bereits gewählten Ordner -> automatisch dabei
            val viaParent = chosen.any { it != f && FreeRules.inFolder(f, it) }
            CheckLine(f.substringAfterLast('/'), f in chosen || viaParent, indent = depth) { on ->
                chosen = if (on) chosen + f else chosen - f
            }
        }
    }
}

@Composable
private fun TypesDialog(
    offered: List<FileKind>, current: FreeCriteria, allowExtensions: Boolean,
    onDismiss: () -> Unit, onDone: (Set<FileKind>, Set<String>) -> Unit,
) {
    var kinds by remember { mutableStateOf(current.kinds) }
    var extText by remember { mutableStateOf(current.extensions.sorted().joinToString(", ")) }
    PickDialog("Dateitypen", onDismiss, confirm = {
        TextButton(onClick = { onDone(kinds, if (allowExtensions) FreeRules.parseExtensions(extText) else emptySet()) }) {
            Text("Übernehmen")
        }
    }) {
        CheckLine("Alle Typen", kinds.isEmpty() && extText.isBlank()) { if (it) { kinds = emptySet(); extText = "" } }
        for (k in offered) CheckLine(k.label, k in kinds) { on -> kinds = if (on) kinds + k else kinds - k }
        if (allowExtensions) {
            OutlinedTextField(
                extText, { extText = it }, label = { Text("Eigene Endungen") },
                placeholder = { Text("z. B. iso, bak") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Hint("„Andere“ = alles, was in keine der Gruppen passt.")
        }
    }
}

@Composable
private fun SizeDialog(current: Long, onDismiss: () -> Unit, onDone: (Long) -> Unit) {
    val mb = 1024L * 1024
    val presets = listOf(0L, mb, 10 * mb, 100 * mb)
    var custom by remember { mutableStateOf(current > 0 && current !in presets) }
    var value by remember { mutableStateOf(current) }
    var text by remember { mutableStateOf(if (current > 0) (current / mb).toString() else "") }
    val customMb = text.toLongOrNull()?.takeIf { it in 1..1_000_000 }
    PickDialog("Grösse ab", onDismiss, confirm = {
        TextButton(enabled = !custom || customMb != null, onClick = { onDone(if (custom) customMb!! * mb else value) }) {
            Text("Übernehmen")
        }
    }) {
        presets.forEach { p ->
            RadioLine(if (p == 0L) "Kein Minimum" else Notifications.formatBytes(p), !custom && value == p) { custom = false; value = p }
        }
        RadioLine("Eigener Wert", custom) { custom = true }
        if (custom) OutlinedTextField(
            text, { v -> text = v.filter { it.isDigit() }.take(7) }, label = { Text("MB") },
            isError = customMb == null, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExcludeDialog(current: List<String>, onDismiss: () -> Unit, onDone: (List<String>) -> Unit) {
    var text by remember { mutableStateOf(current.joinToString("\n")) }
    PickDialog("Ausnehmen", onDismiss, confirm = {
        TextButton(onClick = { onDone(com.sync2pc.app.mirror.FileFilter.split(text)) }) { Text("Übernehmen") }
    }) {
        Hint("Ein Muster pro Zeile. Passende Dateien bleiben immer auf dem Handy.")
        Hint("Beispiele: *.kdbx (alle Dateien mit dieser Endung), Wichtig/** (alles im Ordner „Wichtig“), Thumbs.db")
        OutlinedTextField(
            text, { text = it }, label = { Text("Muster") }, minLines = 4,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
