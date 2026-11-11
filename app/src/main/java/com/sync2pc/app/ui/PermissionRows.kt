package com.sync2pc.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sync2pc.app.R
import com.sync2pc.app.l10n.str

/**
 * Berechtigungen als Zeilen mit Knopf (0.9.0: gemeinsam für die Karte auf der Startseite und die Seite
 * "App-Berechtigungen"). [showDone] = auch erteilte Punkte mit "✓ erledigt" zeigen. [needs] = nur diese Gruppen (0.11.0).
 */
@SuppressLint("BatteryLife")
@Composable
fun PermissionRows(perms: PermStatus, showDone: Boolean, needs: PermNeeds = PermNeeds.ALL) {
    val ctx = LocalContext.current
    val multi = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val grant = str(R.string.perm_grant)
    val open = str(R.string.perm_open)
    var blockedHint by remember { mutableStateOf(false) }
    val askedPrefs = remember { ctx.getSharedPreferences("perm_requests", Context.MODE_PRIVATE) }

    /**
     * B1 (SYNC-20261110-137): Hat der Benutzer schon einmal gefragt und "Nicht mehr fragen" gewählt, zeigt Android
     * keinen Dialog mehr. Dann statt eines wirkungslosen Tipps den Hinweis mit Knopf zur App-Info zeigen.
     */
    fun ask(needed: Array<String>, launch: () -> Unit) {
        val act = ctx as? Activity
        val open = needed.filter { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }
        val asked = open.any { askedPrefs.getBoolean("asked_$it", false) }
        val canAsk = act == null || open.any { ActivityCompat.shouldShowRequestPermissionRationale(act, it) }
        if (asked && !canAsk) { blockedHint = true; return }
        askedPrefs.edit().apply { open.forEach { putBoolean("asked_$it", true) } }.apply()
        launch()
    }

    if (blockedHint) AlertDialog(
        onDismissRequest = { blockedHint = false },
        title = { Text(str(R.string.perm_blocked_title)) },
        text = { Text(str(R.string.perm_blocked_text)) },
        confirmButton = {
            TextButton(onClick = {
                blockedHint = false
                try {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                } catch (ignored: Exception) {}
            }) { Text(str(R.string.settings_perm_app_info)) }
        },
        dismissButton = { TextButton(onClick = { blockedHint = false }) { Text(str(R.string.common_cancel)) } },
    )

    @Composable
    fun row(ok: Boolean, text: String, button: String, onClick: () -> Unit) {
        if (ok && !showDone) return
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text, modifier = Modifier.weight(1f))
            if (ok) Text(str(R.string.perm_granted), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
            else TextButton(onClick = onClick) { Text(button) }
        }
    }

    if (needs.media) {
        row(perms.media, str(R.string.perm_media), grant) { ask(PermStatus.mediaPermissions) { multi.launch(PermStatus.mediaPermissions) } }
        // Standort in Fotos erst nach dem Medienzugriff (Android verlangt die Reihenfolge)
        if (perms.media || showDone) row(perms.mediaLocation, str(R.string.perm_media_location), grant) {
            if (perms.media) ask(arrayOf(Manifest.permission.ACCESS_MEDIA_LOCATION)) { single.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) }
            else ask(PermStatus.mediaPermissions) { multi.launch(PermStatus.mediaPermissions) }
        }
    }
    if (needs.base && Build.VERSION.SDK_INT >= 33) row(perms.notifications, str(R.string.perm_notifications), grant) {
        ask(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) { single.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
    if (needs.wifi) {
        row(perms.location, str(R.string.perm_location), grant) { ask(PermStatus.locationPermissions) { multi.launch(PermStatus.locationPermissions) } }
        if (perms.location || showDone) row(perms.backgroundLocation, str(R.string.perm_location_always), open) {
            if (perms.location) single.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else ask(PermStatus.locationPermissions) { multi.launch(PermStatus.locationPermissions) }
        }
        row(perms.locationServiceOn,
            str(if (perms.locationServiceOn) R.string.perm_location_service else R.string.perm_location_off),
            str(R.string.perm_turn_on)) {
            ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
    if (needs.base) row(perms.batteryUnrestricted, str(R.string.perm_battery), open) {
        ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
    }
}
