@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.collectAsState
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
import com.sync2pc.app.data.ConflictMode
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileOverlap
import com.sync2pc.app.data.ProfileWizard
import com.sync2pc.app.data.ProfileWizard.Step
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.saveable.rememberSaveable
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncDirection
import com.sync2pc.app.mirror.FolderCheck
import com.sync2pc.app.mirror.GuardStore
import com.sync2pc.app.mirror.LocalTree
import com.sync2pc.app.mirror.MirrorGuard
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.smb.SmbSession
import com.sync2pc.app.smb.SmbTarget
import com.sync2pc.app.sync.SyncScheduler
import com.sync2pc.app.sync.WifiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(profileId: Long, onBack: () -> Unit, onSimulate: (Long) -> Unit = {}) {
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
    // Ordner-Spiegelung: Handy-Ordner mit dauerhafter Lese-/Schreibberechtigung (für alle Dateitypen)
    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.takePersistableUriPermission(uri, LocalTree.FLAGS)
            p = p.copy(localTreeUri = uri.toString())
        } catch (e: SecurityException) {
            Toast.makeText(ctx, "Für diesen Ordner wurde kein dauerhafter Zugriff erteilt.", Toast.LENGTH_LONG).show()
        }
    }
    /** Neues Profil: zuerst Typ wählen (Assistent) */
    var typeChosen by remember { mutableStateOf(profileId != 0L) }
    var showRemotePicker by remember { mutableStateOf(false) }
    var pollText by remember { mutableStateOf("") }
    var trashText by remember { mutableStateOf("") }
    var guardText by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var overlap by remember { mutableStateOf<ProfileOverlap.Hit?>(null) }
    // Ordnerwechsel (0.4.1): Stichprobe läuft / passt nicht
    var checkingFolder by remember { mutableStateOf(false) }
    var folderWarning by remember { mutableStateOf<FolderWarning?>(null) }
    // Stand beim Öffnen -> ungespeicherte Änderungen erkennen (0.3.1)
    var baseline by remember { mutableStateOf<Profile?>(null) }
    var basePassword by remember { mutableStateOf("") }
    var askLeave by remember { mutableStateOf(false) }
    /** Gerade neu angelegtes Profil -> Dialog "Wie weiter?" */
    var createdId by remember { mutableStateOf<Long?>(null) }
    var chargeWarnFor by remember { mutableStateOf<Long?>(null) }
    val currentSsid = remember { WifiHelper.currentSsid(ctx) }
    // Andere Profile mit Zugangsdaten -> zum Übernehmen anbieten
    val allProfiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())
    val credentialSources = allProfiles.filter { it.id != profileId && it.smbUser.isNotBlank() }

    LaunchedEffect(profileId) {
        if (profileId != 0L) {
            withContext(Dispatchers.IO) {
                dao.profile(profileId)?.let {
                    p = it
                    password = CredentialCrypto.decrypt(it.smbPasswordEnc)
                    baseline = it
                    basePassword = password
                    pollText = it.remotePollMinutes.toString()
                    trashText = it.trashRetentionDays.toString()
                    guardText = it.massDeletePercent.toString()
                }
            }
            loaded = true
        }
    }

    fun withPassword(): Profile = p.copy(smbPasswordEnc = CredentialCrypto.encrypt(password))

    /**
     * Speichern. [force] = Überschneidung bestätigt. [folderChoice] = Antwort auf "Ist das wirklich der richtige
     * Ordner?" (null = Stichprobe noch nicht gemacht).
     */
    fun save(force: Boolean = false, folderChoice: FolderChoice? = null) {
        scope.launch {
            val toSave = withPassword()
            // Doppelte Profile verhindern bzw. bei Überschneidung warnen
            val hit = withContext(Dispatchers.IO) { ProfileOverlap.check(toSave, dao.profiles()) }
            if (hit != null && (hit.kind == ProfileOverlap.Kind.IDENTICAL || !force)) {
                overlap = hit
                return@launch
            }
            val isNew = toSave.id == 0L
            // Ordner-Profil mit gewechseltem Ordner (0.4.1): Stichprobe, ob der neue Ordner zum Sync-Stand passt
            val base = baseline
            val changedSides: Set<Side> = if (isNew || base == null || toSave.type() != ProfileType.MIRROR) emptySet() else buildSet {
                if (toSave.localTreeUri != base.localTreeUri) add(Side.LOCAL)
                if (ProfileOverlap.target(toSave) != ProfileOverlap.target(base)) add(Side.REMOTE)
            }
            var checked = false
            if (changedSides.isNotEmpty() && folderChoice == null) {
                checkingFolder = true
                val result = FolderCheck.check(ctx, toSave, changedSides)
                checkingFolder = false
                when (result) {
                    FolderCheck.Result.MISMATCH -> { folderWarning = FolderWarning(changedSides, force); return@launch }
                    FolderCheck.Result.OK -> checked = true
                    FolderCheck.Result.UNAVAILABLE -> {} // prüft der nächste Lauf
                }
            }
            if (folderChoice == FolderChoice.ACCEPT) checked = true
            val savedId = withContext(Dispatchers.IO) {
                val id = if (isNew) dao.insertProfile(toSave) else { dao.updateProfile(toSave); toSave.id }
                if (changedSides.isNotEmpty()) MirrorGuard.afterFolderChange(
                    ctx, id, changedSides, resetState = folderChoice == FolderChoice.RESET, checked = checked,
                )
                // Neues Profil: erst nach dem ersten manuellen Start automatisch (VOR setupAll, sonst startet
                // der WLAN-Auslöser sofort einen Sync)
                if (isNew) com.sync2pc.app.data.GlobalSettings(ctx).setAwaitingFirstStart(id, true)
                SyncScheduler.updateRemotePoll(ctx, dao.profiles())
                id
            }
            SyncScheduler.setupAll(ctx)
            Toast.makeText(ctx, "Gespeichert", Toast.LENGTH_SHORT).show()
            if (isNew) {
                // Stand gilt jetzt als gespeichert (keine Nachfrage beim Verlassen)
                p = toSave.copy(id = savedId); baseline = p; basePassword = password
                createdId = savedId
            } else onBack()
        }
    }

    // Ungespeicherte Änderungen? Dann beim Verlassen nachfragen (Zurück-Pfeil, Zurück-Geste, "Abbrechen")
    val dirty = typeChosen && baseline != null && (p != baseline || password != basePassword)
    fun leave() { if (dirty) askLeave = true else onBack() }
    BackHandler(enabled = dirty) { askLeave = true }

    // Profil-Assistent (0.5.0): neues Profil Schritt für Schritt
    val wizard = profileId == 0L
    var step by rememberSaveable { mutableStateOf(Step.NAME) }
    var showMissing by remember { mutableStateOf(false) }
    val steps = ProfileWizard.steps(p)
    val stepIndex = steps.indexOf(step).coerceAtLeast(0)
    // Zurück-Geste im Assistenten: einen Schritt zurück (später registriert -> hat Vorrang)
    BackHandler(enabled = wizard && typeChosen && stepIndex > 0) { showMissing = false; step = steps[stepIndex - 1] }

    ScreenScaffold(
        title = if (profileId == 0L) "Neues Profil" else "Profil bearbeiten",
        onBack = { if (wizard && typeChosen && stepIndex > 0) { showMissing = false; step = steps[stepIndex - 1] } else leave() },
        actions = {
            // Im Assistenten erst ab den optionalen Schritten speichern (vorher fehlen Pflichtangaben)
            if (loaded && typeChosen && (!wizard || step.optional)) IconButton(onClick = { save() }, enabled = p.name.isNotBlank()) {
                Icon(Icons.Filled.Check, "Speichern")
            }
            if (profileId != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Profil löschen") }
        },
    ) { pad ->
        if (!loaded) {
            Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            return@ScreenScaffold
        }
        if (!typeChosen) {
            ProfileTypeChooser(Modifier.padding(pad)) { type ->
                p = p.copy(
                    profileType = type.name,
                    name = if (type == ProfileType.MIRROR) "Ordner" else "Fotos",
                    direction = if (type == ProfileType.MIRROR) SyncDirection.BOTH.name else SyncDirection.UPLOAD.name,
                )
                pollText = p.remotePollMinutes.toString()
                trashText = p.trashRetentionDays.toString()
                guardText = p.massDeletePercent.toString()
                baseline = p
                basePassword = password
                typeChosen = true
            }
            return@ScreenScaffold
        }
        val isMirror = p.type() == ProfileType.MIRROR
        // Assistent (0.5.0): nur der aktuelle Schritt; beim Bearbeiten alle Karten in derselben Reihenfolge
        fun show(s: Step) = !wizard || step == s
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (wizard) item {
                Column {
                    LinearProgressIndicator(
                        progress = { (stepIndex + 1f) / steps.size }, modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Schritt ${stepIndex + 1} von ${steps.size}: ${step.title}" + if (step.optional) " (optional)" else "",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(stepIntro(step, isMirror), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (show(Step.NAME)) item {
                SectionCard("Allgemein") {
                    OutlinedTextField(p.name, { p = p.copy(name = it) }, label = { Text("Profilname") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    SwitchRow("Profil aktiv", p.enabled) { p = p.copy(enabled = it) }
                    Hint("Typ: ${p.type().label}" +
                        if (isMirror) " – 1:1-Spiegelung mit Unterordnern" else " – sortiert nach Jahr\\Monat - Land")
                }
            }

            if (isMirror && show(Step.SOURCE)) item {
                SectionCard("Ordner auf dem Handy") {
                    val treeUri = p.localTreeUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    if (treeUri == null) Hint("Noch kein Ordner gewählt.")
                    else {
                        Text(LocalTree.label(treeUri))
                        if (!LocalTree.hasPermission(ctx, treeUri))
                            Text("Zugriff verloren – bitte erneut wählen.", color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedButton(onClick = { treePicker.launch(null) }) {
                        Text(if (treeUri == null) "Ordner wählen…" else "Anderen Ordner wählen…")
                    }
                    Hint("Alle Dateien in diesem Ordner und seinen Unterordnern werden mit dem PC abgeglichen. " +
                        "Android fragt einmalig, ob LeoSync auf den Ordner zugreifen darf (nötig für alle Dateitypen).")
                }
            }

            if (!isMirror && show(Step.SOURCE)) item {
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
                    Hint("Öffnet die Android-Dateiauswahl: Ordner wählen und „Diesen Ordner verwenden“ tippen. " +
                        "Kamera-Fotos liegen meist in „DCIM/Camera“, Bildschirmfotos in „Pictures/Screenshots“.")
                    SwitchRow("Unterordner einbeziehen", p.includeSubfolders,
                        subtitle = "Auch Fotos in Ordnern innerhalb der gewählten Ordner sichern") { p = p.copy(includeSubfolders = it) }
                    SwitchRow("Fotos", p.includeImages) { p = p.copy(includeImages = it) }
                    SwitchRow("Videos", p.includeVideos) { p = p.copy(includeVideos = it) }
                }
            }

            if (show(Step.TARGET)) item {
                SectionCard("Ziel (Windows-Freigabe)") {
                    if (credentialSources.isNotEmpty()) {
                        Hint("Zugangsdaten von einem anderen Profil übernehmen:")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(credentialSources, key = { it.id }) { src ->
                                AssistChip(
                                    onClick = {
                                        p = p.copy(
                                            smbUser = src.smbUser,
                                            smbDomain = src.smbDomain,
                                            // Server/Freigabe nur vorschlagen, wenn noch leer
                                            smbPath = p.smbPath.ifBlank { src.smbPath },
                                        )
                                        password = CredentialCrypto.decrypt(src.smbPasswordEnc)
                                        testResult = null
                                        Toast.makeText(ctx, "Zugangsdaten von „${src.name}“ übernommen", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("${src.name} (${src.smbUser})") },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        p.smbPath, { p = p.copy(smbPath = it) },
                        label = { Text("Freigabe-Pfad") },
                        placeholder = { Text("\\\\192.168.1.20\\Fotos") },
                        isError = p.smbPath.isNotBlank() && SmbTarget.parse(p.smbPath) == null,
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Hint("Format: \\\\Server\\Freigabe (IP-Adresse oder Name)")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            p.remoteSubfolder, { p = p.copy(remoteSubfolder = it) },
                            label = { Text("Unterordner auf dem Share (optional)") },
                            placeholder = { Text(if (isMirror) "Dokumente\\Handy" else "Handy\\Fotos") },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            enabled = SmbTarget.parse(p.smbPath) != null,
                            onClick = { showRemotePicker = true },
                        ) { Text("Durchsuchen") }
                    }
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

            if (show(Step.OPTIONS)) item {
                SectionCard("WLAN (automatischer Sync)") {
                    Hint("Optional. Ist das Telefon mit einem dieser WLANs verbunden, synchronisiert LeoSync automatisch " +
                        "im Hintergrund. Ohne WLAN-Namen synchronisiert dieses Profil nur, wenn du „Synchronisieren“ tippst " +
                        "(z. B. wenn du den PC über VPN erreichst).")
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

            if (isMirror && show(Step.OPTIONS)) item {
                SectionCard("Richtung und Änderungen") {
                    Hint("In welche Richtung wird abgeglichen?")
                    SyncDirection.entries.forEach { d ->
                        Row(
                            Modifier.fillMaxWidth().clickable { p = p.copy(direction = d.name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = p.dir() == d, onClick = { p = p.copy(direction = d.name) })
                            Text(d.label)
                        }
                    }
                    SwitchRow(
                        "Löschungen übernehmen", p.propagateDeletes,
                        subtitle = "Wird eine Datei auf einer Seite gelöscht, kommt sie auf der anderen Seite in den " +
                            "Ordner „.LeoSync-Papierkorb“. Aus: gelöschte Dateien werden wiederhergestellt.",
                    ) { p = p.copy(propagateDeletes = it) }
                    SwitchRow(
                        "Alte Versionen im Papierkorb behalten", p.keepVersions,
                        subtitle = "Wird eine Datei überschrieben, kommt die alte Version in den Ordner „.LeoSync-Papierkorb“ " +
                            "und lässt sich über „Wiederherstellen“ zurückholen. Braucht etwas mehr Speicher.",
                    ) { p = p.copy(keepVersions = it) }
                    OutlinedTextField(
                        trashText,
                        { v ->
                            trashText = v.filter { it.isDigit() }.take(4)
                            trashText.toIntOrNull()?.let { p = p.copy(trashRetentionDays = it.coerceIn(0, 3650)) }
                        },
                        label = { Text("Papierkorb leeren nach … Tagen") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Hint("Ältere Einträge im „.LeoSync-Papierkorb“ werden auf Handy und PC endgültig gelöscht. 0 = nie. Standard: 30")
                    if (p.dir() == SyncDirection.BOTH) {
                        Text("Wenn eine Datei auf beiden Seiten geändert wurde:", style = MaterialTheme.typography.bodyMedium)
                        ConflictMode.entries.forEach { m ->
                            Row(
                                Modifier.fillMaxWidth().clickable { p = p.copy(conflictMode = m.name) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = p.conflict() == m, onClick = { p = p.copy(conflictMode = m.name) })
                                Text(m.label)
                            }
                        }
                        if (p.conflict() == ConflictMode.NEWER) Hint("Die ältere Version wird in den Papierkorb verschoben, nicht gelöscht.")
                    }
                    OutlinedTextField(
                        pollText,
                        { v ->
                            pollText = v.filter { it.isDigit() }.take(4)
                            pollText.toIntOrNull()?.let { p = p.copy(remotePollMinutes = it.coerceIn(15, 24 * 60)) }
                        },
                        label = { Text("Auf Änderungen prüfen alle … Minuten") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Hint("Nur solange das Telefon im Ziel-WLAN ist (mindestens 15). Zusätzlich sofort beim Verbinden mit dem WLAN.")
                }
            }

            if (!isMirror && show(Step.OPTIONS)) item {
                SectionCard("Nach erfolgreicher Übertragung") {
                    Hint("Was passiert mit den Fotos auf dem Telefon, wenn sie sicher auf dem PC liegen?")
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

            if (show(Step.FILTER)) item {
                SectionCard("Filter (optional)") {
                    OutlinedTextField(
                        p.includePatterns, { p = p.copy(includePatterns = it) },
                        label = { Text("Nur diese Dateien (Whitelist)") },
                        placeholder = { Text("*.pdf, *.docx") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        p.excludePatterns, { p = p.copy(excludePatterns = it) },
                        label = { Text("Diese Dateien ausschliessen (Blacklist)") },
                        placeholder = { Text("Thumbs.db, *.tmp, Temp/**") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Hint("Mehrere Muster mit Komma oder Zeilenumbruch trennen. * = beliebig, ? = ein Zeichen, " +
                        "Ordner/** = alles in diesem Ordner. Leere Whitelist = alles. Die Blacklist hat Vorrang.")
                }
            }

            // Schutzbremse (0.4.2): letzte der optionalen Einstellungen (so auch im Profil-Assistenten ab 0.5.0)
            if (isMirror && show(Step.GUARD)) item {
                SectionCard("Schutzbremse (optional)") {
                    OutlinedTextField(
                        guardText,
                        { v ->
                            guardText = v.filter { it.isDigit() }.take(3)
                            guardText.toIntOrNull()?.let { p = p.copy(massDeletePercent = it.coerceIn(1, 100)) }
                        },
                        label = { Text("Nachfragen, wenn mehr als … % gelöscht würden") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Hint("Sollen auf einmal mehr als so viele Prozent der Dateien in den Papierkorb, hält der Sync an und " +
                        "fragt dich zuerst. Standard: 20. Unabhängig davon fragt LeoSync immer ab 100 Löschungen auf einmal " +
                        "und wenn ein ganzer Unterordner mit mehr als 5 Dateien verschwinden würde. " +
                        "Bei höchstens 5 Dateien im Profil nur, wenn alle gelöscht würden.")
                }
            }

            // Assistent: Zurück / Weiter, ab dem ersten optionalen Schritt direkt "Speichern"
            if (wizard) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val missingText = ProfileWizard.missing(p, step)
                    if (showMissing && missingText != null) Text(missingText, color = MaterialTheme.colorScheme.error)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (stepIndex > 0) OutlinedButton(onClick = { showMissing = false; step = steps[stepIndex - 1] }) {
                            Text("Zurück")
                        }
                        Spacer(Modifier.weight(1f))
                        if (stepIndex < steps.lastIndex) {
                            val next: () -> Unit = {
                                if (missingText != null) { showMissing = true }
                                else { showMissing = false; step = steps[stepIndex + 1] }
                            }
                            if (step.optional) OutlinedButton(onClick = next) { Text("Weiter") }
                            else Button(onClick = next) { Text("Weiter") }
                        }
                        if (step.optional || stepIndex == steps.lastIndex) Button(onClick = { save() }) { Text("Speichern") }
                    }
                    if (step.optional && stepIndex < steps.lastIndex)
                        Hint("Die weiteren Schritte sind optional – du kannst jetzt schon speichern.")
                }
            }

            // Simulation dieses Profils (nur gespeicherte Profile; simuliert wird der gespeicherte Stand)
            if (profileId != 0L) item {
                SectionCard("Simulation") {
                    Hint("Zeigt, was beim Synchronisieren mit diesem Profil passieren würde – ohne etwas zu übertragen.")
                    OutlinedButton(onClick = { onSimulate(profileId) }, enabled = !dirty) { Text("Simulieren") }
                    if (dirty) Hint("Zuerst speichern – simuliert wird der gespeicherte Stand.")
                }
            }

            if (!wizard) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { save() }, enabled = p.name.isNotBlank()) { Text("Speichern") }
                    OutlinedButton(onClick = { leave() }) { Text("Abbrechen") }
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

    if (showRemotePicker) RemoteFolderPicker(
        profile = { withPassword() },
        initial = p.remoteSubfolder,
        onDismiss = { showRemotePicker = false },
        onPick = { p = p.copy(remoteSubfolder = it); showRemotePicker = false },
    )

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Profil löschen?") },
        text = { Text("Einstellungen und Sync-Status dieses Profils werden gelöscht. Dateien auf dem PC bleiben erhalten.") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dao.clearSynced(profileId); dao.clearMappings(profileId)
                        dao.clearDecisions(profileId); dao.clearMirrorState(profileId)
                        dao.clearConflicts(profileId); dao.deleteProfileRow(profileId); dao.clearRestores(profileId)
                        GuardStore(ctx).forget(profileId)
                        com.sync2pc.app.data.GlobalSettings(ctx).setAwaitingFirstStart(profileId, false)
                        SyncScheduler.updateRemotePoll(ctx, dao.profiles())
                    }
                    confirmDelete = false; onBack()
                }
            }) { Text("Löschen") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
    )

    // Nach dem Anlegen: zuerst simulieren, gleich synchronisieren oder später (bis dahin keine Automatik)
    createdId?.let { id ->
        AlertDialog(
            onDismissRequest = { createdId = null; onBack() },
            title = { Text("Profil gespeichert") },
            text = {
                Text("Wie weiter? Bis du dieses Profil einmal synchronisierst, startet es nicht automatisch. " +
                    "So kannst du zuerst in Ruhe simulieren.")
            },
            confirmButton = {
                TextButton(onClick = {
                    createdId = null
                    scope.launch {
                        // Neues Profil = erste Synchronisierung -> ohne Ladegerät vorher warnen
                        if (!FirstSync.isCharging(ctx)) chargeWarnFor = id
                        else { SyncScheduler.enqueueManual(ctx); onBack() }
                    }
                }) { Text("Synchronisieren") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { createdId = null; onBack() }) { Text("Später") }
                    TextButton(onClick = { createdId = null; onSimulate(id) }) { Text("Simulieren") }
                }
            },
        )
    }
    chargeWarnFor?.let {
        ChargeWarningDialog(
            onStart = { chargeWarnFor = null; SyncScheduler.enqueueManual(ctx); onBack() },
            onDismiss = { chargeWarnFor = null; onBack() },
        )
    }

    if (askLeave) AlertDialog(
        onDismissRequest = { askLeave = false },
        title = { Text("Änderungen speichern?") },
        text = { Text("Du hast Änderungen an diesem Profil noch nicht gespeichert.") },
        confirmButton = { TextButton(onClick = { askLeave = false; save() }) { Text("Speichern") } },
        dismissButton = {
            Row {
                TextButton(onClick = { askLeave = false }) { Text("Abbrechen") }
                TextButton(onClick = { askLeave = false; onBack() }) { Text("Verwerfen") }
            }
        },
    )

    overlap?.let { hit ->
        val identical = hit.kind == ProfileOverlap.Kind.IDENTICAL
        AlertDialog(
            onDismissRequest = { overlap = null },
            title = { Text(if (identical) "Profil existiert bereits" else "Profile überschneiden sich") },
            text = { Text(hit.message() + if (identical) "\n\nDas Profil wurde nicht gespeichert." else "") },
            confirmButton = {
                if (identical) TextButton(onClick = { overlap = null }) { Text("OK") }
                else TextButton(onClick = { overlap = null; save(force = true) }) { Text("Trotzdem speichern") }
            },
            dismissButton = { if (!identical) TextButton(onClick = { overlap = null }) { Text("Abbrechen") } },
        )
    }

    if (checkingFolder) AlertDialog(
        onDismissRequest = {},
        title = { Text("Prüfe Ordner…") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Passt der neue Ordner zum bisherigen Sync-Stand?")
            }
        },
        confirmButton = {},
    )

    // Stichprobe nach einem Ordnerwechsel passt nicht (0.4.1) – bewusst ohne Zahlen
    folderWarning?.let { w ->
        AlertDialog(
            onDismissRequest = { folderWarning = null },
            title = { Text("Ist das wirklich der richtige Ordner?") },
            text = { Text("Der gewählte Ordner passt nicht zum bisherigen Sync-Stand dieses Profils.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        folderWarning = null
                        if (Side.LOCAL in w.sides) treePicker.launch(null) else showRemotePicker = true
                    }) { Text("Anderen Ordner wählen") }
                    TextButton(onClick = { folderWarning = null; save(w.force, FolderChoice.ACCEPT) }) { Text("Trotzdem übernehmen") }
                    TextButton(onClick = { folderWarning = null; save(w.force, FolderChoice.RESET) }) { Text("Als neuen Ordner behandeln") }
                    TextButton(onClick = { folderWarning = null }) { Text("Abbrechen") }
                }
            },
        )
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Sync-Status zurücksetzen?") },
        text = { Text("Der nächste Sync gleicht alle Dateien erneut mit dem Share ab. Das kann bei vielen Dateien länger dauern.") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dao.clearSynced(profileId); dao.clearMirrorState(profileId); dao.clearConflicts(profileId)
                        GuardStore(ctx).forget(profileId)
                    }
                    confirmReset = false
                    Toast.makeText(ctx, "Zurückgesetzt", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Zurücksetzen") }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") } },
    )
}

/** Einleitung je Schritt des Assistenten: Was stellt man hier ein und warum? */
private fun stepIntro(step: Step, mirror: Boolean): String = when (step) {
    Step.NAME -> "Wie soll das Profil heissen? Der Name erscheint auf der Startseite, im Verlauf und in den " +
        "Benachrichtigungen, z. B. „Kamera“ oder „Dokumente“."
    Step.SOURCE -> if (mirror) "Welcher Ordner auf dem Handy soll mit dem PC abgeglichen werden? Alle Dateien und " +
        "Unterordner darin werden synchronisiert."
    else "Welche Fotos und Videos sollen gesichert werden? Wähle die Ordner auf dem Telefon, in denen sie liegen."
    Step.TARGET -> "Wohin auf dem PC? Gib die Windows-Freigabe an und melde dich mit dem Windows-Benutzer an, der " +
        "darauf zugreifen darf. „Verbindung testen“ zeigt, ob alles stimmt."
    Step.OPTIONS -> if (mirror) "Wann wird synchronisiert, in welche Richtung, und was passiert mit gelöschten oder " +
        "geänderten Dateien?"
    else "Wann wird synchronisiert, und was passiert danach mit den Fotos auf dem Telefon?"
    Step.FILTER -> "Optional: Nur bestimmte Dateien synchronisieren oder einzelne ausschliessen. Ohne Angaben wird alles " +
        "synchronisiert."
    Step.GUARD -> "Optional: Ab wann LeoSync bei vielen Löschungen zuerst nachfragt. Der Standard passt für die meisten."
}

/** Antwort auf "Ist das wirklich der richtige Ordner?" */
enum class FolderChoice { ACCEPT, RESET }

/** Offene Ordner-Warnung: betroffene Seiten und ob eine Überschneidung schon bestätigt war */
data class FolderWarning(val sides: Set<Side>, val force: Boolean)

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
