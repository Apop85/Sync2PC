package com.sync2pc.app

import com.sync2pc.app.data.FolderLayout
import com.sync2pc.app.data.FolderLayoutGuard
import com.sync2pc.app.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regel hinter SyncDao.updateProfile (SYNC-20261109-083): Ordnerstruktur nach dem ersten Sync nie überschreiben. */
class FolderLayoutGuardTest {
    private val stored = Profile(id = 1, name = "Fotos")
    private fun incoming(code: String) = stored.copy(name = "Umbenannt", folderLayout = code)

    @Test fun lockedProfileKeepsStoredLayoutButOtherFieldsAreSaved() {
        val w = FolderLayoutGuard.apply(incoming("COUNTRY_FOLDER"), stored, locked = true)
        assertEquals("MONTH_SUFFIX", w.folderLayout)
        assertEquals("Umbenannt", w.name)
        assertFalse(FolderLayoutGuard.changed(w, stored))
    }

    @Test fun lockedProfileWithOtherStoredLayoutKeepsIt() {
        val s = stored.copy(folderLayout = "COUNTRY_SUBFOLDER")
        assertEquals("COUNTRY_SUBFOLDER", FolderLayoutGuard.apply(incoming("MONTH_SUFFIX"), s, locked = true).folderLayout)
    }

    @Test fun unlockedProfileMayChangeLayoutAndDropsOpenDecisions() {
        val w = FolderLayoutGuard.apply(incoming("COUNTRY_SUBFOLDER"), stored, locked = false)
        assertEquals(FolderLayout.COUNTRY_SUBFOLDER, w.layout())
        assertTrue(FolderLayoutGuard.changed(w, stored))
    }

    @Test fun unchangedLayoutDoesNotDropDecisions() {
        val inc = incoming("MONTH_SUFFIX")
        val w = FolderLayoutGuard.apply(inc, stored, locked = false)
        assertSame(inc, w)
        assertFalse(FolderLayoutGuard.changed(w, stored))
    }

    @Test fun unknownIncomingCodeIsNormalisedNeverWrittenToDb() {
        assertEquals("MONTH_SUFFIX", FolderLayoutGuard.apply(incoming("XYZ"), stored, locked = false).folderLayout)
    }

    @Test fun enumCodesAreStableAndParseIsStrict() {
        assertEquals(listOf("MONTH_SUFFIX", "COUNTRY_FOLDER", "COUNTRY_SUBFOLDER"), FolderLayout.entries.map { it.code })
        assertEquals(FolderLayout.MONTH_SUFFIX, FolderLayout.of("unbekannt"))
        assertEquals(FolderLayout.MONTH_SUFFIX, FolderLayout.of(null))
        assertNull(FolderLayout.parse("unbekannt"))
        assertEquals(FolderLayout.COUNTRY_FOLDER, FolderLayout.parse("COUNTRY_FOLDER"))
        assertEquals(FolderLayout.MONTH_SUFFIX, Profile().layout())
    }
}