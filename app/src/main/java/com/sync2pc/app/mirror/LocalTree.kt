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

        /** Anzeige: "primary:Documents/Scans" -> "Documents/Scans" (SD-Karte: "SD-Karte: …") */
        fun label(uri: Uri): String = try {
            val id = DocumentsContract.getTreeDocumentId(uri)
            val vol = id.substringBefore(':')
            val path = id.substringAfter(':', "")
            (if (vol == "primary") "" else "SD-Karte: ") + path.ifEmpty { "(Hauptordner)" }
        } catch (e: Exception) { uri.toString() }

        fun mimeOf(name: String): String =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
                ?: "application/octet-stream"
    }

    private fun docUri(docId: String): Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

    private val projection = arrayOf(
        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
        Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED,
    )

    fun children(docId: String): List<LocalEntry> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val out = ArrayList<LocalEntry>()
        resolver.query(uri, projection, null, null, null)?.use { c ->
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

    /** Alle Dateien rekursiv, Schlüssel = relativer Pfad. [skipDir] lässt Zweige aus. */
    fun listRecursive(skipDir: (String) -> Boolean = { false }): Map<String, LocalEntry> {
        val out = LinkedHashMap<String, LocalEntry>()
        fun walk(rel: String, docId: String) {
            for (e in children(docId)) {
                val childRel = if (rel.isEmpty()) e.name else "$rel/${e.name}"
                if (e.isDir) {
                    dirIds[childRel.lowercase()] = e.docId
                    if (!skipDir(childRel)) walk(childRel, e.docId)
                } else out[childRel] = e
            }
        }
        walk("", rootDocId)
        return out
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
        // Alte Version ersetzen
        siblings.firstOrNull { !it.isDir && it.name.equals(name, ignoreCase = true) }?.let { delete(it.docId) }
        val finalUri = DocumentsContract.renameDocument(resolver, tmpUri, name)
            ?: throw IOException("Umbenennen von '$relPath' fehlgeschlagen")
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
        write: (OutputStream, Long) -> Unit,
    ): Pair<String, Long> {
        val parentRel = relPath.substringBeforeLast('/', "")
        val name = relPath.substringAfterLast('/')
        val parentId = ensureDir(parentRel)
        val siblings = children(parentId)
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
            out = try { resolver.openOutputStream(tmpUri!!, "wa") } catch (e: Exception) { null }
            if (out == null) { try { DocumentsContract.deleteDocument(resolver, tmpUri!!) } catch (ignored: Exception) {}; tmpUri = null; offset = 0 }
        }
        if (tmpUri == null) {
            tmpUri = DocumentsContract.createDocument(resolver, docUri(parentId), "application/octet-stream", name + TEMP)
                ?: throw IOException("Datei '$relPath' konnte nicht angelegt werden")
            out = resolver.openOutputStream(tmpUri!!, "w") ?: throw IOException("Datei nicht beschreibbar")
        }
        val uri = tmpUri!!
        out!!.use { write(it, offset) }
        // Grösse prüfen: Hat der Anbieter "anhängen" ignoriert, stimmt sie nicht -> verwerfen, nächstes Mal neu
        val written = stat(DocumentsContract.getDocumentId(uri))?.size ?: -1
        if (written != expectedSize) {
            try { DocumentsContract.deleteDocument(resolver, uri) } catch (ignored: Exception) {}
            throw IOException("Grösse nach Download stimmt nicht ($written statt $expectedSize Bytes)")
        }
        siblings.firstOrNull { !it.isDir && it.name.equals(name, ignoreCase = true) }?.let { delete(it.docId) }
        val finalUri = DocumentsContract.renameDocument(resolver, uri, name)
            ?: throw IOException("Umbenennen von '$relPath' fehlgeschlagen")
        return DocumentsContract.getDocumentId(finalUri) to offset
    }

    fun rename(docId: String, newName: String): String {
        val uri = DocumentsContract.renameDocument(resolver, docUri(docId), newName)
            ?: throw IOException("Umbenennen fehlgeschlagen")
        return DocumentsContract.getDocumentId(uri)
    }

    fun delete(docId: String) {
        DocumentsContract.deleteDocument(resolver, docUri(docId))
    }

    /** Verschiebt eine Datei in "<Ordner>/.LeoSync-Papierkorb/<Datum>/<relPath>" */
    fun moveToTrash(relPath: String, entry: LocalEntry, dateFolder: String) {
        val parentRel = relPath.substringBeforeLast('/', "")
        val sourceParent = ensureDir(parentRel)
        val trashDir = listOf(MirrorEngine.TRASH_DIR, dateFolder, parentRel).filter { it.isNotEmpty() }.joinToString("/")
        val targetParent = ensureDir(trashDir)
        // Gleicher Name schon im Papierkorb -> vorher umbenennen
        var name = entry.name
        val existing = children(targetParent).map { it.name.lowercase() }.toSet()
        var n = 2
        while (name.lowercase() in existing) {
            name = com.sync2pc.app.sync.FolderNames.conflictName(entry.name, n++)
        }
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
    }

    /**
     * Löscht im Papierkorb (neuer und alter Name) alle Datumsordner "yyyy-MM-dd", die vor [cutoff] liegen.
     * Liefert die Anzahl gelöschter Ordner.
     */
    fun purgeTrash(cutoff: String): Int {
        var n = 0
        val trashDirs = children(rootDocId).filter {
            it.isDir && (it.name.equals(MirrorEngine.TRASH_DIR, true) || it.name.equals(MirrorEngine.OLD_TRASH_DIR, true))
        }
        for (t in trashDirs) {
            for (d in children(t.docId)) {
                if (d.isDir && TrashPolicy.isExpired(d.name, cutoff)) { delete(d.docId); n++ }
            }
        }
        return n
    }

    private val TEMP = com.sync2pc.app.smb.SmbSession.TEMP_SUFFIX
}
