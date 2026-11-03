package com.sync2pc.app.ui

import android.content.Context
import android.os.BatteryManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Erste Synchronisierung eines Profils (0.3.6): Hinweis, sie am Ladegerät auszuführen. */
object FirstSync {
    fun isCharging(ctx: Context): Boolean =
        ctx.getSystemService(BatteryManager::class.java)?.isCharging == true

    /**
     * Erste Synchronisierung dieses Profils? Noch nichts gesichert bzw. abgeglichen UND noch nie ein erfolgreicher
     * Lauf. Ohne die zweite Bedingung galt ein Profil mit leeren Ordnern bei jedem Start als "erstes Mal" (0.5.2).
     */
    suspend fun isFirst(ctx: Context, p: Profile): Boolean = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(ctx).dao()
        val nothingYet = if (p.type() == ProfileType.MIRROR) dao.mirrorStateCount(p.id) == 0 else dao.syncedCountNow(p.id) == 0
        nothingYet && dao.lastSuccessAt(p.id) == null
    }

    /** Warnen, wenn nicht am Strom und mindestens eines der Profile zum ersten Mal synchronisiert */
    suspend fun warnNeeded(ctx: Context, profiles: List<Profile>): Boolean =
        !isCharging(ctx) && profiles.filter { it.enabled && it.isConfigured() }.any { isFirst(ctx, it) }
}

@Composable
fun ChargeWarningDialog(onStart: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Erste Synchronisierung") },
        text = {
            Text("Die erste Synchronisierung überträgt alle vorhandenen Dateien. Je nach Menge dauert sie lange " +
                "und braucht viel Akku. Schliesse das Telefon am besten ans Ladegerät an.")
        },
        confirmButton = { TextButton(onClick = onStart) { Text("Trotzdem starten") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
