package com.sync2pc.app.sync

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.sync2pc.app.R
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.mirror.FileFilter
import java.util.Calendar

/**
 * "Speicher freigeben" (0.7.0): Auswahlregeln für Fotos- und Ordner-Profile. Rein, getestet in FreeRulesTest.
 */

/** Dateiart nach Endung */
enum class FileKind(@StringRes private val labelRes: Int, val exts: Set<String>) {
    IMAGE(R.string.kind_images, setOf("jpg", "jpeg", "png", "heic", "heif", "gif", "webp", "bmp", "tif", "tiff", "dng", "raw", "cr2", "cr3", "nef", "arw", "orf", "rw2")),
    VIDEO(R.string.kind_videos, setOf("mp4", "mov", "mkv", "avi", "3gp", "webm", "m4v", "wmv", "mts", "m2ts")),
    AUDIO(R.string.kind_audio, setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr", "wma", "aiff")),
    DOCUMENT(R.string.kind_documents, setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "txt", "rtf", "csv", "md", "epub", "pages", "numbers", "key")),
    ARCHIVE(R.string.kind_archives, setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "apk", "dmg")),
    OTHER(R.string.kind_other, emptySet());

    val label: String get() = str(labelRes)

    companion object {
        fun ext(name: String): String =
            name.substringAfterLast('/').let { n -> if (n.lastIndexOf('.') <= 0) "" else n.substringAfterLast('.').lowercase() }

        fun of(name: String): FileKind {
            val e = ext(name)
            return entries.firstOrNull { e.isNotEmpty() && e in it.exts } ?: OTHER
        }
    }
}

enum class AgeUnit(@StringRes private val manyRes: Int, @PluralsRes val amountRes: Int, val calendarField: Int, val factor: Int) {
    DAYS(R.string.unit_days, R.plurals.age_days, Calendar.DAY_OF_YEAR, 1),
    WEEKS(R.string.unit_weeks, R.plurals.age_weeks, Calendar.DAY_OF_YEAR, 7),
    MONTHS(R.string.unit_months, R.plurals.age_months, Calendar.MONTH, 1),
    YEARS(R.string.unit_years, R.plurals.age_years, Calendar.YEAR, 1);

    /** "Tage", "Wochen" … (Auswahlliste) */
    val many: String get() = str(manyRes)
}

/** "älter als [amount] [unit]" */
data class Age(val amount: Int, val unit: AgeUnit) {
    /** Stichtag: Dateien vor diesem Zeitpunkt sind älter */
    fun cutoff(now: Long): Long =
        Calendar.getInstance().apply { timeInMillis = now; add(unit.calendarField, -amount * unit.factor) }.timeInMillis

    val label: String get() = plural(unit.amountRes, amount)
}

/**
 * Eine Datei, die freigegeben werden könnte.
 * [key] eindeutig je Profil, [folder] relativer Ordner ("" = Hauptordner, Trenner "/"),
 * [time] Aufnahme- bzw. Änderungsdatum (ms), [group] Live-Foto-Paare nie trennen,
 * [backedUp] nachweislich unverändert auf dem PC gesichert (laut Datenbank; geprüft wird vor dem Löschen erneut).
 */
data class FreeItem(
    val key: String,
    val folder: String,
    val name: String,
    val size: Long,
    val time: Long,
    val backedUp: Boolean,
    val favorite: Boolean = false,
    val group: String = key,
) {
    val rel: String get() = if (folder.isEmpty()) name else "$folder/$name"
}

data class FreeCriteria(
    /** null = Alter egal */
    val age: Age? = Age(12, AgeUnit.MONTHS),
    /** leer = alle Ordner; ein Ordner schliesst seine Unterordner ein */
    val folders: Set<String> = emptySet(),
    /** leer (und keine [extensions]) = alle Dateitypen */
    val kinds: Set<FileKind> = emptySet(),
    /** zusätzliche Endungen ohne Punkt, Kleinbuchstaben */
    val extensions: Set<String> = emptySet(),
    /** Mindestgrösse in Bytes, 0 = kein Minimum */
    val minSize: Long = 0,
    // Muster wie beim Filter, z. B. *.kdbx oder Wichtig/ mit zwei Sternen (Zeilenkommentar: Stern-Schrägstrich-Folgen
    // in einem Blockkommentar öffnen in Kotlin einen verschachtelten Kommentar)
    val exclude: List<String> = emptyList(),
    /** nur Fotos-Profile (Android 11+) */
    val keepFavorites: Boolean = true,
)

