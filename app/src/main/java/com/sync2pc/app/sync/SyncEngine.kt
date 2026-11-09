package com.sync2pc.app.sync

import com.sync2pc.app.data.RunStatus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.sync2pc.app.R
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.data.DeleteMode
import com.sync2pc.app.data.DeleteState
import com.sync2pc.app.data.GlobalSettings
import com.sync2pc.app.data.LogEntry
import com.sync2pc.app.data.MediaInfo
import com.sync2pc.app.data.PendingDecision
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.SyncRun
import com.sync2pc.app.data.SyncedFile
import com.sync2pc.app.data.VerifyLevel
import com.sync2pc.app.geo.CountryLookup
import com.sync2pc.app.media.MediaAnalyzer
import com.sync2pc.app.media.MediaItem
import com.sync2pc.app.media.MediaScanner
import com.sync2pc.app.smb.RemoteEntry
import com.sync2pc.app.smb.SmbSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Fortschritt eines Laufs (für Benachrichtigung und UI). */
data class SyncProgress(
    val profileName: String,
    val phase: String,
    val doneFiles: Int,
    val totalFiles: Int,
    val doneBytes: Long,
    val totalBytes: Long,
    val currentFile: String?,
)

/** Ein Eintrag der Vorschau (Trockenlauf). */
data class PreviewEntry(val targetFolder: String, val fileName: String, val needsDecision: Boolean)

/**
 * Führt einen Sync-Lauf für EIN Profil aus.
 *
 * Ablauf: Scan → bekannte Dateien per DB aussortieren → Analyse + Zielordner (sequenziell, "Produzent")
 *         → Übertragung durch mehrere parallele Worker mit je eigener SMB-Verbindung.
 * dryRun = true: nichts wird übertragen/angelegt, nur Zielordner ermittelt (Vorschau).
 */
