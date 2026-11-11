@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sync2pc.app.ui

import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import com.sync2pc.app.data.VerifyLevel
import com.sync2pc.app.data.CredentialGroups
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.sync2pc.app.data.ChangeConfirm
import com.sync2pc.app.data.NumberInput
import com.sync2pc.app.data.Profile
import com.sync2pc.app.mirror.FileFilter
import com.sync2pc.app.data.ProfileOverlap
import com.sync2pc.app.data.ProfileWizard
import com.sync2pc.app.data.ProfileWizard.Step
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.saveable.rememberSaveable
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SmbEncryption
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
import com.sync2pc.app.sync.PcCheckMode
import com.sync2pc.app.sync.PcDecision
import com.sync2pc.app.sync.PcEditorRules
import com.sync2pc.app.sync.PcIdentityException
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
    val pState = remember { mutableStateOf(Profile(name = str(R.string.profile_default_photos))) }
    var p by pState
    var password by remember { mutableStateOf("") }
    var newSsid by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testFailed by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testDetail by remember { mutableStateOf<String?>(null) }
    // -146 Teil B: Verschlüsselungszeile des Verbindungstests, Hilfeknopf und Signal zum Aufklappen der Karte
    var encLine by remember { mutableStateOf<String?>(null) }
    var encHelp by remember { mutableStateOf(false) }
    var encOpenSignal by remember { mutableStateOf(0) }
    // PC-Erkennung (0.13.0): Modus vom Benutzer gewählt? Zuletzt "anderer PC" gemeldet? Rückfrage zum Neuvertrauen
    var pcTouched by remember { mutableStateOf(false) }
    var pcChanged by remember { mutableStateOf(false) }
    var trustAsk by remember { mutableStateOf<TrustAsk?>(null) }
    var hostAsk by remember { mutableStateOf<HostAsk?>(null) }
    var confirmedHost by remember { mutableStateOf<String?>(null) }
    var folderError by remember { mutableStateOf<String?>(null) }
    // V1: Bestätigung bei Änderung von Richtung / «Löschungen übernehmen» (bestehendes Ordner-Profil)
    var changeAsk by remember { mutableStateOf<ChangeAsk?>(null) }
    var confirmedChange by remember { mutableStateOf<String?>(null) }
    // Assistent (0.5.0): aktueller Schritt; «Hinweis fehlt» nach einem blockierten «Weiter» (-120: auch bei ungültigem Zahlenfeld)
    var step by rememberSaveable { mutableStateOf(Step.NAME) }
    var showMissing by remember { mutableStateOf(false) }
    // Android-Systemdialog zur Ordnerauswahl. Die Auswahl wird in einen MediaStore-Pfad
    // umgerechnet (z. B. "DCIM/Camera/"); gelesen wird weiterhin schnell über den MediaStore.
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val rel = treeUriToRelativePath(uri)
        if (rel == null) {
            // P3: Grund dauerhaft unter der Karte statt nur als flüchtiger Hinweis
            folderError = str(treeUriProblem(uri))
            Toast.makeText(ctx, folderError, Toast.LENGTH_LONG).show()
        } else {
            folderError = null
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
    val pollState = remember { mutableStateOf("") }
    var pollText by pollState
    val trashState = remember { mutableStateOf("") }
    var trashText by trashState
    var guardText by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmFullVerify by remember { mutableStateOf(false) }
    var showCredentialPicker by remember { mutableStateOf(false) }
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
    // Berechtigungen bei Bedarf (0.11.0): nach dem Erteilen neu lesen (onResume), auch den WLAN-Namen
    val permTick = rememberResumeTick()
    val perms = remember(permTick) { PermStatus.read(ctx) }
    val currentSsid = remember(permTick) { WifiHelper.currentSsid(ctx) }
    // Andere Profile mit Zugangsdaten -> zum Übernehmen anbieten
    val allProfiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())
    val credentialSources = allProfiles.filter { it.id != profileId && it.smbUser.isNotBlank() }

    // Ordnerstruktur der Fotos (SYNC-20261109-085): gesperrt nach dem ersten Sync (aus der DB) oder während eines Laufs
    val wm = remember { androidx.work.WorkManager.getInstance(ctx) }
    val syncRunning by remember {
        kotlinx.coroutines.flow.combine(SyncScheduler.ALL_SYNC_WORK.map { wm.getWorkInfosForUniqueWorkFlow(it) }) { arr ->
            arr.any { list -> list.any { it.state == androidx.work.WorkInfo.State.RUNNING } }
        }
    }.collectAsState(initial = false)
    var layoutLocked by remember { mutableStateOf(false) }
    LaunchedEffect(profileId, syncRunning) {
        if (profileId != 0L) layoutLocked = withContext(Dispatchers.IO) { dao.layoutLocked(profileId) }
    }
    val layoutEditable = ProfileWizard.layoutEditable(p, layoutLocked, syncRunning)
    // Gleiches Ziel wie ein anderes Fotoprofil: dessen Struktur übernehmen (Hinweis; Abweichung wird beim Speichern blockiert)
    val layoutOwner = ProfileWizard.layoutOwner(p, allProfiles)
    // Eigene Wahl getrennt merken (nur Auswahl in der Zeile "Ordnerstruktur" bzw. gespeicherter Wert); die Vorlage des
    // Eigentümers überschreibt sie nur in der Anzeige und geht bei Zieländerung wieder verloren (SYNC-20261109-089)
    var ownLayout by remember { mutableStateOf<com.sync2pc.app.data.FolderLayout?>(null) }
    val effectiveLayout = ProfileWizard.effectiveLayout(ownLayout, layoutOwner?.layout(), layoutEditable, p.layout())
    LaunchedEffect(effectiveLayout) {
        if (effectiveLayout.code != p.folderLayout) p = p.copy(folderLayout = effectiveLayout.code)
    }

    LaunchedEffect(profileId) {
        if (profileId != 0L) {
            withContext(Dispatchers.IO) {
                dao.profile(profileId)?.let { loadedProfile ->
                    // 0.11.0: alter Unterordner wird Teil des Freigabe-Pfads (gleiches Ziel, nur ein Feld)
                    val it = if (loadedProfile.remoteSubfolder.isBlank()) loadedProfile else loadedProfile.copy(
                        smbPath = loadedProfile.smbPath.trimEnd('\\', '/') + "\\" + loadedProfile.remoteSubfolder.trim('\\', '/'),
                        remoteSubfolder = "",
                    )
                    ownLayout = it.layout()
                    p = it
                    password =CredentialCrypto.decrypt(it.smbPasswordEnc)
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

    /** Neues Profil ohne eigene Wahl: BOTH mit WLAN-Namen, sonst GUID (Vorgabe SYNC-20261109-048); sonst der gewählte Modus */
    fun pcModeNow(): PcCheckMode =
        if (profileId == 0L && !pcTouched) (if (p.ssids().isNotEmpty()) PcCheckMode.BOTH else PcCheckMode.GUID) else p.pcMode()

    fun withPassword(): Profile = p.copy(smbPasswordEnc = CredentialCrypto.encrypt(password), pcCheck = pcModeNow().name)

    /** Rückfrage VOR dem Anmelden (blockiert den IO-Thread, bis der Benutzer antwortet; ohne Antwort = nicht vertrauen) */
    fun askTrust(host: String): Boolean {
        val answer = java.util.concurrent.CompletableFuture<Boolean>()
        trustAsk = TrustAsk(host, answer)
        return try { answer.get(45, java.util.concurrent.TimeUnit.SECONDS) } catch (e: Exception) { false } finally { trustAsk = null }
    }

    /**
     * Hostwechsel bei bestehendem Profil (GUID/BOTH): vor jedem Senden von Zugangsdaten an den neuen Host fragen (-079).
     * Ohne Bestätigung wird [action] nicht ausgeführt (keine Anmeldung, nichts gespeichert).
     */
    fun confirmHost(action: () -> Unit) {
        val oldHost = baseline?.let { SmbTarget.hostOf(it.smbPath) }
        val newHost = SmbTarget.hostOf(p.smbPath).orEmpty()
        if (PcEditorRules.needsHostConfirm(p.id != 0L && baseline != null, pcModeNow(), oldHost, newHost, confirmedHost))
            hostAsk = HostAsk(oldHost.orEmpty(), newHost, action)
        else action()
    }

    fun runTestNow() {
        // P2: Eingabe zuerst prüfen, keine rohe Ausnahme zeigen
        if (SmbTarget.hostOf(p.smbPath) == null) {
            testResult = "✗ " + str(R.string.profile_test_need_path, "\\\\192.168.1.20"); testFailed = true; testDetail = null
            return
        }
        testing = true; testResult = null; testFailed = false; testDetail = null; pcChanged = false; encLine = null; encHelp = false
        scope.launch {
            val prof = withPassword()
            withContext(Dispatchers.IO) {
                try {
                    // Netzbedingung fürs erste Kennenlernen (BOTH: WLAN muss in der Liste stehen), Test lernt als einziger Weg neben dem Speichern
                    val netOk = com.sync2pc.app.sync.PcLearner.learnNetOk(ctx, prof)
                    val r = SmbSession.testDetailed(prof, netOk) { host, _, _ -> askTrust(host) }
                    testResult = "✓ " + r.message
                    // Zeile zur Verschlüsselung: Symbol plus Text, die SMB-Version steht immer in Klammern
                    when {
                        r.encrypted -> encLine = str(R.string.enc_test_ok, r.dialect)
                        r.setting == SmbEncryption.OFF -> encLine = str(R.string.enc_test_off, r.dialect)
                        else -> { encLine = str(R.string.enc_test_warn, r.dialect); encHelp = true }
                    }
                    // Gelernte/übernommene Kennung in die Ansicht holen (übrige Eingaben bleiben unangetastet)
                    if (prof.id != 0L) dao.profile(prof.id)?.let { db ->
                        p = p.copy(pcGuid = db.pcGuid, pcGuidHost = db.pcGuidHost)
                        baseline = baseline?.copy(pcGuid = db.pcGuid, pcGuidHost = db.pcGuidHost)
                    }
                } catch (e: Exception) {
                    if (e is PcIdentityException && e.decision == PcDecision.CHANGED) pcChanged = true
                    if (e is com.sync2pc.app.smb.SmbEncryptionUnavailableException) {
                        // Erforderlich, aber der PC kann nicht verschlüsseln: keine Anmeldedaten gesendet; Karte klappt auf
                        testResult = str(R.string.enc_test_required_fail); testFailed = true
                        testDetail = com.sync2pc.app.sync.shortError(e); encHelp = true; encOpenSignal++
                        testing = false
                        return@withContext
                    }
                    // 0.11.0: verständlich, technischer Text aufklappbar
                    testResult = "✗ " + (if (e is IllegalArgumentException) str(R.string.profile_test_bad_input)
                        else com.sync2pc.app.sync.userError(e)); testFailed = true
                    testDetail = com.sync2pc.app.sync.shortError(e)
                }
            }
            testing = false
        }
    }

    /**
     * Wechsel der Erkennungsart. WIFI → GUID/BOTH: still gelernte Kennung zählt nicht als Vertrauen und wird gelöscht (verringert
     * nur Vertrauen). Kein automatischer Test mehr (V6): Zugangsdaten werden nicht durch blosses Antippen einer Option gesendet;
     * die Kennung wird nach dem Speichern bzw. mit «Verbindung testen» gelernt.
     */
    fun setPcMode(m: PcCheckMode) {
        val old = pcModeNow()
        pcTouched = true
        p = p.copy(pcCheck = m.name)
        if (PcEditorRules.clearOnModeChange(old, m) && p.id != 0L) scope.launch {
            withContext(Dispatchers.IO) { dao.clearPcGuid(p.id) }
            p = p.copy(pcGuid = "", pcGuidHost = "")
            baseline = baseline?.copy(pcGuid = "", pcGuidHost = "")
        }
    }

    fun saveNow(force: Boolean, folderChoice: FolderChoice?) {
        scope.launch {
            // Leerzeichen am Rand des Namens entfernen (sonst "(Fotos )" im Verlauf); verifyUpload spiegelt die Stufe
            val toSave = withPassword().let { it.copy(name = it.name.trim(), verifyUpload = it.mustVerify()) }
            // Doppelte Profile verhindern bzw. bei Überschneidung warnen
            val hit = withContext(Dispatchers.IO) {
                val cache = HashMap<String, String?>()
                ProfileOverlap.check(toSave, dao.profiles()) { uri -> cache.getOrPut(uri) { LocalTree.canonical(ctx, uri) } }
            }
            if (hit != null && (hit.kind != ProfileOverlap.Kind.PARTIAL || !force)) {
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
                // Gelernte Kennung stammt immer aus der Datenbank (der Editor kann veraltet sein) und gilt nur für den
                // aktuellen Host; bei Hostwechsel wird sie verworfen (nie eine Kennung eines anderen PCs behalten)
                val db = if (isNew) null else dao.profile(toSave.id)
                val host = SmbTarget.hostOf(toSave.smbPath)?.let { com.sync2pc.app.sync.PcIdentity.normalizeHost(it) }.orEmpty()
                // V7: eine bei WIFI still gelernte Kennung wird nie zu Vertrauen (Modus in der Datenbank WIFI, neu GUID/BOTH)
                val keep = PcEditorRules.keepStoredOnSave(db?.pcMode(), toSave.pcMode(), db?.pcGuidHost, host)
                val finalProfile = toSave.copy(pcGuid = if (keep) db!!.pcGuid else "", pcGuidHost = if (keep) db!!.pcGuidHost else "")
                val id = if (isNew) dao.insertProfile(finalProfile) else { dao.updateProfile(finalProfile); toSave.id }
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
            // PC-Kennung nach dem Speichern lernen (V5): app-weit, nach dem DB-Schreiben; Speichern gelingt unabhängig vom Ergebnis
            com.sync2pc.app.sync.PcLearner.learnAfterSave(ctx, savedId)
            Toast.makeText(ctx, str(R.string.profile_saved_toast), Toast.LENGTH_SHORT).show()
            if (isNew) {
                // Stand gilt jetzt als gespeichert (keine Nachfrage beim Verlassen)
                pcTouched = true
                p = toSave.copy(id = savedId); baseline = p; basePassword = password
                createdId = savedId
            } else onBack()
        }
    }

    /**
     * Speichern. [force] = Überschneidung bestätigt. [folderChoice] = Antwort auf "Ist das wirklich der richtige
     * Ordner?" (null = Stichprobe noch nicht gemacht).
     */
    fun save(force: Boolean = false, folderChoice: FolderChoice? = null) {
        // P7/P8: nie mit leerem oder ungültigem Zahlenfeld speichern (sonst stiller Zwischenwert)
        val badStep = ProfileWizard.firstInvalidNumberStep(p, trashText, pollText, guardText)
        if (badStep != null) {
            // -120: im Assistenten zum ersten fehlerhaften Schritt zurückspringen, damit der Fehlertext sichtbar ist
            if (profileId == 0L) { showMissing = false; step = badStep }
            Toast.makeText(ctx, str(R.string.profile_number_blocked), Toast.LENGTH_LONG).show()
            return
        }
        // P9: ungültiges Filtermuster nicht speichern
        (FileFilter.unbalancedPattern(p.includePatterns) ?: FileFilter.unbalancedPattern(p.excludePatterns))?.let { bad ->
            Toast.makeText(ctx, str(R.string.profile_filter_bad, bad), Toast.LENGTH_LONG).show()
            return
        }
        // V1: Bestätigung vor dem Speichern, einmal je Änderung (auch bei späterem Wiederaufruf nach anderen Dialogen)
        val sig = "${p.direction}|${p.propagateDeletes}"
        val text = ChangeConfirm.text(baseline, p, MirrorEngine.TRASH_DIR)
        if (text != null && confirmedChange != sig) { changeAsk = ChangeAsk(text, sig, force, folderChoice); return }
        confirmHost { saveNow(force, folderChoice) }
    }

    // Ungespeicherte Änderungen? Dann beim Verlassen nachfragen (Zurück-Pfeil, Zurück-Geste, "Abbrechen")
    val dirty = typeChosen && baseline != null && (p != baseline || password != basePassword)
    fun leave() { if (dirty) askLeave = true else onBack() }
    BackHandler(enabled = dirty) { askLeave = true }

    // Profil-Assistent (0.5.0): neues Profil Schritt für Schritt
    val wizard = profileId == 0L
    val steps = ProfileWizard.steps(p)
    val stepIndex = steps.indexOf(step).coerceAtLeast(0)
    // Zurück-Geste im Assistenten: einen Schritt zurück (später registriert -> hat Vorrang)
    BackHandler(enabled = wizard && typeChosen && stepIndex > 0) { showMissing = false; step = steps[stepIndex - 1] }

    val trashName = MirrorEngine.TRASH_DIR

    // Anderer PC unter demselben Server: Rückfrage VOR dem Senden der Zugangsdaten (nur "Verbindung testen")
    trustAsk?.let { ask ->
        AlertDialog(
            onDismissRequest = { ask.answer.complete(false) },
            title = { Text(str(R.string.profile_pc_trust_title)) },
            text = { DialogScroll { Text(str(R.string.profile_pc_trust_body, ask.host)) } },
            confirmButton = { TextButton(onClick = { ask.answer.complete(true) }) { Text(str(R.string.profile_pc_trust_yes)) } },
            dismissButton = { TextButton(onClick = { ask.answer.complete(false) }) { Text(str(R.string.profile_pc_trust_no)) } },
        )
    }

    // Hostwechsel bei bestehendem Profil: Rückfrage VOR dem Senden der Zugangsdaten (-079)
    hostAsk?.let { ask ->
        AlertDialog(
            onDismissRequest = { hostAsk = null },
            title = { Text(str(R.string.profile_host_change_title)) },
            text = { DialogScroll { Text(str(R.string.profile_host_change_body, ask.oldHost, ask.newHost)) } },
            confirmButton = { TextButton(onClick = { confirmedHost = ask.newHost; hostAsk = null; ask.action() }) { Text(str(R.string.profile_host_change_yes)) } },
            dismissButton = { TextButton(onClick = { hostAsk = null }) { Text(str(R.string.common_cancel)) } },
        )
    }

    ScreenScaffold(
        title = str(if (profileId == 0L) R.string.profile_title_new else R.string.profile_title_edit),
        onBack = { if (wizard && typeChosen && stepIndex > 0) { showMissing = false; step = steps[stepIndex - 1] } else leave() },
        actions = {
            // Im Assistenten erst ab den optionalen Schritten speichern (vorher fehlen Pflichtangaben)
            if (loaded && typeChosen && (!wizard || step.optional)) IconButton(onClick = { save() }, enabled = p.name.isNotBlank()) {
                Icon(Icons.Filled.Check, str(R.string.common_save))
            }
            // Löschen liegt seit 0.9.0 unten unter "Wartung" (nicht mehr direkt neben Speichern)
        },
        // Beim Bearbeiten: Speichern/Abbrechen fest unten (0.9.0), statt mitten auf der langen Seite
        bottomBar = {
            if (loaded && typeChosen && !wizard) Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { leave() }, modifier = Modifier.weight(1f)) { Text(str(R.string.common_cancel)) }
                    Button(onClick = { save() }, enabled = p.name.isNotBlank(), modifier = Modifier.weight(1f)) {
                        Text(str(R.string.common_save))
                    }
                }
            }
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
                // Im Assistenten ohne Überschrift und ohne "Profil aktiv" (ein neues Profil ist immer aktiv)
                SectionCard(if (wizard) null else str(R.string.profile_general)) {
                    OutlinedTextField(p.name, { p = p.copy(name = it) }, label = { Text(str(R.string.profile_name)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (!wizard) SwitchRow(str(R.string.profile_active), p.enabled) { p = p.copy(enabled = it) }
                    Hint(str(if (isMirror) R.string.profile_type_mirror else R.string.profile_type_photos, p.type().label))
                }
            }

            if (isMirror && show(Step.SOURCE)) item {
                SectionCard(str(R.string.profile_local_folder)) {
                    val treeUri = p.localTreeUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    if (treeUri == null) HintMore(str(R.string.profile_no_folder_yet))
                    else {
                        Text(LocalTree.label(ctx, treeUri))
                        if (!LocalTree.hasPermission(ctx, treeUri))
                            Text(str(R.string.profile_access_lost), color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedButton(onClick = { treePicker.launch(null) }) {
                        Text(str(if (treeUri == null) R.string.profile_choose_folder else R.string.profile_choose_other_folder))
                    }
                    HintMore(str(R.string.profile_local_folder_hint))
                }
            }

            if (!isMirror && show(Step.SOURCE)) item {
                SectionCard(str(R.string.profile_sources)) {
                    // Medienzugriff erst hier erfragen, wo er gebraucht wird (0.11.0)
                    PermissionRows(perms, showDone = false, needs = PermNeeds(media = true, wifi = false, base = false))
                    if (p.folders().isEmpty()) HintMore(str(R.string.profile_sources_empty))
                    p.folders().forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f, Modifier.weight(1f))
                            IconButton(onClick = { p = p.copy(sourceFolders = Profile.joinLines(p.folders() - f)) }) {
                                Icon(Icons.Filled.Close, str(R.string.profile_remove))
                            }
                        }
                    }
                    OutlinedButton(onClick = { folderPicker.launch(null) }) { Text(str(R.string.profile_add_folder)) }
                    folderError?.let { ErrorText(it) }
                    HintMore(str(R.string.profile_sources_hint))
                    SwitchRow(str(R.string.profile_subfolders), p.includeSubfolders,
                        subtitle = str(R.string.profile_subfolders_sub)) { p = p.copy(includeSubfolders = it) }
                    SwitchRow(str(R.string.profile_photos), p.includeImages) { p = p.copy(includeImages = it) }
                    SwitchRow(str(R.string.profile_videos), p.includeVideos) { p = p.copy(includeVideos = it) }
                }
            }

            if (show(Step.TARGET)) item {
                SectionCard(str(R.string.profile_target)) {
                    // 0.10.0: ein Knopf statt eines Chips pro Profil; gleiche Zugangsdaten erscheinen nur einmal
                    if (credentialSources.isNotEmpty()) OutlinedButton(onClick = { showCredentialPicker = true }) {
                        Text(str(R.string.profile_copy_credentials_button))
                    }
                    OutlinedTextField(
                        p.smbPath, { p = p.copy(smbPath = it) },
                        label = { Text(str(R.string.profile_share_path)) },
                        placeholder = { Text("\\\\192.168.1.20\\" + str(R.string.profile_share_example)) },
                        isError = p.smbPath.isNotBlank() && SmbTarget.parse(p.smbPath) == null,
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    HintMore(str(R.string.profile_share_format))
                    // 0.11.0: kein eigenes Unterordner-Feld mehr; Freigabe und Ordner wählt man unten im PC-Browser
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
                    HintMore(str(R.string.profile_domain_hint))
                    // Freigabe und Ordner auf dem PC auswählen (0.11.0): braucht nur Server und Zugangsdaten
                    OutlinedButton(
                        enabled = SmbTarget.hostOf(p.smbPath) != null && p.smbUser.isNotBlank(),
                        onClick = { confirmHost { showRemotePicker = true } },
                    ) { Text(str(R.string.profile_pick_pc_folder)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(enabled = !testing, onClick = { confirmHost { runTestNow() } }) { Text(str(R.string.profile_test)) }
                        if (testing) CircularProgressIndicator(Modifier.padding(start = 12.dp))
                    }
                    testResult?.let { if (testFailed) ErrorText(it) else Text(it) }
                    testDetail?.let { ErrorDetails(it) }
                    encLine?.let { Text(it) }
                    if (encHelp) TextButton(onClick = {
                        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(encryptionHelpUrl())))
                    }) { Text(str(R.string.enc_help)) }
                }
            }

            if (!isMirror && show(Step.TARGET)) item {
                FolderLayoutCard(
                    layout = p.layout(),
                    lockedText = when {
                        layoutLocked -> str(R.string.profile_layout_locked)
                        syncRunning -> str(R.string.profile_layout_running)
                        else -> null
                    },
                    adoptedFrom = layoutOwner?.takeIf { layoutEditable }?.name,
                    // Bei gleichem Ziel gilt die Vorlage des Eigentümers: Auswahl gesperrt (Anzeige mit Hinweis)
                    pickable = layoutOwner == null || !layoutEditable,
                    onPick = { ownLayout = it; p = p.copy(folderLayout = it.code) },
                )
            }

            if (show(Step.OPTIONS)) item {
                SectionCard(str(R.string.profile_pc_check)) {
                    val mode = pcModeNow()
                    HintMore(str(R.string.profile_pc_check_hint))
                    listOf(
                        Triple(PcCheckMode.WIFI, R.string.profile_pc_opt_wifi, R.string.profile_pc_opt_wifi_sub),
                        Triple(PcCheckMode.GUID, R.string.profile_pc_opt_guid, R.string.profile_pc_opt_guid_sub),
                        Triple(PcCheckMode.BOTH, R.string.profile_pc_opt_both, R.string.profile_pc_opt_both_sub),
                    ).forEach { (m, title, sub) ->
                        Row(Modifier.fillMaxWidth().clickable { setPcMode(m) }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mode == m, onClick = { setPcMode(m) })
                            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                                Text(str(title))
                                Hint(str(sub))
                            }
                        }
                    }
                    val host = SmbTarget.hostOf(p.smbPath).orEmpty()
                    val known = com.sync2pc.app.sync.PcIdentity.storedFor(p.pcGuid, p.pcGuidHost, host) != null
                    Text(
                        str(when {
                            !mode.checksGuid -> R.string.profile_pc_status_wifi
                            pcChanged -> R.string.profile_pc_status_changed
                            known -> R.string.profile_pc_status_known
                            p.id == 0L -> R.string.profile_pc_status_new
                            else -> R.string.profile_pc_status_unknown
                        }),
                        color = if (pcChanged && mode.checksGuid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Verschlüsselung (-146 Teil B): direkt unter «PC-Kennung», standardmässig eingeklappt
            if (show(Step.OPTIONS)) item {
                val enc = p.encryption()
                CollapsibleCard(
                    str(R.string.enc_title), startOpen = false, closedHint = str(R.string.enc_intro),
                    titleSuffix = str(when (enc) {
                        SmbEncryption.OFF -> R.string.enc_value_off
                        SmbEncryption.PREFERRED -> R.string.enc_value_preferred
                        SmbEncryption.REQUIRED -> R.string.enc_value_required
                    }),
                    openSignal = encOpenSignal,
                ) {
                    HintMore(str(R.string.enc_intro))
                    listOf(
                        Triple(SmbEncryption.OFF, R.string.enc_off, R.string.enc_off_sub),
                        Triple(SmbEncryption.PREFERRED, R.string.enc_preferred, R.string.enc_preferred_sub),
                        Triple(SmbEncryption.REQUIRED, R.string.enc_required, R.string.enc_required_sub),
                    ).forEach { (m, title, sub) ->
                        Row(Modifier.fillMaxWidth().clickable { p = p.copy(smbEncryption = m.name) }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = enc == m, onClick = { p = p.copy(smbEncryption = m.name) })
                            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                                Text(str(title))
                                Hint(str(sub))
                            }
                        }
                    }
                }
            }

            if (show(Step.OPTIONS) && com.sync2pc.app.sync.NetworkRules.needsWifiName(pcModeNow())) item {
                SectionCard(str(R.string.profile_wifi)) {
                    // Standort nur, wenn ein WLAN eingetragen ist bzw. werden soll (Android braucht ihn für den WLAN-Namen)
                    PermissionRows(perms, showDone = false, needs = PermNeeds(media = false, wifi = true, base = false))
                    HintMore(str(R.string.profile_wifi_hint))
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
                MirrorOptionsCard(pState, trashState, pollState, trashName)
            }

            if (!isMirror && show(Step.OPTIONS)) item {
                PhotoOptionsCard(pState) { confirmFullVerify = true }
            }

            // Optionale Bereiche: beim Bearbeiten zugeklappt, solange nichts eingestellt ist (0.9.0)
            if (show(Step.FILTER)) item {
                // P9: Kurzfassung der eingeklappten Karte; ungültiges Muster (nicht geschlossene Klammer) zeigt einen Fehlertext
                val badInclude = FileFilter.unbalancedPattern(p.includePatterns)
                val badExclude = FileFilter.unbalancedPattern(p.excludePatterns)
                val filterSummary = listOfNotNull(
                    p.includePatterns.takeIf { it.isNotBlank() }
                        ?.let { str(R.string.profile_filter_summary_include, FileFilter.split(it).joinToString(", ")) },
                    FileFilter.split(p.excludePatterns).size.takeIf { it > 0 }
                        ?.let { str(R.string.profile_filter_summary_exclude, it) },
                ).joinToString(" · ").ifEmpty { str(R.string.profile_section_unset) }
                OptionalCard(
                    str(R.string.profile_filter),
                    startOpen = wizard || p.includePatterns.isNotBlank() || p.excludePatterns.isNotBlank(),
                    collapsible = !wizard,
                    closedHint = filterSummary,
                ) {
                    OutlinedTextField(
                        p.includePatterns, { p = p.copy(includePatterns = it) },
                        label = { Text(str(R.string.profile_filter_include)) },
                        placeholder = { Text("*.pdf, *.docx") },
                        isError = badInclude != null,
                        supportingText = when {
                            badInclude != null -> {{ Text(str(R.string.profile_filter_bad, badInclude)) }}
                            p.includePatterns.isNotBlank() -> {{ Text(str(R.string.profile_filter_include_hint)) }}
                            else -> null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        p.excludePatterns, { p = p.copy(excludePatterns = it) },
                        label = { Text(str(R.string.profile_filter_exclude)) },
                        placeholder = { Text("Thumbs.db, *.tmp, Temp/**") },
                        isError = badExclude != null,
                        supportingText = if (badExclude != null) {{ Text(str(R.string.profile_filter_bad, badExclude)) }} else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HintMore(str(R.string.profile_filter_hint))
                }
            }

            // Schutzbremse (0.4.2): letzte der optionalen Einstellungen (so auch im Profil-Assistenten ab 0.5.0)
            if (isMirror && show(Step.GUARD)) item {
                OptionalCard(
                    str(R.string.profile_guard),
                    startOpen = wizard || p.massDeletePercent != Profile().massDeletePercent,
                    collapsible = !wizard,
                ) {
                    OutlinedTextField(
                        guardText,
                        { v ->
                            guardText = NumberInput.filter(v, 3)
                            NumberInput.parse(guardText, NumberInput.GUARD_PERCENT.first, NumberInput.GUARD_PERCENT.last)
                                ?.let { p = p.copy(massDeletePercent = it) }
                        },
                        label = { Text(str(R.string.profile_guard_percent)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !NumberInput.isValid(guardText, NumberInput.GUARD_PERCENT),
                        supportingText = rangeError(guardText, NumberInput.GUARD_PERCENT),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    HintMore(str(R.string.profile_guard_hint))
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
                                else if (ProfileWizard.numberBlocksStep(p, step, trashText, pollText, guardText)) {
                                    // -120: Fehlertext steht schon unter dem Feld; zusätzlich Hinweis, kein stilles Korrigieren
                                    showMissing = false
                                    Toast.makeText(ctx, str(R.string.profile_number_step_blocked), Toast.LENGTH_LONG).show()
                                }
                                else { showMissing = false; step = steps[stepIndex + 1] }
                            }
                            if (step.optional) OutlinedButton(onClick = next) { Text(str(R.string.profile_next)) }
                            else Button(onClick = next) { Text(str(R.string.profile_next)) }
                        }
                        if (step.optional || stepIndex == steps.lastIndex) Button(onClick = { save() }) { Text(str(R.string.common_save)) }
                    }
                    if (step.optional && stepIndex < steps.lastIndex)
                        HintMore(str(R.string.profile_optional_hint))
                }
            }

            // Simulation dieses Profils (nur gespeicherte Profile; simuliert wird der gespeicherte Stand)
            if (profileId != 0L) item {
                SectionCard(str(R.string.profile_simulation)) {
                    HintMore(str(R.string.profile_simulation_hint))
                    OutlinedButton(onClick = { onSimulate(profileId) }, enabled = !dirty) { Text(str(R.string.profile_simulate)) }
                    if (dirty) HintMore(str(R.string.profile_simulation_save_first))
                }
            }

            // Wartung (zugeklappt): Sync-Status zurücksetzen und Profil löschen – beides weit weg von "Speichern"
            if (profileId != 0L) item {
                OptionalCard(str(R.string.profile_maintenance), startOpen = false, collapsible = true,
                    closedHint = str(R.string.profile_maintenance_closed)) {
                    HintMore(str(R.string.profile_maintenance_hint))
                    OutlinedButton(onClick = { confirmReset = true }) { Text(str(R.string.profile_reset)) }
                    HintMore(str(R.string.profile_delete_section_hint))
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(8.dp)); Text(str(R.string.profile_delete_button))
                    }
                }
            }
        }
    }

    if (showRemotePicker) RemoteFolderPicker(
        profile = { withPassword() },
        onDismiss = { showRemotePicker = false },
        // Ergebnis ist der ganze Pfad \\Server\Freigabe\Ordner (0.11.0, ohne separaten Unterordner)
        onPick = { p = p.copy(smbPath = it, remoteSubfolder = ""); testResult = null; testDetail = null; showRemotePicker = false },
    )

    // V1: Änderung von Richtung / Löschungen bestätigen (nur «Abbrechen» und «Speichern»)
    changeAsk?.let { ask ->
        AlertDialog(
            onDismissRequest = { changeAsk = null },
            title = { Text(str(R.string.profile_confirm_title)) },
            text = { DialogScroll { Text(ask.text) } },
            confirmButton = {
                TextButton(onClick = { changeAsk = null; confirmedChange = ask.signature; save(ask.force, ask.folderChoice) }) {
                    Text(str(R.string.common_save))
                }
            },
            dismissButton = { TextButton(onClick = { changeAsk = null }) { Text(str(R.string.common_cancel)) } },
        )
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(str(R.string.profile_delete_title)) },
        text = { DialogScroll { Text(str(R.string.profile_delete_text)) } },
        confirmButton = {
            TextButton(onClick = {
                // 0.11.0: erst vormerken; die Startseite zeigt "Profil gelöscht – Rückgängig"
                PendingProfileDelete.request(ctx, profileId, p.name.trim())
                confirmDelete = false; onBack()
            }) { Text(str(R.string.common_delete)) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(str(R.string.common_cancel)) } },
    )

    // Nach dem Anlegen: zuerst simulieren, gleich synchronisieren oder später (bis dahin keine Automatik)
    createdId?.let { id ->
        // Kennung wird gerade gelernt (V5): kurze Anzeige, damit ein sofortiger Start nicht an «PC unbekannt» scheitert
        val learning by com.sync2pc.app.sync.PcLearner.running.collectAsState()
        AlertDialog(
            onDismissRequest = { createdId = null; onBack() },
            title = { Text(str(R.string.profile_created_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(str(R.string.profile_created_text))
                    if (id in learning) Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(str(R.string.profile_pc_learning), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                DialogButtonColumn {
                    StackedButton(str(R.string.home_sync)) {
                        createdId = null
                        scope.launch {
                            // Neues Profil = erste Synchronisierung -> ohne Ladegerät vorher warnen
                            if (!FirstSync.isCharging(ctx)) chargeWarnFor = id
                            else { SyncScheduler.enqueueManual(ctx); onBack() }
                        }
                    }
                    StackedButton(str(R.string.profile_later)) { createdId = null; onBack() }
                    StackedButton(str(R.string.profile_simulate)) { createdId = null; onSimulate(id) }
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
        // R1: drei Knöpfe gestapelt, von oben Speichern, Verwerfen, Abbrechen
        confirmButton = {
            DialogButtonColumn {
                StackedButton(str(R.string.common_save)) { askLeave = false; save() }
                StackedButton(str(R.string.home_discard)) { askLeave = false; onBack() }
                StackedButton(str(R.string.common_cancel)) { askLeave = false }
            }
        },
    )

    overlap?.let { hit ->
        // IDENTICAL und BLOCKED (andere Ordnerstruktur bei gleichem Ziel, SYNC-20261109-083): nicht speicherbar
        val identical = hit.kind != ProfileOverlap.Kind.PARTIAL
        val blocked = hit.kind == ProfileOverlap.Kind.BLOCKED
        AlertDialog(
            onDismissRequest = { overlap = null },
            title = { Text(str(when {
                blocked -> R.string.profile_layout_blocked_title
                identical -> R.string.profile_exists_title
                else -> R.string.profile_overlap_title
            })) },
            text = {
                // BLOCKED: roter Fehler (DEC-207) mit Namen des anderen Profils und seiner Struktur (DEC-189), kein «Trotzdem speichern»
                DialogScroll {
                if (blocked) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ErrorText(hit.message())
                    Text(str(R.string.profile_not_saved))
                } else Text(hit.message() + if (identical) "\n\n" + str(R.string.profile_not_saved) else "")
                }
            },
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
            text = {
                DialogScroll(Arrangement.spacedBy(8.dp)) {
                    Text(str(R.string.profile_folder_warning_text))
                    // P10: Wirkung der Optionen erklären
                    HintMore(str(R.string.profile_folder_warning_options))
                }
            },
            confirmButton = {
                DialogButtonColumn {
                    StackedButton(str(R.string.guard_choose_other)) {
                        folderWarning = null
                        if (Side.LOCAL in w.sides) treePicker.launch(null) else showRemotePicker = true
                    }
                    StackedButton(str(R.string.guard_accept)) { folderWarning = null; save(w.force, FolderChoice.ACCEPT) }
                    StackedButton(str(R.string.guard_as_new)) { folderWarning = null; save(w.force, FolderChoice.RESET) }
                    StackedButton(str(R.string.common_cancel)) { folderWarning = null }
                }
            },
        )
    }

    // Zugangsdaten übernehmen (0.10.0): Auswahl aus den zusammengefassten Zugangsdaten der anderen Profile
    if (showCredentialPicker) {
        val groups = remember(credentialSources) {
            CredentialGroups.group(credentialSources) { CredentialCrypto.decrypt(it.smbPasswordEnc) }
        }
        AlertDialog(
            onDismissRequest = { showCredentialPicker = false },
            title = { Text(str(R.string.profile_copy_credentials_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HintMore(str(R.string.profile_copy_credentials_hint))
                    groups.forEach { g ->
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                val src = g.source
                                p = p.copy(
                                    smbUser = src.smbUser,
                                    smbDomain = src.smbDomain,
                                    // Server/Freigabe nur vorschlagen, wenn noch leer
                                    smbPath = p.smbPath.ifBlank { src.smbPath },
                                )
                                password = CredentialCrypto.decrypt(src.smbPasswordEnc)
                                testResult = null
                                showCredentialPicker = false
                                Toast.makeText(ctx, str(R.string.profile_credentials_copied, g.user), Toast.LENGTH_SHORT).show()
                            }.padding(vertical = 8.dp),
                        ) {
                            val who = if (g.domain.isBlank()) g.user else "${g.domain}\\${g.user}"
                            Text(if (g.host.isBlank()) who else "$who · ${g.host}", style = MaterialTheme.typography.bodyLarge)
                            HintMore(str(R.string.profile_copy_credentials_profiles, g.profileNames.joinToString(", ")))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCredentialPicker = false }) { Text(str(R.string.common_cancel)) } },
        )
    }

    // Prüfstufe "Komplett" (0.9.0): einmal bestätigen lassen, weil sie viel Zeit und Akku braucht
    if (confirmFullVerify) AlertDialog(
        onDismissRequest = { confirmFullVerify = false },
        title = { Text(str(R.string.verify_full_title)) },
        text = { Text(str(R.string.verify_full_text)) },
        confirmButton = {
            TextButton(onClick = { confirmFullVerify = false; p = p.copy(verifyLevel = VerifyLevel.FULL.code) }) {
                Text(str(R.string.verify_full_confirm))
            }
        },
        dismissButton = { TextButton(onClick = { confirmFullVerify = false }) { Text(str(R.string.common_cancel)) } },
    )

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(str(R.string.profile_reset_title)) },
        text = { DialogScroll { Text(str(R.string.profile_reset_text)) } },
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

/** Optionaler Bereich: beim Bearbeiten aufklappbar, im Assistenten eine normale Karte (0.9.0) */
@Composable
private fun OptionalCard(
    title: String,
    startOpen: Boolean,
    collapsible: Boolean,
    closedHint: String = str(R.string.profile_section_unset),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (collapsible) CollapsibleCard(title, startOpen, closedHint, content = content)
    else SectionCard(title, content = content)
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
    val docId = try { DocumentsContract.getTreeDocumentId(uri) } catch (e: Exception) { return null }
    if (uri.authority == "com.android.providers.downloads.documents") {
        // Über "Downloads" gewählt: "raw:/storage/emulated/0/Download/Test" -> "Download/Test/" (interner Speicher)
        val raw = docId.removePrefix("raw:").takeIf { docId.startsWith("raw:") } ?: return null
        val rest = Regex("""^/storage/emulated/\d+/?""").replace(raw, "").trim('/')
        return if (rest.isEmpty() || rest == raw.trim('/')) null else "$rest/"
    }
    if (uri.authority != "com.android.externalstorage.documents") return null
    val path = docId.substringAfter(':', "").trim('/')
    return if (path.isEmpty()) null else "$path/"
}

/** Warum [treeUriToRelativePath] null liefert (Text-Ressource): oberster Ordner des Speichers oder nicht unterstützter Anbieter */
fun treeUriProblem(uri: Uri): Int {
    val docId = try { DocumentsContract.getTreeDocumentId(uri) } catch (e: Exception) { return R.string.profile_folder_unusable }
    return when {
        uri.authority == "com.android.externalstorage.documents" && docId.substringAfter(':', "").trim('/').isEmpty() ->
            R.string.profile_folder_unusable_root
        uri.authority == "com.android.providers.downloads.documents" && docId.startsWith("raw:") &&
            Regex("""^/storage/emulated/\d+/?$""").matches(docId.removePrefix("raw:")) -> R.string.profile_folder_unusable_root
        uri.authority == "com.android.providers.downloads.documents" && docId.startsWith("raw:") -> R.string.profile_folder_unusable
        uri.authority == "com.android.providers.downloads.documents" -> R.string.profile_folder_unusable_provider
        else -> R.string.profile_folder_unusable
    }
}

/** Offene Bestätigung V1: Text, Kennung der bestätigten Änderung und die Parameter des unterbrochenen Speicherns */
private class ChangeAsk(val text: String, val signature: String, val force: Boolean, val folderChoice: FolderChoice?)

/** Offene Rückfrage zum Neuvertrauen eines anderen PCs (Antwort kommt vom Dialog, wartet der IO-Thread) */
private class HostAsk(val oldHost: String, val newHost: String, val action: () -> Unit)
private class TrustAsk(val host: String, val answer: java.util.concurrent.CompletableFuture<Boolean>)
