package com.sync2pc.app

import com.sync2pc.app.mirror.MirrorEngine
import com.sync2pc.app.mirror.PcTrashMover
import com.sync2pc.app.mirror.PcTrashSide
import com.sync2pc.app.mirror.TrashMigration
import com.sync2pc.app.mirror.TrashMigration.Status
import com.sync2pc.app.mirror.TrashMigrationUnsupported
import com.sync2pc.app.mirror.TrashNode
import com.sync2pc.app.mirror.TrashSide
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketException

/**
 * Papierkorb-Übernahme ".LeoSync-Bin" -> "@LeoSync-Bin" (SYNC-20261111-244, Stufe B): der reine Kern [TrashMigration] auf einer
 * Seite im Speicher ([MemSide]) und die PC-Seite [PcTrashSide] auf der [FakeShare]; dazu der Rückfall in [PcTrashMover].
 *
 * Inhalte werden über eine Kennung unterschieden ("alt-a", "neu-a" …): so lässt sich prüfen, dass nichts überschrieben,
 * nichts gedoppelt und nichts verloren geht ([MemSide.contents]).
 *
 * Nicht abgedeckt: das echte Verhalten von SAF ([com.sync2pc.app.mirror.PhoneTrashSide], DocumentsContract) und von SMB
 * ([com.sync2pc.app.smb.SmbSession.renameDir]/`deleteEmptyDir`); beides ist nur am Gerät bzw. an einer echten Freigabe prüfbar.
 * Ebenso das Einhängen in den Lauf (`MirrorEngine.runInner`): hier nur die Entscheidung [TrashMigration.mayRun].
 */
class TrashMigrationTest {

    private val old = MirrorEngine.DOT_TRASH_DIR
    private val new = MirrorEngine.TRASH_DIR
    private val time = { "12-00-00" }

    private fun run(side: TrashSide, network: Boolean = false) =
        TrashMigration.run(side, propagateNetworkErrors = network, time = time)

    // ---- (1) Ausgangslagen -----------------------------------------------------------------

    @Test
    fun emptyShareDoesNothing() {
        val side = MemSide().file("live/keep.txt", "k")
        val r = run(side)
        assertEquals(Status.NOTHING, r.status)
        assertTrue(side.ops.isEmpty())
    }

    @Test
    fun onlyNewNameDoesNothing() {
        val side = MemSide().file("$new/2026-11-01/a.txt", "neu-a")
        val before = side.contents()
        assertEquals(Status.NOTHING, run(side).status)
        assertTrue(side.ops.isEmpty())
        assertEquals(before, side.contents())
    }

    @Test
    fun onlyOldNameIsRenamedAsAWhole() {
        val side = MemSide()
            .file("$old/2026-11-01/docs/a.txt", "alt-a").file("$old/2026-11-02/b.txt", "alt-b")
            .file("live/keep.txt", "k")
        val r = run(side)
        assertEquals(Status.MIGRATED, r.status)
        assertEquals("eine einzige Operation, keine Datei angefasst", listOf("renameDir $old -> $new"), side.ops)
        assertEquals(
            setOf("$new/2026-11-01/docs/a.txt=alt-a", "$new/2026-11-02/b.txt=alt-b", "live/keep.txt=k"), side.contents(),
        )
        assertFalse(side.hasDir(old))
    }

    @Test
    fun oldNameWithoutAnyFilesIsRenamedToo() {
        val side = MemSide().dir(old)
        assertEquals(Status.MIGRATED, run(side).status)
        assertTrue(side.hasDir(new))
        assertFalse(side.hasDir(old))
    }

    @Test
    fun bothNamesAreMergedFileByFileAndOldFolderDisappears() {
        val side = MemSide()
            .file("$old/2026-11-01/a.txt", "alt-a")
            .file("$old/2026-11-01/docs/x.txt", "alt-x")
            .file("$old/2026-10-01/z.txt", "alt-z")
            .file("$new/2026-11-01/b.txt", "neu-b")
            .file("$new/2026-11-03/c.txt", "neu-c")
            .file("live/keep.txt", "k")
        val r = run(side)
        assertEquals(Status.MIGRATED, r.status)
        assertEquals(3, r.moved)
        assertEquals(
            setOf(
                "$new/2026-11-01/a.txt=alt-a", "$new/2026-11-01/docs/x.txt=alt-x", "$new/2026-10-01/z.txt=alt-z",
                "$new/2026-11-01/b.txt=neu-b", "$new/2026-11-03/c.txt=neu-c", "live/keep.txt=k",
            ), side.contents(),
        )
        assertFalse("Altname-Ordner samt leerer Unterordner entfernt", side.hasDir(old))
        assertTrue("nur verschoben, nie kopiert oder gelöscht", side.ops.none { it.startsWith("copy") || it.startsWith("deleteFile") })
    }

    // ---- (2) Gleicher Name am selben Tag ----------------------------------------------------------

