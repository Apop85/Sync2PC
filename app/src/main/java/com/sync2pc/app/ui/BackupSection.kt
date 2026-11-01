package com.sync2pc.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sync2pc.app.backup.BackupCodec
import com.sync2pc.app.backup.BackupException
import com.sync2pc.app.backup.BackupManager
import com.sync2pc.app.backup.ParsedBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Einstellungen → Sicherung: Profile, Einstellungen und Sync-Status exportieren/importieren. */
@Composable
fun BackupSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }

    // --- Export ----------------------------------------------------------------------
    var exportDialog by remember { mutableStateOf(false) }
    // rememberSaveable: Android kann die App während der Dateiauswahl neu aufbauen – Auswahl nicht verlieren
    var includeSync by rememberSaveable { mutableStateOf(true) }
    var pw1 by rememberSaveable { mutableStateOf("") }
    var pw2 by rememberSaveable { mutableStateOf("") }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = "Exportiere…"
        scope.launch {
            try {
                val r = BackupManager.export(ctx, uri, includeSync, pw1.ifEmpty { null })
                Toast.makeText(ctx, "Gesichert: ${r.profiles} Profil(e)" +
                    (if (includeSync) ", ${r.synced} Dateien Sync-Status" else "") +
                    (if (r.withPasswords) ", Passwörter verschlüsselt" else ", ohne Passwörter"), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, "Export fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                busy = null; pw1 = ""; pw2 = ""
            }
        }
    }

    // --- Import ----------------------------------------------------------------------
    var importJson by remember { mutableStateOf<String?>(null) }
    var askPassword by remember { mutableStateOf(false) }
    var importPw by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<ParsedBackup?>(null) }
    var result by remember { mutableStateOf<String?>(null) }

    fun parse(json: String, password: String?) {
        busy = "Lese Sicherung…"
        scope.launch {
            try {
                // PBKDF2 braucht bewusst etwas Rechenzeit -> nicht im UI-Thread
                parsed = withContext(Dispatchers.Default) { BackupCodec.parse(json, password) }
            } catch (e: Exception) {
                Toast.makeText(ctx, e.message ?: "Sicherung nicht lesbar", Toast.LENGTH_LONG).show()
                importJson = null
            } finally {
                busy = null
            }
        }
    }

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = BackupManager.read(ctx, uri)
                importJson = json
                if (BackupCodec.isEncrypted(json)) { importPw = ""; askPassword = true } else parse(json, null)
            } catch (e: Exception) {
                Toast.makeText(ctx, (e as? BackupException)?.message ?: "Datei nicht lesbar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun runImport(b: ParsedBackup, replace: Boolean) {
        parsed = null
        busy = "Importiere…"
        scope.launch {
            try {
                val r = BackupManager.import(ctx, b, replace)
                result = buildString {
                    append("${r.profiles} Profil(e) importiert")
                    if (r.synced > 0) append(", Sync-Status für ${r.synced} Dateien")
                    append(".")
                    if (r.skippedIdentical.isNotEmpty())
                        append("\n\nÜbersprungen, weil schon vorhanden (gleiches Ziel und gleiche Quellen): ${r.skippedIdentical.joinToString(", ")}")
                    if (r.overlapping.isNotEmpty())
                        append("\n\nHinweis: Überschneidet sich mit einem bestehenden Profil (gleiches Ziel, gemeinsame Quellordner): " +
                            r.overlapping.joinToString(", "))
                    if (r.withoutPassword.isNotEmpty())
                        append("\n\nPasswort neu eingeben bei: ${r.withoutPassword.joinToString(", ")}")
                    if (r.needFolder.isNotEmpty())
                        append("\n\nHandy-Ordner neu wählen bei: ${r.needFolder.joinToString(", ")} " +
                            "(Android überträgt die Ordner-Freigabe nicht).")
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "Import fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                busy = null; importJson = null
            }
        }
    }

    // --- Anzeige ---------------------------------------------------------------------
    SectionCard("Sicherung") {
        Hint("Sichert Profile, Einstellungen, Ordner-Zuordnungen und auf Wunsch den Sync-Status in eine Datei – " +
            "z. B. vor einem Handywechsel oder einer Neuinstallation.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = busy == null, onClick = { exportDialog = true }) { Text("Exportieren…") }
            OutlinedButton(enabled = busy == null, onClick = { openDoc.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                Text("Importieren…")
            }
        }
        busy?.let { Hint(it) }
    }

    if (exportDialog) {
        val mismatch = pw1 != pw2
        AlertDialog(
            onDismissRequest = { exportDialog = false },
            title = { Text("Sicherung exportieren") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwitchRow("Sync-Status mitsichern", includeSync,
                        subtitle = "Nach dem Import ist sofort bekannt, welche Dateien schon auf dem PC sind.") { includeSync = it }
                    OutlinedTextField(pw1, { pw1 = it }, label = { Text("Export-Passwort (optional)") },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (pw1.isNotEmpty()) OutlinedTextField(pw2, { pw2 = it }, label = { Text("Export-Passwort wiederholen") },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, isError = mismatch,
                        modifier = Modifier.fillMaxWidth())
                    Hint(if (pw1.isEmpty()) "Ohne Export-Passwort enthält die Datei keine SMB-Passwörter; du gibst sie nach dem Import neu ein."
                    else "Die SMB-Passwörter werden mit diesem Passwort verschlüsselt. Ohne es lassen sie sich nicht wiederherstellen.")
                }
            },
            confirmButton = {
                TextButton(enabled = pw1.isEmpty() || !mismatch, onClick = {
                    exportDialog = false
                    createDoc.launch(BackupManager.suggestedFileName())
                }) { Text("Datei wählen…") }
            },
            dismissButton = { TextButton(onClick = { exportDialog = false; pw1 = ""; pw2 = "" }) { Text("Abbrechen") } },
        )
    }

    if (askPassword) {
        AlertDialog(
            onDismissRequest = { askPassword = false; importJson = null },
            title = { Text("Export-Passwort") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Hint("Die Sicherung enthält verschlüsselte SMB-Passwörter. Ohne Export-Passwort werden die Profile " +
                        "trotzdem importiert, die Passwörter gibst du dann neu ein.")
                    OutlinedTextField(importPw, { importPw = it }, label = { Text("Export-Passwort") },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { askPassword = false; importJson?.let { parse(it, importPw) } }) { Text("Weiter") }
            },
            dismissButton = {
                TextButton(onClick = { askPassword = false; importJson?.let { parse(it, null) } }) { Text("Ohne Passwort") }
            },
        )
    }

    parsed?.let { b ->
        val c = b.content
        AlertDialog(
            onDismissRequest = { parsed = null; importJson = null },
            title = { Text("Sicherung importieren") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Erstellt: ${formatTime(c.createdAt)} (LeoSync ${c.appVersion})")
                    Text("Profile: " + c.profiles.joinToString(", ") { it.name }.ifEmpty { "keine" })
                    if (c.synced.isNotEmpty()) Text("Sync-Status: ${c.synced.size} Dateien")
                    when (b.passwordOk) {
                        true -> Text("SMB-Passwörter: werden übernommen")
                        false -> Text("SMB-Passwörter: Export-Passwort fehlt oder ist falsch – Passwörter werden nicht übernommen")
                        null -> Text("SMB-Passwörter: nicht enthalten")
                    }
                    Hint("„Ersetzen“ löscht alle bisherigen Profile samt Sync-Status und übernimmt auch die Einstellungen. " +
                        "„Hinzufügen“ legt die Profile zusätzlich an.")
                }
            },
            confirmButton = { TextButton(onClick = { runImport(b, replace = true) }) { Text("Ersetzen") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { parsed = null; importJson = null }) { Text("Abbrechen") }
                    TextButton(onClick = { runImport(b, replace = false) }) { Text("Hinzufügen") }
                }
            },
        )
    }

    result?.let { msg ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text("Import abgeschlossen") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { result = null }) { Text("OK") } },
        )
    }
}
