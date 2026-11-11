package com.sync2pc.app

import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.data.PendingRestore
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.mirror.LocalEntry
import com.sync2pc.app.mirror.LocalFiles
import com.sync2pc.app.mirror.MirrorEngine
import com.sync2pc.app.mirror.RestoreExecutor
import com.sync2pc.app.mirror.RestoreHost
import com.sync2pc.app.mirror.RestoreRetryPolicy
import com.sync2pc.app.mirror.TrashIndex
import com.sync2pc.app.smb.RemoteEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketException

/**
 * [RestoreExecutor] (Wiederherstellen aus der Papierkorb-Vormerkung, SYNC-20261111-163 Stufe 2) auf der JVM mit
 * [FakeShare], einem Fake für [LocalFiles] und einem Fake für [RestoreHost]. Fehlerinjektion über [RestoreEnv.failOnce].
 *
 * Inhalte werden über die Grösse unterschieden: 10 = alte Version (Papierkorb), 20 = aktuelle PC-Version,
 * 30 = aktuelle Handy-Version.
 *
 * Tests mit "IST" im Namen halten das HEUTIGE Verhalten fest, das kein Sollverhalten ist (Abbruch ohne Zurückrollen,
 * unvollständiges Zurückrollen). Das Sollverhalten hängt an SYNC-20261110-125; wer es umsetzt, passt diese Tests an.
 *
 * Nicht abgedeckt: die echte SMB-Verbindung, die echte Handy-Ordner-Schicht (LocalTree, SAF), Room und RestoreFailures
 * (Prefs) sowie die Übertragungen `upload`/`download` der Engine (hier vereinfachte Fakes).
 */
class RestoreExecutorTest {

    private val bin = MirrorEngine.TRASH_DIR
    private val oldTrash = "$bin/2026-11-03/docs/a.txt" // Pfad der alten Version (Handy-Schreibweise "/")
    private val oldTrashPc = "Base\\$bin\\2026-11-03\\docs\\a.txt"
    private val newTrashLocal = "$bin/2026-11-04/docs/a.txt" // Ziel der aktuellen Handy-Version
    private val newTrashPc = "Base\\$bin\\2026-11-04\\docs\\a.txt" // Ziel der aktuellen PC-Version

    /** Ein Ausgangszustand: PC-Papierkorb enthält die alte Version, Handy und PC die aktuellen */
    private fun pcToPhone(id: Long = 1, name: String = "a.txt"): RestoreEnv = RestoreEnv().also { it.addPcToPhone(id, name) }

    /** Ein Ausgangszustand: Handy-Papierkorb enthält die alte Version, Handy und PC die aktuellen */
    private fun phoneToPc(id: Long = 1, name: String = "a.txt"): RestoreEnv = RestoreEnv().also { it.addPhoneToPc(id, name) }

    // ---- (a) Erfolg ------------------------------------------------------------------

    @Test
    fun successPcToPhone_movesCurrentVersionsToTrashFirst_thenRestoresOldThenTransfers() {
        val env = pcToPhone()
        val ran = env.attempt().getOrThrow()
        assertTrue(ran)
        assertEquals(
            listOf(
                "H deleteMirrorState",
                "L moveToTrash docs/a.txt",
                "R ensureDir Base\\$bin\\2026-11-04\\docs",
                "R move Base\\docs\\a.txt",
                "R ensureDir Base\\docs",
                "R move $oldTrashPc",
                "H download docs/a.txt",
                "H deleteConflict",
                "H deleteRestore",
            ), env.events
        )
        // Alte Version am Ort, aktuelle Versionen im Papierkorb, nichts verloren
        assertEquals(10L, env.share.stat(env.share.path("docs\\a.txt"))?.size)
        assertEquals(20L, env.share.stat(newTrashPc)?.size)
        assertNull(env.share.stat(oldTrashPc))
        assertEquals(10L, env.local.find("docs/a.txt")?.size)
        assertEquals(30L, env.local.find(newTrashLocal)?.size)
        // Sync-Stand vom Transfer gesetzt, Vormerkung und Konflikt weg, Zähler
        assertEquals(10L, env.states["docs/a.txt"]?.localSize)
        assertEquals(10L, env.states["docs/a.txt"]?.remoteSize)
        assertTrue(env.restores.isEmpty())
        assertFalse("docs/a.txt" in env.conflicts)
        assertEquals(2, env.trashedCount)
        assertEquals(0, env.failedCount)
        assertEquals(listOf("eng_restoring", "eng_restored"), env.logKeys())
    }

