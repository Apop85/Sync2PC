package com.sync2pc.app

import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashEntry
import com.sync2pc.app.mirror.TrashIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Papierkorb-Einträge zuordnen und auswählen (0.5.0) */
class TrashIndexTest {

    private fun e(path: String, date: String, modified: Long = 0, side: Side = Side.LOCAL) =
        TrashEntry(side, ".LeoSync-Papierkorb/$date/$path", path, date, 10, modified)

    @Test fun entryFromTrashPath() {
        val x = TrashIndex.entry(Side.REMOTE, ".LeoSync-Papierkorb/2026-11-03/Docs/Steuern/a.pdf", 5, 7, setOf("a.pdf"))!!
        assertEquals("Docs/Steuern/a.pdf", x.origPath)
        assertEquals("2026-11-03", x.date)
        assertEquals("Docs/Steuern", x.folder)
        assertEquals("a.pdf", x.name)
        // Datei direkt im Profilordner
        assertEquals("a.txt", TrashIndex.entry(Side.LOCAL, ".LeoSync-Papierkorb/2026-11-03/a.txt", 1, 1, emptySet())!!.origPath)
        // Kein Datumsordner -> kein Eintrag
        assertNull(TrashIndex.entry(Side.LOCAL, ".LeoSync-Papierkorb/irgendwas/a.txt", 1, 1, emptySet()))
        assertNull(TrashIndex.entry(Side.LOCAL, ".LeoSync-Papierkorb/a.txt", 1, 1, emptySet()))
    }

    /** 0.7.0: weitere Versionen am selben Tag tragen die Uhrzeit – eindeutig zuordenbar */
    @Test fun versionNamesWithTime() {
        assertEquals("Foto (14-32-05).jpg", TrashIndex.versionName("Foto.jpg", "14-32-05"))
        assertEquals("Foto (14-32-05-2).jpg", TrashIndex.versionName("Foto.jpg", "14-32-05", 2))
        assertEquals("archiv.tar (14-32-05).gz", TrashIndex.versionName("archiv.tar.gz", "14-32-05"))
        assertEquals("README (14-32-05)", TrashIndex.versionName("README", "14-32-05"))
        assertEquals(".env (14-32-05)", TrashIndex.versionName(".env", "14-32-05"))
        // Zurück zum Originalnamen, auch ohne Geschwister
        for (n in listOf("Foto.jpg", "archiv.tar.gz", "README", ".env", "Foto_2.jpg"))
            for (i in 1..3) assertEquals(n, TrashIndex.originalName(TrashIndex.versionName(n, "09-00-00", i), emptySet()))
        // Altbestand bis 0.6.x ("_2") wird weiterhin nach der bisherigen Regel erkannt
        assertEquals("Foto.jpg", TrashIndex.originalName("Foto_2.jpg", setOf("foto.jpg")))
        // Freier Name: erst Original, dann Uhrzeit, dann Uhrzeit-2
        val taken = mutableSetOf<String>()
        repeat(3) { taken += TrashIndex.uniqueTrashName("a.txt", "10-00-00") { it in taken } }
        assertEquals(setOf("a.txt", "a (10-00-00).txt", "a (10-00-00-2).txt"), taken)
    }

    @Test fun numberedNamesOnlyStrippedWhenBaseExists() {
        assertEquals("a.txt", TrashIndex.originalName("a_2.txt", setOf("a.txt", "a_2.txt")))
        assertEquals("A.txt", TrashIndex.originalName("A_3.txt", setOf("a.txt"))) // Schreibweise bleibt
        // Heisst wirklich so (kein Grundname im selben Ordner)
        assertEquals("bild_2.jpg", TrashIndex.originalName("bild_2.jpg", setOf("bild_2.jpg")))
        // _1 vergibt LeoSync nie
        assertEquals("a_1.txt", TrashIndex.originalName("a_1.txt", setOf("a.txt")))
        // Ohne Endung
        assertEquals("README", TrashIndex.originalName("README_2", setOf("readme")))
    }

    @Test fun latestVersionPerFile() {
        val list = listOf(e("a.txt", "2026-11-01"), e("a.txt", "2026-11-03"), e("b.txt", "2026-11-02"))
        val latest = TrashIndex.latestPerFile(list)
        assertEquals(2, latest.size)
        assertEquals("2026-11-03", latest.first { it.origPath == "a.txt" }.date)
    }

    @Test fun everythingOfOneDay() {
        val list = listOf(
            e("a.txt", "2026-11-03", modified = 1), e("a.txt", "2026-11-03", modified = 5),
            e("b.txt", "2026-11-03"), e("c.txt", "2026-11-01"),
        )
        val day = TrashIndex.ofDate(list, "2026-11-03")
        assertEquals(listOf("a.txt", "b.txt"), day.map { it.origPath })
        assertEquals(5L, day.first().modified) // neueste Version des Tages
    }

    @Test fun wholeFolderIncludesSubfolders() {
        val list = listOf(e("Docs/a.txt", "2026-11-03"), e("Docs/Sub/b.txt", "2026-11-03"), e("Docsalt/c.txt", "2026-11-03"))
        assertEquals(listOf("Docs/a.txt", "Docs/Sub/b.txt"), TrashIndex.inFolder(list, "Docs").map { it.origPath })
    }

    @Test fun onlyOneVersionPerFileSelected() {
        val old = e("a.txt", "2026-11-01")
        val new = e("a.txt", "2026-11-03")
        val sel = TrashIndex.select(listOf(old, e("b.txt", "2026-11-01")), listOf(new))
        assertEquals(2, sel.size)
        assertEquals("2026-11-03", sel.first { it.origPath == "a.txt" }.date)
    }

    @Test fun datesNewestFirst() {
        val list = listOf(e("a", "2026-11-01"), e("b", "2026-11-03"), e("c", "2026-11-01"))
        assertEquals(listOf("2026-11-03", "2026-11-01"), TrashIndex.dates(list))
    }
}
