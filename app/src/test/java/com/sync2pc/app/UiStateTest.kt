package com.sync2pc.app

import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.ProfileType
import com.sync2pc.app.ui.MainActivity
import com.sync2pc.app.ui.ProfileSaver
import com.sync2pc.app.ui.Screen
import com.sync2pc.app.ui.SettingsSection
import com.sync2pc.app.ui.uncNoBreak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SYNC-20261111-218: Zustand überlebt Konfigurationsänderungen, UNC-Pfad ohne Umbruch nach «\\» */
class UiStateTest {

    @Test
    fun profileSaver_rundreiseBehaeltAlleFelder() {
        val p = Profile(
            id = 7, name = "Folder", enabled = false, smbPath = "\\\\192.168.3.3\\Share\\Sub", smbUser = "qa",
            sourceFolders = "DCIM/Camera/", localTreeUri = "content://tree/x", propagateDeletes = true,
            remotePollMinutes = 15, trashRetentionDays = 0, massDeletePercent = 35, verifyLevel = 1,
            profileType = ProfileType.MIRROR.name, direction = "BOTH", pcCheck = "GUID",
        )
        assertEquals(p, ProfileSaver.restore(ProfileSaver.save(p)))
        assertEquals(Profile(), ProfileSaver.restore(ProfileSaver.save(Profile())))
    }

    @Test
    fun profileSaver_defekterZustandErgibtNull() {
        assertNull(ProfileSaver.restore("none"))
        assertNull(ProfileSaver.restore(arrayListOf<Any>(1L, "x")))
        val kaputt = ProfileSaver.save(Profile()).also { it[0] = "keine Zahl" }
        assertNull(ProfileSaver.restore(kaputt))
    }

    @Test
    fun bildschirm_rundreise() {
        val alle = listOf(
            Screen.Home, Screen.Settings(SettingsSection.entries.first()), Screen.ProfileEdit(0), Screen.ProfileEdit(12),
            Screen.Decisions, Screen.Preview(3), Screen.Log, Screen.Stats, Screen.Update, Screen.Conflicts,
            Screen.FreeSpace, Screen.Trash,
        )
        for (s in alle) assertEquals(s, MainActivity.decodeScreen(MainActivity.encodeScreen(s)))
    }

    @Test
    fun bildschirm_unbekanntOderDefektErgibtNull() {
        assertNull(MainActivity.decodeScreen(""))
        assertNull(MainActivity.decodeScreen("profile:abc"))
        assertNull(MainActivity.decodeScreen("settings:GIBTS_NICHT"))
        assertNull(MainActivity.decodeScreen("irgendwas"))
    }

    @Test
    fun uncPfad_keinUmbruchNachPraefix() {
        val s = uncNoBreak("\\\\192.168.3.3\\Share")
        assertEquals("\\⁠\\⁠192.168.3.3\\Share", s)
        assertEquals("\\\\192.168.3.3\\Share", s.replace("⁠", ""))
        // Andere Texte bleiben unverändert
        assertEquals("Download/Testsync", uncNoBreak("Download/Testsync"))
        assertEquals("\\einzeln", uncNoBreak("\\einzeln"))
        assertTrue(uncNoBreak("").isEmpty())
    }
}
