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
import com.sync2pc.app.R
import com.sync2pc.app.backup.BackupCodec
import com.sync2pc.app.backup.BackupException
import com.sync2pc.app.backup.BackupManager
import com.sync2pc.app.backup.ParsedBackup
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
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
        busy = str(R.string.backup_exporting)
        scope.launch {
            try {
                val r = BackupManager.export(ctx, uri, includeSync, pw1.ifEmpty { null })
                Toast.makeText(ctx, plural(R.plurals.backup_exported, r.profiles) +
                    (if (includeSync) ", " + plural(R.plurals.backup_exported_sync, r.synced) else "") +
                    ", " + str(if (r.withPasswords) R.string.backup_passwords_encrypted else R.string.backup_without_passwords),
                    Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, str(R.string.log_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
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
        busy = str(R.string.backup_reading)
        scope.launch {
            try {
                // PBKDF2 braucht bewusst etwas Rechenzeit -> nicht im UI-Thread
                parsed = withContext(Dispatchers.Default) { BackupCodec.parse(json, password) }
            } catch (e: Exception) {
                Toast.makeText(ctx, e.message ?: str(R.string.backup_unreadable), Toast.LENGTH_LONG).show()
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
                Toast.makeText(ctx, (e as? BackupException)?.message ?: str(R.string.backup_file_unreadable, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun runImport(b: ParsedBackup, replace: Boolean) {
        parsed = null
        busy = str(R.string.backup_importing)
        scope.launch {
            try {
                val r = BackupManager.import(ctx, b, replace)
                result = buildString {
                    append(plural(R.plurals.backup_imported, r.profiles))
                    if (r.synced > 0) append(", ").append(plural(R.plurals.backup_imported_sync, r.synced))
                    append(".")
                    if (r.skippedIdentical.isNotEmpty())
                        append("\n\n").append(str(R.string.backup_skipped_identical, r.skippedIdentical.joinToString(", ")))
                    if (r.overlapping.isNotEmpty())
                        append("\n\n").append(str(R.string.backup_overlapping, r.overlapping.joinToString(", ")))
                    if (r.withoutPassword.isNotEmpty())
                        append("\n\n").append(str(R.string.backup_without_password, r.withoutPassword.joinToString(", ")))
                    if (r.needFolder.isNotEmpty())
                        append("\n\n").append(str(R.string.backup_need_folder, r.needFolder.joinToString(", ")))
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, str(R.string.backup_import_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
            } finally {
                busy = null; importJson = null
            }
        }
    }

    // --- Anzeige ---------------------------------------------------------------------
    SectionCard(str(R.string.backup_title)) {
        Hint(str(R.string.backup_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = busy == null, onClick = { exportDialog = true }) { Text(str(R.string.backup_export)) }
            OutlinedButton(enabled = busy == null, onClick = { openDoc.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                Text(str(R.string.backup_import))
            }
        }
        busy?.let { Hint(it) }
    }

    if (exportDialog) {
        val mismatch = pw1 != pw2
        AlertDialog(
            onDismissRequest = { exportDialog = false },
            title = { Text(str(R.string.backup_export_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwitchRow(str(R.string.backup_include_sync), includeSync,
                        subtitle = str(R.string.backup_include_sync_sub)) { includeSync = it }
                    OutlinedTextField(pw1, { pw1 = it }, label = { Text(str(R.string.backup_password_optional)) },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (pw1.isNotEmpty()) OutlinedTextField(pw2, { pw2 = it }, label = { Text(str(R.string.backup_password_repeat)) },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, isError = mismatch,
                        modifier = Modifier.fillMaxWidth())
                    Hint(str(if (pw1.isEmpty()) R.string.backup_no_password_hint else R.string.backup_password_hint))
                }
            },
            confirmButton = {
                TextButton(enabled = pw1.isEmpty() || !mismatch, onClick = {
                    exportDialog = false
                    createDoc.launch(BackupManager.suggestedFileName())
                }) { Text(str(R.string.backup_choose_file)) }
            },
            dismissButton = { TextButton(onClick = { exportDialog = false; pw1 = ""; pw2 = "" }) { Text(str(R.string.common_cancel)) } },
        )
    }

    if (askPassword) {
        AlertDialog(
            onDismissRequest = { askPassword = false; importJson = null },
            title = { Text(str(R.string.backup_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Hint(str(R.string.backup_ask_password_hint))
                    OutlinedTextField(importPw, { importPw = it }, label = { Text(str(R.string.backup_password)) },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { askPassword = false; importJson?.let { parse(it, importPw) } }) { Text(str(R.string.profile_next)) }
            },
            dismissButton = {
                TextButton(onClick = { askPassword = false; importJson?.let { parse(it, null) } }) { Text(str(R.string.backup_without_password_button)) }
            },
        )
    }

    parsed?.let { b ->
        val c = b.content
        AlertDialog(
            onDismissRequest = { parsed = null; importJson = null },
            title = { Text(str(R.string.backup_import_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(str(R.string.backup_created, formatTime(c.createdAt), c.appVersion))
                    Text(str(R.string.backup_profiles, c.profiles.joinToString(", ") { it.name }.ifEmpty { str(R.string.home_profile_none) }))
                    if (c.synced.isNotEmpty()) Text(plural(R.plurals.backup_sync_status, c.synced.size))
                    Text(str(when (b.passwordOk) {
                        true -> R.string.backup_pw_taken
                        false -> R.string.backup_pw_wrong
                        null -> R.string.backup_pw_none
                    }))
                    Hint(str(R.string.backup_replace_hint))
                }
            },
            confirmButton = { TextButton(onClick = { runImport(b, replace = true) }) { Text(str(R.string.backup_replace)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { parsed = null; importJson = null }) { Text(str(R.string.common_cancel)) }
                    TextButton(onClick = { runImport(b, replace = false) }) { Text(str(R.string.profile_add)) }
                }
            },
        )
    }

    result?.let { msg ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text(str(R.string.backup_import_done)) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { result = null }) { Text(str(R.string.common_ok)) } },
        )
    }
}
