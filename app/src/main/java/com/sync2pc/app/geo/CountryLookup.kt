package com.sync2pc.app.geo

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import com.sync2pc.app.l10n.AppLanguage
import com.sync2pc.app.l10n.Texts
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Ein Land mit seinen Namen aus Natural Earth (0.8.0): Sprache ("de", "en", "fr" …) -> Name.
 * [nameDe] bleibt als Kennzeichen im Analyse-Cache (media_info.countryName != null = Land bestimmt).
 */
data class Country(val iso: String, val names: Map<String, String>) {
    val nameDe: String get() = names["de"] ?: names["en"] ?: names.values.first()

    /** Name in der App-Sprache (Rückfall Englisch) */
    fun name(lang: String = Texts.locale.language): String = names[lang] ?: names["en"] ?: nameDe
}

/**
 * Offline-Ländererkennung aus GPS-Koordinaten.
 * Datenquelle: Natural Earth "Admin 0 – Countries" 1:50m (Public Domain),
 * Datei assets/geo/countries.geojson (wird von tools/fetch-geodata.ps1 geladen).
 *
 * Ablauf: Bounding-Box-Vorfilter -> Punkt-in-Polygon (Even-Odd, inkl. Löcher).
 * Liegt ein Punkt knapp ausserhalb jeder Küstenlinie (Strand, Boot), wird das
 * nächste Land innerhalb von ~25 km genommen.
 */
class CountryLookup private constructor(private val shapes: List<Shape>) {

    private class Shape(
        val country: Country,
        /** Ringe als flache Arrays [lon0, lat0, lon1, lat1, ...] */
        val rings: List<DoubleArray>,
        val minLon: Double, val minLat: Double, val maxLon: Double, val maxLat: Double,
    )

