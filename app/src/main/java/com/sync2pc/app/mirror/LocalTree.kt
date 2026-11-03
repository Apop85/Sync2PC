package com.sync2pc.app.mirror

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.webkit.MimeTypeMap
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

data class LocalEntry(
    val docId: String,
    val name: String,
    val size: Long,
    val modified: Long,
    val isDir: Boolean,
)

/**
 * Zugriff auf einen vom Benutzer freigegebenen Handy-Ordner (Storage Access Framework).
 * Nötig für beliebige Dateitypen (PDF, Dokumente …), die der MediaStore ab Android 11 nicht liefert.
 * Pfade sind relativ zum freigegebenen Ordner, Trenner "/".
 */
class LocalTree(private val context: Context, val treeUri: Uri) {
    private val resolver = context.contentResolver
    val rootDocId: String = DocumentsContract.getTreeDocumentId(treeUri)

    /** relativer Ordnerpfad (Kleinbuchstaben) -> Dokument-ID (parallele Übertragungen -> threadsicher) */
    private val dirIds = java.util.concurrent.ConcurrentHashMap<String, String>().apply { put("", rootDocId) }

    companion object {
        const val FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        /** Ist die dauerhafte Lese-/Schreibberechtigung für den Ordner noch vorhanden? */
        fun hasPermission(context: Context, uri: Uri): Boolean =
            context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission && it.isWritePermission }

        /**
         * Anzeige: "primary:Documents/Scans" -> "Documents/Scans" (SD-Karte: "SD-Karte: …").
         * Über "Downloads" gewählte Ordner liefern "raw:/storage/emulated/0/Download/Test" -> "Download/Test".
         */
        fun label(uri: Uri): String = try {
            val id = DocumentsContract.getTreeDocumentId(uri)
            val vol = id.substringBefore(':')
            val path = id.substringAfter(':', "")
            when {
                vol == "primary" -> path.ifEmpty { "(Hauptordner)" }
                vol == "raw" || path.startsWith("/storage/") -> rawPathLabel(path)
                uri.authority == "com.android.providers.downloads.documents" -> "Download" + (if (path.isNotEmpty()) "/$path" else "")
                else -> "SD-Karte: " + path.ifEmpty { "(Hauptordner)" }
            }
        } catch (e: Exception) { uri.toString() }

        /** "/storage/emulated/0/Download/Test" -> "Download/Test", "/storage/1234-ABCD/DCIM" -> "SD-Karte: DCIM" */
        fun rawPathLabel(path: String): String {
            val internal = Regex("""^/storage/emulated/\d+/?""")
            if (internal.containsMatchIn(path)) return path.replace(internal, "").trimEnd('/').ifEmpty { "(Hauptordner)" }
            val sd = Regex("""^/storage/[^/]+/?""")
            return "SD-Karte: " + path.replace(sd, "").trimEnd('/').ifEmpty { "(Hauptordner)" }
        }