    @Test
    fun successPhoneToPc_movesCurrentVersionsToTrashFirst_thenRestoresOldThenTransfers() {
        val env = phoneToPc()
        assertTrue(env.attempt().getOrThrow())
        assertEquals(
            listOf(
                "H deleteMirrorState",
                "L moveToTrash docs/a.txt",
                "R ensureDir Base\\$bin\\2026-11-04\\docs",
                "R move Base\\docs\\a.txt",
                "L moveFile $oldTrash -> docs/a.txt",
                "H upload docs/a.txt",
                "H deleteConflict",
                "H deleteRestore",
            ), env.events
        )
        assertEquals(10L, env.local.find("docs/a.txt")?.size)
        assertNull(env.local.find(oldTrash))
        assertEquals(30L, env.local.find(newTrashLocal)?.size)
        assertEquals(10L, env.share.stat(env.share.path("docs\\a.txt"))?.size)
        assertEquals(20L, env.share.stat(newTrashPc)?.size)
        assertEquals(10L, env.states["docs/a.txt"]?.remoteSize)
        assertTrue(env.restores.isEmpty())
        assertEquals(2, env.trashedCount)
        assertEquals(listOf("eng_restoring", "eng_restored"), env.logKeys())
    }

    @Test
    fun successWithoutCurrentVersions_onlyRestoresAndTransfers() {
        // Aktuelle Version existiert auf keiner Seite mehr (z. B. gelöscht): nichts in den Papierkorb, kein Sync-Stand vorher
        val env = RestoreEnv()
        env.restores += PendingRestore(1, 1, "REMOTE", oldTrash, "docs/a.txt", 0)
        env.share.add("$bin\\2026-11-03\\docs\\a.txt", 10, "old")
        assertTrue(env.attempt().getOrThrow())
        assertEquals(
            listOf(
                "H deleteMirrorState", "R ensureDir Base\\docs", "R move $oldTrashPc",
                "H download docs/a.txt", "H deleteConflict", "H deleteRestore",
            ), env.events
        )
        assertEquals(0, env.trashedCount)
        assertEquals(10L, env.local.find("docs/a.txt")?.size)
    }

    // ---- (b) Fehler (IOException) in jedem Schritt -> Zurückrollen ---------------------

    @Test
    fun errorInEveryStepPcToPhone_rollsBackToTheStateBefore_andCountsTheFailure() {
        val steps = listOf(
            "H deleteMirrorState", "L moveToTrash docs/a.txt", "R ensureDir Base\\$bin\\2026-11-04",
            "R move Base\\docs\\a.txt", "R ensureDir Base\\docs", "R move Base\\$bin\\2026-11-03", "H download",
        )
        for (step in steps) {
            val env = pcToPhone()
            val before = env.snapshot()
            env.failOnce(step, IOException("simuliert: $step"))
            assertTrue("Schritt $step: Lauf endet normal", env.attempt().getOrThrow())
            assertEquals("Schritt $step: Zustand wie vorher", before, env.snapshot())
            assertEquals("Schritt $step", 1, env.failedCount)
            assertEquals("Schritt $step", 1, env.failures[1L])
            assertEquals("Schritt $step: Vormerkung bleibt", 1, env.restores.size)
            assertFalse("Schritt $step", env.connectionFailed)
            assertEquals("Schritt $step", "eng_restore_failed_rolled_back", env.logKeys().last())
        }
    }

