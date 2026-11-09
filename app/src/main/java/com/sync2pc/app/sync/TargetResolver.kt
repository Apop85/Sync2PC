package com.sync2pc.app.sync

import com.sync2pc.app.data.FolderLayout
import com.sync2pc.app.data.FolderMapping
import com.sync2pc.app.data.SyncDao
import com.sync2pc.app.smb.RemoteDirs

/** Gespeicherte Ordner-Entscheidungen (in Tests durch ein Fake ersetzbar; Produktion: [SyncDao]) */
interface MappingStore {
    suspend fun mapping(profileId: Long, groupKey: String): FolderMapping?
    suspend fun upsertMapping(m: FolderMapping)
    suspend fun deleteMapping(profileId: Long, groupKey: String)

    companion object {
        fun of(dao: SyncDao): MappingStore = object : MappingStore {
            override suspend fun mapping(profileId: Long, groupKey: String) = dao.mapping(profileId, groupKey)
            override suspend fun upsertMapping(m: FolderMapping) = dao.upsertMapping(m)
            override suspend fun deleteMapping(profileId: Long, groupKey: String) = dao.deleteMapping(profileId, groupKey)
        }
    }
}

/**
 * Bestimmt den Zielordner (relativer Pfad unterhalb des Jahresordners) – unter Berücksichtigung von Ordnern,
 * die der Benutzer umbenannt hat (nur die Monatszahl muss erhalten bleiben). Je nach [layout] (SYNC-20261109-084):
 *  - V1 `MONTH_SUFFIX`: `MM` bzw. `MM - Land` im Jahr (DEC-016, unverändert)
 *  - V2 `COUNTRY_FOLDER`: `Land\MM` (ohne Land: `MM`); Länderordner nur am Standardnamen erkannt
 *  - V3 `COUNTRY_SUBFOLDER`: `MM\Land` (ohne Land: `MM`); Gruppe (Rückfrage) je Monat, nicht je Land
 *
 * Monatsordner, Reihenfolge:
 *  1. Gespeicherte Benutzer-Entscheidung für die Gruppe (Jahr, Monat, Land)
 *  2. Standardname ("06" / "06 - Ägypten") existiert bereits -> verwenden
 *  3. Es gibt umbenannte Ordner mit gleicher Monatszahl -> Benutzer fragen
 *  4. Sonst Standardname neu anlegen
 */
