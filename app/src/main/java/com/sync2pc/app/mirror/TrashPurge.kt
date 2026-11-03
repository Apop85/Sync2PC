package com.sync2pc.app.mirror

import android.content.Context

/**
 * "Papierkorb leeren" (0.6.0): Welche Datumsordner endgültig gelöscht werden. Rein bis auf [PurgeStore],
 * getestet in TrashPurgeTest.
 */
data class PurgeRule(
    val mode: Mode,
    /** OLDER: Datumsordner vor diesem Stichtag ("yyyy-MM-dd", siehe TrashPolicy.cutoff) */
    val before: String = "",
    /** DATES: genau diese Tage */
    val dates: Set<String> = emptySet(),
) {
    enum class Mode { ALL, OLDER, DATES }

    /** Gilt die Regel für diesen Datumsordner? */
    fun matches(date: String): Boolean = when (mode) {
        Mode.ALL -> true
        Mode.OLDER -> TrashPolicy.isExpired(date, before)
        Mode.DATES -> date in dates
    }

    /** Für die Vormerkung: "ALL", "OLDER:2026-10-04", "DATES:2026-11-01,2026-11-02" */
    fun encode(): String = when (mode) {
        Mode.ALL -> "ALL"
        Mode.OLDER -> "OLDER:$before"
        Mode.DATES -> "DATES:" + dates.sorted().joinToString(",")
    }

    companion object {
        fun older(days: Int, now: Long = System.currentTimeMillis()) = PurgeRule(Mode.OLDER, before = TrashPolicy.cutoff(days, now))

        fun decode(s: String): PurgeRule? = when {
            s == "ALL" -> PurgeRule(Mode.ALL)
            s.startsWith("OLDER:") -> PurgeRule(Mode.OLDER, before = s.removePrefix("OLDER:")).takeIf { it.before.isNotEmpty() }
            s.startsWith("DATES:") -> s.removePrefix("DATES:").split(',').filter { it.isNotBlank() }.toSet()
                .takeIf { it.isNotEmpty() }?.let { PurgeRule(Mode.DATES, dates = it) }
            else -> null
        }
    }
}

/** Welche Seite geleert wird */
enum class PurgeSides(val label: String, val local: Boolean, val remote: Boolean) {
    BOTH("Handy und PC", true, true),
    LOCAL("Nur Handy", true, false),
    REMOTE("Nur PC", false, true),
}

object TrashPurge {
    /** Einträge, die die Regel auf den gewählten Seiten treffen würde */
    fun affected(entries: List<TrashEntry>, rule: PurgeRule, sides: PurgeSides): List<TrashEntry> =
        entries.filter { (if (it.side == Side.LOCAL) sides.local else sides.remote) && rule.matches(it.date) }

    fun bytes(entries: List<TrashEntry>): Long = entries.sumOf { it.size }
}

/**
 * Vorgemerktes Leeren auf dem PC (PC war nicht erreichbar). Der nächste Ordner-Sync mit PC führt es aus
 * (MirrorEngine.runPendingPurges). Regeln + einzelne Dateien je Profil, in SharedPreferences.
 */
class PurgeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("purge", Context.MODE_PRIVATE)

    fun rules(profileId: Long): List<PurgeRule> =
        (prefs.getStringSet("rules_$profileId", emptySet()) ?: emptySet()).mapNotNull { PurgeRule.decode(it) }

    /** Einzelne Dateien im PC-Papierkorb (Pfade relativ zum Profilordner) */
    fun files(profileId: Long): Set<String> = prefs.getStringSet("files_$profileId", emptySet()) ?: emptySet()

    fun addRule(profileId: Long, rule: PurgeRule) {
        prefs.edit().putStringSet("rules_$profileId", (prefs.getStringSet("rules_$profileId", emptySet()).orEmpty() + rule.encode())).apply()
    }

    fun addFiles(profileId: Long, paths: Collection<String>) {
        if (paths.isEmpty()) return
        prefs.edit().putStringSet("files_$profileId", files(profileId) + paths).apply()
    }

    fun hasPending(profileId: Long): Boolean = rules(profileId).isNotEmpty() || files(profileId).isNotEmpty()

    /** Anzahl Profile mit vorgemerktem Leeren (Hinweis auf der Startseite) */
    fun pendingProfiles(): Set<Long> = prefs.all.keys.filter { it.startsWith("rules_") || it.startsWith("files_") }
        .mapNotNull { it.substringAfter('_', "").toLongOrNull()?.takeIf { id -> hasPending(id) } }.toSet()

    fun clear(profileId: Long) { prefs.edit().remove("rules_$profileId").remove("files_$profileId").apply() }

    // ---- Zuletzt gemessene Grösse (für die Kachel auf der Startseite, ohne jedes Mal den PC abzufragen) ----
    /** [remoteBytes] < 0 = PC war nicht erreichbar, letzter bekannter Wert bleibt */
    fun saveSizes(profileId: Long, localBytes: Long, remoteBytes: Long) {
        val e = prefs.edit().putLong("sizeLocal_$profileId", localBytes)
        if (remoteBytes >= 0) e.putLong("sizeRemote_$profileId", remoteBytes)
        e.apply()
    }

    /** (Handy, PC) in Bytes; -1 = noch nie gemessen */
    fun sizes(profileId: Long): Pair<Long, Long> =
        prefs.getLong("sizeLocal_$profileId", -1) to prefs.getLong("sizeRemote_$profileId", -1)

    fun forget(profileId: Long) {
        prefs.edit().remove("rules_$profileId").remove("files_$profileId")
            .remove("sizeLocal_$profileId").remove("sizeRemote_$profileId").apply()
    }
}
