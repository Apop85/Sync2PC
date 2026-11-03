package com.sync2pc.app.diag

import android.os.Build
import com.sync2pc.app.R
import com.sync2pc.app.l10n.str

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
                    str(R.string.vendor_oplus_1),
                    str(R.string.vendor_oplus_2),
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
                    str(R.string.vendor_samsung_1),
                    str(R.string.vendor_samsung_2),
                ),
                "samsung",
            )
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> Hint(
                "Xiaomi",
                listOf(
                    str(R.string.vendor_xiaomi_1),
                    str(R.string.vendor_xiaomi_2),
                    str(R.string.vendor_xiaomi_3),
                ),
                "xiaomi",
            )
            m.contains("huawei") || m.contains("honor") -> Hint(
                if (m.contains("honor")) "Honor" else "Huawei",
                listOf(
                    str(R.string.vendor_huawei_1),
                ),
                if (m.contains("honor")) "honor" else "huawei",
            )
            else -> null
        }
    }

    /** Ausführliche, gepflegte Anleitungen pro Hersteller */
    fun webUrl(hint: Hint?): String = "https://dontkillmyapp.com/" + (hint?.slug ?: "")
}
