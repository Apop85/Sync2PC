package com.sync2pc.app.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    /** z. B. "DCIM/Camera/" (immer mit abschliessendem /) */
    val relativePath: String,
    val size: Long,
    val dateModifiedSec: Long,
    val dateAddedSec: Long,
    val isVideo: Boolean,
    val mime: String?,
) {
    val sourceKey: String get() = relativePath + displayName
    val baseName: String get() = displayName.substringBeforeLast('.')
}

data class FolderInfo(val relativePath: String, val count: Int)

/** Liest Fotos/Videos über den MediaStore (schnell, keine Dateisystem-Traversierung). */
object MediaScanner {
    private const val TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
    private const val TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

    private val filesUri: Uri get() = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private fun escapeLike(s: String) =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun typeSelection(images: Boolean, videos: Boolean): String? {
        val types = buildList { if (images) add(TYPE_IMAGE); if (videos) add(TYPE_VIDEO) }
        if (types.isEmpty()) return null
        return "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${types.joinToString(",")})"
    }

    fun scan(
        context: Context,
        folders: List<String>,
        includeSubfolders: Boolean,
        images: Boolean,
        videos: Boolean,
    ): List<MediaItem> {
        if (folders.isEmpty()) return emptyList()
        val typeSel = typeSelection(images, videos) ?: return emptyList()
        val args = ArrayList<String>()
        val folderSel = folders.joinToString(" OR ", "(", ")") { f ->
            val norm = if (f.endsWith("/")) f else "$f/"
            if (includeSubfolders) {
                args += escapeLike(norm) + "%"
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? ESCAPE '\\'"
            } else {
                args += norm
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            }
        }
        // IS_TRASHED gibt es erst ab Android 11 (API 30); minSdk ist 29
        val trashedSel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            " AND ${MediaStore.MediaColumns.IS_TRASHED} = 0" else ""
        val selection = "$typeSel AND $folderSel AND ${MediaStore.MediaColumns.IS_PENDING} = 0$trashedSel"
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.VOLUME_NAME,
        )
        val result = ArrayList<MediaItem>()
        context.contentResolver.query(
            filesUri, projection, selection, args.toTypedArray(),
            "${MediaStore.MediaColumns.DATE_ADDED} ASC"
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val iType = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val iPath = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val iMod = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val iAdd = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val iMime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val iVol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)
            while (c.moveToNext()) {
                val name = c.getString(iName) ?: continue
                val size = c.getLong(iSize)
                if (size <= 0) continue
                val id = c.getLong(iId)
                val isVideo = c.getInt(iType) == TYPE_VIDEO
                val volume = c.getString(iVol) ?: MediaStore.VOLUME_EXTERNAL_PRIMARY
                val base = if (isVideo) MediaStore.Video.Media.getContentUri(volume)
                else MediaStore.Images.Media.getContentUri(volume)
                result += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(base, id),
                    displayName = name,
                    relativePath = c.getString(iPath) ?: "",
                    size = size,
                    dateModifiedSec = c.getLong(iMod),
                    dateAddedSec = c.getLong(iAdd),
                    isVideo = isVideo,
                    mime = c.getString(iMime),
                )
            }
        }
        return result
    }

    /** Wurden seit dem Zeitpunkt (Sekunden) neue Fotos/Videos hinzugefügt? */
    fun hasAddedSince(context: Context, sinceSec: Long): Boolean =
        context.contentResolver.query(
            filesUri, arrayOf(MediaStore.Files.FileColumns._ID),
            "${typeSelection(true, true)} AND ${MediaStore.MediaColumns.DATE_ADDED} > ?",
            arrayOf(sinceSec.toString()), null
        )?.use { it.count > 0 } ?: false

    /** Alle Ordner mit Fotos/Videos inkl. Anzahl – für die Ordnerauswahl. */
    fun listFolders(context: Context): List<FolderInfo> {
        val counts = HashMap<String, Int>()
        context.contentResolver.query(
            filesUri,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
            "${typeSelection(true, true)}",
            null, null
        )?.use { c ->
            val i = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            while (c.moveToNext()) {
                val p = c.getString(i) ?: continue
                counts[p] = (counts[p] ?: 0) + 1
            }
        }
        return counts.map { FolderInfo(it.key, it.value) }.sortedBy { it.relativePath.lowercase() }
    }
}
