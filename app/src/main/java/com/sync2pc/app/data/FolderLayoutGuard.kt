package com.sync2pc.app.data

/**
 * Regel für die Ordnerstruktur eines Profils (SYNC-20261109-083), als reine Logik ohne Datenbank (per Unit-Test prüfbar).
 * Die DAO ([SyncDao.updateProfile]) wendet sie innerhalb einer Transaktion an.
 */
object FolderLayoutGuard {
    /**
     * Zu schreibende Zeile: Ist die Struktur gesperrt ([locked] = Profil hat Sync-Stand), kommt sie immer aus [stored].
     * Sonst wird der neue Code übernommen, ein unbekannter Code wird auf einen gültigen normalisiert
     * (nie ein ungültiger Wert in der DB).
     */
    fun apply(incoming: Profile, stored: Profile, locked: Boolean): Profile {
        val layout = if (locked) stored.layout() else incoming.layout()
        return if (incoming.folderLayout == layout.code) incoming else incoming.copy(folderLayout = layout.code)
    }

    /** Wechselt die Vorlage gegenüber der gespeicherten Zeile? (dann gehören offene Ordner-Rückfragen zur alten Vorlage) */
    fun changed(toWrite: Profile, stored: Profile): Boolean = toWrite.layout() != stored.layout()
}
