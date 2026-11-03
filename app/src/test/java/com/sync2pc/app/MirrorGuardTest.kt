package com.sync2pc.app

import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.mirror.GuardPolicy
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.sync.PartialHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Schutzbremse und gründlicher Inhaltsvergleich des Ordner-Syncs (0.4.1) */
class MirrorGuardTest {

    private fun st(path: String, local: Long = 10, remote: Long = 10) =
        MirrorState(1, path, local, 1000, remote, 2000, 0)

    @Test fun massDeleteAbove20Percent() {
        assertFalse(GuardPolicy.massDelete(20, 100))   // genau 20 % -> noch kein Halt
        assertTrue(GuardPolicy.massDelete(21, 100))
        assertFalse(GuardPolicy.massDelete(0, 100))
        assertFalse(GuardPolicy.massDelete(5, 0))      // Erstabgleich: nichts synchronisiert
    }

    @Test fun smallFoldersOnlyWhenEverythingGoes() {
        // ≤ 5 Dateien: einzelne Löschungen laufen durch, nur "alles weg" hält an (0.4.2)
        assertFalse(GuardPolicy.massDelete(1, 3))
        assertFalse(GuardPolicy.massDelete(4, 5))
        assertTrue(GuardPolicy.massDelete(3, 3))
        assertTrue(GuardPolicy.massDelete(5, 5))
        assertTrue(GuardPolicy.massDelete(1, 1))
        // Ab 6 Dateien gilt wieder die 20-%-Grenze
        assertFalse(GuardPolicy.massDelete(1, 6))
        assertTrue(GuardPolicy.massDelete(2, 6))
    }

    @Test fun from100DeletionsAlwaysAsk() {
        assertFalse(GuardPolicy.massDelete(99, 10_000))
        assertTrue(GuardPolicy.massDelete(100, 10_000))
    }

    @Test fun percentPerProfile() {
        assertFalse(GuardPolicy.massDelete(30, 100, percent = 50))
        assertTrue(GuardPolicy.massDelete(51, 100, percent = 50))
        assertTrue(GuardPolicy.massDelete(6, 100, percent = 5))
        // 100 % = nur noch die festen Regeln (ab 100 Dateien, ganze Unterordner)
        assertFalse(GuardPolicy.massDelete(99, 99, percent = 100))
    }

    @Test fun vanishedSubfolder() {
        val synced = (1..6).map { "steuern 2024/beleg$it.pdf" } + (1..500).map { "andere/f$it.txt" }
        val all = (1..6).map { "Steuern 2024/beleg$it.pdf" }
        assertEquals("Steuern 2024", GuardPolicy.vanishedFolder(all, synced))
        // Nur ein Teil des Ordners gelöscht -> kein Halt
        assertEquals(null, GuardPolicy.vanishedFolder(all.take(5), synced))
        // Ganzer Ordner, aber nur 5 Dateien -> kein Halt
        val small = (1..5).map { "klein/x$it" }
        assertEquals(null, GuardPolicy.vanishedFolder(small, synced + small.map { it.lowercase() }))
        // Verschachtelt: der oberste vollständig verschwundene Ordner wird gemeldet
        val nested = (1..3).map { "a/b/c$it" } + (1..4).map { "a/d$it" }
        assertEquals("a", GuardPolicy.vanishedFolder(nested, nested + synced))
        // Dateien direkt im Profilordner bilden keinen Unterordner
        assertEquals(null, GuardPolicy.vanishedFolder((1..10).map { "f$it" }, (1..10).map { "f$it" }))
    }

    @Test fun sampleIsLimited() {
        val states = (1..100).map { st("f$it") }
        assertEquals(20, GuardPolicy.sample(states, random = Random(1)).size)
        assertEquals(5, GuardPolicy.sample(states.take(5)).size)
    }

    @Test fun topDirsFromKeys() {
        val dirs = GuardPolicy.topDirs(listOf("a.txt", "Fotos/x.jpg", "fotos/y.jpg", "docs/sub/z.pdf"))
        assertEquals(listOf("docs", "fotos"), dirs)
    }

    @Test fun matchesWhenEverythingIsThere() {
        val sample = listOf(st("a.txt"), st("docs/b.txt"))
        val files = mapOf("a.txt" to 10L, "docs/b.txt" to 10L)
        assertTrue(GuardPolicy.matches(sample, listOf("docs"), Side.LOCAL, { files[it] }, { it == "docs" }))
    }

    @Test fun missingFileOrDirIsMismatch() {
        val sample = listOf(st("a.txt"), st("docs/b.txt"))
        val files = mapOf("a.txt" to 10L)
        assertFalse(GuardPolicy.matches(sample, listOf("docs"), Side.REMOTE, { files[it] }, { true }))
        val all = mapOf("a.txt" to 10L, "docs/b.txt" to 10L)
        assertFalse(GuardPolicy.matches(sample, listOf("docs"), Side.REMOTE, { all[it] }, { false }))
    }

    @Test fun someChangedSizesAreTolerated() {
        // 10 Dateien, 2 inzwischen geändert (80 % gleich) -> passt; 3 geändert -> passt nicht
        val sample = (1..10).map { st("f$it", local = 10) }
        val two = sample.associate { it.relPath to if (it.relPath in setOf("f1", "f2")) 99L else 10L }
        assertTrue(GuardPolicy.matches(sample, emptyList(), Side.LOCAL, { two[it] }, { true }))
        val three = sample.associate { it.relPath to if (it.relPath in setOf("f1", "f2", "f3")) 99L else 10L }
        assertFalse(GuardPolicy.matches(sample, emptyList(), Side.LOCAL, { three[it] }, { true }))
    }

    @Test fun usesSizeOfTheCheckedSide() {
        val sample = listOf(st("a", local = 5, remote = 7))
        assertTrue(GuardPolicy.matches(sample, emptyList(), Side.REMOTE, { 7L }, { true }))
        assertFalse(GuardPolicy.matches(sample, emptyList(), Side.LOCAL, { 7L }, { true }))
    }

    @Test fun thoroughHashesSmallFilesCompletely() {
        val size = 3L * 1024 * 1024 + 5
        val regions = PartialHash.thoroughRegions(size)
        assertEquals(0L, regions.first().first)
        assertEquals(size, regions.sumOf { it.second.toLong() })
        // lückenlos
        regions.zipWithNext().forEach { (a, b) -> assertEquals(a.first + a.second, b.first) }
    }

    @Test fun thoroughSamplesLargeFiles() {
        val size = 4L * 1024 * 1024 * 1024
        val regions = PartialHash.thoroughRegions(size)
        assertEquals(PartialHash.SAMPLES, regions.size)
        assertEquals(0L, regions.first().first)
        assertEquals(size - PartialHash.CHUNK, regions.last().first)
        assertTrue(regions.all { it.second == PartialHash.CHUNK })
    }

    @Test fun thoroughDetectsChangeInTheMiddle() {
        // 1 MB grosse Datei, ein Byte in der Mitte geändert: der alte Teil-Hash (3 × 64 KB) sieht das nicht
        val a = ByteArray(1024 * 1024) { (it % 251).toByte() }
        val b = a.copyOf().also { it[300_000] = (it[300_000] + 1).toByte() }
        fun read(x: ByteArray) = { off: Long, len: Int -> x.copyOfRange(off.toInt(), off.toInt() + len) }
        assertEquals(PartialHash.compute(a.size.toLong(), read(a)), PartialHash.compute(b.size.toLong(), read(b)))
        assertFalse(PartialHash.thorough(a.size.toLong(), read(a)) == PartialHash.thorough(b.size.toLong(), read(b)))
    }
}
