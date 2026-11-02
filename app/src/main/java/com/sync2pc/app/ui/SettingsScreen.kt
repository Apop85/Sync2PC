package com.sync2pc.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sync2pc.app.BuildConfig
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.diag.Diagnostics
import com.sync2pc.app.diag.VendorHints
import com.sync2pc.app.geo.CountryLookup
import com.sync2pc.app.sync.StaleCheck
import com.sync2pc.app.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Rubriken der Einstellungen (0.4.0). Das Zahnrad auf der Startseite öffnet ein Menü mit diesen Punkten;
 * jede Rubrik ist eine eigene Seite. "Updates" führt direkt zur Update-Seite (prüft sofort).
 */
enum class SettingsSection(val title: String) {
    NOTIFICATIONS("Benachrichtigungen"),
    TRANSFER("Übertragung"),
    PERMISSIONS("App-Berechtigungen"),
    APP("App-Einstellungen"),
    UPDATES("Updates"),
    DIAGNOSE("Diagnose"),
}

@Composable
fun SettingsScreen(section: SettingsSection, onBack: () -> Unit) {
    ScreenScaffold(title = section.title, onBack = onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (section) {
                SettingsSection.NOTIFICATIONS -> notificationsPage()
                SettingsSection.TRANSFER -> transferPage()
                SettingsSection.PERMISSIONS -> permissionsPage()
                SettingsSection.APP -> appPage()
                SettingsSection.UPDATES -> {} // eigene Seite (UpdateScreen)
                SettingsSection.DIAGNOSE -> diagnosePage()
            }
        }
    }
}

// ---- 1. Benachrichtigungen ---------------------------------------------------------------
private fun LazyListScope.notificationsPage() {
    item {
        val ctx = LocalContext.current
        val s = remember { GlobalSettings(ctx) }
        var summary by remember { mutableStateOf(s.notifySummary) }
        var update by remember { mutableStateOf(s.notifyUpdate) }
        var hints by remember { mutableStateOf(s.notifyHints) }
        SectionCard("Benachrichtigungen") {
            SwitchRow("Meldung nach jedem Sync", summary, subtitle = "z. B. „3 Dateien übertragen“") {
                summary = it; s.notifySummary = it
            }
            SwitchRow("Update verfügbar", update, subtitle = "Neue Version gefunden bzw. installiert") {
                update = it; s.notifyUpdate = it
            }
            SwitchRow("Hinweise", hints,
                subtitle = "Ordnerauswahl nötig, Konflikte, Dateien entfernen, Fehler. Erscheinen auch ohne Benachrichtigung auf der Startseite.") {
                hints = it; s.notifyHints = it
            }
            Hint("Die Fortschrittsanzeige während eines Syncs und „Pausiert – wartet auf WLAN“ lassen sich nicht abschalten: " +
                "Android verlangt sie, damit der Sync im Hintergrund weiterlaufen bzw. fortgesetzt werden kann.")
            OutlinedButton(onClick = {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                } catch (ignored: Exception) {}
            }) { Text("Android-Einstellungen für Benachrichtigungen") }
        }
    }
    item {
        val ctx = LocalContext.current
        val s = remember { GlobalSettings(ctx) }
        var staleOn by remember { mutableStateOf(s.staleWarnEnabled) }
        var staleDays by remember { mutableStateOf(s.staleWarnDays.toString()) }
        SectionCard("Erinnerung") {
            SwitchRow("Erinnern, wenn lange nicht gesichert", staleOn,
                subtitle = "Nur wenn neue Fotos/Videos warten. Wiederholt sich alle ${StaleCheck.REPEAT_DAYS} Tage.") {
                staleOn = it; s.staleWarnEnabled = it; SyncScheduler.setupAll(ctx)
            }
            if (staleOn) OutlinedTextField(
                staleDays,
                { v ->
                    staleDays = v.filter { it.isDigit() }.take(3)
                    staleDays.toIntOrNull()?.takeIf { it > 0 }?.let { s.staleWarnDays = it }
                },
                label = { Text("Nach … Tagen ohne Sicherung") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---- 2. Übertragung -------------------------------------------------------------------
private fun LazyListScope.transferPage() {
    item {
        val ctx = LocalContext.current
        val s = remember { GlobalSettings(ctx) }
        var autoWifi by remember { mutableStateOf(s.autoSyncOnWifi) }
        var onNew by remember { mutableStateOf(s.syncOnNewMedia) }
        var charging by remember { mutableStateOf(s.onlyWhileCharging) }
        var minAge by remember { mutableStateOf(s.minFileAgeMinutes.toString()) }
        fun apply() = SyncScheduler.setupAll(ctx)
        SectionCard("Automatik") {
            SwitchRow("Sync beim Verbinden mit Heim-WLAN", autoWifi,
                subtitle = "Startet sofort, wenn ein WLAN eines Profils verbunden wird") {
                autoWifi = it; s.autoSyncOnWifi = it; apply()
            }
            SwitchRow("Neue Fotos/Videos sofort übertragen", onNew,
                subtitle = "Nur, wenn gerade ein Heim-WLAN verbunden ist") {
                onNew = it; s.syncOnNewMedia = it; apply()
            }
            SwitchRow("Nur beim Laden", charging,
                subtitle = "Automatische Syncs nur am Ladegerät (manueller Sync immer möglich)") {
                charging = it; s.onlyWhileCharging = it; apply()
            }
            OutlinedTextField(
                minAge,
                { v ->
                    minAge = v.filter { it.isDigit() }.take(4)
                    minAge.toIntOrNull()?.let { s.minFileAgeMinutes = it; apply() }
                },
                label = { Text("Mindestalter einer Datei (Minuten)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Hint("Verhindert, dass Fotos übertragen werden, die gerade noch bearbeitet werden.")
        }
    }
    item {
        val ctx = LocalContext.current
        val s = remember { GlobalSettings(ctx) }
        var parallel by remember { mutableStateOf(s.parallelUploads) }
        SectionCard("Parallele Übertragungen") {
            Text("Gleichzeitige Übertragungen: $parallel")
            Slider(
                value = parallel.toFloat(),
                onValueChange = { parallel = it.roundToInt().coerceIn(1, 4); s.parallelUploads = parallel },
                valueRange = 1f..4f,
                steps = 2,
            )
            Hint("Mehr Verbindungen beschleunigen viele kleine Dateien. Bei grossen Videos bringt es wenig. Standard: 3")
        }
    }
}

// ---- 3. App-Berechtigungen ------------------------------------------------------------
@SuppressLint("BatteryLife")
private fun LazyListScope.permissionsPage() {
    item {
        val ctx = LocalContext.current
        SectionCard("Hintergrund") {
            Hint("Damit Android den Sync nicht verzögert oder abbricht, sollte die Akku-Optimierung für LeoSync aus sein.")
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            }) { Text("App-Berechtigungen") }
        }
    }
    item {
        val ctx = LocalContext.current
        SectionCard("Akku-Optimierungen") {
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
            }) { Text("Akku-Optimierung ausschalten") }
            val vendor = remember { VendorHints.forDevice() }
            if (vendor != null) {
                Text("Zusätzlich bei ${vendor.vendor}:", style = MaterialTheme.typography.bodyMedium)
                vendor.steps.forEach { Hint("• $it") }
                Hint("Die Bezeichnungen können je nach Software-Stand leicht abweichen.")
            }
            OutlinedButton(onClick = {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(VendorHints.webUrl(vendor))))
                } catch (ignored: Exception) {}
            }) { Text("Anleitung im Browser") }
        }
    }
}

