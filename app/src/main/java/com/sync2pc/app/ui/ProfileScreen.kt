package com.sync2pc.app.ui

import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.CredentialCrypto
import com.sync2pc.app.data.DeleteMode
import com.sync2pc.app.data.Profile
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.smb.SmbTarget
import com.sync2pc.app.sync.SyncScheduler
import com.sync2pc.app.sync.WifiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(profileId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(profileId == 0L) }
    var p by remember { mutableStateOf(Profile(name = "Fotos")) }
    var password by remember { mutableStateOf("") }
    var newSsid by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    // Android-Systemdialog zur Ordnerauswahl. Die Auswahl wird in einen MediaStore-Pfad
    // umgerechnet (z. B. "DCIM/Camera/"); gelesen wird weiterhin schnell über den MediaStore.
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val rel = treeUriToRelativePath(uri)
        if (rel == null) {
            Toast.makeText(ctx, "Dieser Ordner kann nicht verwendet werden. Bitte einen Unterordner wählen.", Toast.LENGTH_LONG).show()
        } else {
            p = p.copy(sourceFolders = Profile.joinLines(p.folders() + rel))
        }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val currentSsid = remember { WifiHelper.currentSsid(ctx) }

    LaunchedEffect(profileId) {
        if (profileId != 0L) {
            withContext(Dispatchers.IO) {
                dao.profile(profileId)?.let {
                    p = it
                    password = CredentialCrypto.decrypt(it.smbPasswordEnc)
                }
            }
            loaded = true
        }
    }

    fun withPassword(): Profile = p.copy(smbPasswordEnc = CredentialCrypto.encrypt(password))

    fun save() {
        scope.launch {
            val toSave = withPassword()
            withContext(Dispatchers.IO) {
                if (toSave.id == 0L) dao.insertProfile(toSave) else dao.updateProfile(toSave)
            }
            SyncScheduler.setupAll(ctx)
            Toast.makeText(ctx, "Gespeichert", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    ScreenScaffold(
        title = if (profileId == 0L) "Neues Profil" else "Profil bearbeiten",
        onBack = onBack,
        actions = {
            if (profileId != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Profil löschen") }
        },
    ) { pad ->
        if (!loaded) {
            Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            return@ScreenScaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard("Allgemein") {
                    OutlinedTextField(p.name, { p = p.copy(name = it) }, label = { Text("Profilname") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    SwitchRow("Profil aktiv", p.enabled) { p = p.copy(enabled = it) }
                }
            }

            item {
                SectionCard("Ziel (Windows-Freigabe)") {
                    OutlinedTextField(
                        p.smbPath, { p = p.copy(smbPath = it) },
                        label = { Text("Freigabe-Pfad") },
                        placeholder = { Text("\\\\fs01.local\\Fotos") },
                        isError = p.smbPath.isNotBlank() && SmbTarget.parse(p.smbPath) == null,
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Hint("Format: \\\\Server\\Freigabe oder \\\\Server\\Freigabe\\Unterordner")
                    OutlinedTextField(p.smbUser, { p = p.copy(smbUser = it) }, label = { Text("Benutzer") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        password, { password = it }, label = { Text("Passwort") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(p.smbDomain, { p = p.copy(smbDomain = it) },
                        label = { Text("Domäne / PC-Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Hint("Bei einem lokalen Windows-Konto meist leer lassen. Microsoft-Konto: E-Mail-Adresse als Benutzer.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(enabled = !testing, onClick = {
                            testing = true; testResult = null
                            scope.launch {
                                val prof = withPassword()
                                testResult = withContext(Dispatchers.IO) {
                                    try { "✓ " + SmbSession.test(prof) } catch (e: Exception) {
                                        "✗ ${e.javaClass.simpleName}: ${e.message}"
                                    }
                                }
                                testing = false
                            }
                        }) { Text("Verbindung testen") }
                        if (testing) CircularProgressIndicator(Modifier.padding(start = 12.dp))
                    }
                    testResult?.let { Text(it) }
                }
            }

            item {
                SectionCard("WLAN (automatischer Sync)") {
                    Hint("Sobald das Telefon mit einem dieser WLANs verbunden ist, wird synchronisiert.")
                    p.ssids().forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s, Modifier.weight(1f))
                            IconButton(onClick = { p = p.copy(wifiSsids = Profile.joinLines(p.ssids() - s)) }) {
                                Icon(Icons.Filled.Close, "Entfernen")
                            }
                        }
                    }
                    if (currentSsid != null && !p.ssids().contains(currentSsid)) {
                        OutlinedButton(onClick = { p = p.copy(wifiSsids = Profile.joinLines(p.ssids() + currentSsid)) }) {
                            Text("Aktuelles WLAN „$currentSsid“ hinzufügen")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newSsid, { newSsid = it }, label = { Text("WLAN-Name") },
                            singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(enabled = newSsid.isNotBlank(), onClick = {
                            p = p.copy(wifiSsids = Profile.joinLines(p.ssids() + newSsid.trim())); newSsid = ""
                        }) { Text("Hinzufügen") }
                    }
                }
            }

            item {
                SectionCard("Quellordner auf dem Telefon") {
                    if (p.folders().isEmpty()) Hint("Noch kein Ordner gewählt (z. B. DCIM/Camera).")
                    p.folders().forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f, Modifier.weight(1f))
                            IconButton(onClick = { p = p.copy(sourceFolders = Profile.joinLines(p.folders() - f)) }) {
                                Icon(Icons.Filled.Close, "Entfernen")
                            }
                        }
                    }
                    OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Ordner hinzufügen…") }
                    Hint("Öffnet die Android-Dateiauswahl. Ordner wählen und „Diesen Ordner verwenden“ tippen.")
                    SwitchRow("Unterordner einbeziehen", p.includeSubfolders) { p = p.copy(includeSubfolders = it) }
                    SwitchRow("Fotos", p.includeImages) { p = p.copy(includeImages = it) }
                    SwitchRow("Videos", p.includeVideos) { p = p.copy(includeVideos = it) }
                }
            }

            item {
                SectionCard("Nach erfolgreicher Übertragung") {
                    DeleteMode.entries.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clickable { p = p.copy(deleteMode = m.name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = p.delete() == m, onClick = { p = p.copy(deleteMode = m.name) })
                            Text(m.label)
                        }
                    }
                    if (p.delete() != DeleteMode.KEEP) Hint(
                        "Dateien werden nur entfernt, wenn der Upload geprüft wurde (Grösse + Prüfsumme). " +
                            "Android fragt dabei einmal pro Durchgang nach Bestätigung."
                    )
                    SwitchRow(
                        "Upload prüfen",
                        checked = p.mustVerify(),
                        subtitle = if (p.delete() != DeleteMode.KEEP) "Pflicht, weil Dateien entfernt werden"
                        else "Liest 192 KB der Zieldatei zurück und vergleicht die Prüfsumme",
                        enabled = p.delete() == DeleteMode.KEEP,
                    ) { p = p.copy(verifyUpload = it) }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { save() }, enabled = p.name.isNotBlank()) { Text("Speichern") }
                    OutlinedButton(onClick = onBack) { Text("Abbrechen") }
                }
            }

            if (profileId != 0L) item {
                SectionCard("Wartung") {
                    Hint("Vergisst, welche Dateien bereits übertragen wurden. Beim nächsten Sync wird alles mit dem Share " +
                        "abgeglichen (identische Dateien werden nicht erneut übertragen).")
                    OutlinedButton(onClick = { confirmReset = true }) { Text("Sync-Status zurücksetzen") }
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Profil löschen?") },
        text = { Text("Einstellungen und Sync-Status dieses Profils werden gelöscht. Dateien auf dem PC bleiben erhalten.") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dao.clearSynced(profileId); dao.clearMappings(profileId)
                        dao.clearDecisions(profileId); dao.deleteProfileRow(profileId)
                    }
                    confirmDelete = false; onBack()
                }
            }) { Text("Löschen") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
    )

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Sync-Status zurücksetzen?") },
        text = { Text("Der nächste Sync gleicht alle Dateien erneut mit dem Share ab. Das kann bei vielen Dateien länger dauern.") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) { dao.clearSynced(profileId) }
                    confirmReset = false
                    Toast.makeText(ctx, "Zurückgesetzt", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Zurücksetzen") }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") } },
    )
}

/**
 * Wandelt eine Ordner-URI des Systemdialogs in einen MediaStore-Pfad um.
 * content://…/tree/primary%3ADCIM%2FCamera  ->  "DCIM/Camera/"
 * Funktioniert auch für SD-Karten ("1234-ABCD:DCIM"). null = Speicher-Wurzel oder unbekannter Anbieter.
 */
fun treeUriToRelativePath(uri: Uri): String? {
    if (uri.authority != "com.android.externalstorage.documents") return null
    val docId = try { DocumentsContract.getTreeDocumentId(uri) } catch (e: Exception) { return null }
    val path = docId.substringAfter(':', "").trim('/')
    return if (path.isEmpty()) null else "$path/"
}
