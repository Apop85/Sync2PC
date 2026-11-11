package com.sync2pc.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.sync2pc.app.R
import com.sync2pc.app.l10n.AppLanguage
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
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
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState

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
    /** Papierkorb: wiederherstellen und aufräumen (0.6.3, ersetzt Restore und Purge) */
    data object Trash : Screen()
}

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf<Screen>(
        // Nach dem Sprachwechsel (Activity wird neu aufgebaut) auf der Einstellungsseite bleiben
        returnToSection?.also { returnToSection = null }?.let { Screen.Settings(it) } ?: Screen.Home
    )
    private var startDeleteFlow by mutableStateOf(false)

    companion object {
        /** Einstellungsseite, auf die nach dem nächsten Neuaufbau der Activity (Sprachwechsel) zurückgekehrt wird */
        @Volatile var returnToSection: SettingsSection? = null

        private const val STATE_SCREEN = "screen"

        /** Bildschirm als Text für den Instanzzustand (SYNC-20261111-218 B6) */
        internal fun encodeScreen(s: Screen): String = when (s) {
            Screen.Home -> "home"
            is Screen.Settings -> "settings:${s.section.name}"
            is Screen.ProfileEdit -> "profile:${s.id}"
            Screen.Decisions -> "decisions"
            is Screen.Preview -> "preview:${s.profileId}"
            Screen.Log -> "log"
            Screen.Stats -> "stats"
            Screen.Update -> "update"
            Screen.Conflicts -> "conflicts"
            Screen.FreeSpace -> "free"
            Screen.Trash -> "trash"
        }

        /** Gegenstück zu [encodeScreen]; unbekannt oder defekt = null (dann bleibt die Startseite) */
        internal fun decodeScreen(code: String): Screen? {
            val arg = code.substringAfter(':', "")
            return when (code.substringBefore(':')) {
                "home" -> Screen.Home
                "settings" -> SettingsSection.entries.firstOrNull { it.name == arg }?.let { Screen.Settings(it) }
                "profile" -> arg.toLongOrNull()?.let { Screen.ProfileEdit(it) }
                "decisions" -> Screen.Decisions
                "preview" -> arg.toLongOrNull()?.let { Screen.Preview(it) }
                "log" -> Screen.Log
                "stats" -> Screen.Stats
                "update" -> Screen.Update
                "conflicts" -> Screen.Conflicts
                "free" -> Screen.FreeSpace
                "trash" -> Screen.Trash
                else -> null
            }
        }
    }

    /** Android 10–12: gewählte App-Sprache auch für System- und Material-Texte (ab 13 erledigt das Android) */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localized(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // SYNC-20261111-218 B6: nach einer Konfigurationsänderung (Schriftgrösse, Hell/Dunkel) auf dem Bildschirm bleiben
        savedInstanceState?.getString(STATE_SCREEN)?.let { decodeScreen(it) }?.let { screen = it }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SCREEN, encodeScreen(screen))
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
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: str(R.string.main_install_status, status)
                val text = if (status == PackageInstaller.STATUS_FAILURE_CONFLICT || status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE)
                    str(R.string.main_update_failed_key, msg) else str(R.string.main_update_failed, msg)
                if (status != PackageInstaller.STATUS_FAILURE_ABORTED)
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
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
                Toast.makeText(ctx, plural(R.plurals.main_files_removed, current.keys.size), Toast.LENGTH_SHORT).show()
            }
            batches = if (res.resultCode == Activity.RESULT_OK) rest else emptyList()
        }
    }
    LaunchedEffect(batches) {
        batches.firstOrNull()?.let {
            deleteLauncher.launch(IntentSenderRequest.Builder(it.intent.intentSender).build())
        }
    }
    val homeSnackbar = remember { SnackbarHostState() }
    val confirmDeletes: () -> Unit = {
        scope.launch {
            val out = withContext(Dispatchers.IO) { DeletionManager.buildRequests(ctx) }
            // Vor dem Systemdialog wird die PC-Kopie geprüft (SYNC-20261111-142): Ist der PC nicht erreichbar, wird nichts
            // gelöscht, die Dateien bleiben vorgemerkt
            val note = when {
                out.unreachable -> str(R.string.delete_pc_unreachable_retry)
                out.rejectedMessage != null -> str(R.string.delete_pc_rejected, out.rejectedMessage)
                out.reset > 0 -> str(R.string.delete_pc_reset, out.reset)
                out.batches.isEmpty() -> str(R.string.main_nothing_to_remove)
                else -> null
            }
            if (out.unreachable) {
                // R12: Snackbar (ca. 8 s, schliessbar) statt flüchtigem Toast, mit Empfehlung
                homeSnackbar.currentSnackbarData?.dismiss()
                launch {
                    withTimeoutOrNull(8_000) {
                        homeSnackbar.showSnackbar(note!!, withDismissAction = true, duration = SnackbarDuration.Indefinite)
                    }
                }
            } else if (note != null) Toast.makeText(ctx, note, Toast.LENGTH_LONG).show()
            batches = out.batches
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
        Screen.Home -> HomeScreen(navigate = navigate, onConfirmDeletes = confirmDeletes, snackbar = homeSnackbar)
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
        Screen.Trash -> TrashScreen(onBack = back)
    }
}
