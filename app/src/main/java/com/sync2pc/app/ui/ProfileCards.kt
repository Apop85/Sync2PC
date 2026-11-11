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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import com.sync2pc.app.data.VerifyLevel
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
import com.sync2pc.app.data.NumberInput
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
import androidx.compose.runtime.MutableState

// Karten von "Profil bearbeiten" (0.9.5 aus ProfileScreen.kt ausgelagert). Sie bekommen den Profil-Zustand
// als MutableState, damit der Code der Karten unverändert bleibt.

/** Fehlertext unter einem Zahlenfeld ([text] leer oder ausserhalb von [range]), sonst null */
internal fun rangeError(text: String, range: IntRange): (@Composable () -> Unit)? =
    if (NumberInput.isValid(text, range)) null
    else { { Text(str(R.string.profile_number_range, range.first, range.last)) } }

/** Ordner-Profil: Richtung, Löschungen, Versionen, Papierkorb-Frist, Konflikte, Prüfintervall */
@Composable
internal fun MirrorOptionsCard(
    pState: MutableState<Profile>, trashState: MutableState<String>, pollState: MutableState<String>, trashName: String,
) {
    var p by pState
    var trashText by trashState
    var pollText by pollState
    SectionCard(str(R.string.profile_direction)) {
        HintMore(str(R.string.profile_direction_hint))
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
        // P7/P8: leer oder ausserhalb des Bereichs wird nie still gespeichert (Fehlertext, Speichern gesperrt)
        OutlinedTextField(
            trashText,
            { v ->
                trashText = NumberInput.filter(v, 4)
                NumberInput.parse(trashText, NumberInput.TRASH_DAYS.first, NumberInput.TRASH_DAYS.last)
                    ?.let { p = p.copy(trashRetentionDays = it) }
            },
            label = { Text(str(R.string.profile_trash_days)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = !NumberInput.isValid(trashText, NumberInput.TRASH_DAYS),
            supportingText = rangeError(trashText, NumberInput.TRASH_DAYS),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        HintMore(str(R.string.profile_trash_days_hint, trashName))
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
            if (p.conflict() == ConflictMode.NEWER) HintMore(str(R.string.profile_conflict_newer_hint))
        }
        OutlinedTextField(
            pollText,
            { v ->
                pollText = NumberInput.filter(v, 4)
                NumberInput.parse(pollText, NumberInput.POLL_MINUTES.first, NumberInput.POLL_MINUTES.last)
                    ?.let { p = p.copy(remotePollMinutes = it) }
            },
            label = { Text(str(R.string.profile_poll)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = !NumberInput.isValid(pollText, NumberInput.POLL_MINUTES),
            supportingText = rangeError(pollText, NumberInput.POLL_MINUTES),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        HintMore(str(R.string.profile_poll_hint))
    }
}

/** Fotos-Profil: was nach dem Hochladen passiert, Prüfstufe ([onRequestFull] = Rückfrage für "Komplett") */
@Composable
internal fun PhotoOptionsCard(pState: MutableState<Profile>, onRequestFull: () -> Unit) {
    val ctx = LocalContext.current
    var p by pState
    SectionCard(str(R.string.profile_after_upload)) {
        HintMore(str(R.string.profile_after_upload_hint))
        // Wer Fotos entfernt, braucht eine Prüfung: "Aus" springt dann auf "Mittel" (0.9.0)
        fun setDelete(m: DeleteMode) {
            var np = p.copy(deleteMode = m.name)
            if (m != DeleteMode.KEEP && VerifyLevel.of(np.verifyLevel) == VerifyLevel.OFF) {
                np = np.copy(verifyLevel = VerifyLevel.MEDIUM.code)
                Toast.makeText(ctx, str(R.string.verify_raised), Toast.LENGTH_LONG).show()
            }
            p = np
        }
        DeleteMode.entries.forEach { m ->
            Row(
                Modifier.fillMaxWidth().clickable { setDelete(m) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = p.delete() == m, onClick = { setDelete(m) })
                Text(m.label)
            }
        }
        if (p.delete() != DeleteMode.KEEP) HintMore(str(R.string.profile_delete_hint))
        if (p.delete() == DeleteMode.DELETE) Text(
            str(R.string.profile_delete_final_hint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
        )
        // Prüfstufe nach dem Hochladen (0.9.0)
        Text(str(R.string.profile_verify), style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp))
        HintMore(str(R.string.profile_verify_hint))
        val deleting = p.delete() != DeleteMode.KEEP
        fun chooseLevel(lv: VerifyLevel) {
            if (lv == VerifyLevel.FULL && p.verify() != VerifyLevel.FULL) onRequestFull()
            else p = p.copy(verifyLevel = lv.code)
        }
        VerifyLevel.entries.forEach { lv ->
            val enabled = !(lv == VerifyLevel.OFF && deleting)
            Row(
                Modifier.fillMaxWidth().clickable(enabled = enabled) { chooseLevel(lv) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = p.verify() == lv, onClick = { chooseLevel(lv) }, enabled = enabled)
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(lv.label, color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                    Text(lv.sub, style = MaterialTheme.typography.bodySmall,
                        color = if (lv == VerifyLevel.FULL) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (deleting) HintMore(str(R.string.profile_verify_required))
    }
}

/** Beispielpfad je Ordnerstruktur (Text in den Sprachdateien) */
private fun layoutExample(l: com.sync2pc.app.data.FolderLayout): String = str(when (l) {
    com.sync2pc.app.data.FolderLayout.MONTH_SUFFIX -> R.string.profile_layout_ex_month_suffix
    com.sync2pc.app.data.FolderLayout.COUNTRY_FOLDER -> R.string.profile_layout_ex_country_folder
    com.sync2pc.app.data.FolderLayout.COUNTRY_SUBFOLDER -> R.string.profile_layout_ex_country_subfolder
})

/**
 * Fotos-Profil: Ordnerstruktur auf dem PC (SYNC-20261109-085, DEC-148/151: Wertfeld mit Auswahlliste).
 * [lockedText] != null = nur Anzeige mit diesem Hinweis (nach dem ersten Sync oder während eines Laufs);
 * [adoptedFrom] = Name des Profils, dessen Struktur wegen gleichem Ziel übernommen wurde.
 */
@Composable
internal fun FolderLayoutCard(
    layout: com.sync2pc.app.data.FolderLayout, lockedText: String?, adoptedFrom: String?, pickable: Boolean = true,
    onPick: (com.sync2pc.app.data.FolderLayout) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    SectionCard(str(R.string.profile_layout_title)) {
        val value = layout.label
        if (lockedText == null && pickable) ValueField(str(R.string.profile_layout_title), value) { open = true }
        else Column {
            Text(str(R.string.profile_layout_title), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Hint(layoutExample(layout))
        Hint(str(R.string.profile_layout_example_none))
        if (adoptedFrom != null) Hint(str(R.string.profile_layout_adopted, adoptedFrom))
        Hint(lockedText ?: str(R.string.profile_layout_hint))
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(str(R.string.profile_layout_pick_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                com.sync2pc.app.data.FolderLayout.entries.forEach { l ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(l); open = false },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = l == layout, onClick = { onPick(l); open = false })
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(l.label)
                            Hint(layoutExample(l))
                        }
                    }
                }
                Hint(str(R.string.profile_layout_example_none))
                Hint(str(R.string.profile_layout_hint))
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { open = false }) { Text(str(R.string.common_cancel)) } },
    )
}
