package com.sync2pc.app.diag

import android.content.Context
import com.sync2pc.app.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hält Abstürze fest: Unbehandelte Ausnahmen werden mit Stacktrace in eine Datei geschrieben
 * (nur lokal, nie übertragen) und erscheinen in der Diagnose. Danach übernimmt der normale
 * Android-Absturz-Handler.
 */
object CrashReporter {
    /** So viele Abstürze werden aufbewahrt (neueste zuerst) */
    const val KEEP = 3
    /** Stacktrace je Absturz kürzen, damit die Diagnose handlich bleibt */
    const val MAX_TRACE_CHARS = 6000
    const val SEPARATOR = "\n=====\n"

    private fun file(context: Context) = File(context.filesDir, "crash/crashes.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                record(app, thread.name, error)
            } catch (ignored: Throwable) {
                // Beim Absturz nie selbst einen weiteren Fehler auslösen
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun record(context: Context, threadName: String, error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY).format(Date())
        val entry = format(time, BuildConfig.VERSION_NAME, threadName, sw.toString())
        val f = file(context)
        f.parentFile?.mkdirs()
        val old = if (f.exists()) f.readText() else ""
        f.writeText(merge(entry, old))
    }

    /** Ein Eintrag: Kopfzeile + gekürzter Stacktrace */
    fun format(time: String, version: String, thread: String, trace: String): String =
        "$time · LeoSync $version · Thread $thread\n" +
            (if (trace.length > MAX_TRACE_CHARS) trace.take(MAX_TRACE_CHARS) + "\n… gekürzt" else trace.trimEnd())

    /** Neuen Eintrag vorne anfügen und nur die letzten [KEEP] behalten */
    fun merge(entry: String, existing: String): String =
        (listOf(entry) + split(existing)).take(KEEP).joinToString(SEPARATOR)

    fun split(content: String): List<String> =
        content.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

    /** Gespeicherte Abstürze, neueste zuerst */
    fun read(context: Context): List<String> {
        val f = file(context)
        return if (f.exists()) split(f.readText()) else emptyList()
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}