    @Test
    fun errorInEveryStepPhoneToPc_rollsBackToTheStateBefore_andCountsTheFailure() {
        val steps = listOf(
            "H deleteMirrorState", "L moveToTrash docs/a.txt", "R ensureDir Base\\$bin\\2026-11-04",
            "R move Base\\docs\\a.txt", "L moveFile $oldTrash", "H upload",
        )
        for (step in steps) {
            val env = phoneToPc()
            val before = env.snapshot()
            env.failOnce(step, IOException("simuliert: $step"))
            assertTrue("Schritt $step: Lauf endet normal", env.attempt().getOrThrow())
            assertEquals("Schritt $step: Zustand wie vorher", before, env.snapshot())
            assertEquals("Schritt $step", 1, env.failedCount)
            assertEquals("Schritt $step", 1, env.failures[1L])
            assertEquals("Schritt $step: Vormerkung bleibt", 1, env.restores.size)
            assertEquals("Schritt $step", "eng_restore_failed_rolled_back", env.logKeys().last())
        }
    }

    @Test
    fun failedRestoreDoesNotStopTheNextOne() {
        val env = pcToPhone(1, "a.txt")
        env.addPcToPhone(2, "b.txt")
        env.failOnce("L moveToTrash docs/a.txt", IOException("simuliert"))
        assertTrue(env.attempt().getOrThrow())
        assertEquals(listOf(1L), env.restores.map { it.id })
        assertEquals(10L, env.local.find("docs/b.txt")?.size)
        assertEquals(30L, env.local.find("docs/a.txt")?.size) // a.txt unverändert
        assertEquals(1, env.failedCount)
    }

    @Test
    fun fifthFailureGivesUp_andAnExhaustedRestoreIsNotRunAgain() {
        val env = phoneToPc()
        env.failures[1L] = RestoreRetryPolicy.MAX_ATTEMPTS - 1
        env.failOnce("H upload", IOException("simuliert"))
        assertTrue(env.attempt().getOrThrow())
        assertEquals(RestoreRetryPolicy.MAX_ATTEMPTS, env.failures[1L])
        assertEquals("eng_restore_gave_up", env.logKeys().last())
        assertEquals(1, env.restores.size) // Vormerkung bleibt (schützt die Papierkorb-Version)
        // Nächster Lauf: aufgebraucht -> nichts geschieht
        env.events.clear(); env.logs.clear()
        assertFalse(env.attempt().getOrThrow())
        assertTrue(env.events.isEmpty())
        assertTrue(env.logs.isEmpty())
    }

    @Test
    fun noPendingRestores_doesNothingAndLogsNothing() {
        val env = RestoreEnv()
        assertFalse(env.attempt().getOrThrow())
        assertTrue(env.events.isEmpty())
        assertTrue(env.logs.isEmpty())
    }

    // ---- (b/IST) Fehler nach der Übertragung: Zurückrollen unvollständig --------------

    @Test
    fun IST_errorAfterTransferPcToPhone_rollbackIsPartial_notTheDesiredBehavior() {
        // IST-Verhalten, nicht Soll: Schlägt erst das Aufräumen nach dem Download fehl (hier deleteConflict), wird
        // zurückgerollt, ABER die heruntergeladene Datei bleibt am Ort -> die aktuelle Handy-Version kann nicht zurück
        // (Ort belegt, ok=false), der Sync-Stand bleibt der des Downloads (nicht der alte).
        val env = pcToPhone()
        env.failOnce("H deleteConflict", IOException("simuliert"))
        assertTrue(env.attempt().getOrThrow())
        assertEquals("eng_restore_failed_partial", env.logKeys().last())
        assertEquals(1, env.restores.size) // Vormerkung bleibt
        assertEquals(1, env.failures[1L])
        // PC: alte Version wieder im Papierkorb, aktuelle Version wieder am Ort
        assertEquals(10L, env.share.stat(oldTrashPc)?.size)
        assertEquals(20L, env.share.stat(env.share.path("docs\\a.txt"))?.size)
        // Handy: die heruntergeladene alte Version liegt am Ort, die aktuelle (30) bleibt im Papierkorb
        assertEquals(10L, env.local.find("docs/a.txt")?.size)
        assertEquals(30L, env.local.find(newTrashLocal)?.size)
        // Sync-Stand: der des Downloads (remote 10), obwohl der PC jetzt wieder 20 hat; der alte Stand (30/20) ist weg
        assertEquals(10L, env.states["docs/a.txt"]?.remoteSize)
    }

