package com.sync2pc.app.ui

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.sync2pc.app.R
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.mirror.FolderFreer
import com.sync2pc.app.mirror.PendingIntervention
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
        title = str(R.string.home_tool_free),
        onBack = { if (chosen != null && eligible.size > 1) chosen = null else onBack() },
    ) { pad ->
        when {
            profiles == null -> Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            eligible.isEmpty() -> Column(Modifier.padding(pad).padding(16.dp)) {
                SectionCard {
                    Text(str(R.string.free_no_profile))
                    Hint(str(R.string.free_hint))
                }
            }
            profile == null -> LazyColumn(
                Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Hint(str(R.string.free_choose_profile)) }
                items(eligible, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth().clickable { chosen = p.id }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Hint(str(if (p.type() == ProfileType.PHOTOS) R.string.free_kind_photos else R.string.free_kind_folder))
                        }
                    }
                }
            }
            else -> FreeView(profile, eligible, onSwitch = { chosen = it }, modifier = Modifier.padding(pad))
        }
    }
}

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
        } catch (e: Exception) { loadError = e.message ?: str(R.string.free_read_error) }
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
    var resultFailed by remember(profile.id) { mutableStateOf(false) }
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
            if (requests.isEmpty()) { resultText = plural(R.plurals.free_moved_to_gallery_trash, moved); reload++ }
        } else {
            requests = emptyList()
            if (moved > 0) { resultText = plural(R.plurals.free_moved_to_gallery_trash, moved); reload++ }
        }
    }
    LaunchedEffect(requests) {
        requests.firstOrNull()?.let { launcher.launch(IntentSenderRequest.Builder(it.first.intentSender).build()) }
    }

    // Wartender/laufender Eingriff (Ordner-Profile): Anzeige mit "Abbrechen"; Ergebnis nach dem Ende hier oder als Hinweis
    val task = rememberIntervention(profile.id) { resultText = null; reload++ }
    if (task != null) {
        InterventionPanel(profile.id, task, modifier)
        return
    }

    val all = items
    if (all == null) {
        Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loadError != null) {
                Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { reload++ }) { Text(str(R.string.home_retry)) }
            } else {
                CircularProgressIndicator()
                Hint(str(R.string.free_reading))
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
        busy = str(R.string.free_checking_pc); progress = 0 to list.size; resultText = null; resultFailed = false
        scope.launch {
            try {
                val v = SpaceFreer.verify(ctx, list) { d, t -> progress = d to t }
                resultText = buildString {
                    append(plural(R.plurals.free_confirmed, v.confirmed.size))
                    if (v.missing > 0) append(" ").append(plural(R.plurals.free_missing, v.missing))
                    if (v.unreachable.isNotEmpty()) append(" ").append(str(R.string.free_unreachable_hosts, v.unreachable.joinToString(", ")))
                    if (v.rejected.isNotEmpty()) append(" ").append(str(R.string.free_other_pc_hosts, v.rejected.joinToString(", ")))
                }
                resultFailed = v.unreachable.isNotEmpty() || v.rejected.isNotEmpty()   // nichts verifiziert = Fehler (rot)
                if (v.confirmed.isNotEmpty()) {
                    if (hasTrash) { moved = 0; requests = SpaceFreer.trashRequests(ctx, v.confirmed) }
                    else confirmDirect = v.confirmed
                }
            } catch (e: Exception) {
                resultText = str(R.string.free_check_failed, e.message ?: ""); resultFailed = true
            } finally { busy = null; progress = null }
        }
    }

    fun runFolder() {
        resultText = null; resultFailed = false
        val list = final
        val app = ctx.applicationContext
        // Wartet auf einen laufenden Ordner-Sync und überlebt das Verlassen der Ansicht (0.11.10, DEC-144).
        // FolderFreer.verifyAndDelete prüft jede Datei unter der Sperre neu; die Auswahl wird nicht neu ausgewertet.
        val started = PendingIntervention.start(app, profile.id) { onStart, report ->
            val r = FolderFreer.verifyAndDelete(app, profile, list, { d, t -> report(d, t) }, onStart)
            buildString {
                append(plural(R.plurals.free_deleted, r.deleted, r.deleted, Notifications.formatBytes(r.bytes)))
                if (r.foldersRemoved > 0) append(" ").append(plural(R.plurals.free_folders_removed, r.foldersRemoved))
                if (r.kept > 0) append(" ").append(plural(R.plurals.free_kept, r.kept))
                if (r.unreachable) append(" ").append(str(R.string.free_pc_lost))
                r.rejectedMessage?.let { append(" ").append(it) }
            }
        }
        if (!started) Toast.makeText(ctx, str(R.string.intervention_already_pending), Toast.LENGTH_LONG).show()
    }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionCard(profile.name) {
                Hint(str(if (photos) R.string.free_intro_photos else R.string.free_intro_folder))
                // 0.9.5: Bei Prüfstufe "Aus" gelten Fotos nicht als geprüft und erscheinen hier nicht – sagen, warum
                if (photos && profile.verify() == com.sync2pc.app.data.VerifyLevel.OFF) Text(
                    str(R.string.free_verify_off_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                )
            }
        }
        // Kriterien
        item {
            SectionCard(str(R.string.free_selection)) {
                if (eligible.size > 1) ValueField(str(R.string.free_profile), profile.name) { picker = Picker.PROFILE }
                ValueField(str(if (photos) R.string.free_older_taken else R.string.free_older_modified),
                    criteria.age?.label ?: str(R.string.free_any)) { picker = Picker.AGE }
                ValueField(str(R.string.free_folders), when (criteria.folders.size) {
                    0 -> str(R.string.free_all)
                    1 -> criteria.folders.first()
                    else -> plural(R.plurals.free_n_folders, criteria.folders.size)
                }) { picker = Picker.FOLDERS }
                TextButton(onClick = { more = !more }) { Text(str(if (more) R.string.free_less else R.string.free_more)) }
                // Geänderte Zeilen bleiben auch zugeklappt sichtbar
                if (more || criteria.kinds != defaults.kinds || criteria.extensions.isNotEmpty())
                    ValueField(str(R.string.free_types), typesLabel(criteria)) { picker = Picker.TYPES }
                if (more || criteria.minSize > 0)
                    ValueField(str(R.string.free_min_size), if (criteria.minSize <= 0) str(R.string.free_no_minimum) else Notifications.formatBytes(criteria.minSize)) {
                        picker = Picker.SIZE
                    }
                if (more || criteria.exclude.isNotEmpty())
                    ValueField(str(R.string.free_exclude), when (criteria.exclude.size) {
                        0 -> str(R.string.free_nothing)
                        1 -> criteria.exclude.first()
                        else -> plural(R.plurals.free_n_patterns, criteria.exclude.size)
                    }) { picker = Picker.EXCLUDE }
                if (photos && (more || !criteria.keepFavorites)) {
                    if (hasTrash) SwitchRow(str(R.string.free_keep_favorites), criteria.keepFavorites,
                        subtitle = str(R.string.free_keep_favorites_sub)) {
                        criteria = criteria.copy(keepFavorites = it); settings.freeSpaceKeepFavorites = it
                    }
                    else Hint(str(R.string.free_favorites_android11))
                }
            }
        }
        // Ergebnis
        item {
            SectionCard {
                Text(plural(R.plurals.free_matching, final.size, final.size, Notifications.formatBytes(finalBytes)),
                    style = MaterialTheme.typography.titleMedium)
                if (deselected.isNotEmpty()) Hint(str(R.string.free_deselected, selection.candidates.size - final.size))
                if (selection.notBackedUp > 0) Hint(plural(R.plurals.free_not_backed_up, selection.notBackedUp))
                if (selection.favoritesKept > 0) Hint(plural(R.plurals.free_favorites_kept, selection.favoritesKept))
                // 0.9.0: Vorschau ist der Hauptschritt, Entfernen bewusst zurückhaltend (umrandet, Warnfarbe)
                Button(
                    enabled = selection.candidates.isNotEmpty(), onClick = { showPreview = !showPreview },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(str(if (showPreview) R.string.free_preview_hide else R.string.free_preview))
                }
                OutlinedButton(
                    enabled = busy == null && final.isNotEmpty(),
                    onClick = { if (photos) runPhotos() else confirmFolder = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp,
                        if (busy == null && final.isNotEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                ) {
                    Text(str(if (photos && hasTrash) R.string.free_run_trash else R.string.free_run_delete))
                }
                if (photos && hasTrash) Hint(str(R.string.free_gallery_trash_hint))
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
        resultText?.let { t -> item { SectionCard(str(R.string.free_result)) { if (resultFailed) ErrorText(t) else Text(t) } } }

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
                        Text("📁 " + folder.ifEmpty { str(R.string.free_main_folder) } + " · ${inFolder.size}",
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
            if (photos) item { Hint(str(R.string.free_live_photos)) }
        }
    }

    // ---- Auswahlfenster ---------------------------------------------------------------
    when (picker) {
        Picker.PROFILE -> PickDialog(str(R.string.free_profile), onDismiss = { picker = null }) {
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
        title = { Text(plural(R.plurals.free_confirm_title, final.size)) },
        text = { Text(str(R.string.free_confirm_text, Notifications.formatBytes(finalBytes))) },
        confirmButton = {
            TextButton(onClick = { confirmFolder = false; runFolder() }) {
                Text(str(R.string.free_confirm_button), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmFolder = false }) { Text(str(R.string.common_cancel)) } },
    )

    confirmDirect?.let { list ->
        AlertDialog(
            onDismissRequest = { confirmDirect = null },
            title = { Text(str(R.string.free_direct_title)) },
            text = { Text(plural(R.plurals.free_direct_text, list.size)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDirect = null
                    scope.launch {
                        val n = SpaceFreer.deleteDirectly(ctx, list)
                        Toast.makeText(ctx, plural(R.plurals.free_direct_done, n), Toast.LENGTH_LONG).show()
                        reload++
                    }
                }) { Text(str(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDirect = null }) { Text(str(R.string.common_cancel)) } },
        )
    }
}

private fun typesLabel(c: FreeCriteria): String {
    val parts = c.kinds.sortedBy { it.ordinal }.map { it.label } + c.extensions.sorted().map { "*.$it" }
    return if (parts.isEmpty()) str(R.string.free_all) else parts.joinToString(", ")
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(str(R.string.common_cancel)) } },
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
    PickDialog(str(R.string.free_age_title), onDismiss, confirm = {
        TextButton(enabled = any || n != null, onClick = { onDone(if (any) null else Age(n!!, unit)) }) { Text(str(R.string.free_apply)) }
    }) {
        RadioLine(str(R.string.free_age_any), any) { any = true }
        RadioLine(str(R.string.free_age_older), !any) { any = false }
        if (!any) {
            OutlinedTextField(
                amount, { v -> amount = v.filter { it.isDigit() }.take(4) },
                label = { Text(str(R.string.free_amount)) }, isError = n == null, singleLine = true,
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
    PickDialog(str(R.string.free_folders), onDismiss, confirm = { TextButton(onClick = { onDone(chosen) }) { Text(str(R.string.free_apply)) } }) {
        CheckLine(str(R.string.free_all_folders), chosen.isEmpty()) { if (it) chosen = emptySet() }
        if (folders.isEmpty()) Hint(str(R.string.free_no_subfolders))
        Hint(str(R.string.free_folder_includes_sub))
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
    PickDialog(str(R.string.free_types), onDismiss, confirm = {
        TextButton(onClick = { onDone(kinds, if (allowExtensions) FreeRules.parseExtensions(extText) else emptySet()) }) {
            Text(str(R.string.free_apply))
        }
    }) {
        CheckLine(str(R.string.free_all_types), kinds.isEmpty() && extText.isBlank()) { if (it) { kinds = emptySet(); extText = "" } }
        for (k in offered) CheckLine(k.label, k in kinds) { on -> kinds = if (on) kinds + k else kinds - k }
        if (allowExtensions) {
            OutlinedTextField(
                extText, { extText = it }, label = { Text(str(R.string.free_own_extensions)) },
                placeholder = { Text(str(R.string.free_own_extensions_example)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Hint(str(R.string.free_other_hint, FileKind.OTHER.label))
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
    PickDialog(str(R.string.free_min_size), onDismiss, confirm = {
        TextButton(enabled = !custom || customMb != null, onClick = { onDone(if (custom) customMb!! * mb else value) }) {
            Text(str(R.string.free_apply))
        }
    }) {
        presets.forEach { p ->
            RadioLine(if (p == 0L) str(R.string.free_no_minimum) else Notifications.formatBytes(p), !custom && value == p) { custom = false; value = p }
        }
        RadioLine(str(R.string.free_custom_value), custom) { custom = true }
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
    PickDialog(str(R.string.free_exclude), onDismiss, confirm = {
        TextButton(onClick = { onDone(com.sync2pc.app.mirror.FileFilter.split(text)) }) { Text(str(R.string.free_apply)) }
    }) {
        Hint(str(R.string.free_exclude_hint))
        Hint(str(R.string.free_exclude_examples))
        OutlinedTextField(
            text, { text = it }, label = { Text(str(R.string.free_patterns)) }, minLines = 4,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
