package com.sync2pc.app

import com.sync2pc.app.data.FolderLayout
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

    private val existing = photos(1, "\\\\192.0.2.3\\Fotos", "DCIM/Camera/")

    @Test fun identicalIsDetectedDespiteSpelling() {
        val p = photos(0, "//192.0.2.3/fotos/", "dcim/camera")
        assertEquals(Kind.IDENTICAL, ProfileOverlap.check(p, listOf(existing))?.kind)
    }

    @Test fun additionalFolderIsPartial() {
        val p = photos(0, "\\\\192.0.2.3\\Fotos", "DCIM/Camera/\nPictures/Screenshots/")
        val hit = ProfileOverlap.check(p, listOf(existing))
        assertEquals(Kind.PARTIAL, hit?.kind)
        assertEquals(listOf("DCIM/Camera/"), hit?.sharedFolders)
    }

    @Test fun parentFolderWithSubfoldersIsPartial() {
        val p = photos(0, "\\\\192.0.2.3\\Fotos", "DCIM/", sub = true)
        assertEquals(Kind.PARTIAL, ProfileOverlap.check(p, listOf(existing))?.kind)
        // ohne Unterordner überschneidet sich DCIM/ nicht mit DCIM/Camera/
        assertNull(ProfileOverlap.check(photos(0, "\\\\192.0.2.3\\Fotos", "DCIM/", sub = false), listOf(existing.copy(includeSubfolders = false))))
    }

    @Test fun otherTargetOrOtherFoldersIsFine() {
        assertNull(ProfileOverlap.check(photos(0, "\\\\192.0.2.3\\Archiv", "DCIM/Camera/"), listOf(existing)))
        assertNull(ProfileOverlap.check(photos(0, "\\\\192.0.2.3\\Fotos", "DCIM/Camera/", remoteSub = "Leo"), listOf(existing)))
        assertNull(ProfileOverlap.check(photos(0, "\\\\192.0.2.3\\Fotos", "Pictures/WhatsApp/"), listOf(existing)))
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
        // Fotos- und Ordner-Profil mit gleichem Ziel: seit SYNC-20261111-152 eine Warnung (nur PARTIAL)
        val cross = ProfileOverlap.check(photos(0, "\\\\pc\\Doku", "DCIM/"), listOf(m1))
        assertEquals(Kind.PARTIAL, cross?.kind)
        assertEquals(ProfileOverlap.Cross.PC_FROM_PHOTOS, cross?.cross)
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

    // ---- Sperrregel gleiches Ziel + andere Ordnerstruktur (SYNC-20261109-083) ----
    private val v2 = FolderLayout.COUNTRY_FOLDER.code

    @Test fun sameTargetOtherLayoutIsBlocked() {
        val p = photos(0, "//192.0.2.3/fotos/", "Pictures/WhatsApp/").copy(folderLayout = v2)
        val hit = ProfileOverlap.check(p, listOf(existing))
        assertEquals(Kind.BLOCKED, hit?.kind)
        assertEquals(1L, hit?.other?.id)
        assertEquals(FolderLayout.MONTH_SUFFIX, hit?.other?.layout())
    }

    @Test fun sameTargetSameSourcesOtherLayoutIsBlockedNotIdentical() {
        val p = photos(0, "\\\\192.0.2.3\\Fotos", "DCIM/Camera/").copy(folderLayout = v2)
        assertEquals(Kind.BLOCKED, ProfileOverlap.check(p, listOf(existing))?.kind)
        // mit gleicher Vorlage bleibt es IDENTICAL
        assertEquals(Kind.IDENTICAL, ProfileOverlap.check(p.copy(folderLayout = FolderLayout.MONTH_SUFFIX.code), listOf(existing))?.kind)
    }

    @Test fun blockedAppliesToDisabledProfilesAndWinsOverIdentical() {
        val disabledOther = existing.copy(id = 2, enabled = false, folderLayout = v2)
        val p = photos(0, "\\\\192.0.2.3\\Fotos", "DCIM/Camera/")
        // Reihenfolge egal: das deaktivierte Profil mit anderer Vorlage blockiert, auch wenn ein identisches vorher kommt
        assertEquals(Kind.BLOCKED, ProfileOverlap.check(p, listOf(existing, disabledOther))?.kind)
        assertEquals(Kind.BLOCKED, ProfileOverlap.check(p, listOf(disabledOther, existing))?.kind)
    }

    @Test fun otherLayoutAtOtherOrNestedTargetIsNotBlocked() {
        val p = photos(0, "\\\\192.0.2.3\\Fotos", "DCIM/Camera/", remoteSub = "Leo").copy(folderLayout = v2)
        assertNull(ProfileOverlap.check(p, listOf(existing)))   // verschachtelt: keine Sperre
        assertNull(ProfileOverlap.check(p.copy(smbPath = "\\\\192.0.2.3\\Archiv", remoteSubfolder = ""), listOf(existing)))
    }

    @Test fun mirrorProfilesAreNeverBlockedByLayout() {
        val m1 = Profile(id = 1, profileType = ProfileType.MIRROR.name, smbPath = "\\\\pc\\Doku", localTreeUri = "content://tree/primary%3ADocuments")
        val ph = photos(0, "\\\\pc\\Doku", "DCIM/").copy(folderLayout = v2)
        assertEquals(Kind.PARTIAL, ProfileOverlap.check(ph, listOf(m1))?.kind) // nie BLOCKED, nur Warnung
    }

    @Test fun editingTheSameProfileIsNotBlockedByItself() {
        assertNull(ProfileOverlap.check(existing.copy(folderLayout = v2), listOf(existing)))
    }

    // ---- Fotos ↔ Ordner typübergreifend und kanonischer Vergleich (SYNC-20261111-152) ----
    private val ext = "content://com.android.externalstorage.documents/tree/"
    private val dl = "content://com.android.providers.downloads.documents/tree/"
    private fun mirror(id: Long, pc: String, tree: String, dir: String = "BOTH") =
        Profile(id = id, name = "M$id", profileType = ProfileType.MIRROR.name, smbPath = pc, localTreeUri = tree, direction = dir)

    @Test fun photosInsideMirrorPcFolderWarnsInBothDirectionsAndAnyDirection() {
        for (dir in listOf("UPLOAD", "DOWNLOAD", "BOTH")) {
            val m = mirror(1, "\\\\pc\\Share", ext + "primary%3ADocuments", dir)
            val inside = photos(0, "\\\\pc\\Share", "DCIM/Camera/", remoteSub = "Fotos")
            assertEquals(ProfileOverlap.Cross.PC_FROM_PHOTOS, ProfileOverlap.check(inside, listOf(m))?.cross)
            // umgekehrt: Fotoprofil existiert, Ordner-Profil wird gespeichert
            val ph = photos(2, "\\\\pc\\Share", "DCIM/Camera/", remoteSub = "Fotos")
            assertEquals(ProfileOverlap.Cross.PC_FROM_MIRROR, ProfileOverlap.check(m.copy(id = 0), listOf(ph))?.cross)
        }
        // anderes Ziel: keine Warnung
        assertNull(ProfileOverlap.check(photos(0, "\\\\pc\\Fotos", "DCIM/Camera/"), listOf(mirror(1, "\\\\pc\\Doku", ext + "primary%3AScans"))))
    }

    @Test fun photoSourceInsideWritingMirrorFolderWarns() {
        val m = mirror(1, "\\\\pc\\Doku", ext + "primary%3ADCIM", "BOTH")
        val ph = photos(0, "\\\\pc\\Fotos", "DCIM/Camera/")
        assertEquals(ProfileOverlap.Cross.PHONE_FROM_PHOTOS, ProfileOverlap.check(ph, listOf(m))?.cross)
        assertEquals(ProfileOverlap.Cross.PHONE_FROM_PHOTOS, ProfileOverlap.check(ph, listOf(m.copy(direction = "DOWNLOAD")))?.cross)
        // Ordner-Profil «Handy → PC» schreibt nicht aufs Handy: keine Meldung
        assertNull(ProfileOverlap.check(ph, listOf(m.copy(direction = "UPLOAD"))))
        // gleicher Ordner, umgekehrte Richtung des Speicherns
        val same = mirror(1, "\\\\pc\\Doku", ext + "primary%3ADCIM%2FCamera")
        assertEquals(ProfileOverlap.Cross.PHONE_FROM_MIRROR, ProfileOverlap.check(same.copy(id = 0), listOf(photos(2, "\\\\pc\\Fotos", "DCIM/Camera/")))?.cross)
        // Handy-Ordner des Ordner-Profils enthält den Foto-Quellordner nur mit Unterordnern wirksam
        val parent = photos(0, "\\\\pc\\Fotos", "DCIM/", sub = true)
        assertEquals(ProfileOverlap.Cross.PHONE_FROM_PHOTOS, ProfileOverlap.check(parent, listOf(same))?.cross)
        assertNull(ProfileOverlap.check(parent.copy(includeSubfolders = false), listOf(same)))
        // anderer Ordner
        assertNull(ProfileOverlap.check(photos(0, "\\\\pc\\Fotos", "Pictures/"), listOf(m)))
        // nicht abbildbare Kennung (msd:) -> keine Handy-Warnung, kein Absturz
        assertNull(ProfileOverlap.check(ph, listOf(mirror(1, "\\\\pc\\Doku", dl + "msd%3A12"))))
    }

    @Test fun canonicalTreeMapsPrimaryAndRaw() {
        assertEquals("primary:documents/scans", ProfileOverlap.canonicalTree(ext + "primary%3ADocuments%2FScans"))
        assertEquals("primary:documents/scans", ProfileOverlap.canonicalTree(ext + "primary%3ADocuments%2FScans%2F/document/primary%3ADocuments"))
        assertEquals("primary:download/testsync", ProfileOverlap.canonicalTree(dl + "raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2FTestsync"))
        assertNull(ProfileOverlap.canonicalTree(dl + "msd%3A12"))
        assertNull(ProfileOverlap.canonicalTree(ext + "1234-ABCD%3ADCIM"))
        assertNull(ProfileOverlap.canonicalTree(""))
    }

    @Test fun sameFolderViaDifferentIdsIsRecognized() {
        val a = mirror(1, "\\\\pc\\Doku", ext + "primary%3ADownload%2FTestsync")
        val b = mirror(0, "\\\\pc\\Doku", dl + "raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2FTestsync")
        assertEquals(Kind.IDENTICAL, ProfileOverlap.check(b, listOf(a))?.kind)
        // Unterordner über raw: im Ordner eines anderen Profils (anderes PC-Ziel): verschachtelt, Handy-Seite
        val sub = mirror(0, "\\\\pc\\Andere", dl + "raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2FTestsync%2Funter")
        val hit = ProfileOverlap.check(sub, listOf(a))
        assertEquals(true, hit?.nested)
        assertEquals(true, hit?.localSide)
        assertEquals(false, hit?.remoteSide)
    }

    @Test fun sameFolderTwoPcTargetsStaysAllowed() {
        // DEC-219: derselbe Handy-Ordner mit verschiedenen PC-Zielen bleibt ohne Meldung, auch über verschiedene Kennungen
        val a = mirror(1, "\\\\pc\\Eins", ext + "primary%3ADownload%2FTestsync")
        val b = mirror(0, "\\\\pc\\Zwei", dl + "raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2FTestsync")
        assertNull(ProfileOverlap.check(b, listOf(a)))
    }

    @Test fun injectedCanonicalizerIsUsed() {
        val a = mirror(1, "\\\\pc\\Doku", dl + "msd%3A12")
        val b = mirror(0, "\\\\pc\\Doku", ext + "primary%3ADownload%2FTestsync")
        // ohne Auflösung: nicht vergleichbar -> keine falsche Warnung
        assertEquals(Kind.PARTIAL, ProfileOverlap.check(b, listOf(a))?.kind) // gleiches PC-Ziel bleibt eine Warnung
        val hit = ProfileOverlap.check(b, listOf(a)) { uri -> if (uri.contains("msd")) "primary:download/testsync" else ProfileOverlap.canonicalTree(uri) }
        assertEquals(Kind.IDENTICAL, hit?.kind)
    }
}