    @Test
    fun IST_errorAfterTransferPhoneToPc_rollbackIsPartial_notTheDesiredBehavior() {
        // IST-Verhalten, nicht Soll: gespiegelt zum Test davor. Die hochgeladene alte Version bleibt auf dem PC liegen
        // (Ort belegt, ok=false), die aktuelle PC-Version bleibt im PC-Papierkorb; das Handy ist zurückgerollt.
        val env = phoneToPc()
        env.failOnce("H deleteConflict", IOException("simuliert"))
        assertTrue(env.attempt().getOrThrow())
        assertEquals("eng_restore_failed_partial", env.logKeys().last())
        assertEquals(1, env.restores.size)
        assertEquals(10L, env.local.find(oldTrash)?.size) // alte Version wieder im Handy-Papierkorb
        assertEquals(30L, env.local.find("docs/a.txt")?.size) // aktuelle Handy-Version wieder am Ort
        assertEquals(10L, env.share.stat(env.share.path("docs\\a.txt"))?.size) // hochgeladene alte Version bleibt
        assertEquals(20L, env.share.stat(newTrashPc)?.size) // aktuelle PC-Version bleibt im Papierkorb
        assertEquals(10L, env.states["docs/a.txt"]?.remoteSize) // Sync-Stand des Uploads
    }

    // ---- (c) Netzwerkfehler -----------------------------------------------------------

    @Test
    fun networkErrorPcToPhone_rollsBack_keepsRestorePending_andThrows() {
        val env = pcToPhone(1, "a.txt")
        env.addPcToPhone(2, "b.txt")
        val before = env.snapshot()
        env.failOnce("R move Base\\docs\\a.txt", SocketException("simuliert"))
        val result = env.attempt()
        assertTrue(result.exceptionOrNull() is SocketException)
        assertTrue(env.connectionFailed)
        assertEquals(0, env.failedCount)
        assertTrue("Netzwerkfehler zählt nicht als Fehlversuch", env.failures.isEmpty())
        assertEquals(listOf(1L, 2L), env.restores.map { it.id }) // beide Vormerkungen bleiben
        assertEquals("Zustand wie vorher (auch die zweite Wiederherstellung unberührt)", before, env.snapshot())
        assertTrue("kein Fehlerprotokoll", env.logKeys().none { it.startsWith("eng_restore_failed") })
    }

    @Test
    fun networkErrorPhoneToPc_duringTransfer_rollsBack_keepsRestorePending_andThrows() {
        val env = phoneToPc()
        val before = env.snapshot()
        env.failOnce("H upload", IOException("Übertragung", SocketException("simuliert")))
        val result = env.attempt()
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(env.connectionFailed)
        assertEquals(0, env.failedCount)
        assertTrue(env.failures.isEmpty())
        assertEquals(before, env.snapshot())
    }

    @Test
    fun IST_networkErrorAndRollbackFailsToo_oldSyncStateIsNotRestored() {
        // IST-Verhalten, nicht Soll: Bricht die Verbindung ab und scheitert deshalb auch das Zurückrollen der PC-Version,
        // bleibt die aktuelle PC-Version im Papierkorb (am Ort fehlt sie), der Handy-Teil wird zurückgerollt, der alte
        // Sync-Stand wird NICHT wiederhergestellt (er wurde in Schritt 1 gelöscht). Vormerkung bleibt, Fehler geht nach aussen.
        val env = pcToPhone()
        env.failFrom("R ensureDir Base\\docs", SocketException("Verbindung weg"))
        val result = env.attempt()
        assertTrue(result.exceptionOrNull() is SocketException)
        assertTrue(env.connectionFailed)
        assertEquals(1, env.restores.size)
        assertTrue("alter Sync-Stand nicht zurückgeschrieben", env.states.isEmpty())
        assertEquals(30L, env.local.find("docs/a.txt")?.size) // Handy zurückgerollt
        assertNull(env.share.stat(env.share.path("docs\\a.txt"))) // aktuelle PC-Version fehlt am Ort ...
        assertEquals(20L, env.share.stat(newTrashPc)?.size) // ... liegt aber im PC-Papierkorb
        assertEquals(10L, env.share.stat(oldTrashPc)?.size) // alte Version unangetastet
        assertTrue(env.warnings.any { it.contains("PC-Version") })
    }

