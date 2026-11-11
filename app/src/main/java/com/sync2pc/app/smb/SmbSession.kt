package com.sync2pc.app.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileBasicInformation
import com.hierynomus.mserref.NtStatus
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2Dialect
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
import com.sync2pc.app.data.SmbEncryption
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

/**
 * Gemeinsame Verbindungskonfiguration für [SmbSession.open], [SmbSession.checkPc] und [SmbSession.listShares]
 * (SYNC-20261111-144). Zeitlimits wie bisher: [timeoutSec] (Verbindung/Transaktion), [soTimeoutSec] nur wenn gesetzt,
 * [multiProtocol] nur für open. Verschlüsselung:
 * - OFF: wie bisher (smbj-Standard, nie verschlüsseln);
 * - PREFERRED: `withEncryptData(true)` (verschlüsselt, sobald der Server SMB3-Verschlüsselung kann, sonst still unverschlüsselt);
 * - REQUIRED: zusätzlich nur die Dialekte SMB 3.1.1, 3.0.2 und 3.0 (die Pflichtprüfung vor/nach der Anmeldung steht in [PcLogin]).
 * `withSigningRequired` bleibt unverändert (nicht Teil des Auftrags).
 */
internal fun smbConfigFor(
    encryption: SmbEncryption, timeoutSec: Long, soTimeoutSec: Long? = null, multiProtocol: Boolean = false,
): SmbConfig {
    val b = SmbConfig.builder().withTimeout(timeoutSec, TimeUnit.SECONDS)
    if (soTimeoutSec != null) b.withSoTimeout(soTimeoutSec, TimeUnit.SECONDS)
    if (multiProtocol) b.withMultiProtocolNegotiate(true)
    when (encryption) {
        SmbEncryption.OFF -> {}
        SmbEncryption.PREFERRED -> b.withEncryptData(true)
        SmbEncryption.REQUIRED -> {
            b.withEncryptData(true)
            b.withDialects(SMB2Dialect.SMB_3_1_1, SMB2Dialect.SMB_3_0_2, SMB2Dialect.SMB_3_0)
        }
    }
    return b.build()
}

/** Verschlüsselt die angemeldete Sitzung tatsächlich? (SMB 3.x, Ver- und Entschlüsselungsschlüssel, smbj verschlüsselt Pakete) */
internal fun smbSessionEncrypted(session: Session): Boolean = try {
    val ctx = session.sessionContext
    session.connection.connectionContext.negotiatedProtocol.dialect.isSmb3x &&
        ctx.encryptionKey != null && ctx.decryptionKey != null && session.shouldEncryptData()
} catch (e: Exception) { false }

/** Dialekt als Text ("3.1.1", "3.0.2", "3.0", "2.1", "2.0.2"), sonst der Aufzählungsname */
internal fun smbDialectText(d: SMB2Dialect?): String = when (d) {
    null -> "?"
    SMB2Dialect.SMB_3_1_1 -> "3.1.1"
    SMB2Dialect.SMB_3_0_2 -> "3.0.2"
    SMB2Dialect.SMB_3_0 -> "3.0"
    SMB2Dialect.SMB_2_1 -> "2.1"
    SMB2Dialect.SMB_2_0_2 -> "2.0.2"
    else -> d.name
}

/**
 * Ergebnis von [SmbSession.testDetailed]: bisheriger Meldetext plus Verbindungsdaten (strukturiert, kein UI-Text).
 * [dialect] z. B. "3.1.1" ("?" unbekannt), [smb3] Dialekt ist SMB 3.x, [encrypted] die angemeldete Sitzung ist verschlüsselt,
 * [cipher] Verfahren ("AES_128_GCM" / "AES_128_CCM") nur wenn [encrypted], [serverEncryption] der PC bietet Verschlüsselung an
 * (bei [setting] OFF und SMB 3.0/3.0.2 nicht erkennbar: dann false, da der Client keine Verschlüsselung anmeldet), [setting] die Einstellung des getesteten Profils.
 * [folderMissing] true: der Zielordner fehlt ([message] ist dann der Hinweis), sonst ist [message] die Erfolgsmeldung.
 */