    @Test
    fun sameFileOnSameDayInBothKeepsBothWithTimeSuffix() {
        val side = MemSide()
            .file("$old/2026-11-01/a.txt", "alt-a")
            .file("$new/2026-11-01/a.txt", "neu-a")
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(
            setOf("$new/2026-11-01/a.txt=neu-a", "$new/2026-11-01/a (12-00-00).txt=alt-a"), side.contents(),
        )
        assertFalse(side.hasDir(old))
    }

    @Test
    fun secondCollisionInTheSameSecondGetsACounter() {
        val side = MemSide()
            .file("$old/2026-11-01/a.txt", "alt-a")
            .file("$new/2026-11-01/a.txt", "neu-a")
            .file("$new/2026-11-01/a (12-00-00).txt", "neu-a2")
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(
            setOf("$new/2026-11-01/a.txt=neu-a", "$new/2026-11-01/a (12-00-00).txt=neu-a2", "$new/2026-11-01/a (12-00-00-2).txt=alt-a"),
            side.contents(),
        )
    }

    @Test
    fun caseDifferenceInFileNamesCountsAsSameFile() {
        val side = MemSide()
            .file("$old/2026-11-01/A.TXT", "alt-a")
            .file("$new/2026-11-01/a.txt", "neu-a")
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(setOf("$new/2026-11-01/a.txt=neu-a", "$new/2026-11-01/A (12-00-00).TXT=alt-a"), side.contents())
    }

    // ---- (3) Gross-/Kleinschreibung der Ordnernamen --------------------------------------------------

    @Test
    fun differentSpellingOfTheOldNameIsFoundAndRenamed() {
        val side = MemSide().file(".leosync-BIN/2026-11-01/a.txt", "alt-a")
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(setOf("$new/2026-11-01/a.txt=alt-a"), side.contents())
    }

    @Test
    fun existingNewFolderInOtherSpellingIsReusedNotDuplicated() {
        val side = MemSide()
            .file("$old/2026-11-01/a.txt", "alt-a")
            .file("@LEOSYNC-bin/2026-11-01/b.txt", "neu-b")
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(setOf("@LEOSYNC-bin/2026-11-01/a.txt=alt-a", "@LEOSYNC-bin/2026-11-01/b.txt=neu-b"), side.contents())
        assertEquals("kein zweiter Papierkorb-Ordner", 1, side.rootDirs().count { it.equals(new, true) })
    }

    // ---- (4) Abbruch und Wiederholung ------------------------------------------------------------

    private fun mergeScenario() = MemSide()
        .file("$old/2026-11-01/a.txt", "alt-a").file("$old/2026-11-01/b.txt", "alt-b")
        .file("$old/2026-11-02/c.txt", "alt-c").file("$old/2026-11-02/d.txt", "alt-d")
        .file("$new/2026-11-01/a.txt", "neu-a")

    @Test
    fun processDeathAfterNMovesLosesNothingAndRetryCompletes() {
        for (n in 0..3) {
            val side = mergeScenario()
            val before = side.contentValues()
            var moves = 0
            side.hook = { op -> if (op.startsWith("moveFile") && moves++ == n) throw ProcessDeath() }
            try { run(side); fail("n=$n: Abbruch erwartet") } catch (e: ProcessDeath) { /* erwartet */ }
            // Mitten im Lauf: jede Version genau einmal vorhanden (entweder noch im alten oder schon im neuen Ordner)
            assertEquals("n=$n", before, side.contentValues())
            side.hook = null
            val r = run(side)
            assertEquals("n=$n", Status.MIGRATED, r.status)
            assertEquals("n=$n: nichts gedoppelt, nichts verloren", before, side.contentValues())
            assertFalse("n=$n", side.hasDir(old))
            assertTrue("n=$n: alle fünf Versionen unter dem neuen Namen", side.contents().all { it.startsWith("$new/") || it.startsWith("live/") })
        }
    }

    @Test
    fun processDeathBetweenRenameAndMoveOfASuffixedFileIsHarmless() {
        // Handy-Ablauf: erst im Quellordner umbenennen, dann verschieben; bricht es dazwischen ab, liegt die Datei unter dem
        // neuen Namen im alten Papierkorb und wird beim nächsten Lauf einfach weiterverschoben
        val side = MemSide().file("$old/2026-11-01/a.txt", "alt-a").file("$new/2026-11-01/a.txt", "neu-a")
        side.renameBeforeMove = true
        side.hook = { op -> if (op.startsWith("moveFile")) throw ProcessDeath() }
        try { run(side); fail("Abbruch erwartet") } catch (e: ProcessDeath) { /* erwartet */ }
        assertEquals(setOf("$old/2026-11-01/a (12-00-00).txt=alt-a", "$new/2026-11-01/a.txt=neu-a"), side.contents())
        side.hook = null
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(setOf("$new/2026-11-01/a (12-00-00).txt=alt-a", "$new/2026-11-01/a.txt=neu-a"), side.contents())
    }

