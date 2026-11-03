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
import com.sync2pc.app.R
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
import com.sync2pc.app.l10n.str
import com.sync2pc.app.mirror.FolderCheck
import com.sync2pc.app.mirror.GuardStore
import com.sync2pc.app.mirror.LocalTree
import com.sync2pc.app.mirror.MirrorEngine
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
    var p by remember { mutableStateOf(Profile(name = str(R.string.profile_default_photos))) }
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
            Toast.makeText(ctx, str(R.string.profile_folder_unusable), Toast.LENGTH_LONG).show()
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
            Toast.makeText(ctx, str(R.string.profile_no_persistent_access), Toast.LENGTH_LONG).show()
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
            Toast.makeText(ctx, str(R.string.profile_saved_toast), Toast.LENGTH_SHORT).show()
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

    val trashName = MirrorEngine.TRASH_DIR

    ScreenScaffold(
        title = str(if (profileId == 0L) R.string.profile_title_new else R.string.profile_title_edit),
        onBack = { if (wizard && typeChosen && stepIndex > 0) { showMissing = false; step = steps[stepIndex - 1] } else leave() },
        actions = {
            // Im Assistenten erst ab den optionalen Schritten speichern (vorher fehlen Pflichtangaben)
            if (loaded && typeChosen && (!wizard || step.optional)) IconButton(onClick = { save() }, enabled = p.name.isNotBlank()) {
                Icon(Icons.Filled.Check, str(R.string.common_save))
            }
            if (profileId != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, str(R.string.profile_delete_cd)) }
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
                    name = str(if (type == ProfileType.MIRROR) R.string.profile_default_folder else R.string.profile_default_photos),
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
                        str(if (step.optional) R.string.profile_step_optional else R.string.profile_step,
                            stepIndex + 1, steps.size, step.title),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(stepIntro(step, isMirror), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (show(Step.NAME)) item {
                SectionCard(str(R.string.profile_general)) {
                    OutlinedTextField(p.name, { p = p.copy(name = it) }, label = { Text(str(R.string.profile_name)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    SwitchRow(str(R.string.profile_active), p.enabled) { p = p.copy(enabled = it) }
                    Hint(str(if (isMirror) R.string.profile_type_mirror else R.string.profile_type_photos, p.type().label))
                }
            }

            if (isMirror && show(Step.SOURCE)) item {
                SectionCard(str(R.string.profile_local_folder)) {
                    val treeUri = p.localTreeUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    if (treeUri == null) Hint(str(R.string.profile_no_folder_yet))
                    else {
                        Text(LocalTree.label(treeUri))
                        if (!LocalTree.hasPermission(ctx, treeUri))
                            Text(str(R.string.profile_access_lost), color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedButton(onClick = { treePicker.launch(null) }) {
                        Text(str(if (treeUri == null) R.string.profile_choose_folder else R.string.profile_choose_other_folder))
                    }
                    Hint(str(R.string.profile_local_folder_hint))
                }
            }

            if (!isMirror && show(Step.SOURCE)) item {
                SectionCard(str(R.string.profile_sources)) {
                    if (p.folders().isEmpty()) Hint(str(R.string.profile_sources_empty))
                    p.folders().forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f, Modifier.weight(1f))
                            IconButton(onClick = { p = p.copy(sourceFolders = Profile.joinLines(p.folders() - f)) }) {
                                Icon(Icons.Filled.Close, str(R.string.profile_remove))
                            }
                        }
                    }
                    OutlinedButton(onClick = { folderPicker.launch(null) }) { Text(str(R.string.profile_add_folder)) }
                    Hint(str(R.string.profile_sources_hint))
                    SwitchRow(str(R.string.profile_subfolders), p.includeSubfolders,
                        subtitle = str(R.string.profile_subfolders_sub)) { p = p.copy(includeSubfolders = it) }
                    SwitchRow(str(R.string.profile_photos), p.includeImages) { p = p.copy(includeImages = it) }
                    SwitchRow(str(R.string.profile_videos), p.includeVideos) { p = p.copy(includeVideos = it) }
                }
            }

            if (show(Step.TARGET)) item {
                SectionCard(str(R.string.profile_target)) {
                    if (credentialSources.isNotEmpty()) {
                        Hint(str(R.string.profile_copy_credentials))
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
                                        Toast.makeText(ctx, str(R.string.profile_credentials_copied, src.name), Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("${src.name} (${src.smbUser})") },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        p.smbPath, { p = p.copy(smbPath = it) },
                        label = { Text(str(R.string.profile_share_path)) },
                        placeholder = { Text("\\\\192.168.1.20\\" + str(R.string.profile_share_example)) },
                        isError = p.smbPath.isNotBlank() && SmbTarget.parse(p.smbPath) == null,
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Hint(str(R.string.profile_share_format))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            p.remoteSubfolder, { p = p.copy(remoteSubfolder = it) },
                            label = { Text(str(R.string.profile_subfolder)) },
                            placeholder = { Text(str(if (isMirror) R.string.profile_subfolder_example_mirror else R.string.profile_subfolder_example_photos)) },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            enabled = SmbTarget.parse(p.smbPath) != null,
                            onClick = { showRemotePicker = true },
                        ) { Text(str(R.string.profile_browse)) }
                    }
                    OutlinedTextField(p.smbUser, { p = p.copy(smbUser = it) }, label = { Text(str(R.string.profile_user)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        password, { password = it }, label = { Text(str(R.string.profile_password)) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(p.smbDomain, { p = p.copy(smbDomain = it) },
                        label = { Text(str(R.string.profile_domain)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Hint(str(R.string.profile_domain_hint))
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
                        }) { Text(str(R.string.profile_test)) }
                        if (testing) CircularProgressIndicator(Modifier.padding(start = 12.dp))
                    }
                    testResult?.let { Text(it) }
                }
            }

            if (show(Step.OPTIONS)) item {
                SectionCard(str(R.string.profile_wifi)) {
                    Hint(str(R.string.profile_wifi_hint))
                    p.ssids().forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s, Modifier.weight(1f))
                            IconButton(onClick = { p = p.copy(wifiSsids = Profile.joinLines(p.ssids() - s)) }) {
                                Icon(Icons.Filled.Close, str(R.string.profile_remove))
                            }
                        }
                    }
                    if (currentSsid != null && !p.ssids().contains(currentSsid)) {
                        OutlinedButton(onClick = { p = p.copy(wifiSsids = Profile.joinLines(p.ssids() + currentSsid)) }) {
                            Text(str(R.string.profile_wifi_add_current, currentSsid))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newSsid, { newSsid = it }, label = { Text(str(R.string.profile_wifi_name)) },
                            singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(enabled = newSsid.isNotBlank(), onClick = {
                            p = p.copy(wifiSsids = Profile.joinLines(p.ssids() + newSsid.trim())); newSsid = ""
                        }) { Text(str(R.string.profile_add)) }
                    }
                }
            }

            if (isMirror && show(Step.OPTIONS)) item {
                SectionCard(str(R.string.profile_direction)) {
                    Hint(str(R.string.profile_direction_hint))
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
                        str(R.string.profile_propagate), p.propagateDeletes,
                        subtitle = str(R.string.profile_propagate_sub, trashName),
                    ) { p = p.copy(propagateDeletes = it) }
                    SwitchRow(
                        str(R.string.profile_keep_versions), p.keepVersions,
                        subtitle = str(R.string.profile_keep_versions_sub, trashName),
                    ) { p = p.copy(keepVersions = it) }
                    OutlinedTextField(
                        trashText,
                        { v ->
                            trashText = v.filter { it.isDigit() }.take(4)
                            trashText.toIntOrNull()?.let { p = p.copy(trashRetentionDays = it.coerceIn(0, 3650)) }
                        },
                        label = { Text(str(R.string.profile_trash_days)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Hint(str(R.string.profile_trash_days_hint, trashName))
                    if (p.dir() == SyncDirection.BOTH) {
                        Text(str(R.string.profile_conflict_question), style = MaterialTheme.typography.bodyMedium)
                        ConflictMode.entries.forEach { m ->
                            Row(
                                Modifier.fillMaxWidth().clickable { p = p.copy(conflictMode = m.name) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = p.conflict() == m, onClick = { p = p.copy(conflictMode = m.name) })
                                Text(m.label)
                            }
                        }
                        if (p.conflict() == ConflictMode.NEWER) Hint(str(R.string.profile_conflict_newer_hint))
                    }
                    OutlinedTextField(
                        pollText,
                        { v ->
                            pollText = v.filter { it.isDigit() }.take(4)
                            pollText.toIntOrNull()?.let { p = p.copy(remotePollMinutes = it.coerceIn(15, 24 * 60)) }
                        },
                        label = { Text(str(R.string.profile_poll)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Hint(str(R.string.profile_poll_hint))
                }
            }

            if (!isMirror && show(Step.OPTIONS)) item {
                SectionCard(str(R.string.profile_after_upload)) {
                    Hint(str(R.string.profile_after_upload_hint))
                    DeleteMode.entries.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clickable { p = p.copy(deleteMode = m.name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = p.delete() == m, onClick = { p = p.copy(deleteMode = m.name) })
                            Text(m.label)
                        }
                    }
                    if (p.delete() != DeleteMode.KEEP) Hint(str(R.string.profile_delete_hint))
                    SwitchRow(
                        str(R.string.profile_verify),
                        checked = p.mustVerify(),
                        subtitle = str(if (p.delete() != DeleteMode.KEEP) R.string.profile_verify_required else R.string.profile_verify_sub),
                        enabled = p.delete() == DeleteMode.KEEP,
                    ) { p = p.copy(verifyUpload = it) }
                }
            }

            if (show(Step.FILTER)) item {
                SectionCard(str(R.string.profile_filter)) {
                    OutlinedTextField(
                        p.includePatterns, { p = p.copy(includePatterns = it) },
                        label = { Text(str(R.string.profile_filter_include)) },
                        placeholder = { Text("*.pdf, *.docx") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        p.excludePatterns, { p = p.copy(excludePatterns = it) },
                        label = { Text(str(R.string.profile_filter_exclude)) },
                        placeholder = { Text("Thumbs.db, *.tmp, Temp/**") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Hint(str(R.string.profile_filter_hint))
                }
            }

            // Schutzbremse (0.4.2): letzte der optionalen Einstellungen (so auch im Profil-Assistenten ab 0.5.0)
            if (isMirror && show(Step.GUARD)) item {
                SectionCard(str(R.string.profile_guard)) {
                    OutlinedTextField(
                        guardText,
                        { v ->
                            guardText = v.filter { it.isDigit() }.take(3)
                            guardText.toIntOrNull()?.let { p = p.copy(massDeletePercent = it.coerceIn(1, 100)) }
                        },
                        label = { Text(str(R.string.profile_guard_percent)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Hint(str(R.string.profile_guard_hint))
                }
            }

            // Assistent: Zurück / Weiter, ab dem ersten optionalen Schritt direkt "Speichern"
            if (wizard) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val missingText = ProfileWizard.missing(p, step)
                    if (showMissing && missingText != null) Text(missingText, color = MaterialTheme.colorScheme.error)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (stepIndex > 0) OutlinedButton(onClick = { showMissing = false; step = steps[stepIndex - 1] }) {
                            Text(str(R.string.common_back))
                        }
                        Spacer(Modifier.weight(1f))
                        if (stepIndex < steps.lastIndex) {
                            val next: () -> Unit = {
                                if (missingText != null) { showMissing = true }
                                else { showMissing = false; step = steps[stepIndex + 1] }
                            }
                            if (step.optional) OutlinedButton(onClick = next) { Text(str(R.string.profile_next)) }
                            else Button(onClick = next) { Text(str(R.string.profile_next)) }
                        }
                        if (step.optional || stepIndex == steps.lastIndex) Button(onClick = { save() }) { Text(str(R.string.common_save)) }
                    }
                    if (step.optional && stepIndex < steps.lastIndex)
                        Hint(str(R.string.profile_optional_hint))
                }
            }

            // Simulation dieses Profils (nur gespeicherte Profile; simuliert wird der gespeicherte Stand)
            if (profileId != 0L) item {
                SectionCard(str(R.string.profile_simulation)) {
                    Hint(str(R.string.profile_simulation_hint))
                    OutlinedButton(onClick = { onSimulate(profileId) }, enabled = !dirty) { Text(str(R.string.profile_simulate)) }
                    if (dirty) Hint(str(R.string.profile_simulation_save_first))
                }
            }

            if (!wizard) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { save() }, enabled = p.name.isNotBlank()) { Text(str(R.string.common_save)) }
                    OutlinedButton(onClick = { leave() }) { Text(str(R.string.common_cancel)) }
                }
            }

            if (profileId != 0L) item {
                SectionCard(str(R.string.profile_maintenance)) {
                    Hint(str(R.string.profile_maintenance_hint))
                    OutlinedButton(onClick = { confirmReset = true }) { Text(str(R.string.profile_reset)) }
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
        title = { Text(str(R.string.profile_delete_title)) },
        text = { Text(str(R.string.profile_delete_text)) },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dao.clearSynced(profileId); dao.clearMappings(profileId)
                        dao.clearDecisions(profileId); dao.clearMirrorState(profileId)
                        dao.clearConflicts(profileId); dao.deleteProfileRow(profileId); dao.clearRestores(profileId)
                        GuardStore(ctx).forget(profileId)
                        com.sync2pc.app.mirror.PurgeStore(ctx).forget(profileId)
                        com.sync2pc.app.mirror.RestoreFailures(ctx).forget(profileId)
                        com.sync2pc.app.data.GlobalSettings(ctx).setAwaitingFirstStart(profileId, false)
                        SyncScheduler.updateRemotePoll(ctx, dao.profiles())
                    }
                    confirmDelete = false; onBack()
                }
            }) { Text(str(R.string.common_delete)) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(str(R.string.common_cancel)) } },
    )

    // Nach dem Anlegen: zuerst simulieren, gleich synchronisieren oder später (bis dahin keine Automatik)
    createdId?.let { id ->
        AlertDialog(
            onDismissRequest = { createdId = null; onBack() },
            title = { Text(str(R.string.profile_created_title)) },
            text = { Text(str(R.string.profile_created_text)) },
            confirmButton = {
                TextButton(onClick = {
                    createdId = null
                    scope.launch {
                        // Neues Profil = erste Synchronisierung -> ohne Ladegerät vorher warnen
                        if (!FirstSync.isCharging(ctx)) chargeWarnFor = id
                        else { SyncScheduler.enqueueManual(ctx); onBack() }
                    }
                }) { Text(str(R.string.home_sync)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { createdId = null; onBack() }) { Text(str(R.string.profile_later)) }
                    TextButton(onClick = { createdId = null; onSimulate(id) }) { Text(str(R.string.profile_simulate)) }
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
        title = { Text(str(R.string.profile_leave_title)) },
        text = { Text(str(R.string.profile_leave_text)) },
        confirmButton = { TextButton(onClick = { askLeave = false; save() }) { Text(str(R.string.common_save)) } },
        dismissButton = {
            Row {
                TextButton(onClick = { askLeave = false }) { Text(str(R.string.common_cancel)) }
                TextButton(onClick = { askLeave = false; onBack() }) { Text(str(R.string.home_discard)) }
            }
        },
    )

    overlap?.let { hit ->
        val identical = hit.kind == ProfileOverlap.Kind.IDENTICAL
        AlertDialog(
            onDismissRequest = { overlap = null },
            title = { Text(str(if (identical) R.string.profile_exists_title else R.string.profile_overlap_title)) },
            text = { Text(hit.message() + if (identical) "\n\n" + str(R.string.profile_not_saved) else "") },
            confirmButton = {
                if (identical) TextButton(onClick = { overlap = null }) { Text(str(R.string.common_ok)) }
                else TextButton(onClick = { overlap = null; save(force = true) }) { Text(str(R.string.profile_save_anyway)) }
            },
            dismissButton = { if (!identical) TextButton(onClick = { overlap = null }) { Text(str(R.string.common_cancel)) } },
        )
    }

    if (checkingFolder) AlertDialog(
        onDismissRequest = {},
        title = { Text(str(R.string.profile_checking_folder)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text(str(R.string.profile_checking_folder_text))
            }
        },
        confirmButton = {},
    )

    // Stichprobe nach einem Ordnerwechsel passt nicht (0.4.1) – bewusst ohne Zahlen
    folderWarning?.let { w ->
        AlertDialog(
            onDismissRequest = { folderWarning = null },
            title = { Text(str(R.string.profile_folder_warning_title)) },
            text = { Text(str(R.string.profile_folder_warning_text)) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        folderWarning = null
                        if (Side.LOCAL in w.sides) treePicker.launch(null) else showRemotePicker = true
                    }) { Text(str(R.string.guard_choose_other)) }
                    TextButton(onClick = { folderWarning = null; save(w.force, FolderChoice.ACCEPT) }) { Text(str(R.string.guard_accept)) }
                    TextButton(onClick = { folderWarning = null; save(w.force, FolderChoice.RESET) }) { Text(str(R.string.guard_as_new)) }
                    TextButton(onClick = { folderWarning = null }) { Text(str(R.string.common_cancel)) }
                }
            },
        )
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(str(R.string.profile_reset_title)) },
        text = { Text(str(R.string.profile_reset_text)) },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dao.clearSynced(profileId); dao.clearMirrorState(profileId); dao.clearConflicts(profileId)
                        GuardStore(ctx).forget(profileId)
                    }
                    confirmReset = false
                    Toast.makeText(ctx, str(R.string.profile_reset_done), Toast.LENGTH_SHORT).show()
                }
            }) { Text(str(R.string.profile_reset_button)) }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(str(R.string.common_cancel)) } },
    )
}

/** Einleitung je Schritt des Assistenten: Was stellt man hier ein und warum? */
private fun stepIntro(step: Step, mirror: Boolean): String = str(when (step) {
    Step.NAME -> R.string.wizard_intro_name
    Step.SOURCE -> if (mirror) R.string.wizard_intro_source_mirror else R.string.wizard_intro_source_photos
    Step.TARGET -> R.string.wizard_intro_target
    Step.OPTIONS -> if (mirror) R.string.wizard_intro_options_mirror else R.string.wizard_intro_options_photos
    Step.FILTER -> R.string.wizard_intro_filter
    Step.GUARD -> R.string.wizard_intro_guard
})

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
