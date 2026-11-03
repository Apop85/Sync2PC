package com.sync2pc.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * "App-Berechtigungen"). [showDone] = auch erteilte Punkte mit "✓ erledigt" zeigen.
 */
@SuppressLint("BatteryLife")
@Composable
fun PermissionRows(perms: PermStatus, showDone: Boolean) {
    val ctx = LocalContext.current
    val multi = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val grant = str(R.string.perm_grant)
    val open = str(R.string.perm_open)

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

    row(perms.media, str(R.string.perm_media), grant) { multi.launch(PermStatus.mediaPermissions) }
    // Standort in Fotos erst nach dem Medienzugriff (Android verlangt die Reihenfolge)
    if (perms.media || showDone) row(perms.mediaLocation, str(R.string.perm_media_location), grant) {
        if (perms.media) single.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) else multi.launch(PermStatus.mediaPermissions)
    }
    if (Build.VERSION.SDK_INT >= 33) row(perms.notifications, str(R.string.perm_notifications), grant) {
        single.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    row(perms.location, str(R.string.perm_location), grant) { multi.launch(PermStatus.locationPermissions) }
    if (perms.location || showDone) row(perms.backgroundLocation, str(R.string.perm_location_always), open) {
        if (perms.location) single.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else multi.launch(PermStatus.locationPermissions)
    }
    row(perms.locationServiceOn,
        str(if (perms.locationServiceOn) R.string.perm_location_service else R.string.perm_location_off),
        str(R.string.perm_turn_on)) {
        ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
    row(perms.batteryUnrestricted, str(R.string.perm_battery), open) {
        ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
    }
}