    @Test
    fun errorOnOneFileKeepsTheRestMovingAndTheOldFolderUntilRetry() {
        val side = mergeScenario()
        val before = side.contentValues()
        side.hook = { op -> if (op == "moveFile $old/2026-11-02/c.txt -> $new/2026-11-02/c.txt") throw IOException("gesperrt") }
        val r = run(side)
        assertEquals(Status.FAILED, r.status)
        assertEquals("gesperrt", r.error?.message)
        assertEquals(3, r.moved)
        assertEquals(before, side.contentValues())
        assertTrue("Altname-Ordner bleibt, solange etwas darin liegt", side.contents().contains("$old/2026-11-02/c.txt=alt-c"))
        side.hook = null
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(before, side.contentValues())
        assertFalse(side.hasDir(old))
    }

    @Test
    fun runningTwiceChangesNothingTheSecondTime() {
        val side = mergeScenario()
        assertEquals(Status.MIGRATED, run(side).status)
        val snapshot = side.contents()
        side.ops.clear()
        assertEquals(Status.NOTHING, run(side).status)
        assertTrue(side.ops.isEmpty())
        assertEquals(snapshot, side.contents())
    }

    @Test
    fun cancellationPropagatesAndLeavesEverythingIntact() {
        val side = mergeScenario()
        val before = side.contentValues()
        var calls = 0
        try {
            TrashMigration.run(side, check = { if (++calls == 3) throw CancellationException("abgebrochen") }, time = time)
            fail("Abbruch muss nach aussen gehen")
        } catch (e: CancellationException) { /* erwartet */ }
        assertEquals(before, side.contentValues())
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(before, side.contentValues())
    }

    // ---- (5) Anbieter kann nicht ---------------------------------------------------------------

    @Test
    fun providerThatCannotRenameLeavesTheOldFolderReadable() {
        val side = MemSide().file("$old/2026-11-01/a.txt", "alt-a").also { it.canRename = false }
        val before = side.contents()
        val r = run(side)
        assertEquals(Status.UNSUPPORTED, r.status)
        assertEquals(before, side.contents())
        assertTrue(side.ops.isEmpty())
        assertTrue("Altname bleibt für Liste/Wiederherstellen erkennbar", MirrorEngine.isTrashDir(old))
    }

    @Test
    fun providerThatCannotMoveLeavesBothFoldersUnchanged() {
        val side = mergeScenario().also { it.canMove = false }
        val before = side.contents()
        val r = run(side)
        assertEquals(Status.UNSUPPORTED, r.status)
        assertEquals("nicht kopiert, nicht gelöscht, nicht überschrieben", before, side.contents())
        assertTrue(side.hasDir(old))
        side.canMove = true
        assertEquals("beim nächsten Lauf geht es weiter", Status.MIGRATED, run(side).status)
        assertFalse(side.hasDir(old))
    }

    // ---- (6) Sonderfälle ------------------------------------------------------------------

    @Test
    fun aFileWithTheNewNameBlocksTheMigration() {
        val side = MemSide().file("$old/2026-11-01/a.txt", "alt-a").file(new, "datei")
        val before = side.contents()
        assertEquals(Status.INCOMPLETE, run(side).status)
        assertEquals(before, side.contents())
        assertTrue(side.ops.isEmpty())
    }

    @Test
    fun aFileInTheWayOfASubfolderIsLeftAloneAndTheOldFolderStays() {
        val side = MemSide()
            .file("$old/2026-11-01/docs/x.txt", "alt-x").file("$old/2026-11-01/b.txt", "alt-b")
            .file("$new/2026-11-01/docs", "datei-statt-ordner")
        val r = run(side)
        assertEquals(Status.INCOMPLETE, r.status)
        assertEquals(setOf("$old/2026-11-01/docs/x.txt=alt-x", "$new/2026-11-01/b.txt=alt-b", "$new/2026-11-01/docs=datei-statt-ordner"), side.contents())
    }

    @Test
    fun emptySubfoldersOfTheOldFolderAreRemovedWithIt() {
        val side = MemSide().dir("$old/2026-11-01/leer").dir("$new/2026-11-05")
        assertEquals(Status.MIGRATED, run(side).status)
        assertFalse(side.hasDir(old))
    }

    @Test
    fun onlyTheTrashFolderIsTouched() {
        val side = mergeScenario().file("live/a.txt", "x").file("@Other/b.txt", "y").file(".LeoSync-Papierkorb/2026-01-01/p.txt", "p")
        run(side)
        assertTrue(side.contents().containsAll(setOf("live/a.txt=x", "@Other/b.txt=y", ".LeoSync-Papierkorb/2026-01-01/p.txt=p")))
    }

    // ---- (7) PC ----------------------------------------------------------------------------