    // ---- (d) Prozessabbruch / Abbruch durch den Benutzer: kein Zurückrollen -----------

    @Test
    fun IST_processDeathMidwayPcToPhone_noRollback_thenNextRunCompletes() {
        // IST-Verhalten, nicht Soll: Ein Error (Prozessabbruch) wird von `catch (e: Exception)` nicht gefangen -> KEIN
        // Zurückrollen. Zustand: beide aktuellen Versionen im Papierkorb, am Ort nichts, Sync-Stand gelöscht,
        // Vormerkung bleibt, die alte Version liegt noch im Papierkorb. Der nächste Lauf führt die Vormerkung zu Ende.
        val env = pcToPhone()
        env.failOnce("R ensureDir Base\\docs", RestoreProcessDeath())
        val result = env.attempt()
        assertTrue(result.exceptionOrNull() is RestoreProcessDeath)
        assertNull(env.local.find("docs/a.txt"))
        assertEquals(30L, env.local.find(newTrashLocal)?.size)
        assertNull(env.share.stat(env.share.path("docs\\a.txt")))
        assertEquals(20L, env.share.stat(newTrashPc)?.size)
        assertEquals(10L, env.share.stat(oldTrashPc)?.size)
        assertTrue(env.states.isEmpty())
        assertEquals(1, env.restores.size)
        assertTrue(env.failures.isEmpty())
        assertEquals(0, env.failedCount)
        assertEquals(listOf("eng_restoring"), env.logKeys()) // nichts über den Abbruch protokolliert

        // Wiederanlauf: Vormerkung ist noch da und die Quelle auch -> wird abgeschlossen
        assertTrue(env.attempt().getOrThrow())
        assertTrue(env.restores.isEmpty())
        assertEquals(10L, env.local.find("docs/a.txt")?.size)
        assertEquals(10L, env.share.stat(env.share.path("docs\\a.txt"))?.size)
        assertEquals("eng_restored", env.logKeys().last())
    }

    @Test
    fun IST_processDeathAfterLocalSourceMoved_noRollback_nextRunReportsSourceGone() {
        // IST-Verhalten, nicht Soll: Nach dem Verschieben der Handy-Quelle (Papierkorb -> Ort) und vor dem Upload fehlt
        // die Quelle im Papierkorb. Der nächste Lauf meldet "Quelle fehlt" und löscht die Vormerkung, obwohl nie auf den
        // PC kopiert wurde. Daten gehen nicht verloren (alte Version auf dem Handy am Ort, aktuelle Versionen im
        // Papierkorb), aber der PC hat die Datei nicht, bis der nächste normale Abgleich sie neu hochlädt.
        val env = phoneToPc()
        env.failOnce("H upload", RestoreProcessDeath())
        assertTrue(env.attempt().exceptionOrNull() is RestoreProcessDeath)
        assertEquals(10L, env.local.find("docs/a.txt")?.size)
        assertNull(env.local.find(oldTrash))
        assertEquals(30L, env.local.find(newTrashLocal)?.size)
        assertNull(env.share.stat(env.share.path("docs\\a.txt")))
        assertEquals(20L, env.share.stat(newTrashPc)?.size)
        assertTrue(env.states.isEmpty())
        assertEquals(1, env.restores.size)

        assertTrue(env.attempt().getOrThrow())
        assertEquals("eng_restore_gone", env.logKeys().last())
        assertTrue("Vormerkung gelöscht", env.restores.isEmpty())
        assertNull("PC hat die Datei weiterhin nicht", env.share.stat(env.share.path("docs\\a.txt")))
        assertEquals(10L, env.local.find("docs/a.txt")?.size)
    }

