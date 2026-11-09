package com.sync2pc.app.smb

import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.SyncDao
import kotlinx.coroutines.runBlocking

/**
 * Zugriff von [SmbSession] auf den aktuellen Stand der PC-Kennung in der Datenbank (SYNC-20261109-047).
 * Die Methoden blockieren und dürfen nur auf Hintergrund-Threads laufen (wie jeder SMB-Zugriff).
 */
interface PcStore {
    /** Aktuelles Profil per ID, null wenn nicht (mehr) gespeichert */
    fun profile(id: Long): Profile?

    /** Lernt die Kennung nur, wenn noch keine gespeichert ist und der Modus in der Datenbank noch [mode] ist; Rückgabe: geänderte Zeilen */
    fun learn(id: Long, host: String, guid: String, mode: String): Int

    /** Neu vertrauen für alle Profile desselben Hosts mit der alten Kennung; Rückgabe: geänderte Zeilen */
    fun trust(host: String, oldGuid: String, newGuid: String): Int
}

/** Room-Umsetzung; wird in [com.sync2pc.app.App] gesetzt */
class RoomPcStore(private val dao: SyncDao) : PcStore {
    override fun profile(id: Long): Profile? = runBlocking { dao.profile(id) }
    override fun learn(id: Long, host: String, guid: String, mode: String): Int = runBlocking { dao.learnPcGuid(id, host, guid, mode) }
    override fun trust(host: String, oldGuid: String, newGuid: String): Int =
        runBlocking { dao.trustNewPcGuid(host, oldGuid, newGuid) }
}
