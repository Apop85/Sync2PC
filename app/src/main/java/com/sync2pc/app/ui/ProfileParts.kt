package com.sync2pc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sync2pc.app.R
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.l10n.str
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.smb.SmbTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Assistent beim neuen Profil: Was soll synchronisiert werden? */
@Composable
fun ProfileTypeChooser(modifier: Modifier, onChoose: (ProfileType) -> Unit) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(str(R.string.type_chooser_title), style = MaterialTheme.typography.titleLarge)
        TypeCard(
            "📷  " + ProfileType.PHOTOS.label,
            str(R.string.type_chooser_photos),
        ) { onChoose(ProfileType.PHOTOS) }
        TypeCard(
            "📁  " + ProfileType.MIRROR.label,
            str(R.string.type_chooser_mirror),
        ) { onChoose(ProfileType.MIRROR) }
    }
}

@Composable
private fun TypeCard(title: String, text: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Ziel auf dem PC wählen (seit 0.11.0): oberste Ebene = Freigaben des PCs, darunter die Ordner.
 * Startet beim bisherigen Pfad, falls einer eingetragen ist. Ergebnis: "\\Server\Freigabe\Ordner…".
 */
@Composable
fun RemoteFolderPicker(
    profile: () -> Profile,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val start = remember { profile() }
    val host = remember { SmbTarget.hostOf(start.smbPath).orEmpty() }
    // Pfad unter dem Server: [] = Freigaben, [Freigabe, Ordner …]
    var parts by remember {
        mutableStateOf(start.smbPath.replace('/', '\\').split('\\').map { it.trim() }.filter { it.isNotEmpty() }.drop(1))
    }
    var entries by remember { mutableStateOf<List<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<String?>(null) }
    // Ordner anlegen (0.11.3, DEC-192)
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }
    var createBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(parts) {
        entries = null; error = null; detail = null
        val result = withContext(Dispatchers.IO) {
            try {
                // Profil-Editor (SETUP): meldet sich an, lernt aber nie (Host der Kopie kann ungespeichert sein); bei BOTH ohne
                // bekannte Kennung nur im eingetragenen WLAN, abweichende Kennung wird hier nur gemeldet
                val netOk = com.sync2pc.app.sync.PcLearner.learnNetOk(ctx, start)
                if (parts.isEmpty()) Result.success(SmbSession.listShares(start, com.sync2pc.app.sync.PcAccess.SETUP, null, netOk))
                else SmbSession.open(start.copy(smbPath = "\\\\$host\\${parts.first()}", remoteSubfolder = ""), com.sync2pc.app.sync.PcAccess.SETUP, learnNetOk = netOk).use { s ->
                    Result.success(s.listDir(parts.drop(1).joinToString("\\"))
                        .filter { it.isDirectory && !it.name.startsWith(".LeoSync") && !it.name.startsWith(".Sync2PC") }
                        .map { it.name }.sortedBy { it.lowercase() })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        result.onSuccess { entries = it }.onFailure {
            error = com.sync2pc.app.sync.userError(it); detail = com.sync2pc.app.sync.shortError(it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(if (parts.isEmpty()) R.string.remote_picker_title_shares else R.string.remote_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\\\\" + (listOf(host) + parts).joinToString("\\"), style = MaterialTheme.typography.bodySmall)
                val list = entries
                when {
                    error != null -> {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        detail?.let { ErrorDetails(it) }
                    }
                    list == null -> CircularProgressIndicator()
                    else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        if (parts.isNotEmpty()) item {
                            Row(Modifier.fillMaxWidth().clickable { parts = parts.dropLast(1) }.padding(vertical = 10.dp)) {
                                Text("⬆  ..")
                            }
                        }
                        // Auf der Ebene der Freigaben kann man keine Freigabe anlegen, nur Ordner darunter
                        if (parts.isNotEmpty()) item {
                            Row(Modifier.fillMaxWidth().clickable {
                                newName = ""; createError = null; creating = true
                            }.padding(vertical = 10.dp)) {
                                Text("➕  " + str(R.string.remote_picker_new_folder))
                            }
                        }
                        if (list.isEmpty()) item {
                            Hint(str(if (parts.isEmpty()) R.string.remote_picker_no_shares else R.string.remote_picker_empty))
                        }
                        items(list) { d ->
                            Row(Modifier.fillMaxWidth().clickable { parts = parts + d }.padding(vertical = 10.dp)) {
                                Text((if (parts.isEmpty()) "🖧  " else "📁  ") + d)
                            }
                        }
                    }
                }
            }
        },
        // Auf der Ebene der Freigaben gibt es noch nichts zu übernehmen
        confirmButton = {
            TextButton(onClick = { onPick("\\\\" + (listOf(host) + parts).joinToString("\\")) },
                enabled = error == null && parts.isNotEmpty()) { Text(str(R.string.remote_picker_use)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str(R.string.common_cancel)) } },
    )

    if (creating) {
        val close = { if (!createBusy) creating = false }
        AlertDialog(
            onDismissRequest = close,
            title = { Text(str(R.string.remote_picker_new_folder)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        newName, { newName = it; createError = null },
                        label = { Text(str(R.string.remote_picker_new_folder_name)) },
                        singleLine = true, enabled = !createBusy, modifier = Modifier.fillMaxWidth(),
                    )
                    createError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(enabled = !createBusy && newName.isNotBlank(), onClick = {
                    val name = newName.trim()
                    val bad = invalidFolderName(name)
                    when {
                        bad -> createError = str(R.string.remote_picker_name_invalid)
                        entries.orEmpty().any { it.equals(name, ignoreCase = true) } ->
                            createError = str(R.string.remote_picker_name_exists, name)
                        else -> {
                            createBusy = true
                            val share = parts.first()
                            val full = (parts.drop(1) + name).joinToString("\\")
                            scope.launch {
                                val r = withContext(Dispatchers.IO) {
                                    try {
                                        SmbSession.open(start.copy(smbPath = "\\\\$host\\$share", remoteSubfolder = ""), com.sync2pc.app.sync.PcAccess.SETUP,
                                            learnNetOk = com.sync2pc.app.sync.PcLearner.learnNetOk(ctx, start)).use { s ->
                                            s.ensureDir(full)
                                            // Nur als Erfolg werten, wenn der Ordner wirklich da ist
                                            if (!s.dirExists(full)) throw java.io.IOException(str(R.string.remote_picker_create_failed))
                                        }
                                        Result.success(Unit)
                                    } catch (e: Exception) { Result.failure(e) }
                                }
                                createBusy = false
                                r.onSuccess { creating = false; parts = parts + name }
                                    .onFailure { createError = com.sync2pc.app.sync.userError(it) }
                            }
                        }
                    }
                }) { Text(str(R.string.remote_picker_create)) }
            },
            dismissButton = { TextButton(enabled = !createBusy, onClick = close) { Text(str(R.string.common_cancel)) } },
        )
    }
}

/** Windows-Ordnername ungültig? (leer, Sonderzeichen, Punkt/Leerzeichen am Ende, "." und "..", zu lang) */
internal fun invalidFolderName(name: String): Boolean =
    name.isEmpty() || name.length > 255 || name == "." || name == ".." ||
        name.any { it in "\\/:*?\"<>|" || it.code < 32 } || name.endsWith(".") || name.endsWith(" ")