data class SmbTestResult(
    val message: String,
    val folderMissing: Boolean,
    val dialect: String,
    val smb3: Boolean,
    val encrypted: Boolean,
    val cipher: String?,
    val serverEncryption: Boolean,
    val setting: SmbEncryption,
)

/** Nur Ordnerlisten des Share (für [com.sync2pc.app.sync.TargetResolver]; in Tests durch ein Fake ersetzbar). */
interface RemoteDirs {
    /** Pfad relativ zur Share-Wurzel, ausgehend vom Basisordner des Profils */
    fun path(vararg parts: String): String
    fun listDir(path: String): List<RemoteEntry>
}

/** Lesezugriff für die Suche nach vorhandenen Dateien (für `SpaceFreer`; in Tests durch ein Fake ersetzbar). */
interface RemoteFiles : RemoteDirs {
    fun stat(path: String): RemoteEntry?
    fun partialHash(path: String, size: Long): String
}

/**
 * Schreibender Zugriff auf die Freigabe mit genau den Operationen, die Papierkorb-Aufräumen, Wiederherstellen und
 * «Speicher freigeben» brauchen (SYNC-20261111-163 Stufe 2, Entscheid Architecture -183). [RemoteFiles] bleibt bewusst nur
 * lesend. [SmbSession] implementiert dies mit seinen vorhandenen Methoden; in Tests ersetzt es ein Fake.
 */
interface RemoteStore : RemoteFiles, Closeable {
    fun dirExists(path: String): Boolean
    fun listRecursive(basePath: String, skipDir: (String) -> Boolean = { false }): Map<String, RemoteEntry>
    fun ensureDir(path: String)
    fun move(from: String, to: String)
    fun delete(path: String)
    fun deleteDir(path: String)
    /** Ordner (samt Inhalt) umbenennen; das Ziel darf nicht existieren, der Zielelternordner muss es (Papierkorb-Übernahme) */
    fun renameDir(from: String, to: String)
    /** Ordner löschen, **nur wenn er leer ist** (nie rekursiv); wirft, wenn er Inhalt hat oder nicht gelöscht werden kann */
    fun deleteEmptyDir(path: String)
    fun contentHash(path: String, size: Long, check: () -> Unit = {}, onBytes: (Long) -> Unit = {}): String
}

