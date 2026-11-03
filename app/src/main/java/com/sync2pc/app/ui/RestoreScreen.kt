@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sync2pc.app.ui

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.mirror.RestoreManager
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashEntry
import com.sync2pc.app.mirror.TrashIndex
import com.sync2pc.app.sync.Notifications
import kotlinx.coroutines.launch

/** Ordner-Profile, deren alte Versionen wiederhergestellt werden können (Versionsschutz an) */
fun restoreProfiles(all: List<Profile>): List<Profile> =
    all.filter { it.type() == ProfileType.MIRROR && it.keepVersions }

/**
 * "Papierkorb & alte Versionen" (0.5.0). Zuerst das Profil wählen (bei nur einem direkt), dann Versionen
 * auswählen: einzeln, ganzer Ordner oder "alles vom Datum X". Pro Datei höchstens eine Version.
 */
@Composable
fun RestoreScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = null as List<Profile>?)
    var chosen by remember { mutableStateOf<Long?>(null) }
    val eligible = profiles?.let(::restoreProfiles).orEmpty()
    val profile = eligible.firstOrNull { it.id == chosen } ?: eligible.singleOrNull()

    ScreenScaffold(
        title = "Papierkorb & alte Versionen",
        // Bei mehreren Profilen führt "Zurück" zuerst zur Profilauswahl
        onBack = { if (chosen != null && eligible.size > 1) chosen = null else onBack() },
    ) { pad ->
        when {
            profiles == null -> Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            eligible.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                SectionCard {
                    Text("Kein Ordner-Profil mit Versionsschutz.")
                    Hint("Schalte im Ordner-Profil „Alte Versionen im Papierkorb behalten“ ein.")
                }
            }
            profile == null -> LazyColumn(
                Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Hint("Für welches Profil möchtest du Dateien wiederherstellen?") }
                items(eligible, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth().clickable { chosen = p.id }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Hint("📁 " + p.smbPath + (if (p.remoteSubfolder.isNotBlank()) "\\" + p.remoteSubfolder else ""))
                        }
                    }
                }
            }
            else -> RestoreList(profile, Modifier.padding(pad), onDone = onBack)
        }
    }
}

@Composable
private fun RestoreList(profile: Profile, modifier: Modifier, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var loaded by remember(profile.id) { mutableStateOf<RestoreManager.Loaded?>(null) }
    var selected by remember(profile.id) { mutableStateOf<List<TrashEntry>>(emptyList()) }
    var date by remember(profile.id) { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(profile.id, reload) { loaded = null; loaded = RestoreManager.load(ctx, profile) }

    val data = loaded
    if (data == null) {
        Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Hint("Lese den Papierkorb auf Handy und PC…")
        }
        return
    }
    val visible = if (date == null) data.entries else data.entries.filter { it.date == date }
    fun isSel(e: TrashEntry) = selected.any { it.side == e.side && it.trashPath == e.trashPath }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionCard(profile.name) {
                Hint("Hier liegen gelöschte Dateien und ältere Versionen aus dem Ordner „.LeoSync-Papierkorb“ " +
                    "(Handy und PC). Beim Wiederherstellen kommt die aktuelle Version zuerst in den Papierkorb, " +
                    "die gewählte Version wird auf Handy und PC zurückgeschrieben.")
                if (!data.pcAvailable) Text(
                    "PC nicht erreichbar – Versionen aus dem PC-Papierkorb fehlen in der Liste. Wiederherstellungen " +
                        "werden vorgemerkt und beim nächsten Sync mit erreichbarem PC ausgeführt.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { reload++ }) { Text("Neu einlesen") }
            }
        }
        if (data.entries.isEmpty()) {
            item { Hint("Der Papierkorb ist leer.") }
            return@LazyColumn
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
                            val state = if (first.origPath.lowercase() in data.existing) "ältere Version" else "gelöscht"
                            Hint(state)
                            for (v in versions) {
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        selected = if (isSel(v)) selected - v else TrashIndex.select(selected, listOf(v))
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(checked = isSel(v), onCheckedChange = {
                                        selected = if (isSel(v)) selected - v else TrashIndex.select(selected, listOf(v))
                                    })
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
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { confirm = true }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (selected.isEmpty()) "Dateien auswählen" else "${selected.size} Datei(en) wiederherstellen")
                }
                // 0.6.0: markierte Einträge endgültig löschen
                OutlinedButton(onClick = { confirmDelete = true }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text("Endgültig löschen", color = if (selected.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("${selected.size} Datei(en) endgültig löschen?") },
        text = {
            Text("Die markierten Versionen werden aus dem Papierkorb gelöscht und lassen sich danach nicht mehr " +
                "wiederherstellen." + if (!data.pcAvailable && selected.any { it.side == Side.REMOTE })
                    " Der PC ist nicht erreichbar: Dort wird beim nächsten Sync mit PC gelöscht." else "")
        },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.launch {
                    val r = com.sync2pc.app.mirror.TrashCleaner.deleteEntries(ctx, profile, selected)
                    Toast.makeText(ctx, if (r.remotePending) "Gelöscht – PC folgt beim nächsten Sync mit PC"
                        else "Endgültig gelöscht", Toast.LENGTH_SHORT).show()
                    selected = emptyList()
                    reload++
                }
            }) { Text("Endgültig löschen", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
    )

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("${selected.size} Datei(en) wiederherstellen?") },
        text = {
            Text("Die aktuellen Versionen kommen zuerst in den Papierkorb. Die gewählten Versionen werden auf Handy und PC " +
                "zurückgeschrieben. " + if (data.pcAvailable) "Das passiert gleich mit einem Sync."
                else "Der PC ist nicht erreichbar: Das passiert beim nächsten Sync mit PC.")
        },
        confirmButton = {
            TextButton(onClick = {
                confirm = false
                scope.launch {
                    RestoreManager.request(ctx, profile.id, selected)
                    Toast.makeText(ctx, "Wiederherstellung vorgemerkt", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }) { Text("Wiederherstellen") }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Abbrechen") } },
    )
}

/** "2026-11-03" -> "03.11.2026" */
fun dayLabel(d: String): String {
    val p = d.split('-')
    return if (p.size == 3) "${p[2]}.${p[1]}.${p[0]}" else d
}
