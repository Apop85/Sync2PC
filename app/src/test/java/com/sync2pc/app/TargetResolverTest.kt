package com.sync2pc.app

import com.sync2pc.app.data.FolderLayout
import com.sync2pc.app.data.FolderMapping
import com.sync2pc.app.sync.FolderNames
import com.sync2pc.app.sync.TargetResolver
import com.sync2pc.app.sync.TargetResolver.Resolution
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zielordner je Ordnervorlage mit Fake-Freigabe (SYNC-20261109-084) */
class TargetResolverTest {
    private val names = mapOf("EG" to listOf("Ägypten", "Egypt"), "IT" to listOf("Italien", "Italy"))
    private val allNames = names.values.flatten().toSet()

    private fun resolver(
        share: FakeShare, layout: FolderLayout, mappings: FakeMappings = FakeMappings(),
        offline: Boolean = false, dryRun: Boolean = false,
    ) = TargetResolver(
        { share }, mappings, 1L, allNames, dryRun, offline,
        namesOfCountry = { names[it] ?: emptyList() }, layout = layout,
    )

    private fun TargetResolver.eg(month: Int = 6) = runBlocking { resolve(2026, month, "EG", "Ägypten") }
    private fun TargetResolver.home(month: Int = 6) = runBlocking { resolve(2026, month, null, null) }
    private fun folder(r: Resolution) = (r as Resolution.Folder).name

    // ---- V1: unverändert --------------------------------------------------------------
    @Test fun v1_newFolders() {
        val r = resolver(FakeShare("Fotos"), FolderLayout.MONTH_SUFFIX)
        assertEquals("06 - Ägypten", folder(r.eg()))
        assertEquals("06", folder(r.home()))
    }

    @Test fun v1_existingStandardNameAlsoInOtherLanguage() {
        val share = FakeShare("Fotos").addDir("2026\\06 - Egypt")
        assertEquals("06 - Egypt", folder(resolver(share, FolderLayout.MONTH_SUFFIX).eg()))
    }

    @Test fun v1_renamedFolderAsksOncePerGroup() {
        val share = FakeShare("Fotos").addDir("2026\\06 Urlaub").addDir("2026\\06")
        val res = resolver(share, FolderLayout.MONTH_SUFFIX).eg() as Resolution.Ask
        assertEquals("2026/06/EG", res.groupKey)
        assertEquals("06 - Ägypten", res.defaultName)
        assertEquals(listOf("06 Urlaub"), res.candidates)   // "06" gilt als von der App erzeugt
    }

    @Test fun v1_mappingWithOnePathSegmentStaysValid() {
        val share = FakeShare("Fotos").addDir("2026\\06 Urlaub")
        val maps = FakeMappings()
        maps.map["1|2026/06/EG"] = FolderMapping(1L, "2026/06/EG", "06 Urlaub", seen = false)
        assertEquals("06 Urlaub", folder(resolver(share, FolderLayout.MONTH_SUFFIX, maps).eg()))
        assertTrue(maps.map["1|2026/06/EG"]!!.seen)
    }

    // ---- V2: Land\MM -----------------------------------------------------------------
    @Test fun v2_withCountryNewFolder() {
        assertEquals("Ägypten\\06", folder(resolver(FakeShare("Fotos"), FolderLayout.COUNTRY_FOLDER).eg()))
    }

    @Test fun v2_homeCountryOrNoGpsIsPlainMonth() {
        assertEquals("06", folder(resolver(FakeShare("Fotos"), FolderLayout.COUNTRY_FOLDER).home()))
    }

    @Test fun v2_countryFolderInOtherLanguageIsReused() {
        val share = FakeShare("Fotos").addDir("2026\\Egypt\\06")
        assertEquals("Egypt\\06", folder(resolver(share, FolderLayout.COUNTRY_FOLDER).eg()))
    }

    @Test fun v2_countryFolderCaseInsensitive() {
        val share = FakeShare("Fotos").addDir("2026\\ägypten")
        assertEquals("ägypten\\06", folder(resolver(share, FolderLayout.COUNTRY_FOLDER).eg()))
    }

    @Test fun v2_renamedMonthFolderInCountryFolderAsks() {
        val share = FakeShare("Fotos").addDir("2026\\Ägypten\\06 Hurghada")
        val res = resolver(share, FolderLayout.COUNTRY_FOLDER).eg() as Resolution.Ask
        assertEquals("2026/06/EG", res.groupKey)
        assertEquals("Ägypten\\06", res.defaultName)
        assertEquals(listOf("Ägypten\\06 Hurghada"), res.candidates)
    }

