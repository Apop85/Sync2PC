package com.sync2pc.app.ui

import androidx.compose.runtime.saveable.Saver
import com.sync2pc.app.data.Profile

/**
 * Speichert ein [Profile] im Instanzzustand der Activity (SYNC-20261111-218 B6): Der Assistent «Neues Profil» behält seine
 * Eingaben bei einer Konfigurationsänderung (Schriftgrösse, Hell/Dunkel, Sprache). Das Passwort gehört bewusst nicht dazu:
 * Der Editor hält es getrennt im Klartext, und das wird nicht in den Instanzzustand geschrieben (Benutzerfreigabe offen).
 */
internal object ProfileSaver {
    /** Anzahl Felder; ändert sich [Profile], schlägt [restore] bewusst fehl (null) statt falsche Werte zu setzen */
    private const val SIZE = 32

    fun save(p: Profile): ArrayList<Any> = arrayListOf(
        p.id, p.name, p.enabled, p.smbPath, p.smbUser, p.smbDomain, p.smbPasswordEnc, p.sourceFolders, p.wifiSsids,
        p.includeImages, p.includeVideos, p.includeSubfolders, p.deleteMode, p.verifyUpload, p.profileType, p.direction,
        p.localTreeUri, p.remoteSubfolder, p.propagateDeletes, p.conflictMode, p.includePatterns, p.excludePatterns,
        p.remotePollMinutes, p.trashRetentionDays, p.massDeletePercent, p.keepVersions, p.verifyLevel, p.pcCheck,
        p.pcGuid, p.pcGuidHost, p.folderLayout, p.smbEncryption,
    )

    fun restore(v: Any): Profile? {
        val l = v as? List<*> ?: return null
        if (l.size != SIZE) return null
        return try {
            Profile(
                id = l[0] as Long, name = l[1] as String, enabled = l[2] as Boolean, smbPath = l[3] as String,
                smbUser = l[4] as String, smbDomain = l[5] as String, smbPasswordEnc = l[6] as String,
                sourceFolders = l[7] as String, wifiSsids = l[8] as String, includeImages = l[9] as Boolean,
                includeVideos = l[10] as Boolean, includeSubfolders = l[11] as Boolean, deleteMode = l[12] as String,
                verifyUpload = l[13] as Boolean, profileType = l[14] as String, direction = l[15] as String,
                localTreeUri = l[16] as String, remoteSubfolder = l[17] as String, propagateDeletes = l[18] as Boolean,
                conflictMode = l[19] as String, includePatterns = l[20] as String, excludePatterns = l[21] as String,
                remotePollMinutes = l[22] as Int, trashRetentionDays = l[23] as Int, massDeletePercent = l[24] as Int,
                keepVersions = l[25] as Boolean, verifyLevel = l[26] as Int, pcCheck = l[27] as String,
                pcGuid = l[28] as String, pcGuidHost = l[29] as String, folderLayout = l[30] as String,
                smbEncryption = l[31] as String,
            )
        } catch (e: ClassCastException) { null }
    }

    /** Für ein Profil, das es im Zustand gibt */
    val saver: Saver<Profile, Any> = Saver(save = { save(it) }, restore = { restore(it) })

    /** Für ein Profil, das auch fehlen kann (Vergleichsstand «baseline») */
    val nullableSaver: Saver<Profile?, Any> = Saver(
        save = { it?.let { p -> save(p) } ?: "none" },
        restore = { restore(it) },
    )
}
