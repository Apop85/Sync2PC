package com.sync2pc.app.sync

import com.sync2pc.app.data.FolderMapping
import com.sync2pc.app.data.SyncDao
import com.sync2pc.app.smb.SmbSession

/**
 * Bestimmt den Monatsordner im Jahresordner – unter Berücksichtigung von Ordnern,
 * die der Benutzer umbenannt hat (nur die Monatszahl muss erhalten bleiben).
 *
 * Reihenfolge:
 *  1. Gespeicherte Benutzer-Entscheidung für (Jahr, Monat, Land)
 *  2. Standardname ("06" / "06 - Ägypten") existiert bereits -> verwenden
 *  3. Es gibt umbenannte Ordner mit gleicher Monatszahl -> Benutzer fragen
 *  4. Sonst Standardname neu anlegen
 */
class TargetResolver(
    /** Liefert eine lebende Verbindung (baut sie bei Bedarf neu auf) oder null = offline (Vorschau) */
    private val smbProvider: () -> SmbSession?,
    private val dao: SyncDao,
    private val profileId: Long,
    private val countryNames: Set<String>,
    private val dryRun: Boolean,
    private val offline: Boolean,
) {
    sealed class Resolution {
        data class Folder(val name: String) : Resolution()
        data class Ask(val groupKey: String, val defaultName: String, val candidates: List<String>) : Resolution()
    }

    /** Jahr -> Unterordner-Namen */
    private val yearFolders = HashMap<Int, MutableList<String>>()

    private fun foldersOf(year: Int): MutableList<String> =
        yearFolders.getOrPut(year) {
            val s = if (offline) null else smbProvider()
            s?.listDir(s.path(year.toString()))
                ?.filter { it.isDirectory }?.map { it.name }?.toMutableList()
                ?: mutableListOf()
        }

    private fun findExisting(year: Int, name: String): String? =
        foldersOf(year).firstOrNull { it.equals(name, ignoreCase = true) }

    /** Alle Ordner im Jahresordner mit dieser Monatszahl (für die Duplikatsuche) */
    fun monthFolders(year: Int, month: Int): List<String> =
        foldersOf(year).filter { FolderNames.monthOf(it) == month }

    fun folderCreated(year: Int, name: String) {
        if (findExisting(year, name) == null) foldersOf(year).add(name)
    }

    suspend fun resolve(year: Int, month: Int, countryKey: String?, countryName: String?): Resolution {
        val groupKey = FolderNames.groupKey(year, month, countryKey)

        // 1. Benutzer-Entscheidung
        dao.mapping(profileId, groupKey)?.let { m ->
            val existing = findExisting(year, m.folderName)
            if (existing != null) {
                if (!m.seen && !dryRun) dao.upsertMapping(m.copy(seen = true))
                return Resolution.Folder(existing)
            }
            if (!m.seen || offline) {
                // Neu gewählter Name, existiert noch nicht -> wird angelegt
                if (!dryRun) dao.upsertMapping(m.copy(seen = true))
                return Resolution.Folder(m.folderName)
            }
            // Ordner wurde seither umbenannt/gelöscht -> neu entscheiden
            if (!dryRun) dao.deleteMapping(profileId, groupKey)
        }

        // 2. Standardname vorhanden
        val default = FolderNames.defaultName(month, countryName)
        findExisting(year, default)?.let { return Resolution.Folder(it) }

        // 3. Umbenannte Ordner mit gleicher Monatszahl
        val candidates = foldersOf(year).filter {
            FolderNames.monthOf(it) == month && !FolderNames.isGeneratedName(it, countryNames)
        }
        if (candidates.isNotEmpty()) return Resolution.Ask(groupKey, default, candidates.sorted())

        // 4. Neu anlegen
        return Resolution.Folder(default)
    }

    companion object {
        fun mapping(profileId: Long, groupKey: String, folder: String) =
            FolderMapping(profileId, groupKey, FolderNames.sanitize(folder), seen = false)
    }
}