class TargetResolver(
    /** Liefert eine lebende Verbindung (baut sie bei Bedarf neu auf) oder null = offline (Vorschau) */
    private val smbProvider: () -> RemoteDirs?,
    private val mappings: MappingStore,
    private val profileId: Long,
    private val countryNames: Set<String>,
    private val dryRun: Boolean,
    private val offline: Boolean,
    /** Alle Namen eines Landes (alle Sprachen) zum Länderkürzel – findet Standardordner nach einem Sprachwechsel */
    private val namesOfCountry: (String) -> Collection<String> = { emptyList() },
    private val layout: FolderLayout = FolderLayout.MONTH_SUFFIX,
) {
    sealed class Resolution {
        /** [name] = relativer Pfad unterhalb des Jahres (`\`), z. B. "06", "06 - Ägypten", "Ägypten\06", "06\Ägypten" */
        data class Folder(val name: String) : Resolution()
        /** [defaultName] und [candidates] sind relative Pfade unterhalb des Jahres */
        data class Ask(val groupKey: String, val defaultName: String, val candidates: List<String>) : Resolution()
    }

    /** "Jahr\relativer Elternpfad" (klein) -> Unterordner-Namen */
    private val dirCache = HashMap<String, MutableList<String>>()
    private val monthCache = HashMap<Long, List<String>>()

    private fun join(parent: String, name: String) = if (parent.isEmpty()) name else "$parent\\$name"

    private fun foldersIn(year: Int, parent: String): MutableList<String> =
        dirCache.getOrPut("$year\\${parent.lowercase()}") {
            val s = if (offline) null else smbProvider()
            s?.listDir(s.path(year.toString(), parent))
                ?.filter { it.isDirectory }?.map { it.name }?.toMutableList()
                ?: mutableListOf()
        }

    private fun findExisting(year: Int, parent: String, name: String): String? =
        foldersIn(year, parent).firstOrNull { it.equals(name, ignoreCase = true) }

    /** Vorhandener Pfad (jedes Segment ohne Gross-/Kleinschreibung gesucht) in der vorhandenen Schreibweise, sonst null */
    private fun findExistingPath(year: Int, relPath: String): String? {
        var cur = ""
        for (seg in relPath.split('\\')) {
            cur = join(cur, findExisting(year, cur, seg) ?: return null)
        }
        return cur
    }

    /**
     * Alle Ordner des Monats im Jahr (für die Duplikatsuche, vorlagenunabhängig, siehe [MonthFolderSearch.related]).
     * Relative Pfade; gecacht, bis [folderCreated] etwas Neues meldet.
     */
    fun monthFolders(year: Int, month: Int): List<String> =
        monthCache.getOrPut(year * 100L + month) { MonthFolderSearch.related(month) { foldersIn(year, it) } }

    /** Der Ordner (relativer Pfad) gilt ab jetzt als vorhanden (wird vom Worker angelegt) */
    fun folderCreated(year: Int, relPath: String) {
        var parent = ""
        var added = false
        for (seg in relPath.split('\\')) {
            val list = foldersIn(year, parent)
            if (list.none { it.equals(seg, ignoreCase = true) }) { list.add(seg); added = true }
            parent = join(parent, seg)
        }
        if (added) monthCache.clear()
    }

    suspend fun resolve(year: Int, month: Int, countryKey: String?, countryName: String?): Resolution {
        val hasCountry = countryKey != null && countryName != null
        // V3: eine Gruppe je Monat (nicht je Land)
        val groupKey = FolderNames.groupKey(year, month, if (layout == FolderLayout.COUNTRY_SUBFOLDER) null else countryKey)

        val monthRes: Resolution = mappingOf(year, groupKey) ?: when (layout) {
            FolderLayout.MONTH_SUFFIX ->
                resolveIn(year, "", month, countryKey, countryName, groupKey, suffix = true)
            FolderLayout.COUNTRY_FOLDER -> {
                val parent = if (hasCountry) countryFolderIn(year, "", countryKey, countryName) else ""
                resolveIn(year, parent, month, null, null, groupKey, suffix = false)
            }
            FolderLayout.COUNTRY_SUBFOLDER ->
                resolveIn(year, "", month, null, null, groupKey, suffix = false)
        }
        if (layout != FolderLayout.COUNTRY_SUBFOLDER || !hasCountry || monthRes !is Resolution.Folder) return monthRes
        return Resolution.Folder(join(monthRes.name, countryFolderIn(year, monthRes.name, countryKey, countryName)))
    }

    /** Schritt 1: gespeicherte Benutzer-Entscheidung; null = keine (gültige) vorhanden */
    private suspend fun mappingOf(year: Int, groupKey: String): Resolution.Folder? {
        val stored = mappings.mapping(profileId, groupKey) ?: return null
        // Pfad beim Lesen bereinigen (Sicherung/DB): kein "..", höchstens 2 Segmente; sonst Zuordnung ignorieren
        val safe = FolderNames.sanitizePath(stored.folderName)
        if (safe.isEmpty() || safe.split('\\').size > 2) return null
        val m = if (safe == stored.folderName) stored else stored.copy(folderName = safe)
        val existing = findExistingPath(year, m.folderName)
        if (existing != null) {
            if (!m.seen && !dryRun) mappings.upsertMapping(m.copy(seen = true))
            return Resolution.Folder(existing)
        }
        if (!m.seen || offline) {
            // Neu gewählter Name, existiert noch nicht -> wird angelegt
            if (!dryRun) mappings.upsertMapping(m.copy(seen = true))
            return Resolution.Folder(m.folderName)
        }
        // Ordner wurde seither umbenannt/gelöscht -> neu entscheiden
        if (!dryRun) mappings.deleteMapping(profileId, groupKey)
        return null
    }

    /** Länderordner im Ordner [parent]: vorhandener Standardname in einer beliebigen App-Sprache, sonst der Name der App-Sprache */
    private fun countryFolderIn(year: Int, parent: String, countryKey: String, countryName: String): String {
        val own = FolderNames.sanitize(countryName)
        findExisting(year, parent, own)?.let { return it }
        for (alt in namesOfCountry(countryKey)) findExisting(year, parent, FolderNames.sanitize(alt))?.let { return it }
        return own
    }

    /** Schritte 2-4 für den Monatsordner im Ordner [parent]; [suffix] = V1 ("MM - Land") */
    private fun resolveIn(
        year: Int, parent: String, month: Int, countryKey: String?, countryName: String?, groupKey: String, suffix: Boolean,
    ): Resolution {
        val default = FolderNames.defaultName(month, if (suffix) countryName else null)
        // 2. Standardname vorhanden
        findExisting(year, parent, default)?.let { return Resolution.Folder(join(parent, it)) }
        // 2b. Standardname in einer anderen Sprache (z. B. "06 - Ägypten" nach dem Wechsel auf Englisch, 0.8.0)
        if (suffix && countryKey != null && countryName != null) for (alt in namesOfCountry(countryKey)) {
            findExisting(year, parent, FolderNames.defaultName(month, alt))?.let { return Resolution.Folder(join(parent, it)) }
        }
        // 3. Umbenannte Ordner mit gleicher Monatszahl (V2/V3: alles ausser "MM" gilt als umbenannt)
        val generated = if (suffix) countryNames else emptySet()
        val candidates = foldersIn(year, parent).filter {
            FolderNames.monthOf(it) == month && !FolderNames.isGeneratedName(it, generated)
        }
        if (candidates.isNotEmpty()) {
            return Resolution.Ask(groupKey, join(parent, default), candidates.sorted().map { join(parent, it) })
        }
        // 4. Neu anlegen
        return Resolution.Folder(join(parent, default))
    }

    companion object {
        /** Gespeicherte Zuordnung: relativer Pfad unter dem Jahr, jedes Segment bereinigt (V1: ein Segment) */
        fun mapping(profileId: Long, groupKey: String, folder: String) =
            FolderMapping(profileId, groupKey, FolderNames.sanitizePath(folder).ifEmpty { "_" }, seen = false)

        /**
         * Anzahl Segmente einer Ordnerentscheidung je Vorlage: V2 mit Land = Länderordner + Monatsordner (2),
         * sonst nur der Monatsordner (1; V3: Länderunterordner kommt automatisch dazu).
         */
        fun decisionSegments(layout: FolderLayout, groupKey: String): Int =
            if (layout == FolderLayout.COUNTRY_FOLDER && FolderNames.countryOfGroupKey(groupKey) != null) 2 else 1
    }
}
