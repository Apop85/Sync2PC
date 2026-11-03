package com.sync2pc.app

import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileOverlap
import com.sync2pc.app.data.ProfileOverlap.Kind
import com.sync2pc.app.data.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileOverlapTest {
    private fun photos(id: Long, path: String, folders: String, sub: Boolean = true, remoteSub: String = "") =
        Profile(id = id, name = "P$id", smbPath = path, sourceFolders = folders, includeSubfolders = sub, remoteSubfolder = remoteSub)

    private val existing = photos(1, "\\\\192.168.3.3\\Fotos", "DCIM/Camera/")

    @Test fun identicalIsDetectedDespiteSpelling() {
        val p = photos(0, "//192.168.3.3/fotos/", "dcim/camera")
        assertEquals(Kind.IDENTICAL, ProfileOverlap.check(p, listOf(existing))?.kind)
    }

    @Test fun additionalFolderIsPartial() {
        val p = photos(0, "\\\\192.168.3.3\\Fotos", "DCIM/Camera/\nPictures/Screenshots/")
        val hit = ProfileOverlap.check(p, listOf(existing))
        assertEquals(Kind.PARTIAL, hit?.kind)
        assertEquals(listOf("DCIM/Camera/"), hit?.sharedFolders)
    }

    @Test fun parentFolderWithSubfoldersIsPartial() {
        val p = photos(0, "\\\\192.168.3.3\\Fotos", "DCIM/", sub = true)
        assertEquals(Kind.PARTIAL, ProfileOverlap.check(p, listOf(existing))?.kind)
        // ohne Unterordner überschneidet sich DCIM/ nicht mit DCIM/Camera/
        assertNull(ProfileOverlap.check(photos(0, "\\\\192.168.3.3\\Fotos", "DCIM/", sub = false), listOf(existing.copy(includeSubfolders = false))))
    }

    @Test fun otherTargetOrOtherFoldersIsFine() {
        assertNull(ProfileOverlap.check(photos(0, "\\\\192.168.3.3\\Archiv", "DCIM/Camera/"), listOf(existing)))
        assertNull(ProfileOverlap.check(photos(0, "\\\\192.168.3.3\\Fotos", "DCIM/Camera/", remoteSub = "Leo"), listOf(existing)))
        assertNull(ProfileOverlap.check(photos(0, "\\\\192.168.3.3\\Fotos", "Pictures/WhatsApp/"), listOf(existing)))
    }

    @Test fun editingTheSameProfileIsNoConflict() {
        assertNull(ProfileOverlap.check(existing.copy(name = "Umbenannt"), listOf(existing)))
    }

    @Test fun mirrorProfilesCompareLocalFolder() {
        val m1 = Profile(id = 1, profileType = ProfileType.MIRROR.name, smbPath = "\\\\pc\\Doku", localTreeUri = "content://tree/primary%3ADocuments")
        val same = m1.copy(id = 0, direction = "DOWNLOAD")
        val other = m1.copy(id = 0, localTreeUri = "content://tree/primary%3ADownload")
        assertEquals(Kind.IDENTICAL, ProfileOverlap.check(same, listOf(m1))?.kind)
        // Seit 0.4.1: gleicher PC-Ordner mit anderem Handy-Ordner -> Warnung
        val sameTarget = ProfileOverlap.check(other, listOf(m1))
        assertEquals(Kind.PARTIAL, sameTarget?.kind)
        assertEquals(true, sameTarget?.nested)
        // Fotos- und Ordner-Profil mit gleichem Ziel stören sich nicht
        assertNull(ProfileOverlap.check(photos(0, "\\\\pc\\Doku", "DCIM/"), listOf(m1)))
    }

    @Test fun nestedMirrorProfilesWarn() {
        val m1 = Profile(id = 1, profileType = ProfileType.MIRROR.name, smbPath = "\\\\pc\\Doku", localTreeUri = "content://tree/primary%3ADocuments")
        // PC-Ordner innerhalb, Handy-Ordner verschieden
        val innerPc = m1.copy(id = 0, remoteSubfolder = "Scans", localTreeUri = "content://tree/primary%3AScans")
        assertEquals(true, ProfileOverlap.check(innerPc, listOf(m1))?.nested)
        // Handy-Ordner innerhalb, PC-Ordner verschieden
        val innerPhone = m1.copy(id = 0, smbPath = "\\\\pc\\Andere", localTreeUri = "content://tree/primary%3ADocuments%2FScans")
        assertEquals(true, ProfileOverlap.check(innerPhone, listOf(m1))?.nested)
        // Ganz getrennt -> keine Warnung
        val separate = m1.copy(id = 0, smbPath = "\\\\pc\\Andere", localTreeUri = "content://tree/primary%3ADownload")
        assertNull(ProfileOverlap.check(separate, listOf(m1)))
        // Ähnlicher Name ist nicht verschachtelt ("Doku" vs. "Dokumente")
        val similar = m1.copy(id = 0, smbPath = "\\\\pc\\Dokumente", localTreeUri = "content://tree/primary%3ADocumentsAlt")
        assertNull(ProfileOverlap.check(similar, listOf(m1)))
    }
}