    @Test fun v2_renamedCountryFolderIsNotRecognisedAndNeverAsks() {
        val share = FakeShare("Fotos").addDir("2026\\Mein Aegypten\\06")
        assertEquals("Ägypten\\06", folder(resolver(share, FolderLayout.COUNTRY_FOLDER).eg()))
    }

    @Test fun v2_monthFolderInOtherCountryFolderDoesNotAsk() {
        val share = FakeShare("Fotos").addDir("2026\\Italien\\06 Rom")
        assertEquals("Ägypten\\06", folder(resolver(share, FolderLayout.COUNTRY_FOLDER).eg()))
    }

    @Test fun v2_homePhotosAskForRenamedMonthFolderInYear() {
        val share = FakeShare("Fotos").addDir("2026\\06 Sommer").addDir("2026\\Ägypten\\06")
        val res = resolver(share, FolderLayout.COUNTRY_FOLDER).home() as Resolution.Ask
        assertEquals("2026/06/-", res.groupKey)
        assertEquals(listOf("06 Sommer"), res.candidates)
    }

    @Test fun v2_mappingWithTwoSegments() {
        val share = FakeShare("Fotos").addDir("2026\\Ägypten\\06 Hurghada")
        val maps = FakeMappings()
        maps.map["1|2026/06/EG"] = TargetResolver.mapping(1L, "2026/06/EG", "Ägypten\\06 Hurghada")
        assertEquals("Ägypten\\06 Hurghada", folder(resolver(share, FolderLayout.COUNTRY_FOLDER, maps).eg()))
    }

    // ---- V3: MM\Land -----------------------------------------------------------------
    @Test fun v3_withCountryNewFolder() {
        assertEquals("06\\Ägypten", folder(resolver(FakeShare("Fotos"), FolderLayout.COUNTRY_SUBFOLDER).eg()))
    }

    @Test fun v3_homeCountryDirectlyInMonthFolder() {
        assertEquals("06", folder(resolver(FakeShare("Fotos"), FolderLayout.COUNTRY_SUBFOLDER).home()))
    }

    @Test fun v3_existingSubfolderInOtherLanguage() {
        val share = FakeShare("Fotos").addDir("2026\\06\\Egypt")
        assertEquals("06\\Egypt", folder(resolver(share, FolderLayout.COUNTRY_SUBFOLDER).eg()))
    }

    @Test fun v3_oneGroupPerMonthForAllCountries() {
        val share = FakeShare("Fotos").addDir("2026\\06 Reise")
        val r = resolver(share, FolderLayout.COUNTRY_SUBFOLDER)
        val a = r.eg() as Resolution.Ask
        val b = runBlocking { r.resolve(2026, 6, "IT", "Italien") } as Resolution.Ask
        val c = r.home() as Resolution.Ask
        assertEquals("2026/06/-", a.groupKey)
        assertEquals(a.groupKey, b.groupKey)
        assertEquals(a.groupKey, c.groupKey)
        assertEquals("06", a.defaultName)
        assertEquals(listOf("06 Reise"), a.candidates)
    }

    @Test fun v3_mappingAddsCountrySubfolder() {
        val share = FakeShare("Fotos").addDir("2026\\06 Reise")
        val maps = FakeMappings()
        maps.map["1|2026/06/-"] = TargetResolver.mapping(1L, "2026/06/-", "06 Reise")
        val r = resolver(share, FolderLayout.COUNTRY_SUBFOLDER, maps)
        assertEquals("06 Reise\\Ägypten", folder(r.eg()))
        assertEquals("06 Reise", folder(r.home()))
    }

    // ---- Mapping / Rückfall bei verschwundenem Ordner ---------------------------------
    @Test fun mappingSanitizesEverySegment() {
        assertEquals("Ä_gypten\\06 a_b", TargetResolver.mapping(1, "k", "Ä:gypten/06 a|b").folderName)
        assertEquals("_\\06", TargetResolver.mapping(1, "k", "..\\06").folderName)
        assertEquals("06", TargetResolver.mapping(1, "k", "06").folderName)
    }

    @Test fun vanishedMappedFolderIsDecidedAgain() {
        val share = FakeShare("Fotos")
        val maps = FakeMappings()
        maps.map["1|2026/06/EG"] = FolderMapping(1L, "2026/06/EG", "Ägypten\\06 Reise", seen = true)
        assertEquals("Ägypten\\06", folder(resolver(share, FolderLayout.COUNTRY_FOLDER, maps).eg()))
        assertNull(maps.map["1|2026/06/EG"])
    }

