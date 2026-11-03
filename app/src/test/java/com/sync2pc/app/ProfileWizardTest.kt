package com.sync2pc.app

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

    @Test fun saveFromFirstOptionalStep() {
        assertFalse(ProfileWizard.canSaveAt(photos, Step.OPTIONS))
        assertTrue(ProfileWizard.canSaveAt(photos, Step.FILTER))
        assertTrue(ProfileWizard.canSaveAt(mirror, Step.FILTER))
        assertTrue(ProfileWizard.canSaveAt(mirror, Step.GUARD))
    }
}