    private val cache = object : LinkedHashMap<Long, Country?>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Country?>?) = size > 2000
    }

    val isAvailable: Boolean get() = shapes.isNotEmpty()

    /**
     * Alle Ländernamen in den Sprachen der App (AppLanguage.SUPPORTED) – Erkennung "unveränderter" Standard-Ordner (0.8.0).
     * Bewusst nicht alle Sprachen der Geodaten: Ein selbst gewählter Name wie "06 - Italia" soll weiter als umbenannt gelten.
     */
    val allCountryNames: Set<String> by lazy { shapes.flatMap { it.country.names.values }.toSet() }

    /** Anzahl Länder (Anzeige in den Einstellungen) */
    val countryCount: Int get() = byIso.size

    private val byIso: Map<String, Country> by lazy {
        shapes.map { it.country }.filter { it.iso.isNotEmpty() }.associateBy { it.iso.uppercase() }
    }

    /** Name für den Ordner in der App-Sprache; unbekanntes Kürzel -> [fallback] (z. B. alter Name aus dem Cache) */
    fun nameFor(iso: String?, fallback: String?): String? =
        iso?.uppercase()?.let { byIso[it] }?.name() ?: fallback

    /** Alle Namen eines Landes (alle Sprachen), um bestehende Standardordner nach einem Sprachwechsel zu finden */
    fun namesOf(iso: String): Collection<String> = byIso[iso.uppercase()]?.names?.values?.distinct() ?: emptyList()

    @Synchronized
    fun lookup(lat: Double, lon: Double): Country? {
        // Raster ~1 km als Cache-Schlüssel
        val key = (Math.round(lat * 100) shl 32) xor (Math.round(lon * 100) and 0xffffffffL)
        if (cache.containsKey(key)) return cache[key]
        val result = findInside(lat, lon) ?: findNearest(lat, lon, 0.25)
        cache[key] = result
        return result
    }

    private fun findInside(lat: Double, lon: Double): Country? {
        for (s in shapes) {
            if (lon < s.minLon || lon > s.maxLon || lat < s.minLat || lat > s.maxLat) continue
            var inside = false
            for (r in s.rings) if (ringContains(r, lon, lat)) inside = !inside
            if (inside) return s.country
        }
        return null
    }

    private fun ringContains(r: DoubleArray, x: Double, y: Double): Boolean {
        var c = false
        val n = r.size / 2
        var j = n - 1
        for (i in 0 until n) {
            val xi = r[2 * i]; val yi = r[2 * i + 1]
            val xj = r[2 * j]; val yj = r[2 * j + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c
            j = i
        }
        return c
    }

    private fun findNearest(lat: Double, lon: Double, maxDeg: Double): Country? {
        val kx = cos(Math.toRadians(lat))
        var best: Country? = null
        var bestD = maxDeg
        for (s in shapes) {
            if (lon < s.minLon - maxDeg / kx || lon > s.maxLon + maxDeg / kx ||
                lat < s.minLat - maxDeg || lat > s.maxLat + maxDeg) continue
            for (r in s.rings) {
                val n = r.size / 2
                for (i in 0 until n - 1) {
                    val d = segDist(lon * kx, lat, r[2 * i] * kx, r[2 * i + 1], r[2 * i + 2] * kx, r[2 * i + 3])
                    if (d < bestD) { bestD = d; best = s.country }
                }
            }
        }
        return best
    }

    private fun segDist(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax; val dy = by - ay
        val len2 = dx * dx + dy * dy
        var t = if (len2 == 0.0) 0.0 else ((px - ax) * dx + (py - ay) * dy) / len2
        t = t.coerceIn(0.0, 1.0)
        val cx = ax + t * dx - px; val cy = ay + t * dy - py
        return sqrt(cx * cx + cy * cy)
    }

    companion object {
        const val ASSET = "geo/countries.geojson"
        @Volatile private var instance: CountryLookup? = null

        /**
         * Ländername zur Anzeige (Statistik, Ordnerauswahl) in der App-Sprache: aus den Geodaten, falls schon geladen,
         * sonst aus Java (Locale). [fallback] = gespeicherter Name. null = kein Land.
         */
        fun displayName(iso: String?, fallback: String?): String? {
            val code = iso?.trim()?.uppercase()?.takeIf { it.length == 2 } ?: return fallback
            instance?.byIso?.get(code)?.let { return it.name() }
            val n = try { Locale.Builder().setRegion(code).build().getDisplayCountry(Texts.locale) } catch (e: Exception) { "" }
            return if (n.isNotBlank() && !n.equals(code, ignoreCase = true)) n else fallback
        }

        fun get(context: Context): CountryLookup =
            instance ?: synchronized(this) {
                instance ?: load(context).also { instance = it }
            }

        private fun load(context: Context): CountryLookup = try {
            context.assets.open(ASSET).use { input ->
                JsonReader(InputStreamReader(input.buffered(1 shl 16), Charsets.UTF_8)).use { parse(it) }
            }
        } catch (e: Exception) {
            CountryLookup(emptyList())
        }

        // ---- Streaming-Parser (wenig Speicher, nur benötigte Felder) ----------
        private fun parse(r: JsonReader): CountryLookup {
            val shapes = ArrayList<Shape>(300)
            r.beginObject()
            while (r.hasNext()) {
                if (r.nextName() == "features") {
                    r.beginArray()
                    while (r.hasNext()) readFeature(r, shapes)
                    r.endArray()
                } else r.skipValue()
            }
            r.endObject()
            return CountryLookup(shapes)
        }

        private fun readFeature(r: JsonReader, out: MutableList<Shape>) {
            var props: Map<String, String> = emptyMap()
            var polys: List<List<DoubleArray>> = emptyList()
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "properties" -> props = readProps(r)
                    "geometry" -> polys = readGeometry(r)
                    else -> r.skipValue()
                }
            }
            r.endObject()
            val iso = listOf("ISO_A2_EH", "ISO_A2", "WB_A2")
                .mapNotNull { props[it] }.firstOrNull { it.length == 2 && it != "-9" } ?: ""
            // Namen je Sprache: NAME_DE -> "de", NAME_EN (sonst NAME) -> "en", NAME_FR -> "fr" …
            val names = LinkedHashMap<String, String>()
            for ((k, v) in props) {
                if (v.isBlank()) continue
                if (k.length == 7 && k.startsWith("NAME_")) {
                    val lang = k.substring(5).lowercase()
                    if (lang in AppLanguage.SUPPORTED) names[lang] = v
                }
            }
            if ("en" !in names) props["NAME"]?.takeIf { it.isNotBlank() }?.let { names["en"] = it }
            if (names.isEmpty()) return
            val country = Country(iso.uppercase(), names)
            for (rings in polys) {
                if (rings.isEmpty()) continue
                val outer = rings[0]
                var minX = Double.MAX_VALUE; var minY = Double.MAX_VALUE
                var maxX = -Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
                var i = 0
                while (i < outer.size) {
                    val x = outer[i]; val y = outer[i + 1]
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                    i += 2
                }
                out += Shape(country, rings, minX, minY, maxX, maxY)
            }
        }

        private val wantedProps = setOf("ISO_A2_EH", "ISO_A2", "WB_A2", "NAME")
        /** Namen in einzelnen Sprachen, z. B. NAME_DE, NAME_EN (nicht NAME_LONG, NAME_SORT …) */
        private val langNameProp = Regex("^NAME_[A-Z]{2}$")

        private fun readProps(r: JsonReader): Map<String, String> {
            val m = HashMap<String, String>()
            if (r.peek() == JsonToken.NULL) { r.nextNull(); return m }
            r.beginObject()
            while (r.hasNext()) {
                val k = r.nextName().uppercase()
                if ((k in wantedProps || langNameProp.matches(k)) && r.peek() == JsonToken.STRING) m[k] = r.nextString()
                else r.skipValue()
            }
            r.endObject()
            return m
        }

        /** Liefert Liste von Polygonen; jedes Polygon = Liste von Ringen. */
        private fun readGeometry(r: JsonReader): List<List<DoubleArray>> {
            if (r.peek() == JsonToken.NULL) { r.nextNull(); return emptyList() }
            var type = ""
            var coords: List<List<DoubleArray>> = emptyList()
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "type" -> type = r.nextString()
                    "coordinates" -> coords = readCoords(r, type)
                    else -> r.skipValue()
                }
            }
            r.endObject()
            return coords
        }

        private fun readCoords(r: JsonReader, type: String): List<List<DoubleArray>> {
            // "type" steht in Natural Earth vor "coordinates"; Tiefe trotzdem robust erkennen.
            return when (type) {
                "Polygon" -> listOf(readPolygon(r))
                "MultiPolygon" -> {
                    val list = ArrayList<List<DoubleArray>>()
                    r.beginArray(); while (r.hasNext()) list += readPolygon(r); r.endArray()
                    list
                }
                else -> { r.skipValue(); emptyList() }
            }
        }

        private fun readPolygon(r: JsonReader): List<DoubleArray> {
            val rings = ArrayList<DoubleArray>()
            r.beginArray()
            while (r.hasNext()) {
                val pts = ArrayList<Double>(64)
                r.beginArray()
                while (r.hasNext()) {
                    r.beginArray()
                    pts += r.nextDouble(); pts += r.nextDouble()
                    while (r.hasNext()) r.skipValue()
                    r.endArray()
                }
                r.endArray()
                rings += pts.toDoubleArray()
            }
            r.endArray()
            return rings
        }
    }
}
