@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sync2pc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.sync2pc.app.R
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.mirror.MirrorEngine
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.ConflictResolution
import com.sync2pc.app.data.MirrorConflict
import com.sync2pc.app.sync.Notifications
import com.sync2pc.app.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date


/** Dateiname ohne Ordner für Meldungen */
private fun fileName(c: MirrorConflict): String = c.relPath.substringAfterLast('/').substringAfterLast('\\')

/** Konflikte der Ordner-Spiegelung: Welche Version gilt? Die verworfene kommt in den Papierkorb. */
@Composable
fun ConflictScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    // 0.11.1: Konflikte eines Profils in der Rückgängig-Frist der Löschung ausblenden (nicht löschen)
    val pendingDelete by PendingProfileDelete.pending.collectAsState()
    val allConflicts by remember { dao.openConflictsFlow() }.collectAsState(initial = emptyList())
    val conflicts = allConflicts.filter { it.profileId != pendingDelete?.id }
    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())
    var confirmAllNewer by remember { mutableStateOf(false) }

    /** Entscheidung speichern, Meldung mit "Rückgängig" zeigen (gilt, solange die Synchronisierung sie nicht angewendet hat) */
    fun resolve(list: List<MirrorConflict>, message: String, choose: (MirrorConflict) -> String) {
        scope.launch {
            val chosen = list.associate { (it.profileId to it.relPath) to choose(it) }
            withContext(Dispatchers.IO) {
                list.forEach { dao.resolveConflict(it.profileId, it.relPath, chosen[it.profileId to it.relPath]!!) }
            }
            NotificationManagerCompat.from(ctx).cancel(Notifications.ID_CONFLICT)
            SyncScheduler.enqueueAuto(ctx, com.sync2pc.app.l10n.Msg.of(R.string.trigger_conflicts))
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(message, actionLabel = str(R.string.home_undo), duration = SnackbarDuration.Long)
            if (r == SnackbarResult.ActionPerformed) {
                // Rückgängig = Konflikt wieder offen (resolution = null), atomar und nur bei unveränderter Entscheidung:
                // Hat die Synchronisierung die Zeile schon gelöscht, ändert die Abfrage nichts (SYNC-20261111-172)
                val restored = withContext(Dispatchers.IO) {
                    list.count { c -> dao.undoResolution(c.profileId, c.relPath, chosen[c.profileId to c.relPath]!!) == 1 }
                }
                snackbar.currentSnackbarData?.dismiss()
                if (restored < list.size) snackbar.showSnackbar(str(R.string.conflict_undo_late), duration = SnackbarDuration.Short)
            }
        }
    }

    val newerMsgArgs = MirrorEngine.TRASH_DIR
    fun keepNewer(c: MirrorConflict) =
        if (c.localModified >= c.remoteModified) ConflictResolution.KEEP_LOCAL else ConflictResolution.KEEP_REMOTE

    if (confirmAllNewer) {
        AlertDialog(
            onDismissRequest = { confirmAllNewer = false },
            title = { Text(str(R.string.conflict_all_newer)) },
            text = {
                DialogScroll {
                    Text(plural(R.plurals.conflict_all_confirm, conflicts.size, conflicts.size, newerMsgArgs))
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    onClick = {
                        confirmAllNewer = false
                        resolve(conflicts, plural(R.plurals.conflict_snack_all, conflicts.size, conflicts.size), ::keepNewer)
                    },
                ) { Text(str(R.string.conflict_all_newer)) }
            },
            dismissButton = {
                TextButton(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = { confirmAllNewer = false }) {
                    Text(str(R.string.common_cancel))
                }
            },
        )
    }

    ScreenScaffold(
        title = str(R.string.conflict_title),
        onBack = onBack,
        snackbarHost = { LeoSnackbarHost(snackbar) },
        actions = {
            if (conflicts.isNotEmpty()) TextButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = { confirmAllNewer = true },
            ) { Text(str(R.string.conflict_all_newer)) }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (conflicts.isEmpty()) item { Text(str(R.string.conflict_none)) }
            else item { Hint(str(R.string.conflict_hint, MirrorEngine.TRASH_DIR)) }
            items(conflicts, key = { "${it.profileId}|${it.relPath}" }) { c ->
                val profileName = profiles.firstOrNull { it.id == c.profileId }?.name ?: ""
                // R11: «(neuer)» nur bei echtem Zeitunterschied (auf die Sekunde); sonst «(gleich alt)» auf beiden Seiten
                val sameAge = c.localModified / 1000 == c.remoteModified / 1000
                val localNewer = c.localModified > c.remoteModified
                val newer = "  " + str(R.string.conflict_newer)
                // B5: nicht trennbare Einheit (geschütztes Leerzeichen), sonst bricht «(gleich alt)» innerhalb der Klammer um
                val same = "  " + str(R.string.conflict_same_age).replace(' ', ' ')
                SectionCard {
                    Text(c.relPath, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (profiles.size > 1) Hint(str(R.string.decision_profile, profileName))
                    Text("📱 " + str(R.string.side_phone) + ": ${formatTimeSec(c.localModified)} · ${Notifications.formatBytes(c.localSize)}" +
                        if (sameAge) same else if (localNewer) newer else "")
                    Text("💻 " + str(R.string.side_pc) + ": ${formatTimeSec(c.remoteModified)} · ${Notifications.formatBytes(c.remoteSize)}" +
                        if (sameAge) same else if (!localNewer) newer else "")
                    // Knöpfe untereinander, gleich breit (bei grosser Schrift nie abgeschnitten)
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            onClick = { resolve(listOf(c), str(R.string.conflict_snack_phone, fileName(c))) { ConflictResolution.KEEP_LOCAL } },
                        ) { Text(str(R.string.conflict_keep_phone)) }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            onClick = { resolve(listOf(c), str(R.string.conflict_snack_pc, fileName(c))) { ConflictResolution.KEEP_REMOTE } },
                        ) { Text(str(R.string.conflict_keep_pc)) }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            onClick = { resolve(listOf(c), str(R.string.conflict_snack_both, fileName(c))) { ConflictResolution.KEEP_BOTH } },
                        ) { Text(str(R.string.conflict_keep_both)) }
                    }
                    // Folge jeder Option, bevor sie gewählt wird
                    Hint(str(R.string.conflict_keep_phone_hint, MirrorEngine.TRASH_DIR))
                    Hint(str(R.string.conflict_keep_pc_hint, MirrorEngine.TRASH_DIR))
                    Hint(str(R.string.conflict_keep_both_hint, MirrorEngine.CONFLICT_SUFFIX))
                }
            }
        }
    }
}
