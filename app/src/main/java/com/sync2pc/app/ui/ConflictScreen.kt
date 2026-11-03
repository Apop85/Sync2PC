package com.sync2pc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.sync2pc.app.R
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

/** Konflikte der Ordner-Spiegelung: Welche Version gilt? Die verworfene kommt in den Papierkorb. */
@Composable
fun ConflictScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    val scope = rememberCoroutineScope()
    val conflicts by remember { dao.openConflictsFlow() }.collectAsState(initial = emptyList())
    val profiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())

    fun resolve(list: List<MirrorConflict>, choose: (MirrorConflict) -> String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                list.forEach { dao.resolveConflict(it.profileId, it.relPath, choose(it)) }
            }
            NotificationManagerCompat.from(ctx).cancel(Notifications.ID_CONFLICT)
            SyncScheduler.enqueueAuto(ctx, str(R.string.trigger_conflicts))
        }
    }

    ScreenScaffold(
        title = str(R.string.conflict_title),
        onBack = onBack,
        actions = {
            if (conflicts.isNotEmpty()) TextButton(onClick = {
                resolve(conflicts) {
                    if (it.localModified >= it.remoteModified) ConflictResolution.KEEP_LOCAL else ConflictResolution.KEEP_REMOTE
                }
            }) { Text(str(R.string.conflict_all_newer)) }
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
                val localNewer = c.localModified >= c.remoteModified
                val newer = "  " + str(R.string.conflict_newer)
                SectionCard {
                    Text(c.relPath, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (profiles.size > 1) Hint(str(R.string.decision_profile, profileName))
                    Text("📱 " + str(R.string.side_phone) + ": ${formatTime(c.localModified)} · ${Notifications.formatBytes(c.localSize)}" +
                        if (localNewer) newer else "")
                    Text("💻 " + str(R.string.side_pc) + ": ${formatTime(c.remoteModified)} · ${Notifications.formatBytes(c.remoteSize)}" +
                        if (!localNewer) newer else "")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { resolve(listOf(c)) { ConflictResolution.KEEP_LOCAL } }) { Text(str(R.string.side_phone)) }
                        OutlinedButton(onClick = { resolve(listOf(c)) { ConflictResolution.KEEP_REMOTE } }) { Text(str(R.string.side_pc)) }
                        OutlinedButton(onClick = { resolve(listOf(c)) { ConflictResolution.KEEP_BOTH } }) { Text(str(R.string.conflict_both)) }
                    }
                }
            }
        }
    }
}