/** Eine offene Verbindung zu einem Windows-Share (SMB2/SMB3 via smbj). */
class SmbSession private constructor(
    private val client: SMBClient,
    private val connection: Connection,
    private val session: Session,
    private val share: DiskShare,
    val target: SmbTarget,
) : RemoteStore {

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
            // smbj 0.13.0: Dialekt SMB 3.x und ConnectionContext.supportsEncryption() (3.1.1: Verfahren ausgehandelt;
            // 3.0/3.0.2: Client-Fähigkeit [nur mit withEncryptData(true)] und Server-Fähigkeit)
            override fun encryptionAvailable(): Boolean = try {
                val cc = connection.getConnectionContext()
                cc.negotiatedProtocol.dialect.isSmb3x && cc.supportsEncryption()
            } catch (e: Exception) { false }
            override fun authenticate(): Session = connection.authenticate(AuthenticationContext(
                profile.smbUser,
                CredentialCrypto.decrypt(profile.smbPasswordEnc).toCharArray(),
                profile.smbDomain.ifBlank { null },
            ))
            override fun sessionEncrypted(session: Session): Boolean = smbSessionEncrypted(session)
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
                    // «Erforderlich» schränkt die Dialekte auf SMB 3.x ein: lehnt der Server die Aushandlung ab
                    // (STATUS_NOT_SUPPORTED, nur SMB 2.x), ist das "Verschlüsselung nicht verfügbar", kein Verbindungsfehler
                    if (profile.encryption() == SmbEncryption.REQUIRED && isDialectRejected(e))
                        throw SmbEncryptionUnavailableException(host, cause = e)
                    throw e
                }
            }

        }

        /**
         * SYNC-20261111-232 (6): Verlangt die Freigabe Verschlüsselung (EncryptData), die Sitzung ist aber unverschlüsselt
         * (Stufe «Aus»), meldet der Server beim Verbinden mit der Freigabe «Zugriff verweigert». Das sieht aus wie ein
         * Rechteproblem. Deshalb wird dieser Fall bei Stufe «Aus» mit einem Hinweis auf die Verschlüsselung markiert
         * ([SmbShareAccessDeniedException]); der Server unterscheidet beide Ursachen nicht.
         */
        private fun connectShareHinted(session: Session, share: String, enc: SmbEncryption) = try {
            session.connectShare(share)
        } catch (e: SMBApiException) {
            if (enc == SmbEncryption.OFF && e.statusCode == NtStatus.STATUS_ACCESS_DENIED.value)
                throw SmbShareAccessDeniedException(e)
            throw e
        }

        /** Lehnt der Server die Dialekt-Aushandlung ab (STATUS_NOT_SUPPORTED, nur SMB 2.x)? Auch als Ursache in der Kette. */
        internal fun isDialectRejected(e: Throwable): Boolean =
            generateSequence(e) { t -> t.cause?.takeIf { it !== t } }.take(9)
                .any { it is SMBApiException && it.statusCode == NtStatus.STATUS_NOT_SUPPORTED.value }

        /** Nur normale Datei-Freigaben (Typ Disk, keine versteckten/administrativen «C$», «IPC$»), nach Name sortiert */
        internal fun filterShares(shares: List<Pair<String, Int>>): List<String> =
            shares.filter { (name, type) -> (type and 0x0FFFFFFF) == 0 && !name.endsWith("$") }
                .map { it.first }
                .sortedBy { it.lowercase() }

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
            val config = if (quick) smbConfigFor(profile.encryption(), 30, 30, multiProtocol = true)
                else smbConfigFor(profile.encryption(), 60, 90, multiProtocol = true)
            val c = connectChecked(profile, target.host, config, access, learn, learnNetOk, onChangedGuid, authenticate = true)
            try {
                val link = c.link as SmbLink
                val session = c.session!!
                val share = connectShareHinted(session, target.share, profile.encryption()) as? DiskShare
                    ?: throw IllegalStateException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_not_disk, target.share))
                PcLogin.commit(c, pcStore)
                EncryptionStatus.record(profile, smbSessionEncrypted(session))
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
            val config = smbConfigFor(profile.encryption(), 30)
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
            val config = smbConfigFor(profile.encryption(), 30)
            val c = connectChecked(profile, host, config, access, false, learnNetOk, onChangedGuid, authenticate = true)
            try {
                val transport = com.rapid7.client.dcerpc.transport.SMBTransportFactories.SRVSVC.getTransport(c.session!!)
                val shares = filterShares(
                    com.rapid7.client.dcerpc.mssrvs.ServerService(transport).shares1.map { it.netName to it.type }
                )
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
        fun isReachable(host: String, timeoutMs: Int = 3000, port: Int = 445): Boolean = try {
            java.net.Socket().use { it.connect(java.net.InetSocketAddress(host, port), timeoutMs) }
            true
        } catch (e: Exception) {
            false
        }

        /** Verbindung testen: verbinden, Zielordner prüfen. Liefert eine lesbare Meldung (Kurzform von [testDetailed]). */
        fun test(
            profile: Profile,
            learnNetOk: Boolean = true,
            onChangedGuid: ((host: String, oldGuid: String, newGuid: String) -> Boolean)? = null,
        ): String = testDetailed(profile, learnNetOk, onChangedGuid).message

        /**
         * Wie [test], liefert aber zusätzlich den ausgehandelten Dialekt und den Verschlüsselungsstatus der Sitzung
         * (SYNC-20261111-144, strukturierte Daten in [SmbTestResult]). Fehler (inkl. [SmbEncryptionUnavailableException]) werden
         * wie bei [test] geworfen.
         */
        fun testDetailed(
            profile: Profile,
            learnNetOk: Boolean = true,
            onChangedGuid: ((host: String, oldGuid: String, newGuid: String) -> Boolean)? = null,
        ): SmbTestResult = open(profile, PcAccess.SETUP, onChangedGuid, learn = true, learnNetOk = learnNetOk).use { s ->
            val base = s.target.basePath
            val missing = base.isNotEmpty() && !s.share.folderExists(base)
            val message = if (missing)
                com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_test_missing_folder, base)
            else {
                val n = s.listDir(base).size
                com.sync2pc.app.l10n.plural(com.sync2pc.app.R.plurals.smb_test_ok, n,
                    "${s.target.host}\\${s.target.share}" + (if (base.isNotEmpty()) "\\$base" else ""), n)
            }
            val cc = try { s.connection.getConnectionContext() } catch (e: Exception) { null }
            val dialect = try { cc?.negotiatedProtocol?.dialect } catch (e: Exception) { null }
            val encrypted = smbSessionEncrypted(s.session)
            SmbTestResult(
                message = message,
                folderMissing = missing,
                dialect = smbDialectText(dialect),
                smb3 = dialect?.isSmb3x == true,
                encrypted = encrypted,
                cipher = if (encrypted) try { cc?.cipherId?.name } catch (e: Exception) { null } else null,
                serverEncryption = try { dialect?.isSmb3x == true && cc?.supportsEncryption() == true } catch (e: Exception) { false },
                setting = profile.encryption(),
            )
        }
    }

    /** Verbindung noch nutzbar? (Windows trennt unbenutzte Verbindungen nach ca. 15 Minuten) */
    fun isAlive(): Boolean = try { share.isConnected && connection.isConnected } catch (e: Exception) { false }

    /** Pfad relativ zur Share-Wurzel, ausgehend vom Basisordner des Profils */
    override fun path(vararg parts: String): String =
        (listOf(target.basePath) + parts).filter { it.isNotEmpty() }.joinToString("\\")

    /** Existiert der Ordner? Die Wurzel der Freigabe ("") existiert immer */
    override fun dirExists(path: String): Boolean = path.isEmpty() || share.folderExists(path)

    override fun listDir(path: String): List<RemoteEntry> {
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
    override fun listRecursive(basePath: String, skipDir: (String) -> Boolean): Map<String, RemoteEntry> {
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
    override fun stat(path: String): RemoteEntry? {
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
        /** Erwartete Grösse laut Einlesen; -1 = unbekannt (kein Abschlusscheck). Siehe [DownloadEnd]. */
        expectedSize: Long = -1,
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
            // SYNC-20261111-232 (2): Ein vorzeitiges Ende (Lesen liefert «Dateiende», obwohl noch Bytes fehlen, z. B. nach einem
            // Verbindungsabbruch mit Wiederverbinden) darf nicht als fertiger Download gelten. Die Temp-Datei bleibt dann liegen
            // und der Download setzt beim nächsten Lauf fort.
            if (expectedSize >= 0 && offset != expectedSize) {
                val current = try { f.fileInformation.standardInformation.endOfFile } catch (e: Exception) { null }
                when (DownloadEnd.classify(offset, expectedSize, current, progressed = offset > startOffset)) {
                    DownloadEnd.Kind.COMPLETE -> {}
                    DownloadEnd.Kind.CHANGED -> throw DownloadSizeException(
                        com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.tree_err_size, offset, expectedSize))
                    DownloadEnd.Kind.INTERRUPTED -> throw java.io.EOFException(
                        com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_download_cut, offset, expectedSize))
                }
            }
        }
    }

    /** Verschiebt/benennt eine Datei um (Zielordner muss existieren) */
    override fun move(from: String, to: String) {
        share.openFile(
            from, EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        ).use { it.rename(to, false) }
    }

    /** Benennt einen Ordner um (Ziel darf nicht existieren; kein Überschreiben). [move] taugt nur für Dateien. */
    override fun renameDir(from: String, to: String) {
        share.openDirectory(
            from, EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, null
        ).use { it.rename(to, false) }
    }

    /** Löscht einen Ordner nur, wenn er leer ist (nicht rekursiv: der Server lehnt einen gefüllten Ordner ab) */
    override fun deleteEmptyDir(path: String) {
        if (path.isNotEmpty() && share.folderExists(path)) share.rmdir(path, false)
    }

    /** Legt alle Ebenen an, falls nötig (verträgt parallele Aufrufe aus mehreren Verbindungen) */
    override fun ensureDir(path: String) {
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

    override fun delete(path: String) {
        if (share.fileExists(path)) share.rm(path)
    }

    /** Ordner samt Inhalt endgültig löschen */
    override fun deleteDir(path: String) {
        if (path.isNotEmpty() && share.folderExists(path)) share.rmdir(path, true)
    }

    override fun partialHash(path: String, size: Long): String =
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

    /**
     * Vollständiger Inhalts-Hash für den Ordner-Vergleich (siehe [PartialHash.comparison]): ganze Datei, auch über
     * 32 MB (SYNC-20261111-142). [check] bricht per Exception ab, [onBytes] meldet den Fortschritt.
     */
    override fun contentHash(path: String, size: Long, check: () -> Unit, onBytes: (Long) -> Unit): String =
        share.openFile(
            path, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN, null
        ).use { f -> PartialHash.comparison(size, { off, len -> readFully(f, off, len) }, check, onBytes) }

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
     * @param sourceChanged Prüfung kurz vor dem Umbenennen: Hat sich die Quelle geändert, wird die Temp-Datei gelöscht und
     *   [com.sync2pc.app.mirror.SourceChangedException] geworfen (SYNC-20261111-202)
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
        /** true = Quelldatei hat sich seit dem Einlesen geändert (Ordner-Spiegelung, -202); null = keine Prüfung */
        sourceChanged: (() -> Boolean)? = null,
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
            // Quelle auf dem Handy nicht lesbar (z. B. Phantomzeile im MediaStore, ENOENT): angelegte Temp-Datei entfernen,
            // sie wäre eine 0-Byte-Rest-Datei auf dem PC (SYNC-20261111-203). Netzwerkfehler betreffen nur das Schreiben (unten).
            val source = try { openInput(offset) } catch (e: java.io.IOException) {
                if (resumedAt == 0L) try { f.deleteOnClose() } catch (ignored: Exception) {}
                throw e
            }
            source.use { input ->
                val buf = ByteArray(BUFFER)
                while (true) {
                    checkCancel()
                    // Puffer möglichst voll machen -> weniger SMB-Roundtrips
                    // Höchstens ein Byte über die erwartete Grösse lesen: wächst die Datei, genügt das zum Erkennen (-202)
                    var filled = 0
                    while (filled < buf.size) {
                        val room = com.sync2pc.app.mirror.UploadVerdict.room(size, offset + filled, buf.size - filled)
                        if (room <= 0) break
                        val n = input.read(buf, filled, room)
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
            // Quelle noch wie beim Einlesen? (-202) Geändert -> Temp-Datei verwerfen und zurückstellen, nie eine halbe oder
            // gemischte Version zur fertigen Datei machen. Die Prüfung gilt auch bei passender Bytezahl (Änderung gleicher Länge).
            when (com.sync2pc.app.mirror.UploadVerdict.of(size, offset, sourceChanged?.invoke() ?: false)) {
                com.sync2pc.app.mirror.UploadVerdict.DEFER -> {
                    try { f.deleteOnClose() } catch (ignored: Exception) {}
                    throw com.sync2pc.app.mirror.SourceChangedException()
                }
                com.sync2pc.app.mirror.UploadVerdict.INCOMPLETE ->
                    throw java.io.IOException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.smb_err_incomplete, offset, size))
                com.sync2pc.app.mirror.UploadVerdict.COMMIT -> {}
            }
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