    @Test
    fun IST_processDeathAfterPcSourceMoved_noRollback_nextRunReportsSourceGone() {
        // IST-Verhalten, nicht Soll: gespiegelt (PC -> Handy). Die alte Version liegt am Ort auf dem PC, aufs Handy
        // wurde nichts kopiert, die Vormerkung fällt im nächsten Lauf als "Quelle fehlt" weg.
        val env = pcToPhone()
        env.failOnce("H download", RestoreProcessDeath())
        assertTrue(env.attempt().exceptionOrNull() is RestoreProcessDeath)
        assertEquals(10L, env.share.stat(env.share.path("docs\\a.txt"))?.size)
        assertNull(env.local.find("docs/a.txt"))
        assertEquals(30L, env.local.find(newTrashLocal)?.size)

        assertTrue(env.attempt().getOrThrow())
        assertEquals("eng_restore_gone", env.logKeys().last())
        assertTrue(env.restores.isEmpty())
        assertNull(env.local.find("docs/a.txt"))
    }

    @Test
    fun IST_cancellationDuringTransfer_noRollback_restoreStaysPending() {
        // IST-Verhalten, nicht Soll: Bricht der Benutzer (oder Android) den Lauf mitten im Download ab, kommt eine
        // CancellationException; sie wird bewusst durchgereicht, ohne Zurückrollen und ohne Fehlerzähler. Zustand wie
        // beim Prozessabbruch nach dem Verschieben der PC-Quelle.
        val env = pcToPhone()
        env.failOnce("H download", CancellationException("simuliert"))
        val result = env.attempt()
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertFalse(env.connectionFailed)
        assertEquals(0, env.failedCount)
        assertTrue(env.failures.isEmpty())
        assertEquals(1, env.restores.size)
        assertEquals(10L, env.share.stat(env.share.path("docs\\a.txt"))?.size)
        assertEquals(20L, env.share.stat(newTrashPc)?.size)
        assertNull(env.local.find("docs/a.txt"))
        assertTrue(env.states.isEmpty())
    }

    // ---- (e) Quelle fehlt -------------------------------------------------------------

    @Test
    fun sourceMissing_deletesRestore_clearsFailureAndTouchesNothing() {
        for (side in listOf("REMOTE", "LOCAL")) {
            val env = RestoreEnv()
            env.restores += PendingRestore(1, 1, side, oldTrash, "docs/a.txt", 0)
            env.failures[1L] = 2
            env.share.add("docs\\a.txt", 20, "cur")
            env.local.put("docs/a.txt", 30)
            env.states["docs/a.txt"] = MirrorState(1, "docs/a.txt", 30, 0, 20, 0, 1)
            env.conflicts += "docs/a.txt"
            val before = env.snapshot().replace(Regex("restores=.*"), "")
            assertTrue("Seite $side", env.attempt().getOrThrow())
            assertTrue("Seite $side", env.restores.isEmpty())
            assertTrue("Seite $side: Fehlerzähler zurückgesetzt", env.failures.isEmpty())
            assertEquals("Seite $side", listOf("eng_restoring", "eng_restore_gone"), env.logKeys())
            assertEquals("Seite $side: nichts verschoben, Sync-Stand und Konflikt unberührt", listOf("H deleteRestore"), env.events)
            assertEquals("Seite $side", before, env.snapshot().replace(Regex("restores=.*"), ""))
            assertTrue("Seite $side: Konflikt bleibt", "docs/a.txt" in env.conflicts)
        }
    }
}

/** Simulierter Prozessabbruch: ein [Error] wird von `catch (e: Exception)` nicht gefangen */
private class RestoreProcessDeath : Error("simulierter Prozessabbruch")

/** Handy-Ordner im Speicher (Schlüssel = relativer Pfad in Kleinbuchstaben); [hook] wird vor jeder schreibenden Operation gerufen */
private class RestoreFakeLocal(private val hook: (String) -> Unit) : LocalFiles {
    class F(val name: String, val size: Long, val modified: Long)

    val files = LinkedHashMap<String, F>()