// ---- 4. App-Einstellungen -------------------------------------------------------------
private fun LazyListScope.appPage() {
    item {
        val ctx = LocalContext.current
        val s = remember { GlobalSettings(ctx) }
        var home by remember { mutableStateOf(s.homeCountryIso) }
        var geoInfo by remember { mutableStateOf("Lade Länderdaten…") }
        LaunchedEffect(Unit) {
            geoInfo = withContext(Dispatchers.IO) {
                val c = CountryLookup.get(ctx)
                if (c.isAvailable) "${c.allCountryNames.size} Länder geladen (offline)"
                else "Länderdaten fehlen – Ordner werden ohne Land angelegt"
            }
        }
        SectionCard("Länderordner") {
            OutlinedTextField(
                home,
                { v -> home = v.filter { it.isLetter() }.take(2).uppercase(); if (home.length == 2) s.homeCountryIso = home },
                label = { Text("Heimatland (ISO-Code)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Hint("Fotos aus diesem Land bekommen keinen Länderzusatz (Standard: CH = Schweiz).")
            Hint(geoInfo)
        }
    }
}

// ---- 6. Diagnose ----------------------------------------------------------------------
private fun LazyListScope.diagnosePage() {
    item { BackupSection() }
    item {
        val ctx = LocalContext.current
        val s = remember { GlobalSettings(ctx) }
        val scope = rememberCoroutineScope()
        var diagBusy by remember { mutableStateOf(false) }
        SectionCard("Beta / Diagnose") {
            Hint("Sammelt Angaben zu Gerät, Hintergrund-Einschränkungen, Berechtigungen, Profilen und den letzten Läufen " +
                "(ohne Passwörter) und kopiert sie in die Zwischenablage. Geräte-ID: ${s.deviceId}")
            OutlinedButton(enabled = !diagBusy, onClick = {
                diagBusy = true
                scope.launch {
                    try {
                        Diagnostics.copyToClipboard(ctx, Diagnostics.build(ctx))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(ctx, "Diagnose fehlgeschlagen: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    } finally {
                        diagBusy = false
                    }
                }
            }) { Text(if (diagBusy) "Sammle Daten…" else "Diagnosedaten kopieren") }
        }
    }
    item { Hint("LeoSync ${BuildConfig.VERSION_NAME} – Open Source (MIT)") }
}
