package com.sync2pc.app

import com.sync2pc.app.data.FolderMapping
import com.sync2pc.app.smb.RemoteEntry
import com.sync2pc.app.smb.RemoteFiles
import com.sync2pc.app.sync.MappingStore

/** Fake-Freigabe für Tests: Dateien (Pfad -> Grösse + Inhalts-Kennung) und Ordner im Speicher, Pfade ohne Gross-/Kleinschreibung */
class FakeShare(private val base: String = "") : RemoteFiles {
    private class F(val name: String, val size: Long, val hash: String)

    private val files = LinkedHashMap<String, F>()
    /** Schlüssel (klein) -> volle Schreibweise */
    private val dirs = LinkedHashMap<String, String>()
    /** Wie oft wurde eine Prüfsumme gelesen */
    var hashReads = 0
        private set

    private fun key(p: String) = p.lowercase()

    /** Datei unterhalb der Basis, z. B. add("2026\\06\\a.jpg", 100, "h1"); Elternordner entstehen mit */
    fun add(rel: String, size: Long = 100, hash: String = "h"): FakeShare {
        val full = path(rel)
        files[key(full)] = F(full.substringAfterLast('\\'), size, hash)
        addDir(rel.substringBeforeLast('\\', ""))
        return this
    }

    /** Ordner unterhalb der Basis (samt Elternordnern) */
    fun addDir(rel: String): FakeShare {
        var cur = ""
        for (seg in (base.split('\\') + rel.split('\\')).filter { it.isNotEmpty() }) {
            cur = if (cur.isEmpty()) seg else "$cur\\$seg"
            dirs.putIfAbsent(key(cur), cur)
        }
        return this
    }

    override fun path(vararg parts: String): String =
        (listOf(base) + parts).filter { it.isNotEmpty() }.joinToString("\\")

    override fun listDir(path: String): List<RemoteEntry> {
        if (path.isNotEmpty() && key(path) !in dirs) return emptyList()
        val prefix = if (path.isEmpty()) "" else key(path) + "\\"
        val out = ArrayList<RemoteEntry>()
        for ((k, full) in dirs) {
            if (k.startsWith(prefix) && k != key(path) && '\\' !in k.substring(prefix.length))
                out += RemoteEntry(full.substringAfterLast('\\'), 0, true)
        }
        for ((k, f) in files) {
            if (k.startsWith(prefix) && '\\' !in k.substring(prefix.length)) out += RemoteEntry(f.name, f.size, false)
        }
        return out
    }

    override fun stat(path: String): RemoteEntry? = files[key(path)]?.let { RemoteEntry(it.name, it.size, false) }

    override fun partialHash(path: String, size: Long): String {
        hashReads++
        return files[key(path)]?.hash ?: error("Datei fehlt: $path")
    }
}

/** Fake für gespeicherte Ordner-Entscheidungen */
class FakeMappings : MappingStore {
    val map = HashMap<String, FolderMapping>()
    override suspend fun mapping(profileId: Long, groupKey: String) = map["$profileId|$groupKey"]
    override suspend fun upsertMapping(m: FolderMapping) { map["${m.profileId}|${m.groupKey}"] = m }
    override suspend fun deleteMapping(profileId: Long, groupKey: String) { map.remove("$profileId|$groupKey") }
}