    fun put(rel: String, size: Long, modified: Long = 0L) {
        files[rel.lowercase()] = F(rel.substringAfterLast('/'), size, modified)
    }

    private fun entry(rel: String, f: F) = LocalEntry(docId = rel, name = f.name, size = f.size, modified = f.modified, isDir = false)

    override fun find(relPath: String): LocalEntry? = files[relPath.lowercase()]?.let { entry(relPath, it) }

    override fun stat(docId: String): LocalEntry? = find(docId)

    override fun moveFile(fromRel: String, entry: LocalEntry, toRel: String): String {
        hook("L moveFile $fromRel -> $toRel")
        val f = files[fromRel.lowercase()] ?: throw IOException("Quelle fehlt: $fromRel")
        if (toRel.lowercase() in files) throw IOException("Ziel existiert: $toRel")
        files.remove(fromRel.lowercase())
        files[toRel.lowercase()] = F(toRel.substringAfterLast('/'), f.size, f.modified)
        return toRel
    }

    override fun moveToTrash(relPath: String, entry: LocalEntry, dateFolder: String): String {
        hook("L moveToTrash $relPath")
        val f = files[relPath.lowercase()] ?: throw IOException("Quelle fehlt: $relPath")
        val parent = relPath.substringBeforeLast('/', "")
        val dir = listOf(MirrorEngine.TRASH_DIR, dateFolder, parent).filter { it.isNotEmpty() }.joinToString("/")
        val name = TrashIndex.uniqueTrashName(f.name, "12-00-00") { "$dir/$it".lowercase() in files }
        val target = "$dir/$name"
        files.remove(relPath.lowercase())
        files[target.lowercase()] = F(name, f.size, f.modified)
        return target
    }
}

/** Gemeinsamer Zustand eines Testlaufs und zugleich der [RestoreHost] (Datenbank, Zähler, Protokoll, Übertragungen als Fakes) */
private class RestoreEnv : RestoreHost {
    /** Zeitleiste aller schreibenden Operationen (R = PC, L = Handy, H = Host/Engine); Fehler werden vorher eingespielt */
    val events = ArrayList<String>()
    val share = FakeShare("Base")
    val local = RestoreFakeLocal { hook(it) }
    val states = LinkedHashMap<String, MirrorState>()
    val restores = ArrayList<PendingRestore>()
    val conflicts = HashSet<String>()
    val failures = HashMap<Long, Int>()
    val logs = ArrayList<Pair<String, Msg>>()
    val warnings = ArrayList<String>()
    var trashedCount = 0
    var failedCount = 0
    var connectionFailed = false
    private var fault: ((String) -> Unit)? = null

    init { share.onOp = { op, path -> hook("R $op $path") } }

    override val today = "2026-11-04"

    private fun hook(event: String) {
        fault?.invoke(event)
        events += event
    }

    /** Beim ersten Ereignis, das mit [prefix] beginnt, [error] werfen (einmalig) */
    fun failOnce(prefix: String, error: Throwable) {
        fault = { e -> if (e.startsWith(prefix)) { fault = null; throw error } }
    }

    /** Ab dem ersten Ereignis mit [prefix] schlägt jede PC-Operation mit [error] fehl (Verbindung weg) */
    fun failFrom(prefix: String, error: Throwable) {
        var dead = false
        fault = { e ->
            if (!dead && e.startsWith(prefix)) dead = true
            if (dead && e.startsWith("R ")) throw error
        }
    }

    fun addPcToPhone(id: Long, name: String) {
        val dir = "docs"
        restores += PendingRestore(id, 1, "REMOTE", "$bin/2026-11-03/$dir/$name", "$dir/$name", 0)
        share.add("$bin\\2026-11-03\\$dir\\$name", 10, "old")
        addCurrent(dir, name)
    }

    fun addPhoneToPc(id: Long, name: String) {
        val dir = "docs"
        restores += PendingRestore(id, 1, "LOCAL", "$bin/2026-11-03/$dir/$name", "$dir/$name", 0)
        local.put("$bin/2026-11-03/$dir/$name", 10)
        addCurrent(dir, name)
    }

