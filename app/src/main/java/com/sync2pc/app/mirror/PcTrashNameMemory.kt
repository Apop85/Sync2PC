package com.sync2pc.app.mirror

import android.content.Context
import com.sync2pc.app.data.Profile

/**
 * Merkt sich je Profil, dass der PC den Papierkorb-Namen «@LeoSync-Bin» ablehnt (SYNC-20261111-244, Review M1):
 * Sonst scheiterten bei dauerhafter Ablehnung in jedem Lauf zuerst die Übernahme und dann das Anlegen mit je einer Warnung.
 * Mit dem Merker nutzt ein Lauf gleich den Ausweichnamen [MirrorEngine.DOT_TRASH_DIR] und warnt nicht erneut.
 * Der Merker gilt nur für dasselbe PC-Ziel und verfällt nach [TTL_MS]: der Name wird später wieder versucht
 * (Server oder Freigabe können sich ändern). Speicherung in SharedPreferences, kein Schema.
 */
class PcTrashNameMemory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("trashname", Context.MODE_PRIVATE)

    fun isRejected(profile: Profile, now: Long = System.currentTimeMillis()): Boolean =
        active(prefs.getString(key(profile), null), targetOf(profile), now)

    fun remember(profile: Profile, now: Long = System.currentTimeMillis()) {
        prefs.edit().putString(key(profile), encode(targetOf(profile), now)).apply()
    }

    fun forget(profileId: Long) {
        prefs.edit().remove("pc_rejects_$profileId").apply()
    }

    private fun key(p: Profile) = "pc_rejects_${p.id}"

    companion object {
        /** Nach 7 Tagen wird der Name wieder versucht */
        const val TTL_MS = 7L * 24 * 3_600_000

        fun targetOf(p: Profile): String = p.smbPath.trim().lowercase() + "|" + p.remoteSubfolder.trim().lowercase()

        fun encode(target: String, now: Long): String = "$now|$target"

        /** Gilt der gespeicherte Merker [stored] für [target] zur Zeit [now]? Reine Logik (getestet). */
        fun active(stored: String?, target: String, now: Long): Boolean {
            if (stored == null) return false
            val at = stored.substringBefore('|').toLongOrNull() ?: return false
            if (stored.substringAfter('|', "") != target) return false
            return now in at..(at + TTL_MS)
        }
    }
}