class SyncEngine(
    private val context: Context,
    private val profile: Profile,
    private val trigger: Msg,
    private val dryRun: Boolean = false,
    /** false zurückgeben, um den Lauf sauber zu beenden (z. B. WLAN getrennt) */
    private val shouldContinue: () -> Boolean = { true },
    private val onProgress: (SyncProgress) -> Unit = {},
    /** Bei einem Stopp: Grund, wenn Android unterbrochen hat (z. B. "WLAN getrennt"); null = vom Benutzer abgebrochen */
    private val interruptReason: () -> Msg? = { null },
    /** Simulation (dryRun): nur die ersten so vielen neuen Dateien analysieren (EXIF/GPS lesen kostet Zeit) */
    private val previewLimit: Int = Int.MAX_VALUE,
) : SyncRunner {
    private val db = AppDatabase.get(context)
    private val dao = db.dao()
    private val settings = GlobalSettings(context)
    private val canReadLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_MEDIA_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    override val preview = ArrayList<PreviewEntry>()
    /** true, wenn der Lauf abgebrochen wurde, weil das Ziel-WLAN nicht mehr verbunden war */
    override var wifiLost: Boolean = false
        private set

    /** true, wenn der Lauf wegen fehlender Netzwerkverbindung zum Share scheiterte */
    override var connectionFailed: Boolean = false
        private set

    /** Frühester Zeitpunkt (ms), zu dem zu junge Dateien übertragen werden dürfen; 0 = keine */
    override var nextYoungFileAt: Long = 0
        private set

    private var run = SyncRun(
        profileId = profile.id, profileName = profile.name, trigger = trigger.text(), triggerCode = trigger.toCode(),
        dryRun = dryRun, startedAt = System.currentTimeMillis()
    )

    /** Log-Zeile als Kennung (DEC-159): Text in der Schreibsprache als Rückfall, Kennung für die Anzeige */
    private suspend fun log(level: String, msg: Msg) {
        if (dryRun) return
        dao.insertLog(LogEntry(runId = run.id, time = System.currentTimeMillis(), level = level,
            message = msg.text(), code = msg.toCode()))
    }

    /** Wörtliche Zeile (Symbole, Dateinamen, Fehlertexte): bleibt wie sie ist, ohne Kennung */
    private suspend fun log(level: String, text: String) = log(level, Msg.lit(text))

    private class DecisionAcc(
        val year: Int, val month: Int, val countryIso: String?, val countryName: String?,
        val defaultName: String, val candidates: List<String>,
    ) {
        var count = 0
        var bytes = 0L
        val samples = ArrayList<String>()
    }

    /** Ein Übertragungsauftrag: Datei → Jahr\Monatsordner */
    private class Job(
        val item: MediaItem, val year: Int, val folderName: String,
        /** Andere Ordner desselben Monats (z. B. "10" neben "10 - Ägypten") – für die Duplikatsuche */
        val siblingFolders: List<String> = emptyList(),
    )

    /** Zwischen den parallelen Workern geteilter Zustand */
    private class Shared(val totalFiles: Int, val totalBytes: Long) {
        val uploaded = AtomicInteger()
        val skipped = AtomicInteger()
        val failed = AtomicInteger()
        val done = AtomicInteger()
        /** Bytes vollständig verarbeiteter Dateien */
        val bytesDone = AtomicLong()
        /** Bytes der gerade laufenden Uploads (MediaStore-ID -> Bytes) */
        val inflight = ConcurrentHashMap<Long, Long>()
        val stop = AtomicBoolean(false)
        /** Schützt Ordnerlisten und reservierte Namen (Namensvergabe bild_2, bild_3 …) */
        val nameLock = Mutex()
        val listings = HashMap<String, HashMap<String, RemoteEntry>>()
        val reserved = HashSet<String>()
        @Volatile var lastEmit = 0L
        /** Bereits vorhandene Dateien je Zielordner, noch nicht ins Log geschrieben */
        val already = ConcurrentHashMap<String, AtomicInteger>()
        val alreadyPending = AtomicInteger()
    }

    /** Schreibt die gesammelten "bereits vorhanden"-Zähler als eine Zeile pro Ordner ins Log */
    private suspend fun flushAlready(sh: Shared?) {
        sh ?: return
        sh.alreadyPending.set(0)
        for ((folder, counter) in sh.already.entries.sortedBy { it.key }) {
            val n = counter.getAndSet(0)
            if (n > 0) log("I", Msg.plural(R.plurals.eng_already_in, n, n, folder))
        }
    }

    override suspend fun run(): SyncRun {
        run = run.copy(id = dao.insertRun(run))
        var smb: SmbSession? = null
        var skippedKnown = 0
        var waiting = 0
        var shared: Shared? = null
        var status = RunStatus.OK
        var message: Msg? = null
        fun up() = shared?.uploaded?.get() ?: 0
        fun sk() = skippedKnown + (shared?.skipped?.get() ?: 0)
        fun fl() = shared?.failed?.get() ?: 0
        fun by() = shared?.bytesDone?.get() ?: 0L
        try {
            if (!dryRun) {
                val day = 24L * 3600 * 1000
                val now = System.currentTimeMillis()
                dao.pruneLogs(now - 30 * day)   // Detail-Logs 30 Tage
                dao.pruneRuns(now - 90 * day)   // Verlaufseinträge 90 Tage
            }
            log("I", Msg.of(R.string.eng_start_photos, trigger, profile.name, profile.smbPath))

            // --- 1. Quellen scannen und bekannte Dateien ohne I/O aussortieren --------
            emit(str(R.string.eng_searching), 0, 0, 0, 0, null)
            val items = MediaScanner.scan(
                context, profile.folders(), profile.includeSubfolders,
                profile.includeImages, profile.includeVideos
            ).toMutableList()
            // White-/Blacklist (Muster auf Dateiname bzw. relativen Pfad)
            val filter = com.sync2pc.app.mirror.FileFilter(profile.includePatterns, profile.excludePatterns)
            val filtered = items.size
            items.retainAll { filter.accepts(it.relativePath + it.displayName) }
            if (items.size < filtered) log("I", Msg.plural(R.plurals.eng_filtered, filtered - items.size))
            val known = dao.syncedLite(profile.id).associateBy { it.sourceKey }
            val nowSec = System.currentTimeMillis() / 1000
            val minAge = settings.minFileAgeMinutes * 60L
            val todo = ArrayList<MediaItem>()
            var young = 0
            for (m in items) {
                val k = known[m.sourceKey]
                if (k != null && k.size == m.size && k.dateModified == m.dateModifiedSec) { skippedKnown++; continue }
                val age = nowSec - maxOf(m.dateAddedSec, m.dateModifiedSec)
                if (!dryRun && age < minAge) {
                    young++
                    val readyAt = (maxOf(m.dateAddedSec, m.dateModifiedSec) + minAge) * 1000
                    if (nextYoungFileAt == 0L || readyAt < nextYoungFileAt) nextYoungFileAt = readyAt
                    continue
                }
                todo += m
            }
            log("I", Msg.join("", listOfNotNull(
                Msg.plural(R.plurals.eng_found, items.size, items.size, todo.size, skippedKnown),
                if (young > 0) Msg.join("", listOf(Msg.lit(", "), Msg.of(R.string.eng_too_young, young, settings.minFileAgeMinutes))) else null)))
            if (todo.isEmpty()) {
                return finish(RunStatus.OK, RunSummary.NOTHING, 0, skippedKnown, 0, 0, 0)
            }

            // Live-Fotos/Bewegtbilder: Video mit gleichem Basisnamen wie ein Bild -> gleicher Ordner wie Bild
            val imageByBase = items.filter { !it.isVideo }
                .associateBy { (it.relativePath + it.baseName).lowercase() }

            // --- 2. Verbinden -------------------------------------------------------
            val totalBytes = todo.sumOf { it.size }
            emit(str(R.string.eng_connecting), 0, todo.size, 0, totalBytes, null)
            smb = try {
                SmbSession.open(profile)
            } catch (e: Exception) {
                if (dryRun) { message = Msg.of(R.string.eng_offline_preview, e.message ?: ""); null }
                else throw e
            }
            val countries = CountryLookup.get(context)
            if (!countries.isAvailable) log("W", Msg.of(R.string.eng_no_geo))
            val offline = smb == null
            // Die Nachschlage-Verbindung bleibt während langer Uploads ungenutzt; Windows trennt sie nach
            // ca. 15 Minuten. Deshalb vor jeder Nutzung prüfen und bei Bedarf neu aufbauen.
            val producerSmb: () -> SmbSession? = {
                if (offline) null else {
                    val cur = smb
                    if (cur != null && cur.isAlive()) cur
                    else {
                        try { cur?.close() } catch (ignored: Exception) {}
                        SmbSession.open(profile).also { smb = it }
                    }
                }
            }
            val resolver = TargetResolver(producerSmb, dao, profile.id, countries.allCountryNames, dryRun, offline,
                namesOfCountry = { countries.namesOf(it) })
            val analyzer = MediaAnalyzer(context, canReadLocation)
            if (!canReadLocation) log("W", Msg.of(R.string.eng_no_media_location))
            val home = settings.homeCountryIso
            val decisions = LinkedHashMap<String, DecisionAcc>()
            if (!dryRun) dao.deleteDecisionsOfProfile(profile.id)

            /** Analyse + Zielordner. Liefert einen Auftrag oder null (Entscheidung nötig / Vorschau). */
            suspend fun plan(item: MediaItem): Job? {
                val partner = if (item.isVideo) imageByBase[(item.relativePath + item.baseName).lowercase()] else null
                val info = mediaInfo(partner ?: item, analyzer, countries)
                val isHome = info.countryIso != null && info.countryIso.equals(home, ignoreCase = true)
                // Ordnername in der App-Sprache aus dem Länderkürzel (0.8.0); der Cache-Name nur als Rückfall
                val cName = if (isHome) null else countries.nameFor(info.countryIso, info.countryName)
                val cKey = if (isHome || cName == null) null else (info.countryIso?.ifBlank { null } ?: cName)
                return when (val res = resolver.resolve(info.year, info.month, cKey, cName)) {
                    is TargetResolver.Resolution.Ask -> {
                        val acc = decisions.getOrPut(res.groupKey) {
                            DecisionAcc(info.year, info.month, cKey, cName, res.defaultName, res.candidates)
                        }
                        acc.count++; acc.bytes += item.size
                        if (acc.samples.size < 8) acc.samples += item.uri.toString()
                        waiting++
                        if (dryRun) preview += PreviewEntry("${info.year}\\? (${res.defaultName})", item.displayName, true)
                        null
                    }
                    is TargetResolver.Resolution.Folder -> {
                        // Ordner gilt ab jetzt als vorhanden (wird vom Worker angelegt)
                        resolver.folderCreated(info.year, res.name)
                        if (dryRun || offline) {
                            preview += PreviewEntry("${info.year}\\${res.name}", item.displayName, false)
                            null
                        } else Job(
                            item, info.year, res.name,
                            resolver.monthFolders(info.year, info.month).filter { !it.equals(res.name, ignoreCase = true) },
                        )
                    }
                }
            }

            // --- 3. Dateien verarbeiten ---------------------------------------------
            if (dryRun || offline) {
                val simulated = todo.take(previewLimit)
                for ((index, item) in simulated.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    try { plan(item) } catch (e: CancellationException) { throw e } catch (ignored: Exception) {}
                    if (index % 25 == 0) emit(str(R.string.eng_analysing), index, simulated.size, 0, totalBytes, null)
                }
                if (message == null) message = Msg.plural(
                    if (simulated.size < todo.size) R.plurals.eng_new_files_first else R.plurals.eng_new_files_all,
                    todo.size, todo.size, simulated.size)
            } else {
                val sh = Shared(todo.size, totalBytes).also { shared = it }
                val parallel = settings.parallelUploads
                log("I", Msg.of(R.string.eng_transfer_photos, parallel))
                coroutineScope {
                    val channel = Channel<Job>(capacity = 32)
                    val workers = List(parallel) { launch { worker(channel, sh) } }
                    try {
                        for ((index, item) in todo.withIndex()) {
                            currentCoroutineContext().ensureActive()
                            if (index % 20 == 0 && !shouldContinue()) {
                                sh.stop.set(true)
                                wifiLost = true
                                status = RunStatus.PARTIAL; message = Msg.of(R.string.eng_wifi_lost)
                                log("W", Msg.of(R.string.eng_wifi_lost)); break
                            }
                            val job = try {
                                plan(item)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                // Verbindungsfehler: Lauf unterbrechen (kein Datei-Fehler). Die Verbindung wird
                                // bei jeder Nutzung ohnehin geprüft und neu aufgebaut – scheitert das, ist das Netz weg.
                                if (isConnectionError(e)) throw e
                                sh.failed.incrementAndGet()
                                log("E", Msg.of(R.string.eng_analysis_failed, item.displayName, e.message ?: ""))
                                null
                            }
                            if (job != null) channel.send(job) else countDone(sh, item.size)
                        }
                    } finally {
                        channel.close()
                    }
                    workers.joinAll()
                }
                flushAlready(sh)
            }

            // --- 4. Offene Ordner-Entscheidungen speichern ---------------------------
            if (!dryRun) {
                for ((key, d) in decisions) {
                    dao.upsertDecision(
                        PendingDecision(
                            profileId = profile.id, groupKey = key, year = d.year, month = d.month,
                            countryIso = d.countryIso, countryName = d.countryName,
                            defaultName = d.defaultName, candidates = d.candidates.joinToString("\n"),
                            fileCount = d.count, totalBytes = d.bytes,
                            sampleUris = d.samples.joinToString("\n"), updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                if (decisions.isNotEmpty()) log("W", Msg.plural(R.plurals.eng_waiting_groups, waiting, waiting, Msg.plural(R.plurals.home_groups, decisions.size)))
                DeletionManager.sweep(context) { log("I", it) }
            }

            if (status == RunStatus.OK && fl() > 0) status = RunStatus.PARTIAL
            val summary = message ?: RunSummary.photos(up(), shared?.skipped?.get() ?: 0, fl(), waiting)
            return finish(status, summary, up(), sk(), fl(), waiting, by())
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                flushAlready(shared)
                val reason = interruptReason()
                if (reason != null) {
                    log("W", Msg.of(R.string.eng_interrupted_log, reason))
                    finish(RunStatus.INTERRUPTED, Msg.of(R.string.eng_interrupted_photos, reason, up()),
                        up(), sk(), fl(), waiting, by())
                } else {
                    log("W", Msg.of(R.string.eng_paused_log))
                    finish(RunStatus.CANCELLED, Msg.of(R.string.eng_paused_photos, up()),
                        up(), sk(), fl(), waiting, by())
                }
            }
            throw e
        } catch (e: Exception) {
            flushAlready(shared)
            if (isConnectionError(e)) {
                // Erwartbar (WLAN weg, PC im Ruhezustand …): kein Fehler, offene Dateien folgen beim nächsten Sync.
                // Abgebrochene Uploads werden dort an der Abbruchstelle fortgesetzt (.s2ptmp).
                connectionFailed = true
                log("W", Msg.of(R.string.eng_connection_log_photos, shortError(e)))
                return finish(
                    RunStatus.INTERRUPTED, Msg.of(R.string.eng_connection_photos, up()),
                    up(), sk(), fl(), waiting, by()
                )
            }
            log("E", Msg.of(R.string.eng_run_failed, shortError(e)))
            return finish(RunStatus.FAILED, userErrorMsg(e), up(), sk(), fl(), waiting, by())
        } finally {
            smb?.close()
        }
    }

    // ---- Paralleler Worker -----------------------------------------------------------
    private suspend fun worker(channel: ReceiveChannel<Job>, sh: Shared) {
        var session: SmbSession? = null
        try {
            for (job in channel) {
                if (sh.stop.get()) continue // Rest der Warteschlange verwerfen
                currentCoroutineContext().ensureActive()
                val item = job.item
                try {
                    var attempt = 0
                    while (true) {
                        try {
                            // Tote Verbindung (Leerlauf-Trennung, kurzer WLAN-Aussetzer) -> neu aufbauen
                            val s = session?.takeIf { it.isAlive() }
                                ?: SmbSession.open(profile).also { try { session?.close() } catch (ignored: Exception) {}; session = it }
                            val uploaded = transfer(s, job, sh)
                            val folderRel = "${job.year}\\${job.folderName}"
                            if (uploaded) {
                                sh.uploaded.incrementAndGet()
                                log("I", "↑ ${item.displayName} → $folderRel")
                            } else {
                                sh.skipped.incrementAndGet()
                                // Nicht jede Datei einzeln protokollieren: Zusammenfassung pro Ordner
                                sh.already.getOrPut(folderRel) { AtomicInteger() }.incrementAndGet()
                                if (sh.alreadyPending.incrementAndGet() >= 250) flushAlready(sh)
                            }
                            break
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            if (isPendingOrTrashed(e)) break // leise überspringen; nicht als "uploaded"/"failed" gezählt -> nächster Lauf
                            if (isConnectionError(e)) {
                                try { session?.close() } catch (ignored: Exception) {}
                                session = null
                                // Einmal mit neuer Verbindung wiederholen (Upload setzt an der Abbruchstelle fort)
                                if (attempt++ == 0) { delay(2_000); continue }
                                throw e // Netz wirklich weg -> Lauf unterbrechen, Datei folgt beim nächsten Sync
                            }
                            sh.failed.incrementAndGet()
                            log("E", "✗ ${item.displayName}: ${shortError(e)}")
                            break
                        }
                    }
                } finally {
                    sh.inflight.remove(item.id)
                }
                countDone(sh, item.size)
            }
        } finally {
            session?.close()
        }
    }

    private fun countDone(sh: Shared, bytes: Long) {
        sh.bytesDone.addAndGet(bytes)
        val done = sh.done.incrementAndGet()
        emitShared(sh, null, force = done == sh.totalFiles)
    }

    private fun emitShared(sh: Shared, file: String?, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - sh.lastEmit < 700) return
        sh.lastEmit = now
        val bytes = sh.bytesDone.get() + sh.inflight.values.sum()
        emit(str(R.string.eng_transferring), sh.done.get(), sh.totalFiles, bytes.coerceAtMost(sh.totalBytes), sh.totalBytes, file)
    }

    private suspend fun finish(
        status: String, message: Msg, uploaded: Int, skipped: Int, failed: Int, waiting: Int, bytes: Long,
    ): SyncRun {
        val text = message.text()
        run = run.copy(
            finishedAt = System.currentTimeMillis(), status = status, message = text, messageCode = message.toCode(),
            uploaded = uploaded, skipped = skipped, failed = failed, waiting = waiting, bytes = bytes
        )
        dao.updateRun(run)
        if (!dryRun) log("I", Msg.of(R.string.eng_end, message))
        return run
    }

    private fun emit(phase: String, done: Int, total: Int, doneBytes: Long, totalBytes: Long, file: String?) =
        onProgress(SyncProgress(profile.name, phase, done, total, doneBytes, totalBytes, file))

    private fun isConnectionError(e: Throwable): Boolean = isNetworkError(e)

    // ---- Analyse mit Cache ------------------------------------------------------
    private suspend fun mediaInfo(item: MediaItem, analyzer: MediaAnalyzer, countries: CountryLookup): MediaInfo {
        dao.mediaInfo(item.sourceKey)?.let { c ->
            if (c.size == item.size && c.dateModified == item.dateModifiedSec) {
                // Länderdaten erst nachträglich verfügbar? -> nur Land neu bestimmen
                if (c.latitude != null && c.longitude != null && c.countryName == null && countries.isAvailable) {
                    val country = countries.lookup(c.latitude, c.longitude)
                    val upd = c.copy(countryIso = country?.iso, countryName = country?.nameDe)
                    if (!dryRun) dao.upsertMediaInfo(upd)
                    return upd
                }
                return c
            }
        }
        val r = analyzer.analyze(item)
        val country = if (r.latitude != null && r.longitude != null) countries.lookup(r.latitude, r.longitude) else null
        val info = MediaInfo(
            sourceKey = item.sourceKey, size = item.size, dateModified = item.dateModifiedSec,
            year = r.yearMonth.year, month = r.yearMonth.monthValue, dateSource = r.dateSource,
            latitude = r.latitude, longitude = r.longitude,
            countryIso = country?.iso?.ifBlank { null }, countryName = country?.nameDe,
        )
        dao.upsertMediaInfo(info)
        return info
    }

    // ---- Übertragung einer Datei ---------------------------------------------------
    /** Original-URI (inkl. GPS-Daten), sonst würde Android die Standortdaten aus der Kopie entfernen. */
    private fun readUri(item: MediaItem): Uri =
        if (canReadLocation) try { MediaStore.setRequireOriginal(item.uri) } catch (e: Exception) { item.uri }
        else item.uri

    private fun openLocal(item: MediaItem): ParcelFileDescriptor.AutoCloseInputStream {
        val pfd = try {
            context.contentResolver.openFileDescriptor(readUri(item), "r")
        } catch (e: IllegalStateException) {
            // pending/trashed: kein Dateifehler, Datei folgt beim nächsten Lauf
            if (isPendingOrTrashed(e)) throw LocalItemNotReadyException(item.displayName, e) else throw e
        } ?: throw java.io.IOException(str(R.string.eng_err_unreadable, item.displayName))
        return ParcelFileDescriptor.AutoCloseInputStream(pfd)
    }

    private fun localHash(item: MediaItem): String = localLevelHash(item, PartialHash.LEVEL_LOW)

    /** Prüfsumme der Datei auf dem Handy nach Prüfstufe (0.9.0); "Wenig" = [PartialHash.compute] */
    private fun localLevelHash(item: MediaItem, level: Int): String = openLocal(item).use { ins ->
        val ch: FileChannel = ins.channel
        PartialHash.forLevel(level, item.size) { off, len ->
            val bb = ByteBuffer.allocate(len)
            var pos = off
            while (bb.hasRemaining()) {
                val n = ch.read(bb, pos)
                if (n < 0) break
                pos += n
            }
            if (bb.position() == len) bb.array() else bb.array().copyOf(bb.position())
        }
    }

    /**
     * Überträgt eine Datei in den Zielordner.
     * @return true = hochgeladen, false = identische Datei war schon vorhanden
     */
    private suspend fun transfer(smb: SmbSession, job: Job, sh: Shared): Boolean {
        val item = job.item
        val folderPath = smb.path(job.year.toString(), job.folderName)
        val folderKey = folderPath.lowercase()
        val name = FolderNames.sanitize(item.displayName)
        var lHash: String? = null
        fun lh(): String = lHash ?: localHash(item).also { lHash = it }

        // Namensvergabe unter Sperre: bild.jpg, bild_2.jpg, bild_3.jpg, …
        // (andere Worker könnten gleichzeitig eine gleichnamige Datei in denselben Ordner laden)
        val candidate: String
        sh.nameLock.withLock {
            val listing = sh.listings.getOrPut(folderKey) {
                HashMap<String, RemoteEntry>().apply {
                    smb.listDir(folderPath).filter { !it.isDirectory }.forEach { put(it.name.lowercase(), it) }
                }
            }
            var n = 1
            var c = name
            while (true) {
                val key = c.lowercase()
                if ("$folderKey\\$key" !in sh.reserved) {
                    val e = listing[key] ?: break
                    if (e.size == item.size && smb.partialHash("$folderPath\\${e.name}", e.size) == lh()) {
                        record(item, "$folderPath\\${e.name}", lh(), verified = true)
                        return false
                    }
                }
                n++
                c = FolderNames.conflictName(name, n)
            }
            // Duplikatsuche in den anderen Ordnern desselben Monats (z. B. von Hand von "10" nach
            // "10 - Ägypten" verschoben): gleicher Name (auch bild_2 …), gleiche Grösse, gleiche Prüfsumme
            for (sibling in job.siblingFolders) {
                val sPath = smb.path(job.year.toString(), sibling)
                val sListing = sh.listings.getOrPut(sPath.lowercase()) {
                    HashMap<String, RemoteEntry>().apply {
                        smb.listDir(sPath).filter { !it.isDirectory }.forEach { put(it.name.lowercase(), it) }
                    }
                }
                for (e in sListing.values) {
                    if (e.size != item.size || !FolderNames.sameBaseName(e.name, name)) continue
                    if (smb.partialHash("$sPath\\${e.name}", e.size) == lh()) {
                        record(item, "$sPath\\${e.name}", lh(), verified = true)
                        return false
                    }
                }
            }
            sh.reserved += "$folderKey\\${c.lowercase()}"
            smb.ensureDir(folderPath)
            candidate = c
        }

        val finalPath = "$folderPath\\$candidate"
        val reservedKey = "$folderKey\\${candidate.lowercase()}"
        var ok = false
        try {
            val ctx = currentCoroutineContext()
            smb.upload(
                finalPath = finalPath,
                size = item.size,
                openInput = { offset -> openAt(item, offset) },
                onProgress = { b -> sh.inflight[item.id] = b; emitShared(sh, item.displayName) },
                checkCancel = { ctx.ensureActive() },
                modifiedMillis = item.dateModifiedSec * 1000,
                // Fortsetzen einer angefangenen .s2ptmp nur mit Prüfung gegen die Datei auf dem Handy (0.4.1)
                expectedHash = { lh() },
            )

            // Prüfung nach Stufe (0.9.0): immer die Grösse, ab "Wenig" zusätzlich die Prüfsumme der Zieldatei
            // (Pflicht, wenn gelöscht werden soll). "Aus" gilt nicht als verifiziert.
            var verified = false
            val level = profile.verify()
            val remoteSize = smb.fileSize(finalPath)
            val sameContent = when (level) {
                VerifyLevel.OFF -> true
                VerifyLevel.LOW -> smb.partialHash(finalPath, item.size) == lh()
                else -> smb.levelHash(finalPath, item.size, level.code) == localLevelHash(item, level.code)
            }
            if (remoteSize != item.size || !sameContent) {
                try { smb.delete(finalPath) } catch (ignored: Exception) {}
                throw java.io.IOException(str(R.string.eng_err_verify))
            }
            if (level != VerifyLevel.OFF) verified = true
            record(item, finalPath, lHash, verified)
            ok = true
            return true
        } finally {
            withContext(NonCancellable) {
                sh.nameLock.withLock {
                    sh.reserved -= reservedKey
                    if (ok) sh.listings[folderKey]?.put(candidate.lowercase(), RemoteEntry(candidate, item.size, false))
                }
            }
        }
    }

    private fun openAt(item: MediaItem, offset: Long): InputStream {
        val ins = openLocal(item)
        if (offset > 0) ins.channel.position(offset)
        return ins
    }

    private suspend fun record(item: MediaItem, remotePath: String, hash: String?, verified: Boolean) {
        val wantDelete = verified && profile.delete() != DeleteMode.KEEP
        dao.upsertSynced(
            SyncedFile(
                profileId = profile.id, sourceKey = item.sourceKey, mediaUri = item.uri.toString(),
                size = item.size, dateModified = item.dateModifiedSec, partialHash = hash,
                remotePath = remotePath, syncedAt = System.currentTimeMillis(), verified = verified,
                deleteState = if (wantDelete) DeleteState.WANTED else DeleteState.NONE,
            )
        )
    }
}