    @Test
    fun networkErrorOnThePcPropagatesWhenRequestedAndNothingIsLost() {
        val share = pcShare()
        val before = share.allFiles().sorted()
        var moves = 0
        share.onOp = { op, _ -> if (op == "move" && moves++ == 1) throw SocketException("Verbindung weg") }
        try {
            run(PcTrashSide(share), network = true)
            fail("Netzwerkfehler muss nach aussen gehen (Lauf wird unterbrochen)")
        } catch (e: SocketException) { /* erwartet */ }
        // Dateien zählen: jede Version genau einmal, nur der Pfad ändert sich
        assertEquals(before.size, share.allFiles().size)
        share.onOp = null
        assertEquals(Status.MIGRATED, run(PcTrashSide(share), network = true).status)
        assertEquals(before.size, share.allFiles().size)
        assertTrue(share.allFiles().all { it.startsWith("base\\${new.lowercase()}\\") || it.startsWith("base\\live\\") })
        assertFalse(share.dirExists(share.path(old)))
    }

    @Test
    fun networkErrorWithoutPropagationIsAPlainFailure() {
        val share = pcShare()
        share.onOp = { op, _ -> if (op == "move") throw SocketException("Verbindung weg") }
        val r = run(PcTrashSide(share), network = false)
        assertEquals(Status.FAILED, r.status)
        assertTrue(r.error is SocketException)
    }

    @Test
    fun pcOnlyOldNameIsRenamedWithOneDirectoryOperation() {
        val share = FakeShare("Base").add("$old\\2026-11-01\\docs\\a.txt", 10, "a").add("live\\k.txt", 5, "k")
        assertEquals(Status.MIGRATED, run(PcTrashSide(share)).status)
        assertEquals(listOf("renameDir ${share.path(old)} -> ${share.path(new)}"), share.ops)
        assertTrue(share.fileAt(share.path("$new\\2026-11-01\\docs\\a.txt")))
        assertFalse(share.dirExists(share.path(old)))
        assertTrue(share.fileAt(share.path("live\\k.txt")))
    }

    @Test
    fun pcMergeKeepsBothVersionsOfTheSameDay() {
        val share = FakeShare("Base")
            .add("$old\\2026-11-01\\a.txt", 10, "alt").add("$new\\2026-11-01\\a.txt", 20, "neu")
        assertEquals(Status.MIGRATED, run(PcTrashSide(share)).status)
        assertEquals("neu", hashOf(share, "$new\\2026-11-01\\a.txt"))
        assertEquals("alt", hashOf(share, "$new\\2026-11-01\\a (12-00-00).txt"))
        assertFalse(share.dirExists(share.path(old)))
    }

    @Test
    fun pcFolderThatFilledUpMeanwhileIsNotDeleted() {
        val share = FakeShare("Base").add("$old\\2026-11-01\\a.txt", 10, "alt").add("$new\\2026-11-02\\b.txt", 20, "b")
        // Zwischen dem Leer-Test und dem Löschen kommt eine Datei dazu: der Server lehnt das nicht-rekursive Löschen ab
        share.onOp = { op, path ->
            if (op == "deleteEmptyDir" && path.endsWith("2026-11-01")) {
                share.add("$old\\2026-11-01\\neu.txt", 1, "n")
                throw IOException("Ordner nicht leer")
            }
        }
        val r = run(PcTrashSide(share))
        assertEquals(Status.INCOMPLETE, r.status)
        assertNull(r.error)
        assertTrue("Datei, die dazukam, bleibt", share.fileAt(share.path("$old\\2026-11-01\\neu.txt")))
        assertTrue(share.fileAt(share.path("$new\\2026-11-01\\a.txt")))
    }

    @Test
    fun pcMoveNeverOverwritesAnExistingTarget() {
        // Die Fake-Freigabe verhält sich wie SMB: ein vorhandenes Ziel wird nicht überschrieben (Fehler statt Verlust)
        val share = FakeShare("Base").add("$old\\d\\a.txt", 1, "alt").add("$new\\d\\a.txt", 2, "neu")
        try { share.move(share.path("$old\\d\\a.txt"), share.path("$new\\d\\a.txt")); fail() } catch (e: IOException) { /* erwartet */ }
        assertEquals("neu", hashOf(share, "$new\\d\\a.txt"))
        assertEquals("alt", hashOf(share, "$old\\d\\a.txt"))
    }

    // ---- (8) Wann läuft die Übernahme? ------------------------------------------------------------

    @Test
    fun migrationWaitsForPendingRestoresPurgesAndFolderCheck() {
        assertTrue(TrashMigration.mayRun(pendingRestores = 0, purgePending = false, folderCheckPending = false))
        assertFalse("offene Wiederherstellung", TrashMigration.mayRun(1, false, false))
        assertFalse("vorgemerktes Leeren", TrashMigration.mayRun(0, true, false))
        assertFalse("Ordner noch nicht bestätigt", TrashMigration.mayRun(0, false, true))
        assertFalse(TrashMigration.mayRun(3, true, true))
    }

