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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.mirror.PurgeRule
import com.sync2pc.app.mirror.PurgeSides
import com.sync2pc.app.mirror.RestoreManager
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashCleaner
import com.sync2pc.app.mirror.TrashIndex
import com.sync2pc.app.mirror.TrashPurge
import com.sync2pc.app.sync.Notifications
import kotlinx.coroutines.launch

/** Profile mit LeoSync-Papierkorb (nur Ordner-Profile; Fotos nutzen den Android-Papierkorb der Galerie) */
fun purgeProfiles(all: List<Profile>): List<Profile> = all.filter { it.type() == ProfileType.MIRROR }

/** "Papierkorb leeren" (0.6.0): zuerst Profil wählen (bei einem direkt), dann Regel und Seite. */
@Composable
fun PurgeScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = null as List<Profile>?)
    var chosen by remember { mutableStateOf<Long?>(null) }
    val eligible = profiles?.let(::purgeProfiles).orEmpty()
    val profile = eligible.firstOrNull { it.id == chosen } ?: eligible.singleOrNull()
    val hasPhotos = profiles.orEmpty().any { it.type() == ProfileType.PHOTOS }

    ScreenScaffold(
        title = "Papierkorb leeren",
        onBack = { if (chosen != null && eligible.size > 1) chosen = null else onBack() },
    ) { pad ->
        when {
            profiles == null -> Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            eligible.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                SectionCard { Text("Kein Ordner-Profil vorhanden."); PhotosHint(hasPhotos) }
            }
            profile == null -> LazyColumn(
                Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Hint("Für welches Profil möchtest du den Papierkorb leeren?") }
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
            else -> PurgeView(profile, hasPhotos, Modifier.padding(pad))
        }
    }
}

@Composable
private fun PhotosHint(show: Boolean) {
    if (show) Hint("Fotos-Profile haben keinen eigenen Papierkorb: Entfernte Fotos liegen im Papierkorb der Galerie " +
        "und werden in der Galerie-App geleert.")
}

@Composable
private fun PurgeView(profile: Profile, hasPhotos: Boolean, modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var loaded by remember(profile.id) { mutableStateOf<RestoreManager.Loaded?>(null) }
    var mode by remember { mutableStateOf(PurgeRule.Mode.OLDER) }
    var daysText by remember { mutableStateOf("7") }
    var dates by remember(profile.id) { mutableStateOf<Set<String>>(emptySet()) }
    var sides by remember { mutableStateOf(PurgeSides.BOTH) }
    var confirm by remember { mutableStateOf(false) }
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

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(profile.name) {
                val local = data.entries.filter { it.side == Side.LOCAL }
                val remote = data.entries.filter { it.side == Side.REMOTE }
                Text("Handy: ${local.size} Dateien, ${Notifications.formatBytes(TrashPurge.bytes(local))}")
                Text(if (data.pcAvailable) "PC: ${remote.size} Dateien, ${Notifications.formatBytes(TrashPurge.bytes(remote))}"
                    else "PC: nicht erreichbar")
                Hint("Im Papierkorb liegen gelöschte Dateien und alte Versionen aus dem Ordner „.LeoSync-Papierkorb“. " +
                    "Endgültig gelöschte Dateien lassen sich nicht mehr wiederherstellen.")
                TextButton(onClick = { reload++ }) { Text("Neu einlesen") }
            }
        }
        item {
            SectionCard("Was leeren?") {
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
            }
        }
        item {
            SectionCard("Wo?") {
                PurgeSides.entries.forEach { s -> ModeRow(s.label, sides == s) { sides = s } }
                Hint("„Nur Handy“ schafft Platz auf dem Telefon; die Kopien im PC-Papierkorb bleiben erhalten.")
            }
        }
        item {
            SectionCard {
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
                PhotosHint(hasPhotos)
            }
        }
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
            TextButton(onClick = {
                confirm = false; working = true
                scope.launch {
                    val r = TrashCleaner.purge(ctx, profile, rule, sides)
                    working = false
                    Toast.makeText(ctx, if (r.remotePending) "Handy geleert – PC wird beim nächsten Sync mit PC geleert"
                        else "Papierkorb geleert", Toast.LENGTH_LONG).show()
                    reload++
                }
            }) { Text("Endgültig löschen", color = MaterialTheme.colorScheme.error) }
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
