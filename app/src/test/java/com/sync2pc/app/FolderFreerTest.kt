package com.sync2pc.app

import com.sync2pc.app.mirror.FolderFreer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Leere Ordner nach "Speicher freigeben" aufräumen (0.11.8, DEC-147) */
class FolderFreerTest {

    @Test fun rootIsAlwaysProtected() {
        assertTrue(FolderFreer.isProtectedDir("Documents/Scans", ""))
    }

    @Test fun dcimCameraIsProtected() {
        // Profil-Ordner ist DCIM: "Camera" darunter bleibt
        assertTrue(FolderFreer.isProtectedDir("DCIM", "Camera"))
        assertTrue(FolderFreer.isProtectedDir("DCIM", "camera"))
        // Profil-Ordner ist der Hauptspeicher oder ein Elternordner
        assertTrue(FolderFreer.isProtectedDir("Main folder", "DCIM/Camera"))
        assertTrue(FolderFreer.isProtectedDir("SD card: DCIM", "Camera"))
        assertTrue(FolderFreer.isProtectedDir("DCIM/Camera", ""))
    }

    @Test fun otherFoldersAreNotProtected() {
        assertFalse(FolderFreer.isProtectedDir("DCIM", "Camera/Alt"))
        assertFalse(FolderFreer.isProtectedDir("DCIM", "Screenshots"))
        assertFalse(FolderFreer.isProtectedDir("Pictures", "Camera"))
        assertFalse(FolderFreer.isProtectedDir("Documents", "MyDCIM/Camera2"))
    }

    @Test fun candidatesDeepestFirstWithoutProtected() {
        val c = FolderFreer.emptyDirCandidates(listOf("a/b/c/x.jpg", "a/y.jpg", "top.txt")) { false }
        assertEquals(listOf("a/b/c", "a/b", "a"), c)
    }

    @Test fun candidatesSkipProtectedButKeepOthers() {
        val prot: (String) -> Boolean = { FolderFreer.isProtectedDir("DCIM", it) }
        val c = FolderFreer.emptyDirCandidates(listOf("Camera/x.jpg", "Videos/2026/v.mp4"), prot)
        assertEquals(listOf("Videos/2026", "Videos"), c)
    }
}