        fun mimeOf(name: String): String =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
                ?: "application/octet-stream"
    }

    private fun docUri(docId: String): Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

    private val projection = arrayOf(
        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
        Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED,
    )

    /**
     * Inhalt eines Ordners. Liefert Android keine Antwort (Abfrage = null), ist das ein Fehler und nicht
     * "leer" – sonst sähe ein Lauf alle Dateien als gelöscht an (0.4.1).
     */
    fun children(docId: String): List<LocalEntry> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val out = ArrayList<LocalEntry>()
        val cursor = resolver.query(uri, projection, null, null, null)
            ?: throw IOException("Handy-Ordner nicht lesbar")
        cursor.use { c ->
            while (c.moveToNext()) {
                val mime = c.getString(2) ?: ""
                out += LocalEntry(
                    docId = c.getString(0),
                    name = c.getString(1) ?: continue,
                    size = if (c.isNull(3)) 0 else c.getLong(3),
                    modified = if (c.isNull(4)) 0 else c.getLong(4),
                    isDir = mime == Document.MIME_TYPE_DIR,
                )
            }
        }
        return out
    }

    /**
     * Alle Dateien rekursiv, Schlüssel = relativer Pfad. [skipDir] lässt Zweige aus.
     * Liegengebliebene "<name>.s2pold" (Abbruch beim Ersetzen, siehe [replaceWith]):
     * fehlt "<name>", ist sie die gültige Datei -> mit [recover] zurückbenennen; sonst ist sie überholt
     * -> mit [recover] löschen. Ohne [recover] (Simulation) wird nichts verändert.
     */
    fun listRecursive(skipDir: (String) -> Boolean = { false }, recover: Boolean = false): Map<String, LocalEntry> {
        val out = LinkedHashMap<String, LocalEntry>()
        fun walk(rel: String, docId: String) {
            val all = children(docId)
            val names = all.filter { !it.isDir }.map { it.name.lowercase() }.toSet()
            for (e in all) {
                val childRel = if (rel.isEmpty()) e.name else "$rel/${e.name}"
                if (e.isDir) {
                    dirIds[childRel.lowercase()] = e.docId
                    if (!skipDir(childRel)) walk(childRel, e.docId)
                } else if (e.name.endsWith(OLD, ignoreCase = true)) {
                    val base = e.name.dropLast(OLD.length)
                    val baseRel = if (rel.isEmpty()) base else "$rel/$base"
                    if (base.lowercase() in names) {
                        if (recover) try { delete(e.docId) } catch (ignored: Exception) {}
                    } else if (recover) {
                        val id = rename(e.docId, base)
                        out[baseRel] = stat(id) ?: e.copy(docId = id, name = base)
                    } else out[baseRel] = e.copy(name = base)
                } else out[childRel] = e
            }
        }
        walk("", rootDocId)
        return out
    }

    /** Datei unter einem relativen Pfad suchen (Gross-/Kleinschreibung egal), null = nicht vorhanden */
    fun find(relPath: String): LocalEntry? {
        val parentRel = relPath.substringBeforeLast('/', "")
        val name = relPath.substringAfterLast('/')
        val parentId = findDir(parentRel) ?: return null
        return children(parentId).firstOrNull { !it.isDir && it.name.equals(name, ignoreCase = true) }
    }

    /** Dokument-ID eines Ordners (ohne ihn anzulegen), null = nicht vorhanden */
    fun findDir(relDir: String): String? {
        if (relDir.isEmpty()) return rootDocId
        dirIds[relDir.lowercase()]?.let { return it }
        var id = rootDocId
        for (part in relDir.split('/')) {
            id = children(id).firstOrNull { it.isDir && it.name.equals(part, ignoreCase = true) }?.docId ?: return null
        }
        dirIds[relDir.lowercase()] = id
        return id
    }

    /** Gründlicher Inhalts-Hash für den Ordner-Vergleich (siehe [com.sync2pc.app.sync.PartialHash.thorough]) */
    fun contentHash(docId: String, size: Long): String =
        (openInput(docId) as ParcelFileDescriptor.AutoCloseInputStream).use { ins ->
            com.sync2pc.app.sync.PartialHash.thorough(size) { off, len ->
                val bb = java.nio.ByteBuffer.allocate(len)
                var pos = off
                while (bb.hasRemaining()) {
                    val n = ins.channel.read(bb, pos)
                    if (n < 0) break
                    pos += n
                }
                if (bb.position() == len) bb.array() else bb.array().copyOf(bb.position())
            }
        }

    fun stat(docId: String): LocalEntry? =
        resolver.query(docUri(docId), projection, null, null, null)?.use { c ->
            if (!c.moveToFirst()) null
            else LocalEntry(
                c.getString(0), c.getString(1) ?: "",
                if (c.isNull(3)) 0 else c.getLong(3),
                if (c.isNull(4)) 0 else c.getLong(4),
                c.getString(2) == Document.MIME_TYPE_DIR,
            )
        }

    /**
     * Ordner (inkl. Zwischenebenen) anlegen, liefert die Dokument-ID.
     * Synchronized: Legen zwei Übertragungen gleichzeitig denselben Ordner an, entstünde sonst "Ordner (1)".
     */
    @Synchronized
    fun ensureDir(relDir: String): String {
        if (relDir.isEmpty()) return rootDocId
        dirIds[relDir.lowercase()]?.let { return it }
        val parentRel = relDir.substringBeforeLast('/', "")
        val name = relDir.substringAfterLast('/')
        val parentId = ensureDir(parentRel)
        val existing = children(parentId).firstOrNull { it.isDir && it.name.equals(name, ignoreCase = true) }
        val id = existing?.docId ?: run {
            val uri = DocumentsContract.createDocument(resolver, docUri(parentId), Document.MIME_TYPE_DIR, name)
                ?: throw IOException("Ordner '$relDir' konnte nicht angelegt werden")
            DocumentsContract.getDocumentId(uri)
        }
        dirIds[relDir.lowercase()] = id
        return id
    }

    fun openInput(docId: String, offset: Long = 0): InputStream {
        val pfd = resolver.openFileDescriptor(docUri(docId), "r") ?: throw IOException("Datei nicht lesbar")
        val ins = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        if (offset > 0) ins.channel.position(offset)
        return ins
    }

    /** Liest Bytes an einer Position (für den Teil-Hash) */
    fun readAt(docId: String, offset: Long, length: Int): ByteArray =
        (openInput(docId) as ParcelFileDescriptor.AutoCloseInputStream).use { ins ->
            val bb = java.nio.ByteBuffer.allocate(length)
            var pos = offset
            while (bb.hasRemaining()) {
                val n = ins.channel.read(bb, pos)
                if (n < 0) break
                pos += n
            }
            if (bb.position() == length) bb.array() else bb.array().copyOf(bb.position())
        }

    /**
     * Schreibt eine Datei sicher: zuerst "<name>.s2ptmp", dann Ersetzen der alten Datei und Umbenennen.
     * [write] erhält den Ausgabestrom. Liefert die Dokument-ID der fertigen Datei.
     */
    fun writeFile(relPath: String, write: (OutputStream) -> Unit): String {
        val parentRel = relPath.substringBeforeLast('/', "")
        val name = relPath.substringAfterLast('/')
        val parentId = ensureDir(parentRel)
        val siblings = children(parentId)
        siblings.firstOrNull { !it.isDir && it.name == name + TEMP }?.let { delete(it.docId) }
        val tmpUri = DocumentsContract.createDocument(resolver, docUri(parentId), "application/octet-stream", name + TEMP)
            ?: throw IOException("Datei '$relPath' konnte nicht angelegt werden")
        try {
            (resolver.openOutputStream(tmpUri, "w") ?: throw IOException("Datei nicht beschreibbar")).use(write)
        } catch (e: Exception) {
            try { DocumentsContract.deleteDocument(resolver, tmpUri) } catch (ignored: Exception) {}
            throw e
        }
        return replaceWith(tmpUri, siblings, name, relPath)
    }

    /**
     * Ersetzt "<name>" durch die fertige Temp-Datei, ohne dass die alte Version vorher verloren geht (0.4.1):
     * alte Datei -> "<name>.s2pold", Temp -> "<name>", erst danach "<name>.s2pold" löschen.
     * Scheitert das Umbenennen der Temp-Datei, wird die alte Version zurückbenannt. Bricht die App mittendrin ab,
     * räumt der nächste Lauf auf ([listRecursive] mit recover = true).
     */
    private fun replaceWith(tmpUri: Uri, siblings: List<LocalEntry>, name: String, relPath: String): String {
        val old = siblings.firstOrNull { !it.isDir && it.name.equals(name, ignoreCase = true) }
        var backupId: String? = null
        if (old != null) {
            siblings.firstOrNull { !it.isDir && it.name.equals(name + OLD, ignoreCase = true) }
                ?.let { try { delete(it.docId) } catch (ignored: Exception) {} }
            backupId = rename(old.docId, name + OLD)
        }
        val finalUri = try {
            DocumentsContract.renameDocument(resolver, tmpUri, name)
                ?: throw IOException("Umbenennen von '$relPath' fehlgeschlagen")
        } catch (e: Exception) {
            // Alte Version wiederherstellen; die Temp-Datei bleibt liegen (wird nie synchronisiert)
            backupId?.let { try { rename(it, name) } catch (ignored: Exception) {} }
            throw e
        }
        backupId?.let { try { delete(it) } catch (ignored: Exception) {} }
        return DocumentsContract.getDocumentId(finalUri)
    }

    /**
     * Wie [writeFile], aber ein abgebrochener Download wird fortgesetzt: Eine vorhandene "<name>.s2ptmp"
     * wird weitergeschrieben, wenn [TransferPlan.resumeOffset] es erlaubt. [write] erhält den Ausgabestrom
     * und die Startposition. Liefert Dokument-ID der fertigen Datei und die Startposition (0 = neu).
     * Die Temp-Datei bleibt bei einem Abbruch liegen (wird nie synchronisiert, siehe FileFilter).
     */
    fun writeFileResumable(
        relPath: String, expectedSize: Long, remoteModified: Long,
        /** Die Datei gab es beim Einlesen nicht: ist sie inzwischen entstanden -> [ChangedDuringRunException] */
        expectNew: Boolean = false,
        write: (OutputStream, Long) -> Unit,
    ): Pair<String, Long> {
        val parentRel = relPath.substringBeforeLast('/', "")
        val name = relPath.substringAfterLast('/')
        val parentId = ensureDir(parentRel)
        val siblings = children(parentId)
        if (expectNew && siblings.any { !it.isDir && it.name.equals(name, ignoreCase = true) })
            throw ChangedDuringRunException()
        var offset = 0L
        var tmpUri: Uri? = null
        val tmp = siblings.firstOrNull { !it.isDir && it.name == name + TEMP }
        if (tmp != null) {
            offset = TransferPlan.resumeOffset(tmp.size, tmp.modified, expectedSize, remoteModified)
            if (offset > 0) {
                tmpUri = docUri(tmp.docId)
            } else {
                delete(tmp.docId)
            }
        }
        var out: OutputStream? = null
        if (tmpUri != null) {
            // "wa" = anhängen; unterstützt der Anbieter das nicht, neu beginnen
            out = try { resolver.openOutputStream(tmpUri, "wa") } catch (e: Exception) { null }
            if (out == null) { try { DocumentsContract.deleteDocument(resolver, tmpUri) } catch (ignored: Exception) {}; tmpUri = null; offset = 0 }
        }
        if (tmpUri == null) {
            tmpUri = DocumentsContract.createDocument(resolver, docUri(parentId), "application/octet-stream", name + TEMP)
                ?: throw IOException("Datei '$relPath' konnte nicht angelegt werden")
            out = resolver.openOutputStream(tmpUri, "w") ?: throw IOException("Datei nicht beschreibbar")
        }
        val uri = tmpUri
        out!!.use { write(it, offset) }
        // Grösse prüfen: Hat der Anbieter "anhängen" ignoriert, stimmt sie nicht -> verwerfen, nächstes Mal neu
        val written = stat(DocumentsContract.getDocumentId(uri))?.size ?: -1
        if (written != expectedSize) {
            try { DocumentsContract.deleteDocument(resolver, uri) } catch (ignored: Exception) {}
            throw IOException("Grösse nach Download stimmt nicht ($written statt $expectedSize Bytes)")
        }
        return replaceWith(uri, siblings, name, relPath) to offset
    }

    fun rename(docId: String, newName: String): String {
        val uri = DocumentsContract.renameDocument(resolver, docUri(docId), newName)
            ?: throw IOException("Umbenennen fehlgeschlagen")
        return DocumentsContract.getDocumentId(uri)
    }

    fun delete(docId: String) {
        DocumentsContract.deleteDocument(resolver, docUri(docId))
    }

    /**
     * Verschiebt eine Datei in "<Ordner>/.LeoSync-Papierkorb/<Datum>/<relPath>". Gibt es den Namen dort schon,
     * bekommt sie die Uhrzeit angehängt ("a (14-32-05).txt", 0.7.0). Liefert den Pfad im Papierkorb.
     */
    fun moveToTrash(relPath: String, entry: LocalEntry, dateFolder: String): String {
        val parentRel = relPath.substringBeforeLast('/', "")
        val sourceParent = ensureDir(parentRel)
        val trashDir = listOf(MirrorEngine.TRASH_DIR, dateFolder, parentRel).filter { it.isNotEmpty() }.joinToString("/")
        val targetParent = ensureDir(trashDir)
        val existing = children(targetParent).map { it.name.lowercase() }.toSet()
        val name = TrashIndex.uniqueTrashName(entry.name, TrashIndex.timeNow()) { it.lowercase() in existing }
        var docId = entry.docId
        if (name != entry.name) docId = rename(docId, name)
        try {
            DocumentsContract.moveDocument(resolver, docUri(docId), docUri(sourceParent), docUri(targetParent))
                ?: throw IOException("Verschieben nicht unterstützt")
        } catch (e: Exception) {
            // Anbieter kann nicht verschieben -> kopieren + löschen
            val target = "$trashDir/$name"
            writeFile(target) { out -> openInput(docId).use { it.copyTo(out, 1 shl 16) } }
            delete(docId)
        }
        return "$trashDir/$name"
    }

    /** Alle Dateien im Papierkorb (neuer und alter Name) als Einträge für die Wiederherstellung (0.5.0) */
    fun listTrash(): List<TrashEntry> {
        val out = ArrayList<TrashEntry>()
        fun walk(rel: String, docId: String) {
            val all = children(docId)
            val names = all.filter { !it.isDir }.map { it.name.lowercase() }.toSet()
            for (e in all) {
                val r = "$rel/${e.name}"
                if (e.isDir) walk(r, e.docId)
                else if (!e.name.endsWith(TEMP, true) && !e.name.endsWith(OLD, true))
                    TrashIndex.entry(Side.LOCAL, r, e.size, e.modified, names)?.let { out += it }
            }
        }
        children(rootDocId).filter {
            it.isDir && (it.name.equals(MirrorEngine.TRASH_DIR, true) || it.name.equals(MirrorEngine.OLD_TRASH_DIR, true))
        }.forEach { walk(it.name, it.docId) }
        return out
    }

    /**
     * Verschiebt eine Datei von [fromRel] nach [toRel] (Wiederherstellen aus dem Papierkorb, 0.5.0) und benennt sie
     * dabei um. Kann der Anbieter nicht verschieben: kopieren + löschen. Liefert die Dokument-ID am neuen Ort.
     */
    fun moveFile(fromRel: String, entry: LocalEntry, toRel: String): String {
        val sourceParent = ensureDir(fromRel.substringBeforeLast('/', ""))
        val targetParent = ensureDir(toRel.substringBeforeLast('/', ""))
        val name = toRel.substringAfterLast('/')
        val moved = try {
            DocumentsContract.moveDocument(resolver, docUri(entry.docId), docUri(sourceParent), docUri(targetParent))
                ?.let { DocumentsContract.getDocumentId(it) }
        } catch (e: Exception) { null }
        if (moved == null) {
            val id = writeFile(toRel) { out -> openInput(entry.docId).use { it.copyTo(out, 1 shl 16) } }
            delete(entry.docId)
            return id
        }
        return if (entry.name != name) rename(moved, name) else moved
    }

    /**
     * "Papierkorb leeren" (0.6.0): Datumsordner löschen, die [rule] trifft; ALL löscht die ganzen Papierkorb-Ordner.
     * Liefert die Anzahl gelöschter Ordner.
     */
    fun purgeTrash(rule: PurgeRule, keep: Set<String> = emptySet()): Int {
        var n = 0
        val trashDirs = children(rootDocId).filter {
            it.isDir && (it.name.equals(MirrorEngine.TRASH_DIR, true) || it.name.equals(MirrorEngine.OLD_TRASH_DIR, true))
        }
        for (t in trashDirs) {
            val keepHere = keep.filter { it.startsWith(t.name.lowercase() + "/") }
            if (rule.mode == PurgeRule.Mode.ALL && keepHere.isEmpty()) { delete(t.docId); n++ }
            else for (d in children(t.docId)) {
                if (!d.isDir || !rule.matches(d.name)) continue
                val prefix = "${t.name}/${d.name}/".lowercase()
                // Vorgemerkte Wiederherstellungen darin (0.7.0) -> nur die übrigen Dateien löschen
                if (keepHere.none { it.startsWith(prefix) }) { delete(d.docId); n++ }
                else n += deleteExcept(d.docId, "${t.name}/${d.name}", keep)
            }
            forgetDirs(t.name)
        }
        return n
    }

    /** Alle Dateien unterhalb löschen ausser [keep] (Pfade in Kleinbuchstaben). Liefert die Anzahl gelöschter Dateien */
    private fun deleteExcept(docId: String, rel: String, keep: Set<String>): Int {
        var n = 0
        for (e in children(docId)) {
            val r = "$rel/${e.name}"
            if (e.isDir) n += deleteExcept(e.docId, r, keep)
            else if (r.lowercase() !in keep) { delete(e.docId); n++ }
        }
        return n
    }

    /** Eine einzelne Datei im Papierkorb endgültig löschen. false = nicht (mehr) vorhanden */
    fun deleteTrashFile(trashPath: String): Boolean {
        val e = find(trashPath) ?: return false
        delete(e.docId)
        return true
    }

    /** Gemerkte Ordner-IDs unterhalb von [relDir] verwerfen (nach dem Löschen sonst veraltet) */
    private fun forgetDirs(relDir: String) {
        val k = relDir.lowercase()
        dirIds.keys.removeIf { it == k || it.startsWith("$k/") }
    }

    private val TEMP = com.sync2pc.app.smb.SmbSession.TEMP_SUFFIX
    private val OLD = OLD_SUFFIX
}

/** Alte Version während des Ersetzens auf dem Handy (0.4.1), wird nie synchronisiert */
const val OLD_SUFFIX = ".s2pold"

/** Zieldatei wurde seit dem Einlesen geändert bzw. ist neu entstanden -> nicht überschreiben (0.4.1) */
class ChangedDuringRunException : IOException("Datei wurde während des Laufs geändert")