    // ---- (9) Rückfall beim Verschieben in den PC-Papierkorb --------------------------------------------------

    @Test
    fun pcTrashMovesIntoTheNewFolderNormally() {
        val share = FakeShare("Base").add("docs\\a.txt", 10, "a")
        var fallback: Exception? = null
        val mover = PcTrashMover { fallback = it }
        val full = mover.trash(share, "docs/a.txt", "2026-11-11", "12-00-00")
        assertEquals(share.path("$new\\2026-11-11\\docs\\a.txt"), full)
        assertTrue(share.fileAt(full))
        assertFalse(share.fileAt(share.path("docs\\a.txt")))
        assertNull(fallback)
        assertEquals(new, mover.dirName)
    }

    @Test
    fun pcTrashFallsBackToTheDotNameWhenTheNewFolderCannotBeCreated() {
        val share = FakeShare("Base").add("docs\\a.txt", 10, "a").add("docs\\b.txt", 20, "b")
        share.onOp = { op, path -> if (op == "ensureDir" && path.contains(new)) throw IOException("Name ungültig") }
        var fallbacks = 0
        val mover = PcTrashMover(isNameRejection = { true }) { fallbacks++ }
        val f1 = mover.trash(share, "docs/a.txt", "2026-11-11", "12-00-00")
        val f2 = mover.trash(share, "docs/b.txt", "2026-11-11", "12-00-00")
        assertEquals(share.path("$old\\2026-11-11\\docs\\a.txt"), f1)
        assertEquals(share.path("$old\\2026-11-11\\docs\\b.txt"), f2)
        assertTrue(share.fileAt(f1) && share.fileAt(f2))
        assertEquals("Warnung einmal je Rückfall, danach direkt der Ausweichname", 1, fallbacks)
        assertEquals(old, mover.dirName)
    }

    @Test
    fun pcTrashDoesNotFallBackOnNetworkErrorAndLeavesTheFile() {
        val share = FakeShare("Base").add("docs\\a.txt", 10, "a")
        share.onOp = { op, _ -> if (op == "ensureDir") throw SocketException("Verbindung weg") }
        var fallbacks = 0
        val mover = PcTrashMover { fallbacks++ }
        try { mover.trash(share, "docs/a.txt", "2026-11-11", "12-00-00"); fail() } catch (e: SocketException) { /* erwartet */ }
        assertEquals(0, fallbacks)
        assertEquals(new, mover.dirName)
        assertTrue("Datei unberührt", share.fileAt(share.path("docs\\a.txt")))
    }

    @Test
    fun pcTrashFailsWithoutLosingTheFileWhenBothNamesAreRejected() {
        val share = FakeShare("Base").add("docs\\a.txt", 10, "a")
        share.onOp = { op, _ -> if (op == "ensureDir") throw IOException("kein Zugriff") }
        var fallbacks = 0
        val mover = PcTrashMover(isNameRejection = { true }) { fallbacks++ }
        try { mover.trash(share, "docs/a.txt", "2026-11-11", "12-00-00"); fail() } catch (e: IOException) { /* erwartet */ }
        assertEquals("kein Rückfall gemeldet, wenn auch der Ausweichname scheitert", 0, fallbacks)
        assertTrue(share.fileAt(share.path("docs\\a.txt")))
    }

    /** Review -244 M2: nur eine Namensablehnung löst den Rückfall aus, nicht jeder Fehler (Zugriff verweigert, Quota …) */
    @Test
    fun pcTrashDoesNotFallBackOnAnErrorThatIsNotANameRejection() {
        val share = FakeShare("Base").add("docs\\a.txt", 10, "a")
        share.onOp = { op, path -> if (op == "ensureDir" && path.contains(new)) throw IOException("kein Zugriff") }
        var fallbacks = 0
        val mover = PcTrashMover { fallbacks++ }
        try { mover.trash(share, "docs/a.txt", "2026-11-11", "12-00-00"); fail() } catch (e: IOException) { /* erwartet */ }
        assertEquals(0, fallbacks)
        assertEquals("Name bleibt der neue", new, mover.dirName)
        assertTrue(share.fileAt(share.path("docs\\a.txt")))
    }

    @Test
    fun nameRejectionIsRecognisedByTheSmbStatusInTheCauseChain() {
        fun api(status: com.hierynomus.mserref.NtStatus) =
            com.hierynomus.mssmb2.SMBApiException(status.value, com.hierynomus.mssmb2.SMB2MessageCommandCode.SMB2_CREATE, "x", null)
        assertTrue(PcTrashMover.nameRejected(api(com.hierynomus.mserref.NtStatus.STATUS_OBJECT_NAME_INVALID)))
        assertTrue(PcTrashMover.nameRejected(IOException("umhüllt", api(com.hierynomus.mserref.NtStatus.STATUS_OBJECT_NAME_INVALID))))
        assertFalse(PcTrashMover.nameRejected(api(com.hierynomus.mserref.NtStatus.STATUS_ACCESS_DENIED)))
        assertFalse(PcTrashMover.nameRejected(api(com.hierynomus.mserref.NtStatus.STATUS_DISK_FULL)))
        assertFalse(PcTrashMover.nameRejected(IOException("irgendwas")))
    }

