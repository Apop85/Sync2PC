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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
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
import com.sync2pc.app.R
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.mirror.PendingIntervention
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
        title = str(R.string.home_tool_trash),
        // Bei mehreren Profilen führt "Zurück" zuerst zur Profilauswahl
        onBack = { if (chosen != null && eligible.size > 1) chosen = null else onBack() },
        actions = {
            // Profil wechseln, ohne zurückzugehen
            if (profile != null && eligible.size > 1) Box {
                TextButton(onClick = { menu = true }) {
                    Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
                    Icon(Icons.Filled.ArrowDropDown, str(R.string.trash_switch_profile))
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
                SectionCard { Text(str(R.string.home_tool_trash_hint)); PhotosHint(hasPhotos) }
            }
            profile == null -> LazyColumn(
                Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Hint(str(R.string.trash_choose_profile)) }
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
    if (show) Hint(str(R.string.trash_photos_hint))
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
    var pickDay by remember { mutableStateOf(false) }
    LaunchedEffect(profile.id, reload) { loaded = null; loaded = RestoreManager.load(ctx, profile) }

    // Eingriffe warten auf einen laufenden Ordner-Sync und überleben das Verlassen der Ansicht (0.11.10, DEC-144)
    val task = rememberIntervention(profile.id) { selected = emptyList(); reload++ }
    val app = remember { ctx.applicationContext }
    fun busyToast() = Toast.makeText(ctx, str(R.string.intervention_already_pending), Toast.LENGTH_LONG).show()

    val data = loaded
    if (task != null) {
        InterventionPanel(profile.id, task, modifier)
        return
    }
    if (data == null) {
        Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Hint(str(R.string.trash_reading))
        }
        return
    }
    // Gewählter Tag nach "Neu einlesen" nicht mehr vorhanden -> wieder alle Tage
    val day = date?.takeIf { d -> data.entries.any { it.date == d } }
    if (pickDay) DayPickerDialog(
        entries = data.entries, initial = setOfNotNull(day), multi = false,
        onDismiss = { pickDay = false },
        onDone = { date = it.firstOrNull(); pickDay = false },
    )
    val visible = if (day == null) data.entries else data.entries.filter { it.date == day }
    fun isSel(e: TrashEntry) = selected.any { it.side == e.side && it.trashPath == e.trashPath }
    /** Ohne vorgemerkte Einträge (die sind schon unterwegs) */
    fun free(list: List<TrashEntry>) = list.filter { TrashPurge.key(it) !in data.reserved }
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
                    Text(plural(R.plurals.trash_phone_summary, local.size, local.size, Notifications.formatBytes(TrashPurge.bytes(local))))
                    if (data.pcAvailable) Text(plural(R.plurals.trash_pc_summary, remote.size, remote.size, Notifications.formatBytes(TrashPurge.bytes(remote))))
                    else if (data.pcMessage != null) ErrorText(data.pcMessage)   // anderer/unbekannter PC antwortet
                    else ErrorText(str(R.string.trash_pc_unreachable))
                    if (!data.pcAvailable) Text(
                        str(R.string.trash_pc_unreachable_hint),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showCleanup = true },
                            enabled = data.entries.isNotEmpty() || !data.pcAvailable,
                        ) { Text(str(R.string.trash_cleanup_button)) }
                        TextButton(onClick = { reload++ }) { Text(str(R.string.trash_reload)) }
                    }
                }
            }
            if (data.entries.isEmpty()) {
                item { Hint(str(R.string.trash_empty)) }
                item { PhotosHint(hasPhotos) }
                return@LazyColumn
            }
            item { Hint(str(R.string.trash_intro)) }
            // Filter nach Datum (0.6.4: ein Wertfeld statt einer Chip-Reihe pro Tag)
            item {
                ValueField(str(R.string.trash_day), day?.let(::dayLabel) ?: str(R.string.trash_all_days)) { pickDay = true }
            }
            day?.let { d ->
                item {
                    OutlinedButton(onClick = { selected = TrashIndex.select(selected, free(TrashIndex.ofDate(data.entries, d))) },
                        modifier = Modifier.fillMaxWidth()) { Text(str(R.string.trash_select_day, dayLabel(d))) }
                }
            }
            // Nach Ordnern gruppiert, darin je Datei alle Versionen (neueste zuerst)
            val byFolder = visible.groupBy { it.folder }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            for ((folder, inFolder) in byFolder) {
                item(key = "folder-$folder") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text("📁 " + folder.ifEmpty { str(R.string.free_main_folder) }, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { selected = TrashIndex.select(selected, free(TrashIndex.inFolder(visible, folder))) }) {
                            Text(str(R.string.trash_whole_folder))
                        }
                    }
                }
                for ((_, versions) in inFolder.groupBy { it.origPath.lowercase() }) {
                    val first = versions.first()
                    item(key = "file-${first.origPath.lowercase()}") {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(first.name, style = MaterialTheme.typography.bodyLarge)
                                Hint(str(if (first.origPath.lowercase() in data.existing) R.string.trash_older_version else R.string.trash_deleted))
                                for (v in versions) {
                                    // Zum Wiederherstellen vorgemerkt -> nicht nochmals wählbar, wird nie gelöscht (0.7.0)
                                    val reserved = TrashPurge.key(v) in data.reserved
                                    Row(
                                        Modifier.fillMaxWidth().clickable(enabled = !reserved) { toggle(v) },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(checked = isSel(v), onCheckedChange = { toggle(v) }, enabled = !reserved)
                                        Column {
                                            Text(
                                                (if (v.modified > 0) formatTime(v.modified) else dayLabel(v.date)) + " · " + str(if (v.side == Side.LOCAL) R.string.side_phone else R.string.side_pc) +
                                                    " · " + Notifications.formatBytes(v.size),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            if (reserved) Hint(str(R.string.trash_reserved))
                                        }
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
                    Text(str(R.string.trash_selected, selected.size, Notifications.formatBytes(TrashPurge.bytes(selected))),
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { selected = emptyList() }) { Text(str(R.string.trash_clear_selection)) }
                }
                // Je Knopf eine Zeile (0.7.1): nebeneinander wurden die Texte abgeschnitten
                Button(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(str(R.string.trash_restore))
                }
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(str(R.string.delete_mode_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showCleanup) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showCleanup = false }, sheetState = sheetState) {
            CleanupSheet(data) { rule, sides ->
                showCleanup = false
                // Nur der hier bestätigte Stand wird ausgeführt, auch wenn der Eingriff auf einen Sync warten muss
                val confirmed = TrashPurge.affected(data.entries, rule, sides, data.reserved)
                val pcKnown = data.pcAvailable
                val started = PendingIntervention.start(app, profile.id) { onStart, _ ->
                    val r = TrashCleaner.purgeConfirmed(app, profile, rule, sides, confirmed, pcKnown, onStart)
                    str(when {
                        r.remotePending -> R.string.trash_purged_pc_pending
                        r.local + r.remote == 0 -> R.string.trash_nothing_deleted // nichts mehr da/geändert/vorgemerkt
                        else -> R.string.trash_purged
                    })
                }
                if (!started) busyToast()
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = {
            // Bei genau einer Datei wird der Name genannt
            if (selected.size == 1) Text(str(R.string.trash_delete_title_one, selected.first().name))
            else Text(plural(R.plurals.trash_delete_title, selected.size))
        },
        text = {
            Text(str(R.string.trash_delete_text) + if (!data.pcAvailable && selected.any { it.side == Side.REMOTE })
                " " + str(R.string.trash_delete_pc_later) else "")
        },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                val chosenEntries = selected
                val started = PendingIntervention.start(app, profile.id) { onStart, _ ->
                    val r = TrashCleaner.deleteEntries(app, profile, chosenEntries, onStart)
                    str(when {
                        r.remotePending -> R.string.trash_deleted_pc_pending
                        r.local + r.remote == 0 -> R.string.trash_nothing_deleted
                        else -> R.string.trash_deleted_done
                    })
                }
                if (!started) busyToast()
            }) { Text(str(R.string.delete_mode_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(str(R.string.common_cancel)) } },
    )

    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text(plural(R.plurals.trash_restore_title, selected.size)) },
        text = {
            Text(str(R.string.trash_restore_text) + " " +
                str(if (data.pcAvailable) R.string.trash_restore_now else R.string.trash_restore_later))
        },
        confirmButton = {
            TextButton(onClick = {
                confirmRestore = false
                scope.launch {
                    RestoreManager.request(ctx, profile.id, selected)
                    Toast.makeText(ctx, str(R.string.home_restore_pending_title), Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }) { Text(str(R.string.trash_restore)) }
        },
        dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text(str(R.string.common_cancel)) } },
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
    var pickDays by remember { mutableStateOf(false) }

    // 1 oder mehr Tage; leer/0 ist ungültig (Hinweiszeile, «Endgültig löschen» bleibt gesperrt)
    val days = daysText.toIntOrNull()?.coerceIn(0, 3650)?.takeIf { it >= 1 }
    val rule: PurgeRule? = when (mode) {
        PurgeRule.Mode.ALL -> PurgeRule(PurgeRule.Mode.ALL)
        PurgeRule.Mode.OLDER -> days?.let { PurgeRule.older(it) }
        PurgeRule.Mode.DATES -> if (dates.isEmpty()) null else PurgeRule(PurgeRule.Mode.DATES, dates = dates)
    }
    val affected = rule?.let { TrashPurge.affected(data.entries, it, sides, data.reserved) }.orEmpty()
    // Vorgemerkte Wiederherstellungen bleiben liegen (0.7.0)
    val kept = rule?.let { TrashPurge.protectedHits(data.entries, it, sides, data.reserved) } ?: 0
    // PC nicht erreichbar: dort ist unbekannt, was betroffen ist -> trotzdem vormerken können
    val pcUnknown = sides.remote && !data.pcAvailable
    val canRun = rule != null && (affected.isNotEmpty() || pcUnknown)
    val affectedBytes = Notifications.formatBytes(TrashPurge.bytes(affected))

    Column(Modifier.fillMaxWidth()) {
    // Inhalt scrollt; Zähler und Knopf liegen darunter in einer festen Fusszeile (immer sichtbar, auch mit Tastatur/grosser Schrift)
    Column(
        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(str(R.string.trash_cleanup), style = MaterialTheme.typography.titleLarge)
        Hint(str(R.string.trash_cleanup_hint))

        Text(str(R.string.trash_what), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ModeRow(str(R.string.trash_mode_older), mode == PurgeRule.Mode.OLDER) { mode = PurgeRule.Mode.OLDER }
        if (mode == PurgeRule.Mode.OLDER) OutlinedTextField(
            daysText, { v -> daysText = v.filter { it.isDigit() }.take(4) },
            label = { Text(str(R.string.unit_days)) }, isError = days == null, singleLine = true,
            supportingText = if (days == null) ({ Text(str(R.string.trash_days_invalid)) }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        ModeRow(str(R.string.trash_mode_dates), mode == PurgeRule.Mode.DATES) { mode = PurgeRule.Mode.DATES }
        if (mode == PurgeRule.Mode.DATES) {
            if (data.entries.isEmpty()) Hint(str(R.string.trash_no_days))
            else ValueField(str(R.string.unit_days), when (dates.size) {
                0 -> str(R.string.trash_none_chosen)
                1 -> dayLabel(dates.first())
                else -> plural(R.plurals.trash_days_chosen, dates.size)
            }) { pickDays = true }
        }
        ModeRow(str(R.string.trash_mode_all), mode == PurgeRule.Mode.ALL) { mode = PurgeRule.Mode.ALL }

        Text(str(R.string.trash_where), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        PurgeSides.entries.forEach { s -> ModeRow(s.label, sides == s) { sides = s } }
        Hint(str(R.string.trash_phone_only_hint, PurgeSides.LOCAL.label))
    }

    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                when {
                    mode == PurgeRule.Mode.OLDER && days == null -> str(R.string.trash_days_invalid)
                    rule == null -> str(R.string.trash_choose_what)
                    else -> plural(R.plurals.trash_affects, affected.size, affected.size, affectedBytes) +
                        if (pcUnknown) " " + str(R.string.trash_affects_pc_later) else ""
                },
                color = if (mode == PurgeRule.Mode.OLDER && days == null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
            )
            if (pcUnknown) Hint(str(R.string.trash_pc_later_hint))
            if (kept > 0) Hint(plural(R.plurals.trash_kept_reserved, kept))
            Button(
                onClick = { confirm = true }, enabled = canRun, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError),
            ) { Text(str(R.string.delete_mode_delete)) }
        }
    }
    }

    if (pickDays) DayPickerDialog(
        entries = data.entries, initial = dates, multi = true,
        onDismiss = { pickDays = false },
        onDone = { dates = it; pickDays = false },
    )

    if (confirm && rule != null) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(str(R.string.free_direct_title)) },
        text = {
            Text(plural(R.plurals.trash_purge_confirm, affected.size, affected.size, affectedBytes, sides.label) +
                (if (pcUnknown) " " + str(R.string.trash_purge_confirm_pc) else "") +
                " " + str(R.string.trash_purge_irreversible) +
                if (kept > 0) " " + plural(R.plurals.trash_kept_reserved, kept) else "")
        },
        confirmButton = {
            TextButton(onClick = { confirm = false; onPurge(rule, sides) }) {
                Text(str(R.string.delete_mode_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(str(R.string.common_cancel)) } },
    )
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

/** Auswahl als Zeile: Name oben klein, aktueller Wert darunter; Tippen öffnet die Auswahl (0.6.4) */
@Composable
fun ValueField(label: String, value: String, onClick: () -> Unit) {
    // Durchsichtig (0.9.0): sieht in Karten und auf dem Hintergrund gleich aus wie ein umrandetes Eingabefeld
    OutlinedCard(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Filled.ArrowDropDown, null)
        }
    }
}

/**
 * Tage im Papierkorb als Liste (neueste zuerst) mit Anzahl und Grösse pro Tag.
 * multi = false: "Alle Tage" + ein Tag, Tippen übernimmt sofort (leere Menge = alle Tage).
 * multi = true: Häkchen, "Übernehmen".
 */
@Composable
private fun DayPickerDialog(
    entries: List<TrashEntry>, initial: Set<String>, multi: Boolean,
    onDismiss: () -> Unit, onDone: (Set<String>) -> Unit,
) {
    var chosen by remember { mutableStateOf(initial) }
    val byDay = remember(entries) { entries.groupBy { it.date } }
    val days = remember(entries) { TrashIndex.dates(entries) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(if (multi) R.string.trash_pick_days else R.string.trash_pick_day)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                if (multi) item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { chosen = days.toSet() }) { Text(str(R.string.free_all)) }
                        TextButton(onClick = { chosen = emptySet() }) { Text(str(R.string.trash_none)) }
                    }
                } else item {
                    Row(Modifier.fillMaxWidth().clickable { onDone(emptySet()) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = initial.isEmpty(), onClick = { onDone(emptySet()) })
                        Text(str(R.string.trash_all_days), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                items(days, key = { it }) { d ->
                    val inDay = byDay[d].orEmpty()
                    val toggle: () -> Unit = {
                        if (multi) { chosen = if (d in chosen) chosen - d else chosen + d } else { onDone(setOf(d)) }
                    }
                    Row(Modifier.fillMaxWidth().clickable(onClick = toggle), verticalAlignment = Alignment.CenterVertically) {
                        if (multi) Checkbox(checked = d in chosen, onCheckedChange = { toggle() })
                        else RadioButton(selected = d in initial, onClick = toggle)
                        Column {
                            Text(dayLabel(d), style = MaterialTheme.typography.bodyLarge)
                            Hint(plural(R.plurals.trash_day_files, inDay.size, inDay.size, Notifications.formatBytes(TrashPurge.bytes(inDay))))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (multi) TextButton(onClick = { onDone(chosen) }) { Text(str(R.string.free_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str(R.string.common_cancel)) } },
    )
}

/** "2026-11-03" -> Deutsch "03.11.2026", Englisch unverändert (ISO, eindeutig) */
fun dayLabel(d: String): String {
    val p = d.split('-')
    return if (p.size == 3 && Texts.isGerman) "${p[2]}.${p[1]}.${p[0]}" else d
}