    private val bin = MirrorEngine.TRASH_DIR

    private fun addCurrent(dir: String, name: String) {
        share.add("$dir\\$name", 20, "cur")
        local.put("$dir/$name", 30)
        conflicts += "$dir/$name"
        states["$dir/$name".lowercase()] = MirrorState(1, "$dir/$name".lowercase(), 30, 0, 20, 0, 1)
    }

    /** Alles, was ein Test vor/nach einem Lauf vergleichen will */
    fun snapshot(): String =
        "share=" + share.allFiles().joinToString { "$it:${share.stat(it)?.size}" } +
            "\nlocal=" + local.files.entries.sortedBy { it.key }.joinToString { "${it.key}:${it.value.size}" } +
            "\nstates=" + states.entries.sortedBy { it.key }.joinToString { it.value.toString() } +
            "\nrestores=" + restores.map { it.id }

    fun logKeys(): List<String> = logs.map { it.second.keys().firstOrNull() ?: "" }

    /** Ausführen; Fehler (auch Error und CancellationException) kommen als Result zurück */
    fun attempt(): Result<Boolean> = runBlocking { runCatching { RestoreExecutor(local, share, this@RestoreEnv).run() } }

    // ---- RestoreHost ----
    override suspend fun pendingRestores(): List<PendingRestore> = restores.toList()
    override suspend fun mirrorStates(): List<MirrorState> = states.values.toList()
    override suspend fun deleteMirrorState(key: String) { hook("H deleteMirrorState"); states.remove(key) }
    override suspend fun upsertMirrorState(state: MirrorState) { hook("H upsertMirrorState"); states[state.relPath] = state }
    override suspend fun deleteConflict(relPath: String) { hook("H deleteConflict"); conflicts.remove(relPath) }
    override suspend fun deleteRestore(id: Long) { hook("H deleteRestore"); restores.removeAll { it.id == id } }
    override fun isExhausted(restoreId: Long): Boolean = RestoreRetryPolicy.exhausted(failures[restoreId] ?: 0)
    override fun markFailure(restoreId: Long): Int { val n = (failures[restoreId] ?: 0) + 1; failures[restoreId] = n; return n }
    override fun clearFailure(restoreId: Long) { failures.remove(restoreId) }
    override suspend fun log(level: String, msg: Msg) { logs += level to msg }
    override fun warn(what: String, e: Exception) { warnings += what }
    override fun countTrashed() { trashedCount++ }
    override fun countFailed() { failedCount++ }
    override fun markConnectionFailed() { connectionFailed = true }

    /** Wie MirrorEngine.trashRemote (Ordner anlegen, freien Namen suchen, verschieben, zählen) */
    override fun trashRemote(rel: String): String {
        val parent = rel.substringBeforeLast('/', "").replace('/', '\\')
        val trashDir = share.path(listOf(MirrorEngine.TRASH_DIR, today, parent).filter { it.isNotEmpty() }.joinToString("\\"))
        share.ensureDir(trashDir)
        val name = TrashIndex.uniqueTrashName(rel.substringAfterLast('/'), "12-00-00") { share.stat("$trashDir\\$it") != null }
        share.move(share.path(rel.replace('/', '\\')), "$trashDir\\$name")
        trashedCount++
        return "$trashDir\\$name"
    }

    /** Vereinfachter Upload: Datei auf dem PC ersetzen, Sync-Stand setzen */
    override suspend fun upload(key: String, rel: String, local: LocalEntry) {
        hook("H upload $rel")
        share.add(rel.replace('/', '\\'), local.size, "up")
        states[key] = MirrorState(1, key, local.size, local.modified, local.size, 0, 1)
    }

    /** Vereinfachter Download: Datei auf dem Handy anlegen, Sync-Stand setzen */
    override suspend fun download(key: String, rel: String, remote: RemoteEntry) {
        hook("H download $rel")
        this.local.put(rel, remote.size, remote.modified)
        states[key] = MirrorState(1, key, remote.size, remote.modified, remote.size, remote.modified, 1)
    }
}
