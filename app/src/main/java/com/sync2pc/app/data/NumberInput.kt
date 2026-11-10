package com.sync2pc.app.data

/**
 * Eingabeprüfung der Zahlenfelder im Profilformular (SYNC-20261110-114 P7, -115 P8).
 * Ein leeres oder ausserhalb des Bereichs liegendes Feld ist nie ein Wert: der Wert wird weder still übernommen noch
 * still korrigiert, sondern das Feld meldet einen Fehler und das Speichern wird blockiert.
 */
object NumberInput {
    /** Zulässige Bereiche der Zahlenfelder (Papierkorb-Tage, Prüfintervall in Minuten, Schutzbremse in Prozent) */
    val TRASH_DAYS = 0..3650
    val POLL_MINUTES = 15..(24 * 60)
    val GUARD_PERCENT = 1..100

    /** Nur Ziffern, höchstens [maxDigits] Stellen (Tippfilter des Feldes) */
    fun filter(text: String, maxDigits: Int): String = text.filter { it.isDigit() }.take(maxDigits)

    /** Gültiger Wert von [text] im Bereich [min]..[max], sonst null (leer, ungültig, ausserhalb) */
    fun parse(text: String, min: Int, max: Int): Int? = text.toIntOrNull()?.takeIf { it in min..max }

    fun isValid(text: String, min: Int, max: Int): Boolean = parse(text, min, max) != null

    fun isValid(text: String, range: IntRange): Boolean = isValid(text, range.first, range.last)
}
