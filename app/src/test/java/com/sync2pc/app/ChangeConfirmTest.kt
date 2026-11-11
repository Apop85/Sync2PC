package com.sync2pc.app

import com.sync2pc.app.data.ChangeConfirm
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.data.SyncDirection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** V1: Bestätigung nur bei Richtungswechsel oder eingeschalteten Löschungen an bestehendem Ordner-Profil */
class ChangeConfirmTest {
    private fun mirror(dir: SyncDirection = SyncDirection.BOTH, propagate: Boolean = false, id: Long = 5) =
        Profile(id = id, profileType = ProfileType.MIRROR.name, direction = dir.name, propagateDeletes = propagate)

    @Test fun noChangeNoDialog() {
        val b = mirror()
        assertFalse(ChangeConfirm.needed(b, b.copy(name = "anders")))
        assertNull(ChangeConfirm.text(b, b, "@LeoSync-Bin"))
    }

    @Test fun directionChangeAsks() {
        val b = mirror(SyncDirection.BOTH)
        val t = ChangeConfirm.text(b, b.copy(direction = SyncDirection.DOWNLOAD.name), "@LeoSync-Bin")
        assertNotNull(t)
        assertTrue(t!!.contains(SyncDirection.BOTH.label) && t.contains(SyncDirection.DOWNLOAD.label))
        assertTrue(t.contains("@LeoSync-Bin"))
    }

    @Test fun enablingDeletionsAsksDisablingDoesNot() {
        assertTrue(ChangeConfirm.propagateEnabled(mirror(propagate = false), mirror(propagate = true)))
        assertFalse(ChangeConfirm.needed(mirror(propagate = true), mirror(propagate = false)))
    }

    @Test fun bothTriggersGiveTwoParagraphs() {
        val b = mirror(SyncDirection.BOTH, false)
        val t = ChangeConfirm.text(b, b.copy(direction = SyncDirection.UPLOAD.name, propagateDeletes = true), "@LeoSync-Bin")!!
        assertTrue(t.contains("\n\n"))
    }

    @Test fun newProfilePhotosAndMissingBaselineNeverAsk() {
        assertFalse(ChangeConfirm.needed(mirror(id = 0), mirror(SyncDirection.UPLOAD, true, id = 0)))
        assertFalse(ChangeConfirm.needed(null, mirror(SyncDirection.UPLOAD, true)))
        val photoBase = Profile(id = 3, profileType = ProfileType.PHOTOS.name)
        assertFalse(ChangeConfirm.needed(photoBase, photoBase.copy(propagateDeletes = true, direction = SyncDirection.BOTH.name)))
    }
}