data class FreeSelection(
    /** freigebbar (gesichert, keine Favoriten, ganze Live-Foto-Gruppen) */
    val candidates: List<FreeItem>,
    /** passen zu den Kriterien, sind aber (noch) nicht gesichert */
    val notBackedUp: Int,
    /** passen, sind aber Favoriten */
    val favoritesKept: Int,
) {
    val bytes: Long get() = candidates.sumOf { it.size }
}

object FreeRules {

    /** Passt die Datei zu den Kriterien? (ohne Sicherungs- und Favoriten-Prüfung) */
    fun matches(i: FreeItem, c: FreeCriteria, now: Long, filter: FileFilter? = excludeFilter(c)): Boolean {
        c.age?.let { a -> if (i.time <= 0 || i.time >= a.cutoff(now)) return false }
        if (c.folders.isNotEmpty() && c.folders.none { inFolder(i.folder, it) }) return false
        if (c.kinds.isNotEmpty() || c.extensions.isNotEmpty()) {
            if (FileKind.of(i.name) !in c.kinds && FileKind.ext(i.name) !in c.extensions) return false
        }
        if (i.size < c.minSize) return false
        if (filter != null && !filter.accepts(i.rel)) return false
        return true
    }

    /** Liegt [folder] in [chosen] oder darunter? */
    fun inFolder(folder: String, chosen: String): Boolean {
        val f = folder.lowercase(); val c = chosen.trim('/').lowercase()
        return c.isEmpty() || f == c || f.startsWith("$c/")
    }

    fun excludeFilter(c: FreeCriteria): FileFilter? =
        if (c.exclude.isEmpty()) null else FileFilter("", c.exclude.joinToString("\n"))

    /**
     * Auswahl nach Kriterien. Eine Live-Foto-Gruppe zählt nur, wenn alle ihre Dateien passen, gesichert und
     * keine Favoriten sind – sonst bleiben alle.
     */
    fun select(all: List<FreeItem>, c: FreeCriteria, now: Long): FreeSelection {
        val filter = excludeFilter(c)
        val matching = all.filter { matches(it, c, now, filter) }
        val fav = matching.filter { c.keepFavorites && it.favorite }
        val notBacked = matching.filter { !it.backedUp && !(c.keepFavorites && it.favorite) }
        val ok = matching.filter { it.backedUp && !(c.keepFavorites && it.favorite) }.map { it.key }.toSet()
        return FreeSelection(wholeGroups(all, ok), notBacked.size, fav.size)
    }

    /** Nur Gruppen, deren Dateien (in [all]) alle in [chosen] liegen */
    fun wholeGroups(all: List<FreeItem>, chosen: Set<String>): List<FreeItem> =
        all.groupBy { it.group }.values.filter { g -> g.all { it.key in chosen } }.flatten()

    /** Alle Ordner (inkl. übergeordneter), sortiert – für die Ordner-Auswahl */
    fun folders(all: List<FreeItem>): List<String> {
        val out = sortedSetOf<String>(String.CASE_INSENSITIVE_ORDER)
        for (i in all) {
            var f = i.folder
            while (f.isNotEmpty()) { out += f; f = f.substringBeforeLast('/', "") }
        }
        return out.toList()
    }

    /** Endungen aus einer Eingabe wie "iso, .bak  tmp" */
    fun parseExtensions(s: String): Set<String> =
        s.split(',', ';', ' ', '\n').map { it.trim().trimStart('*').trimStart('.').lowercase() }.filter { it.isNotEmpty() }.toSet()
}
