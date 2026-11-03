package com.sync2pc.app.sync

import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Schneller Inhaltsvergleich ohne die ganze Datei zu lesen:
 * SHA-256 über (Dateigrösse + 64 KB Anfang + 64 KB Mitte + 64 KB Ende).
 * Dateien <= 192 KB werden vollständig gehasht.
 * Unterschiedliche Grösse => unterschiedliche Datei (Hash wird gar nicht erst berechnet).
 */
object PartialHash {
    const val CHUNK = 64 * 1024

    /** (Offset, Länge) der zu lesenden Bereiche */
    fun regions(size: Long): List<Pair<Long, Int>> =
        if (size <= 3L * CHUNK) listOf(0L to size.toInt())
        else listOf(
            0L to CHUNK,
            (size / 2 - CHUNK / 2) to CHUNK,
            (size - CHUNK) to CHUNK,
        )

    fun compute(size: Long, read: (offset: Long, length: Int) -> ByteArray): String =
        hash(size, regions(size), read)

    // ---- Gründlicher Vergleich für den Ordner-Sync (0.4.1) ------------------------------------
    /** Bis zu dieser Grösse wird die ganze Datei gehasht */
    const val FULL_LIMIT = 32L * 1024 * 1024
    /** Darüber: so viele Stichproben à 64 KB, gleichmässig über die Datei verteilt */
    const val SAMPLES = 32
    private const val FULL_BLOCK = 1024 * 1024

    /**
     * Bereiche für [thorough]: bis 32 MB die ganze Datei in Blöcken à 1 MB, darüber 32 Stichproben
     * (erste am Anfang, letzte am Ende).
     */
    fun thoroughRegions(size: Long): List<Pair<Long, Int>> {
        if (size <= 0) return emptyList()
        if (size <= FULL_LIMIT) {
            val out = ArrayList<Pair<Long, Int>>()
            var off = 0L
            while (off < size) {
                val len = minOf(FULL_BLOCK.toLong(), size - off).toInt()
                out += off to len
                off += len
            }
            return out
        }
        val last = size - CHUNK
        return (0 until SAMPLES).map { i -> (last * i / (SAMPLES - 1)) to CHUNK }
    }

    /** Inhaltsvergleich beim Erstabgleich bzw. bei auf beiden Seiten geänderten Dateien (Ordner-Sync) */
    fun thorough(size: Long, read: (offset: Long, length: Int) -> ByteArray): String =
        hash(size, thoroughRegions(size), read)

    private fun hash(size: Long, regions: List<Pair<Long, Int>>, read: (Long, Int) -> ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(ByteBuffer.allocate(8).putLong(size).array())
        for ((off, len) in regions) {
            if (len > 0) md.update(read(off, len))
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
