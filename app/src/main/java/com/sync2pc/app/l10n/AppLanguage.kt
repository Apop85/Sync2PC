package com.sync2pc.app.l10n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Sprachwahl der App (0.8.0): Systemsprache, Deutsch oder English.
 * Android 13+: über den LocaleManager (auch in den Android-Einstellungen der App sichtbar, siehe locales_config.xml).
 * Android 10–12: eigene Einstellung (Prefs "lang"), angewendet über einen Konfigurations-Kontext.
 * Nicht Teil von Export/Import (gerätebezogen).
 */
object AppLanguage {
    const val SYSTEM = ""
    const val GERMAN = "de"
    const val ENGLISH = "en"

    /** Sprachen der App (auch locales_config.xml und res/values-xx). Neue Sprache: hier und dort ergänzen. */
    val SUPPORTED = setOf(GERMAN, ENGLISH)

    private const val PREFS = "lang"
    private const val KEY = "tag"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Gewählte Sprache: "" = Systemsprache, sonst "de" / "en" */
    fun current(ctx: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val list = ctx.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (list == null || list.isEmpty) SYSTEM else list[0].language
        } else {
            prefs(ctx).getString(KEY, SYSTEM) ?: SYSTEM
        }

    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android baut die Activity selbst neu auf; App.onConfigurationChanged lädt die Texte neu
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            prefs(activity).edit().putString(KEY, tag).apply()
            Texts.init(activity)
            com.sync2pc.app.sync.Notifications.createChannels(activity)
            activity.recreate()
        }
    }

    /** Kontext in der gewählten Sprache (nur Android 10–12 nötig, ab 13 erledigt das Android). */
    fun localized(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY, SYSTEM) ?: SYSTEM
        if (tag.isEmpty()) return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(config)
    }
}
