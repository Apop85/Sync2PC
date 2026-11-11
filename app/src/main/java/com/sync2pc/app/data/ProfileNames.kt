package com.sync2pc.app.data

import java.util.Locale

/**
 * Profilnamen müssen einmalig sein (SYNC-20261111-235, erweitert DEC-173).
 * Gross-/Kleinschreibung zählt nicht («Foto» = «foto»); Leer- und Steuerzeichen am Rand werden entfernt.
 * Reine Logik (getestet in ProfileNamesTest). Eine Datenbank-Einschränkung gibt es bewusst nicht.
 */
object ProfileNames {
    enum class Verdict {
        /** Name ist in Ordnung */
        OK,
        /** Leer (auch nur Leer- oder Steuerzeichen) */
        EMPTY,
        /** Ein anderes Profil heisst gleich: Speichern bzw. «Weiter» ist gesperrt */
        TAKEN,
        /** Ein bestehendes Profil mit bereits doppeltem Namen, Name unverändert: nur melden, Speichern bleibt möglich */
        TAKEN_UNCHANGED,
    }

    /** Leer-, Steuer- und unsichtbare Formatzeichen (z. B. Zero-Width-Space) */
    private fun strip(c: Char): Boolean =
        c.isWhitespace() || c.isISOControl() || Character.getType(c) == Character.FORMAT.toInt()

    /** Name ohne Leer- und Steuerzeichen am Rand (wird so gespeichert) */
    fun normalize(name: String): String = name.trim { strip(it) }

    /** Vergleichsschlüssel: normalisiert, ohne Beachtung der Gross-/Kleinschreibung */
    fun key(name: String): String = normalize(name).lowercase(Locale.ROOT)

    /** Heisst schon ein anderes Profil so? [others] = Namen aller anderen Profile (das bearbeitete ausgenommen). */
    fun isTaken(name: String, others: Collection<String>): Boolean {
        val k = key(name)
        return k.isNotEmpty() && others.any { key(it) == k }
    }

    /**
     * Prüfung eines Namens. [savedName] = bisher gespeicherter Name des bearbeiteten Profils, null = neues Profil.
     * Ein bestehendes Profil mit bereits doppeltem Namen wird bei unverändertem Namen nur gemeldet, nicht blockiert.
     */
    fun verdict(name: String, savedName: String?, others: Collection<String>): Verdict {
        if (key(name).isEmpty()) return Verdict.EMPTY
        if (!isTaken(name, others)) return Verdict.OK
        return if (savedName != null && key(savedName) == key(name)) Verdict.TAKEN_UNCHANGED else Verdict.TAKEN
    }
}
