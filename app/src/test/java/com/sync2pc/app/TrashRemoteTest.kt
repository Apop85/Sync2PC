package com.sync2pc.app

import com.sync2pc.app.mirror.MirrorEngine
import com.sync2pc.app.mirror.PurgeRule
import com.sync2pc.app.mirror.RestoreManager
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * PC-Papierkorb auf der Fake-Freigabe (SYNC-20261111-163, Stufe 2, P7): `TrashCleaner.purgeRemote`/`deleteRemoteFile` und
 * `RestoreManager.remoteTrash` über [com.sync2pc.app.smb.RemoteStore]. Nicht abgedeckt: `FolderFreer.verifyAndDelete`
 * (braucht Context, Room und LocalTree) und das Verhalten der echten SMB-Verbindung.
 */
class TrashRemoteTest {

    private val bin = MirrorEngine.TRASH_DIR
    private val legacy = MirrorEngine.LEGACY_TRASH_DIR

    private fun share() = FakeShare("Base")
        .add("$bin\\2026-11-01\\docs\\a.txt", 10, "a")
        .add("$bin\\2026-11-01\\b.txt", 20, "b")
        .add("$bin\\2026-11-02\\c.txt", 30, "c")
        .add("$legacy\\2026-10-01\\old.txt", 40, "o")
        .add("live\\keep.txt", 50, "k") // normale Datei neben dem Papierkorb

    @Test
    fun purgeAllWithoutReservationDeletesBothTrashDirsButNotLiveFiles() {
        val s = share()
        val n = TrashCleaner.purgeRemote(s, PurgeRule(PurgeRule.Mode.ALL))
        assertEquals(2, n) // zwei Papierkorb-Ordner (neu und alt) ganz entfernt
        assertFalse(s.dirExists(s.path(bin)))
        assertFalse(s.dirExists(s.path(legacy)))
        assertTrue(s.fileAt(s.path("live\\keep.txt")))
    }

    @Test
    fun purgeAllKeepsReservedFileAndDeletesTheRestOfThatDay() {
        val s = share()
        val keep = setOf("${bin.lowercase()}/2026-11-01/docs/a.txt")
        TrashCleaner.purgeRemote(s, PurgeRule(PurgeRule.Mode.ALL), keep)
        assertTrue("vorgemerkte Datei muss bleiben", s.fileAt(s.path("$bin\\2026-11-01\\docs\\a.txt")))
        assertFalse(s.fileAt(s.path("$bin\\2026-11-01\\b.txt")))
        assertFalse(s.fileAt(s.path("$bin\\2026-11-02\\c.txt")))
        assertFalse(s.fileAt(s.path("$legacy\\2026-10-01\\old.txt")))
    }

    @Test
    fun purgeByDatesTouchesOnlyThoseDays() {
        val s = share()
        val n = TrashCleaner.purgeRemote(s, PurgeRule(PurgeRule.Mode.DATES, dates = setOf("2026-11-02")))
        assertEquals(1, n)
        assertFalse(s.fileAt(s.path("$bin\\2026-11-02\\c.txt")))
        assertTrue(s.fileAt(s.path("$bin\\2026-11-01\\b.txt")))
        assertTrue(s.fileAt(s.path("$legacy\\2026-10-01\\old.txt")))
    }

    @Test
    fun purgeWithMissingTrashDirDoesNothing() {
        val s = FakeShare("Base").add("live\\x.txt")
        assertEquals(0, TrashCleaner.purgeRemote(s, PurgeRule(PurgeRule.Mode.ALL)))
        assertTrue(s.ops.isEmpty())
    }

    @Test
    fun errorWhileDeletingPropagatesAndLeavesOtherFilesUntouched() {
        val s = share()
        s.onOp = { op, path -> if (op == "deleteDir" && path.endsWith("2026-11-02")) throw java.io.IOException("Verbindung weg") }
        try {
            TrashCleaner.purgeRemote(s, PurgeRule(PurgeRule.Mode.DATES, dates = setOf("2026-11-01", "2026-11-02")))
            fail("Fehler muss nach aussen gehen (Aufrufer merkt vor)")
        } catch (e: java.io.IOException) { /* erwartet */ }
        // Reihenfolge der Ordnerliste: erst 2026-11-01 (gelöscht), dann 2026-11-02 (Fehler): nichts darüber hinaus gelöscht
        assertTrue(s.fileAt(s.path("$bin\\2026-11-02\\c.txt")))
        assertTrue(s.fileAt(s.path("$legacy\\2026-10-01\\old.txt")))
    }

    @Test
    fun deleteRemoteFileReportsWhetherItExisted() {
        val s = share()
        assertTrue(TrashCleaner.deleteRemoteFile(s, "$bin/2026-11-02/c.txt"))
        assertFalse(s.fileAt(s.path("$bin\\2026-11-02\\c.txt")))
        assertFalse("zweites Mal: nichts mehr da", TrashCleaner.deleteRemoteFile(s, "$bin/2026-11-02/c.txt"))
    }

    @Test
    fun remoteTrashListsBothNamesAndIgnoresTempFiles() {
        val s = share().add("$bin\\2026-11-02\\partial.txt.s2ptmp", 5, "t")
        val list = RestoreManager.remoteTrash(s)
        val paths = list.map { it.trashPath }.sorted()
        assertEquals(
            listOf("$bin/2026-11-01/b.txt", "$bin/2026-11-01/docs/a.txt", "$bin/2026-11-02/c.txt", "$legacy/2026-10-01/old.txt").sorted(),
            paths,
        )
        assertTrue(list.all { it.side == Side.REMOTE })
        assertEquals(20L, list.first { it.trashPath.endsWith("b.txt") }.size)
    }

    @Test
    fun remoteTrashOnShareWithoutTrashIsEmpty() {
        assertTrue(RestoreManager.remoteTrash(FakeShare("Base").add("live\\x.txt")).isEmpty())
    }
}
