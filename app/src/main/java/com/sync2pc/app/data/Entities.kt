package com.sync2pc.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Was nach erfolgreichem (verifiziertem) Upload mit der Quelldatei passiert. */
enum class DeleteMode(val label: String) {
    KEEP("Dateien behalten"),
    TRASH("In den Papierkorb verschieben"),
    DELETE("Endgültig löschen");

    companion object {
        fun of(name: String?): DeleteMode = entries.firstOrNull { it.name == name } ?: KEEP
    }
}

/** Profiltyp: Bildordner (sortiert nach Jahr/Monat/Land) oder normaler Ordner (1:1-Spiegelung). */
enum class ProfileType(val label: String) {
    PHOTOS("Fotos & Videos sichern"),
    MIRROR("Ordner synchronisieren");

    companion object {
        fun of(name: String?): ProfileType = entries.firstOrNull { it.name == name } ?: PHOTOS
    }
}

/** Sync-Richtung (nur für normale Ordner; Bildordner immer Handy → PC). */
enum class SyncDirection(val label: String, val short: String) {
    UPLOAD("Handy → PC", "→ PC"),
    DOWNLOAD("PC → Handy", "→ Handy"),
    BOTH("Beidseitig", "⇄");

    val up: Boolean get() = this != DOWNLOAD
    val down: Boolean get() = this != UPLOAD

    companion object {
        fun of(name: String?): SyncDirection = entries.firstOrNull { it.name == name } ?: UPLOAD
    }
}

/** Verhalten bei Konflikt (Datei auf beiden Seiten geändert, nur beidseitig). */
enum class ConflictMode(val label: String) {
    ASK("Jedes Mal fragen"),
    NEWER("Immer die neuere Datei übernehmen");

    companion object {
        fun of(name: String?): ConflictMode = entries.firstOrNull { it.name == name } ?: ASK
    }
}

/** Ein Sync-Profil: Ziel-Share, Zugangsdaten, Quellordner, WLANs, Optionen. */
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "Standard",
    val enabled: Boolean = true,
    /** z. B. \\fs01.local\Fotos oder \\fs01.local\Fotos\Handy */
    val smbPath: String = "",
    val smbUser: String = "",
    val smbDomain: String = "",
    /** Mit Android-Keystore verschlüsselt (Base64), siehe CredentialCrypto */
    val smbPasswordEnc: String = "",
    /** Relative Pfade (MediaStore RELATIVE_PATH), zeilengetrennt, z. B. "DCIM/Camera/" */
    val sourceFolders: String = "",
    /** Erlaubte WLAN-Namen, zeilengetrennt */
    val wifiSsids: String = "",
    val includeImages: Boolean = true,
    val includeVideos: Boolean = true,
    val includeSubfolders: Boolean = true,
    val deleteMode: String = DeleteMode.KEEP.name,
    val verifyUpload: Boolean = true,
    // ---- seit DB v2 (0.0.7) ----
    val profileType: String = ProfileType.PHOTOS.name,
    val direction: String = SyncDirection.UPLOAD.name,
    /** SAF-Ordner-URI auf dem Handy (nur MIRROR), mit dauerhafter Lese-/Schreibberechtigung */
    val localTreeUri: String = "",
    /** Optionaler Unterordner auf dem Share, relativ zum Freigabe-Pfad (z. B. "Dokumente\Handy") */
    val remoteSubfolder: String = "",
    /** Löschungen auf die Gegenseite übernehmen (in deren .LeoSync-Papierkorb) */
    val propagateDeletes: Boolean = false,
    val conflictMode: String = ConflictMode.ASK.name,
    /** Whitelist (Muster wie *.pdf), zeilen-/kommagetrennt; leer = alles */
    val includePatterns: String = "",
    /** Blacklist (Muster wie Thumbs.db, *.tmp) */
    val excludePatterns: String = "",
    /** Wie oft auf Änderungen geprüft wird, solange im WLAN (Minuten, nur MIRROR) */
    val remotePollMinutes: Int = 60,
) {
    fun folders(): List<String> = splitLines(sourceFolders)
    fun ssids(): List<String> = splitLines(wifiSsids)
    fun delete(): DeleteMode = if (type() == ProfileType.PHOTOS) DeleteMode.of(deleteMode) else DeleteMode.KEEP
    /** Beim Löschen ist die Prüfung immer Pflicht. */
    fun mustVerify(): Boolean = verifyUpload || delete() != DeleteMode.KEEP
    fun type(): ProfileType = ProfileType.of(profileType)
    /** Bildordner sind immer Handy → PC */
    fun dir(): SyncDirection = if (type() == ProfileType.PHOTOS) SyncDirection.UPLOAD else SyncDirection.of(direction)
    fun conflict(): ConflictMode = ConflictMode.of(conflictMode)
    /** Genug eingerichtet, um zu synchronisieren? */
    fun isConfigured(): Boolean = smbPath.isNotBlank() &&
        (if (type() == ProfileType.PHOTOS) folders().isNotEmpty() else localTreeUri.isNotBlank())

    companion object {
        fun splitLines(s: String): List<String> =
            s.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        fun joinLines(list: Collection<String>): String =
            list.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("\n")
    }
}