    /** Review -244 M1: Merker «PC lehnt den Namen ab» -> gleich der Ausweichname, kein neuer Versuch, keine Warnung */
    @Test
    fun pcTrashStartsWithTheFallbackNameWhenTheRejectionIsRemembered() {
        val share = FakeShare("Base").add("docs\\a.txt", 10, "a")
        var fallbacks = 0
        val mover = PcTrashMover(startWithFallback = true) { fallbacks++ }
        val full = mover.trash(share, "docs/a.txt", "2026-11-11", "12-00-00")
        assertEquals(share.path("$old\\2026-11-11\\docs\\a.txt"), full)
        assertEquals(0, fallbacks)
        assertEquals(old, mover.dirName)
        assertFalse("der neue Ordner wurde nie angelegt", share.dirExists(share.path(new)))
    }

    @Test
    fun useFallbackSwitchesTheNameForTheRestOfTheRun() {
        val share = FakeShare("Base").add("docs\\a.txt", 10, "a")
        val mover = PcTrashMover()
        mover.useFallback()
        assertEquals(share.path("$old\\2026-11-11\\docs\\a.txt"), mover.trash(share, "docs/a.txt", "2026-11-11", "12-00-00"))
    }

    @Test
    fun rejectionMemoryAppliesOnlyToTheSameTargetAndExpires() {
        val t0 = 1_000_000L
        val target = "\\\\pc\\share|sub"
        val stored = com.sync2pc.app.mirror.PcTrashNameMemory.encode(target, t0)
        assertTrue(com.sync2pc.app.mirror.PcTrashNameMemory.active(stored, target, t0 + 1))
        assertFalse("anderes Ziel", com.sync2pc.app.mirror.PcTrashNameMemory.active(stored, "\\\\other\\share|sub", t0 + 1))
        assertFalse("verfallen", com.sync2pc.app.mirror.PcTrashNameMemory.active(stored, target, t0 + com.sync2pc.app.mirror.PcTrashNameMemory.TTL_MS + 1))
        assertFalse("Uhr zurückgestellt", com.sync2pc.app.mirror.PcTrashNameMemory.active(stored, target, t0 - 1))
        assertFalse(com.sync2pc.app.mirror.PcTrashNameMemory.active(null, target, t0))
        assertFalse(com.sync2pc.app.mirror.PcTrashNameMemory.active("kaputt", target, t0))
    }

    /** QA-Check -244 L5: der Merker nach einer fehlgeschlagenen Übernahme nur bei «Name ungültig», nicht bei Kollision/NOT_SUPPORTED */
    @Test
    fun migrationFailureSetsTheRejectionMemoryOnlyForAnInvalidName() {
        fun api(s: com.hierynomus.mserref.NtStatus) =
            com.hierynomus.mssmb2.SMBApiException(s.value, com.hierynomus.mssmb2.SMB2MessageCommandCode.SMB2_CREATE, "x", null)
        assertTrue(PcTrashMover.nameInvalid(api(com.hierynomus.mserref.NtStatus.STATUS_OBJECT_NAME_INVALID)))
        assertFalse(PcTrashMover.nameInvalid(api(com.hierynomus.mserref.NtStatus.STATUS_OBJECT_NAME_COLLISION)))
        assertFalse(PcTrashMover.nameInvalid(api(com.hierynomus.mserref.NtStatus.STATUS_NOT_SUPPORTED)))
        assertFalse(PcTrashMover.nameInvalid(IOException("x")))
    }

    /** QA-Check -244: Ziel des Merkers ist unabhängig von Gross-/Kleinschreibung und Randleerzeichen */
    @Test
    fun rejectionMemoryTargetIgnoresCaseAndEdgeSpaces() {
        val a = com.sync2pc.app.data.Profile().copy(smbPath = " \\\\PC\\Share ", remoteSubfolder = " Sub ")
        val b = com.sync2pc.app.data.Profile().copy(smbPath = "\\\\pc\\share", remoteSubfolder = "sub")
        val c = com.sync2pc.app.data.Profile().copy(smbPath = "\\\\pc\\share", remoteSubfolder = "other")
        assertEquals(com.sync2pc.app.mirror.PcTrashNameMemory.targetOf(a), com.sync2pc.app.mirror.PcTrashNameMemory.targetOf(b))
        assertFalse(com.sync2pc.app.mirror.PcTrashNameMemory.targetOf(a) == com.sync2pc.app.mirror.PcTrashNameMemory.targetOf(c))
    }

