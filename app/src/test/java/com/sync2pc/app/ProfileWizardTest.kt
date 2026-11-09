package com.sync2pc.app

import com.sync2pc.app.data.FolderLayout
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.ProfileWizard
import com.sync2pc.app.data.ProfileWizard.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Schritte und Pflichtangaben des Profil-Assistenten (0.5.0) */
class ProfileWizardTest {
    private val photos = Profile(name = "Kamera", sourceFolders = "DCIM/Camera/", smbPath = "\\\\pc\\Fotos")
    private val mirror = Profile(name = "Doku", profileType = ProfileType.MIRROR.name,
        localTreeUri = "content://tree/primary%3ADocuments", smbPath = "\\\\pc\\Doku")

    @Test fun stepsPerType() {
        assertEquals(listOf(Step.NAME, Step.SOURCE, Step.TARGET, Step.OPTIONS, Step.FILTER), ProfileWizard.steps(photos))
        assertEquals(Step.GUARD, ProfileWizard.steps(mirror).last()) // Schutzbremse nur bei Ordner-Profilen, ganz am Ende
    }

    @Test fun requiredFields() {
        assertNotNull(ProfileWizard.missing(photos.copy(name = " "), Step.NAME))
        assertNotNull(ProfileWizard.missing(photos.copy(sourceFolders = ""), Step.SOURCE))
        assertNotNull(ProfileWizard.missing(photos.copy(includeImages = false, includeVideos = false), Step.SOURCE))
        assertNotNull(ProfileWizard.missing(mirror.copy(localTreeUri = ""), Step.SOURCE))
        assertNotNull(ProfileWizard.missing(photos.copy(smbPath = "pc"), Step.TARGET))
        // WLAN ist optional (ohne WLAN nur manueller Sync)
        assertNull(ProfileWizard.missing(photos.copy(wifiSsids = ""), Step.OPTIONS))
        assertNull(ProfileWizard.firstIncomplete(photos))
        assertEquals(Step.SOURCE, ProfileWizard.firstIncomplete(photos.copy(sourceFolders = "")))
    }

    @Test fun layoutEditableOnlyForUnlockedIdlePhotos() {
        assertTrue(ProfileWizard.layoutEditable(photos, locked = false, syncRunning = false))
        assertFalse(ProfileWizard.layoutEditable(photos, locked = true, syncRunning = false))
        assertFalse(ProfileWizard.layoutEditable(photos, locked = false, syncRunning = true))
        assertFalse(ProfileWizard.layoutEditable(mirror, locked = false, syncRunning = false))
    }

    @Test fun layoutOwnerIsOtherPhotoProfileWithSameTarget() {
        val v2 = FolderLayout.COUNTRY_FOLDER.code
        val other = photos.copy(id = 7, name = "Andere", smbPath = "\\\\PC\\fotos\\", folderLayout = v2)
        // Gleiches Ziel (Gross-/Kleinschreibung und Trenner egal) -> Vorlage des anderen Profils
        assertEquals(7L, ProfileWizard.layoutOwner(photos.copy(id = 0), listOf(other))?.id)
        // eigenes Profil, anderes Ziel, Ordner-Profil -> kein Eigentümer
        assertNull(ProfileWizard.layoutOwner(other, listOf(other)))
        assertNull(ProfileWizard.layoutOwner(photos.copy(smbPath = "\\\\pc\\Anderes"), listOf(other)))
        assertNull(ProfileWizard.layoutOwner(photos, listOf(mirror.copy(id = 8, smbPath = photos.smbPath))))
        assertNull(ProfileWizard.layoutOwner(mirror, listOf(other)))
        assertNull(ProfileWizard.layoutOwner(photos.copy(smbPath = ""), listOf(other.copy(smbPath = ""))))
    }

    @Test fun effectiveLayoutKeepsOwnChoiceAcrossOwnerChanges() {
        val v1 = FolderLayout.MONTH_SUFFIX; val v2 = FolderLayout.COUNTRY_FOLDER; val v3 = FolderLayout.COUNTRY_SUBFOLDER
        // eigene Wahl V3, Zwischenstand beim Tippen: Eigentümer V2 -> V2 gilt
        assertEquals(v2, ProfileWizard.effectiveLayout(v3, v2, editable = true, current = v3))
        // Ziel ändert sich, Eigentümer entfällt -> eigene Wahl kehrt zurück
        assertEquals(v3, ProfileWizard.effectiveLayout(v3, null, editable = true, current = v2))
        // ohne eigene Wahl: Standard V1
        assertEquals(v1, ProfileWizard.effectiveLayout(null, null, editable = true, current = v2))
        assertEquals(v2, ProfileWizard.effectiveLayout(null, v2, editable = true, current = v1))
        // gesperrt (nach Sync / Lauf): unverändert
        assertEquals(v3, ProfileWizard.effectiveLayout(v1, v2, editable = false, current = v3))
    }
    @Test fun saveFromFirstOptionalStep() {
        assertFalse(ProfileWizard.canSaveAt(photos, Step.OPTIONS))
        assertTrue(ProfileWizard.canSaveAt(photos, Step.FILTER))
        assertTrue(ProfileWizard.canSaveAt(mirror, Step.FILTER))
        assertTrue(ProfileWizard.canSaveAt(mirror, Step.GUARD))
    }
}
