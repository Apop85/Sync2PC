package com.sync2pc.app.mirror

import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.data.SyncDirection
import com.sync2pc.app.smb.RemoteEntry

/** Art der geplanten Aktion je Datei (Ordner-Spiegelung) */
enum class PlanKind { UPLOAD, DOWNLOAD, TRASH_LOCAL, TRASH_REMOTE, RECORD, DROP, CONFLICT, KEEP_BOTH }

/**
 * Entscheidungstabelle des 3-Wege-Abgleichs einer Datei (SYNC-20261111-163, P1/P2): aus [MirrorEngine.plan] unverändert
 * ausgelagert, damit sie ohne SMB und Android prüfbar ist. Der Inhaltsvergleich läuft nur, wo die Engine ihn bisher
 * auch aufrief, und kommt als [sameContent] herein.
 *
 * Grundsatz (Datenverlust): ohne gespeicherten Stand ([MirrorState] = null) wird nie gelöscht, nur übertragen oder gemerkt.
 */
object MirrorPlanner {
    data class Decision(val kind: PlanKind, val trashOther: Boolean = false)

    suspend fun decide(
        l: LocalEntry?, r: RemoteEntry?, st: MirrorState?,
        dir: SyncDirection, propagate: Boolean, conflictNewer: Boolean, keepVersions: Boolean,
        sameContent: suspend () -> Boolean,
    ): Decision? {
        if (l == null && r == null) return if (st != null) Decision(PlanKind.DROP) else null

        if (l != null && r == null) {
            if (st == null) return if (dir.up) Decision(PlanKind.UPLOAD) else null // neu auf dem Handy
            // war synchron, auf dem PC gelöscht
            return when {
                dir == SyncDirection.UPLOAD -> Decision(PlanKind.UPLOAD)            // Ziel wiederherstellen
                propagate -> Decision(PlanKind.TRASH_LOCAL)
                dir == SyncDirection.BOTH -> Decision(PlanKind.UPLOAD)              // nichts verlieren
                else -> Decision(PlanKind.DROP)                                     // PC → Handy: Handy-Kopie behalten
            }
        }
        if (l == null && r != null) {
            if (st == null) return if (dir.down) Decision(PlanKind.DOWNLOAD) else null // neu auf dem PC
            return when {
                dir == SyncDirection.DOWNLOAD -> Decision(PlanKind.DOWNLOAD)
                propagate -> Decision(PlanKind.TRASH_REMOTE)
                dir == SyncDirection.BOTH -> Decision(PlanKind.DOWNLOAD)
                else -> Decision(PlanKind.DROP)
            }
        }
        // beide vorhanden
        l!!; r!!
        val changedL = st == null || l.size != st.localSize || l.modified != st.localModified
        val changedR = st == null || r.size != st.remoteSize || r.modified != st.remoteModified
        if (!changedL && !changedR) return null
        if (st == null || (changedL && changedR)) {
            if (sameContent()) return Decision(PlanKind.RECORD)
            return when (dir) {
                SyncDirection.UPLOAD -> Decision(PlanKind.UPLOAD, trashOther = true)
                SyncDirection.DOWNLOAD -> Decision(PlanKind.DOWNLOAD, trashOther = true)
                SyncDirection.BOTH ->
                    if (conflictNewer) {
                        if (l.modified >= r.modified) Decision(PlanKind.UPLOAD, trashOther = true)
                        else Decision(PlanKind.DOWNLOAD, trashOther = true)
                    } else Decision(PlanKind.CONFLICT)
            }
        }
        // Nur eine Seite "geändert", Grösse aber gleich wie auf der anderen Seite: Ist der Inhalt gleich, hat sich nur
        // der Zeitstempel verschoben (z. B. vom Speicherort nachträglich gesetzt) -> Stand merken statt übertragen (0.5.1)
        // (nur prüfen, wenn sonst wirklich übertragen würde – sonst würde bei Einweg-Profilen jeder Lauf neu hashen)
        val wouldTransfer = if (changedL) dir.up else dir.down
        if (wouldTransfer && l.size == r.size && sameContent()) return Decision(PlanKind.RECORD)
        // Nur eine Seite geändert: Versionsschutz (0.5.0) -> die überschriebene Version kommt in den Papierkorb
        if (changedL) return if (dir.up) Decision(PlanKind.UPLOAD, trashOther = keepVersions) else null
        return if (dir.down) Decision(PlanKind.DOWNLOAD, trashOther = keepVersions) else null
    }
}