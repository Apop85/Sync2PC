package com.sync2pc.app.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.sync.DeletionManager
import com.sync2pc.app.update.UpdateInstaller
import com.sync2pc.app.update.UpdateScreen
import com.sync2pc.app.sync.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen {
    data object Home : Screen()
    /** Eine Rubrik der Einstellungen (0.4.0) */
    data class Settings(val section: SettingsSection) : Screen()
    data class ProfileEdit(val id: Long) : Screen()
    data object Decisions : Screen()
    /** Simulation eines Profils; Zurück führt ins Profil */
    data class Preview(val profileId: Long) : Screen()
    data object Log : Screen()
    data object Stats : Screen()
    data object Update : Screen()
    data object Conflicts : Screen()
    data object FreeSpace : Screen()
    /** Papierkorb & alte Versionen (0.5.0) */
    data object Restore : Screen()
}

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf<Screen>(Screen.Home)
    private var startDeleteFlow by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        // Startbildschirm nur beim echten Start (nicht nach Drehen des Bildschirms o. Ä.)
        val showSplashInitially = savedInstanceState == null
        setContent {
            LeoSyncTheme {
                var splash by remember { mutableStateOf(showSplashInitially) }
                LaunchedEffect(Unit) {
                    if (splash) { delay(1_300); splash = false }
                }
                Crossfade(targetState = splash, animationSpec = tween(400), label = "splash") { showSplash ->
                    if (showSplash) SplashScreen()
                    else AppRoot(
                        screen = screen,
                        navigate = { screen = it },
                        startDeleteFlow = startDeleteFlow,
                        onDeleteFlowStarted = { startDeleteFlow = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == UpdateInstaller.ACTION_INSTALL_STATUS) {
            handleInstallStatus(intent)
            intent.action = null
            return
        }
        when (intent?.getStringExtra(Notifications.EXTRA_OPEN)) {
            Notifications.OPEN_DECISIONS -> screen = Screen.Decisions
            Notifications.OPEN_LOG -> screen = Screen.Log
            Notifications.OPEN_DELETE -> { screen = Screen.Home; startDeleteFlow = true }
            Notifications.OPEN_UPDATE -> screen = Screen.Update
            Notifications.OPEN_CONFLICTS -> screen = Screen.Conflicts
        }
        intent?.removeExtra(Notifications.EXTRA_OPEN)
    }

    /** Rückmeldung des Paketinstallers (siehe UpdateInstaller.install) */
    private fun handleInstallStatus(intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Bestätigungsdialog im eigenen Task öffnen -> nach der Installation kehrt Android hierher zurück
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let { startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> { /* App wird ersetzt und neu gestartet */ }
            else -> {
                GlobalSettings(this).updateInstalling = ""
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Status $status"
                val hint = if (status == PackageInstaller.STATUS_FAILURE_CONFLICT || status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE)
                    " – Die neue Version ist evtl. mit einem anderen Schlüssel signiert." else ""
                if (status != PackageInstaller.STATUS_FAILURE_ABORTED)
                    Toast.makeText(this, "Update fehlgeschlagen: $msg$hint", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
private fun AppRoot(
    screen: Screen,
    navigate: (Screen) -> Unit,
    startDeleteFlow: Boolean,
    onDeleteFlowStarted: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Lösch-/Papierkorb-Bestätigung (Android 11+, Systemdialog) -----------------
    var batches by remember { mutableStateOf<List<DeletionManager.Batch>>(emptyList()) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val current = batches.firstOrNull() ?: return@rememberLauncherForActivityResult
        val rest = batches.drop(1)
        scope.launch {
            if (res.resultCode == Activity.RESULT_OK) {
                withContext(Dispatchers.IO) { DeletionManager.markDone(ctx, current.keys) }
                Toast.makeText(ctx, "${current.keys.size} Dateien entfernt", Toast.LENGTH_SHORT).show()
            }
            batches = if (res.resultCode == Activity.RESULT_OK) rest else emptyList()
        }
    }
    LaunchedEffect(batches) {
        batches.firstOrNull()?.let {
            deleteLauncher.launch(IntentSenderRequest.Builder(it.intent.intentSender).build())
        }
    }
    val confirmDeletes: () -> Unit = {
        scope.launch {
            val b = withContext(Dispatchers.IO) { DeletionManager.buildRequests(ctx) }
            if (b.isEmpty()) Toast.makeText(ctx, "Nichts zu entfernen", Toast.LENGTH_SHORT).show()
            batches = b
        }
    }
    LaunchedEffect(startDeleteFlow) {
        if (startDeleteFlow) { onDeleteFlowStarted(); confirmDeletes() }
    }

    BackHandler(enabled = screen != Screen.Home) {
        navigate(Screen.Home)
    }

    val back = { navigate(Screen.Home) }
    when (screen) {
        Screen.Home -> HomeScreen(navigate = navigate, onConfirmDeletes = confirmDeletes)
        is Screen.Settings -> SettingsScreen(section = screen.section, onBack = back)
        is Screen.ProfileEdit -> ProfileScreen(
            profileId = screen.id, onBack = back,
            onSimulate = { id -> navigate(Screen.Preview(id)) },
        )
        Screen.Decisions -> DecisionScreen(onBack = back)
        is Screen.Preview -> PreviewScreen(profileId = screen.profileId, onBack = { navigate(Screen.ProfileEdit(screen.profileId)) })
        Screen.Log -> LogScreen(onBack = back)
        Screen.Stats -> StatsScreen(onBack = back)
        Screen.Update -> UpdateScreen(onBack = back)
        Screen.Conflicts -> ConflictScreen(onBack = back)
        Screen.FreeSpace -> FreeSpaceScreen(onBack = back)
        Screen.Restore -> RestoreScreen(onBack = back)
    }
}
