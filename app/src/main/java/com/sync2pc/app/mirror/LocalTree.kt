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
class LocalTree(private val context: Context, val treeUri: Uri) : LocalFiles {
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
                vol == "primary" -> path.ifEmpty { com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.free_main_folder) }
                vol == "raw" || path.startsWith("/storage/") -> rawPathLabel(path)
                uri.authority == "com.android.providers.downloads.documents" -> "Download" + (if (path.isNotEmpty()) "/$path" else "")
                else -> com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_sd_card) + ": " + path.ifEmpty { com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.free_main_folder) }
            }
        } catch (e: Exception) { uri.toString() }

        /**
         * Wie [label], löst aber Ordner des Download-Anbieters mit Nummern-Kennung ("msf:12") über den Anzeigenamen auf
         * (SYNC-20261110-114 P3: sonst "Download/12" statt "Download/Testsync"). Fällt bei Fehlern auf [label] zurück.
         */
        fun label(context: Context, uri: Uri): String {
            try {
                if (uri.authority == "com.android.providers.downloads.documents") {
                    val id = DocumentsContract.getTreeDocumentId(uri)
                    if (!id.startsWith("raw:") && !id.startsWith("/storage/")) {
                        // -122 Punkt 3: ganzer Pfad (Download/Testsync/unter) über die Elternkette, sonst nur der Ordnername
                        downloadPathLabel(context, uri, id)?.let { return it }
                        // SYNC-20261111-176 R3: Kette nicht lieferbar -> vollständiger Pfad aus der MediaStore-Ablage (API 29+),
                        // sonst fehlen Zwischenordner («Download/unter» statt «Download/Testsync/unter»)
                        mediaStorePathLabel(context, id)?.let { return it }
                        val doc = DocumentsContract.buildDocumentUriUsingTree(uri, id)
                        context.contentResolver.query(
                            doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null,
                        )?.use { c ->
                            if (c.moveToFirst()) c.getString(0)?.takeIf { it.isNotBlank() }?.let { return "Download/$it" }
                        }
                    }
                }
            } catch (e: Exception) { /* Anzeige-Fallback unten */ }
            // SYNC-20261111-239 Punkt 6: nie eine reine Kennung zeigen («Download/690»); Pfad des Profils wird nicht gespeichert
            if (uri.authority == "com.android.providers.downloads.documents") {
                val id = try { DocumentsContract.getTreeDocumentId(uri) } catch (e: Exception) { "" }
                if (id.isNotEmpty() && !id.startsWith("raw:") && !id.startsWith("/storage/"))
                    return com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_unknown_folder)
            }
            return label(uri)
        }

        /**
         * Pfad eines Download-Ordners mit Nummern-Kennung («msf:12», «msd:12») aus der MediaStore-Ablage: RELATIVE_PATH
         * («Download/Testsync/») plus Anzeigename. null = nicht ermittelbar (vor Android 10, Eintrag fehlt).
         */
        internal fun mediaStorePathLabel(context: Context, docId: String): String? = try {
            val num = docId.substringAfter(':', "").toLongOrNull()
            if (num == null || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) null else {
                val base = if (docId.startsWith("msd:")) android.provider.MediaStore.Downloads.getContentUri("external")
                    else android.provider.MediaStore.Files.getContentUri("external")
                context.contentResolver.query(
                    android.content.ContentUris.withAppendedId(base, num),
                    arrayOf(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.provider.MediaStore.MediaColumns.DISPLAY_NAME),
                    null, null, null,
                )?.use { c ->
                    if (!c.moveToFirst()) null else {
                        val rel = c.getString(0)?.trim('/').orEmpty()
                        val name = c.getString(1)?.takeIf { it.isNotBlank() } ?: return@use null
                        if (rel.isEmpty()) name else "$rel/$name"
                    }
                }?.takeIf { it.startsWith("Download") }
            }
        } catch (e: Exception) { null }

        /**
         * Pfad eines Ordners des Download-Anbieters von der Wurzel aus (ab Android 8 über `findDocumentPath`). Das erste Element der
         * Kette ist die Wurzel «Downloads» und entfällt. null = nicht ermittelbar (Anbieter liefert keine Kette, Name fehlt).
         */
        internal fun downloadPathLabel(context: Context, treeUri: Uri, docId: String): String? = try {
            val docUri = DocumentsContract.buildDocumentUri(treeUri.authority, docId)
            val ids = DocumentsContract.findDocumentPath(context.contentResolver, docUri)?.path
            if (ids == null || ids.size < 2) null else {
                val names = ids.drop(1).map { id ->
                    context.contentResolver.query(
                        DocumentsContract.buildDocumentUri(treeUri.authority, id),
                        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null,
                    )?.use { c -> if (c.moveToFirst()) c.getString(0)?.takeIf { it.isNotBlank() } else null } ?: return null
                }
                "Download/" + names.joinToString("/")
            }
        } catch (e: Exception) { null }

        /**
         * Kanonische Form eines Handy-Ordners für die Überschneidungsprüfung (SYNC-20261111-152): erst rein über den URI-Text
         * ([com.sync2pc.app.data.ProfileOverlap.canonicalTree]), sonst für Ordner des Download-Anbieters mit Nummern-Kennung
         * (`msd:12`) über die Elternkette ([downloadPathLabel], Wurzel «Download» = `primary:download`). null = nicht abbildbar
         * (dann gilt der Textvergleich der URI, es entsteht keine falsche Warnung). Greift auf den ContentResolver zu: nicht im UI-Thread.
         */
        fun canonical(context: Context, uriText: String): String? {
            com.sync2pc.app.data.ProfileOverlap.canonicalTree(uriText)?.let { return it }
            return try {
                val uri = Uri.parse(uriText)
                if (uri.authority != "com.android.providers.downloads.documents") return null
                val id = DocumentsContract.getTreeDocumentId(uri)
                if (id.startsWith("raw:") || id.startsWith("/storage/")) return null
                downloadPathLabel(context, uri, id)?.let { "primary:" + it.lowercase() }
            } catch (e: Exception) { null }
        }

        /** "/storage/emulated/0/Download/Test" -> "Download/Test","/storage/1234-ABCD/DCIM" -> "SD-Karte: DCIM" */
        fun rawPathLabel(path: String): String {
            val internal = Regex("""^/storage/emulated/\d+/?""")
            if (internal.containsMatchIn(path)) return path.replace(internal, "").trimEnd('/').ifEmpty { com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.free_main_folder) }
            val sd = Regex("""^/storage/[^/]+/?""")
            return com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_sd_card) + ": " + path.replace(sd, "").trimEnd('/').ifEmpty { com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.free_main_folder) }
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
            ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_unreadable))
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
        val stamps = LinkedHashMap<String, Long>()
        fun walk(rel: String, docId: String) {
            val all = children(docId)
            val names = all.filter { !it.isDir }.map { it.name.lowercase() }.toSet()
            for (e in all) {
                val childRel = if (rel.isEmpty()) e.name else "$rel/${e.name}"
                if (e.isDir) {
                    dirIds[childRel.lowercase()] = e.docId
                    if (!skipDir(childRel)) { stamps[e.docId] = e.modified; walk(childRel, e.docId) }
                } else if (e.name.endsWith(OLD, ignoreCase = true)) {
                    val base = e.name.dropLast(OLD.length)
                    val baseRel = if (rel.isEmpty()) base else "$rel/$base"
                    when (OldFileRecovery.decide(base.lowercase() in names, recover)) {
                        OldFileRecovery.Action.DELETE_OLD -> try { delete(e.docId) } catch (ignored: Exception) {}
                        OldFileRecovery.Action.IGNORE -> {}
                        OldFileRecovery.Action.RENAME_BACK -> {
                            val id = rename(e.docId, base)
                            out[baseRel] = stat(id) ?: e.copy(docId = id, name = base)
                        }
                        OldFileRecovery.Action.LIST_AS_BASE -> out[baseRel] = e.copy(name = base)
                    }
                } else out[childRel] = e
            }
        }
        // Stempel der Wurzel VOR dem Einlesen, damit eine Änderung dazwischen nicht übersehen wird
        stamps[rootDocId] = try { stat(rootDocId)?.modified ?: 0L } catch (e: Exception) { 0L }
        walk("", rootDocId)
        listingStamps = stamps
        return out
    }

    /** Ordner-Stempel (ID -> letzte Änderung) beim letzten [listRecursive]; Grundlage von [FolderChange] (SYNC-20261111-213) */
    @Volatile var listingStamps: Map<String, Long> = emptyMap()
        private set

    /** Hat sich ein Ordner seit dem letzten Einlesen verändert? [ownWrites] siehe [FolderChange.changed]; Fehler = keine Aussage */
    fun changedSinceListing(ownWrites: Boolean): Boolean = try {
        FolderChange.changed(listingStamps, ownWrites) { id -> stat(id)?.takeIf { it.isDir }?.modified }
    } catch (e: Exception) { false }

    /** Datei unter einem relativen Pfad suchen (Gross-/Kleinschreibung egal), null = nicht vorhanden */
    override fun find(relPath: String): LocalEntry? {
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

    /**
     * Vollständiger Inhalts-Hash für den Ordner-Vergleich (siehe [com.sync2pc.app.sync.PartialHash.comparison]):
     * ganze Datei, auch über 32 MB. [check] bricht per Exception ab, [onBytes] meldet den Fortschritt.
     */
    fun contentHash(docId: String, size: Long, check: () -> Unit = {}, onBytes: (Long) -> Unit = {}): String =
        (openInput(docId) as ParcelFileDescriptor.AutoCloseInputStream).use { ins ->
            com.sync2pc.app.sync.PartialHash.comparison(size, { off, len ->
                val bb = java.nio.ByteBuffer.allocate(len)
                var pos = off
                while (bb.hasRemaining()) {
                    val n = ins.channel.read(bb, pos)
                    if (n < 0) break
                    pos += n
                }
                if (bb.position() == len) bb.array() else bb.array().copyOf(bb.position())
            }, check, onBytes)
        }

    override fun stat(docId: String): LocalEntry? =
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
                ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_mkdir, relDir))
            DocumentsContract.getDocumentId(uri)
        }
        dirIds[relDir.lowercase()] = id
        return id
    }

    fun openInput(docId: String, offset: Long = 0): InputStream {
        val pfd = resolver.openFileDescriptor(docUri(docId), "r") ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_read))
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
            ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_create, relPath))
        try {
            (resolver.openOutputStream(tmpUri, "w") ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_write))).use(write)
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
                ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_rename, relPath))
        } catch (e: Exception) {
            // Alte Version wiederherstellen; die Temp-Datei bleibt liegen (wird nie synchronisiert)
            backupId?.let { try { rename(it, name) } catch (ignored: Exception) {} }
            throw e
        }
        // Der Anbieter darf den Namen eigenmächtig ändern («name (1).ext»), wenn es ihn auf dem Datenträger schon gibt, er
        // aber nicht aufgelistet wurde (z. B. nicht im MediaStore indiziert). Dieses Duplikat würde beim nächsten Lauf
        // hochgeladen: eigene frische Kopie entfernen, alte Version zurück, Fehler melden (SYNC-20261111-203).
        val actual = try {
            resolver.query(finalUri, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (e: Exception) { null }
        if (actual != null && !actual.equals(name, ignoreCase = true)) {
            try { DocumentsContract.deleteDocument(resolver, finalUri) } catch (ignored: Exception) {}
            backupId?.let { try { rename(it, name) } catch (ignored: Exception) {} }
            throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_name_taken, relPath))
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
                ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_create, relPath))
            out = resolver.openOutputStream(tmpUri, "w") ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_write))
        }
        val uri = tmpUri
        try {
            out!!.use { write(it, offset) }
        } catch (e: com.sync2pc.app.smb.DownloadSizeException) {
            // Grösse stimmt nicht (Datei geändert, kein Fortschritt): Temp-Datei verwerfen, nächstes Mal neu (wie der Grössencheck unten)
            try { DocumentsContract.deleteDocument(resolver, uri) } catch (ignored: Exception) {}
            throw e
        }
        // Grösse prüfen: Hat der Anbieter "anhängen" ignoriert, stimmt sie nicht -> verwerfen, nächstes Mal neu
        val written = stat(DocumentsContract.getDocumentId(uri))?.size ?: -1
        if (written != expectedSize) {
            try { DocumentsContract.deleteDocument(resolver, uri) } catch (ignored: Exception) {}
            throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_size, written, expectedSize))
        }
        return replaceWith(uri, siblings, name, relPath) to offset
    }

    fun rename(docId: String, newName: String): String {
        val uri = DocumentsContract.renameDocument(resolver, docUri(docId), newName)
            ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_rename_short))
        return DocumentsContract.getDocumentId(uri)
    }

    fun delete(docId: String) {
        DocumentsContract.deleteDocument(resolver, docUri(docId))
    }

    /**
     * Wie [rename], liefert aber null statt einer Ausnahme, wenn der Anbieter nicht umbenennen kann (nur für die
     * [TrashMigration]: dort heisst "geht nicht" = nichts tun, Altname bleibt lesbar).
     */
    fun renameOrNull(docId: String, newName: String): String? = try {
        DocumentsContract.renameDocument(resolver, docUri(docId), newName)?.let { DocumentsContract.getDocumentId(it) }
    } catch (e: UnsupportedOperationException) { null }

    /**
     * Verschiebt ein Dokument von einem Ordner in einen anderen, **ohne Rückfall auf Kopieren und Löschen**; null, wenn der
     * Anbieter nicht verschieben kann (nur für die [TrashMigration]).
     */
    fun moveOrNull(docId: String, fromParentId: String, toParentId: String): String? = try {
        DocumentsContract.moveDocument(resolver, docUri(docId), docUri(fromParentId), docUri(toParentId))
            ?.let { DocumentsContract.getDocumentId(it) }
    } catch (e: UnsupportedOperationException) { null }

    /**
     * Verschiebt eine Datei in "<Ordner>/@LeoSync-Bin/<Datum>/<relPath>". Gibt es den Namen dort schon,
     * bekommt sie die Uhrzeit angehängt ("a (14-32-05).txt", 0.7.0). Liefert den Pfad im Papierkorb.
     */
    override fun moveToTrash(relPath: String, entry: LocalEntry, dateFolder: String): String {
        val parentRel = relPath.substringBeforeLast('/', "")
        val sourceParent = ensureDir(parentRel)
        // `.nomedia` vor der ersten Datei im Papierkorb (nie ein Fehler, höchstens kein Marker)
        ensureTrashNoMedia(create = true)
        val trashDir = listOf(MirrorEngine.TRASH_DIR, dateFolder, parentRel).filter { it.isNotEmpty() }.joinToString("/")
        val targetParent = ensureDir(trashDir)
        val existing = children(targetParent).map { it.name.lowercase() }.toSet()
        val name = TrashIndex.uniqueTrashName(entry.name, TrashIndex.timeNow()) { it.lowercase() in existing }
        var docId = entry.docId
        if (name != entry.name) docId = rename(docId, name)
        try {
            DocumentsContract.moveDocument(resolver, docUri(docId), docUri(sourceParent), docUri(targetParent))
                ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_move))
        } catch (e: Exception) {
            // Anbieter kann nicht verschieben -> kopieren + löschen
            val target = "$trashDir/$name"
            writeFile(target) { out -> openInput(docId).use { it.copyTo(out, 1 shl 16) } }
            delete(docId)
        }
        return "$trashDir/$name"
    }

    private val noMediaDone = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * Legt im Handy-Papierkorb `@LeoSync-Bin` die Marker-Datei `.nomedia` (0 Byte) an, damit die Galerie die Papierkorb-Bilder
     * nicht zeigt (SYNC-20261112-249). Höchstens einmal je [LocalTree] (= je Lauf). [create]: den Papierkorb-Ordner anlegen, falls
     * er fehlt (vor dem ersten Verschieben); sonst nur, wenn er existiert (Reparatur älterer Papierkörbe). Nicht bei Ordnern des
     * Download-Anbieters ([TrashNoMedia.applies]). **Wirft nie** (Anbieter lehnt ab, Cloud-/SD-Anbieter): kein Fehler des Laufs.
     */
    @Synchronized
    fun ensureTrashNoMedia(create: Boolean) {
        if (noMediaDone.get() || !TrashNoMedia.applies(rootDocId)) return
        try {
            val trashId = if (create) ensureDir(MirrorEngine.TRASH_DIR) else findDir(MirrorEngine.TRASH_DIR) ?: return
            if (children(trashId).any { TrashNoMedia.isMarker(it.name) }) { noMediaDone.set(true); return }
            val uri = DocumentsContract.createDocument(resolver, docUri(trashId), "application/octet-stream", MirrorEngine.NOMEDIA)
            if (uri != null) {
                // Hat der Anbieter den Namen geändert («.nomedia (1)», «.nomedia.bin»): wieder entfernen, kein Marker
                val actual = try { stat(DocumentsContract.getDocumentId(uri))?.name } catch (e: Exception) { null }
                if (actual != null && !TrashNoMedia.isMarker(actual)) {
                    try { DocumentsContract.deleteDocument(resolver, uri) } catch (ignored: Exception) {}
                } else scanTrashForGallery()
            }
            noMediaDone.set(true)
        } catch (e: Exception) {
            noMediaDone.set(true) // nur ein Versuch je Lauf
            android.util.Log.i("LocalTree", "Papierkorb: .nomedia nicht angelegt: ${e.javaClass.simpleName}")
        }
    }

    /**
     * Nach dem (nachträglichen) Anlegen der `.nomedia` den Papierkorb gezielt scannen lassen: Android 10 behielt bereits
     * indexierte Papierkorb-Bilder im MediaStore, bis das System neu scannte (SYNC-20261112-283). Nur für den internen
     * Speicher («primary:»); sonst gibt es keinen Dateipfad. Nie ein Fehler, höchstens bleiben die Bilder bis zum Systemscan sichtbar.
     */
    private fun scanTrashForGallery() {
        try {
            if (!rootDocId.startsWith("primary:")) return
            @Suppress("DEPRECATION")
            val base = android.os.Environment.getExternalStorageDirectory().path
            val sub = rootDocId.substringAfter(':').trim('/')
            val dir = listOf(base, sub, MirrorEngine.TRASH_DIR).filter { it.isNotEmpty() }.joinToString("/")
            android.media.MediaScannerConnection.scanFile(context, arrayOf("$dir/${MirrorEngine.NOMEDIA}", dir), null, null)
        } catch (e: Exception) {
            android.util.Log.i("LocalTree", "Papierkorb: Galerie-Scan nicht ausgelöst: ${e.javaClass.simpleName}")
        }
    }

    /** Löscht die Marker-Datei `.nomedia` direkt im Ordner [rel], falls vorhanden (nur für die [TrashMigration]) */
    fun deleteNoMedia(rel: String) {
        val id = findDir(rel) ?: return
        children(id).firstOrNull { !it.isDir && TrashNoMedia.isMarker(it.name) }?.let { delete(it.docId) }
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
            it.isDir && MirrorEngine.isTrashDir(it.name)
        }.forEach { walk(it.name, it.docId) }
        return out
    }

    /**
     * Verschiebt eine Datei von [fromRel] nach [toRel] (Wiederherstellen aus dem Papierkorb, 0.5.0) und benennt sie
     * dabei um. Kann der Anbieter nicht verschieben: kopieren + löschen. Liefert die Dokument-ID am neuen Ort.
     */
    override fun moveFile(fromRel: String, entry: LocalEntry, toRel: String): String {
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
     * Vorgemerktes "Papierkorb leeren" im Lauf (0.6.0): Datumsordner löschen, die [rule] trifft; ALL löscht die ganzen
     * Papierkorb-Ordner (samt `.nomedia`, die vor dem nächsten Verschieben neu angelegt wird). Das «Aufräumen» der Papierkorb-Ansicht
     * ([TrashCleaner]) löscht dagegen nur Datumsordner und Dateien über [deleteTrashDir]/[deleteTrashFile]: der Papierkorb-Ordner
     * bleibt mit seiner `.nomedia` bestehen. Liefert die Anzahl gelöschter Ordner.
     */
    fun purgeTrash(rule: PurgeRule, keep: Set<String> = emptySet()): Int {
        var n = 0
        val trashDirs = children(rootDocId).filter {
            it.isDir && MirrorEngine.isTrashDir(it.name)
        }
        for (t in trashDirs) {
            val keepHere = keep.filter { it.startsWith(t.name.lowercase() + "/") }
            if (rule.mode == PurgeRule.Mode.ALL && keepHere.isEmpty()) {
                delete(t.docId); n++
                // Ordner samt `.nomedia` weg: das nächste Verschieben im selben Lauf legt beides neu an (SYNC-20261112-249)
                if (t.name.equals(MirrorEngine.TRASH_DIR, ignoreCase = true)) noMediaDone.set(false)
            }
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

    /** Einen ganzen Datumsordner im Papierkorb ("<Papierkorb>/<Datum>") löschen (0.11.10). false = nicht vorhanden */
    fun deleteTrashDir(relDir: String): Boolean {
        if (relDir.isEmpty()) return false
        forgetDirs(relDir)
        val id = findDir(relDir) ?: return false
        delete(id)
        forgetDirs(relDir)
        return true
    }

    /** Eine einzelne Datei im Papierkorb endgültig löschen. false = nicht (mehr) vorhanden */
    fun deleteTrashFile(trashPath: String): Boolean {
        val e = find(trashPath) ?: return false
        delete(e.docId)
        return true
    }

    /**
     * Anzahl aller Einträge eines Ordners, auch solcher ohne Anzeigenamen (die [children] überspringt). Nur für die
     * Leer-Prüfung vor dem Löschen (0.11.9): Antwortet Android nicht, ist das ein Fehler und nicht "leer".
     */
    private fun rawChildCount(docId: String): Int {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val cursor = resolver.query(uri, arrayOf(Document.COLUMN_DOCUMENT_ID), null, null, null)
            ?: throw IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_unreadable))
        return cursor.use { it.count }
    }

    /**
     * Ordner löschen, aber nur wenn er tatsächlich leer ist (0.11.8). Unmittelbar vorher neu einlesen, da
     * "deleteDocument" auf Ordnern bei vielen Anbietern rekursiv löscht. Das Profil-Wurzel ("") wird nie gelöscht.
     * false = nicht vorhanden oder nicht leer.
     */
    fun deleteDirIfEmpty(relDir: String): Boolean {
        if (relDir.isEmpty()) return false
        forgetDirs(relDir) // keine veraltete Kennung verwenden
        val id = findDir(relDir) ?: return false
        if (rawChildCount(id) != 0) return false
        delete(id)
        forgetDirs(relDir)
        return true
    }

    /** Gemerkte Ordner-IDs unterhalb von [relDir] verwerfen (nach dem Löschen sonst veraltet) */
    internal fun forgetDirs(relDir: String) {
        val k = relDir.lowercase()
        dirIds.keys.removeIf { it == k || it.startsWith("$k/") }
    }

    private val TEMP = com.sync2pc.app.smb.SmbSession.TEMP_SUFFIX
    private val OLD = OLD_SUFFIX
}

/** Alte Version während des Ersetzens auf dem Handy (0.4.1), wird nie synchronisiert */
const val OLD_SUFFIX = ".s2pold"

/**
 * Wiederanlauf nach Abbruch beim Ersetzen (P5, SYNC-20261111-207): Was geschieht mit einer liegengebliebenen "<name>.s2pold"?
 * Existiert "<name>", ist die alte Version überholt (löschen); fehlt "<name>", ist sie die gültige Datei (zurückbenennen).
 * Ohne [recover] (Simulation) wird nichts verändert; eine verwaiste alte Version zählt dann unter ihrem Namen.
 */
object OldFileRecovery {
    enum class Action { DELETE_OLD, IGNORE, RENAME_BACK, LIST_AS_BASE }

    fun decide(baseExists: Boolean, recover: Boolean): Action = when {
        baseExists -> if (recover) Action.DELETE_OLD else Action.IGNORE
        else -> if (recover) Action.RENAME_BACK else Action.LIST_AS_BASE
    }
}

/** Zieldatei wurde seit dem Einlesen geändert bzw. ist neu entstanden -> nicht überschreiben (0.4.1) */
class ChangedDuringRunException : IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_changed))
