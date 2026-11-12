package com.sync2pc.app.mirror

import com.sync2pc.app.R
import com.sync2pc.app.l10n.str
import com.sync2pc.app.smb.RemoteStore
import java.io.IOException

/**
 * [TrashSide] für den Handy-Ordner (SAF, [LocalTree]) – SYNC-20261111-244. Umbenennen und Verschieben gehen über
 * [LocalTree.renameOrNull] bzw. [LocalTree.moveOrNull]: liefert der Anbieter null, gilt das als [TrashMigrationUnsupported].
 * Kopieren + Löschen als Rückfall gibt es hier nie (anders als beim Papierkorb-Verschieben im Lauf). Nicht auf der JVM testbar
 * (ContentResolver): nur am Gerät prüfbar.
 */
class PhoneTrashSide(private val tree: LocalTree) : TrashSide {
    private fun docIdOfDir(rel: String): String = tree.findDir(rel) ?: throw IOException(str(R.string.tree_err_unreadable))

    override fun list(rel: String): List<TrashNode> {
        val id = tree.findDir(rel) ?: return emptyList()
        return tree.children(id).map { TrashNode(it.name, it.isDir) }
    }

    override fun makeDir(rel: String) {
        tree.ensureDir(rel)
        // Neuer Papierkorb-Ordner (Zusammenführen): `.nomedia` bevor die erste Datei hineinkommt (nie ein Fehler)
        if (rel.equals(MirrorEngine.TRASH_DIR, ignoreCase = true)) tree.ensureTrashNoMedia(create = false)
    }

    override fun renameDir(rel: String, newName: String) {
        val id = docIdOfDir(rel)
        val newId = tree.renameOrNull(id, newName) ?: throw TrashMigrationUnsupported("rename")
        tree.forgetDirs(rel)
        // Der Anbieter darf den Namen eigenmächtig ändern ("Name (1)"): dann zurück und nichts übernehmen
        val actual = try { tree.stat(newId)?.name } catch (e: Exception) { null }
        if (actual != null && !actual.equals(newName, ignoreCase = true)) {
            try { tree.renameOrNull(newId, rel.substringAfterLast('/')) } catch (ignored: Exception) {}
            tree.forgetDirs(rel)
            throw IOException(str(R.string.tree_err_name_taken, newName))
        }
    }

    override fun moveFile(fromRel: String, toRel: String) {
        val fromParentRel = fromRel.substringBeforeLast('/', "")
        val fromParent = docIdOfDir(fromParentRel)
        val toParent = docIdOfDir(toRel.substringBeforeLast('/', ""))
        val fromName = fromRel.substringAfterLast('/')
        val toName = toRel.substringAfterLast('/')
        val entry = tree.children(fromParent).firstOrNull { !it.isDir && it.name == fromName }
            ?: throw IOException(str(R.string.tree_err_unreadable))
        var docId = entry.docId
        // Anderer Zielname (Uhrzeit-Anhang): erst im Quellordner umbenennen, sonst stiesse das Verschieben auf die gleichnamige Datei.
        // Bricht es danach ab, liegt die Datei nur unter dem neuen Namen im alten Papierkorb: der nächste Lauf verschiebt sie
        // einfach weiter (der Name bleibt beim Wiederherstellen erkennbar, siehe TrashIndex.originalName).
        if (toName != fromName) docId = tree.renameOrNull(docId, toName) ?: throw TrashMigrationUnsupported("rename")
        tree.moveOrNull(docId, fromParent, toParent) ?: throw TrashMigrationUnsupported("move")
    }

    override fun deleteDirIfEmpty(rel: String): Boolean = tree.deleteDirIfEmpty(rel)

    override fun deleteNoMedia(rel: String) = tree.deleteNoMedia(rel)
}

/** [TrashSide] für die Freigabe auf dem PC ([RemoteStore], SMB) – SYNC-20261111-244 */
class PcTrashSide(private val s: RemoteStore) : TrashSide {
    private fun full(rel: String): String = s.path(rel.replace('/', '\\'))

    override fun list(rel: String): List<TrashNode> {
        val p = full(rel)
        if (!s.dirExists(p)) return emptyList()
        return s.listDir(p).map { TrashNode(it.name, it.isDirectory) }
    }

    override fun makeDir(rel: String) = s.ensureDir(full(rel))

    override fun renameDir(rel: String, newName: String) {
        val parent = rel.substringBeforeLast('/', "")
        s.renameDir(full(rel), full(if (parent.isEmpty()) newName else "$parent/$newName"))
    }

    override fun moveFile(fromRel: String, toRel: String) = s.move(full(fromRel), full(toRel))

    override fun deleteDirIfEmpty(rel: String): Boolean {
        val p = full(rel)
        if (!s.dirExists(p)) return true
        if (s.listDir(p).isNotEmpty()) return false
        try {
            s.deleteEmptyDir(p)
        } catch (e: Exception) {
            // Inzwischen nicht mehr leer (der Server lehnt das nicht-rekursive Löschen ab) -> bleibt, kein Fehler
            if (s.dirExists(p) && s.listDir(p).isNotEmpty()) return false
            throw e
        }
        return true
    }

    override fun deleteNoMedia(rel: String) {
        val file = full(if (rel.isEmpty()) MirrorEngine.NOMEDIA else "$rel/${MirrorEngine.NOMEDIA}")
        if (s.listDir(full(rel)).any { !it.isDirectory && TrashNoMedia.isMarker(it.name) }) s.delete(file)
    }
}
