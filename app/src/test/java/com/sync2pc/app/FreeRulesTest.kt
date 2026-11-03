package com.sync2pc.app

import com.sync2pc.app.sync.Age
import com.sync2pc.app.sync.AgeUnit
import com.sync2pc.app.sync.FileKind
import com.sync2pc.app.sync.FreeCriteria
import com.sync2pc.app.sync.FreeItem
import com.sync2pc.app.sync.FreeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Speicher freigeben" – Auswahlregeln (0.7.0) */
class FreeRulesTest {
    private val day = 24L * 3600 * 1000
    private val now = 1793707200000L // 3. November 2026
    private val mb = 1024L * 1024

    private fun item(rel: String, ageDays: Int, size: Long = 1000, backedUp: Boolean = true, fav: Boolean = false, group: String = rel) =
        FreeItem(rel, rel.substringBeforeLast('/', ""), rel.substringAfterLast('/'), size, now - ageDays * day, backedUp, fav, group)

    private val none = FreeCriteria(age = null, keepFavorites = false)

    @Test fun age() {
        val c = none.copy(age = Age(30, AgeUnit.DAYS))
        assertTrue(FreeRules.matches(item("a.txt", 31), c, now))
        assertFalse(FreeRules.matches(item("a.txt", 29), c, now))
        assertTrue(FreeRules.matches(item("a.txt", 400), none.copy(age = Age(1, AgeUnit.YEARS)), now))
        assertFalse(FreeRules.matches(item("a.txt", 300), none.copy(age = Age(1, AgeUnit.YEARS)), now))
        assertTrue(FreeRules.matches(item("a.txt", 15), none.copy(age = Age(2, AgeUnit.WEEKS)), now))
        // Ohne Datum nie "alt"
        assertFalse(FreeRules.matches(item("a.txt", 0).copy(time = 0), c, now))
        assertEquals("1 Monat", Age(1, AgeUnit.MONTHS).label)
        assertEquals("6 Monate", Age(6, AgeUnit.MONTHS).label)
    }

    @Test fun foldersIncludeSubfolders() {
        val c = none.copy(folders = setOf("Docs"))
        assertTrue(FreeRules.matches(item("Docs/a.txt", 1), c, now))
        assertTrue(FreeRules.matches(item("docs/2024/a.txt", 1), c, now))
        assertFalse(FreeRules.matches(item("Docsalt/a.txt", 1), c, now))
        assertFalse(FreeRules.matches(item("a.txt", 1), c, now))
        assertEquals(listOf("Docs", "Docs/2024", "Fotos"),
            FreeRules.folders(listOf(item("Docs/2024/a.txt", 1), item("Fotos/b.jpg", 1), item("c.txt", 1))))
    }

    @Test fun typesAndExtensions() {
        assertEquals(FileKind.VIDEO, FileKind.of("Film.MP4"))
        assertEquals(FileKind.OTHER, FileKind.of("README"))
        assertEquals(FileKind.OTHER, FileKind.of(".env"))
        val videos = none.copy(kinds = setOf(FileKind.VIDEO))
        assertTrue(FreeRules.matches(item("Film.mp4", 1), videos, now))
        assertFalse(FreeRules.matches(item("a.pdf", 1), videos, now))
        val iso = none.copy(extensions = FreeRules.parseExtensions("*.ISO, .bak"))
        assertEquals(setOf("iso", "bak"), iso.extensions)
        assertTrue(FreeRules.matches(item("Win.iso", 1), iso, now))
        assertFalse(FreeRules.matches(item("Film.mp4", 1), iso, now))
        // Gruppe + eigene Endung kombinieren (oder)
        assertTrue(FreeRules.matches(item("x.bak", 1), videos.copy(extensions = setOf("bak")), now))
    }

    @Test fun sizeAndExclude() {
        assertFalse(FreeRules.matches(item("a.txt", 1, size = mb - 1), none.copy(minSize = mb), now))
        assertTrue(FreeRules.matches(item("a.txt", 1, size = mb), none.copy(minSize = mb), now))
        val c = none.copy(exclude = listOf("*.kdbx", "Wichtig/**"))
        assertFalse(FreeRules.matches(item("Tresor.kdbx", 1), c, now))
        assertFalse(FreeRules.matches(item("Wichtig/x/a.txt", 1), c, now))
        assertTrue(FreeRules.matches(item("Unwichtig/a.txt", 1), c, now))
    }

    @Test fun selectionKeepsUnsavedFavoritesAndWholeGroups() {
        val all = listOf(
            item("ok.txt", 1),
            item("neu.txt", 1, backedUp = false),
            item("fav.jpg", 1, fav = true),
            // Live-Foto: Video gesichert, Bild nicht -> beide bleiben
            item("live.jpg", 1, backedUp = false, group = "live"),
            item("live.mp4", 1, group = "live"),
        )
        val s = FreeRules.select(all, none.copy(keepFavorites = true), now)
        assertEquals(listOf("ok.txt"), s.candidates.map { it.key })
        assertEquals(2, s.notBackedUp)
        assertEquals(1, s.favoritesKept)
        // Favoriten nicht behalten -> fav.jpg kommt dazu
        assertEquals(setOf("ok.txt", "fav.jpg"), FreeRules.select(all, none, now).candidates.map { it.key }.toSet())
        // Ein Gruppenmitglied abgewählt -> ganze Gruppe bleibt
        val pair = listOf(item("p.jpg", 1, group = "p"), item("p.mp4", 1, group = "p"), item("q.txt", 1))
        assertEquals(listOf("q.txt"), FreeRules.wholeGroups(pair, setOf("p.jpg", "q.txt")).map { it.key })
    }
}
