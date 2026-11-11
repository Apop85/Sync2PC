package com.sync2pc.app

import com.sync2pc.app.data.PendingRestore
import com.sync2pc.app.mirror.FileFilter
import com.sync2pc.app.mirror.MirrorEngine
import com.sync2pc.app.mirror.PurgeRule
import com.sync2pc.app.mirror.RestoreManager
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashCleaner
import com.sync2pc.app.mirror.TrashEntry
import com.sync2pc.app.mirror.TrashIndex
import com.sync2pc.app.mirror.TrashPurge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Papierkorb-Namen (SYNC-20261111-244, Stufe A): "@LeoSync-Bin" ist der aktuelle Name für alle Ordnertypen und Seiten,
 * ".LeoSync-Bin", ".LeoSync-Papierkorb" und ".Sync2PC-Papierkorb" bleiben Altnamen, die weiter erkannt werden (Liste,
 * Wiederherstellen, Leeren, Sync-Ausschluss). Gespeicherte Papierkorb-Pfade (Wiederherstellen-Vormerkung, PurgeStore,
 * Papierkorb-Anzeige) funktionieren mit jedem Namen.
 */
class TrashNamesTest {

    private val all = listOf("@LeoSync-Bin", ".LeoSync-Bin", ".LeoSync-Papierkorb", ".Sync2PC-Papierkorb")

    @Test
    fun currentNameIsTheAtSignAndComesFirst() {
        assertEquals("@LeoSync-Bin", MirrorEngine.TRASH_DIR)
        assertEquals(all, MirrorEngine.TRASH_DIRS)
        assertEquals(".LeoSync-Bin", MirrorEngine.DOT_TRASH_DIR)
    }

    @Test
    fun isTrashDirKnowsEveryNameInAnyCase() {
        for (n in all) {
            assertTrue(n, MirrorEngine.isTrashDir(n))
            assertTrue(n.uppercase(), MirrorEngine.isTrashDir(n.uppercase()))
            assertTrue(n.lowercase(), MirrorEngine.isTrashDir(n.lowercase()))
        }
        assertFalse(MirrorEngine.isTrashDir("LeoSync-Bin")) // ohne Punkt/@ ist es ein gewöhnlicher Ordner
        assertFalse(MirrorEngine.isTrashDir("@LeoSync-Bin2"))
        assertFalse(MirrorEngine.isTrashDir("Bin"))
        assertFalse(MirrorEngine.isTrashDir(""))
    }

    @Test
    fun syncFilterExcludesEveryNameAndEverythingBelowIt() {
        val f = FileFilter("", "")
        for (n in all) {
            assertFalse(n, f.accepts("$n/2026-11-01/x.txt"))
            assertFalse(n.uppercase(), f.accepts("${n.uppercase()}/2026-11-01/docs/x.txt"))
        }
        assertTrue(f.accepts("LeoSync-Bin/x.txt"))
        assertTrue(f.accepts("docs/@LeoSync-Bin-notes.txt"))
    }

    @Test
    fun trashIndexReadsPathsOfEveryName() {
        for (n in all) {
            val e = TrashIndex.entry(Side.LOCAL, "$n/2026-11-03/Docs/a.pdf", 5, 7, emptySet())
            assertNotNull(n, e)
            assertEquals("Docs/a.pdf", e!!.origPath)
            assertEquals("2026-11-03", e.date)
            assertNull(n, TrashIndex.entry(Side.LOCAL, "$n/a.pdf", 1, 1, emptySet())) // ohne Datumsordner
        }
        // Nummerierte Versionen werden auch im neuen Ordner erkannt
        assertEquals("a.txt", TrashIndex.entry(Side.LOCAL, "@LeoSync-Bin/2026-11-03/a (14-32-05).txt", 1, 1, emptySet())!!.origPath)
    }

    @Test
    fun purgeKeysAreCaseInsensitiveAndKeepTheFolderName() {
        val a = TrashEntry(Side.REMOTE, "@LeoSync-Bin/2026-11-03/A.txt", "A.txt", "2026-11-03", 1, 0)
        val b = TrashEntry(Side.REMOTE, ".LeoSync-Bin/2026-11-03/A.txt", "A.txt", "2026-11-03", 1, 0)
        assertEquals("REMOTE|@leosync-bin/2026-11-03/a.txt", TrashPurge.key(a))
        assertEquals("REMOTE|.leosync-bin/2026-11-03/a.txt", TrashPurge.key(b))
        assertTrue("gleiche Datei unter anderem Papierkorb-Namen ist ein anderer Eintrag", TrashPurge.key(a) != TrashPurge.key(b))
        val pending = listOf(
            PendingRestore(1, 1, "REMOTE", "@LeoSync-Bin/2026-11-03/A.txt", "A.txt", 0),
            PendingRestore(2, 1, "LOCAL", ".LeoSync-Bin/2026-11-03/B.txt", "B.txt", 0),
        )
        assertEquals(setOf("@leosync-bin/2026-11-03/a.txt"), TrashPurge.reservedPaths(pending, Side.REMOTE))
        assertEquals(setOf(".leosync-bin/2026-11-03/b.txt"), TrashPurge.reservedPaths(pending, Side.LOCAL))
        assertTrue(TrashPurge.key(a) in TrashPurge.reservedKeys(pending))
        assertFalse("alter Name ist nicht vorgemerkt", TrashPurge.key(b) in TrashPurge.reservedKeys(pending))
    }

