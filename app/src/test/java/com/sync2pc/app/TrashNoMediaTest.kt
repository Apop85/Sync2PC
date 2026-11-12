package com.sync2pc.app

import com.sync2pc.app.mirror.MirrorEngine
import com.sync2pc.app.mirror.Side
import com.sync2pc.app.mirror.TrashIndex
import com.sync2pc.app.mirror.TrashNoMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Handy-Papierkorb vor MediaStore und Galerie verstecken (SYNC-20261112-249): der Segment-Filter [MirrorEngine.isInTrash]
 * (Fotoprofil, «Speicher freigeben»), die Marker-Regeln [TrashNoMedia] und `.nomedia` als Nicht-Inhalt ([TrashIndex.entry]).
 *
 * Nicht abgedeckt (nur am Gerät prüfbar, ContentResolver/DocumentsContract): das Anlegen in `LocalTree.ensureTrashNoMedia`
 * und die Abfragen in `MediaScanner.scan` / `SpaceFreer.query`; dort greift der hier getestete Filter je Zeile.
 */
class TrashNoMediaTest {

    @Test
    fun segmentFilterMatchesWholeSegmentsAnywhere() {
        assertTrue(MirrorEngine.isInTrash("@LeoSync-Bin/2026-11-03/"))
        assertTrue("Wurzel des Fotoordners", MirrorEngine.isInTrash("DCIM/@LeoSync-Bin/2026-11-03/Docs/"))
        assertTrue("verschachtelt", MirrorEngine.isInTrash("Pictures/Urlaub/@LeoSync-Bin/"))
        assertTrue("am Ende ohne Schrägstrich", MirrorEngine.isInTrash("DCIM/@LeoSync-Bin"))
    }

    @Test
    fun segmentFilterKnowsAllOldNamesAndIgnoresCase() {
        for (n in MirrorEngine.TRASH_DIRS) {
            assertTrue(n, MirrorEngine.isInTrash("DCIM/$n/2026-01-01/"))
            assertTrue(n, MirrorEngine.isInTrash("DCIM/${n.uppercase()}/"))
            assertTrue(n, MirrorEngine.isInTrash("DCIM/${n.lowercase()}/"))
        }
    }

    @Test
    fun segmentFilterIgnoresPartialNames() {
        assertFalse(MirrorEngine.isInTrash("DCIM/LeoSync-Bin/"))
        assertFalse(MirrorEngine.isInTrash("DCIM/@LeoSync-Bin-notes/"))
        assertFalse(MirrorEngine.isInTrash("DCIM/x@LeoSync-Bin/"))
        assertFalse(MirrorEngine.isInTrash("DCIM/@LeoSync-Bin2/"))
        assertFalse(MirrorEngine.isInTrash("DCIM/Camera/"))
        assertFalse(MirrorEngine.isInTrash(""))
        assertFalse(MirrorEngine.isInTrash("/"))
    }

    @Test
    fun markerIsNotCreatedForDownloadProviderFolders() {
        assertFalse(TrashNoMedia.applies("msd:12"))
        assertFalse(TrashNoMedia.applies("msf:7"))
        assertTrue(TrashNoMedia.applies("primary:Pictures/Test"))
        assertTrue(TrashNoMedia.applies("1234-ABCD:DCIM"))
        assertTrue("raw-Pfade des Download-Anbieters sind Dateiordner", TrashNoMedia.applies("raw:/storage/emulated/0/Download/T"))
    }

    @Test
    fun markerNameIsCaseInsensitive() {
        assertTrue(TrashNoMedia.isMarker(".nomedia"))
        assertTrue(TrashNoMedia.isMarker(".NoMedia"))
        assertFalse(TrashNoMedia.isMarker(".nomedia (1)"))
        assertFalse(TrashNoMedia.isMarker("nomedia"))
    }

    @Test
    fun markerInTrashRootIsNoTrashEntry() {
        assertNull(TrashIndex.entry(Side.LOCAL, "@LeoSync-Bin/.nomedia", 0, 0, setOf(".nomedia")))
        assertNull(TrashIndex.entry(Side.LOCAL, ".LeoSync-Bin/.nomedia", 0, 0, setOf(".nomedia")))
    }

    @Test
    fun realEntriesNextToTheMarkerStillWork() {
        val e = TrashIndex.entry(Side.LOCAL, "@LeoSync-Bin/2026-11-03/Docs/a.txt", 5, 1, setOf("a.txt"))
        assertEquals("Docs/a.txt", e?.origPath)
    }
}
