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
import androidx.compose.foundation.text.KeyboardOptions
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
import com.sync2pc.app.geo.CountryLookup
import com.sync2pc.app.sync.SyncScheduler
import com.sync2pc.app.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenUpdate: () -> Unit) {
    val ctx = LocalContext.current
    val s = remember { GlobalSettings(ctx) }
    var autoWifi by remember { mutableStateOf(s.autoSyncOnWifi) }
    var onNew by remember { mutableStateOf(s.syncOnNewMedia) }
    var charging by remember { mutableStateOf(s.onlyWhileCharging) }
    var minAge by remember { mutableStateOf(s.minFileAgeMinutes.toString()) }
    var home by remember { mutableStateOf(s.homeCountryIso) }
    var parallel by remember { mutableStateOf(s.parallelUploads) }
    var repo by remember { mutableStateOf(s.updateRepo) }
    var autoUpdate by remember { mutableStateOf(s.autoUpdateCheck) }
    var geoInfo by remember { mutableStateOf("Lade Länderdaten…") }
    var diagBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        geoInfo = withContext(Dispatchers.IO) {
            val c = CountryLookup.get(ctx)
            if (c.isAvailable) "${c.allCountryNames.size} Länder geladen (offline)"
            else "Länderdaten fehlen – Ordner werden ohne Land angelegt"
        }
    }

    fun apply() = SyncScheduler.setupAll(ctx)

    ScreenScaffold(title = "Einstellungen", onBack = onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
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
                            minAge.toIntOrNull()?.let { s.minFileAgeMinutes = it }
                        },
                        label = { Text("Mindestalter einer Datei (Minuten)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Hint("Verhindert, dass Fotos übertragen werden, die gerade noch bearbeitet werden.")
                }
            }
            item {
                SectionCard("Übertragung") {
                    Text("Parallele Übertragungen: $parallel")
                    Slider(
                        value = parallel.toFloat(),
                        onValueChange = { parallel = it.roundToInt().coerceIn(1, 4); s.parallelUploads = parallel },
                        valueRange = 1f..4f,
                        steps = 2,
                    )
                    Hint("Mehr Verbindungen beschleunigen viele kleine Dateien. Bei grossen Videos bringt es wenig. Standard: 3")
                }
            }
            item {
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
            item {
                SectionCard("Hintergrund") {
                    Hint("Damit Android den Sync nicht verzögert oder abbricht, sollte die Akku-Optimierung für LeoSync aus sein.")
                    OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                    }) { Text("Akku-Optimierung") }
                    OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                    }) { Text("App-Berechtigungen") }
                }
            }
            item {
                SectionCard("Updates") {
                    OutlinedTextField(
                        repo,
                        { v ->
                            repo = v
                            s.updateRepo = UpdateChecker.normalizeRepo(v) ?: v.trim()
                            apply()
                        },
                        label = { Text("GitHub-Repository") },
                        placeholder = { Text("benutzer/LeoSync") },
                        isError = repo.isNotBlank() && UpdateChecker.normalizeRepo(repo) == null,
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Hint("Neue Versionen werden aus den GitHub-Releases dieses Repositorys geladen.")
                    SwitchRow("Täglich nach Updates suchen", autoUpdate, subtitle = "Nur im WLAN, Meldung per Benachrichtigung") {
                        autoUpdate = it; s.autoUpdateCheck = it; apply()
                    }
                    OutlinedButton(onClick = onOpenUpdate) { Text("Nach Updates suchen…") }
                }
            }
            item {
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
    }
}