    @Test
    fun confirmedPlanWorksPerFolderNameAndDateFolder() {
        fun e(dir: String, name: String) = TrashEntry(Side.LOCAL, "$dir/2026-11-03/$name", name, "2026-11-03", 10, 0)
        val current = listOf(e("@LeoSync-Bin", "a.txt"), e(".LeoSync-Bin", "a.txt"), e(".LeoSync-Bin", "b.txt"))
        val plan = TrashPurge.planConfirmed(current, listOf(e(".LeoSync-Bin", "a.txt"), e(".LeoSync-Bin", "b.txt")), emptySet())
        assertEquals(listOf(".LeoSync-Bin/2026-11-03"), plan.wholeDirs)
        assertTrue("der neue Ordner wird nicht mitgelöscht, er wurde nicht bestätigt", plan.files.isEmpty())
        assertEquals(".LeoSync-Bin/2026-11-03", TrashPurge.dateDir(".LeoSync-Bin/2026-11-03/sub/a.txt"))
        assertEquals("@LeoSync-Bin/2026-11-03", TrashPurge.dateDir("@LeoSync-Bin/2026-11-03/a.txt"))
    }

    // ---- PC-Papierkorb auf der Fake-Freigabe: alle Namen nebeneinander ------------------------------------

    private fun share() = FakeShare("Base")
        .add("@LeoSync-Bin\\2026-11-02\\neu.txt", 10, "n")
        .add(".LeoSync-Bin\\2026-11-01\\alt.txt", 20, "a")
        .add(".LeoSync-Papierkorb\\2026-10-01\\aelter.txt", 30, "o")
        .add(".Sync2PC-Papierkorb\\2026-09-01\\aeltest.txt", 40, "x")
        .add("live\\keep.txt", 50, "k")

    @Test
    fun remoteTrashListsAllFourNames() {
        val paths = RestoreManager.remoteTrash(share()).map { it.trashPath }.sorted()
        assertEquals(
            listOf(
                ".LeoSync-Bin/2026-11-01/alt.txt", ".LeoSync-Papierkorb/2026-10-01/aelter.txt",
                ".Sync2PC-Papierkorb/2026-09-01/aeltest.txt", "@LeoSync-Bin/2026-11-02/neu.txt",
            ).sorted(), paths,
        )
    }

    @Test
    fun purgeAllRemovesAllFourFoldersButNotLiveFiles() {
        val s = share()
        assertEquals(4, TrashCleaner.purgeRemote(s, PurgeRule(PurgeRule.Mode.ALL)))
        for (n in all) assertFalse(n, s.dirExists(s.path(n)))
        assertTrue(s.fileAt(s.path("live\\keep.txt")))
    }

    @Test
    fun purgeKeepsReservedFileOfTheOldDotNameAndRemovesTheRest() {
        val s = share()
        val keep = setOf(".leosync-bin/2026-11-01/alt.txt")
        TrashCleaner.purgeRemote(s, PurgeRule(PurgeRule.Mode.ALL), keep)
        assertTrue(s.fileAt(s.path(".LeoSync-Bin\\2026-11-01\\alt.txt")))
        assertFalse(s.fileAt(s.path("@LeoSync-Bin\\2026-11-02\\neu.txt")))
        assertFalse(s.fileAt(s.path(".LeoSync-Papierkorb\\2026-10-01\\aelter.txt")))
    }

    @Test
    fun deleteRemoteFileFindsPathsOfBothNames() {
        val s = share()
        assertTrue(TrashCleaner.deleteRemoteFile(s, ".LeoSync-Bin/2026-11-01/alt.txt"))
        assertTrue(TrashCleaner.deleteRemoteFile(s, "@LeoSync-Bin/2026-11-02/neu.txt"))
        assertFalse(TrashCleaner.deleteRemoteFile(s, "@LeoSync-Bin/2026-11-02/neu.txt"))
    }
}