    @Test fun dryRunDoesNotWriteMappings() {
        val share = FakeShare("Fotos").addDir("2026\\06 Urlaub")
        val maps = FakeMappings()
        maps.map["1|2026/06/-"] = FolderMapping(1L, "2026/06/-", "06 Urlaub", seen = false)
        resolver(share, FolderLayout.MONTH_SUFFIX, maps, dryRun = true).home()
        assertFalse(maps.map["1|2026/06/-"]!!.seen)
    }

    // ---- Live-Foto-Video: gleiche Eingaben (Partnerdatum/-land) -> gleicher Pfad -----------
    @Test fun livePhotoVideoLandsInSameFolderInEveryLayout() {
        // SyncEngine.plan: das Video nutzt Datum und Land des Bildes (mediaInfo(partner ?: item)) -> identischer
        // resolve-Aufruf wie für das Bild; der Zielpfad hängt nur von (Jahr, Monat, Land) ab.
        for (layout in FolderLayout.entries) {
            val share = FakeShare("Fotos").addDir("2026\\Egypt\\06").addDir("2026\\06\\Egypt")
            val r = resolver(share, layout)
            val image = folder(runBlocking { r.resolve(2026, 6, "EG", "Ägypten") })
            val video = folder(runBlocking { r.resolve(2026, 6, "EG", "Ägypten") })
            assertEquals(layout.name, image, video)
        }
    }

    // ---- Duplikatsuche: Ordner des Monats (a/b/c) ----------------------------------------
    @Test fun monthFolders_a_b_c() {
        val share = FakeShare("Fotos")
            .addDir("2026\\06")                 // (a)
            .addDir("2026\\06 - Ägypten")       // (a)
            .addDir("2026\\06\\Ägypten")        // (b) V3
            .addDir("2026\\Italien\\06")        // (c) V2
            .addDir("2026\\Italien\\07")        // anderer Monat
            .addDir("2026\\07\\Ägypten")        // anderer Monat
            .addDir("2026\\05 Mai\\06")         // Ordner mit anderer Monatszahl: nicht durchsucht
        val found = resolver(share, FolderLayout.MONTH_SUFFIX).monthFolders(2026, 6).toSet()
        assertEquals(setOf("06", "06 - Ägypten", "06\\Ägypten", "Italien\\06"), found)
    }

    @Test fun monthFolders_sameResultForEveryLayout() {
        val share = FakeShare("Fotos").addDir("2026\\06").addDir("2026\\Italien\\06")
        val expected = resolver(share, FolderLayout.MONTH_SUFFIX).monthFolders(2026, 6).toSet()
        for (l in FolderLayout.entries) assertEquals(expected, resolver(share, l).monthFolders(2026, 6).toSet())
    }

    @Test fun monthFolders_followFolderCreatedAndAreCached() {
        val share = FakeShare("Fotos").addDir("2026\\06")
        val r = resolver(share, FolderLayout.COUNTRY_FOLDER)
        assertEquals(listOf("06"), r.monthFolders(2026, 6))
        r.folderCreated(2026, "Ägypten\\06")
        assertEquals(setOf("06", "Ägypten\\06"), r.monthFolders(2026, 6).toSet())
        r.folderCreated(2026, "06\\Ägypten")
        assertTrue("06\\Ägypten" in r.monthFolders(2026, 6))
    }

    @Test fun offlineListsNothing() {
        val share = FakeShare("Fotos").addDir("2026\\06 Urlaub")
        val r = resolver(share, FolderLayout.MONTH_SUFFIX, offline = true)
        assertEquals("06", folder(r.home()))
        assertTrue(r.monthFolders(2026, 6).isEmpty())
    }

    @Test fun decisionSegmentsPerLayout() {
        assertEquals(1, TargetResolver.decisionSegments(FolderLayout.MONTH_SUFFIX, "2026/06/EG"))
        assertEquals(2, TargetResolver.decisionSegments(FolderLayout.COUNTRY_FOLDER, "2026/06/EG"))
        assertEquals(1, TargetResolver.decisionSegments(FolderLayout.COUNTRY_FOLDER, "2026/06/-"))
        assertEquals(1, TargetResolver.decisionSegments(FolderLayout.COUNTRY_SUBFOLDER, "2026/06/-"))
        assertNotNull(FolderNames.countryOfGroupKey("2026/06/EG"))
    }
}
