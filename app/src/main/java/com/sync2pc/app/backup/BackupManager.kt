package com.sync2pc.app.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.sync2pc.app.BuildConfig
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.CredentialCrypto
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.ProfileOverlap
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.mirror.LocalTree
import com.sync2pc.app.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Export und Import der Sicherung über Dateien, die der Benutzer per Android-Dateiauswahl wählt. */
object BackupManager {

    data class ExportResult(val profiles: Int, val synced: Int, val withPasswords: Boolean)

    data class ImportResult(
        val profiles: Int,
        val synced: Int,
        /** Profile, deren SMB-Passwort neu eingegeben werden muss */
        val withoutPassword: List<String>,
        /** Ordner-Profile: Handy-Ordner muss neu gewählt werden (Android überträgt die Freigabe nicht) */
        val needFolder: List<String>,
        /** Nicht importiert, weil ein identisches Profil (gleiches Ziel, gleiche Quellen) schon besteht */
        val skippedIdentical: List<String> = emptyList(),
        /** Importiert, überschneidet sich aber mit einem bestehenden Profil (gleiches Ziel, gemeinsame Quellordner) */
        val overlapping: List<String> = emptyList(),
    )

    fun suggestedFileName(): String =
        "LeoSync-Sicherung_" + SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date()) + ".json"

    suspend fun export(context: Context, target: Uri, includeSyncState: Boolean, exportPassword: String?): ExportResult =
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.get(context).dao()
            val profiles = dao.profiles()
            val usePw = !exportPassword.isNullOrEmpty()
            val content = BackupContent(
                appVersion = BuildConfig.VERSION_NAME,
                createdAt = System.currentTimeMillis(),
                settings = GlobalSettings(context).exportable(),
                profiles = profiles,
                passwords = if (usePw) profiles.associate { it.id to CredentialCrypto.decrypt(it.smbPasswordEnc) } else emptyMap(),
                mappings = dao.allMappings(),
                synced = if (includeSyncState) dao.allSynced() else emptyList(),
                mirrorStates = if (includeSyncState) dao.allMirrorStates() else emptyList(),
            )
            val json = BackupCodec.encode(content, exportPassword)
            (context.contentResolver.openOutputStream(target, "wt") ?: throw BackupException("Datei kann nicht geschrieben werden"))
                .use { it.write(json.toByteArray(Charsets.UTF_8)) }
            ExportResult(profiles.size, content.synced.size, usePw)
        }

    suspend fun read(context: Context, source: Uri): String = withContext(Dispatchers.IO) {
        (context.contentResolver.openInputStream(source) ?: throw BackupException("Datei kann nicht gelesen werden"))
            .use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /**
     * Spielt eine Sicherung ein.
     * replace = true: Alle bisherigen Profile (inkl. Sync-Status) werden ersetzt, Einstellungen übernommen.
     * replace = false: Profile werden zusätzlich angelegt, Einstellungen bleiben unverändert.
     */
    suspend fun import(context: Context, backup: ParsedBackup, replace: Boolean): ImportResult = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val dao = db.dao()
        val c = backup.content
        val withoutPw = ArrayList<String>()
        val needFolder = ArrayList<String>()
        val skipped = ArrayList<String>()
        val overlapping = ArrayList<String>()
        var synced = 0
        var imported = 0
        db.withTransaction {
            if (replace) {
                for (old in dao.profiles()) {
                    dao.clearSynced(old.id); dao.clearMappings(old.id); dao.clearDecisions(old.id)
                    dao.clearMirrorState(old.id); dao.clearConflicts(old.id); dao.deleteProfileRow(old.id)
                }
            }
            // Bestehende (bei "Ersetzen" keine) + bereits importierte Profile -> doppelte erkennen
            val present = dao.profiles().toMutableList()
            for (p in c.profiles) {
                // id = -1: die ID aus der Sicherung darf nicht mit einer bestehenden ID verwechselt werden
                when (ProfileOverlap.check(p.copy(id = -1), present)?.kind) {
                    ProfileOverlap.Kind.IDENTICAL -> { skipped += p.name; continue }
                    ProfileOverlap.Kind.PARTIAL -> overlapping += p.name
                    null -> {}
                }
                val pw = c.passwords[p.id].orEmpty()
                val newId = dao.insertProfile(p.copy(id = 0, smbPasswordEnc = CredentialCrypto.encrypt(pw)))
                present += p.copy(id = newId)
                imported++
                if (pw.isEmpty() && p.smbUser.isNotBlank()) withoutPw += p.name
                if (p.type() == ProfileType.MIRROR &&
                    (p.localTreeUri.isBlank() || !LocalTree.hasPermission(context, Uri.parse(p.localTreeUri)))
                ) needFolder += p.name
                dao.upsertMappings(c.mappings.filter { it.profileId == p.id }.map { it.copy(profileId = newId) })
                val files = c.synced.filter { it.profileId == p.id }.map { it.copy(profileId = newId) }
                files.chunked(500).forEach { dao.upsertSyncedAll(it) }
                synced += files.size
                c.mirrorStates.filter { it.profileId == p.id }.map { it.copy(profileId = newId) }
                    .chunked(500).forEach { dao.upsertMirrorStates(it) }
            }
        }
        if (replace) GlobalSettings(context).importFrom(c.settings)
        SyncScheduler.updateRemotePoll(context, dao.profiles())
        SyncScheduler.setupAll(context)
        ImportResult(imported, synced, withoutPw, needFolder, skipped, overlapping)
    }
}
