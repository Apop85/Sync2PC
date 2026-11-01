package com.sync2pc.app.diag

import android.os.Build

/**
 * Hersteller mit eigenen Stromspar-Mechanismen, die Apps trotz ausgeschalteter Akku-Optimierung
 * beenden. Liefert die nötigen Schritte für das Gerät. Die Bezeichnungen können je nach Software-Stand abweichen.
 */
object VendorHints {
    data class Hint(val vendor: String, val steps: List<String>, val slug: String)

    fun forDevice(manufacturer: String = Build.MANUFACTURER): Hint? {
        val m = manufacturer.lowercase()
        return when {
            m.contains("oneplus") || m.contains("oppo") || m.contains("realme") -> Hint(
                when {
                    m.contains("oneplus") -> "OnePlus"
                    m.contains("oppo") -> "Oppo"
                    else -> "Realme"
                },
                listOf(
                    "App-Info → Akkunutzung: „Vordergrundaktivität zulassen“, „Hintergrundaktivität zulassen“ und „Automatischer Start“ einschalten",
                    "In der Übersicht der zuletzt genutzten Apps LeoSync sperren (Karte lange drücken → Sperren)",
                ),
                when {
                    m.contains("oneplus") -> "oneplus"
                    m.contains("oppo") -> "oppo"
                    else -> "realme"
                },
            )
            m.contains("samsung") -> Hint(
                "Samsung",
                listOf(
                    "App-Info → Akku: „Nicht eingeschränkt“ wählen",
                    "Einstellungen → Akku → Hintergrundnutzungslimits: LeoSync zu „Nie ruhende Apps“ hinzufügen",
                ),
                "samsung",
            )
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> Hint(
                "Xiaomi",
                listOf(
                    "App-Info → „Autostart“ einschalten",
                    "App-Info → Energiesparmodus: „Keine Einschränkungen“ wählen",
                    "In der Übersicht der zuletzt genutzten Apps LeoSync sperren",
                ),
                "xiaomi",
            )
            m.contains("huawei") || m.contains("honor") -> Hint(
                if (m.contains("honor")) "Honor" else "Huawei",
                listOf(
                    "Einstellungen → Akku → App-Start: LeoSync auf „Manuell verwalten“ stellen und alle drei Schalter einschalten",
                ),
                if (m.contains("honor")) "honor" else "huawei",
            )
            else -> null
        }
    }

    /** Ausführliche, gepflegte Anleitungen pro Hersteller */
    fun webUrl(hint: Hint?): String = "https://dontkillmyapp.com/" + (hint?.slug ?: "")
}
