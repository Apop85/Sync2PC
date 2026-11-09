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
import com.sync2pc.app.sync.PcAccess
import com.sync2pc.app.sync.PcDecision
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

        /** Nur der Server ("\\192.168.1.20" reicht, 0.11.0), null wenn leer */
        fun hostOf(input: String): String? {
            var s = input.trim()
            if (s.lowercase().startsWith("smb:")) s = s.substring(4)
            return s.replace('/', '\\').split('\\').map { it.trim() }.firstOrNull { it.isNotEmpty() }
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

        /**
         * Zugriff auf den aktuellen Stand der PC-Kennung (SYNC-20261109-047). Wird in [com.sync2pc.app.App] gesetzt.
         * Ohne Speicher und mit gespeichertem Profil (id != 0) wird nie angemeldet (sicherer Ausfall).
         */
        @Volatile var pcStore: PcStore? = null

        /** smbj-Verbindung hinter der Schnittstelle von [PcLogin] (Aushandlung hier, Anmeldung erst in [authenticate]) */
        private class SmbLink(
            private val client: SMBClient, private val connection: Connection, private val profile: Profile,
        ) : PcLink<Session> {
            override fun serverGuid(): String? = connection.getConnectionContext().getServerGuid()?.toString()
            override fun authenticate(): Session = connection.authenticate(AuthenticationContext(
                profile.smbUser,
                CredentialCrypto.decrypt(profile.smbPasswordEnc).toCharArray(),
                profile.smbDomain.ifBlank { null },
            ))
            override fun close() {
                try { connection.close() } catch (ignored: Exception) {}
                try { client.close() } catch (ignored: Exception) {}
            }
            val rawClient: SMBClient get() = client
            val rawConnection: Connection get() = connection
        }

        private class SmbTransport(private val config: SmbConfig, private val profile: Profile) : PcTransport<Session> {
            override fun connect(host: String): PcLink<Session> {
                val client = SMBClient(config)
                return try { SmbLink(client, client.connect(host), profile) } catch (e: Exception) {
                    try { client.close() } catch (ignored: Exception) {}
                    throw e
                }
            }
        }

        private fun connectChecked(
            profile: Profile, host: String, config: SmbConfig, access: PcAccess,
            learn: Boolean, learnNetOk: Boolean,
            onChangedGuid: ((host: String, oldGuid: String, newGuid: String) -> Boolean)?,
            authenticate: Boolean, seen: PcSeen? = null,
        ): PcChecked<Session> = PcLogin.connect(
            profile, host, SmbTransport(config, profile), pcStore, access, learn, learnNetOk, onChangedGuid, authenticate, seen,
        )

        /**
         * Öffnet eine Verbindung. [access]: SETUP (Profil-Editor) darf bei GUID/BOTH eine unbekannte Kennung anmelden
         * ([learnNetOk] = Netzbedingung, [learn] = nach Erfolg lernen), USER/AUTO (Standard AUTO) melden bei unbekannter Kennung nie
         * an. Wirft [PcIdentityException], wenn der PC nicht sicher erkannt ist (dann wurden keine Zugangsdaten gesendet).
         * [quick]: kurze Zeitlimits (30 s) für das Lernen nach dem Speichern.
         */
        fun open(
            profile: Profile,
            access: PcAccess = PcAccess.AUTO,
            onChangedGuid: ((host: String, oldGuid: String, newGuid: String) -> Boolean)? = null,
            learn: Boolean = false,
            learnNetOk: Boolean = true,
            quick: Boolean = false,
        ): SmbSession {
            val target = targetOf(profile)
                ?: throw IllegalArgumentException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_path, profile.smbPath))
            val config = (if (quick) SmbConfig.builder().withTimeout(30, TimeUnit.SECONDS).withSoTimeout(30, TimeUnit.SECONDS)
                else SmbConfig.builder().withTimeout(60, TimeUnit.SECONDS).withSoTimeout(90, TimeUnit.SECONDS))
                .withMultiProtocolNegotiate(true)
                .build()
            val c = connectChecked(profile, target.host, config, access, learn, learnNetOk, onChangedGuid, authenticate = true)
            try {
                val link = c.link as SmbLink
                val session = c.session!!
                val share = session.connectShare(target.share) as? DiskShare
                    ?: throw IllegalStateException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_not_disk, target.share))
                PcLogin.commit(c, pcStore)
                return SmbSession(link.rawClient, link.rawConnection, session, share, target)
            } catch (e: Exception) {
                c.close()
                throw e
            }
        }

        /**
         * Vorab-Prüfung ohne Anmeldung (nur Aushandeln, es werden keine Zugangsdaten gesendet): Entscheidung für den
         * Host des Profils. Verbindungsfehler (PC aus, Netz) werden als Exception geworfen. Mit [seen] wird je Host und Lauf
         * nur einmal ausgehandelt (die Entscheidung gilt weiter je Profil mit dessen eigener Kennung).
         */
        fun checkPc(profile: Profile, access: PcAccess = PcAccess.AUTO, seen: PcSeen? = null): PcDecision {
            val host = SmbTarget.hostOf(profile.smbPath)
                ?: throw IllegalArgumentException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_path, profile.smbPath))
            val config = SmbConfig.builder().withTimeout(30, TimeUnit.SECONDS).build()
            return connectChecked(profile, host, config, access, false, true, null, authenticate = false, seen = seen).decision
        }

        /**
         * Freigaben des PCs auflisten (0.11.0, über den Windows-Dienst "srvsvc", Bibliothek dcerpc).
         * Nur normale Datei-Freigaben, keine versteckten/administrativen ("C$", "IPC$" …).
         * Gleiche Kennungsprüfung wie [open] (Standard AUTO; der Profil-Editor übergibt SETUP, lernt hier aber nie).
         */
        fun listShares(
            profile: Profile,
            access: PcAccess = PcAccess.AUTO,
            onChangedGuid: ((host: String, oldGuid: String, newGuid: String) -> Boolean)? = null,
            learnNetOk: Boolean = true,
        ): List<String> {
            val host = SmbTarget.hostOf(profile.smbPath)
                ?: throw IllegalArgumentException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_path, profile.smbPath))
            val config = SmbConfig.builder().withTimeout(30, TimeUnit.SECONDS).build()
            val c = connectChecked(profile, host, config, access, false, learnNetOk, onChangedGuid, authenticate = true)
            try {
                val transport = com.rapid7.client.dcerpc.transport.SMBTransportFactories.SRVSVC.getTransport(c.session!!)
                val shares = com.rapid7.client.dcerpc.mssrvs.ServerService(transport).shares1
                    .filter { (it.type and 0x0FFFFFFF) == 0 && !it.netName.endsWith("$") }
                    .map { it.netName }
                    .sortedBy { it.lowercase() }
                PcLogin.commit(c, pcStore)
                return shares
            } finally {
                c.close()
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
        fun test(
            profile: Profile,
            learnNetOk: Boolean = true,
            onChangedGuid: ((host: String, oldGuid: String, newGuid: String) -> Boolean)? = null,
        ): String = open(profile, PcAccess.SETUP, onChangedGuid, learn = true, learnNetOk = learnNetOk).use { s ->
            val base = s.target.basePath
            if (base.isNotEmpty() && !s.share.folderExists(base))
                com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_test_missing_folder, base)
            else {
                val n = s.listDir(base).size
                com.sync2pc.app.l10n.plural(com.sync2pc.app.R.plurals.smb_test_ok, n,
                    "${s.target.host}\\${s.target.share}" + (if (base.isNotEmpty()) "\\$base" else ""), n)
            }
        }
    }

    /** Verbindung noch nutzbar? (Windows trennt unbenutzte Verbindungen nach ca. 15 Minuten) */
    fun isAlive(): Boolean = try { share.isConnected && connection.isConnected } catch (e: Exception) { false }

    /** Pfad relativ zur Share-Wurzel, ausgehend vom Basisordner des Profils */
    fun path(vararg parts: String): String =
        (listOf(target.basePath) + parts).filter { it.isNotEmpty() }.joinToString("\\")

    /** Existiert der Ordner? Die Wurzel der Freigabe ("") existiert immer */
    fun dirExists(path: String): Boolean = path.isEmpty() || share.folderExists(path)

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

    /**
     * Lädt eine Datei herunter und schreibt sie in [out]. onProgress(bytes inkl. [startOffset]).
     * [startOffset] > 0: Fortsetzen – es wird erst ab dieser Stelle gelesen.
     */
    fun download(
        path: String, out: java.io.OutputStream, onProgress: (Long) -> Unit, checkCancel: () -> Unit,
        startOffset: Long = 0,
    ) {
        share.openFile(
            path, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, null
        ).use { f ->
            val buf = ByteArray(BUFFER)
            var offset = startOffset
            if (offset > 0) onProgress(offset)
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
        ).use { f -> PartialHash.compute(size) { off, len -> readFully(f, off, len) } }

    /** Prüfsumme nach Prüfstufe (0.9.0, siehe [PartialHash.forLevel]) */
    fun levelHash(path: String, size: Long, level: Int): String =
        share.openFile(
            path, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, null
        ).use { f -> PartialHash.forLevel(level, size) { off, len -> readFully(f, off, len) } }

    /** Gründlicher Inhalts-Hash für den Ordner-Vergleich (0.4.1, siehe [PartialHash.thorough]) */
    fun contentHash(path: String, size: Long): String =
        share.openFile(
            path, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, null
        ).use { f -> PartialHash.thorough(size) { off, len -> readFully(f, off, len) } }

    private fun readFully(f: com.hierynomus.smbj.share.File, off: Long, len: Int): ByteArray {
        val buf = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = f.read(buf, off + read, read, len - read)
            if (n <= 0) break
            read += n
        }
        return if (read == len) buf else buf.copyOf(read)
    }

    /**
     * Lädt eine Datei hoch: zuerst in "<name>.s2ptmp", danach Umbenennung.
     * Eine angefangene Temp-Datei wird nur fortgesetzt, wenn sie zur aktuellen Version passt (seit 0.4.1:
     * kleiner als die Datei und die Datei seit dem letzten Schreiben der Temp-Datei nicht geändert, siehe
     * [com.sync2pc.app.mirror.TransferPlan.resumeOffset]). Nach einem Fortsetzen wird die Temp-Datei vor dem
     * Umbenennen mit [expectedHash] (Teil-Hash) geprüft; passt er nicht, wird sie verworfen.
     * @param openInput liefert einen Stream ab dem gegebenen Offset
     * @param onProgress Bytes dieser Datei bisher
     * @param checkCancel wirft eine Exception, wenn abgebrochen werden soll
     * @param modifiedMillis ursprüngliches Änderungsdatum, wird auf der Zieldatei gesetzt und für das Fortsetzen geprüft
     * @return Stelle, ab der fortgesetzt wurde (0 = neu)
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
        /** Teil-Hash der Quelldatei (PartialHash.compute) – Pflichtprüfung nach dem Fortsetzen */
        expectedHash: (() -> String)? = null,
    ): Long {
        val tempPath = finalPath + TEMP_SUFFIX
        val tmp = stat(tempPath)
        var offset = if (tmp == null) 0L else com.sync2pc.app.mirror.TransferPlan.resumeOffset(
            tmp.size, tmp.modified, size, modifiedMillis ?: 0L
        )
        // Ohne Prüfmöglichkeit nie fortsetzen (sonst könnte eine Mischung aus zwei Versionen entstehen)
        if (expectedHash == null) offset = 0L
        val resumedAt = offset
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
            if (offset != size) throw java.io.IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_incomplete, offset, size))
            if (resumedAt > 0 && expectedHash != null) {
                val h = PartialHash.compute(size) { off, len -> readFully(f, off, len) }
                if (h != expectedHash()) {
                    // Angefangene Datei gehörte zu einer anderen Version -> verwerfen, nächster Versuch beginnt neu
                    try { f.deleteOnClose() } catch (ignored: Exception) {}
                    throw java.io.IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_resume))
                }
            }
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
        return resumedAt
    }

    override fun close() {
        try { share.close() } catch (ignored: Exception) {}
        try { session.close() } catch (ignored: Exception) {}
        try { connection.close() } catch (ignored: Exception) {}
        try { client.close() } catch (ignored: Exception) {}
    }
}
