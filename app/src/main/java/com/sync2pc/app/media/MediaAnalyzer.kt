package com.sync2pc.app.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Ermittelt Aufnahme-Monat und GPS-Position einer Datei.
 * Reihenfolge Datum: EXIF (Bild) bzw. Container-Metadaten (Video) -> Dateiname -> Änderungsdatum.
 */
class MediaAnalyzer(
    private val context: Context,
    /** ACCESS_MEDIA_LOCATION erteilt? Sonst schwärzt Android die GPS-Daten. */
    private val canReadLocation: Boolean,
) {
    data class Result(
        val yearMonth: YearMonth,
        val dateSource: String,
        val latitude: Double?,
        val longitude: Double?,
    )

    private val zone: ZoneId = ZoneId.systemDefault()

    fun analyze(item: MediaItem): Result {
        val uri = originalUri(item.uri)
        var ym: YearMonth? = null
        var source = ""
        var lat: Double? = null
        var lon: Double? = null

        if (item.isVideo) {
            readVideo(uri)?.let { (d, l) ->
                if (d != null) { ym = d; source = "META" }
                if (l != null) { lat = l.first; lon = l.second }
            }
        } else {
            readExif(uri)?.let { (d, l) ->
                if (d != null) { ym = d; source = "EXIF" }
                if (l != null) { lat = l.first; lon = l.second }
            }
        }
        if (ym == null) {
            FilenameDateParser.parse(item.displayName, zone)?.let { ym = it; source = "FILENAME" }
        }
        if (ym == null) {
            val dt = Instant.ofEpochSecond(item.dateModifiedSec).atZone(zone)
            ym = YearMonth.of(dt.year, dt.monthValue)
            source = "MODIFIED"
        }
        return Result(ym!!, source, lat, lon)
    }

    private fun originalUri(uri: Uri): Uri =
        if (canReadLocation) try { MediaStore.setRequireOriginal(uri) } catch (e: Exception) { uri } else uri

    private fun validYm(y: Int, m: Int): YearMonth? =
        if (y in 1980..2100 && m in 1..12) YearMonth.of(y, m) else null

    private fun readExif(uri: Uri): Pair<YearMonth?, Pair<Double, Double>?>? = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val exif = ExifInterface(pfd.fileDescriptor)
            val dateStr = listOf(
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_DATETIME,
            ).firstNotNullOfOrNull { tag -> exif.getAttribute(tag)?.takeIf { it.length >= 7 } }
            // Format "yyyy:MM:dd HH:mm:ss" -> Lokalzeit der Kamera, keine Umrechnung nötig
            val ym = dateStr?.let {
                val y = it.substring(0, 4).toIntOrNull()
                val m = it.substring(5, 7).toIntOrNull()
                if (y != null && m != null) validYm(y, m) else null
            }
            val ll = exif.latLong?.let { a ->
                if (a.size == 2 && !(a[0] == 0.0 && a[1] == 0.0)) a[0] to a[1] else null
            }
            ym to ll
        }
    } catch (e: Exception) {
        null
    }

    private val videoDate = Regex("""(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})""")
    private val iso6709 = Regex("""([+-]\d+(?:\.\d+)?)([+-]\d+(?:\.\d+)?)""")

    private fun readVideo(uri: Uri): Pair<YearMonth?, Pair<Double, Double>?>? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            // Video-Datum ist UTC -> in lokale Zeit umrechnen
            val ym = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.let { s ->
                videoDate.find(s)?.let { m ->
                    val g = m.groupValues.drop(1).map { it.toInt() }
                    if (g[0] < 1980) null else try {
                        val local = LocalDateTime.of(g[0], g[1], g[2], g[3], g[4], g[5])
                            .atOffset(ZoneOffset.UTC).atZoneSameInstant(zone)
                        YearMonth.of(local.year, local.monthValue)
                    } catch (e: Exception) { null }
                }
            }
            val ll = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)?.let { s ->
                iso6709.find(s)?.let { m ->
                    val la = m.groupValues[1].toDoubleOrNull()
                    val lo = m.groupValues[2].toDoubleOrNull()
                    if (la != null && lo != null && !(la == 0.0 && lo == 0.0)) la to lo else null
                }
            }
            ym to ll
        } catch (e: Exception) {
            null
        } finally {
            try { mmr.release() } catch (ignored: Exception) {}
        }
    }
}
