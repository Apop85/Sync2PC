package com.sync2pc.app.ui

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.sync.Notifications
import com.sync2pc.app.sync.SpaceFreer
import kotlinx.coroutines.launch

/** Alte, nachweislich gesicherte Fotos/Videos in den Android-Papierkorb verschieben (0.2.0). */
@Composable
fun FreeSpaceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s = remember { GlobalSettings(ctx) }
    val scope = rememberCoroutineScope()
    val hasTrash = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    var monthsText by remember { mutableStateOf(s.freeSpaceMonths.toString()) }
    var keepFav by remember { mutableStateOf(s.freeSpaceKeepFavorites) }
    var scan by remember { mutableStateOf<SpaceFreer.ScanResult?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var verified by remember { mutableStateOf<SpaceFreer.VerifyResult?>(null) }
    var confirmDirect by remember { mutableStateOf(false) }

    // Systemdialoge nacheinander (je max. 500 Dateien)
    var requests by remember { mutableStateOf<List<Pair<android.app.PendingIntent, Int>>>(emptyList()) }
    var moved by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val current = requests.firstOrNull()
        if (res.resultCode == Activity.RESULT_OK && current != null) {
            moved += current.second
            requests = requests.drop(1)
            if (requests.isEmpty()) {
                Toast.makeText(ctx, "$moved Dateien in den Papierkorb verschoben", Toast.LENGTH_LONG).show()
                scan = null; verified = null
            }
        } else {
            requests = emptyList()
            if (moved > 0) Toast.makeText(ctx, "$moved Dateien in den Papierkorb verschoben", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(requests) {
        requests.firstOrNull()?.let { launcher.launch(IntentSenderRequest.Builder(it.first.intentSender).build()) }
    }

    fun months(): Int? = monthsText.toIntOrNull()?.takeIf { it in 1..240 }

    fun search() {
        val m = months() ?: return
        s.freeSpaceMonths = m; s.freeSpaceKeepFavorites = keepFav
        busy = "Suche…"; scan = null; verified = null
        scope.launch {
            try { scan = SpaceFreer.scan(ctx, m, keepFav && hasTrash) }
            catch (e: Exception) { Toast.makeText(ctx, "Suche fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show() }
            finally { busy = null }
        }
    }

    fun verifyAndTrash() {
        val list = scan?.candidates ?: return
        busy = "Prüfe auf dem PC…"; progress = 0 to list.size
        scope.launch {
            try {
                val v = SpaceFreer.verify(ctx, list) { done, total -> progress = done to total }
                verified = v
                if (v.confirmed.isEmpty()) return@launch
                if (hasTrash) { moved = 0; requests = SpaceFreer.trashRequests(ctx, v.confirmed) }
                else confirmDirect = true
            } catch (e: Exception) {
                Toast.makeText(ctx, "Prüfung fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                busy = null; progress = null
            }
        }
    }

    ScreenScaffold(title = "Speicher freigeben", onBack = onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Hint("Verschiebt Fotos und Videos, die nachweislich auf dem PC gesichert sind, in den Papierkorb des Handys. " +
                        "Vor dem Verschieben prüft LeoSync jede Datei auf dem PC. Was dort fehlt oder anders ist, bleibt auf dem Handy.")
                    OutlinedTextField(
                        monthsText,
                        { v -> monthsText = v.filter { it.isDigit() }.take(3); scan = null; verified = null },
                        label = { Text("Älter als … Monate") },
                        isError = months() == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    if (hasTrash) SwitchRow("Favoriten behalten", keepFav,
                        subtitle = "In der Galerie als Favorit markierte Fotos bleiben auf dem Handy.") { keepFav = it; scan = null; verified = null }
                    else Hint("Favoriten kann Android erst ab Version 11 melden.")
                    Button(enabled = busy == null && months() != null, onClick = { search() }) { Text("Suchen") }
                }
            }
            scan?.let { r ->
                item {
                    SectionCard("Ergebnis") {
                        if (r.candidates.isEmpty()) Text("Keine passenden, gesicherten Dateien gefunden.")
                        else Text("${r.candidates.size} Dateien · ${Notifications.formatBytes(r.bytes)}",
                            style = MaterialTheme.typography.titleMedium)
                        if (r.notBackedUp > 0) Hint("${r.notBackedUp} ältere Dateien sind (noch) nicht gesichert und bleiben.")
                        if (r.favoritesKept > 0) Hint("${r.favoritesKept} Favoriten bleiben.")
                        if (r.candidates.isNotEmpty()) OutlinedButton(enabled = busy == null, onClick = { verifyAndTrash() }) {
                            Text(if (hasTrash) "Auf dem PC prüfen und in den Papierkorb" else "Auf dem PC prüfen und löschen")
                        }
                        if (hasTrash) Hint("Im Papierkorb bleiben die Dateien 30 Tage wiederherstellbar; der Speicher wird danach frei.")
                    }
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
            verified?.let { v ->
                item {
                    SectionCard("Prüfung auf dem PC") {
                        Text("${v.confirmed.size} Dateien bestätigt")
                        if (v.missing > 0) Hint("${v.missing} Dateien wurden auf dem PC nicht gefunden oder sind anders – sie bleiben auf dem Handy.")
                        if (v.unreachable.isNotEmpty()) Text("PC nicht erreichbar: ${v.unreachable.joinToString(", ")}",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDirect) {
        val list = verified?.confirmed.orEmpty()
        AlertDialog(
            onDismissRequest = { confirmDirect = false },
            title = { Text("Endgültig löschen?") },
            text = { Text("Android 10 hat keinen Papierkorb. ${list.size} Dateien werden endgültig vom Handy gelöscht. Sie sind auf dem PC vorhanden.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDirect = false
                    scope.launch {
                        val n = SpaceFreer.deleteDirectly(ctx, list)
                        Toast.makeText(ctx, "$n Dateien gelöscht", Toast.LENGTH_LONG).show()
                        scan = null; verified = null
                    }
                }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { confirmDirect = false }) { Text("Abbrechen") } },
        )
    }
}
