package com.sync2pc.app.mirror

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Regeln für das automatische Leeren des Papierkorbs (Datumsordner "yyyy-MM-dd"). */
object TrashPolicy {
    private val DATE_FOLDER = Regex("""\d{4}-\d{2}-\d{2}""")

    /** Stichtag: Ordner mit älterem Datum sind abgelaufen */
    fun cutoff(days: Int, now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date(now - days * 24L * 3600 * 1000))

    /** Papierkorb-Unterordner, der vor [cutoff] liegt? Andere Ordnernamen werden nie gelöscht. */
    fun isExpired(name: String, cutoff: String): Boolean =
        DATE_FOLDER.matches(name) && name < cutoff
}
