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

    fun compute(size: Long, read: (offset: Long, length: Int) -> ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(ByteBuffer.allocate(8).putLong(size).array())
        for ((off, len) in regions(size)) {
            if (len > 0) md.update(read(off, len))
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
