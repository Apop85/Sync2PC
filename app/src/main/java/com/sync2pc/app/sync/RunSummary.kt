package com.sync2pc.app.sync

import com.sync2pc.app.R
import com.sync2pc.app.l10n.Msg

/**
 * Kurztext eines Laufs für Verlauf und Benachrichtigung (0.3.4): nur was passiert ist.
 * Die Zahl der bereits gesicherten (unveränderten) Dateien interessiert hier nicht – sie steht im Detail-Log.
 * Seit 0.12.0 als Kennung ([Msg]) statt fertigem Text: der Verlauf erscheint in der aktiven Sprache, und
 * [com.sync2pc.app.data.RunGrouping.isIdle] prüft die Schlüssel in [NOT_IDLE_KEYS] statt Merkmal-Wörtern (DEC-159).
 */
object RunSummary {
    /**
     * Schlüssel im Kurztext, die einen Lauf "nicht ruhig" machen (Papierkorb, bereits vorhanden, Konflikte,
     * Speicher voll). Namen der Ressourcen, die unten verwendet werden – bei Umbenennung hier mitziehen
     * (Unit-Test prüft die Verwendung).
     */
    val NOT_IDLE_KEYS: Set<String> = setOf("run_trashed", "run_found", "run_conflicts", "run_space_full")

    val NOTHING: Msg get() = Msg.of(R.string.run_nothing)

    /** Fotos-Sync. [found] = von der Duplikatsuche auf dem PC gefundene, der App noch unbekannte Dateien */
    fun photos(uploaded: Int, found: Int, failed: Int, waiting: Int): Msg {
        val parts = buildList {
            if (uploaded > 0) add(Msg.of(R.string.run_uploaded, uploaded))
            if (found > 0) add(Msg.of(R.string.run_found, found))
            if (failed > 0) add(Msg.plural(R.plurals.run_failed, failed))
            if (waiting > 0) add(Msg.plural(R.plurals.run_waiting, waiting))
        }
        return if (parts.isEmpty()) NOTHING else Msg.join(", ", parts)
    }

    /** Ordner-Sync: nur Teile, die nicht 0 sind */
    fun mirror(up: Int, down: Int, trashed: Int, conflicts: Int, failed: Int, spaceShortage: Boolean): Msg {
        val parts = buildList {
            if (up > 0) add(Msg.of(R.string.run_up, up))
            if (down > 0) add(Msg.of(R.string.run_down, down))
            if (trashed > 0) add(Msg.of(R.string.run_trashed, trashed))
            if (conflicts > 0) add(Msg.plural(R.plurals.run_conflicts, conflicts))
            if (failed > 0) add(Msg.plural(R.plurals.run_failed, failed))
            if (spaceShortage) add(Msg.of(R.string.run_space_full))
        }
        return if (parts.isEmpty()) NOTHING else Msg.join(", ", parts)
    }
}