    @Test
    fun pcTrashAppendsTimeWhenTheNameIsTakenInTheCurrentFolder() {
        val share = FakeShare("Base").add("docs\\a.txt", 10, "neu").add("$new\\2026-11-11\\docs\\a.txt", 5, "alt")
        val full = PcTrashMover().trash(share, "docs/a.txt", "2026-11-11", "12-00-00")
        assertEquals(share.path("$new\\2026-11-11\\docs\\a (12-00-00).txt"), full)
        assertEquals("alt", hashOf(share, "$new\\2026-11-11\\docs\\a.txt"))
        assertEquals("neu", hashOf(share, "$new\\2026-11-11\\docs\\a (12-00-00).txt"))
    }

    // ---- Hilfen ------------------------------------------------------------------------------------

    private fun hashOf(share: FakeShare, rel: String): String = share.partialHash(share.path(rel), 0)

    private fun pcShare(): FakeShare = FakeShare("Base")
        .add("$old\\2026-11-01\\a.txt", 10, "alt-a").add("$old\\2026-11-02\\b.txt", 11, "alt-b")
        .add("$old\\2026-11-02\\c.txt", 12, "alt-c").add("$new\\2026-11-01\\a.txt", 20, "neu-a")
        .add("live\\k.txt", 5, "k")

    // ---- `.nomedia` (SYNC-20261112-249) ----------------------------------------------------

    @Test
    fun nomediaInOldFolderIsNotMovedAndTheMigrationCompletes() {
        val side = MemSide().file("$old/2026-11-01/a.txt", "alt-a").file("$old/.nomedia", "").file("$new/2026-11-02/b.txt", "neu-b")
            .file("$new/.nomedia", "")
        val r = run(side)
        assertEquals(Status.MIGRATED, r.status)
        assertFalse(side.hasDir(old))
        // keine ".nomedia (12-00-00)" im Ziel, die Ziel-Marker-Datei bleibt, nichts doppelt
        assertEquals(setOf("$new/2026-11-01/a.txt=alt-a", "$new/2026-11-02/b.txt=neu-b", "$new/.nomedia="), side.contents())
        assertEquals(1, r.moved)
    }

    @Test
    fun nomediaInOldFolderIsRemovedEvenWhenTargetHasNone() {
        val side = MemSide().file("$old/2026-11-01/a.txt", "alt-a").file("$old/.nomedia", "").file("$new/2026-11-02/b.txt", "neu-b")
        assertEquals(Status.MIGRATED, run(side).status)
        assertFalse(side.hasDir(old))
        assertEquals(setOf("$new/2026-11-01/a.txt=alt-a", "$new/2026-11-02/b.txt=neu-b"), side.contents())
    }

    @Test
    fun nomediaDeeperInTheTreeIsNormalContent() {
        // nur direkt im Altnamen-Ordner ist sie ein Marker; tiefer liegt sie wie jede Datei (hier: Name belegt -> Uhrzeit-Anhang)
        val side = MemSide().file("$old/2026-11-01/.nomedia", "alt-n").file("$new/2026-11-01/.nomedia", "neu-n")
        assertEquals(Status.MIGRATED, run(side).status)
        assertEquals(2, side.contentValues().size)
    }

    @Test
    fun pcNomediaInOldFolderIsRemovedAndNeverMoved() {
        val share = FakeShare("Base").add("$old\\2026-11-01\\a.txt", 10, "alt").add("$old\\.nomedia", 0, "")
            .add("$new\\2026-11-02\\b.txt", 20, "b")
        assertEquals(Status.MIGRATED, run(PcTrashSide(share)).status)
        assertFalse(share.dirExists(share.path(old)))
        assertTrue(share.fileAt(share.path("$new\\2026-11-01\\a.txt")))
        assertFalse("nie eine .nomedia auf dem PC", share.allFiles().any { it.contains(".nomedia") })
    }

    private class ProcessDeath : Error("simulierter Prozessabbruch")
}

/**
 * [TrashSide] im Speicher, Pfade ohne Gross-/Kleinschreibung. Verhält sich wie SMB/SAF: Ziel muss frei, Zielordner muss
 * vorhanden sein, kein Überschreiben. [hook] wird vor jeder schreibenden Operation mit einem Text wie "moveFile a -> b" gerufen.
 */
private class MemSide : TrashSide {
    private val dirs = LinkedHashMap<String, String>()                 // klein -> Schreibweise
    private val files = LinkedHashMap<String, Pair<String, String>>()  // klein -> (Schreibweise, Inhaltskennung)
    val ops = ArrayList<String>()
    var hook: ((String) -> Unit)? = null
    var canRename = true
    var canMove = true
    /** Wie das Handy: beim Verschieben mit anderem Zielnamen erst im Quellordner umbenennen (zwei Schritte) */
    var renameBeforeMove = false

    private fun k(p: String) = p.lowercase()

