package com.sync2pc.app.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileBasicInformation
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.sync2pc.app.data.CredentialCrypto
import com.sync2pc.app.data.Profile
import com.sync2pc.app.sync.PartialHash
import java.io.Closeable
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit

/** Zerlegt "\\server\share\unter\ordner" (auch smb://server/share/...) */
data class SmbTarget(val host: String, val share: String, val basePath: String) {
    companion object {
        fun parse(input: String): SmbTarget? {
            var s = input.trim()
            if (s.lowercase().startsWith("smb:")) s = s.substring(4)
            val parts = s.replace('/', '\\').split('\\').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 2) return null
            return SmbTarget(parts[0], parts[1], parts.drop(2).joinToString("\\"))
        }
    }
}

data class RemoteEntry(val name: String, val size: Long, val isDirectory: Boolean, val modified: Long = 0L)

/** Eine offene Verbindung zu einem Windows-Share (SMB2/SMB3 via smbj). */
class SmbSession private constructor(
    private val client: SMBClient,
    private val connection: Connection,
    private val session: Session,
    private val share: DiskShare,
    val target: SmbTarget,
) : Closeable {

    companion object {
        const val TEMP_SUFFIX = ".s2ptmp"
        private const val BUFFER = 1 shl 20 // 1 MB

        /** Freigabe-Pfad + optionaler Unterordner des Profils */
        fun targetOf(profile: Profile): SmbTarget? {
            val t = SmbTarget.parse(profile.smbPath) ?: return null
            val sub = profile.remoteSubfolder.replace('/', '\\').split('\\').map { it.trim() }.filter { it.isNotEmpty() }
            return if (sub.isEmpty()) t
            else t.copy(basePath = (listOf(t.basePath) + sub).filter { it.isNotEmpty() }.joinToString("\\"))
        }

        fun open(profile: Profile): SmbSession {
            val target = targetOf(profile)
                ?: throw IllegalArgumentException("Ungültiger Share-Pfad: '${profile.smbPath}' (erwartet \\\\server\\freigabe)")
            val config = SmbConfig.builder()
                .withTimeout(60, TimeUnit.SECONDS)
                .withSoTimeout(90, TimeUnit.SECONDS)
                .withMultiProtocolNegotiate(true)
                .build()
            val client = SMBClient(config)
            var connection: Connection? = null
            try {
                connection = client.connect(target.host)
                val auth = AuthenticationContext(
                    profile.smbUser,
                    CredentialCrypto.decrypt(profile.smbPasswordEnc).toCharArray(),
                    profile.smbDomain.ifBlank { null }
                )
                val session = connection.authenticate(auth)
                val share = session.connectShare(target.share) as? DiskShare
                    ?: throw IllegalStateException("'${target.share}' ist keine Datei-Freigabe")
                return SmbSession(client, connection, session, share, target)
            } catch (e: Exception) {
                try { connection?.close() } catch (ignored: Exception) {}
                try { client.close() } catch (ignored: Exception) {}
                throw e
            }
        }

        /**
         * Schneller, akkuschonender Erreichbarkeitstest: nur TCP-Verbindung auf Port 445,
         * ohne Anmeldung. true = der PC antwortet.
         */
        fun isReachable(host: String, timeoutMs: Int = 3000): Boolean = try {
            java.net.Socket().use { it.connect(java.net.InetSocketAddress(host, 445), timeoutMs) }
            true
        } catch (e: Exception) {
            false
        }

        /** Verbindung testen: verbinden, Zielordner prüfen. Liefert eine lesbare Meldung. */
        fun test(profile: Profile): String = open(profile).use { s ->
            val base = s.target.basePath
            if (base.isNotEmpty() && !s.share.folderExists(base))
                "Verbunden, aber Ordner '$base' existiert nicht (wird beim ersten Sync angelegt)."
            else {
                val n = s.listDir(base).size
                "Verbindung OK – ${s.target.host}\\${s.target.share}" +
                    (if (base.isNotEmpty()) "\\$base" else "") + " ($n Einträge)"
            }
        }
    }

    /** Verbindung noch nutzbar? (Windows trennt unbenutzte Verbindungen nach ca. 15 Minuten) */
    fun isAlive(): Boolean = try { share.isConnected && connection.isConnected } catch (e: Exception) { false }

    /** Pfad relativ zur Share-Wurzel, ausgehend vom Basisordner des Profils */
    fun path(vararg parts: String): String =
        (listOf(target.basePath) + parts).filter { it.isNotEmpty() }.joinToString("\\")

    fun listDir(path: String): List<RemoteEntry> {
        if (path.isNotEmpty() && !share.folderExists(path)) return emptyList()
        val dirFlag = FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value
        return share.list(path)
            .filter { it.fileName != "." && it.fileName != ".." }
            .map {
                RemoteEntry(
                    it.fileName, it.endOfFile, (it.fileAttributes and dirFlag) != 0L,
                    it.lastWriteTime?.toEpochMillis() ?: 0L
                )
            }
    }

    /**
     * Alle Dateien unterhalb von [basePath] (rekursiv), Schlüssel = relativer Pfad mit "/".
     * [skipDir] erhält den relativen Ordnerpfad und kann ganze Zweige auslassen.
     */
    fun listRecursive(basePath: String, skipDir: (String) -> Boolean = { false }): Map<String, RemoteEntry> {
        val out = LinkedHashMap<String, RemoteEntry>()
        fun walk(rel: String) {
            val full = if (rel.isEmpty()) basePath else (if (basePath.isEmpty()) rel.replace('/', '\\') else basePath + "\\" + rel.replace('/', '\\'))
            for (e in listDir(full)) {
                val childRel = if (rel.isEmpty()) e.name else "$rel/${e.name}"
                if (e.isDirectory) { if (!skipDir(childRel)) walk(childRel) }
                else out[childRel] = e
            }
        }
        walk("")
        return out
    }

    /** Grösse und Änderungszeit einer Datei, null wenn nicht vorhanden */
    fun stat(path: String): RemoteEntry? {
        if (!share.fileExists(path)) return null
        val info = share.getFileInformation(path)
        return RemoteEntry(
            path.substringAfterLast('\\'), info.standardInformation.endOfFile, false,
            info.basicInformation.lastWriteTime?.toEpochMillis() ?: 0L
        )
    }

    /** Lädt eine Datei herunter und schreibt sie in [out]. onProgress(bytes) */
    fun download(path: String, out: java.io.OutputStream, onProgress: (Long) -> Unit, checkCancel: () -> Unit) {
        share.openFile(
            path, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, null
        ).use { f ->
            val buf = ByteArray(BUFFER)
            var offset = 0L
            while (true) {
                checkCancel()
                val n = f.read(buf, offset, 0, buf.size)
                if (n <= 0) break
                out.write(buf, 0, n)
                offset += n
                onProgress(offset)
            }
        }
    }

    /** Verschiebt/benennt eine Datei um (Zielordner muss existieren) */
    fun move(from: String, to: String) {
        share.openFile(
            from, EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        ).use { it.rename(to, false) }
    }

    /** Legt alle Ebenen an, falls nötig (verträgt parallele Aufrufe aus mehreren Verbindungen) */
    fun ensureDir(path: String) {
        if (path.isEmpty() || share.folderExists(path)) return
        val parts = path.split('\\')
        var cur = ""
        for (p in parts) {
            cur = if (cur.isEmpty()) p else "$cur\\$p"
            if (!share.folderExists(cur)) {
                try { share.mkdir(cur) } catch (e: SMBApiException) {
                    if (!share.folderExists(cur)) throw e
                }
            }
        }
    }

    fun fileSize(path: String): Long? =
        if (!share.fileExists(path)) null
        else share.getFileInformation(path).standardInformation.endOfFile

    fun delete(path: String) {
        if (share.fileExists(path)) share.rm(path)
    }

    /** Ordner samt Inhalt endgültig löschen */
    fun deleteDir(path: String) {
        if (path.isNotEmpty() && share.folderExists(path)) share.rmdir(path, true)
    }

    fun partialHash(path: String, size: Long): String =
        share.openFile(
            path, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, null
        ).use { f ->
            PartialHash.compute(size) { off, len ->
                val buf = ByteArray(len)
                var read = 0
                while (read < len) {
                    val n = f.read(buf, off + read, read, len - read)
                    if (n <= 0) break
                    read += n
                }
                if (read == len) buf else buf.copyOf(read)
            }
        }

    /**
     * Lädt eine Datei hoch: zuerst in "<name>.s2ptmp", danach Umbenennung.
     * Existiert bereits eine angefangene Temp-Datei, wird dort fortgesetzt.
     * @param openInput liefert einen Stream ab dem gegebenen Offset
     * @param onProgress Bytes dieser Datei bisher
     * @param checkCancel wirft eine Exception, wenn abgebrochen werden soll
     * @param modifiedMillis ursprüngliches Änderungsdatum, wird auf der Zieldatei gesetzt
     */
    fun upload(
        finalPath: String,
        size: Long,
        openInput: (offset: Long) -> InputStream,
        onProgress: (Long) -> Unit,
        checkCancel: () -> Unit,
        modifiedMillis: Long? = null,
        /** Vorhandene Zieldatei ersetzen (Ordner-Spiegelung) */
        replaceExisting: Boolean = false,
    ) {
        val tempPath = finalPath + TEMP_SUFFIX
        val existing = fileSize(tempPath) ?: 0L
        var offset = if (existing in 1 until size) existing else 0L
        val disposition = if (offset > 0) SMB2CreateDisposition.FILE_OPEN else SMB2CreateDisposition.FILE_OVERWRITE_IF
        share.openFile(
            tempPath,
            EnumSet.of(AccessMask.GENERIC_READ, AccessMask.GENERIC_WRITE, AccessMask.DELETE),
            EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
            SMB2ShareAccess.ALL,
            disposition,
            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
        ).use { f ->
            onProgress(offset)
            openInput(offset).use { input ->
                val buf = ByteArray(BUFFER)
                while (true) {
                    checkCancel()
                    // Puffer möglichst voll machen -> weniger SMB-Roundtrips
                    var filled = 0
                    while (filled < buf.size) {
                        val n = input.read(buf, filled, buf.size - filled)
                        if (n < 0) break
                        filled += n
                    }
                    if (filled == 0) break
                    f.write(buf, offset, 0, filled)
                    offset += filled
                    onProgress(offset)
                    if (filled < buf.size) break
                }
            }
            if (offset != size) throw java.io.IOException("Upload unvollständig: $offset von $size Bytes")
            if (modifiedMillis != null && modifiedMillis > 0) {
                // Erstell- und Änderungsdatum = Original. Nach explizitem Setzen ändert Windows es beim Schliessen nicht mehr.
                val t = FileTime.ofEpochMillis(modifiedMillis)
                try {
                    f.setFileInformation(
                        FileBasicInformation(t, FileBasicInformation.DONT_SET, t, FileBasicInformation.DONT_SET, 0L)
                    )
                } catch (ignored: Exception) {
                    // Zeitstempel ist "nice to have" – Upload deswegen nicht scheitern lassen
                }
            }
            f.rename(finalPath, replaceExisting)
        }
    }

    override fun close() {
        try { share.close() } catch (ignored: Exception) {}
        try { session.close() } catch (ignored: Exception) {}
        try { connection.close() } catch (ignored: Exception) {}
        try { client.close() } catch (ignored: Exception) {}
    }
}
