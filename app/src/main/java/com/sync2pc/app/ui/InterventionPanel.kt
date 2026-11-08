package com.sync2pc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sync2pc.app.R
import com.sync2pc.app.l10n.str
import com.sync2pc.app.mirror.InterventionState
import com.sync2pc.app.mirror.InterventionTask
import com.sync2pc.app.mirror.PendingIntervention

/**
 * Zustand des Eingriffs (Aufräumen, Endgültig löschen, Speicher freigeben) für ein Profil (0.11.10, DEC-144).
 * Meldet die Ansicht an, damit das Ergebnis hier statt als Benachrichtigung erscheint; [onFinished] wird einmal
 * aufgerufen, wenn der Eingriff fertig ist (Liste neu einlesen). null = es steht kein Eingriff an.
 */
@Composable
fun rememberIntervention(profileId: Long, onFinished: () -> Unit): InterventionTask? {
    DisposableEffect(profileId) {
        PendingIntervention.viewerOpened(profileId)
        onDispose { PendingIntervention.viewerClosed(profileId) }
    }
    val task = PendingIntervention.tasks.collectAsState().value[profileId]
    val state = task?.state
    LaunchedEffect(state) { if (state == InterventionState.DONE) onFinished() }
    return task
}

/**
 * Anzeige eines wartenden, laufenden oder fertigen Eingriffs. Wartend: Hinweis "Wird nach dem laufenden Sync
 * ausgeführt" mit "Abbrechen". Die Ansicht darf verlassen werden, der Eingriff läuft weiter.
 */
@Composable
fun InterventionPanel(profileId: Long, task: InterventionTask, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (task.state) {
            InterventionState.WAITING -> {
                Text(str(R.string.intervention_waiting))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Hint(str(R.string.intervention_waiting_hint))
                OutlinedButton(onClick = { PendingIntervention.cancel(profileId) }, modifier = Modifier.fillMaxWidth()) {
                    Text(str(R.string.common_cancel))
                }
            }
            InterventionState.RUNNING -> {
                Text(str(R.string.intervention_running))
                val p = task.progress
                if (p != null && p.second > 0) {
                    LinearProgressIndicator(progress = { p.first.toFloat() / p.second }, modifier = Modifier.fillMaxWidth())
                    Hint("${p.first} / ${p.second}")
                } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Hint(str(R.string.intervention_running_hint))
            }
            InterventionState.DONE -> {
                SectionCard(str(R.string.free_result)) { Text(task.result.orEmpty()) }
                Button(onClick = { PendingIntervention.dismiss(profileId) }, modifier = Modifier.fillMaxWidth()) {
                    Text(str(R.string.common_ok))
                }
            }
        }
    }
}