    fun dir(rel: String): MemSide { mk(rel); return this }
    fun file(rel: String, content: String): MemSide {
        mk(rel.substringBeforeLast('/', ""))
        files[k(rel)] = rel to content
        return this
    }
    private fun mk(rel: String) {
        var cur = ""
        for (p in rel.split('/').filter { it.isNotEmpty() }) {
            cur = if (cur.isEmpty()) p else "$cur/$p"
            dirs.putIfAbsent(k(cur), cur)
        }
    }

    fun hasDir(rel: String) = k(rel) in dirs
    fun rootDirs(): List<String> = dirs.values.filter { '/' !in it }

    /** "Pfad=Inhalt" aller Dateien in der Schreibweise des Pfads */
    fun contents(): Set<String> = files.values.map { "${it.first}=${it.second}" }.toSet()
    /** Nur die Inhalte (Mehrfachmenge, sortiert): zeigt Verlust und Doppelung */
    fun contentValues(): List<String> = files.values.map { it.second }.sorted()

    override fun list(rel: String): List<TrashNode> {
        if (rel.isNotEmpty() && k(rel) !in dirs) return emptyList()
        val prefix = if (rel.isEmpty()) "" else k(rel) + "/"
        val out = ArrayList<TrashNode>()
        for ((key, full) in dirs) if (key.startsWith(prefix) && key != k(rel) && '/' !in key.substring(prefix.length))
            out += TrashNode(full.substringAfterLast('/'), true)
        for ((key, f) in files) if (key.startsWith(prefix) && '/' !in key.substring(prefix.length))
            out += TrashNode(f.first.substringAfterLast('/'), false)
        return out
    }

    override fun makeDir(rel: String) {
        hook?.invoke("makeDir $rel")
        mk(rel)
        ops += "makeDir $rel"
    }

    override fun renameDir(rel: String, newName: String) {
        hook?.invoke("renameDir $rel -> $newName")
        if (!canRename) throw TrashMigrationUnsupported("rename")
        if (k(rel) !in dirs) throw IOException("Ordner fehlt: $rel")
        val parent = rel.substringBeforeLast('/', "")
        val to = if (parent.isEmpty()) newName else "$parent/$newName"
        if (k(to) in dirs || k(to) in files) throw IOException("Ziel existiert: $to")
        val prefix = k(rel) + "/"
        val moved = dirs.entries.filter { it.key == k(rel) || it.key.startsWith(prefix) }.map { it.key to it.value }
        for ((key, _) in moved) dirs.remove(key)
        for ((_, full) in moved) { val nf = to + full.substring(rel.length); dirs[k(nf)] = nf }
        val movedFiles = files.entries.filter { it.key.startsWith(prefix) }.map { it.key to it.value }
        for ((key, f) in movedFiles) {
            files.remove(key)
            val nf = to + f.first.substring(rel.length)
            files[k(nf)] = nf to f.second
        }
        ops += "renameDir $rel -> $to"
    }

    override fun moveFile(fromRel: String, toRel: String) {
        if (!canMove) throw TrashMigrationUnsupported("move")
        var cur = fromRel
        val f0 = files[k(fromRel)] ?: throw IOException("Quelle fehlt: $fromRel")
        if (renameBeforeMove && fromRel.substringAfterLast('/') != toRel.substringAfterLast('/')) {
            val renamed = fromRel.substringBeforeLast('/') + "/" + toRel.substringAfterLast('/')
            files.remove(k(fromRel)); files[k(renamed)] = renamed to f0.second
            ops += "renameFile $fromRel -> $renamed"
            cur = renamed
        }
        hook?.invoke("moveFile $cur -> $toRel")
        val f = files[k(cur)] ?: throw IOException("Quelle fehlt: $cur")
        if (k(toRel) in files || k(toRel) in dirs) throw IOException("Ziel existiert: $toRel")
        if (k(toRel.substringBeforeLast('/', "")) !in dirs) throw IOException("Zielordner fehlt")
        files.remove(k(cur))
        files[k(toRel)] = toRel to f.second
        ops += "moveFile $cur -> $toRel"
    }

    override fun deleteDirIfEmpty(rel: String): Boolean {
        hook?.invoke("deleteDirIfEmpty $rel")
        if (k(rel) !in dirs) return true
        val prefix = k(rel) + "/"
        if (files.keys.any { it.startsWith(prefix) }) return false
        // leere Unterordner zählen nicht als Inhalt (wie der echte Aufruf nach dem Verschieben: sie wurden schon entfernt)
        if (dirs.keys.any { it.startsWith(prefix) }) return false
        dirs.remove(k(rel))
        ops += "deleteDir $rel"
        return true
    }

    override fun deleteNoMedia(rel: String) {
        val key = k(if (rel.isEmpty()) ".nomedia" else "$rel/.nomedia")
        if (files.remove(key) != null) ops += "deleteNoMedia $rel"
    }
}
