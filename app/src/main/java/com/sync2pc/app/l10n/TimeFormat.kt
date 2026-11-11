package com.sync2pc.app.l10n

import android.content.res.Resources
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zentrale Anzeige von Datum und Uhrzeit (SYNC-20261111-233): folgt den Systemeinstellungen des Handys
 * (Region/Sprache: Datumsreihenfolge und Trennzeichen, 12 h/24 h), nicht der App-Sprache.
 *
 * Nur für die Anzeige. Dateinamen, Papierkorb-Tagesordner, Diagnose, Logs und gespeicherte Zeitwerte
 * behalten ihre festen Formate. Ohne Android-Kontext (Unit-Tests) gilt [FALLBACK_DATE]/[FALLBACK_TIME].
 */
object TimeFormat {
    const val FALLBACK_DATE = "yyyy-MM-dd"
    const val FALLBACK_TIME = "HH:mm"
    const val FALLBACK_TIME_SEC = "HH:mm:ss"
    const val FALLBACK_DAY_MONTH = "MM-dd"

    /** Datum + Uhrzeit, z. B. «11.11.2026 14:30» bzw. «11/11/2026 2:30 PM» */
    fun dateTime(ms: Long, seconds: Boolean = false): String =
        format(ms, skeleton(withDate = true, is24h = is24h(), seconds = seconds),
            "$FALLBACK_DATE ${if (seconds) FALLBACK_TIME_SEC else FALLBACK_TIME}")

    /** Nur Datum mit Jahr */
    fun date(ms: Long): String = format(ms, "yMd", FALLBACK_DATE)

    /** Nur Tag und Monat, z. B. «11.11.» */
    fun dayMonth(ms: Long): String = format(ms, "Md", FALLBACK_DAY_MONTH)

    /** Nur Uhrzeit */
    fun clock(ms: Long, seconds: Boolean = false): String =
        format(ms, skeleton(withDate = false, is24h = is24h(), seconds = seconds),
            if (seconds) FALLBACK_TIME_SEC else FALLBACK_TIME)

    /** Skelett für [DateFormat.getBestDateTimePattern]; reine Logik, ohne Android testbar. */
    internal fun skeleton(withDate: Boolean, is24h: Boolean, seconds: Boolean): String =
        (if (withDate) "yMd" else "") + (if (is24h) "H" else "h") + "m" + (if (seconds) "s" else "")

    private fun is24h(): Boolean {
        val ctx = Texts.appContext ?: return true
        return DateFormat.is24HourFormat(ctx)
    }

    private fun systemLocale(): Locale = try {
        Resources.getSystem().configuration.locales[0]
    } catch (e: Exception) {
        Locale.getDefault()
    }

    private fun format(ms: Long, skeleton: String, fallback: String): String {
        if (Texts.appContext == null) return SimpleDateFormat(fallback, Locale.ROOT).format(Date(ms))
        val locale = systemLocale()
        return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(ms))
    }
}
