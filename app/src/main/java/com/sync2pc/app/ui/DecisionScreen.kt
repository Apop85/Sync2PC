package com.sync2pc.app.ui

import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sync2pc.app.R
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.FolderLayout
import com.sync2pc.app.data.PendingDecision
import com.sync2pc.app.data.Profile
import com.sync2pc.app.sync.FolderNames
import com.sync2pc.app.sync.Notifications
import com.sync2pc.app.sync.SyncScheduler
import com.sync2pc.app.sync.TargetResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CUSTOM = "\u0000custom"

/**
 * Ordnerauswahl: Pro Gruppe (Jahr/Monat/Land) EINE Entscheidung – egal wie viele Dateien.
 * Vorauswahl nur bei genau einem vorhandenen Ordner (sonst muss bewusst gewählt werden, O1).
 * "Alle übernehmen" wirkt nur auf Gruppen mit getroffener Wahl und fragt vorher nach.
 */
@Composable
fun DecisionScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val scope = rememberCoroutineScope()
    val decisions by remember { dao.decisionsFlow() }.collectAsState(initial = emptyList())
    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())
    val choice = remember { mutableStateMapOf<String, String>() }
    val custom = remember { mutableStateMapOf<String, String>() }
    val snackbar = remember { SnackbarHostState() }
    var confirmAll by remember { mutableStateOf(false) }

    fun key(d: PendingDecision) = "${d.profileId}|${d.groupKey}"
    fun candidates(d: PendingDecision) = Profile.splitLines(d.candidates)
    /** Wirksame Auswahl: getroffene Wahl; bei mehreren vorhandenen Ordnern keine Vorauswahl (null) */
    fun currentSel(d: PendingDecision): String? =
        DecisionChoice.effective(candidates(d), d.defaultName, d.countryName, choice[key(d)])
    /** Anzahl Ordnerebenen unter dem Jahr, die die Vorlage des Profils für diese Gruppe verlangt (SYNC-20261109-084) */
    fun segments(d: PendingDecision): Int =
        TargetResolver.decisionSegments(
            profiles.firstOrNull { it.id == d.profileId }?.layout() ?: FolderLayout.MONTH_SUFFIX, d.groupKey)
    /** Wert des Felds "Eigener Name": Eingabe, sonst Vorgabe (V2 mit Land: "<Länderordner>\MM - "); Anzeige und Prüfung nutzen denselben Wert (SYNC-20261109-091) */
    fun customText(d: PendingDecision): String = custom[key(d)]
        ?: (d.defaultName.substringBeforeLast('\\', "").let { if (it.isEmpty()) "" else "$it\\" } + "%02d - ".format(d.month))
    /** Eigene Eingabe als bereinigter relativer Pfad; null = ungültig (Segmentzahl, Monatszahl, leer, "..") */
    fun customPath(d: PendingDecision, text: String): String? =
        FolderNames.validateDecisionPath(text, d.month, segments(d))
    /** Fehlertext zur eigenen Eingabe mit erwarteter Form; null = gültig */
    fun customError(d: PendingDecision, text: String): String? {
        val two = segments(d) == 2
        val month = "%02d".format(d.month)
        return when (FolderNames.decisionPathProblem(text, d.month, segments(d))) {
            null -> null
            FolderNames.DecisionPathProblem.SEGMENTS ->
                if (two) str(R.string.decision_err_two, d.defaultName) else str(R.string.decision_err_one, month)
            FolderNames.DecisionPathProblem.EMPTY_OR_DOTS -> str(R.string.decision_err_empty)
            FolderNames.DecisionPathProblem.MONTH -> str(R.string.decision_err_month, month)
        }
    }
    fun selectedFolder(d: PendingDecision): String? {
        // Vorlage des Profils noch nicht geladen -> Segmentzahl unbekannt, nicht bestätigen (SYNC-20261109-089)
        if (profiles.isEmpty()) return null
        val sel = currentSel(d) ?: return null
        if (sel != CUSTOM) return sel
        return customPath(d, customText(d))
    }

    fun apply(list: List<PendingDecision>) {
        scope.launch {
            val done = ArrayList<Pair<PendingDecision, String>>()
            withContext(Dispatchers.IO) {
                for (d in list) {
                    val folder = selectedFolder(d) ?: continue
                    dao.upsertMapping(TargetResolver.mapping(d.profileId, d.groupKey, folder))
                    dao.deleteDecision(d.profileId, d.groupKey)
                    done += d to folder
                }
            }
            if (done.isNotEmpty()) {
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(Notifications.ID_DECISION)
                SyncScheduler.enqueueAuto(ctx, com.sync2pc.app.l10n.Msg.of(R.string.trigger_folder_choice))
                // O2: Rückmeldung, wohin die Dateien kommen (die Karte verschwindet sonst wortlos)
                val msg = if (done.size == 1) {
                    val (d, folder) = done[0]
                    plural(R.plurals.decision_snack_one, d.fileCount, d.fileCount, "${d.year}\\$folder")
                } else {
                    val files = done.sumOf { it.first.fileCount }
                    plural(R.plurals.decision_snack_all, files, files, plural(R.plurals.decision_n_groups, done.size, done.size))
                }
                snackbar.currentSnackbarData?.dismiss()
                scope.launch { snackbar.showSnackbar(msg, duration = SnackbarDuration.Short) }
            }
        }
    }
    // Gruppen mit getroffener, gültiger Wahl (O1): nur diese wirkt "Alle übernehmen"
    val ready = decisions.filter { selectedFolder(it) != null }
    val readyFiles = ready.sumOf { it.fileCount }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text(str(R.string.decision_apply_all)) },
            text = {
                DialogScroll {
                    Text(plural(R.plurals.decision_all_confirm, ready.size, ready.size, plural(R.plurals.decision_n_files, readyFiles, readyFiles)))
                }
            },
            confirmButton = {
                TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { confirmAll = false; apply(ready) }) {
                    Text(str(R.string.decision_apply_all))
                }
            },
            dismissButton = {
                TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { confirmAll = false }) { Text(str(R.string.common_cancel)) }
            },
        )
    }

    ScreenScaffold(
        title = str(R.string.decision_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
        actions = {
            if (decisions.isNotEmpty()) TextButton(
                modifier = Modifier.heightIn(min = 48.dp),
                enabled = ready.isNotEmpty(),
                onClick = { confirmAll = true },
            ) { Text(str(R.string.decision_apply_all)) }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (decisions.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(str(R.string.decision_none))
                    Hint(str(R.string.decision_none_hint))
                }
            }
            else item { Hint(str(R.string.decision_hint)) }
            items(decisions, key = { key(it) }) { d ->
                val k = key(d)
                val sel = currentSel(d)
                val profileName = profiles.firstOrNull { it.id == d.profileId }?.name
                val country = com.sync2pc.app.geo.CountryLookup.displayName(d.countryIso, d.countryName)
                SectionCard(
                    "${monthNames[d.month - 1]} ${d.year}" + (country?.let { " · $it" } ?: "") +
                        " · " + plural(R.plurals.decision_files, d.fileCount, d.fileCount, Notifications.formatBytes(d.totalBytes))
                ) {
                    if (profiles.size > 1 && profileName != null) Hint(str(R.string.decision_profile, profileName))
                    Thumbnails(Profile.splitLines(d.sampleUris))
                    val options = candidates(d) + listOf(d.defaultName).filter { it !in candidates(d) }
                    options.forEach { o ->
                        Row(
                            Modifier.fillMaxWidth().clickable { choice[k] = o },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = sel == o, onClick = { choice[k] = o })
                            Text(if (o == d.defaultName && o !in candidates(d)) str(R.string.decision_new_folder, "${d.year}\\$o") else "${d.year}\\$o")
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { choice[k] = CUSTOM },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = sel == CUSTOM, onClick = { choice[k] = CUSTOM })
                        Text(str(R.string.decision_custom_name))
                    }
                    if (sel == CUSTOM) {
                        val twoLevels = segments(d) == 2
                        val txt = customText(d)
                        OutlinedTextField(
                            txt, { custom[k] = it },
                            label = {
                                Text(
                                    if (twoLevels) str(R.string.decision_folder_path, d.defaultName, "%02d".format(d.month))
                                    else str(R.string.decision_folder_name, "%02d".format(d.month))
                                )
                            },
                            isError = customError(d, txt) != null,
                            supportingText = customError(d, txt)?.let { e -> { Text(e) } },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (sel == null) Hint(str(R.string.decision_choose_first))
                    Button(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = { apply(listOf(d)) }, enabled = selectedFolder(d) != null,
                    ) { Text(str(R.string.free_apply)) }
                }
            }
        }
    }
}

@Composable
private fun Thumbnails(uris: List<String>) {
    if (uris.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(uris) { u -> Thumb(u) }
    }
}

@Composable
private fun Thumb(uri: String) {
    val ctx = LocalContext.current
    var bmp by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bmp = withContext(Dispatchers.IO) {
            try { ctx.contentResolver.loadThumbnail(Uri.parse(uri), Size(240, 240), null).asImageBitmap() }
            catch (e: Exception) { null }
        }
    }
    Box(
        Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        bmp?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}
