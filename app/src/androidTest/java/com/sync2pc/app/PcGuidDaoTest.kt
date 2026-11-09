package com.sync2pc.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.Profile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SQL der PC-Kennung gegen eine echte In-Memory-Room-Datenbank (SYNC-20261109-055 T2): Lernen nur bei leerer Kennung bzw. anderem
 * Host (nie still überschreiben), Neuvertrauen nur für denselben Host mit der alten Kennung, Löschen betrifft nur das Profil.
 * Läuft auf Gerät/Emulator (`gradlew connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class PcGuidDaoTest {
    private lateinit var db: AppDatabase
    private val a = "11111111-2222-3333-4444-555555555555"
    private val b = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val host = "192.168.1.20"

    @Before fun open() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun close() { db.close() }

    private fun insert(guid: String = "", guidHost: String = "", mode: String = "GUID"): Long = runBlocking {
        db.dao().insertProfile(Profile(name = "p", pcCheck = mode, pcGuid = guid, pcGuidHost = guidHost))
    }

    private fun get(id: Long): Profile = runBlocking { db.dao().profile(id)!! }

    @Test fun learnFillsEmptyIdOnce_neverOverwritesSameHost() = runBlocking {
        val id = insert()
        assertEquals(1, db.dao().learnPcGuid(id, host, a, "GUID"))
        assertEquals(a to host, get(id).let { it.pcGuid to it.pcGuidHost })
        // andere Kennung für denselben Host: nie still überschreiben
        assertEquals(0, db.dao().learnPcGuid(id, host, b, "GUID"))
        assertEquals(a, get(id).pcGuid)
    }

    @Test fun learnReplacesIdOfAnotherHostOnly() = runBlocking {
        val id = insert(a, "192.168.1.99")
        assertEquals(1, db.dao().learnPcGuid(id, host, b, "GUID"))
        assertEquals(b to host, get(id).let { it.pcGuid to it.pcGuidHost })
    }

    /** L1: Modus in der Datenbank hat inzwischen gewechselt: nicht still lernen */
    @Test fun learnRequiresUnchangedMode() = runBlocking {
        val id = insert(mode = "GUID")
        assertEquals(0, db.dao().learnPcGuid(id, host, a, "WIFI"))
        assertEquals("", get(id).pcGuid)
    }

    @Test fun learnTouchesOnlyTheGivenProfile() = runBlocking {
        val id1 = insert(); val id2 = insert()
        db.dao().learnPcGuid(id1, host, a, "GUID")
        assertEquals("", get(id2).pcGuid)
    }

    @Test fun trustNewGuidOnlyForSameHostAndOldId() = runBlocking {
        val same1 = insert(a, host); val same2 = insert(a, host)
        val otherId = insert(b, host)
        val otherHost = insert(a, "192.168.1.99")
        val empty = insert()
        val n = db.dao().trustNewPcGuid(host, a, "cccccccc-dddd-eeee-ffff-000000000001")
        assertEquals(2, n)
        assertEquals("cccccccc-dddd-eeee-ffff-000000000001", get(same1).pcGuid)
        assertEquals("cccccccc-dddd-eeee-ffff-000000000001", get(same2).pcGuid)
        assertEquals("andere Kennung bleibt", b, get(otherId).pcGuid)
        assertEquals("anderer Host bleibt", a, get(otherHost).pcGuid)
        assertEquals("leer bleibt leer", "", get(empty).pcGuid)
    }

    @Test fun clearOnlyAffectsTheGivenProfile() = runBlocking {
        val id1 = insert(a, host); val id2 = insert(a, host)
        assertEquals(1, db.dao().clearPcGuid(id1))
        assertEquals("" to "", get(id1).let { it.pcGuid to it.pcGuidHost })
        assertEquals(a to host, get(id2).let { it.pcGuid to it.pcGuidHost })
    }
}
