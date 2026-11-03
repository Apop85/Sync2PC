package com.sync2pc.app.ui

import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * Vorauswahl: vorhandener Ordner, der den Ländernamen enthält, sonst der erste passende Ordner.
 * "Alle übernehmen" bestätigt alle Vorauswahlen mit einem Klick.
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

    fun key(d: PendingDecision) = "${d.profileId}|${d.groupKey}"
    fun candidates(d: PendingDecision) = Profile.splitLines(d.candidates)
    fun preselect(d: PendingDecision): String {
        val c = candidates(d)
        return d.countryName?.let { n -> c.firstOrNull { it.contains(n, ignoreCase = true) } }
            ?: c.firstOrNull() ?: d.defaultName
    }
    fun selectedFolder(d: PendingDecision): String? {
        val sel = choice[key(d)] ?: preselect(d)
        if (sel != CUSTOM) return sel
        val name = custom[key(d)]?.trim().orEmpty()
        return if (name.isNotEmpty() && FolderNames.monthOf(name) == d.month) name else null
    }

    fun apply(list: List<PendingDecision>) {
        scope.launch {
            var applied = 0
            withContext(Dispatchers.IO) {
                for (d in list) {
                    val folder = selectedFolder(d) ?: continue
                    dao.upsertMapping(TargetResolver.mapping(d.profileId, d.groupKey, folder))
                    dao.deleteDecision(d.profileId, d.groupKey)
                    applied++
                }
            }
            if (applied > 0) {
                androidx.core.app.NotificationManagerCompat.from(ctx).cancel(Notifications.ID_DECISION)
                SyncScheduler.enqueueAuto(ctx, str(R.string.trigger_folder_choice))
            }
        }
    }

    ScreenScaffold(
        title = str(R.string.decision_title),
        onBack = onBack,
        actions = {
            if (decisions.isNotEmpty()) TextButton(onClick = { apply(decisions) }) { Text(str(R.string.decision_apply_all)) }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (decisions.isEmpty()) item { Text(str(R.string.decision_none)) }
            else item { Hint(str(R.string.decision_hint)) }
            items(decisions, key = { key(it) }) { d ->
                val k = key(d)
                val sel = choice[k] ?: preselect(d)
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
                        val txt = custom[k] ?: "%02d - ".format(d.month)
                        OutlinedTextField(
                            txt, { custom[k] = it },
                            label = { Text(str(R.string.decision_folder_name, "%02d".format(d.month))) },
                            isError = FolderNames.monthOf(txt) != d.month,
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(onClick = { apply(listOf(d)) }, enabled = selectedFolder(d) != null) { Text(str(R.string.free_apply)) }
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
