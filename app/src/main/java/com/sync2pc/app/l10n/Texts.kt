package com.sync2pc.app.l10n

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.util.Locale

/**
 * Zugriff auf die App-Texte (0.8.0) – überall nutzbar, auch ausserhalb von Compose (Worker, Engines, reine Logik).
 * Englisch ist die Grundsprache (res/values), Deutsch liegt in res/values-de.
 *
 * In Unit-Tests gibt es kein Android: Ist nichts initialisiert, wird per Reflection `com.sync2pc.app.TestTexts`
 * (src/test) geladen, das die deutschen Texte direkt aus den XML-Dateien liest.
 */
object Texts {
    interface Resolver {
        fun string(id: Int, args: Array<out Any>): String
        fun plural(id: Int, count: Int, args: Array<out Any>): String
        val locale: Locale
    }

    @Volatile
    var resolver: Resolver? = null

    /** Beim App-Start und nach einem Sprachwechsel aufrufen. */
    fun init(ctx: Context) {
        resolver = AndroidResolver(AppLanguage.localized(ctx.applicationContext ?: ctx))
    }

    fun get(): Resolver = resolver ?: testFallback() ?: error("Texts.init wurde nicht aufgerufen")

    /** Sprache der App-Texte (für Datumsformate, Ländernamen) */
    val locale: Locale get() = get().locale

    val isGerman: Boolean get() = locale.language == "de"

    private fun testFallback(): Resolver? = try {
        Class.forName("com.sync2pc.app.TestTexts").getMethod("install").invoke(null)
        resolver
    } catch (ignored: Exception) {
        null
    }
}

private class AndroidResolver(private val ctx: Context) : Texts.Resolver {
    override fun string(id: Int, args: Array<out Any>): String =
        if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)

    override fun plural(id: Int, count: Int, args: Array<out Any>): String =
        ctx.resources.getQuantityString(id, count, *args)

    override val locale: Locale get() = ctx.resources.configuration.locales[0]
}

/** Text aus res/values*.xml, Platzhalter %1$s, %2$s … */
fun str(@StringRes id: Int, vararg args: Any): String = Texts.get().string(id, args)

/** Mehrzahl-Text. Ohne weitere Argumente wird die Anzahl als %1$s eingesetzt. */
fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
    Texts.get().plural(id, count, if (args.isEmpty()) arrayOf<Any>(count) else args)