object DeleteState {
    const val NONE = 0
    /** Löschen gewünscht, wartet auf andere Profile, die die Datei auch sichern */
    const val WANTED = 1
    /** Bereit für Papierkorb, wartet auf Bestätigung durch den Benutzer (Android 11+) */
    const val QUEUED_TRASH = 2
    const val DONE = 3
    /** Bereit für endgültiges Löschen, wartet auf Bestätigung (Android 11+) */
    const val QUEUED_DELETE = 4
}

/** Datei, die von einem Profil bereits auf den Share übertragen wurde. */
@Entity(
    tableName = "synced_files",
    primaryKeys = ["profileId", "sourceKey"],
    indices = [Index("deleteState")]
)
data class SyncedFile(
    val profileId: Long,
    /** RELATIVE_PATH + DISPLAY_NAME, z. B. "DCIM/Camera/IMG_1.jpg" */
    val sourceKey: String,
    val mediaUri: String,
    val size: Long,
    val dateModified: Long,
    val partialHash: String?,
    /** Pfad relativ zur Share-Wurzel */
    val remotePath: String,
    val syncedAt: Long,
    val verified: Boolean,
    val deleteState: Int = DeleteState.NONE,
)

/** Kompakte Sicht für den schnellen Abgleich zu Beginn eines Laufs. */
data class SyncedLite(
    val sourceKey: String,
    val size: Long,
    val dateModified: Long,
)

/**
 * Stand einer Datei beim letzten erfolgreichen Abgleich (Ordner-Spiegelung).
 * Grundlage des 3-Wege-Vergleichs: Was hat sich seitdem auf welcher Seite geändert / wurde gelöscht?
 */
@Entity(tableName = "mirror_state", primaryKeys = ["profileId", "relPath"])
data class MirrorState(
    val profileId: Long,
    /** Relativer Pfad in Kleinbuchstaben mit "/" (Schlüssel) */
    val relPath: String,
    val localSize: Long,
    val localModified: Long,
    val remoteSize: Long,
    val remoteModified: Long,
    val syncedAt: Long,
)

object ConflictResolution {
    const val KEEP_LOCAL = "LOCAL"
    const val KEEP_REMOTE = "REMOTE"
    const val KEEP_BOTH = "BOTH"
}

/** Konflikt, der auf eine Benutzerentscheidung wartet (resolution = null) oder entschieden ist. */
@Entity(tableName = "mirror_conflicts", primaryKeys = ["profileId", "relPath"])
data class MirrorConflict(
    val profileId: Long,
    /** Relativer Pfad wie angezeigt (Originalschreibweise) */
    val relPath: String,
    val localSize: Long,
    val localModified: Long,
    val remoteSize: Long,
    val remoteModified: Long,
    val detectedAt: Long,
    val resolution: String? = null,
)

/** Zeile für die Statistik (synchronisierte Datei + Analyse-Infos, falls vorhanden). */
data class StatRow(
    val sourceKey: String,
    val remotePath: String,
    val size: Long,
    val year: Int?,
    val countryName: String?,
)

/** Benutzerentscheidung: Gruppe (Jahr/Monat/Land) -> Monatsordner. */
@Entity(tableName = "folder_mappings", primaryKeys = ["profileId", "groupKey"])
data class FolderMapping(
    val profileId: Long,
    /** "2026/06/EG" bzw. "2026/06/-" ohne Land */
    val groupKey: String,
    val folderName: String,
    /** true, sobald der Ordner auf dem Share existiert hat. Verschwindet er danach, wird neu gefragt. */
    val seen: Boolean = false,
)

/** Offene Frage an den Benutzer: In welchen vorhandenen Monatsordner? */
@Entity(tableName = "pending_decisions", primaryKeys = ["profileId", "groupKey"])
data class PendingDecision(
    val profileId: Long,
    val groupKey: String,
    val year: Int,
    val month: Int,
    val countryIso: String?,
    val countryName: String?,
    val defaultName: String,
    /** Vorhandene, passende Ordner (zeilengetrennt) */
    val candidates: String,
    val fileCount: Int,
    val totalBytes: Long,
    /** Einige content://-URIs für Vorschaubilder (zeilengetrennt) */
    val sampleUris: String,
    val updatedAt: Long,
)

/** Zwischenspeicher der Analyse (Datum/Land), damit Dateien nicht mehrfach geöffnet werden. */
@Entity(tableName = "media_info")
data class MediaInfo(
    @PrimaryKey val sourceKey: String,
    val size: Long,
    val dateModified: Long,
    val year: Int,
    val month: Int,
    /** EXIF, META, FILENAME, MODIFIED */
    val dateSource: String,
    val latitude: Double?,
    val longitude: Double?,
    val countryIso: String?,
    val countryName: String?,
)

@Entity(tableName = "sync_runs")
data class SyncRun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val profileName: String,
    val trigger: String,
    val dryRun: Boolean,
    val startedAt: Long,
    val finishedAt: Long? = null,
    /** RUNNING, OK, PARTIAL, FAILED, CANCELLED */
    val status: String = "RUNNING",
    val uploaded: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val waiting: Int = 0,
    val bytes: Long = 0,
    val message: String = "",
)

@Entity(tableName = "log_entries", indices = [Index("runId")])
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val time: Long,
    /** I, W, E */
    val level: String,
    val message: String,
)
