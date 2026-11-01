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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.smb.SmbSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Assistent beim neuen Profil: Was soll synchronisiert werden? */
@Composable
fun ProfileTypeChooser(modifier: Modifier, onChoose: (ProfileType) -> Unit) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Was möchtest du synchronisieren?", style = MaterialTheme.typography.titleLarge)
        TypeCard(
            "📷  Fotos & Videos sichern",
            "Fotos und Videos vom Handy auf den PC. Ablage sortiert nach Jahr, Monat und Reiseland, " +
                "z. B. 2026\\06 - Ägypten. Optional danach vom Handy entfernen.",
        ) { onChoose(ProfileType.PHOTOS) }
        TypeCard(
            "📁  Ordner synchronisieren",
            "Einen beliebigen Ordner 1:1 spiegeln, mit Unterordnern und allen Dateitypen. " +
                "Richtung wählbar: Handy → PC, PC → Handy oder beidseitig.",
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
 * Ordner auf dem Share durchsuchen und als Unterordner wählen.
 * Pfade relativ zum Freigabe-Pfad des Profils, Trenner "\".
 */
@Composable
fun RemoteFolderPicker(
    profile: () -> Profile,
    initial: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var path by remember { mutableStateOf(initial.replace('/', '\\').trim('\\')) }
    var dirs by remember { mutableStateOf<List<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(path) {
        dirs = null; error = null
        val result = withContext(Dispatchers.IO) {
            try {
                SmbSession.open(profile().copy(remoteSubfolder = "")).use { s ->
                    Result.success(s.listDir(s.path(path)).filter { it.isDirectory && !it.name.startsWith(".Sync2PC") }
                        .map { it.name }.sortedBy { it.lowercase() })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        result.onSuccess { dirs = it }.onFailure { error = "${it.javaClass.simpleName}: ${it.message}" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ordner auf dem Share") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\\" + path, style = MaterialTheme.typography.bodySmall)
                val list = dirs
                when {
                    error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    list == null -> CircularProgressIndicator()
                    else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        if (path.isNotEmpty()) item {
                            Row(Modifier.fillMaxWidth().clickable { path = path.substringBeforeLast('\\', "") }.padding(vertical = 10.dp)) {
                                Text("⬆  ..")
                            }
                        }
                        if (list.isEmpty()) item { Hint("Keine Unterordner") }
                        items(list) { d ->
                            Row(Modifier.fillMaxWidth().clickable { path = if (path.isEmpty()) d else "$path\\$d" }.padding(vertical = 10.dp)) {
                                Text("📁  $d")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(path) }, enabled = error == null) { Text("Diesen Ordner verwenden") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
