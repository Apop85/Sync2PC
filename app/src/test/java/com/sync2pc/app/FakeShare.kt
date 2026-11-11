package com.sync2pc.app

import com.sync2pc.app.data.FolderMapping
import com.sync2pc.app.smb.RemoteEntry
import com.sync2pc.app.smb.RemoteStore
import com.sync2pc.app.sync.MappingStore

/**
 * Fake-Freigabe für Tests: Dateien (Pfad -> Grösse, Inhalts-Kennung, Änderungszeit) und Ordner im Speicher, Pfade ohne
 * Gross-/Kleinschreibung. Implementiert [RemoteStore] (SYNC-20261111-163 Stufe 2): Verschieben, Löschen, Ordner anlegen.
 * [onOp] wird vor jeder schreibenden Operation mit (Operation, Pfad) aufgerufen; ein Test wirft dort eine Exception oder
 * einen Error, um einen Fehler bzw. Prozessabbruch an genau dieser Stelle zu simulieren.
 */
class FakeShare(private val base: String = "") : RemoteStore {
    private class F(val name: String, val size: Long, val hash: String, val modified: Long)

    private val files = LinkedHashMap<String, F>()
    /** Schlüssel (klein) -> volle Schreibweise */
    private val dirs = LinkedHashMap<String, String>()
    /** Wie oft wurde eine Prüfsumme gelesen */
    var hashReads = 0
        private set
    /** Fehlerinjektion: (Operation "move"/"delete"/"deleteDir"/"ensureDir", Pfad) */
    var onOp: ((String, String) -> Unit)? = null
    /** Protokoll der schreibenden Operationen, z. B. "move a -> b" */
    val ops = ArrayList<String>()
    var closed = false
        private set

    private fun key(p: String) = p.lowercase()

    /** Datei unterhalb der Basis, z. B. add("2026\\06\\a.jpg", 100, "h1"); Elternordner entstehen mit */
    fun add(rel: String, size: Long = 100, hash: String = "h", modified: Long = 0L): FakeShare {
        val full = path(rel)
        files[key(full)] = F(full.substringAfterLast('\\'), size, hash, modified)
        addDir(rel.substringBeforeLast('\\', ""))
        return this
    }

    /** Ordner unterhalb der Basis (samt Elternordnern) */
    fun addDir(rel: String): FakeShare {
        mkdirs(base.split('\\') + rel.split('\\'))
        return this
    }

    private fun mkdirs(segments: List<String>) {
        var cur = ""
        for (seg in segments.filter { it.isNotEmpty() }) {
            cur = if (cur.isEmpty()) seg else "$cur\\$seg"
            dirs.putIfAbsent(key(cur), cur)
        }
    }

    /** Vorhandene Datei (volle Pfadschreibweise der Freigabe) oder null */
    fun fileAt(path: String): Boolean = key(path) in files

    /** Alle Dateipfade (klein), sortiert, für Vergleiche in Tests */
    fun allFiles(): List<String> = files.keys.sorted()

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
            if (k.startsWith(prefix) && '\\' !in k.substring(prefix.length)) out += RemoteEntry(f.name, f.size, false, f.modified)
        }
        return out
    }

    override fun stat(path: String): RemoteEntry? = files[key(path)]?.let { RemoteEntry(it.name, it.size, false, it.modified) }

    override fun partialHash(path: String, size: Long): String {
        hashReads++
        return files[key(path)]?.hash ?: error("Datei fehlt: $path")
    }

    override fun dirExists(path: String): Boolean = path.isEmpty() || key(path) in dirs

    override fun listRecursive(basePath: String, skipDir: (String) -> Boolean): Map<String, RemoteEntry> {
        val out = LinkedHashMap<String, RemoteEntry>()
        fun walk(rel: String) {
            val full = if (rel.isEmpty()) basePath else (if (basePath.isEmpty()) rel.replace('/', '\\') else basePath + "\\" + rel.replace('/', '\\'))
            for (e in listDir(full)) {
                val childRel = if (rel.isEmpty()) e.name else "$rel/${e.name}"
                if (e.isDirectory) { if (!skipDir(childRel)) walk(childRel) } else out[childRel] = e
            }
        }
        walk("")
        return out
    }

    override fun ensureDir(path: String) {
        onOp?.invoke("ensureDir", path)
        if (path.isEmpty()) return
        mkdirs(path.split('\\'))
        ops += "ensureDir $path"
    }

    /** Wie SMB: Zielordner muss existieren, die Quelle muss da sein, ein vorhandenes Ziel wird nicht überschrieben */
    override fun move(from: String, to: String) {
        onOp?.invoke("move", from)
        val f = files[key(from)] ?: throw java.io.IOException("Quelle fehlt: $from")
        if (key(to) in files) throw java.io.IOException("Ziel existiert: $to")
        val parent = to.substringBeforeLast('\\', "")
        if (!dirExists(parent)) throw java.io.IOException("Zielordner fehlt: $parent")
        files.remove(key(from))
        files[key(to)] = F(to.substringAfterLast('\\'), f.size, f.hash, f.modified)
        ops += "move $from -> $to"
    }

    override fun delete(path: String) {
        onOp?.invoke("delete", path)
        if (files.remove(key(path)) != null) ops += "delete $path"
    }

    override fun deleteDir(path: String) {
        onOp?.invoke("deleteDir", path)
        if (path.isEmpty() || key(path) !in dirs) return
        val prefix = key(path) + "\\"
        files.keys.removeAll { it.startsWith(prefix) }
        dirs.keys.removeAll { it == key(path) || it.startsWith(prefix) }
        ops += "deleteDir $path"
    }

    override fun contentHash(path: String, size: Long, check: () -> Unit, onBytes: (Long) -> Unit): String {
        hashReads++
        check()
        return files[key(path)]?.hash ?: error("Datei fehlt: $path")
    }

    override fun close() { closed = true }
}
/** Fake für gespeicherte Ordner-Entscheidungen */
class FakeMappings : MappingStore {
    val map = HashMap<String, FolderMapping>()
    override suspend fun mapping(profileId: Long, groupKey: String) = map["$profileId|$groupKey"]
    override suspend fun upsertMapping(m: FolderMapping) { map["${m.profileId}|${m.groupKey}"] = m }
    override suspend fun deleteMapping(profileId: Long, groupKey: String) { map.remove("$profileId|$groupKey") }
}
